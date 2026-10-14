package app.template.patches.linkedin.navpanel

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.template.patches.linkedin.controls.linkedInControls
import app.template.patches.shared.hevalcontrols.HEVAL_SETTINGS_CLASS
import app.template.patches.shared.hevalcontrols.HevalFeature
import app.template.patches.shared.hevalcontrols.addHevalGate
import app.template.patches.shared.hevalcontrols.featurePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val HIDE_PUZZLE_GAMES = "linkedin.hide_puzzle_games"

private const val TRANSFORMER = "Lcom/linkedin/android/home/navpanel/HomeNavPanelTransformer;"
private const val SECTION_TYPE = "Lcom/linkedin/android/home/navpanel/SectionType;"
private const val SECTION_VIEW_DATA = "Lcom/linkedin/android/home/navpanel/HomeNavPanelSectionViewData;"
private const val GAMES_SECTION_VIEW_DATA = "Lcom/linkedin/android/home/navpanel/HomeNavPanelGamesSectionViewData;"
private const val AGGREGATE_RESPONSE = "Lcom/linkedin/android/home/navpanel/HomeNavPanelAggregateResponse;"

/**
 * Hides the "Puzzle Games" row of the home nav panel (the panel opened from the avatar, top left),
 * at runtime through Heval Controls. Off gives the exact stock panel.
 *
 * 1. HomeNavPanelTransformer.toHeaderViewData(SectionType, String) builds each section header row;
 *    for SectionType.GAMES it answers null while the feature is on. Its callers already skip a
 *    null row (the stock "nothing to show" result), and the Saved posts / Groups rows are untouched.
 * 2. transform(HomeNavPanelAggregateResponse) also adds a HomeNavPanelGamesSectionViewData games
 *    entry for a few impressions when LinkedIn's GAMES_HOME_NAV_ENTRY_POINT lix is on; that add is
 *    skipped while the feature is on.
 *
 * The panel list is rebuilt from the cached nav panel response, so a change applies the next time
 * the panel's data is transformed.
 */
internal val hidePuzzleGamesFeature = HevalFeature(
    id = HIDE_PUZZLE_GAMES,
    name = "Hide Puzzle Games",
    description = "Removes the Puzzle Games entry from the panel that opens from your profile photo.",
    category = "Navigation",
    default = true,
)

@Suppress("unused")
val hidePuzzleGamesPatch = linkedInControls.featurePatch(hidePuzzleGamesFeature) {
    val transformer = mutableClassDefBy(TRANSFORMER)

    // Validate both hook sites before the first edit.
    val header = transformer.methods.singleOrNull {
        it.name == "toHeaderViewData" && it.returnType == SECTION_VIEW_DATA &&
            it.parameterTypes.map(CharSequence::toString) == listOf(SECTION_TYPE, "Ljava/lang/String;") &&
            it.implementation != null
    } ?: throw PatchException("$TRANSFORMER has no concrete toHeaderViewData(SectionType, String)")
    // The gate compares p1 with if-ne, whose operands are 4-bit registers.
    val headerImplementation = header.implementation!!
    if (headerImplementation.registerCount - 3 < 1 || headerImplementation.registerCount > 16) {
        throw PatchException("$TRANSFORMER->toHeaderViewData has an unexpected register count")
    }
    classDefBy(SECTION_TYPE).fields.singleOrNull { it.name == "GAMES" && it.type == SECTION_TYPE }
        ?: throw PatchException("$SECTION_TYPE has no GAMES constant")

    val transform = transformer.methods.singleOrNull {
        it.name == "transform" && it.parameterTypes.map(CharSequence::toString) == listOf(AGGREGATE_RESPONSE) &&
            it.implementation != null
    } ?: throw PatchException("$TRANSFORMER has no concrete transform(HomeNavPanelAggregateResponse)")
    val instructions = transform.instructions.toList()
    val gamesInits = instructions.indices.filter { index ->
        instructions[index].opcode == Opcode.INVOKE_DIRECT &&
            instructions[index].getReference<MethodReference>()?.let { it.definingClass == GAMES_SECTION_VIEW_DATA && it.name == "<init>" } == true
    }
    val gamesInit = gamesInits.singleOrNull()
        ?: throw PatchException("$TRANSFORMER->transform: expected one HomeNavPanelGamesSectionViewData, found ${gamesInits.size}")
    val item = (instructions[gamesInit] as FiveRegisterInstruction).registerC
    val addIndex = gamesInit + 1
    val add = instructions[addIndex] as? FiveRegisterInstruction
    if (instructions[addIndex].opcode != Opcode.INVOKE_VIRTUAL || add == null || add.registerCount != 2 || add.registerD != item ||
        instructions[addIndex].getReference<MethodReference>()?.name != "add"
    ) {
        throw PatchException("$TRANSFORMER->transform: the games entry is not added right after it is built")
    }
    val list = add.registerC
    val scratch = transform.findFreeRegister(addIndex, list, item)
    if (scratch > 255) throw PatchException("$TRANSFORMER->transform: no free register below v256 for the games entry gate")

    // 1. Header rows: no GAMES row while the feature is on.
    header.addHevalGate(HIDE_PUZZLE_GAMES) { v ->
        """
            sget-object $v, $SECTION_TYPE->GAMES:$SECTION_TYPE
            if-ne p1, $v, :heval_stock
            const/4 $v, 0x0
            return-object $v
        """
    }

    // 2. The lix games entry: skip its list add while the feature is on.
    transform.addInstructionsWithLabels(
        addIndex,
        """
            const-string v$scratch, "$HIDE_PUZZLE_GAMES"
            invoke-static/range { v$scratch .. v$scratch }, $HEVAL_SETTINGS_CLASS->isEnabled(Ljava/lang/String;)Z
            move-result v$scratch
            if-nez v$scratch, :heval_skip_games_entry
        """,
        ExternalLabel("heval_skip_games_entry", transform.getInstruction(addIndex + 1)),
    )
}
