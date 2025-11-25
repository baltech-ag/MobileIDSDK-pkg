package de.baltech.mobileid_appnote_kotlin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import de.baltech.mobileid.internal.DataStoreContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "credentials")

actual fun createDataStore(): DataStore<Preferences> {
    val context = DataStoreContext.appContext
        ?: throw IllegalStateException("SDK not initialized - DataStoreContext is null")
    return context.dataStore
}