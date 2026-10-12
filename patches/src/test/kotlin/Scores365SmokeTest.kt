import app.template.patches.shared.misc.gms.gmsContentUriReplacement
import app.template.patches.shared.misc.gms.gmsStringReplacements
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.w3c.dom.Element
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory

private const val PKG = "com.scores365"
private const val ENTITLEMENT_STATE = "Lcom/scores365/billingClient/entitlement/EntitlementState;"
private const val BASE_ACTIVITY = "Lcom/scores365/Design/Activities/BaseActivity;"
private const val BASE_ACTION_BAR_ACTIVITY = "Lcom/scores365/Design/Activities/BaseActionBarActivity;"
private const val SPLASH = "Lcom/scores365/ui/Splash;"
private const val GMS_EXTENSION = "Lapp/template/extension/gmscore/GmsCoreSupport;"
private const val SIGNATURE_SHA1 = "47f994bd2eac89f4aeea0aa1d95cb3ba3ec3f1a1"
private const val SIGNATURE_EXTENSION = "Lapp/template/extension/signature/SignatureSpoof;"

class Scores365SmokeTest {

    @TempDir
    lateinit var workDir: File

    private fun Instruction.stringOrNull(): String? =
        ((this as? ReferenceInstruction)?.reference as? StringReference)?.string

    // Registers of an invoke with exactly one argument register, else null.
    private fun Instruction.singleRegister(): Int? = when (this) {
        is RegisterRangeInstruction -> startRegister.takeIf { registerCount == 1 }
        is FiveRegisterInstruction -> registerC.takeIf { registerCount == 1 }
        else -> null
    }

    @Test
    fun `365Scores patches unlock premium, remove ads and update prompts, and route GMS to MicroG RE`() {
        val root = repoRoot()
        val apk = File(root, "apks/365scores/base.apk")

        // The APK lives in the gitignored apks/ directory. Skip rather than fail when it is
        // absent, so CI stays green; locally it is present and the test really runs.
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            version = "14.9.6",
            patchNames = setOf(
                "Disable ads", "Enable Premium", "Disable update prompts", "GmsCore support", "Spoof signature",
            ),
            allPatches = loadAllPatches(newestPatchBundle(root)),
        )
        val byType = classes.associateBy { it.type }

        assertDisableAds(classes, byType)
        assertEnablePremium(byType)
        assertDisableUpdatePrompts(byType)
        assertGmsCoreSupport(classes, byType)
        assertSpoofSignature(classes, byType)
    }

    private fun assertDisableAds(classes: List<ClassDef>, byType: Map<String, ClassDef>) {
        val mobileAds = byType["Lcom/google/android/gms/ads/MobileAds;"]
            ?: error("MobileAds not found in emitted dexes")
        assertReturnsEarlyVoid(
            mobileAds.method("initialize", listOf("Landroid/content/Context;")),
            label = "MobileAds.initialize(Context)",
        )
        assertReturnsEarlyVoid(
            mobileAds.method(
                "initialize",
                listOf(
                    "Landroid/content/Context;",
                    "Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;",
                ),
            ),
            label = "MobileAds.initialize(Context, Listener)",
        )

        // Every concrete app ad gate returns false, including the readable base activities.
        val gates = classes.flatMap { cls ->
            cls.methods.filter {
                it.name in setOf("showAdsForContext", "isBannerNeedToBeShown") &&
                    it.returnType == "Z" && it.parameterTypes.isEmpty() && it.implementation != null
            }.map { cls to it }
        }
        for (type in listOf(
            BASE_ACTION_BAR_ACTIVITY,
            "Lcom/scores365/Design/Activities/BottomNavigationActivity;",
            "Lcom/scores365/gameCenter/GameCenterBaseActivity;",
            "Lcom/scores365/ui/WebViewActivity;",
        )) {
            check(gates.any { (cls, _) -> cls.type == type }) { "no app ad gate patched in $type" }
        }
        for ((cls, method) in gates) {
            assertForcedBoolean(method, expected = false, label = "${cls.type}.${method.name}()")
        }
    }

    private fun assertEnablePremium(byType: Map<String, ClassDef>) {
        // The constructor overwrites the five feature flags (p1..p5) with true before storing them.
        val state = byType[ENTITLEMENT_STATE] ?: error("$ENTITLEMENT_STATE not found")
        val constructor = state.method(
            "<init>",
            listOf("Z", "Z", "Z", "Z", "Z", "Ljava/util/Set;", "Ljava/util/List;", "J", "Z"),
        )
        // p0 = this; parameters take 1+1+1+1+1+1+1+2+1 = 10 registers.
        val p1 = constructor.implementation!!.registerCount - 10
        val insns = constructor.instructions()
        for (slot in 0 until 5) {
            val insn = insns.getOrNull(slot)
            check(
                insn?.opcode == Opcode.CONST_16 &&
                    (insn as OneRegisterInstruction).registerA == p1 + slot &&
                    (insn as WideLiteralInstruction).wideLiteral == 1L
            ) {
                "EntitlementState.<init> instruction $slot is not const/16 p${slot + 1}, 0x1; got ${insn?.opcode}"
            }
        }

        // The Gson-restored cache skips the constructor, so the getters are forced too.
        for (getter in listOf(
            "isPremium", "getAdsFree", "getTipster", "getPlus", "getDelayedNotifications", "getNotificationSounds",
        )) {
            assertForcedBoolean(state.method(getter, emptyList()), expected = true, label = "EntitlementState.$getter()")
        }

        // The Remove ads screen no longer sees the grace start time the forced state triggers.
        val removeAds = byType["Lcom/scores365/removeAds/RemoveAdsBasicActivity;"]
            ?: error("RemoveAdsBasicActivity not found")
        val strings = removeAds.method("onCreate", listOf("Landroid/os/Bundle;")).instructions().mapNotNull { it.stringOrNull() }
        check("iabc_fnrts" !in strings) { "RemoveAdsBasicActivity.onCreate still reads \"iabc_fnrts\"" }
        check("morphe_disabled_iabc_fnrts" in strings) { "RemoveAdsBasicActivity.onCreate is missing the renamed key" }
    }

    private fun assertDisableUpdatePrompts(byType: Map<String, ClassDef>) {
        for (type in listOf(BASE_ACTIVITY, BASE_ACTION_BAR_ACTIVITY)) {
            val onCreate = (byType[type] ?: error("$type not found")).method("onCreate", listOf("Landroid/os/Bundle;"))
            val strings = onCreate.instructions().mapNotNull { it.stringOrNull() }
            for (key in listOf("ForceUpdateDialog", "NewVersionPopup")) {
                check(key !in strings) { "$type.onCreate still reads the \"$key\" extra" }
                check("morphe_disabled_$key" in strings) { "$type.onCreate is missing the renamed \"$key\" key" }
            }
        }
    }

    private fun assertGmsCoreSupport(classes: List<ClassDef>, byType: Map<String, ClassDef>) {
        // The extension is merged, and Splash.onCreate calls it first with p0 (the activity).
        val extension = byType[GMS_EXTENSION]
            ?: error("$GMS_EXTENSION (gmscore extension) not merged into the emitted dexes")
        val check = extension.method("checkGmsCore", listOf("Landroid/app/Activity;"))
        check(AccessFlags.STATIC.isSet(check.accessFlags) && check.implementation != null) {
            "$GMS_EXTENSION.checkGmsCore(Activity) is not a static method with code"
        }
        val onCreate = (byType[SPLASH] ?: error("$SPLASH not found")).method("onCreate", listOf("Landroid/os/Bundle;"))
        val first = onCreate.instructions().firstOrNull()
        val call = (first as? ReferenceInstruction)?.reference as? MethodReference
        check(
            (first?.opcode == Opcode.INVOKE_STATIC_RANGE || first?.opcode == Opcode.INVOKE_STATIC) &&
                call?.definingClass == GMS_EXTENSION && call.name == "checkGmsCore" &&
                first.singleRegister() == onCreate.implementation!!.registerCount - 2 // this + Bundle
        ) { "Splash.onCreate does not start with checkGmsCore(p0); got ${first?.opcode} $call" }

        // isGooglePlayServicesAvailable (here R8-reordered to (int, Context)) reports SUCCESS.
        val availability = classes.flatMap { cls -> cls.methods.map { cls to it } }.filter { (_, m) ->
            m.returnType == "I" && m.parameterTypes.size == 2 &&
                m.instructions().mapNotNull { it.stringOrNull() }.containsAll(listOf("MetadataValueReader", "This should never happen."))
        }
        check(availability.size == 1) { "expected one isGooglePlayServicesAvailable, found ${availability.map { (c, m) -> "${c.type}.${m.name}" }}" }
        val (availabilityClass, availabilityMethod) = availability.single()
        assertReturnsInt(availabilityMethod, 0, "${availabilityClass.type}.${availabilityMethod.name} (isGooglePlayServicesAvailable)")

        // No GMS package / action / authority constant is left, and the ad SDK is untouched.
        val replacements = gmsStringReplacements()
        val leftovers = classes.flatMap { cls ->
            cls.methods.flatMap { m ->
                m.instructions().mapNotNull { it.stringOrNull() }
                    .filter { it in replacements || gmsContentUriReplacement(it) != null }
                    .map { "${cls.type}.${m.name}: $it" }
            }
        }
        check(leftovers.isEmpty()) { "GMS strings were not rewritten:\n${leftovers.joinToString("\n")}" }
        val strings = classes.flatMap { cls -> cls.methods.flatMap { m -> m.instructions().mapNotNull { it.stringOrNull() } } }
        check("app.revanced.android.gms" in strings) { "no \"com.google.android.gms\" constant points at MicroG RE" }
        check(byType.keys.any { it.startsWith("Lcom/google/android/gms/ads/") }) { "com.google.android.gms.ads classes disappeared" }

        // Manifest: MicroG RE spoofing meta-data, <queries>, c2dm renames and the unchanged package.
        val manifestFile = workDir.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && it.readText().contains("SPOOFED_PACKAGE_NAME") }
            .firstOrNull() ?: error("no decoded AndroidManifest.xml with the MicroG meta-data under $workDir")
        val manifestText = manifestFile.readText()
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile).documentElement
        check(manifest.getAttribute("package") == PKG) { "package name changed to ${manifest.getAttribute("package")}" }

        fun Element.children(tag: String) =
            getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }
        val metaData = manifest.children("meta-data").associate { it.getAttribute("android:name") to it.getAttribute("android:value") }
        for ((name, value) in mapOf(
            "app.revanced.android.gms.SPOOFED_PACKAGE_NAME" to PKG,
            "app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE" to SIGNATURE_SHA1,
            "app.revanced.MICROG_PACKAGE_NAME" to "app.revanced.android.gms",
        )) {
            check(metaData[name] == value) { "meta-data $name is ${metaData[name]}, expected $value" }
        }
        val queried = manifest.children("queries").flatMap { it.children("package") }.map { it.getAttribute("android:name") }
        check("app.revanced.android.gms" in queried) { "<queries> is missing app.revanced.android.gms" }
        // FCM: the c2dm permission and receiver actions point at MicroG RE.
        check("com.google.android.c2dm.permission.RECEIVE" !in manifestText) { "c2dm RECEIVE permission was not renamed" }
        check("app.revanced.android.c2dm.permission.RECEIVE" in manifestText) { "MicroG c2dm RECEIVE permission missing" }
    }

    private fun assertSpoofSignature(classes: List<ClassDef>, byType: Map<String, ClassDef>) {
        // The extension is merged with the original package name and certificate injected.
        val extension = byType[SIGNATURE_EXTENSION]
            ?: error("$SIGNATURE_EXTENSION (signature extension) not merged into the emitted dexes")
        fun returnedString(name: String): String? {
            val insns = extension.method(name, emptyList()).instructions()
            check(insns.getOrNull(1)?.opcode == Opcode.RETURN_OBJECT) { "$name() does not return immediately" }
            return insns.firstOrNull()?.stringOrNull()
        }
        check(returnedString("getSpoofedPackageName") == PKG) { "spoofed package name is not $PKG" }
        val certificate = Base64.getDecoder().decode(returnedString("getSpoofedCertificate") ?: error("no certificate injected"))
        val sha1 = MessageDigest.getInstance("SHA-1").digest(certificate).joinToString("") { "%02x".format(it) }
        check(sha1 == SIGNATURE_SHA1) { "spoofed certificate SHA-1 is $sha1, expected $SIGNATURE_SHA1" }

        // No app or library code calls PackageManager.getPackageInfo(String, ...) directly any more;
        // every call goes through the extension. Firebase's cert hash helper is among them.
        fun MethodReference.isGetPackageInfo() = name == "getPackageInfo" &&
            parameterTypes.map(CharSequence::toString).let { params ->
                when (definingClass) {
                    SIGNATURE_EXTENSION -> params.getOrNull(0) == "Landroid/content/pm/PackageManager;" &&
                        params.getOrNull(1) == "Ljava/lang/String;"
                    else -> params.firstOrNull() == "Ljava/lang/String;"
                }
            }
        val direct = mutableListOf<String>()
        var routed = 0
        for (cls in classes) {
            if (cls.type.startsWith("Lapp/template/extension/")) continue
            for (m in cls.methods) for (insn in m.instructions()) {
                val call = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (!call.isGetPackageInfo()) continue
                when (call.definingClass) {
                    "Landroid/content/pm/PackageManager;" ->
                        if (insn.opcode == Opcode.INVOKE_VIRTUAL || insn.opcode == Opcode.INVOKE_VIRTUAL_RANGE) direct += "${cls.type}.${m.name}"
                    SIGNATURE_EXTENSION -> routed++
                }
            }
        }
        check(direct.isEmpty()) { "direct getPackageInfo calls left:\n${direct.joinToString("\n")}" }
        check(routed >= 5) { "only $routed getPackageInfo calls routed through the extension" }
    }
}
