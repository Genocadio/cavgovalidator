plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.apollographql.apollo") version "4.1.0"  // Add Apollo plugin
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    kotlin("kapt")

}

secrets {
    // To add your Maps API key to this project:
    // 1. If the secrets.properties file does not exist, create it in the same folder as the local.properties file.
    // 2. Add this line, where YOUR_API_KEY is your API key:
    //        MAPS_API_KEY=YOUR_API_KEY
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"

    // Configure which keys should be ignored by the plugin by providing regular expressions.
    // "sdk.dir" is ignored by default.
    ignoreList.add("keyToIgnore") // Ignore the key "keyToIgnore"
    ignoreList.add("sdk.*")       // Ignore all keys matching the regexp "sdk.*"
}


android {
    namespace = "com.nexxserve.cavgodrivers"
    compileSdk = 35
//    viewBinding.isEnabled = true


    defaultConfig {
        applicationId = "com.nexxserve.cavgodrivers"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
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

    defaultConfig {
        resourceConfigurations += setOf("en")// Add this to enable Secrets injection
        resValue("string", "google_maps_key", project.findProperty("MAPS_API_KEY") as String? ?: "")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig= true
        compose = true
    }
}


apollo {
    service("service") {
        packageName.set("com.nexxserve.cavgodrivers")
        introspection {
            endpointUrl.set("http://localhost:4000/graphql")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}




dependencies {
    // Android dependencies
    implementation("com.apollographql.apollo3:apollo-coroutines-support:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.security.crypto)
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-database")
//    implementation("com.mapbox.navigationcore:ui-maps:3.5.2")
//    implementation("com.mapbox.navigationcore:android:3.5.2")
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.0")

//    implementation("com.mapbox.navigationcore:navigation:3.5.2")
    // Include .aar files from 'libs' directory
    implementation(files("src/main/libs/demoSDK_v2.19.20240620.aar"))
    implementation(files("src/main/libs/core-3.1.0.jar"))


    implementation("com.google.android.libraries.navigation:navigation:6.0.0")

    // Apollo GraphQL runtime
    implementation("com.apollographql.apollo:apollo-runtime:4.1.0")
    implementation(libs.firebase.firestore.ktx)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug dependencies for UI tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
