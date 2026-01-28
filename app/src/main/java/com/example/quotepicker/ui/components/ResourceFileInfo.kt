package com.example.quotepicker.ui.components

import android.net.Uri
import com.example.quotepicker.data.ResourceType

data class ResourceFileInfo(
    val type: ResourceType,
    val uri: Uri
)

fun encodeResourceFileInfo(type: ResourceType, uri: Uri): String {
    val encodedUri = Uri.encode(uri.toString())
    return "type=${type.name};uri=$encodedUri"
}

fun decodeResourceFileInfo(raw: String): ResourceFileInfo? {
    val trimmed = raw.trim()
    val match = Regex("type=([A-Z]+);uri=(.+)").find(trimmed) ?: return null
    val type = runCatching { ResourceType.valueOf(match.groupValues[1]) }.getOrNull() ?: return null
    val uriValue = Uri.decode(match.groupValues[2])
    return ResourceFileInfo(type = type, uri = Uri.parse(uriValue))
}
