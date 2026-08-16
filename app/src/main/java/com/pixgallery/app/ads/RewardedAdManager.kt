package com.pixgallery.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

// Your real AdMob Rewarded ad unit (created from the AdMob console).
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-2350728358948132/2993938347"

// Google's official test ad unit for Rewarded ads. Always serves a real-looking
// test ad and is completely safe to tap while developing.
private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

/**
 * Loads and shows full-screen Rewarded video ads, with built-in frequency
 * capping so the ad doesn't show on every single action.
 *
 * Usage:
 *   val rewardedAdManager = remember { RewardedAdManager(context, useTestAd = true) }
 *   ...
 *   rewardedAdManager.maybeShow(activity) { proceedToNextScreen() }
 *
 * The `onDone` callback always runs - whether or not an ad was actually
 * shown, and whether or not the user watched it to completion - so your
 * navigation logic stays simple and never gets stuck. (We don't gate any
 * app feature behind actually earning the reward - the ad just plays as a
 * normal video ad opportunity.)
 */
class RewardedAdManager(
    private val context: Context,
    private val useTestAd: Boolean = false,
    // Show an ad every Nth call to maybeShow() - tune this to balance
    // revenue vs. user annoyance. Bumped up from 3 -> 10: showing a
    // full-screen ad every 3rd photo made the app unusable for its core
    // job (quickly flipping through your own photos) and is exactly the
    // kind of "excessive/intrusive advertising" store reviewers reject
    // apps for. 10 keeps some monetization without interrupting normal
    // browsing every few taps.
    private val everyNActions: Int = 10,
    // Extra safety net: never show two of these ads less than this many
    // milliseconds apart, even if the action-count math would allow it
    // (e.g. user rapidly opening/closing photos).
    private val minIntervalMillis: Long = 90_000L
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false
    private var actionCount = 0
    private var lastShownAtMillis = 0L

    private val adUnitId: String
        get() = if (useTestAd) TEST_REWARDED_AD_UNIT_ID else REWARDED_AD_UNIT_ID

    init {
        loadAd()
    }

    private fun loadAd() {
        if (isLoadingAd || rewardedAd != null) return
        isLoadingAd = true
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoadingAd = false
                    Log.w("RewardedAdManager", "Failed to load rewarded ad: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Call this at the moment you'd normally navigate (e.g. opening a photo).
     * Shows an ad only every [everyNActions] calls; every other time it just
     * runs [onDone] immediately. Always call [onDone] to continue - never
     * gate navigation on the ad succeeding or on the reward being earned.
     */
    fun maybeShow(activity: Activity, onDone: () -> Unit) {
        actionCount++
        val now = System.currentTimeMillis()
        val cadenceHit = actionCount % everyNActions == 0
        val cooldownOver = now - lastShownAtMillis >= minIntervalMillis
        val shouldShow = cadenceHit && cooldownOver
        val ad = rewardedAd

        if (!shouldShow || ad == null) {
            onDone()
            return
        }
        lastShownAtMillis = now

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadAd()
                onDone()
            }
        }

        // onUserEarnedReward fires if they watch to completion - we don't
        // need to do anything with it since no in-app reward is tied to it.
        ad.show(activity) { }
    }
}
