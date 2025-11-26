// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "MobileIDSdk",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "MobileIDSdk",
            targets: ["MobileIDSdk"])
    ],
    targets: [
        .binaryTarget(
            name: "MobileIDSdk",
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v1.2.0/MobileIDSDK.xcframework.zip",
            checksum: "1d84f20cbe2e821ea1b6654d99ac3a5bf9dab39441f9a00da1e735708e447208")
    ]
)
