plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.zirakusa.gatewayhub"
    compileSdk = 36
    ndkVersion = "29.0.13113456"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }
    defaultConfig {
        applicationId = "com.zirakusa.gatewayhub"
        minSdk = 26
        targetSdk = 35
        versionCode = 44
        versionName = "0.2.0"
        // Where llama.cpp's source already is (same pinned commit), for machines that can't download it: -PllamaSourceDir=…
        (project.findProperty("llamaSourceDir") as String?)?.let { dir ->
            externalNativeBuild { cmake { arguments += "-DFETCHCONTENT_SOURCE_DIR_LLAMA_CPP=$dir" } }
        }
        val hubUrl = (project.findProperty("gatewayHubUrl") as String?).orEmpty().trimEnd('/')
        buildConfigField("String", "DEFAULT_HUB_URL", "\"$hubUrl\"")
        // 64-bit ARM only, like Luma and Notas: the local model (llama.cpp) only exists there, and 32-bit Vosk added ~4 MB.
        ndk { abiFilters += "arm64-v8a" }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.systemProperty("roborazzi.test.record", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Installable build, signed like Stone's preview.
        create("preview") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Lucía: offline Spanish speech recognition (Vosk, Apache-2.0) with its small model in assets/model-es.
    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
}
