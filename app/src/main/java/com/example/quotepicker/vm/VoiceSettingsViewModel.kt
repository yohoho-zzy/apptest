package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.Repository
import com.example.quotepicker.util.RoleVoiceSetting
import com.example.quotepicker.util.VoiceProfile
import com.example.quotepicker.util.VoiceSettings
import com.example.quotepicker.util.VoiceSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class VoiceSettingsUiState(
    val characterNames: List<String> = emptyList(),
    val narratorName: String = "旁白",
    val settings: VoiceSettings = VoiceSettings()
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repository.get(application)
    private val store = VoiceSettingsStore(application)
    private val localSettings = MutableStateFlow(store.load())

    val uiState: StateFlow<VoiceSettingsUiState> = combine(
        repo.observeCharacters(),
        localSettings
    ) { characters, settings ->
        VoiceSettingsUiState(
            characterNames = characters.map { it.name },
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceSettingsUiState())

    fun addProfile(name: String) {
        val finalName = name.trim().ifBlank { "音色${System.currentTimeMillis() % 10000}" }
        localSettings.update {
            it.copy(
                profiles = it.profiles + VoiceProfile(
                    id = UUID.randomUUID().toString(),
                    name = finalName,
                    modelUri = "",
                    configUri = ""
                )
            )
        }
        persist()
    }

    fun updateProfile(profile: VoiceProfile) {
        localSettings.update { state ->
            state.copy(profiles = state.profiles.map { if (it.id == profile.id) profile else it })
        }
        persist()
    }

    fun removeProfile(profileId: String) {
        localSettings.update { state ->
            state.copy(
                profiles = state.profiles.filterNot { it.id == profileId },
                roleSettings = state.roleSettings.map {
                    if (it.profileId == profileId) it.copy(profileId = null) else it
                }
            )
        }
        persist()
    }

    fun updateRoleSetting(
        roleName: String,
        profileId: String?,
        speechRate: Float,
        speakerId: Int?,
        noiseScale: Float?,
        noiseW: Float?,
        sentenceSilence: Float?
    ) {
        localSettings.update { state ->
            val cleaned = state.roleSettings.filterNot { it.roleName == roleName }
            state.copy(
                roleSettings = cleaned + RoleVoiceSetting(
                    roleName = roleName,
                    profileId = profileId,
                    speechRate = speechRate,
                    speakerId = speakerId,
                    noiseScale = noiseScale,
                    noiseW = noiseW,
                    sentenceSilence = sentenceSilence
                )
            )
        }
        persist()
    }

    private fun persist() {
        viewModelScope.launch { store.save(localSettings.value) }
    }
}
