package com.anongram.app.core.db.schema.chats

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.LinkPreview
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus
import org.json.JSONArray
import org.json.JSONObject

data class PersistedChatMeta(
    val chatId: String,
    val title: String,
    val chatType: String,
    val fingerprint: String,
    val lastPreview: String,
    val lastTs: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val mutedUntilMs: Long?,
    val isDeleted: Boolean = false
)

/**
 * DAO для зашифрованных таблиц `chats` и `messages` в `anongram.db`.
 */
object ChatsDbDao {

    fun hasMessages(chatId: String): Boolean {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return false
        return runCatching {
            db.query(
                "SELECT 1 FROM messages WHERE chat_id = ? AND is_deleted = 0 LIMIT 1",
                arrayOf(chatId)
            ).use { it.moveToFirst() }
        }.getOrDefault(false)
    }

    fun loadMessages(chatId: String, limit: Int = 1000): List<ChatMessage> {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return emptyList()
        return runCatching {
            val out = mutableListOf<ChatMessage>()
            db.query(
                "SELECT message_id, sender_id, body, reply_to_id, status, payload_json, created_at, edited_at " +
                    "FROM messages WHERE chat_id = ? AND is_deleted = 0 ORDER BY created_at DESC LIMIT ?",
                arrayOf(chatId, limit.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val senderId = c.getString(1)
                    val body = c.getString(2)
                    val replyToId = if (c.isNull(3)) null else c.getString(3)
                    val statusOrd = c.getInt(4)
                    val payloadRaw = c.getString(5)
                    val createdAt = c.getLong(6)
                    val isEdited = !c.isNull(7) && c.getLong(7) > 0L
                    out += decodeMessage(id, senderId, body, replyToId, statusOrd, payloadRaw, createdAt, isEdited)
                }
            }
            out.asReversed()
        }.getOrDefault(emptyList())
    }

    fun upsertMessage(chatId: String, message: ChatMessage) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            db.insert("messages", SQLiteDatabase.CONFLICT_REPLACE, toContentValues(chatId, message))
        }
    }

    fun upsertMessagesBatch(chatId: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            db.beginTransaction()
            try {
                for (msg in messages) {
                    db.insert("messages", SQLiteDatabase.CONFLICT_REPLACE, toContentValues(chatId, msg))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun markMessageDeleted(messageId: String) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            db.execSQL("UPDATE messages SET is_deleted = 1 WHERE message_id = ?", arrayOf(messageId))
        }
    }

    fun clearChatMessages(chatId: String) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            db.delete("messages", "chat_id = ?", arrayOf(chatId))
        }
    }

    fun markAllRead(chatId: String) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            val msgs = loadMessages(chatId, 200).filter { it.isUnread }
            if (msgs.isNotEmpty()) {
                upsertMessagesBatch(chatId, msgs.map { it.copy(isUnread = false) })
            }
            db.execSQL("UPDATE chats SET unread_count = 0 WHERE chat_id = ?", arrayOf(chatId))
        }
    }

    fun upsertChatMeta(meta: PersistedChatMeta) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            val payload = JSONObject().apply {
                put("isDeleted", meta.isDeleted)
            }.toString()
            val cv = ContentValues().apply {
                put("chat_id", meta.chatId)
                put("title", meta.title)
                put("chat_type", meta.chatType)
                put("fingerprint", meta.fingerprint)
                put("last_msg_preview", meta.lastPreview)
                put("last_msg_ts", meta.lastTs)
                put("unread_count", meta.unreadCount)
                put("is_pinned", if (meta.isPinned) 1 else 0)
                put("is_archived", if (meta.isArchived) 1 else 0)
                if (meta.mutedUntilMs != null) put("muted_until_ms", meta.mutedUntilMs) else putNull("muted_until_ms")
                put("payload_json", payload)
                put("updated_at", System.currentTimeMillis())
            }
            db.insert("chats", SQLiteDatabase.CONFLICT_REPLACE, cv)
        }
    }

    fun loadAllChatMeta(): Map<String, PersistedChatMeta> {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return emptyMap()
        return runCatching {
            val map = linkedMapOf<String, PersistedChatMeta>()
            db.query(
                "SELECT chat_id, title, chat_type, fingerprint, last_msg_preview, last_msg_ts, unread_count, is_pinned, is_archived, muted_until_ms, payload_json FROM chats",
                emptyArray()
            ).use { c ->
                while (c.moveToNext()) {
                    val payload = runCatching { JSONObject(c.getString(10)) }.getOrNull()
                    val meta = PersistedChatMeta(
                        chatId = c.getString(0),
                        title = c.getString(1),
                        chatType = c.getString(2),
                        fingerprint = c.getString(3),
                        lastPreview = c.getString(4),
                        lastTs = c.getLong(5),
                        unreadCount = c.getInt(6),
                        isPinned = c.getInt(7) != 0,
                        isArchived = c.getInt(8) != 0,
                        mutedUntilMs = if (c.isNull(9)) null else c.getLong(9),
                        isDeleted = payload?.optBoolean("isDeleted", false) ?: false
                    )
                    map[meta.chatId] = meta
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun toContentValues(chatId: String, msg: ChatMessage): ContentValues {
        val payload = encodeMessagePayload(msg)
        return ContentValues().apply {
            put("message_id", msg.id)
            put("chat_id", chatId)
            put("sender_id", msg.direction.name)
            put("body", msg.text)
            put("media_type", attachmentTypeTag(msg.attachment))
            putNull("media_id")
            if (msg.replyToId != null) put("reply_to_id", msg.replyToId) else putNull("reply_to_id")
            put("status", msg.status.ordinal)
            put("payload_json", payload)
            put("created_at", msg.epochMs)
            if (msg.isEdited) put("edited_at", System.currentTimeMillis()) else putNull("edited_at")
            put("is_deleted", 0)
        }
    }

    private fun attachmentTypeTag(att: MessageAttachment?): String? = when (att) {
        is MessageAttachment.Photo -> if (att.isVideo) "video" else "photo"
        is MessageAttachment.MediaAlbum -> "album"
        is MessageAttachment.File -> "file"
        is MessageAttachment.FileGroup -> "file_group"
        is MessageAttachment.Music -> "music"
        is MessageAttachment.MusicGroup -> "music_group"
        is MessageAttachment.Voice -> "voice"
        is MessageAttachment.VideoCircle -> "video_note"
        is MessageAttachment.Contact -> "contact"
        is MessageAttachment.Geo -> "geo"
        is MessageAttachment.Sticker -> "sticker"
        null -> null
    }

    private fun encodeMessagePayload(msg: ChatMessage): String = JSONObject().apply {
        msg.replyToSubIndex?.let { put("replyToSubIndex", it) }
        if (msg.isUnread) put("isUnread", true)
        msg.forwardedFrom?.let { put("forwardedFrom", it) }
        msg.myReaction?.let { put("myReaction", it) }
        if (msg.reactions.isNotEmpty()) {
            val r = JSONObject()
            msg.reactions.forEach { (k, v) -> r.put(k, v) }
            put("reactions", r)
        }
        msg.linkPreview?.let { lp ->
            put("linkPreview", JSONObject().apply {
                put("url", lp.url)
                put("domain", lp.domain)
                put("title", lp.title)
                put("description", lp.description)
            })
        }
        msg.attachment?.let { att ->
            put("attachment", encodeAttachment(att))
        }
    }.toString()

    private fun encodeAttachment(att: MessageAttachment): JSONObject = when (att) {
        is MessageAttachment.Photo -> JSONObject().apply {
            put("type", "photo")
            put("label", att.label)
            put("colorSeed", att.colorSeed)
            put("isVideo", att.isVideo)
            att.uri?.let { put("uri", it) }
            put("hasSpoiler", att.hasSpoiler)
            att.coverUri?.let { put("coverUri", it) }
        }
        is MessageAttachment.MediaAlbum -> JSONObject().apply {
            put("type", "album")
            put("hasSpoiler", att.hasSpoiler)
            put("items", JSONArray().apply {
                att.items.forEach { item ->
                    put(JSONObject().apply {
                        put("uri", item.uri)
                        put("isVideo", item.isVideo)
                        put("hasSpoiler", item.hasSpoiler)
                        item.coverUri?.let { put("coverUri", it) }
                    })
                }
            })
        }
        is MessageAttachment.File -> encodeFile(att)
        is MessageAttachment.FileGroup -> JSONObject().apply {
            put("type", "file_group")
            put("items", JSONArray().apply { att.items.forEach { put(encodeFile(it)) } })
        }
        is MessageAttachment.Music -> encodeMusic(att)
        is MessageAttachment.MusicGroup -> JSONObject().apply {
            put("type", "music_group")
            put("items", JSONArray().apply { att.items.forEach { put(encodeMusic(it)) } })
        }
        is MessageAttachment.Voice -> JSONObject().apply {
            put("type", "voice")
            put("durationLabel", att.durationLabel)
            put("waveform", JSONArray(att.waveform))
            att.uri?.let { put("uri", it) }
        }
        is MessageAttachment.VideoCircle -> JSONObject().apply {
            put("type", "video_circle")
            put("durationLabel", att.durationLabel)
            put("colorSeed", att.colorSeed)
            att.uri?.let { put("uri", it) }
        }
        is MessageAttachment.Contact -> JSONObject().apply {
            put("type", "contact")
            put("name", att.name)
            put("phone", att.phone)
            put("fingerprint", att.fingerprint)
            put("isSasVerified", att.isSasVerified)
        }
        is MessageAttachment.Geo -> JSONObject().apply {
            put("type", "geo")
            put("address", att.address)
            put("lat", att.lat)
            put("lon", att.lon)
        }
        is MessageAttachment.Sticker -> JSONObject().apply {
            put("type", "sticker")
            put("symbol", att.symbol)
            put("colorSeed", att.colorSeed)
        }
    }

    private fun encodeFile(f: MessageAttachment.File) = JSONObject().apply {
        put("type", "file")
        put("name", f.name)
        put("sizeLabel", f.sizeLabel)
        put("kind", f.kind.name)
        f.uri?.let { put("uri", it) }
    }

    private fun encodeMusic(m: MessageAttachment.Music) = JSONObject().apply {
        put("type", "music")
        put("title", m.title)
        put("artist", m.artist)
        put("durationLabel", m.durationLabel)
        put("colorSeed", m.colorSeed)
        m.uri?.let { put("uri", it) }
        put("sizeLabel", m.sizeLabel)
    }

    private fun decodeMessage(
        id: String,
        senderId: String,
        body: String,
        replyToId: String?,
        statusOrd: Int,
        payloadRaw: String,
        createdAt: Long,
        isEdited: Boolean
    ): ChatMessage {
        val dir = if (senderId == MessageDirection.OUTGOING.name) MessageDirection.OUTGOING else MessageDirection.INCOMING
        val status = MessageStatus.entries.getOrElse(statusOrd) { MessageStatus.READ }
        val obj = runCatching { JSONObject(payloadRaw) }.getOrNull()
        val reactions = obj?.optJSONObject("reactions")?.let { r ->
            buildMap {
                r.keys().forEach { k -> put(k, r.optInt(k, 1)) }
            }
        } ?: emptyMap()
        val lp = obj?.optJSONObject("linkPreview")?.let { o ->
            LinkPreview(
                url = o.optString("url"),
                domain = o.optString("domain"),
                title = o.optString("title"),
                description = o.optString("description")
            )
        }
        val att = obj?.optJSONObject("attachment")?.let { decodeAttachment(it) }
        return ChatMessage(
            id = id,
            direction = dir,
            text = body,
            epochMs = createdAt,
            status = status,
            replyToId = replyToId,
            replyToSubIndex = obj?.takeIf { it.has("replyToSubIndex") }?.optInt("replyToSubIndex"),
            isEdited = isEdited,
            isUnread = obj?.optBoolean("isUnread", false) ?: false,
            forwardedFrom = obj?.optString("forwardedFrom", "")?.ifEmpty { null },
            attachment = att,
            linkPreview = lp,
            reactions = reactions,
            myReaction = obj?.optString("myReaction", "")?.ifEmpty { null }
        )
    }

    private fun decodeAttachment(o: JSONObject): MessageAttachment? = when (o.optString("type")) {
        "photo" -> MessageAttachment.Photo(
            label = o.optString("label"),
            colorSeed = o.optInt("colorSeed", 0),
            isVideo = o.optBoolean("isVideo", false),
            uri = o.optString("uri", "").ifEmpty { null },
            hasSpoiler = o.optBoolean("hasSpoiler", false),
            coverUri = o.optString("coverUri", "").ifEmpty { null }
        )
        "album" -> {
            val arr = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until arr.length()).mapNotNull { idx ->
                val item = arr.optJSONObject(idx) ?: return@mapNotNull null
                MessageAttachment.AlbumMediaItem(
                    uri = item.optString("uri"),
                    isVideo = item.optBoolean("isVideo", false),
                    hasSpoiler = item.optBoolean("hasSpoiler", false),
                    coverUri = item.optString("coverUri", "").ifEmpty { null }
                )
            }
            MessageAttachment.MediaAlbum(items = items, hasSpoiler = o.optBoolean("hasSpoiler", false))
        }
        "file" -> decodeFile(o)
        "file_group" -> {
            val arr = o.optJSONArray("items") ?: JSONArray()
            MessageAttachment.FileGroup((0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::decodeFile) })
        }
        "music" -> decodeMusic(o)
        "music_group" -> {
            val arr = o.optJSONArray("items") ?: JSONArray()
            MessageAttachment.MusicGroup((0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::decodeMusic) })
        }
        "voice" -> {
            val arr = o.optJSONArray("waveform") ?: JSONArray()
            MessageAttachment.Voice(
                durationLabel = o.optString("durationLabel", "0:05"),
                waveform = List(arr.length()) { arr.optInt(it, 6) },
                uri = o.optString("uri", "").ifEmpty { null }
            )
        }
        "video_circle" -> MessageAttachment.VideoCircle(
            durationLabel = o.optString("durationLabel", "0:05"),
            colorSeed = o.optInt("colorSeed", 0),
            uri = o.optString("uri", "").ifEmpty { null }
        )
        "contact" -> MessageAttachment.Contact(
            name = o.optString("name"),
            phone = o.optString("phone"),
            fingerprint = o.optString("fingerprint"),
            isSasVerified = o.optBoolean("isSasVerified", false)
        )
        "geo" -> MessageAttachment.Geo(
            address = o.optString("address"),
            lat = o.optDouble("lat", 0.0),
            lon = o.optDouble("lon", 0.0)
        )
        "sticker" -> MessageAttachment.Sticker(
            symbol = o.optString("symbol"),
            colorSeed = o.optInt("colorSeed", 0)
        )
        else -> null
    }

    private fun decodeFile(o: JSONObject) = MessageAttachment.File(
        name = o.optString("name"),
        sizeLabel = o.optString("sizeLabel"),
        kind = FileKind.entries.firstOrNull { it.name == o.optString("kind") } ?: FileKind.UNKNOWN,
        uri = o.optString("uri", "").ifEmpty { null }
    )

    private fun decodeMusic(o: JSONObject) = MessageAttachment.Music(
        title = o.optString("title"),
        artist = o.optString("artist"),
        durationLabel = o.optString("durationLabel"),
        colorSeed = o.optInt("colorSeed", 0),
        uri = o.optString("uri", "").ifEmpty { null },
        sizeLabel = o.optString("sizeLabel", "3.7 MB")
    )
}
