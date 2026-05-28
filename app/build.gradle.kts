plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartexpense"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartexpense"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.register("unitTestClasses") {
    dependsOn("compileDebugUnitTestSources")
    dependsOn("compileReleaseUnitTestSources")
}

tasks.register("androidTestClasses") {
    dependsOn("compileDebugAndroidTestSources")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.core:core:1.13.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
