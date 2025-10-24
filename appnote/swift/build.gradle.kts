// Gradle build file for Swift AppNote
// This doesn't build the Swift app directly, but helps manage dependencies

// Detect if we're running as a subproject or standalone
val isSubproject = rootProject.name == "MobileIDSdk"

tasks.register("prepareSwiftFramework") {
    group = "build"
    description = "Ensures MobileIDSdk.xcframework is available for Swift app"

    doLast {
        val frameworkSource = if (isSubproject) {
            // Subproject mode: use framework from SDK build
            File(project.rootProject.projectDir, "sdk/build/XCFrameworks/debug/MobileIDSdk.xcframework")
        } else {
            // Standalone mode: expect framework in libs/ or use SPM
            File(project.projectDir, "../libs/MobileIDSdk.xcframework")
        }

        val frameworkTarget = File(project.projectDir, "../libs/MobileIDSdk.xcframework")

        if (frameworkSource.exists() && isSubproject) {
            // In subproject mode, we don't need to copy since Xcode references it directly
            println("✓ Using MobileIDSdk.xcframework from: ${frameworkSource.absolutePath}")
            println("  Xcode project references: ../sdk/build/XCFrameworks/debug/ (subproject mode)")
        } else if (frameworkTarget.exists()) {
            println("✓ Using MobileIDSdk.xcframework from: ${frameworkTarget.absolutePath}")
            println("  Xcode project references: ../libs/ (standalone mode)")
        } else {
            if (isSubproject) {
                println("⚠ MobileIDSdk.xcframework not found!")
                println("  Please build the SDK first:")
                println("  cd ${project.rootProject.projectDir}")
                println("  ./gradlew :sdk:assembleMobileIDSdkXCFramework")
            } else {
                println("ℹ️  Running in standalone mode")
                println("  Two options to use the SDK:")
                println("")
                println("  Option 1 (Recommended): Use Swift Package Manager")
                println("    Run: ./configure-for-standalone.sh")
                println("    This will guide you through adding the SPM package dependency")
                println("")
                println("  Option 2: Use local XCFramework")
                println("    Copy MobileIDSdk.xcframework to: ${frameworkTarget.absolutePath}")
                println("    Or run: ./configure-for-development.sh")
            }
        }
    }
}

tasks.register("buildSwiftApp") {
    group = "build"
    description = "Builds the Swift iOS app using xcodebuild"

    dependsOn("prepareSwiftFramework")

    doLast {
        exec {
            workingDir = file(".")
            commandLine(
                "xcodebuild",
                "-project", "appnote-swift.xcodeproj",
                "-scheme", "appnote-swift",
                "-destination", "platform=iOS Simulator,name=iPhone 15",
                "build"
            )
        }
    }
}

tasks.register("installSwiftApp") {
    group = "install"
    description = "Builds and installs the Swift app to a connected device"

    dependsOn("prepareSwiftFramework")

    doLast {
        println("Note: Use Xcode to install on physical devices")
        println("For simulator, use: ./gradlew :appnote:swift:buildSwiftApp")
    }
}
