package com.anongram.app.features.chats.ui.input.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Менеджер стека отмены и повтора правок текста (Undo / Redo) с поддержкой TextFieldValue и курсора.
 */
class UndoRedoState(initialText: String = "") {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    var textFieldValue by mutableStateOf(
        TextFieldValue(text = initialText, selection = TextRange(initialText.length))
    )
        private set

    val currentText: String
        get() = textFieldValue.text

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun updateValue(newValue: TextFieldValue) {
        if (newValue.text != textFieldValue.text) {
            undoStack.add(textFieldValue.text)
            if (undoStack.size > 100) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
        }
        textFieldValue = newValue
    }

    fun updateText(newText: String) {
        if (newText != textFieldValue.text) {
            undoStack.add(textFieldValue.text)
            if (undoStack.size > 100) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
            textFieldValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
        }
    }

    fun undo(): String? {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(text = prev, selection = TextRange(prev.length))
            return prev
        }
        return null
    }

    fun redo(): String? {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue.text)
            textFieldValue = TextFieldValue(text = next, selection = TextRange(next.length))
            return next
        }
        return null
    }
}

@Composable
fun rememberUndoRedoState(initialText: String): UndoRedoState {
    return remember { UndoRedoState(initialText) }
}
