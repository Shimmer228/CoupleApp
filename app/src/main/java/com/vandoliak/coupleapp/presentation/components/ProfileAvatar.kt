package com.vandoliak.coupleapp.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.vandoliak.coupleapp.data.remote.RetrofitInstance

@Composable
fun ProfileAvatar(
    avatarKey: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    localAvatarBytes: ByteArray? = null
) {
    val context = LocalContext.current
    val resolvedAvatarUrl = RetrofitInstance.resolveUrl(avatarUrl)

    if (localAvatarBytes != null) {
        val bitmap = remember(localAvatarBytes) {
            BitmapFactory.decodeByteArray(localAvatarBytes, 0, localAvatarBytes.size)
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            return
        }
    }

    if (!resolvedAvatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = coil3.request.ImageRequest.Builder(context)
                .data(resolvedAvatarUrl)
                .build(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        return
    }

    val normalized = avatarKey
        ?.trim()
        ?.lowercase()
        ?.ifBlank { "heart" }
        ?: "heart"

    val (emoji, backgroundColor) = when (normalized) {
        "cat" -> "\uD83D\uDC31" to Color(0xFFFFE0B2)
        "fox" -> "\uD83E\uDD8A" to Color(0xFFFFCCBC)
        "bear" -> "\uD83D\uDC3B" to Color(0xFFD7CCC8)
        "star", "sun" -> "\u2B50" to Color(0xFFFFF3CD)
        "moon" -> "\uD83C\uDF19" to Color(0xFFDDE7FF)
        else -> "\u2764\uFE0F" to Color(0xFFFAD2E1)
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
