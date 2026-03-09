package com.example.quotepicker.ui.components

import android.net.Uri
import com.example.quotepicker.data.ResourceType

data class ResourceFileInfo(
    val type: ResourceType,
    val name: String,
    val resourceId: Long? = null
)

fun encodeResourceFileInfo(type: ResourceType, uri: Uri, resourceId: Long? = null): String {
    val typeCode = when (type) {
        ResourceType.IMAGE -> "i"
        ResourceType.VIDEO -> "v"
        ResourceType.SOUND -> "s"
        ResourceType.TEXT -> "t"
        ResourceType.SCENE -> "c"
        else -> {""}
    }
    val displayName = uri.lastPathSegment?.takeIf { it.isNotBlank() }
        ?: uri.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: uri.toString()
    val encodedName = Uri.encode(displayName)
    return if (resourceId != null) "$typeCode,$encodedName,$resourceId" else "$typeCode,$encodedName"
}

fun decodeResourceFileInfo(raw: String): ResourceFileInfo? {
    val trimmed = raw.trim()
    val codeOnly = Regex("^([ivstcf])\\d{4,}$").find(trimmed)
    if (codeOnly != null) {
        val type = when (codeOnly.groupValues[1]) {
            "i" -> ResourceType.IMAGE
            "v" -> ResourceType.VIDEO
            "s" -> ResourceType.SOUND
            "t" -> ResourceType.TEXT
            "c" -> ResourceType.SCENE
            "f" -> ResourceType.FLOW
            else -> return null
        }
        return ResourceFileInfo(type = type, name = trimmed, resourceId = null)
    }
    val compact = Regex("^([ivstc]),([^,]+?)(?:,(\\d+))?$").find(trimmed)
    val legacy = Regex("type=([A-Z]+);uri=(.+)").find(trimmed)
    val (type, encodedValue, resourceId) = when {
        compact != null -> {
            val mappedType = when (compact.groupValues[1]) {
                "i" -> ResourceType.IMAGE
                "v" -> ResourceType.VIDEO
                "s" -> ResourceType.SOUND
                "t" -> ResourceType.TEXT
                "c" -> ResourceType.SCENE
                else -> null
            } ?: return null
            Triple(mappedType, compact.groupValues[2], compact.groupValues.getOrNull(3)?.toLongOrNull())
        }
        legacy != null -> {
            val legacyType = runCatching { ResourceType.valueOf(legacy.groupValues[1]) }.getOrNull() ?: return null
            val uri = Uri.parse(Uri.decode(legacy.groupValues[2]))
            val name = uri.lastPathSegment?.takeIf { it.isNotBlank() }
                ?: uri.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: uri.toString()
            Triple(legacyType, Uri.encode(name), null)
        }
        else -> return null
    }
    val nameValue = Uri.decode(encodedValue)
    return ResourceFileInfo(type = type, name = nameValue, resourceId = resourceId)
}
