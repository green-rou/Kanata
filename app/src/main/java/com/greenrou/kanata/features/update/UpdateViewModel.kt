package com.greenrou.kanata.features.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.CheckUpdateUseCase
import com.greenrou.kanata.features.update.model.UpdateEvent
import com.greenrou.kanata.features.update.model.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class UpdateViewModel(
    private val checkUpdate: CheckUpdateUseCase,
    private val settings: SettingsManager,
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateState())
    val state = _state.asStateFlow()

    private val _installFile = Channel<File>(Channel.CONFLATED)
    val installFile = _installFile.receiveAsFlow()

    private val _needInstallPermission = Channel<Unit>(Channel.CONFLATED)
    val needInstallPermission = _needInstallPermission.receiveAsFlow()

    fun handleEvent(event: UpdateEvent) {
        when (event) {
            UpdateEvent.CheckUpdate -> checkForUpdate()
            UpdateEvent.SkipUpdate -> skipUpdate()
            UpdateEvent.DismissDialog -> _state.update { it.copy(pendingRelease = null, error = null) }
            UpdateEvent.StartDownload -> downloadAndInstall()
            UpdateEvent.ConsumeNoUpdatesMessage -> _state.update { it.copy(noUpdatesAvailable = false) }
        }
    }

    private fun checkForUpdate() {
        if (_state.value.isChecking) return
        viewModelScope.launch {
            _state.update { it.copy(isChecking = true, error = null, noUpdatesAvailable = false) }
            checkUpdate()
                .onSuccess { release ->
                    _state.update {
                        it.copy(
                            isChecking = false,
                            pendingRelease = release,
                            noUpdatesAvailable = release == null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isChecking = false, error = e.message) }
                }
        }
    }

    private fun skipUpdate() {
        viewModelScope.launch {
            _state.value.pendingRelease?.version?.let { settings.setSkippedVersion(it) }
            _state.update { it.copy(pendingRelease = null) }
        }
    }

    private fun downloadAndInstall() {
        val apkUrl = _state.value.pendingRelease?.apkUrl ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDownloading = true, downloadProgress = 0f, error = null) }
            runCatching {
                downloadApk(apkUrl)
            }.onSuccess { file ->
                _state.update { it.copy(isDownloading = false, downloadProgress = 1f) }
                if (context.packageManager.canRequestPackageInstalls()) {
                    _installFile.send(file)
                } else {
                    _needInstallPermission.send(Unit)
                }
            }.onFailure { e ->
                _state.update { it.copy(isDownloading = false, error = e.message) }
            }
        }
    }

    private suspend fun downloadApk(url: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        check(response.isSuccessful) { "Download failed: ${response.code}" }
        val body = response.body ?: error("Empty response body")

        val updateDir = File(context.cacheDir, "kanata_update").also { it.mkdirs() }
        val apkFile = File(updateDir, "kanata-update.apk")

        val totalBytes = body.contentLength()
        body.byteStream().use { input ->
            apkFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    if (totalBytes > 0) {
                        _state.update { it.copy(downloadProgress = bytesRead.toFloat() / totalBytes) }
                    }
                }
            }
        }
        apkFile
    }
}
