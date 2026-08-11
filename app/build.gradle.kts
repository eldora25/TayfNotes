import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
}

val majorVersion = versionProps.getProperty("major.version") ?: "01"
val buildNumberFromFile = versionProps.getProperty("build.number") ?: "1"
val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: buildNumberFromFile
val fullVersionName = "$majorVersion.$buildNumber"
val buildNumberInt = buildNumber.toInt()

android {
    namespace = "com.eldora25.tayfnotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eldora25.tayfnotes"
        minSdk = 24
        targetSdk = 35
        versionCode = buildNumberInt
        versionName = fullVersionName

        buildConfigField("String", "BUILD_NO", "\"$buildNumber\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val mainOutput = output as? com.android.build.api.variant.impl.VariantOutputImpl
            mainOutput?.outputFileName?.set("TayfNotes_v$fullVersionName.apk")
        }
    }
}

tasks.register("incrementBuildNumber") {
    doLast {
        if (System.getenv("GITHUB_RUN_NUMBER") == null) {
            val currentBuildNumber = versionProps.getProperty("build.number").toInt()
            versionProps.setProperty("build.number", (currentBuildNumber + 1).toString())
            versionProps.store(versionPropsFile.outputStream(), null)
            println("Build number incremented to ${currentBuildNumber + 1}")
        }
    }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("incrementBuildNumber")
    tasks.findByName("assembleRelease")?.finalizedBy("incrementBuildNumber")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.google.api.services.drive)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Preferences & Security
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.coil.compose)
    
    // GDrive/Dropbox
    implementation(libs.google.api.services.drive)
    implementation(libs.dropbox.core)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
