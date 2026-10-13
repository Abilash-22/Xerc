import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.w3c.dom.Element
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

private const val PKG = "eu.livesport.FlashScore_com"
private const val SDK = "Lcom/google/android/libraries/ads/mobile/sdk/"
private const val ADVERT_ZONE = "Leu/livesport/core/ui/adverts/AdvertZone;"
private const val APPSFLYER_LIB = "Lcom/appsflyer/AppsFlyerLib;"
private const val APPTENTIVE = "Lapptentive/com/android/feedback/Apptentive;"
private const val FIAM_DISPLAY = "Lcom/google/firebase/inappmessaging/FirebaseInAppMessagingDisplay;"
private const val EVENT_LIST_ACTIVITY = "Leu/livesport/LiveSport_cz/EventListActivity;"
private const val CRASHLYTICS_KEY = "firebase_crashlytics_collection_enabled"
private const val CRASHLYTICS_WRAPPER_MARKER = "FirebaseCrashlytics component is not present."

private val FLASHSCORE_PATCHES = setOf(
    "Block marketing popups",
    "Disable ads",
    "Disable rating prompts",
    "Disable telemetry",
    "Disable update prompts",
)

class FlashscoreSmokeTest {

    @TempDir
    lateinit var workDir: File

    private fun Method.strings(): List<String> =
        instructions().mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }

    private fun Method.calls(definingClass: String, name: String): Boolean = instructions().any { insn ->
        val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == definingClass && ref.name == name
    }

    private fun ClassDef.referencesString(value: String): Boolean = methods.any { value in it.strings() }

    private fun Method.params(): List<String> = parameterTypes.map(CharSequence::toString)

    /** The unpatched classes, read straight from the APK's dex files. */
    private fun originalClasses(apk: File): List<ClassDef> = ZipFile(apk).use { zip ->
        zip.entries().toList()
            .filter { Regex("classes\\d*\\.dex").matches(it.name) }
            .flatMap { entry ->
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                DexBackedDexFile(Opcodes.forApi(36), ByteBuffer.wrap(bytes)).classes.toList()
            }
    }

    @Test
    fun `all Flashscore patches apply and mutate the expected bytecode`() {
        val root = repoRoot()
        val apk = File(root, "apks/flashscore/base.apk")

        // The APK lives in the gitignored apks/ directory. Skip rather than fail when it is
        // absent, so CI stays green; locally it is present and the test really runs.
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            version = "26.10.2",
            patchNames = FLASHSCORE_PATCHES,
            allPatches = loadAllPatches(newestPatchBundle(root)),
        )
        val byType = classes.associateBy { it.type }
        val diagnostics = "emitted classes=${classes.size}"

        fun klass(type: String) = byType[type] ?: error("$type not found in emitted dexes\n$diagnostics")

        // Disable ads: SDK initialization is a no-op.
        val mobileAds = klass(SDK + "MobileAds;")
        val initialize = mobileAds.methods.first {
            it.name == "initialize" && it.parameterTypes.size == 2
        }
        assertReturnsEarlyVoid(initialize, label = "MobileAds.initialize(Context, InitializationConfig)")

        // Preloading reports failure and never hands out an ad.
        val preloader = klass(SDK + "banner/BannerAdPreloader;")
        val start = preloader.methods.first {
            it.name == "start" && it.parameterTypes.size == 2 && it.returnType == "Z"
        }
        assertForcedBoolean(start, expected = false, label = "BannerAdPreloader.start(String, PreloadConfiguration)")

        // Direct interstitial loads are a no-op.
        val interstitial = klass(SDK + "interstitial/InterstitialAd;")
        val load = interstitial.methods.first {
            it.name == "load" && it.returnType == "V"
        }
        assertReturnsEarlyVoid(load, label = "InterstitialAd.load(request, callback)")

        // AdvertZone's load entry point hides the slot, runs the (I)V collapse helper and returns.
        val advertZone = klass(ADVERT_ZONE)
        val zoneLoad = advertZone.methods.single {
            it.returnType == "V" && it.parameterTypes.isEmpty() && "Unsupported ad provider: " in it.strings()
        }
        val collapse = advertZone.methods.single {
            it.returnType == "V" && it.params() == listOf("I") &&
                it.calls("Landroidx/constraintlayout/widget/ConstraintLayout;", "setMinHeight")
        }
        val zoneInsns = zoneLoad.instructions()
        val gone = zoneInsns.getOrNull(1)
        assertTrue(
            gone is NarrowLiteralInstruction && gone.narrowLiteral == 8,
            "AdvertZone.${zoneLoad.name}() does not load GONE (8); second instruction is ${gone?.opcode}\n$diagnostics"
        )
        val setVisibility = (zoneInsns.getOrNull(2) as? ReferenceInstruction)?.reference as? MethodReference
        assertTrue(
            setVisibility?.name == "setVisibility",
            "AdvertZone.${zoneLoad.name}() does not call setVisibility first; got $setVisibility\n$diagnostics"
        )
        val collapseCall = (zoneInsns.getOrNull(3) as? ReferenceInstruction)?.reference as? MethodReference
        assertTrue(
            collapseCall?.definingClass == ADVERT_ZONE && collapseCall.name == collapse.name,
            "AdvertZone.${zoneLoad.name}() does not call ${collapse.name}(I); got $collapseCall\n$diagnostics"
        )
        assertTrue(
            zoneInsns.getOrNull(4)?.opcode == Opcode.RETURN_VOID,
            "AdvertZone.${zoneLoad.name}() does not return after collapsing; got ${zoneInsns.getOrNull(4)?.opcode}\n$diagnostics"
        )

        // Video ads: the media item's constructor nulls the VMAP response (p3) first.
        val videoMedia = classes.singleOrNull { cls ->
            cls.methods.any { it.name == "toString" && ", vmapResponse=" in it.strings() }
        } ?: error("no single class with ', vmapResponse=' in toString\n$diagnostics")
        val videoInit = videoMedia.method(
            "<init>",
            listOf("Ljava/util/ArrayList;", "Lcom/zentity/ottplayer/MediaProvider\$DrmData;", "Ljava/lang/String;"),
        )
        val nullVmap = videoInit.instructions().firstOrNull()
        assertTrue(
            nullVmap is NarrowLiteralInstruction && nullVmap.opcode == Opcode.CONST_4 &&
                nullVmap.narrowLiteral == 0 &&
                (nullVmap as OneRegisterInstruction).registerA == videoInit.implementation!!.registerCount - 1,
            "${videoMedia.type}.<init> does not null vmapResponse (p3) first; first instruction is ${nullVmap?.opcode}\n$diagnostics"
        )

        // Disable telemetry: AppsFlyer start / logEvent / logSession / logAdRevenue are no-ops,
        // its push APIs are untouched.
        val appsFlyer = classes.singleOrNull { it.superclass == APPSFLYER_LIB }
            ?: error("no single AppsFlyerLib subclass in emitted dexes\n$diagnostics")
        val appsFlyerSends = appsFlyer.methods.filter {
            it.name in setOf("start", "logEvent", "logSession", "logAdRevenue") && it.implementation != null
        }
        assertTrue(appsFlyerSends.any { it.name == "start" }, "${appsFlyer.type} has no start()\n$diagnostics")
        assertTrue(appsFlyerSends.count { it.name == "logEvent" } >= 2, "${appsFlyer.type} lost a logEvent overload\n$diagnostics")
        for (method in appsFlyerSends) {
            assertReturnsEarlyVoid(method, label = "${appsFlyer.type}.${method.name}(${method.params().joinToString()})", diagnostics)
        }
        for (method in appsFlyer.methods.filter { it.name == "sendPushNotificationData" }) {
            assertTrue(
                method.instructions().firstOrNull()?.opcode != Opcode.RETURN_VOID,
                "${appsFlyer.type}.sendPushNotificationData must stay untouched\n$diagnostics"
            )
        }

        // Crashlytics is R8-renamed; locate its classes the same way the patch does.
        val crashlyticsClasses = classes.filter { it.referencesString(CRASHLYTICS_KEY) }
        assertTrue(crashlyticsClasses.isNotEmpty(), "no class references '$CRASHLYTICS_KEY'\n$diagnostics")
        val collectionGetters = crashlyticsClasses.flatMap { cls ->
            cls.methods.filter { it.returnType == "Z" && it.parameterTypes.isEmpty() && it.implementation != null }
        }
        assertTrue(collectionGetters.isNotEmpty(), "no Crashlytics ()Z getter found\n$diagnostics")
        collectionGetters.forEach {
            assertForcedBoolean(it, expected = false, label = "Crashlytics ${it.definingClass}.${it.name}()Z", diagnostics)
        }
        val recordException = crashlyticsClasses.flatMap { cls ->
            cls.methods.filter { it.returnType == "V" && it.params() == listOf("Ljava/lang/Throwable;") }
        }
        assertTrue(recordException.isNotEmpty(), "no Crashlytics recordException(Throwable) found\n$diagnostics")
        recordException.forEach {
            assertReturnsEarlyVoid(it, label = "Crashlytics ${it.definingClass}.${it.name}(Throwable)", diagnostics)
        }
        val crashlyticsWrapper = classes.single { it.referencesString(CRASHLYTICS_WRAPPER_MARKER) }
        val enableCollection = crashlyticsWrapper.methods.single {
            it.name != "<init>" && it.returnType == "V" && it.parameterTypes.isEmpty() && CRASHLYTICS_KEY in it.strings()
        }
        assertReturnsEarlyVoid(enableCollection, label = "Crashlytics setCrashlyticsCollectionEnabled(true)", diagnostics)

        // Apptentive never registers or engages events.
        val apptentive = klass(APPTENTIVE).methods.filter {
            (it.name == "register" || it.name == "engage") && it.implementation != null
        }
        assertTrue(apptentive.any { it.name == "register" } && apptentive.any { it.name == "engage" },
            "Apptentive register/engage missing\n$diagnostics")
        apptentive.forEach {
            assertReturnsEarlyVoid(it, label = "Apptentive.${it.name}(${it.params().joinToString()})", diagnostics)
        }

        // Disable rating prompts: the settings getBool answers false for the two rating keys.
        val getBool = classes.flatMap { it.methods }.single {
            it.returnType == "Z" && it.strings().containsAll(listOf("Undefined default value for ", " using getBool!"))
        }
        val getBoolInsns = getBool.instructions()
        val nameCall = (getBoolInsns.firstOrNull() as? ReferenceInstruction)?.reference as? MethodReference
        assertTrue(
            nameCall?.definingClass == "Ljava/lang/Enum;" && nameCall.name == "name",
            "${getBool.definingClass}.${getBool.name} does not read the key's name() first; got $nameCall\n$diagnostics"
        )
        val prologue = getBoolInsns.take(12)
        val prologueStrings = prologue.mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }
        assertTrue(
            prologueStrings == listOf("CAN_SHOW_RATING_BANNER", "CAN_SHOW_INAPP_RATING"),
            "${getBool.definingClass}.${getBool.name} prologue compares $prologueStrings\n$diagnostics"
        )
        val forcedFalse = prologue.indexOfFirst {
            it is NarrowLiteralInstruction && it.opcode == Opcode.CONST_4 && it.narrowLiteral == 0
        }
        assertTrue(
            forcedFalse > 0 && prologue.getOrNull(forcedFalse + 1)?.opcode == Opcode.RETURN,
            "${getBool.definingClass}.${getBool.name} prologue does not return false\n$diagnostics"
        )

        // Block marketing popups: the remote dialog reports "not shown".
        val remoteDialog = classes.flatMap { it.methods }.single {
            it.returnType == "Z" && it.params() == listOf(EVENT_LIST_ACTIVITY) && "btnGoUrl" in it.strings()
        }
        assertForcedBoolean(remoteDialog, expected = false, label = "remote dialog ${remoteDialog.definingClass}.${remoteDialog.name}", diagnostics)

        // Every Firebase In-App Messaging display implementation returns at once.
        val fiamDisplays = classes.filter { FIAM_DISPLAY in it.interfaces }
        assertTrue(fiamDisplays.isNotEmpty(), "no FirebaseInAppMessagingDisplay implementation found\n$diagnostics")
        for (display in fiamDisplays) {
            assertReturnsEarlyVoid(
                display.methods.single { it.name == "displayMessage" },
                label = "${display.type}.displayMessage()", diagnostics
            )
        }

        // In-app promo strips parse to an empty list.
        val stripParse = classes.flatMap { it.methods }.single {
            it.returnType == "Ljava/util/List;" && "Failed to parse InAppStrip JSON" in it.strings()
        }
        assertReturnsMethodCall(
            stripParse, "Ljava/util/Collections;", "emptyList",
            label = "InAppStrip parser ${stripParse.definingClass}.${stripParse.name}", diagnostics
        )

        // Disable update prompts: the SOFT/HARD dialog method returns before building the dialog.
        val updateDialog = classes.flatMap { it.methods }.single {
            it.returnType == "V" && "last_notify_" in it.strings() && it.calls("Landroid/app/Dialog;", "show")
        }
        assertReturnsEarlyVoid(updateDialog, label = "update dialog ${updateDialog.definingClass}.${updateDialog.name}", diagnostics)

        // Disable telemetry (manifest): the collection switches are set on <application>.
        val manifestFile = workDir.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && it.readText().contains("firebase_analytics_collection_deactivated") }
            .firstOrNull() ?: error("no decoded AndroidManifest.xml with the telemetry meta-data under $workDir")
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile).documentElement
        fun Element.children(tag: String) =
            getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }
        val application = manifest.children("application").single()
        val metaData = application.children("meta-data")
            .filter { it.parentNode == application }
            .groupBy({ it.getAttribute("android:name") }, { it.getAttribute("android:value") })
        for ((name, value) in mapOf(
            "firebase_analytics_collection_deactivated" to "true",
            "firebase_performance_collection_deactivated" to "true",
            "google_analytics_adid_collection_enabled" to "false",
            "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
            "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
        )) {
            check(metaData[name] == listOf(value)) { "meta-data $name is ${metaData[name]}, expected [$value]" }
        }
        // FCM auto-init is not touched (the stock manifest does not set it).
        check("firebase_messaging_auto_init_enabled" !in metaData) { "firebase_messaging_auto_init_enabled was added" }

        // Push must keep working: the app's FirebaseMessagingService (the MESSAGING_EVENT service
        // that is not Firebase's own) is byte-for-byte the same opcode stream as in the stock APK.
        val messagingServices = application.children("service").filter { service ->
            service.children("action").any { it.getAttribute("android:name") == "com.google.firebase.MESSAGING_EVENT" }
        }.map { "L" + it.getAttribute("android:name").replace('.', '/') + ";" }
        val appService = messagingServices.singleOrNull { it != "Lcom/google/firebase/messaging/FirebaseMessagingService;" }
            ?: error("no single app MESSAGING_EVENT service in $messagingServices")
        val original = originalClasses(apk).associateBy { it.type }
        for (type in listOf(appService, "Lcom/google/firebase/messaging/FirebaseMessagingService;")) {
            val before = original[type] ?: error("$type not in the stock APK")
            val after = klass(type)
            for (name in listOf("onMessageReceived", "onNewToken")) {
                assertTrue(
                    type != appService || after.methods.any { it.name == name },
                    "$type has no $name\n$diagnostics"
                )
            }
            for (method in after.methods) {
                val stock = before.methods.single { it.name == method.name && it.params() == method.params() }
                assertTrue(
                    stock.instructions().map { it.opcode } == method.instructions().map { it.opcode },
                    "$type.${method.name} changed; push handling must stay untouched\n$diagnostics"
                )
            }
        }
    }
}
