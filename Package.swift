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
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v1.1.0/MobileIDSDK.xcframework.zip",
            checksum: "21b7eb838b5815eb3d188c3f256e10b7ddf1ee496323b09eb592bf20e238269e")
    ]
)
