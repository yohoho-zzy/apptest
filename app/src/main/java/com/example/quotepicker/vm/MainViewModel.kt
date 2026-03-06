package com.example.quotepicker.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.GroupEntity
import com.example.quotepicker.data.QuoteEntity
import com.example.quotepicker.data.QuoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class MainUiState(
    val groups: List<GroupEntity> = emptyList(),
    val currentGroupId: Long? = null,
    val quotes: List<QuoteEntity> = emptyList(),
    val randomResult: QuoteEntity? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(
        MainUiState(
            groups = listOf(GroupEntity(id = 1, name = "默认分组")),
            currentGroupId = null,
            quotes = emptyList(),
            randomResult = null
        )
    )
    val uiState: StateFlow<MainUiState> = _state.asStateFlow()

    private var nextGroupId = 2L
    private var nextQuoteId = 1L

    fun setGroup(id: Long?) {
        _state.value = _state.value.copy(currentGroupId = id)
    }

    fun addGroup(name: String) {
        val group = GroupEntity(id = nextGroupId++, name = name)
        _state.value = _state.value.copy(groups = _state.value.groups + group)
    }

    fun deleteGroup(group: GroupEntity) {
        val remainingGroups = _state.value.groups.filterNot { it.id == group.id }
        val remainingQuotes = _state.value.quotes.filterNot { it.groupId == group.id }
        val current = if (_state.value.currentGroupId == group.id) null else _state.value.currentGroupId
        _state.value = _state.value.copy(groups = remainingGroups, quotes = remainingQuotes, currentGroupId = current)
    }

    fun addTextQuote(groupId: Long, text: String, weight: Int) {
        val quote = QuoteEntity(
            id = nextQuoteId++,
            groupId = groupId,
            type = QuoteType.TEXT,
            text = text,
            weight = weight.coerceAtLeast(1),
            enabled = true
        )
        _state.value = _state.value.copy(quotes = _state.value.quotes + quote)
    }

    fun addImageQuote(groupId: Long, base64: String, weight: Int) {
        val quote = QuoteEntity(
            id = nextQuoteId++,
            groupId = groupId,
            type = QuoteType.IMAGE,
            imageBase64 = base64,
            weight = weight.coerceAtLeast(1),
            enabled = true
        )
        _state.value = _state.value.copy(quotes = _state.value.quotes + quote)
    }

    fun deleteQuote(q: QuoteEntity) {
        _state.value = _state.value.copy(quotes = _state.value.quotes.filterNot { it.id == q.id })
    }

    fun updateQuote(q: QuoteEntity) {
        _state.value = _state.value.copy(
            quotes = _state.value.quotes.map { if (it.id == q.id) q else it }
        )
    }

    fun pickRandom() = viewModelScope.launch {
        val gid = _state.value.currentGroupId
        val candidates = _state.value.quotes
            .filter { it.enabled && it.weight > 0 }
            .filter { gid == null || it.groupId == gid }
        val picked = weightedPick(candidates) { it.weight }
        _state.value = _state.value.copy(randomResult = picked)
    }

    fun clearRandom() {
        _state.value = _state.value.copy(randomResult = null)
    }

    private fun <T> weightedPick(items: List<T>, weightOf: (T) -> Int): T? {
        val list = items.filter { weightOf(it) > 0 }
        if (list.isEmpty()) return null
        val total = list.sumOf { weightOf(it) }
        val r = (1..total).random()
        var acc = 0
        for (e in list) {
            acc += weightOf(e)
            if (r <= acc) return e
        }
        return list.first()
    }

    fun encodeImageToBase64(uri: Uri): String {
        val cr = getApplication<Application>().contentResolver
        cr.openInputStream(uri).use { input ->
            val bmp = BitmapFactory.decodeStream(input)
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }
    }

    fun decodeBase64ToBitmap(b64: String): Bitmap {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
