package de.baltech.mobileid_appnote_kotlin

import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.baltech.mobileid.Credential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CredentialRepository(private val dataStore: DataStore<Preferences>) {
    private val _credentials = mutableStateListOf<CredentialData>()
    val credentials: List<CredentialData> = _credentials

    private val _selectedCredential = MutableStateFlow<CredentialData?>(null)
    val selectedCredential: StateFlow<CredentialData?> = _selectedCredential.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    companion object {
        private val CREDENTIALS_KEY = stringPreferencesKey("credentials")
        private val SELECTED_CREDENTIAL_ID_KEY = stringPreferencesKey("selected_credential_id")
    }

    init {
        // Load credentials and selected credential from DataStore
        scope.launch {
            val preferences = dataStore.data.first()

            // Load credentials list
            val credentialsJson = preferences[CREDENTIALS_KEY]
            if (credentialsJson != null) {
                try {
                    val loadedCredentials = Json.decodeFromString<List<CredentialData>>(credentialsJson)
                    _credentials.addAll(loadedCredentials)
                } catch (e: Exception) {
                    // Handle deserialization error
                }
            }

            // Load selected credential
            val selectedCredentialId = preferences[SELECTED_CREDENTIAL_ID_KEY]
            if (selectedCredentialId != null) {
                _selectedCredential.value = _credentials.find { it.credentialId == selectedCredentialId }
            }
        }
    }

    fun addCredential(credential: CredentialData) {
        _credentials.add(credential)
        saveCredentials()
    }

    fun removeCredential(credential: CredentialData) {
        _credentials.remove(credential)
        if (_selectedCredential.value == credential) {
            selectCredential(null)
        }
        saveCredentials()
    }

    fun clear() {
        _credentials.clear()
        selectCredential(null)
        saveCredentials()
    }

    fun selectCredential(credential: CredentialData?) {
        _selectedCredential.value = credential
        saveSelectedCredential()
    }

    fun toCredentials(): List<Credential> {
        return credentials.map { credentialData ->
            val projectKey = credentialData.projectKeyHex.hexToByteArray()
            Credential.create(projectKey, credentialData.credentialId)
        }
    }

    private fun saveCredentials() {
        scope.launch {
            dataStore.edit { preferences ->
                val credentialsJson = Json.encodeToString(_credentials.toList())
                preferences[CREDENTIALS_KEY] = credentialsJson
            }
        }
    }

    private fun saveSelectedCredential() {
        scope.launch {
            dataStore.edit { preferences ->
                val selectedId = _selectedCredential.value?.credentialId
                if (selectedId != null) {
                    preferences[SELECTED_CREDENTIAL_ID_KEY] = selectedId
                } else {
                    preferences.remove(SELECTED_CREDENTIAL_ID_KEY)
                }
            }
        }
    }
}