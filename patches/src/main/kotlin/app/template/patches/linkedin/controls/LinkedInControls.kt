package app.template.patches.linkedin.controls

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.getReference
import app.template.patches.shared.Constants.COMPATIBILITY_LINKEDIN
import app.template.patches.shared.hevalcontrols.hevalControlsPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val ENTRY = "Lapp/template/extension/linkedin/HevalControlsEntry;"
private const val ACCOUNT_SECTION_TYPE = "Lcom/linkedin/android/home/navpanel/AccountSectionType;"
private const val ACCOUNT_PRESENTER = "Lcom/linkedin/android/home/navpanel/presenter/HomeNavPanelAccountSectionPresenter;"
private const val OBSERVABLE_LIST = "Lcom/linkedin/android/infra/list/DefaultObservableList;"
private const val RESOURCE_KT = "Lcom/linkedin/android/architecture/data/ResourceKt;"
private const val TRACKER = "Lcom/linkedin/android/litrackinglib/metric/Tracker;"
private const val TRACKING_LISTENER = "Lcom/linkedin/android/tracking/v2/listeners/TrackingOnClickListener;"
private const val TRACKING_BUILDER = "Lcom/linkedin/android/tracking/v2/event/CustomTrackingEventBuilder;"
private const val VIEW_DATA = "Lcom/linkedin/android/architecture/viewdata/ViewData;"
private const val OBJECT = "Ljava/lang/Object;"

/**
 * The in-app entry point: a "Heval Controls" row at the end of the home nav panel's footer (the
 * panel opened from the avatar, top left), below "Settings & Privacy", drawn by the stock footer
 * presenter. It is part of the "Heval Controls" patch, so the screen can always be reached.
 *
 * 1. The footer list builder appends a HomeNavPanelAccountSectionViewData(SETTINGS, "Heval
 *    Controls") row once its stock rows are in the list.
 * 2. HomeNavPanelAccountSectionPresenter.attachViewData gives that row (and only it) a listener
 *    that opens HevalControlsActivity without LinkedIn's click tracking; stock rows are untouched.
 */
private val linkedInEntryPointPatch = bytecodePatch(
    description = "Adds a \"Heval Controls\" row to the LinkedIn home nav panel.",
) {
    compatibleWith(COMPATIBILITY_LINKEDIN)

    extendWith("extensions/linkedin.mpe")

    execute {
        // Validate every LinkedIn type and member the extension links against before any edit.
        classDefBy(ENTRY)
        requireMember(ACCOUNT_SECTION_TYPE, "SETTINGS field") { it.fields.any { f -> f.name == "SETTINGS" && f.type == ACCOUNT_SECTION_TYPE } }
        requireMember(NAV_PANEL_ACCOUNT_VIEW_DATA, "(AccountSectionType, String) constructor and premiumText") { classDef ->
            classDef.methods.any { it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) == listOf(ACCOUNT_SECTION_TYPE, "Ljava/lang/String;") } &&
                classDef.fields.any { it.name == "premiumText" && it.type == "Ljava/lang/CharSequence;" }
        }
        requireMember(OBSERVABLE_LIST, "addItem(int, Object) and currentSize()") { classDef ->
            classDef.methods.any { it.name == "addItem" && it.parameterTypes.map(CharSequence::toString) == listOf("I", OBJECT) } &&
                classDef.methods.any { it.name == "currentSize" && it.parameterTypes.isEmpty() && it.returnType == "I" }
        }
        requireMember(TRACKING_LISTENER, "an open class with a (Tracker, String, String, builders) constructor") { classDef ->
            !AccessFlags.FINAL.isSet(classDef.accessFlags) &&
                classDef.methods.any {
                    it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) ==
                        listOf(TRACKER, "Ljava/lang/String;", "Ljava/lang/String;", "[$TRACKING_BUILDER")
                } &&
                classDef.methods.none { it.name == "onClick" && AccessFlags.FINAL.isSet(it.accessFlags) }
        }

        // 1. The footer list: after `ResourceKt.map(resource, list)` wraps it, before the lambda
        //    returns, append the row. (The map call itself is a branch target, so nothing is
        //    inserted before it; the list is not observed until the wrapped Resource is returned.)
        val footer = NavPanelFooterListFingerprint.method
        val instructions = footer.instructions.toList()
        val firstRow = instructions.indexOfFirst {
            it.opcode == Opcode.INVOKE_DIRECT && it.getReference<MethodReference>()?.let { call ->
                call.definingClass == NAV_PANEL_ACCOUNT_VIEW_DATA && call.name == "<init>"
            } == true
        }
        val mapIndex = (firstRow + 1 until instructions.size).firstOrNull { index ->
            val instruction = instructions[index]
            instruction.opcode in setOf(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE) &&
                instruction.getReference<MethodReference>()?.let { it.definingClass == RESOURCE_KT && it.name == "map" } == true
        } ?: throw PatchException("${footer.definingClass}->${footer.name}: no ResourceKt.map after the footer row")
        val listRegister = argumentRegisters(instructions[mapIndex]).getOrNull(1)
            ?: throw PatchException("${footer.definingClass}->${footer.name}: unexpected ResourceKt.map call shape")
        val listFilled = (firstRow until mapIndex).any { index ->
            val instruction = instructions[index]
            instruction.getReference<MethodReference>()?.let { it.definingClass == OBSERVABLE_LIST && it.name == "addAll" } == true &&
                argumentRegisters(instruction).firstOrNull() == listRegister
        }
        if (!listFilled) throw PatchException("${footer.definingClass}->${footer.name}: v$listRegister is not the footer list")
        val insertAt = mapIndex + 2
        if (footer.getInstruction(mapIndex + 1).opcode != Opcode.MOVE_RESULT_OBJECT ||
            (footer.getInstruction(insertAt) as BuilderInstruction).location.labels.isNotEmpty()
        ) {
            throw PatchException("${footer.definingClass}->${footer.name}: unexpected code after ResourceKt.map")
        }
        if ((footer.getInstruction(mapIndex + 1) as OneRegisterInstruction).registerA == listRegister) {
            throw PatchException("${footer.definingClass}->${footer.name}: ResourceKt.map overwrites the footer list register")
        }

        // 2. The footer presenter's click listener, chosen per row in attachViewData(ViewData).
        val attach = mutableClassDefBy(ACCOUNT_PRESENTER).methods.singleOrNull {
            it.name == "attachViewData" && it.returnType == "V" &&
                it.parameterTypes.map(CharSequence::toString) == listOf(VIEW_DATA) && it.implementation != null
        } ?: throw PatchException("$ACCOUNT_PRESENTER has no concrete attachViewData(ViewData)")
        val presenterFields = classDefBy(ACCOUNT_PRESENTER).fields
        if (presenterFields.none { it.name == "tracker" && it.type == TRACKER } ||
            presenterFields.none { it.name == "accountSectionButtonClickListener" && it.type == TRACKING_LISTENER }
        ) {
            throw PatchException("$ACCOUNT_PRESENTER lacks tracker / accountSectionButtonClickListener")
        }
        val attachImplementation = attach.implementation!!
        // v0 is free at index 0; p0 and p1 must fit the 4-bit iget/iput operands.
        if (attachImplementation.registerCount < 3 || attachImplementation.registerCount > 16) {
            throw PatchException("$ACCOUNT_PRESENTER->attachViewData has an unexpected register count")
        }

        footer.addInstruction(insertAt, "invoke-static/range { v$listRegister .. v$listRegister }, $ENTRY->addNavPanelRow($OBJECT)V")
        attach.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p0, $ACCOUNT_PRESENTER->tracker:$TRACKER
                invoke-static { v0, p1 }, $ENTRY->navPanelClickListener($TRACKER$OBJECT)$TRACKING_LISTENER
                move-result-object v0
                if-eqz v0, :heval_stock_row
                iput-object v0, p0, $ACCOUNT_PRESENTER->accountSectionButtonClickListener:$TRACKING_LISTENER
                return-void
            """,
            ExternalLabel("heval_stock_row", attach.getInstruction(0)),
        )
    }
}

internal val linkedInControls = hevalControlsPatch(
    COMPATIBILITY_LINKEDIN,
    dependencies = setOf(linkedInEntryPointPatch),
)

// Exposes the host patch, so Manager lists "Heval Controls".
@Suppress("unused")
val linkedInHevalControlsPatch = linkedInControls.patch

private fun BytecodePatchContext.requireMember(
    type: String,
    what: String,
    check: (com.android.tools.smali.dexlib2.iface.ClassDef) -> Boolean,
) {
    if (!check(classDefBy(type))) throw PatchException("$type: $what not found")
}

/** The argument registers of an invoke, in order (35c or /range). */
private fun argumentRegisters(instruction: com.android.tools.smali.dexlib2.iface.instruction.Instruction): List<Int> =
    when (instruction) {
        is RegisterRangeInstruction -> (0 until instruction.registerCount).map { instruction.startRegister + it }
        is FiveRegisterInstruction -> listOf(
            instruction.registerC, instruction.registerD, instruction.registerE, instruction.registerF, instruction.registerG,
        ).take(instruction.registerCount)
        else -> emptyList()
    }
