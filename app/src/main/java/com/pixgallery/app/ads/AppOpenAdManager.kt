package com.pixgallery.app.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

// Your real AdMob "App open" ad unit (PixGallery_AppOpen). Safe to ship - it's
// an ad unit ID, not a secret key.
private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-2350728358948132/1227039453"

// Google's official test ad unit for App Open ads. Always serves a real-looking
// test ad and is completely safe to tap while you're developing - it never
// counts as a real impression/click, so it can't get your AdMob account flagged.
private const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

/**
 * Loads a full-screen "App Open" ad and shows it automatically whenever the
 * app is launched or brought back to the foreground - this is what gives you
 * the "ad plays when the app opens" behavior.
 *
 * Wire it up once from your Application class, e.g.:
 *   AppOpenAdManager(this, useTestAd = true)
 *
 * @param useTestAd keep this true while developing so you never tap a real ad
 *                   by accident. Set it to false only right before you publish
 *                   to the Play Store.
 */
class AppOpenAdManager(
    private val application: Application,
    private val useTestAd: Boolean = true
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0

    private var currentActivity: Activity? = null

    private val adUnitId: String
        get() = if (useTestAd) TEST_APP_OPEN_AD_UNIT_ID else APP_OPEN_AD_UNIT_ID

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        loadAd()
    }

    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        AppOpenAd.load(
            application,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.w("AppOpenAdManager", "Failed to load app open ad: ${loadAdError.message}")
                }
            }
        )
    }

    // App Open ads expire after ~4 hours; past that we need a fresh one.
    private fun isAdAvailable(): Boolean {
        val fourHoursInMillis = 4 * 3600000L
        return appOpenAd != null && Date().time - loadTime < fourHoursInMillis
    }

    private fun showAdIfAvailable() {
        val activity = currentActivity ?: return
        if (isShowingAd) return

        if (!isAdAvailable()) {
            loadAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        appOpenAd?.show(activity)
    }

    // Called by ProcessLifecycleOwner whenever the whole app (any activity)
    // moves to the foreground - i.e. a cold start OR coming back from Home/recents.
    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }
}
