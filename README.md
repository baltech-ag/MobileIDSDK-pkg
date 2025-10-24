# BALTECH Mobile ID SDK - Distribution Package

This repository contains the pre-built distribution packages and sample applications for the BALTECH Mobile ID SDK.

## Repository Structure

```
MobileIDSDK-pkg/
├── Package.swift              # Swift Package Manager manifest
├── VERSION                    # Current SDK version
├── sdk/
│   ├── ios/                  # iOS SDK distribution
│   │   └── v{VERSION}/       # Versioned releases
│   │       └── MobileIDSdk.xcframework.zip
│   └── android/              # Android SDK artifacts (reference only, published to Maven Central)
│       └── sdk-release.aar
├── appnote/
│   ├── kotlin/               # Kotlin Multiplatform sample app source
│   └── swift/                # Swift iOS sample app source
└── README.md                  # This file
```

## Installation

### iOS (Swift Package Manager)

Add this package to your Xcode project:

1. In Xcode, go to **File > Add Package Dependencies...**
2. Enter the repository URL: `https://github.com/baltech-ag/MobileIDSDK-pkg`
3. Select the version you want to use
4. Add `MobileIDSdk` to your target

Or add to your `Package.swift`:

```swift
dependencies: [
    .package(url: "https://github.com/baltech-ag/MobileIDSDK-pkg", from: "0.1.0")
]
```

### Android (Gradle)

The Android SDK is published to Maven Central:

```kotlin
dependencies {
    implementation("de.baltech:sdk-android:VERSION")
}
```

Replace `VERSION` with the desired version from the [VERSION](VERSION) file.

## Sample Applications

### Kotlin Multiplatform AppNote

The Kotlin appnote demonstrates SDK usage in a Kotlin Multiplatform application for both Android and iOS.

**Building from this repository:**

```bash
cd appnote/kotlin
./gradlew assembleDebug
```

The appnote automatically uses the SDK from Maven Central when built standalone.

### Swift AppNote

The Swift appnote demonstrates SDK usage in a native iOS Swift application.

**Building from this repository:**

```bash
cd appnote/swift
./configure-for-standalone.sh  # Follow instructions to add SPM dependency
# Then open in Xcode and build
```

## Documentation

For full SDK documentation, API reference, and development guide, visit the main repository:
https://github.com/baltech-ag/MobileIDSDK

## Version Information

Current version: See [VERSION](VERSION) file

## License

See the main repository for license information.

## Development

This is a distribution repository. For SDK development, bug reports, or contributions, please visit:
https://github.com/baltech-ag/MobileIDSDK

---

*This repository is automatically updated by CI/CD from the main MobileIDSDK repository.*
