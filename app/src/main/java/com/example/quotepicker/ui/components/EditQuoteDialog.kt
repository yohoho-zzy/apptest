package com.example.quotepicker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.quotepicker.data.QuoteEntity

@Composable
fun EditQuoteDialog(
    quote: QuoteEntity,
    onDismiss: ()->Unit,
    onConfirm: (String, Int)->Unit
) {
    var text by remember { mutableStateOf(quote.text ?: "") }
    var weight by remember { mutableStateOf(quote.weight.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑内容") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("名称") },
                    modifier = Modifier
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() } },
                    label = { Text("权重") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toIntOrNull() ?: quote.weight
                    onConfirm(text.trim(), w)
                },
                enabled = text.isNotBlank() && weight.isNotBlank()
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
