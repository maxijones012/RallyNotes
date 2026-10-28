plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "ar.com.rallygpsai"
    compileSdk = 35
    defaultConfig { applicationId = "ar.com.rallygpsai"; minSdk = 26; targetSdk = 35; versionCode = 9; versionName = "0.2.7" }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}
dependencies {
    implementation("org.maplibre.gl:android-sdk:12.3.1")
    implementation("androidx.core:core-ktx:1.15.0")
    testImplementation("junit:junit:4.13.2")
}
