package com.example.quotepicker.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val BUILTIN_PROFILE_NAME = "vits-zh-hf-fanchen-C"
private const val BUILTIN_MODEL_URI = "asset://tts/vits-zh-hf-fanchen-C.onnx"

data class VoiceProfile(
    val id: String,
    val name: String,
    val modelUri: String,
    val configUri: String,
    val speakerId: Int? = null,
    val noiseScale: Float? = null,
    val noiseW: Float? = null,
    val sentenceSilence: Float? = null
)

data class RoleVoiceSetting(
    val roleName: String,
    val profileId: String? = null,
    val speechRate: Float = 1.0f
)

data class VoiceSettings(
    val profiles: List<VoiceProfile> = emptyList(),
    val roleSettings: List<RoleVoiceSetting> = emptyList()
)

class VoiceSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("voice_settings", Context.MODE_PRIVATE)

    fun load(): VoiceSettings {
        val raw = prefs.getString(KEY_PAYLOAD, null)
        val parsed = if (raw == null) {
            VoiceSettings()
        } else {
            runCatching {
                val root = JSONObject(raw)
                val profiles = root.optJSONArray("profiles").toProfiles()
                val roleSettings = root.optJSONArray("roleSettings").toRoleSettings()
                VoiceSettings(profiles = profiles, roleSettings = roleSettings)
            }.getOrDefault(VoiceSettings())
        }
        return parsed.withBuiltinProfile()
    }

    fun save(settings: VoiceSettings) {
        val root = JSONObject()
            .put("profiles", JSONArray().apply {
                settings.profiles.forEach { profile ->
                    put(
                        JSONObject()
                            .put("id", profile.id)
                            .put("name", profile.name)
                            .put("modelUri", profile.modelUri)
                            .put("configUri", profile.configUri)
                            .put("speakerId", profile.speakerId)
                            .put("noiseScale", profile.noiseScale)
                            .put("noiseW", profile.noiseW)
                            .put("sentenceSilence", profile.sentenceSilence)
                    )
                }
            })
            .put("roleSettings", JSONArray().apply {
                settings.roleSettings.forEach { item ->
                    put(
                        JSONObject()
                            .put("roleName", item.roleName)
                            .put("profileId", item.profileId ?: "")
                            .put("speechRate", item.speechRate)
                    )
                }
            })
        prefs.edit().putString(KEY_PAYLOAD, root.toString()).apply()
    }

    fun ensureProfile(name: String): VoiceProfile {
        val settings = load()
        val existing = settings.profiles.firstOrNull { it.name == name }
        if (existing != null) return existing
        val created = VoiceProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            modelUri = "",
            configUri = ""
        )
        save(settings.copy(profiles = settings.profiles + created))
        return created
    }

    companion object {
        private const val KEY_PAYLOAD = "payload"
    }
}


private fun VoiceSettings.withBuiltinProfile(): VoiceSettings {
    val existing = profiles.firstOrNull { it.modelUri == BUILTIN_MODEL_URI }
    if (existing != null) return this
    val builtIn = VoiceProfile(
        id = UUID.randomUUID().toString(),
        name = BUILTIN_PROFILE_NAME,
        modelUri = BUILTIN_MODEL_URI,
        configUri = ""
    )
    return copy(profiles = listOf(builtIn) + profiles)
}

private fun JSONArray?.toProfiles(): List<VoiceProfile> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            add(
                VoiceProfile(
                    id = item.optString("id"),
                    name = item.optString("name"),
                    modelUri = item.optString("modelUri"),
                    configUri = item.optString("configUri"),
                    speakerId = item.optNullableInt("speakerId"),
                    noiseScale = item.optNullableFloat("noiseScale"),
                    noiseW = item.optNullableFloat("noiseW"),
                    sentenceSilence = item.optNullableFloat("sentenceSilence")
                )
            )
        }
    }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (isNull(key) || !has(key)) return null
    return optInt(key)
}

private fun JSONObject.optNullableFloat(key: String): Float? {
    if (isNull(key) || !has(key)) return null
    return optDouble(key).toFloat()
}

private fun JSONArray?.toRoleSettings(): List<RoleVoiceSetting> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val role = item.optString("roleName")
            if (role.isBlank()) continue
            add(
                RoleVoiceSetting(
                    roleName = role,
                    profileId = item.optString("profileId").ifBlank { null },
                    speechRate = item.optDouble("speechRate", 1.0).toFloat()
                )
            )
        }
    }
}
