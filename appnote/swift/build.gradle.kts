// Gradle build file for Swift AppNote
// This doesn't build the Swift app directly, but helps manage dependencies

// Detect if we're running as a subproject or standalone
val isSubproject = rootProject.name == "MobileIDSdk"

tasks.register("prepareSwiftFramework") {
    group = "build"
    description = "Ensures MobileIDSdk.xcframework is available for Swift app"

    // Capture project directories during configuration to avoid accessing project during execution
    val rootProjectDirectory = project.rootProject.projectDir
    val projectDirectory = project.projectDir

    doLast {
        val frameworkSource = if (isSubproject) {
            // Subproject mode: use framework from SDK build
            File(rootProjectDirectory, "sdk/build/XCFrameworks/debug/MobileIDSdk.xcframework")
        } else {
            // Standalone mode: expect framework in libs/ or use SPM
            File(projectDirectory, "../libs/MobileIDSdk.xcframework")
        }

        val frameworkTarget = File(projectDirectory, "../libs/MobileIDSdk.xcframework")

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
                println("  cd ${rootProjectDirectory}")
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

tasks.register("configureStandalone") {
    group = "build setup"
    description = "Creates standalone appnote-swift.xcodeproj from appnote-swift-dev.xcodeproj with SPM configuration"

    // Capture project directory during configuration to avoid accessing project during execution
    val projectDirectory = project.projectDir

    // Read version from VERSION file for SPM minimumVersion
    val versionFile = File(projectDirectory.parentFile.parentFile, "VERSION")
    val sdkVersion = if (versionFile.exists()) {
        versionFile.readText().trim()
    } else {
        "0.10.0" // fallback to current hardcoded version
    }

    doLast {
        val devProjectFile = File(projectDirectory, "appnote-swift-dev.xcodeproj/project.pbxproj")
        val standaloneProjectDir = File(projectDirectory, "appnote-swift.xcodeproj")
        val standaloneProjectFile = File(standaloneProjectDir, "project.pbxproj")

        if (!devProjectFile.exists()) {
            throw GradleException("Development project file not found: ${devProjectFile.absolutePath}")
        }

        // Create standalone project directory
        standaloneProjectDir.mkdirs()

        // Read dev project content
        var content = devProjectFile.readText()

        // 1. Replace project and target names from -dev to standalone
        content = content.replace("appnote-swift-dev", "appnote-swift")

        // 2. Remove local XCFramework file reference (ID: 7555FF82242A565900829871)
        val frameworkFileRef = """		7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/ = \{isa = PBXFileReference; lastKnownFileType = wrapper\.xcframework; name = MobileIDSdk\.xcframework; path = \.\./\.\./sdk/build/XCFrameworks/debug/MobileIDSdk\.xcframework; sourceTree = "<group>"; \};""".toRegex()
        content = content.replace(frameworkFileRef, "")

        // 3. Remove local framework from Frameworks group
        val frameworkInGroup = """				7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/,\n""".toRegex()
        content = content.replace(frameworkInGroup, "")

        // 4. Replace framework references in build files with SPM product references
        // First, update the PBXBuildFile entries
        content = content.replace(
            """17495DE58D31D0DDEA84AC27 /\* MobileIDSdk\.xcframework in Frameworks \*/ = \{isa = PBXBuildFile; fileRef = 7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/; \};""".toRegex(),
            """17495DE58D31D0DDEA84AC27 /* MobileIDSdk in Frameworks */ = {isa = PBXBuildFile; productRef = 17495DE58D31D0DDEA84AC29 /* MobileIDSdk */; };"""
        )
        content = content.replace(
            """17495DE58D31D0DDEA84AC28 /\* MobileIDSdk\.xcframework in Embed Frameworks \*/ = \{isa = PBXBuildFile; fileRef = 7555FF82242A565900829871 /\* MobileIDSdk\.xcframework \*/; settings = \{ATTRIBUTES = \(CodeSignOnCopy, RemoveHeadersOnCopy, \); \}; \};""".toRegex(),
            ""
        )

        // 5. Update references in Frameworks section and remove from Embed Frameworks section
        content = content.replace(
            """				17495DE58D31D0DDEA84AC27 /\* MobileIDSdk\.xcframework in Frameworks \*/,""".toRegex(),
            """				17495DE58D31D0DDEA84AC27 /* MobileIDSdk in Frameworks */,"""
        )
        // Remove the Embed Frameworks entry - must match exact format with tabs
        content = content.replace(
            "\t\t\t\t17495DE58D31D0DDEA84AC28 /* MobileIDSdk.xcframework in Embed Frameworks */,",
            ""
        )

        // 6. Add SPM package references inside PBXProject object (after mainGroup line)
        content = content.replace(
            "mainGroup = 7555FF72242A565900829871;",
            """mainGroup = 7555FF72242A565900829871;
			packageReferences = (
				17495DE58D31D0DDEA84AC30 /* XCRemoteSwiftPackageReference "MobileIDSDK-pkg" */,
			);"""
        )

        // 7. Add XCRemoteSwiftPackageReference and XCSwiftPackageProductDependency sections
        val spmSections = """

/* Begin XCRemoteSwiftPackageReference section */
		17495DE58D31D0DDEA84AC30 /* XCRemoteSwiftPackageReference "MobileIDSDK-pkg" */ = {
			isa = XCRemoteSwiftPackageReference;
			repositoryURL = "https://github.com/baltech-ag/MobileIDSDK-pkg.git";
			requirement = {
				kind = upToNextMajorVersion;
				minimumVersion = $sdkVersion;
			};
		};
/* End XCRemoteSwiftPackageReference section */

/* Begin XCSwiftPackageProductDependency section */
		17495DE58D31D0DDEA84AC29 /* MobileIDSdk */ = {
			isa = XCSwiftPackageProductDependency;
			package = 17495DE58D31D0DDEA84AC30 /* XCRemoteSwiftPackageReference "MobileIDSDK-pkg" */;
			productName = MobileIDSdk;
		};
/* End XCSwiftPackageProductDependency section */"""
        // Insert SPM sections after "/* End XCConfigurationList section */" and before the closing brace
        content = content.replace(
            "/* End XCConfigurationList section */",
            "/* End XCConfigurationList section */\n$spmSections"
        )

        // 9. Remove FRAMEWORK_SEARCH_PATHS with relative paths
        // Remove lines containing sdk/build in FRAMEWORK_SEARCH_PATHS
        content = content.lines().filterNot { it.contains("sdk/build/XCFrameworks") }.joinToString("\n") + "\n"
        // Remove FRAMEWORK_SEARCH_PATHS blocks that only have $(inherited) left
        content = content.replace(Regex("""				FRAMEWORK_SEARCH_PATHS = \(\n\t\t\t\t\t"\$\(inherited\)",\n\t\t\t\t\);\n"""), "")

        // 10. Remove OTHER_LDFLAGS with relative paths that reference sdk/build
        // Filter out lines containing CryptoKitBridge
        content = content.lines().filterNot { it.contains("CryptoKitBridge") }.joinToString("\n") + "\n"
        // Then remove empty OTHER_LDFLAGS blocks (those that only have $(inherited) left)
        content = content.replace(Regex("""\t\t\t\t"OTHER_LDFLAGS\[sdk=[^\]]+\](\[[^\]]+\])?" = \(\n\t\t\t\t\t"\$\(inherited\)",\n\t\t\t\t\);\n"""), "")
        // Simplify OTHER_LDFLAGS from array to string when it only has $(inherited)
        content = content.replace(
            """OTHER_LDFLAGS = (
					"$(inherited)",
				);""",
            """OTHER_LDFLAGS = "$(inherited)";"""
        )

        // Write standalone project
        standaloneProjectFile.writeText(content)

        println("✓ Created standalone project: ${standaloneProjectFile.absolutePath}")
        println("  - Configured to use SPM package from MobileIDSDK-pkg")
        println("  - Removed local SDK build dependencies")
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
                "-project", "appnote-swift-dev.xcodeproj",
                "-scheme", "appnote-swift-dev",
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
