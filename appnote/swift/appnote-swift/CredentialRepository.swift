import Foundation
import Combine

class CredentialRepository: ObservableObject {
    @Published private(set) var credentials: [CredentialData] = []
    @Published var selectedCredential: CredentialData?

    private let userDefaults = UserDefaults.standard
    private let credentialsKey = "credentials"
    private let selectedCredentialIdKey = "selected_credential_id"

    init() {
        loadCredentials()
        loadSelectedCredential()
    }

    func addCredential(_ credential: CredentialData) {
        credentials.append(credential)
        saveCredentials()
    }

    func removeCredential(_ credential: CredentialData) {
        credentials.removeAll { $0.id == credential.id }
        if selectedCredential?.id == credential.id {
            selectCredential(nil)
        }
        saveCredentials()
    }

    func clear() {
        credentials.removeAll()
        selectCredential(nil)
        saveCredentials()
    }

    func selectCredential(_ credential: CredentialData?) {
        selectedCredential = credential
        saveSelectedCredential()
    }

    private func loadCredentials() {
        guard let data = userDefaults.data(forKey: credentialsKey) else { return }
        do {
            let decoded = try JSONDecoder().decode([CredentialData].self, from: data)
            credentials = decoded
        } catch {
            print("Failed to decode credentials: \(error)")
        }
    }

    private func saveCredentials() {
        do {
            let encoded = try JSONEncoder().encode(credentials)
            userDefaults.set(encoded, forKey: credentialsKey)
        } catch {
            print("Failed to encode credentials: \(error)")
        }
    }

    private func loadSelectedCredential() {
        guard let selectedId = userDefaults.string(forKey: selectedCredentialIdKey) else { return }
        selectedCredential = credentials.first { $0.credentialId == selectedId }
    }

    private func saveSelectedCredential() {
        if let selectedId = selectedCredential?.credentialId {
            userDefaults.set(selectedId, forKey: selectedCredentialIdKey)
        } else {
            userDefaults.removeObject(forKey: selectedCredentialIdKey)
        }
    }
}
