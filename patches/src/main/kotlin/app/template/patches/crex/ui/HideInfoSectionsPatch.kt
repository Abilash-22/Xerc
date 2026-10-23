package app.template.patches.crex.ui

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_CREX
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val MATCH_INFO_DATA = "Lin/cricketexchange/app/cricketexchange/matchinfo/MatchInfoData;"
private const val LIVE_MATCH_ACTIVITY_TYPE = "Lin/cricketexchange/app/cricketexchange/activities/LiveMatchActivity;"

// Title string of each Info tab section (res/values/public.xml, v26.10.07). The section's data
// field is the nearest `iget-object` + `if-eqz` before the title, in MatchInfoData.r().
private val INFO_SECTION_TITLES = linkedMapOf(
    "Series Stats" to 0x7f14081d,              // series_stats (the "Series Stats (T20I)" header, link and card)
    "Team Comparison" to 0x7f1408be,           // header above the Overall / On Venue comparison table
    "Avg Scores on Venue" to 0x7f1400cd,       // over-by-over 1st / 2nd innings table
    "Recent Matches on Venue" to 0x7f140770,   // recent matches list with the All Matches link
)

@Suppress("unused")
val hideInfoSectionsPatch = bytecodePatch(
    name = "Hide Info sections",
    description = "Phase 1.1: removes the Series Stats, Team Comparison, Avg Scores on Venue and " +
            "Recent Matches on Venue sections from the match screen's Info tab, using the app's " +
            "own state for matches that have no data for them. Venue Stats, weather, squads, " +
            "umpires and the other sections are not touched."
) {
    compatibleWith(COMPATIBILITY_CREX)

    execute {
        // MatchInfoData.r(LiveMatchActivity) builds the Info tab's section list. Every section
        // starts with `iget-object v3, this, <data field>` / `if-eqz v3, :skip` (the label is on
        // the iget) and then adds a header titled with its string resource, followed by the data.
        // Making the register null before the check takes the app's existing "no data" branch.
        val builder = mutableClassDefBy(MATCH_INFO_DATA).methods.singleOrNull { method ->
            method.returnType == "Ljava/util/ArrayList;" &&
                method.parameterTypes.map { it.toString() } == listOf(LIVE_MATCH_ACTIVITY_TYPE)
        } ?: throw PatchException("Info tab section list builder not found")

        val instructions = builder.implementation!!.instructions.toList()

        val inserts = INFO_SECTION_TITLES.map { (section, titleId) ->
            val title = instructions.indexOfFirst { (it as? NarrowLiteralInstruction)?.narrowLiteral == titleId }
            if (title < 0) throw PatchException("$section header title not found")

            var check = -1
            for (i in title - 1 downTo 0) {
                val instruction = instructions[i]
                if (instruction.opcode != Opcode.IGET_OBJECT) continue
                val field = (instruction as ReferenceInstruction).reference as FieldReference
                if (field.definingClass == MATCH_INFO_DATA && instructions[i + 1].opcode == Opcode.IF_EQZ) {
                    check = i
                    break
                }
            }
            if (check < 0) throw PatchException("$section null check not found")
            check + 1 to (instructions[check] as OneRegisterInstruction).registerA
        }

        // Inserted before each if-eqz (not a branch target). Highest index first so the
        // earlier insertions do not shift the later ones.
        inserts.sortedByDescending { it.first }.forEach { (index, register) ->
            builder.addInstructions(index, "const/4 v$register, 0x0")
        }
    }
}
