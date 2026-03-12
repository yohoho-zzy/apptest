package com.example.quotepicker.ui.components

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

data class MagicDramaTheme(
    val id: String,
    val name: String,
    val resourceBackground: Color,
    val resourceCountdownBubble: Color,
    val resourceCountdownText: Color,
    val statusBackground: Color,
    val statusTitleText: Color,
    val statusBubble: Color,
    val statusBubbleText: Color,
    val dialogBackground: Color,
    val narrationBubble: Color,
    val narrationText: Color,
    val warningBubble: Color,
    val warningText: Color,
    val characterBubble: Color,
    val characterText: Color,
    val avatarBackground: Color,
    val avatarText: Color
)

private const val MAGIC_DRAMA_THEME_ASSET = "magic_drama_themes.json"
const val DEFAULT_MAGIC_DRAMA_THEME_ID = "lonely_night"

private val fallbackTheme = MagicDramaTheme(
    id = DEFAULT_MAGIC_DRAMA_THEME_ID,
    name = "深夜",
    resourceBackground = Color(0xFF020617),
    resourceCountdownBubble = Color(0xFF334155),
    resourceCountdownText = Color(0xFFE2E8F0),
    statusBackground = Color(0xFF1E293B),
    statusTitleText = Color(0xFFE2E8F0),
    statusBubble = Color(0xFF475569),
    statusBubbleText = Color(0xFFF1F5F9),
    dialogBackground = Color(0xFF020617),
    narrationBubble = Color(0xFF1E293B),
    narrationText = Color(0xFFCBD5F5),
    warningBubble = Color(0xFF7C2D12),
    warningText = Color(0xFFFED7AA),
    characterBubble = Color(0xFF1D4ED8),
    characterText = Color(0xFFEFF6FF),
    avatarBackground = Color(0xFF334155),
    avatarText = Color(0xFFF8FAFC)
)

fun loadMagicDramaThemes(context: Context): List<MagicDramaTheme> {
    return runCatching {
        val raw = context.assets.open(MAGIC_DRAMA_THEME_ASSET).bufferedReader().use { it.readText() }
        val themes = JSONObject(raw).optJSONArray("themes") ?: return@runCatching listOf(fallbackTheme)
        buildList {
            for (i in 0 until themes.length()) {
                val obj = themes.optJSONObject(i) ?: continue
                add(
                    MagicDramaTheme(
                        id = obj.optString("id", "").trim(),
                        name = obj.optString("name", "").trim(),
                        resourceBackground = parseThemeColor(obj, "resourceBackground", fallbackTheme.resourceBackground),
                        resourceCountdownBubble = parseThemeColor(obj, "resourceCountdownBubble", fallbackTheme.resourceCountdownBubble),
                        resourceCountdownText = parseThemeColor(obj, "resourceCountdownText", fallbackTheme.resourceCountdownText),
                        statusBackground = parseThemeColor(obj, "statusBackground", fallbackTheme.statusBackground),
                        statusTitleText = parseThemeColor(obj, "statusTitleText", fallbackTheme.statusTitleText),
                        statusBubble = parseThemeColor(obj, "statusBubble", fallbackTheme.statusBubble),
                        statusBubbleText = parseThemeColor(obj, "statusBubbleText", fallbackTheme.statusBubbleText),
                        dialogBackground = parseThemeColor(obj, "dialogBackground", fallbackTheme.dialogBackground),
                        narrationBubble = parseThemeColor(obj, "narrationBubble", fallbackTheme.narrationBubble),
                        narrationText = parseThemeColor(obj, "narrationText", fallbackTheme.narrationText),
                        warningBubble = parseThemeColor(obj, "warningBubble", fallbackTheme.warningBubble),
                        warningText = parseThemeColor(obj, "warningText", fallbackTheme.warningText),
                        characterBubble = parseThemeColor(obj, "characterBubble", fallbackTheme.characterBubble),
                        characterText = parseThemeColor(obj, "characterText", fallbackTheme.characterText),
                        avatarBackground = parseThemeColor(obj, "avatarBackground", fallbackTheme.avatarBackground),
                        avatarText = parseThemeColor(obj, "avatarText", fallbackTheme.avatarText)
                    )
                )
            }
        }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
    }.getOrElse { emptyList() }.ifEmpty { listOf(fallbackTheme) }
}

private fun parseThemeColor(json: JSONObject, key: String, fallback: Color): Color {
    val raw = json.optString(key)
    if (!raw.startsWith("#")) return fallback
    return runCatching { Color(android.graphics.Color.parseColor(raw)) }.getOrDefault(fallback)
}

fun magicDramaAtmosphereAliases(themes: List<MagicDramaTheme>): Map<String, String> {
    val aliases = mutableMapOf<String, String>()
    themes.forEach { theme ->
        aliases[theme.id.lowercase()] = theme.id
        aliases[theme.name.lowercase()] = theme.id
    }
    return aliases
}
