package com.pixgallery.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder

class PixGalleryApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Register a Coil ImageLoader that can decode video thumbnails too,
        // so video files show a frame preview instead of a blank tile.
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()

        Coil.setImageLoader(imageLoader)
    }
}
