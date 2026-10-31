plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// GeckoView 内核版本：单一来源。依赖坐标与 BuildConfig 都从这里取值，
// 避免升级内核时漏改某一处、导致 UA 声称的版本与实际内核不一致。
val geckoViewVersion = "158.0.20261021001945"
// 仅保留主版本号（如 "158.0"），供 UA 生成使用
val geckoViewMajorVersion = geckoViewVersion.split('.').take(2).joinToString(".")

// 柠枺镜像（Lemwood）配置：不硬编码在源码里，由环境变量或 gradle.properties 注入。
// 构建期校验会在缺失时直接失败，避免产出「功能静默消失」的包。
val lemwoodApiBase: String = (System.getenv("LEMWOOD_API_BASE")
    ?: project.findProperty("lemwood.apiBase")?.toString()
    ?: "").trim()
val lemwoodLauncherId: String = (System.getenv("LEMWOOD_LAUNCHER_ID")
    ?: project.findProperty("lemwood.launcherId")?.toString()
    ?: "").trim()

/**
 * 发布构建必须带柠枺镜像配置。
 *
 * 缺配置时 LemwoodMirror.isAvailable 为 false，高速下载入口会静默消失；
 * 那种包看起来能装能用，实则是残废功能，很容易一带而过发出去。
 * 因此在这里把问题提前到构建期，并给出可操作的修复提示。
 *
 * debug 构建不做限制：本地调试未必需要接入点，也能避免开发者处处报错。
 */
fun requireLemwoodConfig(target: String) {
    val missing = mutableListOf<String>()
    if (lemwoodApiBase.isBlank()) missing += "LEMWOOD_API_BASE"
    if (lemwoodLauncherId.isBlank()) missing += "LEMWOOD_LAUNCHER_ID"

    // 格式校验必须在「是否缺失」判断之前：已经填了但填错的地址同样会让功能 404，
    // 而且比完全不填更难排查。
    if (lemwoodApiBase.isNotBlank() && !lemwoodApiBase.contains("/api")) {
        throw GradleException(
            "LEMWOOD_API_BASE 格式不对：应形如 https://<域名>/api/v2，当前为「$lemwoodApiBase」。"
        )
    }

    if (missing.isEmpty()) return

    throw GradleException(
        """
        缺少柠枺镜像配置，已拒绝构建 $target。

        未设置的项：${missing.joinToString("、")}

        设置方式（任选一种）：
          1) CI：在仓库 Secrets 里配置 LEWMOOD_API_BASE 与 LEWMOOD_LAUNCHER_ID；
          2) 本地：在 ~/.gradle/gradle.properties 或项目 gradle.properties 中写入：
               lemwood.apiBase=https://<域名>/api/v2
               lemwood.launcherId=<启动器 ID>
          3) 环境变量：export LEWMOOD_API_BASE=... LEWMOOD_LAUNCHER_ID=...

        参考值：LEMWOOD_API_BASE=https://miawa.cn/api/v2，LEMWOOD_LAUNCHER_ID=ZexBrowse
        """.trimIndent()
    )
}

android {
    namespace = "com.zex.zexbrowse"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zex.zexbrowse"
        minSdk = 28
        targetSdk = 37
        versionCode = 76
        versionName = "11.1.1"
        ndk { abiFilters += "arm64-v8a" }

        // GeckoView 内核主版本号（如 "158.0"），供 UA 生成与兜底使用
        buildConfigField("String", "GECKOVIEW_VERSION", "\"$geckoViewMajorVersion\"")

        // 柠枺镜像（Lemwood）高速下载服务。
        // API 域名与启动器 ID 均不从源码硬编码：公开仓库不应暴露接入点，
        // 由构建环境变量注入（CI 用 Secrets，本地可在 ~/.gradle/gradle.properties 配置）。
        val escapedApiBase = lemwoodApiBase.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "LEMWOOD_API_BASE", "\"$escapedApiBase\"")

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

// 发布构建前置校验：缺柠枺配置时在编译前就失败，不产出功能残缺的包。
//
// 挂在 taskGraph 而非 preReleaseBuild 的 doFirst：后者在任务 UP-TO-DATE 时
// 整个 doFirst 都会被跳过，校验就形同虚设。taskGraph 在任务图确定后、
// 任何任务执行前触发，不受增量构建影响。
gradle.taskGraph.whenReady {
    // 只要任务图里出现本模块的 Release 编译/打包任务，就要求柠枺配置齐备。
    // debug 构建不会被匹配到，本地调试不受影响。
    val buildingRelease = allTasks.any { it.project == project && it.name.contains("Release") }
    if (buildingRelease) requireLemwoodConfig("发布包")
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
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    // core 1.17.0：提供 NotificationCompat.ProgressStyle 与 setRequestPromotedOngoing，
    // 下载通知才能升级为 Android 16+ 的实时活动（Live Update）。
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // material3 固定 1.5.0-beta02：1.4.x 稳定版没有 ContainedLoadingIndicator（带容器背景的形变加载指示器），
    // 它在 1.5.0-beta02 才加入。该版本属于 beta，但 API 已定稿，且项目原本就已在用 1.5.0-beta01。
    implementation("androidx.compose.material3:material3:1.5.0-beta02")
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
