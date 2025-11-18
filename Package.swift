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
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v0.15.0/MobileIDSDK.xcframework.zip",
            checksum: "ea8b5fbdcbdbfcc50c8a5a996112154c0575ca5ee2b002f16f152020b5168737")
    ]
)
