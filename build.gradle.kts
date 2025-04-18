// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
