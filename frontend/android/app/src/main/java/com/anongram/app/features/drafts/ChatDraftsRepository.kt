package com.anongram.app.features.drafts

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.anongram.app.core.db.AnonGramEncryptedDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChatDraftsRepository private constructor() {
    private val _drafts = MutableStateFlow<Map<String, ChatDraft>>(emptyMap())
    val drafts: StateFlow<Map<String, ChatDraft>> = _drafts.asStateFlow()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        ioScope.launch {
            AnonGramEncryptedDb.unlockedFlow.collect { unlocked ->
                if (unlocked) {
                    loadFromDb()
                }
            }
        }
    }

    fun getDraft(chatId: String): ChatDraft? = _drafts.value[chatId]

    fun saveDraft(draft: ChatDraft) {
        val fresh = draft.copy(updatedAtEpochMs = System.currentTimeMillis())
        _drafts.update { current ->
            if (draft.isEmpty) current - draft.chatId else current + (draft.chatId to fresh)
        }
        ioScope.launch {
            val db = AnonGramEncryptedDb.getDbOrNull() ?: return@launch
            runCatching {
                if (draft.isEmpty) {
                    db.delete("chat_drafts", "chat_id = ?", arrayOf(draft.chatId))
                } else {
                    val cv = ContentValues().apply {
                        put("chat_id", fresh.chatId)
                        put("draft_text", encodeDraftJson(fresh))
                        if (fresh.replyToMessageId != null) put("reply_to_id", fresh.replyToMessageId)
                        else putNull("reply_to_id")
                        put("updated_at", fresh.updatedAtEpochMs)
                    }
                    db.insert("chat_drafts", SQLiteDatabase.CONFLICT_REPLACE, cv)
                }
            }
        }
    }

    fun clearDraft(chatId: String) {
        _drafts.update { it - chatId }
        ioScope.launch {
            val db = AnonGramEncryptedDb.getDbOrNull() ?: return@launch
            runCatching {
                db.delete("chat_drafts", "chat_id = ?", arrayOf(chatId))
            }
        }
    }

    private fun loadFromDb() {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        val loaded = runCatching {
            val map = linkedMapOf<String, ChatDraft>()
            db.query("SELECT chat_id, draft_text, reply_to_id, updated_at FROM chat_drafts", emptyArray()).use { c ->
                while (c.moveToNext()) {
                    val chatId = c.getString(0)
                    val raw = c.getString(1)
                    val replyId = if (c.isNull(2)) null else c.getString(2)
                    val updatedAt = c.getLong(3)
                    val decoded = decodeDraft(chatId, raw, replyId, updatedAt)
                    if (!decoded.isEmpty) {
                        map[chatId] = decoded
                    }
                }
            }
            map
        }.getOrNull() ?: return

        _drafts.update { current -> loaded + current }
    }

    private fun encodeDraftJson(draft: ChatDraft): String = JSONObject().apply {
        put("text", draft.text)
        draft.replyToMessageId?.let { put("replyToMessageId", it) }
        draft.replyToSubIndex?.let { put("replyToSubIndex", it) }
        draft.replySnippet?.let { put("replySnippet", it) }
        draft.voiceUri?.let { put("voiceUri", it) }
        draft.voiceDurationLabel?.let { put("voiceDurationLabel", it) }
        if (draft.voiceWaveform.isNotEmpty()) {
            put("voiceWaveform", JSONArray(draft.voiceWaveform))
        }
        draft.videoUri?.let { put("videoUri", it) }
    }.toString()

    private fun decodeDraft(chatId: String, raw: String, replyId: String?, updatedAt: Long): ChatDraft {
        if (!raw.startsWith("{")) {
            return ChatDraft(chatId = chatId, text = raw, replyToMessageId = replyId, updatedAtEpochMs = updatedAt)
        }
        return runCatching {
            val obj = JSONObject(raw)
            val wf = obj.optJSONArray("voiceWaveform")?.let { arr ->
                List(arr.length()) { idx -> arr.optInt(idx, 0) }
            } ?: emptyList()
            ChatDraft(
                chatId = chatId,
                text = obj.optString("text", ""),
                replyToMessageId = obj.optString("replyToMessageId", "").ifEmpty { replyId },
                replyToSubIndex = if (obj.has("replyToSubIndex")) obj.optInt("replyToSubIndex") else null,
                replySnippet = obj.optString("replySnippet", "").ifEmpty { null },
                voiceUri = obj.optString("voiceUri", "").ifEmpty { null },
                voiceDurationLabel = obj.optString("voiceDurationLabel", "").ifEmpty { null },
                voiceWaveform = wf,
                videoUri = obj.optString("videoUri", "").ifEmpty { null },
                updatedAtEpochMs = updatedAt
            )
        }.getOrElse {
            ChatDraft(chatId = chatId, text = raw, replyToMessageId = replyId, updatedAtEpochMs = updatedAt)
        }
    }

    companion object {
        val instance = ChatDraftsRepository()
    }
}
