rootProject.name = "MobileIDSDK-pkg"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":appnote-kotlin")
project(":appnote-kotlin").projectDir = file("appnote/kotlin")
