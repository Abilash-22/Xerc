package app.template.patches.crex.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal const val HOME_TABS = "Lin/cricketexchange/app/cricketexchange/home/model/HomeTabs;"
internal const val NAVIGATION_BAR_VIEW = "Lcom/google/android/material/navigation/NavigationBarView;"

// HomeActivity.onCreate (v26.10.07). Two anchors, both unobfuscated:
//  - NavigationBarView.getMenu(): followed by `move-result-object` and `iput-object` of the
//    Menu into a field (J1 in this version, name not relied on). The bottom nav is built here.
//  - "Default Videos": part of the launch-tab restore block, which compares the restored tab
//    name (kept in a String register) against HomeTabs.VIDEOS / MATCHES / HOMEV2.
internal object HomeCreateFingerprint : Fingerprint(
    definingClass = "Lin/cricketexchange/app/cricketexchange/activities/HomeActivity;",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        methodCall(definingClass = NAVIGATION_BAR_VIEW, name = "getMenu"),
        string("Default Videos"),
    ),
)

internal const val MORE_ACTIVITY = "Lin/cricketexchange/app/cricketexchange/activities/MoreActivity;"

// HomeActivity.Hb(MenuItem): the bottom-nav item-selected handler (reached through the synthetic
// o0 listener -> G8 -> Hb). Returns true when it handled the tab. Unique by its
// MenuItem parameter + "lastOpenedFragmentV3" (the pref it saves the chosen tab under).
internal object HomeTabClickFingerprint : Fingerprint(
    definingClass = "Lin/cricketexchange/app/cricketexchange/activities/HomeActivity;",
    returnType = "Z",
    parameters = listOf("Landroid/view/MenuItem;"),
    filters = listOf(
        string("lastOpenedFragmentV3"),
    ),
)
