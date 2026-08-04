package com.pixgallery.app.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Real, working video playback for the photo viewer (not just a thumbnail).
 * Tap once to start playing; Android's built-in MediaController gives a
 * seek bar / pause / duration display for free.
 *
 * [onPlayingStateChanged] reports whether the video is actively playing right
 * now (true) or paused/stopped (false), so the screen hosting this player can
 * hide things like the share/edit/favorite/delete bar while playback is on
 * and bring it back the moment the user pauses.
 */
@Composable
fun VideoPlayerView(
    uri: Uri,
    modifier: Modifier = Modifier,
    onPlayingStateChanged: (Boolean) -> Unit = {}
) {
    var started by remember { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(uri)
                    setMediaController(MediaController(context).also { it.setAnchorView(this) })
                    videoView = this
                }
            },
            update = { view ->
                if (started && !view.isPlaying) {
                    view.start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!started) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { started = true },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // VideoView/MediaController don't expose a play/pause listener, so we poll
    // lightly while a video is on screen. This is cheap (few times a second)
    // and is what lets the action bar hide during playback and reappear the
    // instant the user taps pause via the native seek controller.
    LaunchedEffect(started) {
        if (!started) {
            onPlayingStateChanged(false)
            return@LaunchedEffect
        }
        while (true) {
            onPlayingStateChanged(videoView?.isPlaying == true)
            delay(300)
        }
    }
}
