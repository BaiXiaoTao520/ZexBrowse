plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// GeckoView 内核版本：单一来源。依赖坐标与 BuildConfig 都从这里取值，
// 避免升级内核时漏改某一处、导致 UA 声称的版本与实际内核不一致。
val geckoViewVersion = "158.0.20261007201254"
// 仅保留主版本号（如 "158.0"），供 UA 生成使用
val geckoViewMajorVersion = geckoViewVersion.split('.').take(2).joinToString(".")

android {
    namespace = "com.zex.zexbrowse"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zex.zexbrowse"
        minSdk = 28
        targetSdk = 36
        versionCode = 53
        versionName = "6.3.7"
        ndk { abiFilters += "arm64-v8a" }

        // GeckoView 内核主版本号（如 "158.0"），供 UA 生成与兜底使用
        buildConfigField("String", "GECKOVIEW_VERSION", "\"$geckoViewMajorVersion\"")

        // 柠枺镜像（Lemwood）高速下载服务。
        // API 域名与启动器 ID 均不从源码硬编码：公开仓库不应暴露接入点，
        // 由构建环境变量注入（CI 用 Secrets，本地可在 ~/.gradle/gradle.properties 配置）。
        // 未注入时功能自动隐藏，用户不会看到半成品入口。
        val lemwoodApiBase: String = (System.getenv("LEMWOOD_API_BASE")
            ?: project.findProperty("lemwood.apiBase")?.toString()
            ?: "").trim()
        val escapedApiBase = lemwoodApiBase.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "LEMWOOD_API_BASE", "\"$escapedApiBase\"")

        val lemwoodLauncherId: String = (System.getenv("LEMWOOD_LAUNCHER_ID")
            ?: project.findProperty("lemwood.launcherId")?.toString()
            ?: "").trim()
        val escapedLauncherId = lemwoodLauncherId.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "LEMWOOD_LAUNCHER_ID", "\"$escapedLauncherId\"")
    }

    splits {
        abi { isEnable = false }
    }

    signingConfigs {
        val storeFilePath = System.getenv("ANDROID_KEYSTORE_PATH")
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:2.2.10",
            "org.jetbrains.kotlin:kotlin-stdlib-common:2.2.10",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-beta01")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // GeckoView 内核：升级到 158（2026-10），替代原先的 131（2024-10）。
    // 旧版本落后 27 个版本，累积了大量已修复的高危漏洞（沙箱逃逸、UAF、同源绕过等），
    // 浏览器内核直接渲染不可信网页，必须保持更新。
    implementation("org.mozilla.geckoview:geckoview:$geckoViewVersion")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
