import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// ビルドツール側の依存の脆弱性対応（BL-112）。修正版を gradle/security-patches.txt に列挙し、「この版以上」の制約として
// プラグインのクラスパス（AGP など）と全モジュールの設定へ適用する。すでに新しい版が使われていれば下げない。
buildscript {
    val patches = file("gradle/security-patches.txt").readLines().filter { it.isNotBlank() && !it.startsWith("#") }
    dependencies {
        constraints {
            patches.forEach { add("classpath", it) }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// 静的解析は全モジュール共通の設定で適用する（CLAUDE.md「本リポジトリの品質ゲート定義」）
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom("src/main/kotlin", "src/test/kotlin")
    }
}

// 各モジュールのツール側の設定（ktlint・lint など）にも同じ修正版を適用する（BL-112）。設定によっては制約を付けられないため、
// 要求された版が修正版より古い時だけ修正版へ上げる（下げない）。
val securityPatches =
    file("gradle/security-patches.txt")
        .readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .associate { line -> line.substringBeforeLast(':') to line.substringAfterLast(':') }

fun isOlderVersion(
    version: String,
    patched: String,
): Boolean {
    val a = version.split('.', '-').map { it.toIntOrNull() ?: 0 }
    val b = patched.split('.', '-').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (diff != 0) return diff < 0
    }
    return false
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val patched = securityPatches["${requested.group}:${requested.name}"]
            val version = requested.version
            if (patched != null && version != null && isOlderVersion(version, patched)) {
                useVersion(patched)
                because("Dependabot アラートの修正版（BL-112）")
            }
        }
    }
}
