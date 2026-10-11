import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val PKG = "app.expert.player"
private const val AD_RUNNABLE = "Lio/flutter/plugins/googlemobileads/FlutterMobileAdsWrapper\$1;"
private const val PAIRIP_LICENSE = "Lcom/pairip/licensecheck/LicenseClient;"

class ExpertPlayerSmokeTest {

    @TempDir
    lateinit var workDir: File

    @Test
    fun `Disable ads returns before the Mobile Ads start`() {
        val root = repoRoot()
        val apk = File(root, "apks/expertplayer/base.apk")

        // The APK lives in the gitignored apks/ directory. Skip rather than fail when it is
        // absent, so CI stays green; locally it is present and the test really runs.
        assumeTrue(apk.exists(), "skipping: base.apk not present at ${apk.path}")

        val classes = applyPatches(
            apk = apk,
            workDir = workDir,
            pkg = PKG,
            version = "11.2.0",
            patchNames = setOf("Disable ads"),
            allPatches = loadAllPatches(newestPatchBundle(root)),
        )

        val runnable = classes.firstOrNull { it.type == AD_RUNNABLE }
            ?: error("$AD_RUNNABLE not found in emitted dexes")

        assertReturnsEarlyVoid(
            runnable.method("run", emptyList()),
            label = "FlutterMobileAdsWrapper\$1.run()",
        )

        val license = classes.firstOrNull { it.type == PAIRIP_LICENSE }
            ?: error("$PAIRIP_LICENSE not found in emitted dexes")

        assertReturnsEarlyVoid(
            license.method("checkLicense", listOf("Landroid/content/Context;")),
            label = "LicenseClient.checkLicense(Context)",
        )
    }
}
