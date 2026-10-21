package app.template.patches.crex.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_CREX
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MATCHES_FRAGMENT = "Lin/cricketexchange/app/cricketexchange/matches/MatchesFragment;"
// @id/iv_hamburger (res/values/public.xml, v26.10.07): the top-right three-line button.
private const val ID_HAMBURGER = 0x7f0a0d8a

private fun Method.literals() =
    implementation?.instructions?.mapNotNull { (it as? NarrowLiteralInstruction)?.narrowLiteral }.orEmpty()

@Suppress("unused")
val hideMenuButtonPatch = bytecodePatch(
    name = "Hide Matches menu button",
    description = "Hides the top-right three-line menu button on the Matches screen. Use with " +
            "Matches and Settings navigation, which keeps the same Menu screen under Settings."
) {
    compatibleWith(COMPATIBILITY_CREX)

    execute {
        val fragment = mutableClassDefBy(MATCHES_FRAGMENT)

        // 1. Layout wiring: w3(View) looks the button up last and sets its click listener, with
        //    the button left in p1. Hide it right before the method returns (v1 is free there).
        val wiring = fragment.methods.singleOrNull { method ->
            method.returnType == "V" &&
                method.parameterTypes.map { it.toString() } == listOf("Landroid/view/View;") &&
                method.literals().contains(ID_HAMBURGER)
        } ?: throw PatchException("Matches toolbar wiring method not found")
        if (wiring.literals().last() != ID_HAMBURGER) {
            throw PatchException("Menu button is no longer the last view the wiring method looks up")
        }
        val last = wiring.implementation!!.instructions.count() - 1
        if (wiring.getInstruction(last).opcode != Opcode.RETURN_VOID) {
            throw PatchException("Matches toolbar wiring method does not end in return-void")
        }
        wiring.addInstructions(
            last,
            """
                const/16 v1, 0x8
                invoke-virtual {p1, v1}, Landroid/view/View;->setVisibility(I)V
            """.trimIndent()
        )

        // 2. onResume shows the button again for signed-out users:
        //    `v4.setVisibility(v0)` (v0 = 0). Add a hide straight after it; v6 holds 8 there
        //    (the next call is `v3.setVisibility(v6)`, checked below).
        val onResume = fragment.methods.singleOrNull {
            it.name == "onResume" && it.returnType == "V" && it.parameterTypes.isEmpty()
        } ?: throw PatchException("MatchesFragment.onResume not found")
        val instructions = onResume.implementation!!.instructions.toList()
        val show = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                ((instruction as ReferenceInstruction).reference as? MethodReference)?.let {
                    it.definingClass == "Landroid/widget/ImageView;" && it.name == "setVisibility"
                } == true &&
                (instruction as FiveRegisterInstruction).let { it.registerC == 4 && it.registerD == 0 }
        }
        if (show < 0) throw PatchException("Menu button visibility call in onResume not found")
        val next = instructions[show + 1] as? FiveRegisterInstruction
        if (next == null || next.registerD != 6) {
            throw PatchException("Unexpected code after the menu button visibility call")
        }
        onResume.addInstructions(
            show + 1,
            "invoke-virtual {v4, v6}, Landroid/widget/ImageView;->setVisibility(I)V"
        )
    }
}
