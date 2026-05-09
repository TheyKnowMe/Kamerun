package com.ME.kamerun.ui.screens.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

data class ServerConfig(
    val host: String = "192.168.1.100",
    val port: Int = 9550,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        val HOST_KEY = stringPreferencesKey("server_host")
        val PORT_KEY = intPreferencesKey("server_port")
    }

    val serverConfig: StateFlow<ServerConfig> = context.dataStore.data
        .map { prefs ->
            ServerConfig(
                host = prefs[HOST_KEY] ?: "192.168.1.100",
                port = prefs[PORT_KEY] ?: 9550,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerConfig())

    fun saveServerConfig(host: String, port: Int) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[HOST_KEY] = host
                prefs[PORT_KEY] = port
            }
        }
    }
}