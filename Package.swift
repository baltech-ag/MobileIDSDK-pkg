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
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/raw/master/sdk/ios/v1.0.0/MobileIDSDK.xcframework.zip",
            checksum: "b5ce18e44c0150ba0aff3721ecebf1730850a3c7609594cafa49eae0b22bd13e")
    ]
)
