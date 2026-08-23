import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")

}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.jeanotto.go4lunch"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jeanotto.go4lunch"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY", "")}\"")
        buildConfigField("String", "PLACES_API_KEY", "\"${localProperties.getProperty("PLACES_API_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // TODO: Re-enable R8/ProGuard (optimization.enable = true) once core features are
            // implemented and we've written keep-rules for Firebase/Gson. See CLAUDE.md "Known TODOs".
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Firebase BoM: automatically manages compatible versions between libraries
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase services needed for this project
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    // Credential Manager: Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // MVVM: ViewModel + LiveData
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)

    // Retrofit + Gson: Google Places API client
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)

    // Google Maps + FusedLocationProviderClient: map screen and current-location lookup
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
}