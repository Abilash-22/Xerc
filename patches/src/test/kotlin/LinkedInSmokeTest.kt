import app.template.patches.linkedin.navpanel.HIDE_PUZZLE_GAMES
import app.template.patches.linkedin.navpanel.hidePuzzleGamesFeature
import app.template.patches.shared.hevalcontrols.HEVAL_SETTINGS_CLASS
import app.template.patches.shared.hevalcontrols.HevalRegistryCodec
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val PKG = "com.linkedin.android"
private const val HEVAL = "Lapp/template/extension/hevalcontrols/"
private const val ENTRY = "Lapp/template/extension/linkedin/HevalControlsEntry;"
private const val ACCOUNT_VIEW_DATA = "Lcom/linkedin/android/home/navpanel/HomeNavPanelAccountSectionViewData;"
private const val ACCOUNT_PRESENTER = "Lcom/linkedin/android/home/navpanel/presenter/HomeNavPanelAccountSectionPresenter;"
private const val NAV_TRANSFORMER = "Lcom/linkedin/android/home/navpanel/HomeNavPanelTransformer;"
private const val SECTION_TYPE = "Lcom/linkedin/android/home/navpanel/SectionType;"
private const val GAMES_SECTION_VIEW_DATA = "Lcom/linkedin/android/home/navpanel/HomeNavPanelGamesSectionViewData;"
private const val FOOTER_SLOT_URN = "urn:li:fsd_premiumUpsellSlot:COMMUNITY_PANEL_FOR_PAGES"

class LinkedInSmokeTest {

    @TempDir
    lateinit var workDir: File

    private fun Instruction.methodRef() = (this as? ReferenceInstruction)?.reference as? MethodReference

    private fun Method.calls(definingClass: String, name: String) = instructions().any {
        it.methodRef()?.let { call -> call.definingClass == definingClass && call.name == name } == true
    }

    /** `const-string vX, "<id>"; invoke-static/range HevalSettings.isEnabled; move-result; <branch>` at [start]. */
    private fun assertGate(method: Method, featureId: String, label: String, start: Int = 0, branch: Opcode = Opcode.IF_EQZ) {
        val insns = method.instructions()
        val id = ((insns.getOrNull(start) as? ReferenceInstruction)?.reference as? StringReference)?.string
        assertEquals(featureId, id, "$label does not start with the $featureId gate")
        val call = insns.getOrNull(start + 1)?.methodRef()
        assertEquals("$HEVAL_SETTINGS_CLASS->isEnabled", "${call?.definingClass}->${call?.name}", "$label: no isEnabled call")
        assertEquals(listOf(Opcode.MOVE_RESULT, branch), insns.subList(start + 2, start + 4).map { it.opcode }, "$label gate shape")
    }

    @Test
    fun `LinkedIn Heval Controls host, entry point and Hide Puzzle Games are patched`() {
        val apk = File(repoRoot(), "apks/linkedin/base.apk")
        // The APK lives in the gitignored apks/ directory. Skip rather than fail when it is absent.
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            version = "4.1.1261",
            patchNames = setOf("Heval Controls", "Hide Puzzle Games"),
            allPatches = loadAllPatches(newestPatchBundle(repoRoot())),
        )
        val byType = classes.associateBy { it.type }

        // Host: both extensions are merged.
        listOf(
            "${HEVAL}HevalSettings;", "${HEVAL}Registry;", "${HEVAL}InitProvider;", "${HEVAL}CrashGuard;",
            "${HEVAL}ui/HevalControlsActivity;", ENTRY,
        ).forEach { assertTrue(it in byType, "$it is not in the patched dex") }

        // Registry: the expected features, the bundle and the patched app version.
        val registry = byType.getValue("${HEVAL}Registry;")
        assertEquals(HevalRegistryCodec.encode(listOf(hidePuzzleGamesFeature)), registry.constString("encoded"))
        assertEquals("4.1.1261 (216900)", registry.constString("patchedAppVersion"))
        // "unknown" here: the test JVM loads the host from the classpath, not from the .mpp (the CLI writes the real version).
        assertTrue(registry.constString("bundleVersion").isNotBlank(), "bundle version not written")

        // Entry point 1: the nav panel footer list appends the Heval Controls row after ResourceKt.map.
        val footer = classes.flatMap { it.methods }.single { method ->
            method.instructions().any { (it as? ReferenceInstruction)?.reference.let { r -> r is StringReference && r.string == FOOTER_SLOT_URN } } &&
                method.calls(ACCOUNT_VIEW_DATA, "<init>")
        }
        val footerInsns = footer.instructions()
        val addRow = footerInsns.indexOfFirst { it.methodRef()?.let { c -> c.definingClass == ENTRY && c.name == "addNavPanelRow" } == true }
        assertTrue(addRow > 0, "footer list builder does not call addNavPanelRow")
        assertEquals(Opcode.MOVE_RESULT_OBJECT, footerInsns[addRow - 1].opcode, "addNavPanelRow is not right after ResourceKt.map's result")
        assertEquals("map", footerInsns[addRow - 2].methodRef()?.name, "addNavPanelRow does not follow ResourceKt.map")
        assertEquals(Opcode.RETURN_OBJECT, footerInsns[addRow + 1].opcode, "addNavPanelRow is not just before the return")

        // Entry point 2: attachViewData asks the extension for the row's listener first.
        val attach = byType.getValue(ACCOUNT_PRESENTER).method("attachViewData")
        val attachInsns = attach.instructions()
        assertEquals(Opcode.IGET_OBJECT, attachInsns[0].opcode)
        assertEquals(ENTRY, attachInsns[1].methodRef()?.definingClass, "attachViewData does not start with the entry hook")
        assertEquals("navPanelClickListener", attachInsns[1].methodRef()?.name)
        assertEquals(listOf(Opcode.MOVE_RESULT_OBJECT, Opcode.IF_EQZ, Opcode.IPUT_OBJECT, Opcode.RETURN_VOID), attachInsns.subList(2, 6).map { it.opcode })
        assertEquals("accountSectionButtonClickListener", ((attachInsns[4] as ReferenceInstruction).reference as FieldReference).name)

        // Feature "Hide Puzzle Games" 1: toHeaderViewData answers null for SectionType.GAMES while it is on.
        val transformer = byType.getValue(NAV_TRANSFORMER)
        val header = transformer.method("toHeaderViewData")
        assertGate(header, HIDE_PUZZLE_GAMES, "toHeaderViewData")
        val headerInsns = header.instructions()
        assertEquals("GAMES", ((headerInsns[4] as ReferenceInstruction).reference as FieldReference).name, "the gate does not load SectionType.GAMES")
        assertEquals(SECTION_TYPE, ((headerInsns[4] as ReferenceInstruction).reference as FieldReference).definingClass)
        assertEquals(listOf(Opcode.IF_NE, Opcode.CONST_4, Opcode.RETURN_OBJECT), headerInsns.subList(5, 8).map { it.opcode })
        assertEquals(Opcode.INVOKE_VIRTUAL, headerInsns[8].opcode, "the stock body (ordinal()) does not follow the gate")

        // Feature 2: the lix games entry's list add is skipped while it is on.
        val transform = transformer.methods.single { it.name == "transform" && it.parameterTypes.map(CharSequence::toString) == listOf("Lcom/linkedin/android/home/navpanel/HomeNavPanelAggregateResponse;") && it.implementation != null }
        val transformInsns = transform.instructions()
        val gamesInit = transformInsns.indexOfFirst { it.methodRef()?.let { c -> c.definingClass == GAMES_SECTION_VIEW_DATA && c.name == "<init>" } == true }
        assertTrue(gamesInit > 0, "no HomeNavPanelGamesSectionViewData in transform")
        assertGate(transform, HIDE_PUZZLE_GAMES, "transform games entry", start = gamesInit + 1, branch = Opcode.IF_NEZ)
        assertEquals(Opcode.IF_NEZ, transformInsns[gamesInit + 4].opcode, "the games entry gate does not skip on enabled")
        val add = transformInsns[gamesInit + 5]
        assertEquals("add", add.methodRef()?.name, "the games entry add does not follow the gate")
        assertEquals((transformInsns[gamesInit] as FiveRegisterInstruction).registerC, (add as FiveRegisterInstruction).registerD)

        // Manifest: non-exported provider (initOrder 1000) and activity, no launcher alias.
        val manifestFile = workDir.walkTopDown()
            .firstOrNull { it.name == "AndroidManifest.xml" && it.readText().contains("hevalcontrols") }
            ?: error("no decoded AndroidManifest.xml with Heval Controls under $workDir")
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile).documentElement
        fun elements(tag: String) = manifest.getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }
        val provider = elements("provider").single { it.getAttribute("android:name") == "app.template.extension.hevalcontrols.InitProvider" }
        assertEquals("$PKG.hevalcontrols", provider.getAttribute("android:authorities"))
        assertEquals("false", provider.getAttribute("android:exported"))
        assertEquals("1000", provider.getAttribute("android:initOrder"))
        val activity = elements("activity").single { it.getAttribute("android:name") == "app.template.extension.hevalcontrols.ui.HevalControlsActivity" }
        assertEquals("false", activity.getAttribute("android:exported"))
        assertTrue(elements("activity-alias").none { it.getAttribute("android:targetActivity").contains("hevalcontrols") })
    }

    /** The string a rewritten Registry getter returns. */
    private fun ClassDef.constString(name: String): String {
        val first = methods.single { it.name == name }.instructions().first()
        assertTrue(first.opcode == Opcode.CONST_STRING || first.opcode == Opcode.CONST_STRING_JUMBO, "$name was not rewritten")
        return ((first as ReferenceInstruction).reference as StringReference).string
    }
}
