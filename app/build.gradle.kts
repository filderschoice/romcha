import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
}

// リリース署名の情報（PLAN 6章）。鍵とパスワードはリポジトリに含めず、ルートの keystore.properties（.gitignore 済み）から読む。
// ファイルが無ければ release は未署名のままビルドする（配布物名に -unsigned を付けて区別する）
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.isFile) file.inputStream().use { load(it) }
    }
val hasReleaseSigning = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "io.github.filderschoice.romcha"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.filderschoice.romcha"
        minSdk = 34
        targetSdk = 36
        // versionCode は MAJOR × 10000 + MINOR × 100 + PATCH（docs/RELEASE.md 2章）
        versionCode = 10000
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

// 配布物（PLAN 6章）: リリース APK を romcha-vX.Y.Z.apk（未署名なら romcha-vX.Y.Z-unsigned.apk）として
// build/dist へ置き、SHA-256 を同名の .sha256（sha256sum と同じ「ハッシュ  ファイル名」形式）で書き出す。
// アセット名を固定するのは Obtainium 等の GitHub 追従インストーラから取得できるようにするため
tasks.register("releaseDist") {
    group = "distribution"
    description = "リリース APK を配布用の名前で build/dist へ置き、SHA-256 を書き出す"
    dependsOn("assembleRelease")
    val apkDir = layout.buildDirectory.dir("outputs/apk/release")
    val distDir = layout.buildDirectory.dir("dist")
    val baseName = "romcha-v${android.defaultConfig.versionName}" + if (hasReleaseSigning) "" else "-unsigned"
    inputs.dir(apkDir)
    outputs.dir(distDir)
    doLast {
        val apks = apkDir.get().asFile.listFiles { file -> file.extension == "apk" }.orEmpty()
        val apk = apks.singleOrNull() ?: error("リリース APK が 1 つに定まりません: ${apks.map { it.name }}")
        val outDir = distDir.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
        val target = outDir.resolve("$baseName.apk")
        apk.copyTo(target)
        val digest = MessageDigest.getInstance("SHA-256").digest(target.readBytes())
        val hex = digest.joinToString("") { "%02x".format(it) }
        outDir.resolve("$baseName.apk.sha256").writeText("$hex  ${target.name}\n")
        logger.lifecycle("配布物: ${target.path}")
    }
}

dependencies {
    implementation(project(":core:chat"))
    implementation(project(":core:media"))
    implementation(project(":core:sync"))
    implementation(project(":feature:overlay"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.aboutlibraries.compose)
    // 更新確認（F-APP-02）の応答の解析。OkHttp は core:chat から api 依存で受け取る
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
