// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //id 'com.android.application' version "$agp_version" apply false
    //id 'com.android.library' version "$agp_version" apply false
    //id 'org.jetbrains.kotlin.android' version "$kotlin_version" apply false
    //id 'com.google.devtools.ksp' version "$kotlin_version-$ksp_version" apply false
    //id "de.undercouch.download" version "5.5.0" apply false
    //id "com.google.gms.google-services" version "4.4.0" apply false

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.room) apply false

    alias(libs.plugins.download) apply false
}

tasks.register("clean",Delete::class.java){
    delete(rootProject.layout.buildDirectory)
}
