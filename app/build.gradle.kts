plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

android {
    namespace = "com.acalc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.acalc"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        create("release") {
            storeFile     = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias      = "androiddebugkey"
            keyPassword   = "android"
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            signingConfig    = signingConfigs.getByName("release")
            isMinifyEnabled  = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Robolectric needs real resources + a merged manifest to inflate the Compose host activity.
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.material)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    // Compose UI tests run on the JVM via Robolectric — no emulator required.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

/**
 * `./gradlew coverageReport` → app/build/reports/jacoco/coverageReport/html/index.html
 *
 * Measures the JVM unit + Robolectric UI suites. Compose-generated synthetics and the
 * placeholder data layer are excluded so the number reflects hand-written logic.
 */
// Robolectric loads classes through its own sandbox classloader; without these two settings
// JaCoCo records nothing for anything the UI tests touch and every composable reads 0%.
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("coverageReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Coverage for the JVM unit and Robolectric UI tests"

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    val excludes = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "**/*\$\$serializer.*",
        "**/ComposableSingletons*.*", "**/*_Factory.*", "**/*Preview*.*",
    )

    // AGP 9 emits Kotlin classes under intermediates/built_in_kotlinc, not tmp/kotlin-classes.
    classDirectories.setFrom(
        layout.buildDirectory
            .dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
            .map { fileTree(it) { exclude(excludes) } }
    )
    sourceDirectories.setFrom(files("src/main/kotlin"))
    // Point at the exec file directly: scanning the whole build dir makes Gradle infer implicit
    // dependencies on unrelated tasks (dexBuilderDebug, compressDebugAssets, …) and fail.
    executionData.setFrom(
        layout.buildDirectory.file(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
        )
    )
}
