package app.template.patches.crex.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_CREX
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val matchesOnlyNavigationPatch = bytecodePatch(
    name = "Matches only (hide bottom navigation)",
    description = "UI only. Hides the whole bottom navigation bar so the app stays on Matches, " +
            "with the Live, For you, Upcoming and Finished tabs at the top (swipe or tap). The " +
            "Menu screen is reached through the top-right three-line button. The bar's items " +
            "are hidden, not removed, because HomeActivity looks them up by index."
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
        //    method, so the insert below cannot shift its index.
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

        // 2. Hide the bar. getMenu() is `invoke-virtual {bar}`, `move-result-object vM`, then the
        //    Menu is stored in a field. Right before the call, vM is free (the very next
        //    instruction overwrites it), so it carries the GONE constant. It must differ from the
        //    bar's register and fit in 4 bits for invoke-virtual; both are checked.
        val getMenu = method.getInstruction(getMenuIndex)
        val moveResult = method.getInstruction(getMenuIndex + 1)
        if (getMenu !is FiveRegisterInstruction || moveResult.opcode != Opcode.MOVE_RESULT_OBJECT) {
            throw PatchException("Expected getMenu() followed by move-result-object")
        }
        val bar = getMenu.registerC
        val temp = (moveResult as OneRegisterInstruction).registerA
        if (temp == bar || temp > 15 || bar > 15) {
            throw PatchException("No free register to hide the bottom bar (bar=v$bar, result=v$temp)")
        }
        method.addInstructions(
            getMenuIndex,
            """
                const/16 v$temp, 0x8
                invoke-virtual {v$bar, v$temp}, Landroid/view/View;->setVisibility(I)V
            """.trimIndent()
        )
    }
}
