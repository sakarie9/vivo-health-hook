plugins {
    alias(libs.plugins.agp.app)
    // Kotlin 编译由 AGP 9 的 built-in Kotlin 提供。
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sakari.vivohealthxposed"
    // libxposed service/interface 102.0.0 的 AAR 元数据要求 compileSdk >= 37
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sakari.vivohealthxposed"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = false
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            // 原型阶段关闭混淆：libxposed 的入口类与 META-INF/xposed/* 都保持原样，便于排查。
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
            // 用 debug 签名，方便直接 sideload 安装；正式发布请替换成自己的签名。
            signingConfig = signingConfigs["debug"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // src/main/resources/META-INF/xposed/* 需要原样打进 APK 根目录。
    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // 编译期可见，运行时由 LSPosed 框架提供。
    compileOnly(libs.libxposed.api)
    // 打进 APK：模块 App 通过它与框架通信（RemotePreferences）。
    implementation(libs.libxposed.service)

    // Jetpack Compose + Material Design 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
