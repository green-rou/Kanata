package com.greenrou.kanata.features.main.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.core.util.UiConstants

@Composable
internal fun ScoreBadge(score: Double) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(
                color = UiConstants.StarColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "★ $score",
            style = MaterialTheme.typography.labelSmall,
            color = UiConstants.StarColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
