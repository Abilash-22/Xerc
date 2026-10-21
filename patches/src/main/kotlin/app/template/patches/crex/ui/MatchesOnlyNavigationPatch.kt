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

// Resource ids from res/values/public.xml (v26.10.07). They are stable within one app version.
private const val ID_HOME = "0x7f0a0bf8"
private const val ID_CREX_SOCIAL = "0x7f0a0456"
private const val ID_VIDEOS = "0x7f0a1d79"
private const val ID_SERIES = "0x7f0a1846"
// @drawable/ic_settings_new, the gear used by "Match Settings" in fragment_more.xml.
private const val DRAWABLE_SETTINGS_GEAR = "0x7f0804ee"
// String resource ed(String) uses when it sets the Series tab's title (0x7f140813).
private const val SERIES_TITLE_STRING = 0x7f140813

@Suppress("unused")
val matchesOnlyNavigationPatch = bytecodePatch(
    name = "Matches and Settings navigation",
    description = "Phase 1: the bottom navigation keeps only Matches and Settings. Home, Social " +
            "and Videos are hidden, the app always opens on Matches, and the Series tab becomes " +
            "Settings, opening the app's Menu screen (MoreActivity). Tabs are hidden, not " +
            "removed, because HomeActivity looks them up by index."
) {
    compatibleWith(COMPATIBILITY_CREX)

    execute {
        val fingerprint = HomeCreateFingerprint
        val method = fingerprint.method
        val getMenuIndex = fingerprint.instructionMatches.first().index
        val stringIndex = fingerprint.instructionMatches.last().index

        // 1. Launch tab. The restore block ends at a label followed by
        //    `sget-object vX, HomeTabs->VIDEOS`; v0 holds the tab name there. Overwrite it with
        //    "MATCHES" right after that sget (inserting after, not at, the labelled instruction
        //    so the branches into the label still run it). Done first: it sits later in the
        //    method, so the earlier insert below cannot shift its index.
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

        // 2. Hide Home/Social/Videos and retitle Series as Settings. getMenu() ->
        //    move-result-object -> iput-object (Menu field).
        val store = method.getInstruction(getMenuIndex + 2)
        val storeRef = (store as? ReferenceInstruction)?.reference as? FieldReference
        if (store.opcode != Opcode.IPUT_OBJECT || storeRef?.type != "Landroid/view/Menu;") {
            throw PatchException("Expected the Menu field store after getMenu(), found ${store.opcode}")
        }
        val menu = (store as OneRegisterInstruction).registerA
        // v10/v11 are written before any read later in onCreate (checked in v26.10.07), and the
        // menu register is reloaded by the very next instruction, so all three are free here.
        fun hide(id: String) = """
            const v10, $id
            invoke-interface {v$menu, v10}, Landroid/view/Menu;->findItem(I)Landroid/view/MenuItem;
            move-result-object v11
            const/4 v10, 0x0
            invoke-interface {v11, v10}, Landroid/view/MenuItem;->setVisible(Z)Landroid/view/MenuItem;
        """.trimIndent()
        val retitle = """
            const v10, $ID_SERIES
            invoke-interface {v$menu, v10}, Landroid/view/Menu;->findItem(I)Landroid/view/MenuItem;
            move-result-object v11
            const-string v10, "Settings"
            invoke-interface {v11, v10}, Landroid/view/MenuItem;->setTitle(Ljava/lang/CharSequence;)Landroid/view/MenuItem;
            const v10, $DRAWABLE_SETTINGS_GEAR
            invoke-interface {v11, v10}, Landroid/view/MenuItem;->setIcon(I)Landroid/view/MenuItem;
        """.trimIndent()
        method.addInstructions(
            getMenuIndex + 3,
            (listOf(ID_HOME, ID_CREX_SOCIAL, ID_VIDEOS).map { hide(it) } + retitle).joinToString("\n")
        )

        // 3. Tapping the Series (now Settings) tab opens the Menu screen instead of switching
        //    tab, and returns false so the bar stays on Matches. Hb has .locals 6 and v0-v5 are
        //    unused at its first instruction.
        HomeTabClickFingerprint.method.addInstructions(
            0,
            """
                invoke-interface {p1}, Landroid/view/MenuItem;->getItemId()I
                move-result v0
                const v1, $ID_SERIES
                if-ne v0, v1, :crex_not_settings
                new-instance v0, Landroid/content/Intent;
                const-class v1, $MORE_ACTIVITY
                invoke-direct {v0, p0, v1}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                invoke-virtual {p0, v0}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
                const/4 v0, 0x0
                return v0
                :crex_not_settings
                nop
            """.trimIndent()
        )

        // 4. ed(String) renames the Series tab after the current series (the "ZIM-WvsIN" label).
        //    Find it by its two resource ids rather than its obfuscatable name, and stop it so
        //    the tab stays "Settings".
        val titleSetter = fingerprint.classDef.methods.firstOrNull { candidate ->
            candidate.returnType == "V" &&
                candidate.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") &&
                candidate.implementation?.instructions?.let { instructions ->
                    val literals = instructions.mapNotNull { (it as? NarrowLiteralInstruction)?.narrowLiteral }
                    literals.contains(SERIES_TITLE_STRING) && literals.contains(ID_SERIES.removePrefix("0x").toInt(16))
                } == true
        } ?: throw PatchException("Series tab title setter (ed) not found")
        titleSetter.returnEarly()
    }
}
