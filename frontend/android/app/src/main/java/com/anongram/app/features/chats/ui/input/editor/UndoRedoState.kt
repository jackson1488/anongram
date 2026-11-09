package com.anongram.app.features.chats.ui.input.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Менеджер стека отмены и повтора правок текста (Undo / Redo).
 */
class UndoRedoState(initialText: String = "") {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    var currentText by mutableStateOf(initialText)
        private set

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun updateText(newText: String) {
        if (newText != currentText) {
            undoStack.add(currentText)
            if (undoStack.size > 100) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
            currentText = newText
        }
    }

    fun undo(): String? {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentText)
            currentText = prev
            return prev
        }
        return null
    }

    fun redo(): String? {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentText)
            currentText = next
            return next
        }
        return null
    }
}

@Composable
fun rememberUndoRedoState(initialText: String): UndoRedoState {
    return remember { UndoRedoState(initialText) }
}
