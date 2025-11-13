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
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v0.14.00/MobileIDSDK.xcframework.zip",
            checksum: "dff45f8caee90b59aa4fd991025497f8273ea0a60008224816a148d7307cea9c")
    ]
)
