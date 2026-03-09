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
    val indexedCode = Regex("^([ivstcf]\\d{4,})\\.(\\d+)$").find(trimmed)
    if (indexedCode != null) {
        val code = indexedCode.groupValues[1]
        val type = when (code.firstOrNull()) {
            'i' -> ResourceType.IMAGE
            'v' -> ResourceType.VIDEO
            's' -> ResourceType.SOUND
            't' -> ResourceType.TEXT
            'c' -> ResourceType.SCENE
            'f' -> ResourceType.FLOW
            else -> return null
        }
        val index = indexedCode.groupValues[2].toLongOrNull()?.takeIf { it > 0 } ?: return null
        return ResourceFileInfo(type = type, name = code, resourceId = index)
    }
    return null
}
