package com.example.offermatrix.ui.screens.interview

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.offermatrix.R

@OptIn(UnstableApi::class) 
@Composable
fun DigitalHumanPlayer(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean
) {
    val context = LocalContext.current

    val idleUri = remember { Uri.parse("android.resource://${context.packageName}/${R.raw.idle}") }
    val speakingUri = remember { Uri.parse("android.resource://${context.packageName}/${R.raw.speaking}") }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // Mute video audio (we use TTS for sound)
        }
    }

    // React to state changes
    LaunchedEffect(isSpeaking) {
        val targetUri = if (isSpeaking) speakingUri else idleUri
        
        // Only change if different (though specific optimization might request avoiding reload if same, 
        // but URI check handles basic sameness. ExoPlayer handles same media item re-prep efficiently usually)
        if (exoPlayer.currentMediaItem?.localConfiguration?.uri != targetUri) {
            val mediaItem = MediaItem.fromUri(targetUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
             if (!exoPlayer.isPlaying) {
                 exoPlayer.play()
             }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill logic
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}
