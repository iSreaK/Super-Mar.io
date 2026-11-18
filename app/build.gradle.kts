import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}
// Trois identifiants publics ; aucun secret OAuth ni clé service_role dans l'APK.
val cloud = Properties().apply {
    val source = rootProject.file("supabase.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}
fun cloudValue(name: String) = cloud.getProperty(name, "").trim()
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val supabaseUrl = cloudValue("SUPABASE_URL").trimEnd('/')
val publishableKey = cloudValue("SUPABASE_PUBLISHABLE_KEY")
val webClientId = cloudValue("GOOGLE_WEB_CLIENT_ID")
val configured = listOf(supabaseUrl, publishableKey, webClientId).all { it.isNotBlank() }
require(publishableKey.isBlank() || publishableKey.startsWith("sb_publishable_")) {
    "Utiliser une clé Supabase sb_publishable_, jamais une clé secrète/service_role."
}
require(supabaseUrl.isBlank() || supabaseUrl.matches(Regex("https://[a-z0-9-]+[.]supabase[.]co"))) {
    "SUPABASE_URL doit être l'URL HTTPS du projet Supabase hébergé."
}
android {
    namespace = "fr.julie.mariosprint"
    compileSdk = 36
    defaultConfig {
        applicationId = "fr.julie.mariosprint"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3.0"
        testInstrumentationRunner = "fr.julie.mariosprint.SprintTestRunner"
        buildConfigField("boolean", "SUPABASE_CONFIGURED", configured.toString())
        buildConfigField("String", "SUPABASE_URL", quoted(supabaseUrl))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", quoted(publishableKey))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quoted(webClientId))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true; resValues = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.navigation.compose)
    implementation(libs.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit4)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.test.manifest)
    debugImplementation(libs.compose.tooling)
}
