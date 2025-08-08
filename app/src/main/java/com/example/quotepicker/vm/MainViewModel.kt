package com.example.quotepicker.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class UiState(
    val groups: List<GroupEntity> = emptyList(),
    val currentGroupId: Long? = null,
    val quotes: List<QuoteEntity> = emptyList(),
    val randomResult: QuoteEntity? = null
)

class MainViewModel(app: Application): AndroidViewModel(app) {
    private val repo = Repository.get(app)

    private val _currentGroupId = MutableStateFlow<Long?>(null)
    private val _randomResult = MutableStateFlow<QuoteEntity?>(null)

    val uiState: StateFlow<UiState> = combine(
        repo.observeGroups(),
        _currentGroupId,
        repo.observeQuotes(null)
    ) { groups, gid, allQuotes ->
        val quotes = if (gid == null) allQuotes else allQuotes.filter { it.groupId == gid }
        UiState(
            groups = groups,
            currentGroupId = gid,
            quotes = quotes,
            randomResult = _randomResult.value
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun setGroup(id: Long?) { _currentGroupId.value = id }

    fun addGroup(name: String) = viewModelScope.launch { repo.addGroup(name) }
    fun deleteGroup(group: GroupEntity) = viewModelScope.launch { repo.deleteGroup(group) }
    fun addTextQuote(groupId: Long, text: String, weight: Int) = viewModelScope.launch {
        repo.addTextQuote(groupId, text, weight)
    }
    fun addImageQuote(groupId: Long, base64: String, weight: Int) = viewModelScope.launch {
        repo.addImageQuote(groupId, base64, weight)
    }
    fun deleteQuote(q: QuoteEntity) = viewModelScope.launch { repo.deleteQuote(q) }
    fun updateQuote(q: QuoteEntity) = viewModelScope.launch { repo.updateQuote(q) }

    fun pickRandom() = viewModelScope.launch {
        val gid = _currentGroupId.value
        val list = repo.observeQuotes(gid).first().filter { it.enabled && it.weight > 0 }
        val picked = weightedPick(list) { it.weight }
        _randomResult.value = picked
    }
    fun clearRandom() { _randomResult.value = null }

    private fun <T> weightedPick(items: List<T>, weightOf: (T)->Int): T? {
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