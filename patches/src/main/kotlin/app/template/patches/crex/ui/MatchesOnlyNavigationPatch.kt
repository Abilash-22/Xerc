package app.template.patches.crex.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.template.patches.shared.Constants.COMPATIBILITY_CREX
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

// Bottom-nav item ids and the Matches TabLayout lookup live in the extension (CrexNav.java).
private const val EXTENSION = "Lapp/template/extension/crexnav/CrexNav;"

// @id of the Series bottom-nav item (res/values/public.xml, v26.10.07) and the string resource
// ed(String) uses when it sets the Series tab's title (0x7f140813). Only used to find ed().
private const val ID_SERIES = 0x7f0a1846
private const val SERIES_TITLE_STRING = 0x7f140813

@Suppress("unused")
val matchesOnlyNavigationPatch = bytecodePatch(
    name = "Matches tabs in bottom navigation",
    description = "UI only. The Matches screen's top tabs (Live, For you, Upcoming, Finished) move " +
            "into the bottom navigation bar and the top tab strip is hidden. The Matches item " +
            "and the Settings item are gone from the bar; the Menu screen is reached through the " +
            "top-right three-line button, which stays visible. The app always opens on Matches. " +
            "The original Home, Social, Videos and Series items are reused for the four tabs " +
            "(not removed) because HomeActivity looks them up by index."
) {
    compatibleWith(COMPATIBILITY_CREX)

    extendWith("extensions/crexnav.mpe")

    execute {
        val fingerprint = HomeCreateFingerprint
        val method = fingerprint.method
        val getMenuIndex = fingerprint.instructionMatches.first().index
        val stringIndex = fingerprint.instructionMatches.last().index

        // 1. Launch tab. The restore block ends at a label followed by
        //    `sget-object vX, HomeTabs->VIDEOS`; v0 holds the tab name there. Overwrite it with
        //    "MATCHES" right after that sget (inserting after, not at, the labelled instruction
        //    so the branches into the label still run it). The app therefore always stays on
        //    Matches; the four visible items only drive the Matches tabs.
        var videosSget = -1
        for (i in stringIndex downTo 0) {
            val instruction = method.getInstruction(i)
            if (instruction.opcode != Opcode.SGET_OBJECT) continue
            val ref = (instruction as ReferenceInstruction).reference as FieldReference
            if (ref.definingClass == HOME_TABS && ref.name == "VIDEOS") {
                videosSget = i
                break
            }
        }
        if (videosSget < 0) throw PatchException("HomeTabs.VIDEOS check before \"Default Videos\" not found")
        method.addInstructions(videosSget + 1, "const-string v0, \"MATCHES\"")

        // 2. Re-label Home/Social/Videos/Series as Live/For you/Upcoming/Finished (with icons) and
        //    hide the rest (Matches). getMenu() -> move-result-object -> iput-object (Menu field).
        val store = method.getInstruction(getMenuIndex + 2)
        val storeRef = (store as? ReferenceInstruction)?.reference as? FieldReference
        if (store.opcode != Opcode.IPUT_OBJECT || storeRef?.type != "Landroid/view/Menu;") {
            throw PatchException("Expected the Menu field store after getMenu(), found ${store.opcode}")
        }
        val menu = (store as OneRegisterInstruction).registerA
        //    The same spot also installs the Matches TabLayout hook (hides the strip and keeps
        //    the selected bottom item in sync with swipes). It only registers a layout listener
        //    on the window, so it needs no views yet and does not depend on how onCreate ends.
        //    p0 is `this`.
        method.addInstructions(
            getMenuIndex + 3,
            """
                invoke-static {v$menu}, $EXTENSION->setupMenu(Landroid/view/Menu;)V
                invoke-static {p0}, $EXTENSION->install(Landroid/app/Activity;)V
            """.trimIndent()
        )

        // 3. Tapping one of the four items selects the matching Matches tab instead of switching
        //    the app's tab; returning true makes the bar show it as selected. Hb has .locals 6
        //    and v0 is unused at its first instruction.
        HomeTabClickFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, $EXTENSION->onItemSelected(Landroid/app/Activity;Landroid/view/MenuItem;)Z
                move-result v0
                if-eqz v0, :crex_not_tab
                return v0
                :crex_not_tab
                nop
            """.trimIndent()
        )

        // 4. ed(String) renames the Series tab after the current series (the "ZIM-WvsIN" label).
        //    Find it by its two resource ids rather than its obfuscatable name, and stop it so
        //    the item stays "Finished".
        val titleSetter = fingerprint.classDef.methods.firstOrNull { candidate ->
            candidate.returnType == "V" &&
                candidate.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") &&
                candidate.implementation?.instructions?.let { instructions ->
                    val literals = instructions.mapNotNull { (it as? NarrowLiteralInstruction)?.narrowLiteral }
                    literals.contains(SERIES_TITLE_STRING) && literals.contains(ID_SERIES)
                } == true
        } ?: throw PatchException("Series tab title setter (ed) not found")
        titleSetter.returnEarly()
    }
}
