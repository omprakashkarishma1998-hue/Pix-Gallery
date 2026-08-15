package com.pixgallery.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.google.android.gms.ads.MobileAds
import com.pixgallery.app.ads.AppOpenAdManager

class PixGalleryApp : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

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

        // Initialize AdMob once, on a background thread, as soon as the app starts.
        MobileAds.initialize(this) {}

        // Loads a full-screen ad and shows it automatically every time the app
        // is opened or brought back to the foreground. useTestAd = false since
        // this build is going out to real users (Uptodown) - it needs to use
        // the real ad unit ID to actually earn revenue.
        appOpenAdManager = AppOpenAdManager(this, useTestAd = false)
    }
}
