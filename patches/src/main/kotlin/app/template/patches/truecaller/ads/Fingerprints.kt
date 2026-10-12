package app.template.patches.truecaller.ads

import app.morphe.patcher.Fingerprint

// The three ad views every banner and native placement binds through. Their class names are kept
// (they are inflated from layouts); their bind methods are R8-renamed, so the patch selects them
// by parameter types.
internal const val ADS_CONTAINER = "Lcom/truecaller/ads/ui/AdsContainer;"
internal const val ADS_CONTAINER_LIGHT = "Lcom/truecaller/ads/ui/AdsContainerLight;"
internal const val ADS_SWITCH_VIEW = "Lcom/truecaller/ads/ui/AdsSwitchView;"
internal const val AD_LAYOUT_TYPE = "Lcom/truecaller/ads/AdLayoutTypeX;"

// AdsSwitchView's bind(bannerAd, AdLayoutTypeX). Its first parameter is the (obfuscated) banner
// ad holder type, which the patch reads from here instead of hard-coding it.
internal object AdsSwitchViewBindFingerprint : Fingerprint(
    definingClass = ADS_SWITCH_VIEW,
    returnType = "V",
    parameters = listOf("L", AD_LAYOUT_TYPE),
    custom = { method, _ -> method.parameterTypes.size == 2 },
)

// Full-screen interstitial and app-open ads are shown in their own activities (kept: declared in
// the manifest).
internal val FULL_SCREEN_AD_ACTIVITIES = listOf(
    "Lcom/truecaller/ads/interstitial/AdInterstitialActivity;",
    "Lcom/truecaller/ads/interstitial/AdAppOpeningActivity;",
    "Lcom/truecaller/ads/adsrouter/ui/interstitial/AdRouterInterstitialActivity;",
)
