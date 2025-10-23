# BALTECH Mobile ID SDK - Developer Integration Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Core Concepts](#core-concepts)
3. [Installation & Setup](#installation--setup)
4. [Public API Reference](#public-api-reference)
5. [Basic Usage Examples](#basic-usage-examples)
6. [Common Integration Patterns](#common-integration-patterns)
7. [Advanced Topics](#advanced-topics)
8. [Troubleshooting](#troubleshooting)
9. [API Compatibility](#api-compatibility)
10. [Migration & Updates](#migration--updates)
11. [Support & Resources](#support--resources)

---

## 1. Introduction

### What is the BALTECH Mobile ID SDK

The BALTECH Mobile ID SDK is a library that provides BLE-based access control capabilities. The SDK enables mobile devices to act as secure credentials for door opening, time & attendance, and other access control applications by transferring secure credentials via Bluetooth Low Energy (BLE) to ID-engine Z-based readers.

### Key Features

- **Cross-Platform**: Supporting both Android and iOS
- **BLE-Based**: Uses Bluetooth Low Energy for communication with readers
- **Secure**: Cryptographic signed credentials transferred over AES128-GCM connection
- **Automatic Permission Handling**: SDK manages BLE permissions automatically
- **Background Operation**: Supports background BLE advertising on iOS
- **Built-in Logging**: Comprehensive logging allows Baltech to support even complex problems
- **Reactive APIs**: APIs for modern reactive programming

### Reference Applications

Complete working examples are available to help you get started:

- **Swift AppNote**: Demonstrates iOS integration using Swift and UIKit
- **Kotlin AppNote**: Demonstrates Android integration using Kotlin and Compose
- Both AppNotes showcase best practices and complete integration workflows

### Minimum Requirements

- **iOS**:
  - iOS 13.0+
  - Xcode with Swift support
  - Physical device with BLE support (BLE not available in simulator)

- **Android**:
  - Android SDK 28+
  - Android Studio with Kotlin support
  - Device with BLE hardware support

---

## 2. Core Concepts

### Credentials

Credentials are the core security component of the Mobile ID system.

**Project Key**: A 16-byte master key shared across a project. This key is used to derive device-specific credentials.

**Credential ID**: An ASCII string identifier that uniquely identifies a credential within a project (currently only employee ID).

**How It Works**:
1. The project key and credential ID are used to derive three components:
   - `keyComm`: Communication key (16 bytes)
   - `keyInitblock`: Initialization block key (16 bytes)
   - `credentialBytes`: Signed credential with AES-CMAC (variable length)

2. These derived components are used for secure BLE communication with readers.

### Security Best Practice: Server-Side Credential Derivation

**IMPORTANT**: For maximum security, credentials should be derived on your backend server, not on the mobile device.

**Why?**
- Prevents mobile devices from simulating other device IDs
- Project key never stored on mobile devices
- Each device receives unique credentials derived from device ID
- Compromised device cannot generate credentials for other devices

See section 6.2 for implementation details and pseudocode.

Attention: As the Appnotes do not utilize a server they generate the credential in the app.

### BLE Protocol Overview

The SDK implements a custom BLE protocol for secure communication:
- Mobile device acts as BLE peripheral
- Readers act as BLE central devices
- Encrypted message exchange using derived keys
- A reader may accept multiple projects (=Project Keys)
- A mobile phone can (currently) only be configured with one credential

### Readers/Gates

**Reader**: Represents a physical smartcard reader connected to an access control system.

**Gate**: Represents a logical access point (door, gate, turnstile) associated with a reader.

**Remote Trigger**: When a reader is in range and authenticated, it appears in the `readers` list. You can manually trigger it by calling the `trigger()` function.

### Availability States

The SDK monitors Bluetooth and permission status through availability states:

- `UNDEFINED`: Initial/unknown state before initialization
- `UNSUPPORTED`: BLE hardware not supported on device
- `DISABLED`: Bluetooth is turned off
- `UNAUTHORIZED`: App not authorized to use Bluetooth
- `UNKNOWN`: Unknown BLE state
- `OK`: Ready for operation
- `PERMISSIONS_DENIED`: Permissions denied but can be requested again
- `PERMISSIONS_PERMANENTLY_DENIED`: Permissions permanently denied, must use settings
- `PERMISSIONS_REQUIRED`: Permissions need to be requested

---

## 3. Installation & Setup

### 3.1 Android Integration (Kotlin/Java)

#### Gradle Configuration

**Step 1**: Add the SDK dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("de.baltech:sdk-android:0.01.00")
}
```

**Step 2**: Configure Java compatibility settings:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}
```

#### AndroidManifest.xml Configuration

Add the following permissions to your `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Bluetooth permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

    <!-- Location permissions required for BLE scanning -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <!-- Android 12+ BLE permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

    <!-- Declare BLE hardware requirement -->
    <uses-feature
        android:name="android.hardware.bluetooth_le"
        android:required="true"/>

    <application>
        <!-- Your application configuration -->
    </application>
</manifest>
```

**Important Notes**:
- Location permissions (`ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`) are required for BLE scanning on Android
- Android 12+ requires `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, and `BLUETOOTH_ADVERTISE`
- The SDK automatically handles permission requests

### 3.2 iOS Integration (Swift/Objective-C)

#### XCode Project Setup

**Step 1**: Add the MobileIDSdk.xcframework to your project
1. Drag `MobileIDSdk.xcframework` into your Xcode project
2. Ensure it's added to "Frameworks, Libraries, and Embedded Content"
3. Select "Embed & Sign" or "Embed Without Signing" based on your needs

**Step 2**: Configure Framework Search Paths
1. Go to Build Settings
2. Search for "Framework Search Paths"
3. Add the path to the directory containing `MobileIDSdk.xcframework`

#### Critical: OTHER_LDFLAGS Configuration

**IMPORTANT**: This step is mandatory for the SDK to work correctly on iOS.

Add the following to your target's "Other Linker Flags" (OTHER_LDFLAGS):

```
-force_load $(PROJECT_DIR)/path/to/MobileIDSdk.framework/MobileIDSdk
```

Replace `path/to/` with the actual path to your framework.

**Why is this needed?**
The SDK uses CryptoKit bridge symbols that must be force-loaded from the static framework. Without this flag, you will experience runtime crashes.

**How to add in Xcode**:
1. Select your target
2. Go to Build Settings
3. Search for "Other Linker Flags"
4. Add the `-force_load` line with your framework path

#### Info.plist Configuration

Add the following keys to your `Info.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Bluetooth usage descriptions (required) -->
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>This app uses Bluetooth to communicate with access control readers for secure credential transfer.</string>

    <key>NSBluetoothPeripheralUsageDescription</key>
    <string>This app acts as a Bluetooth peripheral to transfer credentials to access control readers.</string>

    <!-- Required device capabilities -->
    <key>UIRequiredDeviceCapabilities</key>
    <array>
        <string>bluetooth-le</string>
    </array>

    <!-- Background modes for BLE -->
    <key>UIBackgroundModes</key>
    <array>
        <string>bluetooth-peripheral</string>
    </array>
</dict>
</plist>
```

**Important Notes**:
- Usage descriptions are shown to users when requesting Bluetooth permissions
- Customize the description text to match your app's use case
- `bluetooth-peripheral` background mode enables background BLE operation

---

## 4. Public API Reference

### 4.1 MobileIdSdk Class

The main SDK class providing all functionality for Mobile ID operations.

#### Initialization

```kotlin
val sdk = MobileIdSdk()
```

**Parameters**: None

**Usage**: Create a single instance of `MobileIdSdk` and retain it for the lifetime of your app. The SDK is thread-safe.

#### Properties

##### credentials: List&lt;Credential&gt;

```kotlin
var credentials: List<Credential>
```

**Description**: Gets or sets the list of active credentials. Currently supports 0 or 1 credential only.

**Behavior**:
- Setting credentials to empty list stops the BLE protocol
- Setting credentials to a list with 1 item starts the BLE protocol
- Setting the same credential again is a no-op
- Setting a different credential stops and restarts the protocol

**Example**:
```kotlin
// Activate a credential
sdk.credentials = listOf(credential)

// Deactivate (stop BLE protocol)
sdk.credentials = emptyList()
```

##### readers: List&lt;Reader&gt;

```kotlin
val readers: List<Reader>
```

**Description**: Read-only list of currently available readers/gates in range.

**Behavior**:
- Updated automatically as readers come in/out of range
- Triggers `onReadersUpdate` callback when changed
- Empty when no credentials are active or no readers in range

##### availability: Availability

```kotlin
val availability: Availability
```

**Description**: Current Bluetooth and permission availability status.

**Return Type**: `Availability` (String typealias)

**Possible Values**: See `AvailabilityStates` object for all values.

##### availabilityFlow: StateFlow&lt;Availability&gt;

```kotlin
val availabilityFlow: StateFlow<Availability>
```

**Description**: Reactive flow for observing availability changes.

**Usage**:
```kotlin
// In a coroutine or Compose
lifecycleScope.launch {
    sdk.availabilityFlow.collect { availability ->
        // Handle availability change
    }
}
```

##### debugMode: Boolean

```kotlin
var debugMode: Boolean
```

**Description**: Enable or disable debug console logging.

**Default**: `SdkBuildConfig.isDebug` (true in debug builds)

**Behavior**: When enabled, logs are printed to console via `println()`

##### logger: (String) -&gt; Unit

```kotlin
var logger: (String) -> Unit
```

**Description**: Custom log message handler.

**Default**: Adds messages to `logHandler`

**Usage**: Override to integrate with your own logging system
```kotlin
sdk.logger = { message ->
    MyLogger.log(message)
}
```

##### logHandler: LogHandler

```kotlin
val logHandler: LogHandler
```

**Description**: Access to the SDK's internal log handler.

**Properties**:
- `debugMode: Boolean` - Enable/disable console output
- `getLogs(sinceId: Int?): List<LogEntry>` - Retrieve log entries
- `getFormattedLogs(): String` - Get all logs as formatted string
- `clearLogs()` - Clear all log entries
- `latestLogIdFlow: StateFlow<Int>` - Reactive flow for new logs

#### Callbacks

##### onReadersUpdate: (() -&gt; Unit)?

```kotlin
var onReadersUpdate: (() -> Unit)?
```

**Description**: Called when the list of readers changes.

**Usage**:
```kotlin
sdk.onReadersUpdate = {
    val readers = sdk.readers
    // Update UI with new reader list
}
```

##### onAvailabilityChange: ((Availability) -&gt; Unit)?

```kotlin
var onAvailabilityChange: ((Availability) -> Unit)?
```

**Description**: Called when Bluetooth/permission availability status changes.

**Usage**:
```kotlin
sdk.onAvailabilityChange = { availability ->
    when (availability) {
        AvailabilityStates.OK -> // Ready
        AvailabilityStates.DISABLED -> // Show "turn on Bluetooth"
        // ... handle other states
    }
}
```

#### Methods

##### requestPermissions(): Boolean

```kotlin
suspend fun requestPermissions(): Boolean
```

**Description**: Manually request BLE permissions.

**Returns**: `true` if permissions were granted, `false` otherwise

**Important**: This method is **optional** in most cases. The SDK automatically requests permissions when you set credentials. Use this method only when:
- You want explicit control over when permission dialogs appear
- User denied permissions initially and you want to retry with explanation
- You want to request permissions before setting credentials

**Example**:
```kotlin
lifecycleScope.launch {
    if (!sdk.requestPermissions()) {
        // Permissions denied, show explanation
    }
}
```

##### openPermissionSettings(): Boolean

```kotlin
suspend fun openPermissionSettings(): Boolean
```

**Description**: Opens system settings where users can manually grant permissions.

**Returns**: `true` if settings were opened successfully, `false` otherwise

**When to use**: Call when permissions are permanently denied (`PERMISSIONS_PERMANENTLY_DENIED`) and user needs to manually enable in system settings.

**Typical flow**:
1. Check `availability` state
2. If `PERMISSIONS_PERMANENTLY_DENIED`, show explanation dialog
3. Call this method to open settings
4. User grants permissions and returns to app

**Example**:
```kotlin
if (availability == AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED) {
    // Show dialog explaining why permissions needed
    sdk.openPermissionSettings()
}
```

##### sendLogs(subject: String?, message: String?)

```kotlin
suspend fun sendLogs(subject: String? = null, message: String? = null)
```

**Description**: Send logs via email using the system sharing functionality.

**STRONGLY RECOMMENDED**: Always integrate this method in your app. It enables BALTECH support to diagnose issues effectively.

**Parameters**:
- `subject`: Optional custom email subject (default: "BALTECH Mobile ID support request")
- `message`: Optional custom message before device info (default: "Mobile ID Log Report")

**Behavior**:
- Opens email client with pre-filled recipient (support@baltech.de)
- Attaches logs as `mobile-id-logs.txt`
- Includes device information (OS, version, device model, SDK version)
- Throws `IllegalStateException` if no logs available

**Example**:
```kotlin
// In your "Contact Support" or "Report Issue" button
lifecycleScope.launch {
    try {
        sdk.sendLogs()
    } catch (e: IllegalStateException) {
        // No logs available
    }
}
```

**Integration Recommendation**:
```kotlin
// Add to app settings or help menu
Button("Report Issue") {
    scope.launch {
        sdk.sendLogs(
            subject = "Issue with MyApp",
            message = "User encountered problem: ..."
        )
    }
}
```

##### createLogView() [Android/Compose]

```kotlin
@Composable
fun MobileIdSdk.createLogView()
```

**Platform**: Android with Compose

**Description**: Creates a Compose-based log viewer UI.

**Status**: **Optional** - Not required but improves support quality

**Benefits**:
- Users can see real-time SDK activity
- Support team can request screenshots
- Helps advanced users troubleshoot

**Example**:
```kotlin
@Composable
fun LogScreen(sdk: MobileIdSdk) {
    Box(modifier = Modifier.fillMaxSize()) {
        sdk.createLogView()
    }
}
```

##### createLogViewController() [iOS/Swift]

```kotlin
fun MobileIdSdk.createLogViewController(): UIViewController
```

**Platform**: iOS

**Description**: Creates a UIViewController with log viewer UI.

**Status**: **Optional** - Not required but improves support quality

**Example (Swift)**:
```swift
let logViewController = sdk.createLogViewController()
navigationController?.pushViewController(logViewController, animated: true)
```

### 4.2 Credential Data Class

Represents credential components for Mobile ID operations.

#### Properties

```kotlin
data class Credential(
    val keyComm: ByteArray,              // 16 bytes - Communication key
    val keyInitblock: ByteArray,         // 16 bytes - Init block key
    val credentialBytes: ByteArray,      // Variable - Signed credential
    var onTrigger: (() -> Boolean)? = null  // Optional trigger callback
)
```

- **keyComm**: Derived communication key used for encrypted messaging
- **keyInitblock**: Derived initialization block key
- **credentialBytes**: Complete signed credential structure with AES-CMAC
- **onTrigger**: Optional callback for trigger authorization (return true to allow, false to deny)

#### Companion Object: create()

```kotlin
fun Credential.Companion.create(
    projectKey: ByteArray,
    credentialId: String,
    onTrigger: (() -> Boolean)? = null
): Credential
```

**Description**: Creates credential components from a project key and credential ID.

**Parameters**:
- `projectKey`: 16-byte project-wide master key
- `credentialId`: ASCII string identifier for this credential
- `onTrigger`: Optional callback for trigger authorization

**Returns**: Fully initialized `Credential` object ready for use

**Process**:
1. Derives `keyInitblock` from SHA256(projectKey)
2. Builds credential structure with credential ID
3. Signs with AES-CMAC using project key
4. Derives `keyComm` from project key and credential hash

**Example**:
```kotlin
val projectKey = "00112233445566778899AABBCCDDEEFF".hexToByteArray()
val credential = Credential.create(
    projectKey = projectKey,
    credentialId = "employee_001"
)
sdk.credentials = listOf(credential)
```

**Security Note**: See section 6.2 for recommended server-side derivation approach.

### 4.3 Reader Data Class

Represents a smartcard reader/gate that is currently in range.

```kotlin
data class Reader(
    val displayName: String,    // Name of the gate
    val trigger: () -> Unit     // Callback to trigger/open gate
)
```

#### Properties

- **displayName**: Human-readable name of the gate (e.g., "Main Entrance", "Office Door")
- **trigger**: Function to manually trigger the gate/door opening

#### Usage

```kotlin
sdk.onReadersUpdate = {
    sdk.readers.forEach { reader ->
        println("Reader available: ${reader.displayName}")
    }
}

// Trigger first available reader
sdk.readers.firstOrNull()?.trigger()
```

### 4.4 AvailabilityStates Object

Constants for all possible availability states.

```kotlin
object AvailabilityStates {
    const val UNDEFINED = "undefined"
    const val UNSUPPORTED = "unsupported"
    const val DISABLED = "disabled"
    const val UNAUTHORIZED = "unauthorized"
    const val UNKNOWN = "unknown"
    const val OK = "ok"
    const val PERMISSIONS_DENIED = "permissions_denied"
    const val PERMISSIONS_PERMANENTLY_DENIED = "permissions_permanently_denied"
    const val PERMISSIONS_REQUIRED = "permissions_required"
}
```

#### State Descriptions

- **UNDEFINED**: Initial state before SDK initialization or when no credentials set
- **UNSUPPORTED**: BLE hardware not available on device
- **DISABLED**: Bluetooth is turned off (user should enable it)
- **UNAUTHORIZED**: App not authorized to use Bluetooth
- **UNKNOWN**: BLE state cannot be determined
- **OK**: Everything ready, BLE operational
- **PERMISSIONS_DENIED**: Permissions denied but can request again
- **PERMISSIONS_PERMANENTLY_DENIED**: Permissions permanently denied, must use settings
- **PERMISSIONS_REQUIRED**: Permissions need to be requested (transient state)

---

## 5. Basic Usage Examples

### 5.1 Kotlin (Android) Example

#### Basic Setup and Initialization

```kotlin
import de.baltech.mobileid.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sdk: MobileIdSdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SDK
        sdk = MobileIdSdk()

        // Enable debug logging for development
        sdk.debugMode = true

        // Set up callbacks
        setupCallbacks()

        // Create and activate credential
        activateCredential()
    }

    private fun setupCallbacks() {
        // Monitor availability changes
        sdk.onAvailabilityChange = { availability ->
            handleAvailabilityChange(availability)
        }

        // Monitor reader updates
        sdk.onReadersUpdate = {
            val readers = sdk.readers
            println("Readers available: ${readers.size}")
        }
    }

    private fun activateCredential() {
        lifecycleScope.launch {
            val projectKey = "00112233445566778899AABBCCDDEEFF".hexToByteArray()
            val credential = Credential.create(
                projectKey = projectKey,
                credentialId = "user_001"
            )
            sdk.credentials = listOf(credential)
        }
    }

    private fun handleAvailabilityChange(availability: Availability) {
        when (availability) {
            AvailabilityStates.OK -> {
                // Ready to use
                println("SDK ready")
            }
            AvailabilityStates.DISABLED -> {
                // Show "Please enable Bluetooth"
                showBluetoothDisabledMessage()
            }
            AvailabilityStates.PERMISSIONS_DENIED,
            AvailabilityStates.PERMISSIONS_REQUIRED -> {
                // SDK will automatically request permissions
                // Or manually request if preferred
                lifecycleScope.launch {
                    sdk.requestPermissions()
                }
            }
            AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> {
                // Must use settings
                showPermissionPermanentlyDeniedDialog()
            }
        }
    }

    private fun showPermissionPermanentlyDeniedDialog() {
        // Show dialog explaining why permissions needed
        AlertDialog.Builder(this)
            .setTitle("Bluetooth Permissions Required")
            .setMessage("Please enable Bluetooth permissions in Settings to use Mobile ID")
            .setPositiveButton("Open Settings") { _, _ ->
                lifecycleScope.launch {
                    sdk.openPermissionSettings()
                }
            }
            .show()
    }
}

// Utility extension
fun String.hexToByteArray(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
```

#### Using Compose UI with Log Viewer

```kotlin
@Composable
fun App() {
    val sdk = remember { MobileIdSdk() }
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                sdk = sdk,
                onViewLogs = { currentScreen = Screen.LOGS }
            )
        }
        Screen.LOGS -> {
            LogViewerScreen(sdk = sdk)
        }
    }
}

@Composable
fun LogViewerScreen(sdk: MobileIdSdk) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SDK Logs") })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            sdk.createLogView()  // Optional: Shows SDK logs
        }
    }
}
```

#### Implementing Support Integration

```kotlin
@Composable
fun SupportButton(sdk: MobileIdSdk) {
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                try {
                    sdk.sendLogs(
                        subject = "MyApp Support Request",
                        message = "User encountered issue with access control"
                    )
                } catch (e: IllegalStateException) {
                    // No logs available
                }
            }
        }
    ) {
        Text("Contact Support")
    }
}
```

#### Triggering Readers

```kotlin
@Composable
fun ReaderList(sdk: MobileIdSdk) {
    var readers by remember { mutableStateOf<List<Reader>>(emptyList()) }

    LaunchedEffect(Unit) {
        sdk.onReadersUpdate = {
            readers = sdk.readers
        }
    }

    LazyColumn {
        items(readers) { reader ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { reader.trigger() }
                    .padding(8.dp)
            ) {
                Text(
                    text = reader.displayName,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

### 5.2 Swift (iOS) Example

#### Basic Setup and Initialization

```swift
import SwiftUI
import MobileIDSdk

class SDKManager: ObservableObject {
    let sdk: MobileIdSdk
    @Published var availability: String = AvailabilityStates.shared.UNDEFINED
    @Published var readers: [Reader] = []

    init() {
        sdk = MobileIdSdk()

        // Enable debug logging
        sdk.debugMode = true

        // Set up callbacks
        setupCallbacks()
    }

    private func setupCallbacks() {
        // Monitor availability
        sdk.onAvailabilityChange = { [weak self] newAvailability in
            DispatchQueue.main.async {
                self?.availability = newAvailability
                self?.handleAvailabilityChange(newAvailability)
            }
        }

        // Monitor readers
        sdk.onReadersUpdate = { [weak self] in
            DispatchQueue.main.async {
                self?.readers = self?.sdk.readers ?? []
            }
        }
    }

    func activateCredential() {
        let projectKeyHex = "00112233445566778899AABBCCDDEEFF"
        guard let projectKey = try? projectKeyHex.hexToByteArray() else {
            return
        }

        let credential = Credential.Companion().create(
            projectKey: projectKey.toKotlinByteArray(),
            credentialId: "user_001",
            onTrigger: nil
        )

        sdk.credentials = [credential]
    }

    private func handleAvailabilityChange(_ availability: String) {
        let states = AvailabilityStates.shared

        switch availability {
        case states.OK:
            print("SDK ready")

        case states.DISABLED:
            showAlert("Please enable Bluetooth")

        case states.PERMISSIONS_DENIED, states.PERMISSIONS_REQUIRED:
            // SDK automatically requests - or manually request
            Task {
                try? await sdk.requestPermissions()
            }

        case states.PERMISSIONS_PERMANENTLY_DENIED:
            showPermissionSettings()

        default:
            print("Availability: \(availability)")
        }
    }

    private func showPermissionSettings() {
        // Show alert with option to open settings
        Task {
            try? await sdk.openPermissionSettings()
        }
    }

    func sendSupportEmail() {
        Task {
            try? await sdk.sendLogs(
                subject: "MyApp Support Request",
                message: "User needs assistance"
            )
        }
    }
}

// Utility extension for hex conversion
extension String {
    func hexToByteArray() throws -> [UInt8] {
        var bytes = [UInt8]()
        var index = startIndex
        while index < endIndex {
            let nextIndex = self.index(index, offsetBy: 2)
            let byteString = self[index..<nextIndex]
            guard let byte = UInt8(byteString, radix: 16) else {
                throw NSError(domain: "Invalid hex", code: -1)
            }
            bytes.append(byte)
            index = nextIndex
        }
        return bytes
    }
}

extension Array where Element == UInt8 {
    func toKotlinByteArray() -> KotlinByteArray {
        let data = Data(self)
        return KotlinByteArray(size: Int32(data.count)) { index in
            return Int8(bitPattern: data[Int(truncating: index)])
        }
    }
}
```

#### SwiftUI View Integration

```swift
struct ContentView: View {
    @StateObject private var sdkManager = SDKManager()

    var body: some View {
        NavigationView {
            VStack {
                // Availability Status
                AvailabilityStatusView(availability: sdkManager.availability)

                // Reader List
                List(sdkManager.readers, id: \.displayName) { reader in
                    Button(action: {
                        reader.trigger()
                    }) {
                        Text(reader.displayName)
                    }
                }

                // Support Button
                Button("Contact Support") {
                    sdkManager.sendSupportEmail()
                }
                .padding()
            }
            .navigationTitle("Mobile ID")
            .onAppear {
                sdkManager.activateCredential()
            }
        }
    }
}

struct AvailabilityStatusView: View {
    let availability: String

    var body: some View {
        HStack {
            Text("Status:")
            Text(availability)
                .fontWeight(.bold)
                .foregroundColor(statusColor)
        }
        .padding()
    }

    var statusColor: Color {
        let states = AvailabilityStates.shared
        switch availability {
        case states.OK:
            return .green
        case states.DISABLED, states.UNAUTHORIZED:
            return .red
        case states.PERMISSIONS_DENIED, states.PERMISSIONS_REQUIRED:
            return .orange
        default:
            return .gray
        }
    }
}
```

#### Using Log Viewer (Optional)

```swift
struct LogViewerView: View {
    let sdk: MobileIdSdk

    var body: some View {
        NavigationView {
            LogViewControllerWrapper(
                viewController: sdk.createLogViewController()
            )
            .navigationTitle("SDK Logs")
        }
    }
}

struct LogViewControllerWrapper: UIViewControllerRepresentable {
    let viewController: UIViewController

    func makeUIViewController(context: Context) -> UIViewController {
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed
    }
}
```

---

## 6. Common Integration Patterns

### 6.1 Permission Handling Flow

The SDK provides both automatic and manual permission handling.

#### Automatic Permission Request (Default Behavior)

The SDK automatically handles permissions for you:

1. When you set `credentials`, the SDK checks permission status
2. If permissions not granted, SDK automatically requests them
3. `availabilityFlow` updates to reflect permission status
4. No manual intervention required in most cases

```kotlin
// This triggers automatic permission check and request
sdk.credentials = listOf(credential)

// Monitor the result via availability
sdk.onAvailabilityChange = { availability ->
    when (availability) {
        AvailabilityStates.OK -> // Permissions granted, SDK ready
        AvailabilityStates.PERMISSIONS_DENIED -> // User denied
        AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> // Permanently denied
    }
}
```

**Recommendation**: Let the SDK handle permissions automatically unless you have specific UX requirements.

#### Manual Permission Control (Optional)

Use manual control when you want explicit control over permission flow:

**Use Cases**:
- Show explanation dialog before requesting permissions
- Retry after user initially denied
- Request permissions before setting credentials
- Custom permission explanation UI

**Example: Pre-request with explanation**
```kotlin
fun requestPermissionsWithExplanation() {
    lifecycleScope.launch {
        // Show custom explanation dialog first
        showDialog("We need Bluetooth to connect to access control readers")

        // Then request permissions
        val granted = sdk.requestPermissions()

        if (granted) {
            // Proceed to set credentials
            sdk.credentials = listOf(credential)
        } else {
            // Handle denial
            handlePermissionDenied()
        }
    }
}
```

**Example: Retry after denial**
```kotlin
sdk.onAvailabilityChange = { availability ->
    when (availability) {
        AvailabilityStates.PERMISSIONS_DENIED -> {
            // Show explanation and retry
            showExplanationDialog(
                onRetry = {
                    lifecycleScope.launch {
                        sdk.requestPermissions()
                    }
                }
            )
        }
    }
}
```

#### Handling Different Permission States

```kotlin
fun handlePermissionState(availability: Availability) {
    when (availability) {
        AvailabilityStates.PERMISSIONS_REQUIRED -> {
            // Transient state - SDK is requesting
            showLoadingIndicator()
        }

        AvailabilityStates.PERMISSIONS_DENIED -> {
            // User denied - can request again
            showRetryButton {
                lifecycleScope.launch {
                    sdk.requestPermissions()
                }
            }
        }

        AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> {
            // Must use settings
            showSettingsDialog {
                lifecycleScope.launch {
                    sdk.openPermissionSettings()
                }
            }
        }

        AvailabilityStates.OK -> {
            // All good
            hidePermissionUI()
        }
    }
}
```

#### iOS Permission Flow

```swift
func handleAvailability(_ availability: String) {
    let states = AvailabilityStates.shared

    switch availability {
    case states.PERMISSIONS_DENIED, states.PERMISSIONS_REQUIRED:
        // SDK auto-requests or manually retry
        Task {
            let granted = try? await sdk.requestPermissions()
            if granted == false {
                showPermissionExplanation()
            }
        }

    case states.PERMISSIONS_PERMANENTLY_DENIED:
        // Show alert with settings option
        showAlert(
            title: "Permissions Required",
            message: "Please enable Bluetooth in Settings",
            primaryButton: .default(Text("Open Settings")) {
                Task {
                    try? await sdk.openPermissionSettings()
                }
            },
            secondaryButton: .cancel()
        )

    case states.OK:
        // Ready to use
        break

    default:
        break
    }
}
```

### 6.2 Server-Side Credential Derivation (Security Best Practice)

**STRONGLY RECOMMENDED**: Derive credentials on your backend server rather than on mobile devices.

#### Why Server-Side Derivation?

1. **Security**: Project key never stored on mobile devices
2. **Device Isolation**: Compromised device cannot generate credentials for other devices
3. **Device-Specific Keys**: Each device gets unique credentials based on device ID
4. **Auditability**: Server logs all credential requests
5. **Revocation**: Easy to revoke specific device credentials

#### Implementation Architecture

```
Mobile Device                Backend Server              Access Control System
     |                            |                              |
     |-- 1. Get Device ID ------->|                              |
     |                            |                              |
     |<-- 2. Request Credential --|                              |
     |    (deviceId, credentialId)|                              |
     |                            |                              |
     |                            |-- Load Project Key           |
     |                            |   (from secure storage)      |
     |                            |                              |
     |                            |-- Derive Device Key          |
     |                            |   = SHA256(projectKey +      |
     |                            |            deviceId)[0:16]   |
     |                            |                              |
     |                            |-- Create Credential          |
     |                            |   using SDK logic            |
     |                            |                              |
     |<-- 3. Return Credential ---|                              |
     |    (keyComm, keyInitblock, |                              |
     |     credentialBytes)       |                              |
     |                            |                              |
     |-- 4. Use Credential ---------------------------------->   |
     |                                                            |
```

#### Backend Implementation (Pseudocode)

```python
# Backend API Endpoint
@app.post("/api/credentials/derive")
async def derive_credential(request: CredentialRequest):
    """
    Derives a device-specific credential from the project master key.

    Args:
        deviceId: Unique device identifier (UUID, IMEI, etc.)
        credentialId: User/credential identifier (employee ID, etc.)

    Returns:
        Credential components (keyComm, keyInitblock, credentialBytes)
    """

    # 1. Validate request
    if not is_valid_device(request.deviceId):
        raise Unauthorized("Invalid device")

    if not is_authorized_user(request.credentialId):
        raise Unauthorized("User not authorized")

    # 2. Get project master key from secure storage
    #    (e.g., AWS Secrets Manager, Azure Key Vault, HSM)
    projectKey = get_project_key_from_secure_storage()  # 16 bytes

    # 3. Derive device-specific key
    #    This ensures each device has unique key
    deviceSpecificData = projectKey + request.deviceId.encode('utf-8')
    deviceSpecificKey = SHA256(deviceSpecificData)[0:16]  # First 16 bytes

    # 4. Create credential using SDK logic
    #    (replicate Credential.create() logic on server)

    # Step 4a: Derive keyInitblock
    keyInitblock = SHA256(deviceSpecificKey)[0:16]

    # Step 4b: Build credential structure
    credentialIdBytes = request.credentialId.encode('ascii')
    headerSize = 2 + len(credentialIdBytes)
    header = bytes([headerSize, 0x10, len(credentialIdBytes)]) + credentialIdBytes

    # Step 4c: Sign with AES-CMAC
    cmac = AES_CMAC(header, deviceSpecificKey)
    credentialBytes = header + cmac

    # Step 4d: Derive keyComm
    divdata = SHA256(credentialBytes)[0:16]
    keyComm = SHA256(deviceSpecificKey + divdata)[0:16]

    # 5. Log the request (for audit trail)
    log_credential_request(
        deviceId=request.deviceId,
        credentialId=request.credentialId,
        timestamp=now()
    )

    # 6. Return credential components
    return {
        "keyComm": base64_encode(keyComm),
        "keyInitblock": base64_encode(keyInitblock),
        "credentialBytes": base64_encode(credentialBytes)
    }


# Security Helper Functions
def get_project_key_from_secure_storage():
    """
    Retrieve project master key from secure storage.
    Never hardcode this key!
    """
    # Example with AWS Secrets Manager
    secret = secrets_manager.get_secret("mobile-id-project-key")
    return bytes.fromhex(secret)

def is_valid_device(deviceId):
    """
    Validate device is registered and active.
    """
    device = database.get_device(deviceId)
    return device and device.is_active

def is_authorized_user(credentialId):
    """
    Validate user is authorized for access control.
    """
    user = database.get_user(credentialId)
    return user and user.has_access_rights
```

#### Mobile Client Implementation

**Android/Kotlin**:
```kotlin
class CredentialService(private val api: ApiClient) {

    suspend fun fetchCredential(credentialId: String): Credential {
        // 1. Get device ID
        val deviceId = getDeviceId()  // UUID or similar

        // 2. Request from backend
        val response = api.post("/api/credentials/derive") {
            body = json {
                "deviceId" to deviceId
                "credentialId" to credentialId
            }
        }

        // 3. Parse response
        val keyComm = response.getString("keyComm").base64Decode()
        val keyInitblock = response.getString("keyInitblock").base64Decode()
        val credentialBytes = response.getString("credentialBytes").base64Decode()

        // 4. Create credential object
        return Credential(
            keyComm = keyComm,
            keyInitblock = keyInitblock,
            credentialBytes = credentialBytes
        )
    }

    private fun getDeviceId(): String {
        // Use stable device identifier
        return UUID.randomUUID().toString()  // Or use Android ID, etc.
    }
}

// Usage
lifecycleScope.launch {
    try {
        val credential = credentialService.fetchCredential("employee_001")
        sdk.credentials = listOf(credential)
    } catch (e: Exception) {
        // Handle error
    }
}
```

**iOS/Swift**:
```swift
class CredentialService {

    func fetchCredential(credentialId: String) async throws -> Credential {
        // 1. Get device ID
        let deviceId = getDeviceId()

        // 2. Request from backend
        let url = URL(string: "https://your-api.com/api/credentials/derive")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = [
            "deviceId": deviceId,
            "credentialId": credentialId
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, _) = try await URLSession.shared.data(for: request)

        // 3. Parse response
        let json = try JSONSerialization.jsonObject(with: data) as! [String: String]

        let keyComm = Data(base64Encoded: json["keyComm"]!)!
        let keyInitblock = Data(base64Encoded: json["keyInitblock"]!)!
        let credentialBytes = Data(base64Encoded: json["credentialBytes"]!)!

        // 4. Create credential
        return Credential(
            keyComm: keyComm.toKotlinByteArray(),
            keyInitblock: keyInitblock.toKotlinByteArray(),
            credentialBytes: credentialBytes.toKotlinByteArray(),
            onTrigger: nil
        )
    }

    private func getDeviceId() -> String {
        return UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
    }
}
```

#### Security Considerations

1. **Transport Security**: Always use HTTPS for API communication
2. **Authentication**: Authenticate users before providing credentials
3. **Authorization**: Verify user has access rights
4. **Rate Limiting**: Prevent abuse with rate limits on credential requests
5. **Audit Logging**: Log all credential requests with timestamps
6. **Key Storage**: Use secure key management (HSM, cloud key vaults)
7. **Key Rotation**: Implement project key rotation procedures
8. **Revocation**: Maintain ability to revoke device credentials

### 6.3 Credential Management

#### Secure Storage

**Never hardcode project keys or credentials in your app!**

**Android - Use EncryptedSharedPreferences**:
```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCredentialStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredential(keyComm: ByteArray, keyInitblock: ByteArray, credentialBytes: ByteArray) {
        prefs.edit().apply {
            putString("keyComm", Base64.encodeToString(keyComm, Base64.DEFAULT))
            putString("keyInitblock", Base64.encodeToString(keyInitblock, Base64.DEFAULT))
            putString("credentialBytes", Base64.encodeToString(credentialBytes, Base64.DEFAULT))
            apply()
        }
    }

    fun loadCredential(): Credential? {
        val keyComm = prefs.getString("keyComm", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        val keyInitblock = prefs.getString("keyInitblock", null)?.let { Base64.decode(it, Base64.DEFAULT) }
        val credentialBytes = prefs.getString("credentialBytes", null)?.let { Base64.decode(it, Base64.DEFAULT) }

        return if (keyComm != null && keyInitblock != null && credentialBytes != null) {
            Credential(keyComm, keyInitblock, credentialBytes)
        } else {
            null
        }
    }

    fun clearCredential() {
        prefs.edit().clear().apply()
    }
}
```

**iOS - Use Keychain**:
```swift
import Security

class SecureCredentialStorage {

    func saveCredential(_ credential: Credential, forKey key: String) throws {
        let data = try JSONEncoder().encode(credential)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecValueData as String: data
        ]

        // Delete existing item
        SecItemDelete(query as CFDictionary)

        // Add new item
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed
        }
    }

    func loadCredential(forKey key: String) throws -> Credential? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }

        return try JSONDecoder().decode(Credential.self, from: data)
    }

    func clearCredential(forKey key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
    }
}

enum KeychainError: Error {
    case saveFailed
}
```

#### Loading Credentials at Startup

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var sdk: MobileIdSdk
    private lateinit var storage: SecureCredentialStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdk = MobileIdSdk()
        storage = SecureCredentialStorage(this)

        // Load saved credential if exists
        lifecycleScope.launch {
            storage.loadCredential()?.let { credential ->
                sdk.credentials = listOf(credential)
            }
        }
    }
}
```

#### Activating/Deactivating Credentials

```kotlin
// Activate
fun activateCredential(credential: Credential) {
    sdk.credentials = listOf(credential)
}

// Deactivate (stops BLE protocol)
fun deactivateCredential() {
    sdk.credentials = emptyList()
}

// Switch credentials (automatically stops and restarts)
fun switchCredential(newCredential: Credential) {
    sdk.credentials = listOf(newCredential)  // Old one stopped, new one started
}
```

### 6.4 Reader Management

#### Observing Reader Updates

```kotlin
class ReaderManager(private val sdk: MobileIdSdk) {
    private val _readers = MutableStateFlow<List<Reader>>(emptyList())
    val readers: StateFlow<List<Reader>> = _readers.asStateFlow()

    init {
        sdk.onReadersUpdate = {
            _readers.value = sdk.readers
        }
    }
}

// In Compose
@Composable
fun ReaderScreen(readerManager: ReaderManager) {
    val readers by readerManager.readers.collectAsState()

    LazyColumn {
        items(readers) { reader ->
            ReaderCard(reader)
        }
    }
}
```

#### Displaying Readers in UI

```kotlin
@Composable
fun ReaderCard(reader: Reader) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { reader.trigger() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = reader.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tap to open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null
            )
        }
    }
}
```

#### Remote Trigger Dialog

```kotlin
@Composable
fun RemoteTriggerDialog(
    readers: List<Reader>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Available Readers") },
        text = {
            if (readers.isEmpty()) {
                Text("No readers in range")
            } else {
                LazyColumn {
                    items(readers) { reader ->
                        TextButton(
                            onClick = {
                                reader.trigger()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reader.displayName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
```

#### Handling Disconnections

```kotlin
sdk.onReadersUpdate = {
    val currentReaders = sdk.readers

    if (currentReaders.isEmpty()) {
        // All readers disconnected
        showToast("No readers in range")
    } else {
        // Readers available
        showToast("${currentReaders.size} reader(s) available")
    }
}
```

### 6.5 Logging and Support Integration

#### Implementing sendLogs() - Strongly Recommended

**Why This Is Critical**:
- BALTECH support cannot diagnose issues without logs
- Simple one-method integration
- Dramatically improves support quality

**Basic Integration**:
```kotlin
// Add "Contact Support" button anywhere in your app
Button(onClick = {
    lifecycleScope.launch {
        try {
            sdk.sendLogs()
        } catch (e: IllegalStateException) {
            Toast.makeText(context, "No logs available", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send logs", Toast.LENGTH_SHORT).show()
        }
    }
}) {
    Text("Contact Support")
}
```

**With Custom Message**:
```kotlin
fun reportIssue(issueDescription: String) {
    lifecycleScope.launch {
        try {
            sdk.sendLogs(
                subject = "MyApp - Issue Report",
                message = "User reported: $issueDescription\n\nAdditional context: ..."
            )
        } catch (e: Exception) {
            // Handle error
        }
    }
}
```

**Automatic on Errors**:
```kotlin
sdk.onAvailabilityChange = { availability ->
    if (availability == AvailabilityStates.UNAUTHORIZED ||
        availability == AvailabilityStates.UNSUPPORTED) {

        // Show dialog offering to contact support
        showDialog(
            title = "Issue Detected",
            message = "Would you like to send logs to support?",
            positiveButton = "Send Logs" to {
                lifecycleScope.launch {
                    sdk.sendLogs()
                }
            }
        )
    }
}
```

#### Implementing Log Viewer - Optional

**Android/Compose**:
```kotlin
@Composable
fun LogViewerScreen(sdk: MobileIdSdk) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SDK Logs") },
                actions = {
                    IconButton(onClick = {
                        // Send logs to support
                        lifecycleScope.launch {
                            sdk.sendLogs()
                        }
                    }) {
                        Icon(Icons.Default.Email, "Send to Support")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            sdk.createLogView()
        }
    }
}
```

**iOS/Swift**:
```swift
struct LogViewerView: View {
    let sdk: MobileIdSdk

    var body: some View {
        NavigationView {
            LogViewControllerWrapper(
                viewController: sdk.createLogViewController()
            )
            .navigationTitle("SDK Logs")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: sendLogs) {
                        Image(systemName: "envelope")
                    }
                }
            }
        }
    }

    private func sendLogs() {
        Task {
            try? await sdk.sendLogs()
        }
    }
}
```

#### Debug Mode

**Enable During Development**:
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable debug logging in debug builds
        if (BuildConfig.DEBUG) {
            MobileIdSdk().debugMode = true
        }
    }
}
```

**Custom Log Handler**:
```kotlin
sdk.logger = { message ->
    // Send to your logging system
    Timber.d("MobileID: $message")

    // Also add to SDK's internal logs
    sdk.logHandler.addLogMessage(message)
}
```

#### Log Retention

- **Duration**: 1 hour by default
- **Max Entries**: 10,000 entries
- **Auto-cleanup**: Old entries automatically removed

```kotlin
// Access log handler directly
val logs = sdk.logHandler.getLogs()
val formattedLogs = sdk.logHandler.getFormattedLogs()

// Clear logs manually
sdk.logHandler.clearLogs()

// Observe new logs
lifecycleScope.launch {
    sdk.logHandler.latestLogIdFlow.collect { latestId ->
        // New log entry added
    }
}
```

---

## 7. Advanced Topics

### 7.1 Trigger Authorization

Implement custom logic to control whether a trigger is allowed.

```kotlin
val credential = Credential.create(
    projectKey = projectKey,
    credentialId = "employee_001",
    onTrigger = {
        // Return true to allow, false to deny
        val isAllowed = checkUserPermissions()

        if (isAllowed) {
            logTriggerEvent("Trigger allowed")
            true
        } else {
            logTriggerEvent("Trigger denied")
            showToast("Access denied")
            false
        }
    }
)
```

**Use Cases**:
- Time-based access (only allow during work hours)
- Location-based access (geofencing)
- User confirmation (require PIN or biometric)
- Rate limiting (prevent rapid repeated triggers)

**Example: Time-Based Authorization**
```kotlin
onTrigger = {
    val currentHour = LocalTime.now().hour
    val isWorkingHours = currentHour in 8..18

    if (!isWorkingHours) {
        showDialog("Access only allowed during work hours (8:00-18:00)")
    }

    isWorkingHours
}
```

**Example: User Confirmation**
```kotlin
onTrigger = {
    var allowed = false

    // Show confirmation dialog (must be done on main thread)
    withContext(Dispatchers.Main) {
        allowed = showConfirmationDialog("Open door?")
    }

    allowed
}
```

### 7.2 Background Operation (iOS)

iOS supports background BLE peripheral mode with limitations.

**Requirements**:
- `bluetooth-peripheral` in `UIBackgroundModes`
- App must be launched at least once
- Limited processing time in background

**Limitations**:
- Advertising slower in background
- CPU time limited
- May be suspended if inactive too long

**Best Practices**:
```swift
// Keep credential active even when app backgrounded
class AppDelegate: UIResponder, UIApplicationDelegate {
    let sdk = MobileIdSdk()

    func application(_ application: UIApplication,
                    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

        // Initialize credential early
        setupCredential()

        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Credential remains active in background
        // BLE advertising continues
    }

    private func setupCredential() {
        // Load and activate credential
        if let credential = loadSavedCredential() {
            sdk.credentials = [credential]
        }
    }
}
```

**Battery Considerations**:
- BLE advertising consumes battery
- Background operation increases consumption
- Consider allowing users to disable background operation

### 7.3 Statistics Collection (Optional)

The SDK can collect anonymous usage statistics if configured.

**Build Configuration**:

Set environment variables during build:
```bash
export STATISTICS_URL="https://your-stats-endpoint.com/api/stats"
export STATISTICS_TOKEN="your-secret-token"
```

**What's Collected**:
- Connection attempts
- Success/failure rates
- RSSI (signal strength) distribution
- BLE state changes

**What's NOT Collected**:
- User identity
- Credential IDs
- Actual access events
- Location data

**Privacy Considerations**:
- Statistics are anonymous
- No personally identifiable information
- Optional - disabled if not configured
- Best-effort - failures don't affect SDK operation

---

## 8. Troubleshooting

### 8.1 Common Issues

#### iOS: Runtime Crash on Launch

**Problem**: App crashes immediately when using SDK

**Cause**: Missing `-force_load` linker flag

**Solution**: Add to OTHER_LDFLAGS in Build Settings:
```
-force_load $(PROJECT_DIR)/path/to/MobileIDSdk.framework/MobileIDSdk
```

#### Android: Location Permission Required

**Problem**: BLE scanning doesn't work even with Bluetooth permissions

**Cause**: Android requires location permissions for BLE scanning

**Solution**: Add to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

And ensure runtime permission is granted.

#### Both Platforms: Bluetooth Must Be Enabled

**Problem**: SDK reports `DISABLED` state

**Cause**: Bluetooth is turned off

**Solution**: Prompt user to enable Bluetooth
```kotlin
if (availability == AvailabilityStates.DISABLED) {
    showDialog("Please enable Bluetooth to use Mobile ID")
}
```

#### Both Platforms: Only One Credential Supported

**Problem**: `IllegalArgumentException` when setting multiple credentials

**Cause**: SDK currently supports 0 or 1 credential only

**Solution**: Set only one credential at a time
```kotlin
// Correct
sdk.credentials = listOf(credential)

// Wrong - will throw exception
sdk.credentials = listOf(credential1, credential2)
```

### 8.2 Permission Issues

#### Automatic Permission Handling

The SDK automatically requests permissions when credentials are set. Monitor availability state:

```kotlin
sdk.onAvailabilityChange = { availability ->
    when (availability) {
        AvailabilityStates.PERMISSIONS_REQUIRED -> {
            // SDK is requesting permissions
            showLoadingIndicator()
        }
        AvailabilityStates.PERMISSIONS_DENIED -> {
            // User denied, but can retry
            showRetryButton()
        }
        AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> {
            // Must use settings
            showSettingsButton()
        }
        AvailabilityStates.OK -> {
            // All good
        }
    }
}
```

#### Permanently Denied Permissions

**Problem**: User selected "Don't ask again"

**Solution**: Direct to system settings
```kotlin
if (availability == AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED) {
    AlertDialog.Builder(context)
        .setTitle("Permissions Required")
        .setMessage("Please enable Bluetooth permissions in Settings")
        .setPositiveButton("Open Settings") { _, _ ->
            lifecycleScope.launch {
                sdk.openPermissionSettings()
            }
        }
        .show()
}
```

#### Platform-Specific Requirements

**Android**:
- Bluetooth permissions: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`
- Location permissions: `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`
- Android 12+ requires new Bluetooth permissions

**iOS**:
- Bluetooth usage descriptions in Info.plist
- Automatic permission request by iOS system
- No manual permission request needed (system handles it)

### 8.3 BLE Issues

#### Device Doesn't Support BLE

**Problem**: Availability shows `UNSUPPORTED`

**Cause**: Device lacks BLE hardware

**Solution**: Show appropriate message
```kotlin
if (availability == AvailabilityStates.UNSUPPORTED) {
    showDialog("Your device doesn't support Bluetooth LE, which is required for Mobile ID")
}
```

#### Bluetooth Disabled

**Problem**: Availability shows `DISABLED`

**Solution**: Prompt user to enable Bluetooth
```kotlin
// Android - can request to enable
val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

// iOS - show alert (can't enable programmatically)
showAlert("Please enable Bluetooth in Settings")
```

#### App Not Authorized

**Problem**: Availability shows `UNAUTHORIZED`

**Cause**: iOS/Android hasn't authorized Bluetooth access

**Solution**: Usually resolved by permission handling, but if persistent:
```kotlin
// Check permission status
if (availability == AvailabilityStates.UNAUTHORIZED) {
    // Request permissions again
    sdk.requestPermissions()
}
```

### 8.4 Getting Support

#### Essential: Implement sendLogs()

**CRITICAL**: Always integrate `sendLogs()` in your app

Without logs, BALTECH support cannot effectively diagnose issues.

```kotlin
// Add to every app
Button("Report Issue") {
    lifecycleScope.launch {
        sdk.sendLogs(
            subject = "MyApp Issue Report",
            message = "Describe the issue here"
        )
    }
}
```

#### Recommended: Implement Log Viewer

Better support experience when users can view logs:

```kotlin
// Optional but helpful
NavigationItem("View Logs") {
    navigateTo(LogViewerScreen(sdk))
}
```

#### Contacting Support

**Email**: support@baltech.de

**Include**:
- SDK version (check `SdkBuildConfig.sdkVersion`)
- Platform (Android/iOS) and version
- Device model
- Logs (via `sendLogs()`)
- Description of issue

**Before Contacting**:
1. Check this documentation
2. Review AppNote examples
3. Enable debug mode and check console logs
4. Try on different device if possible

---

## 9. API Compatibility

### 9.1 Platform-Specific APIs

#### Log Viewer

**Android/Kotlin (Compose)**:
```kotlin
@Composable
fun MobileIdSdk.createLogView()
```

**iOS/Swift (UIKit)**:
```kotlin
fun MobileIdSdk.createLogViewController(): UIViewController
```

**Usage**:
- Android: Use directly in Compose hierarchy
- iOS: Wrap in `UIViewControllerRepresentable` for SwiftUI

#### Kotlin-Swift Interop

**Byte Array Conversion**:
```swift
// Swift to Kotlin
extension Array where Element == UInt8 {
    func toKotlinByteArray() -> KotlinByteArray {
        let data = Data(self)
        return KotlinByteArray(size: Int32(data.count)) { index in
            return Int8(bitPattern: data[Int(truncating: index)])
        }
    }
}

// Kotlin to Swift
extension KotlinByteArray {
    func toSwiftArray() -> [UInt8] {
        var array = [UInt8]()
        for i in 0..<size {
            array.append(UInt8(bitPattern: get(index: i)))
        }
        return array
    }
}
```

**Accessing Kotlin Objects from Swift**:
```swift
// Kotlin object becomes class in Swift
let states = AvailabilityStates.shared
let okState = states.OK

// Kotlin companion object
let credential = Credential.Companion().create(
    projectKey: projectKey,
    credentialId: "user_001"
)
```

### 9.2 Threading & Coroutines

#### All SDK Methods are Suspend Functions

**Kotlin**:
```kotlin
// Must call from coroutine scope
lifecycleScope.launch {
    sdk.requestPermissions()
    sdk.sendLogs()
    sdk.openPermissionSettings()
}
```

**Swift**: Suspend functions become async
```swift
// Use with await in async context
Task {
    try await sdk.requestPermissions()
    try await sdk.sendLogs()
    try await sdk.openPermissionSettings()
}
```

#### Callbacks Run on Coroutine Scope

**Kotlin**: Callbacks use Dispatchers.Default
```kotlin
sdk.onAvailabilityChange = { availability ->
    // This runs on background thread
    // Update UI on main thread:
    withContext(Dispatchers.Main) {
        updateUI(availability)
    }
}
```

**Swift**: Callbacks run on background queue
```swift
sdk.onAvailabilityChange = { availability in
    // This runs on background queue
    // Update UI on main queue:
    DispatchQueue.main.async {
        self.updateUI(availability)
    }
}
```

#### Platform Dispatcher Usage

SDK uses appropriate dispatchers:
- `Dispatchers.Default`: Background work
- `Dispatchers.Main`: Permission requests (requires Activity context)
- `Dispatchers.IO`: File I/O, network

---

## 10. Migration & Updates

### Semantic Versioning

SDK follows semantic versioning: MAJOR.MINOR.PATCH

- **MAJOR**: Breaking API changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

Current version: **0.01.00**

### Updating SDK Version

**Gradle (Android)**:
```kotlin
dependencies {
    implementation("de.baltech:sdk-android:0.01.00")  // Update version here
}
```

**iOS**: Replace XCFramework with new version

### Breaking Changes Policy

Breaking changes will be documented in release notes with migration guide.

Check compatibility before upgrading major versions.

---

## 11. Support & Resources

### Primary Support Channel

**Method**: Use `sendLogs()` in your app

**Email**: support@baltech.de

**Important**: Always integrate `sendLogs()` - it's essential for support

### Resources

**AppNote Examples**:
- Swift AppNote: Complete iOS integration example
- Kotlin AppNote: Complete Android integration example

**Documentation**:
- This integration guide
- BLE Protocol Specification (docs/ble-protocol.spec.md)
- SDK Interface Specification (docs/sdk-interface.spec.md)

### Best Practices Summary

1. **Security**: Derive credentials on server side
2. **Support**: Integrate `sendLogs()` method
3. **Permissions**: Let SDK handle automatically
4. **Storage**: Use platform secure storage (Keychain/EncryptedPrefs)
5. **Logging**: Enable debug mode during development
6. **Testing**: Test on physical devices (BLE not available in simulators)

---

## Appendix: Quick Reference

### Essential Integration Checklist

- [ ] Add SDK dependency to build configuration
- [ ] Configure AndroidManifest.xml (Android) or Info.plist (iOS)
- [ ] Add `-force_load` to OTHER_LDFLAGS (iOS only)
- [ ] Initialize SDK instance
- [ ] Set up availability and reader callbacks
- [ ] Implement credential derivation (preferably server-side)
- [ ] Implement secure credential storage
- [ ] **Integrate `sendLogs()` for support**
- [ ] (Optional) Integrate log viewer
- [ ] Test on physical device

**End of Developer Integration Guide**

For additional support, contact support@baltech.de with logs from `sendLogs()`.
