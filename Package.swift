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
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v0.06.00/MobileIDSdk.xcframework.zip",
            checksum: "$CHECKSUM")
    ]
)
