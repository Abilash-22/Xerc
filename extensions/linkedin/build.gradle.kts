// LinkedIn helpers for Heval Controls: the nav panel entry row and its click listener. Builds
// extensions/linkedin.mpe; the Heval Controls core is not bundled here (the host patch merges
// extensions/hevalcontrols.mpe), see docs/heval-controls.md.
extension {
    name = "extensions/linkedin.mpe"
}

android {
    namespace = "app.template.extension"
}

dependencies {
    // Signatures of the few LinkedIn classes the helpers use. compileOnly: the app provides them.
    compileOnly(project(":extensions:linkedin:stub"))
}

// Pure-Java extension. AGP's built-in Kotlin adds kotlin-stdlib by default, which would grow the
// .mpe by ~2 MB and merge an unobfuscated Kotlin stdlib into the target app's dex.
configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}
