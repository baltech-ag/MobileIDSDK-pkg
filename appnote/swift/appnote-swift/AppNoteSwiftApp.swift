import SwiftUI
import MobileIDSdk

@main
struct AppNoteSwiftApp: App {
    private let sdk = MobileIdSdk()

    var body: some Scene {
        WindowGroup {
            ContentView(sdk: sdk)
        }
    }
}