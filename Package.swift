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
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v0.09.00/MobileIDSDK.xcframework.zip",
            checksum: "e94164a3d22aa2a99768449e27f42cd269dc4b4720443a9e7d8e8000a30a490e")
    ]
)
