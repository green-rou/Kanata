package com.greenrou.kanata.data.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())

        val body = request.dataToSend()?.let { RequestBody.create(null, it) }
        val method = request.httpMethod()
        if (method == "GET" || method == "HEAD") {
            builder.method(method, null)
        } else {
            builder.method(method, body ?: RequestBody.create(null, ByteArray(0)))
        }

        request.headers()?.forEach { (key, values) ->
            values.forEach { builder.addHeader(key, it) }
        }

        val response = client.newCall(builder.build()).execute()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string(),
            response.request.url.toString()
        )
    }
}
