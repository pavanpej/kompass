import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
}

android {
    namespace = "com.pavanpej.kompass"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pavanpej.kompass"
        minSdk = 26
        targetSdk = 37
        // Overridable by the release workflow (-PreleaseVersionCode / -PreleaseVersionName,
        // derived from the git tag) so a signed release build doesn't need a manual version bump
        // committed to this file. Falls back to these defaults for local/debug builds.
        versionCode = (project.findProperty("releaseVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = project.findProperty("releaseVersionName") as String? ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Only configured when RELEASE_KEYSTORE_BASE64 is set (i.e. in the release CI job).
            // Local/debug builds and CI jobs without the secret simply produce an unsigned
            // release build instead of failing -- see docs/ARCHITECTURE.md.
            val keystoreBase64 = System.getenv("RELEASE_KEYSTORE_BASE64")
            if (keystoreBase64 != null) {
                val decodedKeystore = File(layout.buildDirectory.asFile.get(), "release-keystore.jks")
                decodedKeystore.parentFile.mkdirs()
                decodedKeystore.writeBytes(Base64.getDecoder().decode(keystoreBase64))
                storeFile = decodedKeystore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
            if (System.getenv("RELEASE_KEYSTORE_BASE64") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    classDirectories.setFrom(files(debugTree))
    // Scoped to exactly where testDebugUnitTest (an explicit dependsOn above) writes its
    // coverage data -- a build-directory-wide glob here trips Gradle's task-input validation,
    // since it would overlap with unrelated tasks' outputs (asset merging, dexing, etc.).
    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest")) {
            include("*.exec")
        }
    )
}
