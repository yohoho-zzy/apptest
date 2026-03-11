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
    val noiseScaleW: Float? = null,
    val lengthScale: Float? = null,
    val maxNumSentences: Int? = null,
    val silenceScale: Float? = null
)

data class RoleVoiceSetting(
    val roleName: String,
    val profileId: String? = null,
    val speechRate: Float = 1.0f,
    val speakerId: Int? = null,
    val noiseScale: Float? = null,
    val noiseScaleW: Float? = null,
    val lengthScale: Float? = null,
    val maxNumSentences: Int? = null,
    val silenceScale: Float? = null
)

data class VoiceSettings(
    val profiles: List<VoiceProfile> = emptyList(),
    val roleSettings: List<RoleVoiceSetting> = emptyList(),
    val rolePreviewTexts: Map<String, String> = emptyMap()
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
                val rolePreviewTexts = root.optJSONObject("rolePreviewTexts").toPreviewTexts()
                VoiceSettings(profiles = profiles, roleSettings = roleSettings, rolePreviewTexts = rolePreviewTexts)
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
                            .put("noiseScaleW", profile.noiseScaleW)
                            .put("lengthScale", profile.lengthScale)
                            .put("maxNumSentences", profile.maxNumSentences)
                            .put("silenceScale", profile.silenceScale)
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
                            .put("speakerId", item.speakerId)
                            .put("noiseScale", item.noiseScale)
                            .put("noiseScaleW", item.noiseScaleW)
                            .put("lengthScale", item.lengthScale)
                            .put("maxNumSentences", item.maxNumSentences)
                            .put("silenceScale", item.silenceScale)
                    )
                }
            })
            .put("rolePreviewTexts", JSONObject().apply {
                settings.rolePreviewTexts.forEach { (role, text) ->
                    put(role, text)
                }
            })
        prefs.edit().putString(KEY_PAYLOAD, root.toString()).apply()
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
                    noiseScaleW = item.optNullableFloat("noiseScaleW")
                        ?: item.optNullableFloat("noiseW"),
                    lengthScale = item.optNullableFloat("lengthScale"),
                    maxNumSentences = item.optNullableInt("maxNumSentences"),
                    silenceScale = item.optNullableFloat("silenceScale")
                        ?: item.optNullableFloat("sentenceSilence")
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
                    speechRate = item.optDouble("speechRate", 1.0).toFloat(),
                    speakerId = item.optNullableInt("speakerId"),
                    noiseScale = item.optNullableFloat("noiseScale"),
                    noiseScaleW = item.optNullableFloat("noiseScaleW")
                        ?: item.optNullableFloat("noiseW"),
                    lengthScale = item.optNullableFloat("lengthScale"),
                    maxNumSentences = item.optNullableInt("maxNumSentences"),
                    silenceScale = item.optNullableFloat("silenceScale")
                        ?: item.optNullableFloat("sentenceSilence")
                )
            )
        }
    }
}

private fun JSONObject?.toPreviewTexts(): Map<String, String> {
    if (this == null) return emptyMap()
    return keys().asSequence().mapNotNull { key ->
        key?.takeIf { it.isNotBlank() }?.let { safeKey ->
            safeKey to optString(safeKey)
        }
    }.toMap()
}
