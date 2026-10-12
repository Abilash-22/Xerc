import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val PKG = "com.truecaller"
private const val TRUECALLER_INIT = "Lcom/truecaller/ui/TruecallerInit;"
private const val SIGNATURE_SHA1 = "0ac1169ae6cead75264c725febd8e8d941f25e31"

private const val PREMIUM_TIER_TYPE = "Lcom/truecaller/premium/data/tier/PremiumTierType;"
private const val PREMIUM_FEATURE = "Lcom/truecaller/premium/data/feature/PremiumFeature;"
private const val PREMIUM_FEATURE_STATUS = "Lcom/truecaller/premium/data/feature/PremiumFeatureStatus;"
private const val SUBSCRIPTION_WORKER = "Lcom/truecaller/premium/PremiumSubscriptionStatusNotificationWorker;"
private const val OTP_SMS_API = "Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;"
private const val UPDATE_TRIGGER = "Lcom/truecaller/inappupdate/UpdateTrigger;"
private const val ADS_CONTAINER = "Lcom/truecaller/ads/ui/AdsContainer;"
private const val ADS_CONTAINER_LIGHT = "Lcom/truecaller/ads/ui/AdsContainerLight;"
private const val ADS_SWITCH_VIEW = "Lcom/truecaller/ads/ui/AdsSwitchView;"
private const val AD_LAYOUT_TYPE = "Lcom/truecaller/ads/AdLayoutTypeX;"

class TruecallerSmokeTest {

    @TempDir
    lateinit var workDir: File

    private fun Method.params() = parameterTypes.map(CharSequence::toString)

    private fun Method.strings() = instructions().mapNotNull { it.stringOrNull() }

    private fun Instruction.field(): FieldReference? = (this as? ReferenceInstruction)?.reference as? FieldReference

    private fun Instruction.call(): MethodReference? = (this as? ReferenceInstruction)?.reference as? MethodReference

    private fun List<ClassDef>.methodsWhere(predicate: (ClassDef, Method) -> Boolean): List<Pair<ClassDef, Method>> =
        flatMap { cls -> cls.methods.filter { predicate(cls, it) }.map { cls to it } }

    private fun List<ClassDef>.singleMethod(label: String, predicate: (ClassDef, Method) -> Boolean): Pair<ClassDef, Method> {
        val found = methodsWhere(predicate)
        check(found.size == 1) { "expected one $label, found ${found.map { (c, m) -> "${c.type}.${m.name}" }}" }
        return found.single()
    }

    private fun assertReturnsGold(method: Method, label: String) {
        val insns = method.instructions()
        val gold = insns.getOrNull(0)?.field()
        check(insns.getOrNull(0)?.opcode == Opcode.SGET_OBJECT && gold?.definingClass == PREMIUM_TIER_TYPE && gold.name == "GOLD") {
            "$label does not load PremiumTierType.GOLD first; got ${insns.getOrNull(0)?.opcode} $gold"
        }
        check(insns.getOrNull(1)?.opcode == Opcode.RETURN_OBJECT) { "$label does not return GOLD at once" }
    }

    @Test
    fun `Truecaller patches unlock premium, remove ads, telemetry and update prompts, and route GMS to MicroG RE`() {
        val root = repoRoot()
        val apk = File(root, "apks/truecaller/base.apk")

        // The APK lives in the gitignored apks/ directory. Skip rather than fail when it is
        // absent, so CI stays green; locally it is present and the test really runs.
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            version = "26.39.6",
            patchNames = setOf(
                "Enable Premium",
                "Disable ads",
                "Disable telemetry",
                "Disable update prompts",
                "Hide Assistant tab",
                "Hide Scam feed",
                "Use SMS for verification codes",
                "GmsCore support",
                "Spoof signature",
            ),
            allPatches = loadAllPatches(newestPatchBundle(root)),
        )
        val byType = classes.associateBy { it.type }

        assertEnablePremium(classes, byType)
        assertDisableAds(classes, byType)
        assertDisableTelemetry(classes, byType)
        assertDisableUpdatePrompts(classes)
        assertFeatureToggles(classes)
        assertUseSmsVerification(classes)
        // onGetCredential tests isAvailableOnDevice(int) in this build, so the routing applies.
        assertGmsCoreSupport(classes, workDir, PKG, TRUECALLER_INIT, SIGNATURE_SHA1, expectCredentialRouting = true)
        assertSpoofSignature(classes, PKG, SIGNATURE_SHA1)
    }

    private fun assertEnablePremium(classes: List<ClassDef>, byType: Map<String, ClassDef>) {
        // 1. Tier getter ("premiumLevel") returns GOLD.
        val (store, tier) = classes.singleMethod("premiumLevel tier getter") { _, m ->
            m.returnType == PREMIUM_TIER_TYPE && m.parameterTypes.isEmpty() && "premiumLevel" in m.strings()
        }
        assertReturnsGold(tier, "${store.type}.${tier.name}() (tier)")

        // 2. The plain isPremium getter in the same class; the grace-period getter is untouched.
        val isPremium = store.methods.single {
            it.returnType == "Z" && it.parameterTypes.isEmpty() &&
                "isPremiumExpired" in it.strings() && "isInGracePeriod" !in it.strings()
        }
        assertForcedBoolean(isPremium, expected = true, label = "${store.type}.${isPremium.name}() (isPremium)")
        val grace = store.methods.single { "isInGracePeriod" in it.strings() && it.returnType == "Z" }
        check(grace.instructions().first().opcode != Opcode.CONST_4) { "${store.type}.${grace.name}() (grace period) was changed" }

        // 3. Static PremiumFeatureStatus == INCLUDED check.
        val (gateClass, gate) = classes.singleMethod("PremiumFeatureStatus gate") { _, m ->
            m.returnType == "Z" && m.params() == listOf(PREMIUM_FEATURE_STATUS) && AccessFlags.STATIC.isSet(m.accessFlags) &&
                m.instructions().any { it.opcode == Opcode.SGET_OBJECT && it.field()?.name == "INCLUDED" }
        }
        assertForcedBoolean(gate, expected = true, label = "${gateClass.type}.${gate.name}(PremiumFeatureStatus)")

        // 4. isFeatureAvailable(feature, refresh).
        val (featureClass, feature) = classes.singleMethod("feature availability check") { _, m ->
            m.returnType == "Z" && m.params() == listOf(PREMIUM_FEATURE, "Z") && "disabledPremiumFeatures" in m.strings()
        }
        assertForcedBoolean(feature, expected = true, label = "${featureClass.type}.${feature.name}(PremiumFeature, Z)")

        // 5. PremiumState constructor: const/16 p1, 0x1 and sget-object p2, GOLD before anything else.
        val stateClass = classes.single { cls -> cls.methods.any { m -> m.strings().any { it.startsWith("PremiumState(isPremium=") } } }
        val constructor = stateClass.method(
            "<init>",
            listOf(
                "Z", PREMIUM_TIER_TYPE, "Lcom/truecaller/premium/data/ProductKind;",
                "Lcom/truecaller/premium/data/PremiumScope;", "Lcom/truecaller/premium/data/InsuranceState;",
            ),
        )
        val p1 = constructor.implementation!!.registerCount - 5 // this + 5 single-register parameters
        val insns = constructor.instructions()
        val first = insns.getOrNull(0)
        check(
            first?.opcode == Opcode.CONST_16 && (first as OneRegisterInstruction).registerA == p1 &&
                (first as NarrowLiteralInstruction).narrowLiteral == 1
        ) { "${stateClass.type}.<init> does not start with const/16 p1, 0x1; got ${first?.opcode}" }
        val second = insns.getOrNull(1)
        check(
            second?.opcode == Opcode.SGET_OBJECT && (second as OneRegisterInstruction).registerA == p1 + 1 &&
                second.field()?.definingClass == PREMIUM_TIER_TYPE && second.field()?.name == "GOLD"
        ) { "${stateClass.type}.<init> does not set p2 to GOLD second; got ${second?.opcode} ${second?.field()}" }

        // 6. The subscription worker returns a fresh no-arg Result (Failure in this build) at once.
        val result = listOf("Success {mOutputData=", "Failure {mOutputData=").map { text ->
            classes.single { cls -> cls.methods.any { m -> m.name == "toString" && text in m.strings() } }
        }.first { cls -> cls.methods.any { it.name == "<init>" && it.parameterTypes.isEmpty() } }
        val doWork = (byType[SUBSCRIPTION_WORKER] ?: error("$SUBSCRIPTION_WORKER not found")).methods.single { it.name == "doWork" }
        val work = doWork.instructions()
        check(work.getOrNull(0)?.opcode == Opcode.NEW_INSTANCE &&
            ((work[0] as ReferenceInstruction).reference as TypeReference).type == result.type) {
            "doWork does not start with new-instance ${result.type}; got ${work.getOrNull(0)?.opcode}"
        }
        val init = work.getOrNull(1)?.call()
        check(work.getOrNull(1)?.opcode == Opcode.INVOKE_DIRECT && init?.definingClass == result.type && init.name == "<init>" &&
            init.parameterTypes.isEmpty()) { "doWork does not construct ${result.type}; got $init" }
        check(work.getOrNull(2)?.opcode == Opcode.RETURN_OBJECT) { "doWork does not return the Result at once" }
    }

    private fun assertDisableAds(classes: List<ClassDef>, byType: Map<String, ClassDef>) {
        fun require(type: String) = byType[type] ?: error("$type not found")

        // Banner and native binds: the holder types are derived the same way the patch does.
        val switchView = require(ADS_SWITCH_VIEW)
        val bannerHolder = switchView.methods.single {
            it.returnType == "V" && it.params().size == 2 && it.params()[1] == AD_LAYOUT_TYPE
        }.params().first()
        val container = require(ADS_CONTAINER)
        val light = require(ADS_CONTAINER_LIGHT)
        val layoutConfig = container.methods.first {
            it.returnType == "V" && it.params().size >= 2 && it.params().first() == bannerHolder
        }.params()[1]
        val nativeHolders = (container.methods + light.methods).map { it.params() }
            .filter { it.size >= 2 && it[1] == layoutConfig && it[0] != bannerHolder }.map { it.first() }.toSet()
        check(nativeHolders.isNotEmpty()) { "no native ad holder type found" }
        val adTypes = nativeHolders + bannerHolder + AD_LAYOUT_TYPE

        var banner = 0
        var native = 0
        for (cls in listOf(container, light, switchView)) {
            val binds = cls.methods.filter { m ->
                m.returnType == "V" && m.name != "<init>" && !AccessFlags.STATIC.isSet(m.accessFlags) &&
                    m.params().firstOrNull()?.startsWith("L") == true && m.params().any { it in adTypes }
            }
            check(binds.isNotEmpty()) { "no ad bind method in ${cls.type}" }
            for (bind in binds) {
                val label = "${cls.type}.${bind.name}(${bind.params().joinToString()})"
                val p0 = bind.implementation!!.registerCount - bind.params().sumOf { if (it == "J" || it == "D") 2 else 1 } - 1
                val insns = bind.instructions()
                val gone = insns.getOrNull(0)
                check(gone?.opcode == Opcode.CONST_16 && (gone as OneRegisterInstruction).registerA == p0 + 1 &&
                    (gone as NarrowLiteralInstruction).narrowLiteral == 8) { "$label does not start with const/16 p1, 0x8" }
                val set = insns.getOrNull(1)
                check(set?.opcode == Opcode.INVOKE_VIRTUAL_RANGE && set.call()?.name == "setVisibility" &&
                    (set as RegisterRangeInstruction).startRegister == p0 && set.registerCount == 2) {
                    "$label does not call setVisibility(p0, GONE); got ${set?.opcode} ${set?.call()}"
                }
                check(insns.getOrNull(2)?.opcode == Opcode.RETURN_VOID) { "$label does not return after hiding" }
                if (bannerHolder in bind.params()) banner++
                if (bind.params().any { it in nativeHolders }) native++
            }
        }
        check(banner >= 3 && native >= 4) { "only $banner banner and $native native bind methods patched" }
        // AdsSwitchView's position helper c(I)V is left alone.
        val position = switchView.methods.single { it.returnType == "V" && it.params() == listOf("I") }
        check(position.instructions().first().opcode != Opcode.CONST_16) { "AdsSwitchView.${position.name}(I) was changed" }

        // Interstitial / app-open activities: super.onCreate, finish(), return-void.
        for (type in listOf(
            "Lcom/truecaller/ads/interstitial/AdInterstitialActivity;",
            "Lcom/truecaller/ads/interstitial/AdAppOpeningActivity;",
            "Lcom/truecaller/ads/adsrouter/ui/interstitial/AdRouterInterstitialActivity;",
        )) {
            val insns = require(type).method("onCreate", listOf("Landroid/os/Bundle;")).instructions()
            val superCall = insns.indexOfFirst { it.opcode == Opcode.INVOKE_SUPER && it.call()?.name == "onCreate" }
            check(superCall >= 0) { "$type.onCreate does not call super.onCreate" }
            val finish = insns.getOrNull(superCall + 1)
            check(finish?.opcode == Opcode.INVOKE_VIRTUAL_RANGE && finish.call()?.name == "finish" &&
                finish.call()?.definingClass == "Landroid/app/Activity;") {
                "$type.onCreate does not finish() right after super.onCreate; got ${finish?.opcode} ${finish?.call()}"
            }
            check(insns.getOrNull(superCall + 2)?.opcode == Opcode.RETURN_VOID) { "$type.onCreate does not return after finish()" }
        }
    }

    private fun assertDisableTelemetry(classes: List<ClassDef>, byType: Map<String, ClassDef>) {
        val tracker = byType["Lcom/truecaller/analytics/technical/AppStartTracker;"] ?: error("AppStartTracker not found")
        assertReturnsEarlyVoid(tracker.method("enableTracking", emptyList()), label = "AppStartTracker.enableTracking()")

        val cleverTap = classes.filter { "Lcom/truecaller/clevertap/CleverTapManager;" in it.interfaces }
        check(cleverTap.isNotEmpty()) { "no CleverTapManager implementation found" }
        for (cls in cleverTap) {
            assertReturnsEarlyVoid(cls.method("push", listOf("Ljava/lang/String;")), label = "${cls.type}.push(String)")
            assertReturnsEarlyVoid(
                cls.method("push", listOf("Ljava/lang/String;", "Ljava/util/Map;")),
                label = "${cls.type}.push(String, Map)",
            )
        }

        val appsFlyer = classes.filter { it.superclass == "Lcom/appsflyer/AppsFlyerLib;" }
        check(appsFlyer.isNotEmpty()) { "no AppsFlyerLib subclass found" }
        for (cls in appsFlyer) {
            val starts = cls.methods.filter { it.name == "start" && it.params().firstOrNull() == "Landroid/content/Context;" }
            check(starts.any { it.params().size == 1 }) { "${cls.type} has no start(Context)" }
            starts.forEach { assertReturnsEarlyVoid(it, label = "${cls.type}.start(${it.params().joinToString()})") }
        }
    }

    private fun assertDisableUpdatePrompts(classes: List<ClassDef>) {
        val manager = classes.single { cls ->
            cls.methods.any {
                it.returnType == "Ljava/util/concurrent/CompletableFuture;" &&
                    it.params() == listOf("Landroid/app/Activity;", UPDATE_TRIGGER)
            }
        }
        val start = manager.methods.single {
            it.returnType == "Ljava/lang/Object;" && it.params().size == 3 &&
                it.params().take(2) == listOf("Landroid/app/Activity;", UPDATE_TRIGGER)
        }
        val insns = start.instructions()
        val value = insns.getOrNull(0)?.field()
        check(insns.getOrNull(0)?.opcode == Opcode.SGET_OBJECT && value?.definingClass == "Ljava/lang/Boolean;" && value.name == "FALSE") {
            "${manager.type}.${start.name} does not load Boolean.FALSE first; got ${insns.getOrNull(0)?.opcode} $value"
        }
        check(insns.getOrNull(1)?.opcode == Opcode.RETURN_OBJECT) { "${manager.type}.${start.name} does not return at once" }

        // The stored server forced-update state always reads NONE.
        val (typeOwner, typeGetter) = classes.singleMethod("forced update type getter") { _, m ->
            m.returnType == "Lcom/truecaller/forcedupdate/UpdateType;" && m.parameterTypes.isEmpty() &&
                "forcedUpdate_updateType" in m.strings()
        }
        val typeInsns = typeGetter.instructions()
        val type = typeInsns.getOrNull(0)?.field()
        check(typeInsns.getOrNull(0)?.opcode == Opcode.SGET_OBJECT && type?.name == "NONE" &&
            type.definingClass == "Lcom/truecaller/forcedupdate/UpdateType;" &&
            typeInsns.getOrNull(1)?.opcode == Opcode.RETURN_OBJECT
        ) { "${typeOwner.type}.${typeGetter.name}() does not return UpdateType.NONE at once; got ${typeInsns.getOrNull(0)?.opcode} $type" }
    }

    private fun assertFeatureToggles(classes: List<ClassDef>) {
        for (key in listOf("featureCallAssistant", "featureScamFeed")) {
            val (cls, toggle) = classes.singleMethod("$key toggle") { _, m ->
                m.returnType == "Z" && m.parameterTypes.isEmpty() && key in m.strings()
            }
            assertForcedBoolean(toggle, expected = false, label = "${cls.type}.${toggle.name}() ($key)")
        }
    }

    private fun assertUseSmsVerification(classes: List<ClassDef>) {
        // The selection branch now loads SMS where it loaded GOOGLE and returns it. Located by its
        // check-cast to OtpSmsApi right before, since the GOOGLE read the patch anchored on is gone.
        val selections = classes.methodsWhere { _, m ->
            m.returnType == "Ljava/lang/Object;" && m.parameterTypes.isEmpty() &&
                m.instructions().windowed(3).any { (cast, sms, ret) ->
                    cast.opcode == Opcode.CHECK_CAST &&
                        ((cast as ReferenceInstruction).reference as TypeReference).type == OTP_SMS_API &&
                        sms.opcode == Opcode.SGET_OBJECT && sms.field()?.definingClass == OTP_SMS_API && sms.field()?.name == "SMS" &&
                        ret.opcode == Opcode.RETURN_OBJECT &&
                        (ret as OneRegisterInstruction).registerA == (sms as OneRegisterInstruction).registerA
                }
        }
        check(selections.size == 1) { "expected one OtpSmsApi selection returning SMS, found ${selections.map { (c, m) -> "${c.type}.${m.name}" }}" }
        val (cls, method) = selections.single()
        check(method.instructions().none { it.opcode == Opcode.SGET_OBJECT && it.field()?.definingClass == OTP_SMS_API && it.field()?.name == "GOOGLE" }) {
            "${cls.type}.${method.name} still compares with OtpSmsApi.GOOGLE"
        }
    }
}
