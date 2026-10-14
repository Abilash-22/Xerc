package app.template.patches.linkedin.controls

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val NAV_PANEL_ACCOUNT_VIEW_DATA = "Lcom/linkedin/android/home/navpanel/HomeNavPanelAccountSectionViewData;"

/**
 * Builds the home nav panel's footer list: a divider, the "Settings & Privacy" row
 * (HomeNavPanelAccountSectionViewData with AccountSectionType.SETTINGS) and the Premium upsell rows,
 * wrapped with ResourceKt.map. It lives in a synthetic lambda of HomeNavPanelFeature, so it is found
 * by the Premium slot URN it compares (also used by PremiumDashUpsellCustomPresenterCreator, which
 * builds no footer row) plus the footer row constructor, not by the lambda's name.
 */
internal object NavPanelFooterListFingerprint : Fingerprint(
    strings = listOf("urn:li:fsd_premiumUpsellSlot:COMMUNITY_PANEL_FOR_PAGES"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.opcode == Opcode.INVOKE_DIRECT &&
                instruction.getReference<MethodReference>()?.let {
                    it.definingClass == NAV_PANEL_ACCOUNT_VIEW_DATA && it.name == "<init>"
                } == true
        } == true
    },
)
