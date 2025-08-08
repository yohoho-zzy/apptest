plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // ❌ 不再使用 KAPT：id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp") // ✅ 改用 KSP
}

android {
    namespace = "com.example.quotepicker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.quotepicker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    // 统一 Java 到 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material 主题
    implementation("com.google.android.material:material:1.12.0")

    // Room（用 KSP，而不是 KAPT）
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // 可选
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
}

// ✅ 用 KSP 传参，关掉 schema 校验（先快速出包；要开就把 false 改 true）
ksp {
    arg("room.verifySchema", "false")
    arg("room.schemaLocation", "$projectDir/schemas")
}
