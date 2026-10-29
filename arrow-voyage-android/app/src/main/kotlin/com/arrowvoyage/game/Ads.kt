package com.arrowvoyage.game

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob for the game, exposed to the page as window.ArrowAds.
 * Every show call ends with exactly one window.__adDone(true|false) so the game never gets stuck.
 * Ad unit IDs live in res/values/admob.xml.
 */
class Ads(private val activity: Activity, private val web: WebView) {
    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var rewarded: RewardedAd? = null
    private val started = AtomicBoolean(false)
    private var loadingInterstitial = false
    private var loadingRewarded = false

    /** Asks for GDPR/US-state consent when needed (Google UMP), then starts the SDK. */
    fun start() {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { if (consent.canRequestAds()) init() }
        }, { if (consent.canRequestAds()) init() })
        if (consent.canRequestAds()) init()
    }

    private fun init() {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(activity) { loadInterstitial(); loadRewarded() }
    }

    private fun loadInterstitial() {
        if (!started.get() || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(activity, activity.getString(R.string.admob_interstitial_id), AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
                override fun onAdFailedToLoad(e: LoadAdError) { loadingInterstitial = false }
            })
    }

    private fun loadRewarded() {
        if (!started.get() || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(activity, activity.getString(R.string.admob_rewarded_id), AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; loadingRewarded = false }
                override fun onAdFailedToLoad(e: LoadAdError) { loadingRewarded = false }
            })
    }

    private fun done(ok: Boolean) = web.post { web.evaluateJavascript("window.__adDone&&window.__adDone($ok)", null) }

    private fun callback(onClosed: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() = onClosed()
        override fun onAdFailedToShowFullScreenContent(e: AdError) = onClosed()
    }

    /** True when the user must be able to reopen the consent form (EEA/UK); the game then shows "Ad privacy" in Settings. */
    @JavascriptInterface
    fun privacyOptionsRequired(): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    @JavascriptInterface
    fun showPrivacyOptions() = activity.runOnUiThread { UserMessagingPlatform.showPrivacyOptionsForm(activity) { } }

    @JavascriptInterface
    fun isRewardedReady(): Boolean {
        if (rewarded == null) activity.runOnUiThread { loadRewarded() }
        return rewarded != null
    }

    @JavascriptInterface
    fun showRewarded() = activity.runOnUiThread {
        val ad = rewarded ?: run { done(false); loadRewarded(); return@runOnUiThread }
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = callback { done(earned); loadRewarded() }
        ad.show(activity) { earned = true }
    }

    @JavascriptInterface
    fun showInterstitial() = activity.runOnUiThread {
        val ad = interstitial ?: run { done(false); loadInterstitial(); return@runOnUiThread }
        interstitial = null
        ad.fullScreenContentCallback = callback { done(true); loadInterstitial() }
        ad.show(activity)
    }
}
