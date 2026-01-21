package com.example.quotepicker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val birthdayFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
private val randomTokenRegex = Regex("【【(.*?)】】")

fun formatTagLabel(name: String, today: LocalDate = LocalDate.now()): String {
    val birthday = runCatching { LocalDate.parse(name, birthdayFormatter) }.getOrNull() ?: return name
    val age = if (birthday.isAfter(today)) 0 else Period.between(birthday, today).years
    return "$name($age)"
}

fun formatDisplayText(text: String, random: Random = Random.Default): String {
    if (text.isBlank()) return text
    val replaced = randomTokenRegex.replace(text) { match ->
        val options = match.groupValues[1]
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (options.isEmpty()) {
            match.value
        } else {
            "【${options.random(random)}】"
        }
    }
    return replaced
}

@Composable
fun rememberFormattedText(text: String): String {
    return remember(text) { formatDisplayText(text) }
}
