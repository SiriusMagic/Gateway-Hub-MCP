plugins {
    // Same versions as Stone: R8 of AGP 8.13 understands Kotlin 2.3 metadata (not 2.4).
    id("com.android.application") version "8.13.2" apply false
    kotlin("android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
