plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
    // +↓↓ 新增这一行（KSP 与 Kotlin 1.9.24 匹配的版本）
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
