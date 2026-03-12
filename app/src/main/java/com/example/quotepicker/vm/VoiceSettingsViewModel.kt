package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceMarkState
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
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
import org.json.JSONObject
import java.util.UUID

data class VoiceSettingsUiState(
    val characterNames: List<String> = emptyList(),
    val narratorName: String = "旁白",
    val noticeName: String = "注意",
    val settings: VoiceSettings = VoiceSettings()
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val DEFAULT_ROLE_KEY = "__default_voice_setting__"
    }
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
        noiseScaleW: Float?,
        lengthScale: Float?,
        maxNumSentences: Int?,
        silenceScale: Float?
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
                    noiseScaleW = noiseScaleW,
                    lengthScale = lengthScale,
                    maxNumSentences = maxNumSentences,
                    silenceScale = silenceScale
                )
            )
        }
        persist()
    }

    fun updatePreviewText(roleName: String, text: String) {
        localSettings.update { state ->
            state.copy(rolePreviewTexts = state.rolePreviewTexts.toMutableMap().apply { put(roleName, text) })
        }
        persist()
    }



    fun updateSpeakerMemo(speakerId: Int, memo: String) {
        val normalizedId = speakerId.coerceIn(0, 186)
        localSettings.update { state ->
            val updated = state.speakerMemos.toMutableMap()
            if (memo.isBlank()) {
                updated.remove(normalizedId)
            } else {
                updated[normalizedId] = memo
            }
            state.copy(speakerMemos = updated)
        }
        persist()
    }

    fun buildSpeakerMemoExportText(): String {
        if (localSettings.value.speakerMemos.isEmpty()) return ""
        return localSettings.value.speakerMemos.toSortedMap().entries.joinToString("\n") { (speakerId, memo) ->
            "$speakerId：$memo"
        }
    }

    fun resolveRoleSettingFromResources(roleName: String, resources: List<ResourceWithTagsCharacters>): RoleVoiceSetting? {
        val candidates = resources.filter { item ->
            item.resource.type == ResourceType.TEXT &&
                item.resource.title.contains("声音配置") &&
                item.characters.any { it.name == roleName }
        }
        val picked = candidates
            .filter { it.resource.markState == ResourceMarkState.FAVORITE }
            .ifEmpty { candidates }
            .firstOrNull()
            ?: return null
        return parseRoleSettingFromText(roleName, picked.resource.quoteText)
    }

    private fun parseRoleSettingFromText(roleName: String, content: String?): RoleVoiceSetting? {
        if (content.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(content)
            RoleVoiceSetting(
                roleName = roleName,
                speechRate = root.optDouble("speechRate", 1.0).toFloat(),
                speakerId = if (root.has("speakerId")) root.optInt("speakerId") else null,
                noiseScale = if (root.has("noiseScale")) root.optDouble("noiseScale").toFloat() else null,
                noiseScaleW = if (root.has("noiseScaleW")) root.optDouble("noiseScaleW").toFloat() else null,
                lengthScale = if (root.has("lengthScale")) root.optDouble("lengthScale").toFloat() else null,
                maxNumSentences = if (root.has("maxNumSentences")) root.optInt("maxNumSentences") else null,
                silenceScale = if (root.has("silenceScale")) root.optDouble("silenceScale").toFloat() else null
            )
        }.getOrNull()
    }


    fun globalRoleSetting(settings: VoiceSettings = localSettings.value): RoleVoiceSetting? {
        return settings.roleSettings.firstOrNull { it.roleName == DEFAULT_ROLE_KEY }
    }

    fun buildDefaultConfigText(): String {
        val roleSetting = globalRoleSetting()
        return JSONObject().apply {
            put("speechRate", roleSetting?.speechRate ?: 1.0)
            put("speakerId", roleSetting?.speakerId ?: 0)
            roleSetting?.noiseScale?.let { put("noiseScale", it) }
            roleSetting?.noiseScaleW?.let { put("noiseScaleW", it) }
            roleSetting?.lengthScale?.let { put("lengthScale", it) }
            roleSetting?.maxNumSentences?.let { put("maxNumSentences", it) }
            roleSetting?.silenceScale?.let { put("silenceScale", it) }
        }.toString(2)
    }

    private fun persist() {
        viewModelScope.launch { store.save(localSettings.value) }
    }
}
