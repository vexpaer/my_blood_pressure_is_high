plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 高德 Android Key：优先级 Gradle Property（-PAMAP_API_KEY）> 环境变量 AMAP_API_KEY。
// 留空时 App 完整可用（地图页显示配置提示），绝不 Crash；真实 Key 通过 GitHub Secret 注入，禁止提交。
val amapApiKey: String = (project.findProperty("AMAP_API_KEY") as String?)
    ?: System.getenv("AMAP_API_KEY")
    ?: ""

android {
    namespace = "io.github.vexpaer.mybp"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.vexpaer.mybp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey
        buildConfigField("String", "AMAP_API_KEY", "\"$amapApiKey\"")
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        // CI 中由 workflow 先把 secret KEYSTORE_BASE64 解码为 keystore.jks 再构建；
        // 以文件是否存在为开关（GitHub 上未配置的 secret 会被注入为空串而非 null）。
        val keystore = rootProject.file("keystore.jks")
        if (keystore.exists() && System.getenv("KEYSTORE_PASSWORD") != null) {
            create("ci") {
                storeFile = keystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "mybp"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfigs.findByName("ci")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        disable += "MissingTranslation"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.amap.all)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
