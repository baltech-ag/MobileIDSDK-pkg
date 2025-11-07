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
            targets: ["MobileIDSdkBinary", "CryptoKitBridge"])
    ],
    targets: [
        .target(
            name: "CryptoKitBridge",
            path: "Sources/CryptoKitBridge",
            publicHeadersPath: "include"
        ),
        .binaryTarget(
            name: "MobileIDSdkBinary",
            url: "https://github.com/baltech-ag/MobileIDSDK-pkg/releases/download/0.10.0/MobileIDSDK.xcframework.zip",
            checksum: "34a9a92f8b08c1d2144d2b11049877d4cf6712129e8a5369e02394b45fbc53c8")
    ]
)
