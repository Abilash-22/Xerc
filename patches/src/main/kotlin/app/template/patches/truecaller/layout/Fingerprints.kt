package app.template.patches.truecaller.layout

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

// Feature toggles are read as isEnabled("<key>", default state) in small R8-renamed getters. The
// keys are exact-matched: both are prefixes of other toggles ("featureCallAssistantNumberSync",
// "featureScamFeedBottomTab", ...), which a plain `strings` match would also accept.

// Master switch of the Call Assistant: about 30 call sites check it, among them the Assistant
// tab, its deep links, FCM message handling and the assistant auth token worker.
internal object CallAssistantFeatureFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(string("featureCallAssistant")),
)

// Master switch of the Scam Feed: the bottom tab, the drawer menu entry, its deep link, the
// call-history hook and the Scam Feed notification worker all check it first.
internal object ScamFeedFeatureFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(string("featureScamFeed")),
)
