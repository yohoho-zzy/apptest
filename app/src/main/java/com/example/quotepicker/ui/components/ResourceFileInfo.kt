package com.example.quotepicker.ui.components

import android.net.Uri
import com.example.quotepicker.data.ResourceType

data class ResourceFileInfo(
    val type: ResourceType,
    val uri: Uri
)

fun encodeResourceFileInfo(type: ResourceType, uri: Uri): String {
    val typeCode = when (type) {
        ResourceType.IMAGE -> "i"
        ResourceType.VIDEO -> "v"
        ResourceType.SOUND -> "s"
        ResourceType.TEXT -> "t"
        ResourceType.SCENE -> "c"
        else -> {""}
    }
    val encodedUri = Uri.encode(uri.toString())
    return "$typeCode,$encodedUri"
}

fun decodeResourceFileInfo(raw: String): ResourceFileInfo? {
    val trimmed = raw.trim()
    val compact = Regex("^([ivstc]),(.+)$").find(trimmed)
    val legacy = Regex("type=([A-Z]+);uri=(.+)").find(trimmed)
    val (type, encodedValue) = when {
        compact != null -> {
            val mappedType = when (compact.groupValues[1]) {
                "i" -> ResourceType.IMAGE
                "v" -> ResourceType.VIDEO
                "s" -> ResourceType.SOUND
                "t" -> ResourceType.TEXT
                "c" -> ResourceType.SCENE
                else -> null
            } ?: return null
            mappedType to compact.groupValues[2]
        }
        legacy != null -> {
            val legacyType = runCatching { ResourceType.valueOf(legacy.groupValues[1]) }.getOrNull() ?: return null
            legacyType to legacy.groupValues[2]
        }
        else -> return null
    }
    val uriValue = Uri.decode(encodedValue)
    return ResourceFileInfo(type = type, uri = Uri.parse(uriValue))
}
