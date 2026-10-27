package com.griaai.app.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.griaai.app.domain.model.Conversation
import com.griaai.app.domain.model.Message
import com.griaai.app.domain.model.MessageRole
import com.griaai.app.domain.model.MessageStatus
import com.griaai.app.domain.model.Persona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class GriaDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "gria_ai.db"
        const val DATABASE_VERSION = 3

        const val TABLE_CONVERSATIONS = "conversations"
        const val COL_CONV_ID = "id"
        const val COL_CONV_TITLE = "title"
        const val COL_CONV_CREATED_AT = "created_at"
        const val COL_CONV_UPDATED_AT = "updated_at"
        const val COL_CONV_MODEL_ID = "model_id"
        const val COL_CONV_IS_PINNED = "is_pinned"
        const val COL_CONV_SYSTEM_PROMPT = "system_prompt"

        const val TABLE_MESSAGES = "messages"
        const val COL_MSG_ID = "id"
        const val COL_MSG_CONV_ID = "conversation_id"
        const val COL_MSG_ROLE = "role"
        const val COL_MSG_CONTENT = "content"
        const val COL_MSG_TIMESTAMP = "timestamp"
        const val COL_MSG_STATUS = "status"
        const val COL_MSG_ERROR = "error_message"
        const val COL_MSG_TOKENS_COUNT = "tokens_count"
        const val COL_MSG_DURATION_MS = "duration_ms"
        const val COL_MSG_TOK_PER_SEC = "tok_per_sec"
        const val COL_MSG_IMAGE_URI = "image_uri"

        const val TABLE_CUSTOM_PERSONAS = "custom_personas"
        const val COL_PERSONA_ID = "id"
        const val COL_PERSONA_TITLE = "title"
        const val COL_PERSONA_SUBTITLE = "subtitle"
        const val COL_PERSONA_SYSTEM_PROMPT = "system_prompt"
        const val COL_PERSONA_CREATED_AT = "created_at"
    }

    private val _dbChangeTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    val dbChangeTrigger: Flow<Unit> = _dbChangeTrigger.asSharedFlow()

    private fun notifyChanged() {
        _dbChangeTrigger.tryEmit(Unit)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CONVERSATIONS (
                $COL_CONV_ID TEXT PRIMARY KEY,
                $COL_CONV_TITLE TEXT NOT NULL,
                $COL_CONV_CREATED_AT INTEGER NOT NULL,
                $COL_CONV_UPDATED_AT INTEGER NOT NULL,
                $COL_CONV_MODEL_ID TEXT NOT NULL,
                $COL_CONV_IS_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_CONV_SYSTEM_PROMPT TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID TEXT PRIMARY KEY,
                $COL_MSG_CONV_ID TEXT NOT NULL,
                $COL_MSG_ROLE TEXT NOT NULL,
                $COL_MSG_CONTENT TEXT NOT NULL,
                $COL_MSG_TIMESTAMP INTEGER NOT NULL,
                $COL_MSG_STATUS TEXT NOT NULL,
                $COL_MSG_ERROR TEXT,
                $COL_MSG_TOKENS_COUNT INTEGER,
                $COL_MSG_DURATION_MS INTEGER,
                $COL_MSG_TOK_PER_SEC REAL,
                $COL_MSG_IMAGE_URI TEXT,
                FOREIGN KEY ($COL_MSG_CONV_ID) REFERENCES $TABLE_CONVERSATIONS($COL_CONV_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CUSTOM_PERSONAS (
                $COL_PERSONA_ID TEXT PRIMARY KEY,
                $COL_PERSONA_TITLE TEXT NOT NULL,
                $COL_PERSONA_SUBTITLE TEXT NOT NULL,
                $COL_PERSONA_SYSTEM_PROMPT TEXT NOT NULL,
                $COL_PERSONA_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX idx_messages_conv_id ON $TABLE_MESSAGES ($COL_MSG_CONV_ID)")
        db.execSQL("CREATE INDEX idx_messages_timestamp ON $TABLE_MESSAGES ($COL_MSG_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_conversations_updated ON $TABLE_CONVERSATIONS ($COL_CONV_UPDATED_AT DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MSG_TOKENS_COUNT INTEGER")
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MSG_DURATION_MS INTEGER")
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MSG_TOK_PER_SEC REAL")
            } catch (ignored: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COL_MSG_IMAGE_URI TEXT")
            } catch (ignored: Exception) {}
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_CUSTOM_PERSONAS (
                        $COL_PERSONA_ID TEXT PRIMARY KEY,
                        $COL_PERSONA_TITLE TEXT NOT NULL,
                        $COL_PERSONA_SUBTITLE TEXT NOT NULL,
                        $COL_PERSONA_SYSTEM_PROMPT TEXT NOT NULL,
                        $COL_PERSONA_CREATED_AT INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            } catch (ignored: Exception) {}
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL(
                "UPDATE $TABLE_CONVERSATIONS SET $COL_CONV_SYSTEM_PROMPT = REPLACE($COL_CONV_SYSTEM_PROMPT, 'Nexus AI', 'Gria-AI') WHERE $COL_CONV_SYSTEM_PROMPT LIKE '%Nexus AI%'"
            )
            db.execSQL(
                "UPDATE $TABLE_CONVERSATIONS SET $COL_CONV_SYSTEM_PROMPT = REPLACE($COL_CONV_SYSTEM_PROMPT, 'Nexus', 'Gria-AI') WHERE $COL_CONV_SYSTEM_PROMPT LIKE '%Nexus%'"
            )
        } catch (_: Exception) {}
    }

    // --- Conversation Operations ---

    suspend fun insertOrUpdateConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_CONV_ID, conversation.id)
            put(COL_CONV_TITLE, conversation.title)
            put(COL_CONV_CREATED_AT, conversation.createdAt)
            put(COL_CONV_UPDATED_AT, conversation.updatedAt)
            put(COL_CONV_MODEL_ID, conversation.modelId)
            put(COL_CONV_IS_PINNED, if (conversation.isPinned) 1 else 0)
            put(COL_CONV_SYSTEM_PROMPT, conversation.systemPrompt)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_CONVERSATIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        notifyChanged()
    }

    suspend fun getConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Conversation>()
        val cursor: Cursor = readableDatabase.query(
            TABLE_CONVERSATIONS,
            null,
            null,
            null,
            null,
            null,
            "$COL_CONV_IS_PINNED DESC, $COL_CONV_UPDATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Conversation(
                        id = it.getString(it.getColumnIndexOrThrow(COL_CONV_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_CONV_TITLE)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_UPDATED_AT)),
                        modelId = it.getString(it.getColumnIndexOrThrow(COL_CONV_MODEL_ID)),
                        isPinned = it.getInt(it.getColumnIndexOrThrow(COL_CONV_IS_PINNED)) == 1,
                        systemPrompt = it.getString(it.getColumnIndexOrThrow(COL_CONV_SYSTEM_PROMPT))
                    )
                )
            }
        }
        list
    }

    suspend fun getConversationById(id: String): Conversation? = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query(
            TABLE_CONVERSATIONS,
            null,
            "$COL_CONV_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                Conversation(
                    id = it.getString(it.getColumnIndexOrThrow(COL_CONV_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_CONV_TITLE)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_CREATED_AT)),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_UPDATED_AT)),
                    modelId = it.getString(it.getColumnIndexOrThrow(COL_CONV_MODEL_ID)),
                    isPinned = it.getInt(it.getColumnIndexOrThrow(COL_CONV_IS_PINNED)) == 1,
                    systemPrompt = it.getString(it.getColumnIndexOrThrow(COL_CONV_SYSTEM_PROMPT))
                )
            } else null
        }
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_CONVERSATIONS, "$COL_CONV_ID = ?", arrayOf(id))
        notifyChanged()
    }

    suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
        writableDatabase.delete(TABLE_CONVERSATIONS, null, null)
        notifyChanged()
    }

    // --- Message Operations ---

    suspend fun insertOrUpdateMessage(message: Message) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_MSG_ID, message.id)
            put(COL_MSG_CONV_ID, message.conversationId)
            put(COL_MSG_ROLE, message.role.name)
            put(COL_MSG_CONTENT, message.content)
            put(COL_MSG_TIMESTAMP, message.timestamp)
            put(COL_MSG_STATUS, message.status.name)
            put(COL_MSG_ERROR, message.errorMessage)
            put(COL_MSG_TOKENS_COUNT, message.tokensCount)
            put(COL_MSG_DURATION_MS, message.generationDurationMs)
            put(COL_MSG_TOK_PER_SEC, message.tokensPerSecond)
            put(COL_MSG_IMAGE_URI, message.imageUri)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_MESSAGES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        // Also update conversation timestamp
        val convValues = ContentValues().apply {
            put(COL_CONV_UPDATED_AT, message.timestamp)
        }
        writableDatabase.update(
            TABLE_CONVERSATIONS,
            convValues,
            "$COL_CONV_ID = ?",
            arrayOf(message.conversationId)
        )

        notifyChanged()
    }

    suspend fun getMessagesForConversation(convId: String): List<Message> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Message>()
        val cursor = readableDatabase.query(
            TABLE_MESSAGES,
            null,
            "$COL_MSG_CONV_ID = ?",
            arrayOf(convId),
            null,
            null,
            "$COL_MSG_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val tokensIdx = it.getColumnIndex(COL_MSG_TOKENS_COUNT)
                val durationIdx = it.getColumnIndex(COL_MSG_DURATION_MS)
                val tokPerSecIdx = it.getColumnIndex(COL_MSG_TOK_PER_SEC)
                val imageUriIdx = it.getColumnIndex(COL_MSG_IMAGE_URI)

                list.add(
                    Message(
                        id = it.getString(it.getColumnIndexOrThrow(COL_MSG_ID)),
                        conversationId = it.getString(it.getColumnIndexOrThrow(COL_MSG_CONV_ID)),
                        role = MessageRole.valueOf(it.getString(it.getColumnIndexOrThrow(COL_MSG_ROLE))),
                        content = it.getString(it.getColumnIndexOrThrow(COL_MSG_CONTENT)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_MSG_TIMESTAMP)),
                        status = MessageStatus.valueOf(it.getString(it.getColumnIndexOrThrow(COL_MSG_STATUS))),
                        errorMessage = it.getString(it.getColumnIndexOrThrow(COL_MSG_ERROR)),
                        tokensCount = if (tokensIdx != -1 && !it.isNull(tokensIdx)) it.getInt(tokensIdx) else null,
                        generationDurationMs = if (durationIdx != -1 && !it.isNull(durationIdx)) it.getLong(durationIdx) else null,
                        tokensPerSecond = if (tokPerSecIdx != -1 && !it.isNull(tokPerSecIdx)) it.getFloat(tokPerSecIdx) else null,
                        imageUri = if (imageUriIdx != -1 && !it.isNull(imageUriIdx)) it.getString(imageUriIdx) else null
                    )
                )
            }
        }
        list
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_MESSAGES, "$COL_MSG_ID = ?", arrayOf(messageId))
        notifyChanged()
    }

    suspend fun searchMessages(query: String): List<Message> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Message>()
        val cursor = readableDatabase.query(
            TABLE_MESSAGES,
            null,
            "$COL_MSG_CONTENT LIKE ?",
            arrayOf("%$query%"),
            null,
            null,
            "$COL_MSG_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Message(
                        id = it.getString(it.getColumnIndexOrThrow(COL_MSG_ID)),
                        conversationId = it.getString(it.getColumnIndexOrThrow(COL_MSG_CONV_ID)),
                        role = MessageRole.valueOf(it.getString(it.getColumnIndexOrThrow(COL_MSG_ROLE))),
                        content = it.getString(it.getColumnIndexOrThrow(COL_MSG_CONTENT)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_MSG_TIMESTAMP)),
                        status = MessageStatus.valueOf(it.getString(it.getColumnIndexOrThrow(COL_MSG_STATUS))),
                        errorMessage = it.getString(it.getColumnIndexOrThrow(COL_MSG_ERROR))
                    )
                )
            }
        }
        list
    }

    // --- Custom Persona Operations ---

    suspend fun getCustomPersonas(): List<Persona> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Persona>()
        try {
            val cursor = readableDatabase.query(
                TABLE_CUSTOM_PERSONAS,
                null,
                null,
                null,
                null,
                null,
                "$COL_PERSONA_CREATED_AT ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        Persona(
                            id = it.getString(it.getColumnIndexOrThrow(COL_PERSONA_ID)),
                            title = it.getString(it.getColumnIndexOrThrow(COL_PERSONA_TITLE)),
                            subtitle = it.getString(it.getColumnIndexOrThrow(COL_PERSONA_SUBTITLE)),
                            systemPrompt = it.getString(it.getColumnIndexOrThrow(COL_PERSONA_SYSTEM_PROMPT)),
                            isCustom = true
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun insertOrUpdateCustomPersona(persona: Persona) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_PERSONA_ID, persona.id)
            put(COL_PERSONA_TITLE, persona.title)
            put(COL_PERSONA_SUBTITLE, persona.subtitle)
            put(COL_PERSONA_SYSTEM_PROMPT, persona.systemPrompt)
            put(COL_PERSONA_CREATED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_CUSTOM_PERSONAS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        notifyChanged()
    }

    suspend fun deleteCustomPersona(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_CUSTOM_PERSONAS, "$COL_PERSONA_ID = ?", arrayOf(id))
        notifyChanged()
    }
}
