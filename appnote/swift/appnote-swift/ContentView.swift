import SwiftUI
import MobileIDSdk

struct ContentView: View {
    let sdk: MobileIdSdk
    @StateObject private var repository = CredentialRepository()
    @SwiftUI.State private var currentScreen: Screen = .credentialList
    @SwiftUI.State private var availability: String = AvailabilityStates.shared.UNDEFINED
    @SwiftUI.State private var showRemoteTriggerDialog = false
    @SwiftUI.State private var readers: [Reader] = []

    var activeCredentialIndex: Int? {
        guard let activeCredential = repository.selectedCredential else { return nil }
        return repository.credentials.firstIndex(where: { $0.id == activeCredential.id })
    }

    var body: some View {
        Group {
            switch currentScreen {
            case .credentialList:
                CredentialListView(
                    credentials: repository.credentials,
                    activeCredentialIndex: activeCredentialIndex,
                    availability: availability,
                    sdk: sdk,
                    onCredentialClick: { index in
                        let credential = repository.credentials[index]
                        if activeCredentialIndex == index {
                            repository.selectCredential(nil)
                        } else {
                            repository.selectCredential(credential)
                        }
                    },
                    onDeleteCredential: { index in
                        let credential = repository.credentials[index]
                        repository.removeCredential(credential)
                    },
                    onAddCredential: {
                        currentScreen = .addCredential
                    },
                    onNavigateToLogs: {
                        currentScreen = .logViewer
                    },
                    onOpenRemoteTrigger: {
                        showRemoteTriggerDialog = true
                    }
                )
                .sheet(isPresented: $showRemoteTriggerDialog) {
                    RemoteTriggerDialog(readers: readers)
                }
            case .addCredential:
                AddCredentialView(
                    onSave: { credential in
                        repository.addCredential(credential)
                        currentScreen = .credentialList
                    },
                    onCancel: {
                        currentScreen = .credentialList
                    }
                )
            case .logViewer:
                LogViewerView(sdk: sdk) {
                    currentScreen = .credentialList
                }
            }
        }
        .onAppear {
            print("ContentView.onAppear: Registering SDK callbacks")
            sdk.onAvailabilityChange = { newAvailability in
                print("ContentView: Availability changed to: \(newAvailability)")
                availability = newAvailability
            }
            sdk.onReadersUpdate = {
                print("ContentView: Readers updated, count: \(sdk.readers.count)")
                readers = sdk.readers
            }
            print("ContentView.onAppear: Initial availability: \(sdk.availability)")
        }
        .task {
            print("ContentView.task: Initializing credentials, activeCredentialIndex=\(activeCredentialIndex ?? -1)")
            updateSDKCredentials(index: activeCredentialIndex)
        }
        .onChange(of: readers.isEmpty) { isEmpty in
            if isEmpty && showRemoteTriggerDialog {
                showRemoteTriggerDialog = false
            }
        }
        .onChange(of: readers.count) { count in
            if count > 0 && !showRemoteTriggerDialog {
                showRemoteTriggerDialog = true
            }
        }
        .onChange(of: activeCredentialIndex) { index in
            updateSDKCredentials(index: index)
        }
        .onChange(of: repository.credentials.count) { _ in
            updateSDKCredentials(index: activeCredentialIndex)
        }
    }

    private func updateSDKCredentials(index: Int?) {
        print("ContentView.updateSDKCredentials: index=\(index ?? -1), credentials.count=\(repository.credentials.count)")
        if let index = index, index < repository.credentials.count {
            let credential = repository.credentials[index]
            print("ContentView: Setting credential with ID: \(credential.credentialId)")
            do {
                let projectKey = try credential.projectKeyHex.hexToByteArray()
                sdk.credentials = [Credential.Companion().create(projectKey: projectKey.toKotlinByteArray(), credentialId: credential.credentialId)]
                print("ContentView: Credential set successfully")
            } catch {
                print("ContentView: Failed to set credential: \(error)")
                repository.selectCredential(nil)
            }
        } else {
            print("ContentView: Clearing credentials")
            sdk.credentials = []
        }
    }
}

// MARK: - Remote Trigger Dialog
struct RemoteTriggerDialog: View {
    let readers: [Reader]
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            VStack {
                if readers.isEmpty {
                    Text("No readers available")
                        .foregroundColor(.secondary)
                        .padding()
                } else {
                    List(readers, id: \.displayName) { reader in
                        Button(action: {
                            reader.trigger()
                        }) {
                            Text(reader.displayName)
                                .fontWeight(.medium)
                                .padding()
                        }
                        .listRowBackground(Color.blue.opacity(0.1))
                    }
                }
            }
            .navigationTitle("Remote Trigger")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Credential List View
struct CredentialListView: View {
    let credentials: [CredentialData]
    let activeCredentialIndex: Int?
    let availability: String
    let sdk: MobileIdSdk
    let onCredentialClick: (Int) -> Void
    let onDeleteCredential: (Int) -> Void
    let onAddCredential: () -> Void
    let onNavigateToLogs: () -> Void
    let onOpenRemoteTrigger: () -> Void

    @SwiftUI.State private var showMenu = false

    var body: some View {
        NavigationView {
            ZStack {
                mainContent
                addButton
            }
            .navigationTitle("Mobile ID Appnote (Swift)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    menuButton
                }
            }
        }
        .navigationViewStyle(.stack)
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            if credentials.isEmpty {
                emptyState
            } else {
                credentialsList
                credentialHint
            }
            AvailabilityStatusView(availability: availability, sdk: sdk)
        }
    }

    private var emptyState: some View {
        VStack {
            Spacer()
            Text("No credentials added yet.\nTap + to add your first credential.")
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
                .padding()
            Spacer()
        }
    }

    private var credentialsList: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(Array(credentials.enumerated()), id: \.element.id) { index, credential in
                    CredentialCard(
                        credential: credential,
                        isActive: activeCredentialIndex == index,
                        onClick: {
                            onCredentialClick(index)
                        },
                        onDelete: {
                            onDeleteCredential(index)
                        }
                    )
                }
            }
            .padding(.horizontal)
            .padding(.top)
        }
    }

    private var credentialHint: some View {
        Group {
            if activeCredentialIndex == nil {
                Text("Tap a credential to activate the Appnote's functionality")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 32)
                    .padding(.vertical)
            } else {
                Spacer()
                    .frame(maxHeight: 50)
            }
        }
    }

    private var addButton: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                Button(action: onAddCredential) {
                    Image(systemName: "plus")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 56, height: 56)
                        .background(Color.blue)
                        .clipShape(Circle())
                        .shadow(radius: 4)
                }
                .padding(.trailing)
                .padding(.bottom, 80)
            }
        }
    }

    private var menuButton: some View {
        Menu {
            Button(action: onOpenRemoteTrigger) {
                Label("Remote Trigger", systemImage: "antenna.radiowaves.left.and.right")
            }
            .disabled(activeCredentialIndex == nil)

            Button(action: onNavigateToLogs) {
                Label("View Logs", systemImage: "list.bullet")
            }

            Button(action: {
                Task {
                    try? await sdk.sendLogs(subject: nil, message: nil)
                }
            }) {
                Label("Send Logs", systemImage: "envelope")
            }
        } label: {
            Image(systemName: "line.3.horizontal")
        }
    }
}

// MARK: - Availability Status View
struct AvailabilityStatusView: View {
    let availability: String
    let sdk: MobileIdSdk

    var backgroundColor: Color {
        let states = AvailabilityStates.shared
        switch availability {
        case states.OK:
            return Color.blue.opacity(0.3)
        case states.DISABLED, states.UNAUTHORIZED, states.UNSUPPORTED:
            return Color.red.opacity(0.3)
        case states.PERMISSIONS_DENIED, states.PERMISSIONS_REQUIRED, states.PERMISSIONS_PERMANENTLY_DENIED:
            return Color.orange.opacity(0.3)
        default:
            return Color.gray.opacity(0.2)
        }
    }

    var statusText: String {
        let states = AvailabilityStates.shared
        switch availability {
        case states.PERMISSIONS_DENIED, states.PERMISSIONS_REQUIRED:
            return "Permissions Required"
        case states.PERMISSIONS_PERMANENTLY_DENIED:
            return "Permissions Denied"
        default:
            return availability
        }
    }

    var statusColor: Color {
        let states = AvailabilityStates.shared
        switch availability {
        case states.OK:
            return .blue
        case states.DISABLED, states.UNAUTHORIZED, states.UNSUPPORTED:
            return .red
        default:
            return .primary
        }
    }

    var body: some View {
        let states = AvailabilityStates.shared
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Bluetooth Status: ")
                    .fontWeight(.medium)
                Text(statusText)
                    .fontWeight(.bold)
                    .foregroundColor(statusColor)
            }

            if availability == states.PERMISSIONS_DENIED || availability == states.PERMISSIONS_REQUIRED {
                Button("Grant Permissions") {
                    Task {
                        try? await sdk.requestPermissions()
                    }
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
            } else if availability == states.PERMISSIONS_PERMANENTLY_DENIED {
                HStack(spacing: 8) {
                    Button("Open Settings") {
                        Task {
                            try? await sdk.openPermissionSettings()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)

                    Button("Try Again") {
                        Task {
                            try? await sdk.requestPermissions()
                        }
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(backgroundColor)
    }
}

// MARK: - Credential Card
struct CredentialCard: View {
    let credential: CredentialData
    let isActive: Bool
    let onClick: () -> Void
    let onDelete: () -> Void

    var backgroundColor: Color {
        isActive ? Color(red: 0, green: 0.4, blue: 0.8) : Color(red: 0.7, green: 0.85, blue: 1.0)
    }

    var textColor: Color {
        isActive ? .white : Color(red: 0, green: 0.2, blue: 0.4)
    }

    var body: some View {
        Button(action: onClick) {
            ZStack {
                VStack {
                    HStack {
                        Spacer()
                        Button(action: onDelete) {
                            Image(systemName: "trash")
                                .foregroundColor(textColor)
                        }
                        .padding(8)
                    }
                    Spacer()
                    HStack {
                        Spacer()
                        VStack(alignment: .trailing) {
                            Text(credential.projectKeyHex)
                                .font(.caption)
                                .foregroundColor(textColor.opacity(0.8))
                            Text(credential.credentialId)
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(textColor)
                        }
                        .padding(8)
                    }
                }
            }
            .frame(height: 224)
            .frame(maxWidth: .infinity)
            .background(backgroundColor)
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Add Credential View
struct AddCredentialView: View {
    let onSave: (CredentialData) -> Void
    let onCancel: () -> Void

    @SwiftUI.State private var projectKey = "00000000000000000000000000000000"
    @SwiftUI.State private var credentialId = "1234"
    @SwiftUI.State private var keyError = ""

    var canSave: Bool {
        !projectKey.isEmpty && !credentialId.isEmpty && projectKey.isValidHex()
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                VStack(alignment: .leading) {
                    TextField("Project Key (32 hex characters)", text: $projectKey)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.allCharacters)
                        .onChange(of: projectKey) { newValue in
                            let uppercased = newValue.uppercased()
                            if uppercased != newValue {
                                projectKey = uppercased
                            }
                            validateProjectKey(uppercased)
                        }

                    if !keyError.isEmpty {
                        Text(keyError)
                            .font(.caption)
                            .foregroundColor(.red)
                    } else {
                        Text("Enter 16-byte key as 32 hexadecimal characters")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                VStack(alignment: .leading) {
                    TextField("Credential ID", text: $credentialId)
                        .textFieldStyle(.roundedBorder)

                    Text("ASCII string identifier for this credential")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                HStack(spacing: 8) {
                    Button("Cancel") {
                        onCancel()
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)

                    Button("Save") {
                        do {
                            _ = try projectKey.hexToByteArray()
                            let credential = CredentialData(projectKeyHex: projectKey, credentialId: credentialId)
                            onSave(credential)
                        } catch {
                            keyError = error.localizedDescription
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .disabled(!canSave)
                }
            }
            .padding()
            .navigationTitle("Add Credential")
            .navigationBarTitleDisplayMode(.inline)
        }
        .navigationViewStyle(.stack)
    }

    private func validateProjectKey(_ value: String) {
        if value.isEmpty {
            keyError = ""
        } else if value.count != 32 {
            keyError = "Key must be exactly 32 hex characters"
        } else if !value.isValidHex() {
            keyError = "Key must contain only hex characters (0-9, A-F)"
        } else {
            keyError = ""
        }
    }
}

// MARK: - Log Viewer View
struct LogViewerView: View {
    let sdk: MobileIdSdk
    let onBack: () -> Void

    var body: some View {
        NavigationView {
            LogViewControllerWrapper(viewController: sdk.createLogViewController())
                .navigationTitle("Mobile ID Logs")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: onBack) {
                            Image(systemName: "arrow.left")
                        }
                    }
                }
        }
        .navigationViewStyle(.stack)
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
