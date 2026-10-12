package app.template.patches.truecaller.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.template.patches.shared.Constants.COMPATIBILITY_TRUECALLER
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val GOLD = "$PREMIUM_TIER_TYPE->GOLD:$PREMIUM_TIER_TYPE"

@Suppress("unused")
val enablePremiumPatch = bytecodePatch(
    name = "Enable Premium",
    description = "Unlocks the Premium interface and the Premium features that are checked on " +
        "the device. Features backed by Truecaller's servers (Who viewed / searched for me data, Identify AI, cloud call recording, fraud " +
        "insurance, the verified badge) are not unlocked.",
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // 1. Stored tier: GOLD instead of the "premiumLevel" pref.
        PremiumTierFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $GOLD
                return-object v0
            """,
        )

        // 2. Stored premium flag: never expired.
        IsPremiumFingerprint.method.returnEarly(true)

        // 3. Every availableFeatures status check reports INCLUDED.
        FeatureStatusIncludedFingerprint.method.returnEarly(true)

        // 4. isFeatureAvailable(feature, refresh). This also skips the server's
        // "disabledPremiumFeatures" kill-switch list, which is read in the same method; forcing
        // only the availability half would need an R8-shaped mid-method edit.
        FeatureAvailableFingerprint.method.returnEarly(true)

        // 5. PremiumState(isPremium, tier, ...): whatever the store or a sync hands in, the state
        // the premium UI observes is premium GOLD. The constructor has no locals, so p1/p2 are
        // low registers; const/16 and sget-object take any register below 256 anyway.
        PremiumStateConstructorFingerprint.method.addInstructions(
            0,
            """
                const/16 p1, 0x1
                sget-object p2, $GOLD
            """,
        )

        // 6. The subscription status worker returns a work Result before building the
        // "subscription expired" notification. Success needs an output Data in this build, so
        // the no-arg Failure is used unless Success gains a no-arg constructor; for this one-shot,
        // push-triggered worker either result only ends the run.
        val success = WorkResultSuccessToStringFingerprint.originalClassDef
        val failure = WorkResultFailureToStringFingerprint.originalClassDef
        if (success.superclass != failure.superclass) {
            throw PatchException("Work Result classes ${success.type} / ${failure.type} do not share a base class")
        }
        val result = success.takeIf { cls ->
            cls.methods.any { it.name == "<init>" && it.parameterTypes.isEmpty() }
        } ?: failure
        if (result.methods.none { it.name == "<init>" && it.parameterTypes.isEmpty() }) {
            throw PatchException("${result.type} has no no-arg constructor")
        }
        SubscriptionStatusWorkerFingerprint.method.apply {
            // doWork already produces this Result type on its own paths; do not guess otherwise.
            val producesResult = implementation!!.instructions.any { instruction ->
                when (val reference = (instruction as? ReferenceInstruction)?.reference) {
                    is TypeReference -> reference.type == result.type
                    is MethodReference -> reference.returnType == result.type
                    else -> false
                }
            }
            if (!producesResult) throw PatchException("$definingClass.doWork no longer returns ${result.type}")
            addInstructions(
                0,
                """
                    new-instance v0, ${result.type}
                    invoke-direct {v0}, ${result.type}-><init>()V
                    return-object v0
                """,
            )
        }
    }
}
