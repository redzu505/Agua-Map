import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Leemos las credenciales de Supabase desde local.properties (no se versiona).
// Si no existen, usamos los valores por defecto para que el build nunca se rompa.
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val supabaseUrl: String =
    localProperties.getProperty("SUPABASE_URL") ?: "https://ivivwimqhgkdmdygdwns.supabase.co/"
val supabaseAnonKey: String =
    localProperties.getProperty("SUPABASE_ANON_KEY") ?: "sb_publishable_45XgBmKYWXiYz_tn2V8uOg_VK6OsUe0"

android {
    namespace = "com.aguamap.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aguamap.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Credenciales de Supabase expuestas de forma segura vía BuildConfig
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.graphics.path)
    implementation(libs.maplibre.android)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(libs.androidx.datastore.preferences)
    // Librería principal de Retrofit para conectar a internet
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Convertidor de Gson (para que Retrofit entienda los archivos JSON automáticamente)
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // OkHttp explícito (4.x) para subir imágenes a Supabase Storage con RequestBody
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Interceptor de logging: muestra en Logcat cada petición/respuesta a Supabase
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}