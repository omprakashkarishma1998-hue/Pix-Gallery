package com.pixgallery.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Your real AdMob banner ad unit (PixGallery_Banner). This is safe to ship -
// it's an ad unit ID, not a secret key.
private const val BANNER_AD_UNIT_ID = "ca-app-pub-2350728358948132/4244085034"

// Google's official sample banner ad unit ID. It ALWAYS serves a real-looking
// test ad and never earns/costs real money or counts as a real impression.
// Use this while you're building/testing on your own device so you don't
// accidentally tap your own live ads (Google can suspend your AdMob account
// for "invalid traffic" if you click your own real ads repeatedly).
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/**
 * A simple AdMob banner, meant to sit above the bottom nav bar.
 *
 * @param useTestAd pass true while developing/testing so you never click a real ad by mistake.
 *                  Switch it to false before publishing to Play Store.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    useTestAd: Boolean = true
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = if (useTestAd) TEST_BANNER_AD_UNIT_ID else BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
