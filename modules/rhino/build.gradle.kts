plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.script"
    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        checkDependencies = true
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    tasks.withType(JavaCompile::class.java).configureEach {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

dependencies {
    // api(fileTree("dir" to "lib", "include" to listOf("rhino-1.7.14.jar")))
    api(libs.mozilla.rhino)

    implementation(libs.kotlinx.coroutines.core)

//    def coroutines_version = '1.7.3'
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
}