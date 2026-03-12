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
const val DEFAULT_MAGIC_DRAMA_THEME_ID = "fresh"

private val fallbackTheme = MagicDramaTheme(
    id = DEFAULT_MAGIC_DRAMA_THEME_ID,
    name = "清新",
    resourceBackground = Color(0xFFA7F3D0),
    resourceCountdownBubble = Color(0xFFF9E24E),
    resourceCountdownText = Color(0xFF022C22),
    statusBackground = Color(0xFF6EE7B7),
    statusTitleText = Color(0xFF064E3B),
    statusBubble = Color(0xFFDCFCE7),
    statusBubbleText = Color(0xFF14532D),
    dialogBackground = Color(0xFFF0FDF4),
    narrationBubble = Color(0xFFBFFDD5),
    narrationText = Color(0xFF14532D),
    warningBubble = Color(0xFFF9E24E),
    warningText = Color(0xFFC25D05),
    characterBubble = Color(0xFFD1D1D1),
    characterText = Color(0xFF6B6B6B),
    avatarBackground = Color(0xFF6EE7B7),
    avatarText = Color(0xFF022C22)
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
