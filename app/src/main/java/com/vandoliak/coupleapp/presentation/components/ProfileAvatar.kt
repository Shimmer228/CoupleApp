package com.vandoliak.coupleapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun ProfileAvatar(
    avatarKey: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val normalized = avatarKey
        ?.trim()
        ?.lowercase()
        ?.ifBlank { "heart" }
        ?: "heart"

    val (emoji, backgroundColor) = when (normalized) {
        "cat" -> "🐱" to Color(0xFFFFE0B2)
        "fox" -> "🦊" to Color(0xFFFFCCBC)
        "bear" -> "🐻" to Color(0xFFD7CCC8)
        "star", "sun" -> "⭐" to Color(0xFFFFF3CD)
        "moon" -> "🌙" to Color(0xFFDDE7FF)
        else -> "❤️" to Color(0xFFFAD2E1)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = avatarFontSize(size),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun avatarFontSize(size: Dp): TextUnit {
    return when {
        size.value >= 72f -> 32.sp
        size.value >= 52f -> 24.sp
        else -> 18.sp
    }
}
