package com.nexusai.app.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusai.app.core.network.StreamEvent
import com.nexusai.app.domain.model.AIModel
import com.nexusai.app.domain.model.ConnectionStatus
import com.nexusai.app.domain.model.Conversation
import com.nexusai.app.domain.model.Message
import com.nexusai.app.domain.model.MessageRole
import com.nexusai.app.domain.model.MessageStatus
import com.nexusai.app.domain.model.ServerConfig
import com.nexusai.app.domain.repository.ChatRepository
import com.nexusai.app.domain.model.PersonaPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatUiState(
    val currentConversationId: String? = null,
    val currentConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val availableModels: List<AIModel> = emptyList(),
    val selectedModel: String = "",
    val serverConfig: ServerConfig = ServerConfig(),
    val isModelSelectorOpen: Boolean = false,
    val isHistoryDrawerOpen: Boolean = false,
    val errorMessage: String? = null,
    val isRefreshingModels: Boolean = false,
    val serverPingMs: Long? = null,
    val activePersona: PersonaPreset = PersonaPreset.DEFAULT
)

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversationsFlow: StateFlow<List<Conversation>> = repository.getConversationsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var streamingJob: Job? = null

    init {
        // Observe server configuration
        viewModelScope.launch {
            repository.serverConfigFlow.collect { config ->
                _uiState.value = _uiState.value.copy(
                    serverConfig = config,
                    selectedModel = if (_uiState.value.selectedModel.isEmpty()) config.defaultModel else _uiState.value.selectedModel
                )
                // Trigger model discovery and ping on start
                refreshModels()
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText)
    }

    fun toggleModelSelector(open: Boolean) {
        _uiState.value = _uiState.value.copy(isModelSelectorOpen = open)
    }

    fun toggleHistoryDrawer(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHistoryDrawerOpen = open)
    }

    fun selectModel(modelId: String) {
        _uiState.value = _uiState.value.copy(selectedModel = modelId)
        viewModelScope.launch {
            repository.setDefaultModel(modelId)
            _uiState.value.currentConversationId?.let { convId ->
                _uiState.value.currentConversation?.let { conv ->
                    repository.saveConversation(conv.copy(modelId = modelId))
                }
            }
        }
    }

    fun setPersona(preset: PersonaPreset) {
        _uiState.value = _uiState.value.copy(activePersona = preset)
        viewModelScope.launch {
            _uiState.value.currentConversation?.let { conv ->
                val updated = conv.copy(systemPrompt = preset.systemPrompt)
                repository.saveConversation(updated)
                _uiState.value = _uiState.value.copy(currentConversation = updated)
            }
        }
    }

    fun pingServer() {
        viewModelScope.launch {
            val config = _uiState.value.serverConfig
            val start = System.currentTimeMillis()
            val result = repository.fetchModels(config.baseUrl)
            val latency = System.currentTimeMillis() - start
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    serverPingMs = latency,
                    connectionStatus = ConnectionStatus.CONNECTED
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    serverPingMs = null,
                    connectionStatus = ConnectionStatus.ERROR
                )
            }
        }
    }

    fun refreshModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshingModels = true,
                connectionStatus = ConnectionStatus.CONNECTING
            )
            val config = _uiState.value.serverConfig
            val start = System.currentTimeMillis()
            val result = repository.fetchModels(config.baseUrl)
            val latency = System.currentTimeMillis() - start

            if (result.isSuccess) {
                val models = result.getOrNull().orEmpty()
                val selected = if (_uiState.value.selectedModel.isNotEmpty() && models.any { it.id == _uiState.value.selectedModel }) {
                    _uiState.value.selectedModel
                } else {
                    models.firstOrNull()?.id ?: config.defaultModel
                }
                _uiState.value = _uiState.value.copy(
                    availableModels = models,
                    selectedModel = selected,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    serverPingMs = latency,
                    errorMessage = null,
                    isRefreshingModels = false
                )
            } else {
                val error = result.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    serverPingMs = null,
                    errorMessage = error?.localizedMessage ?: "Failed to connect to LM Studio",
                    isRefreshingModels = false
                )
            }
        }
    }

    fun createNewConversation() {
        val newConvId = UUID.randomUUID().toString()
        val activePrompt = _uiState.value.activePersona.systemPrompt.ifEmpty { _uiState.value.serverConfig.systemPrompt }
        val newConv = Conversation(
            id = newConvId,
            title = "New Chat",
            modelId = _uiState.value.selectedModel,
            systemPrompt = activePrompt
        )
        viewModelScope.launch {
            repository.saveConversation(newConv)
            selectConversation(newConvId)
        }
    }

    fun selectConversation(convId: String) {
        stopStreaming()
        viewModelScope.launch {
            val conv = repository.getConversationById(convId)
            val msgs = repository.getMessages(convId)
            _uiState.value = _uiState.value.copy(
                currentConversationId = convId,
                currentConversation = conv,
                messages = msgs,
                selectedModel = conv?.modelId?.ifEmpty { _uiState.value.selectedModel } ?: _uiState.value.selectedModel
            )
        }
    }

    fun sendMessage(content: String = _uiState.value.inputText) {
        if (content.isBlank() || _uiState.value.isStreaming) return

        val trimmedText = content.trim()
        _uiState.value = _uiState.value.copy(inputText = "")

        viewModelScope.launch {
            var convId = _uiState.value.currentConversationId
            if (convId == null) {
                convId = UUID.randomUUID().toString()
                val title = if (trimmedText.length > 28) trimmedText.take(28) + "..." else trimmedText
                val newConv = Conversation(
                    id = convId,
                    title = title,
                    modelId = _uiState.value.selectedModel,
                    systemPrompt = _uiState.value.serverConfig.systemPrompt
                )
                repository.saveConversation(newConv)
                _uiState.value = _uiState.value.copy(
                    currentConversationId = convId,
                    currentConversation = newConv
                )
            } else {
                // Update title if it's the first message
                val conv = _uiState.value.currentConversation
                if (conv != null && (conv.title == "New Chat" || conv.title.isEmpty())) {
                    val title = if (trimmedText.length > 28) trimmedText.take(28) + "..." else trimmedText
                    val updated = conv.copy(title = title)
                    repository.saveConversation(updated)
                    _uiState.value = _uiState.value.copy(currentConversation = updated)
                }
            }

            // Save user message
            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.USER,
                content = trimmedText,
                status = MessageStatus.COMPLETE
            )
            repository.saveMessage(userMsg)

            // Prepare assistant placeholder message
            val assistantMsgId = UUID.randomUUID().toString()
            val assistantMsg = Message(
                id = assistantMsgId,
                conversationId = convId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
            repository.saveMessage(assistantMsg)

            // Update UI list
            val updatedList = _uiState.value.messages + userMsg + assistantMsg
            _uiState.value = _uiState.value.copy(
                messages = updatedList,
                isStreaming = true
            )

            // Start streaming from LM Studio
            startStreaming(convId, assistantMsgId)
        }
    }

    private fun startStreaming(convId: String, assistantMsgId: String) {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            val config = _uiState.value.serverConfig
            val modelId = _uiState.value.selectedModel
            val systemPrompt = _uiState.value.currentConversation?.systemPrompt ?: config.systemPrompt
            val history = repository.getMessages(convId).filter { it.id != assistantMsgId }

            val responseAccumulator = StringBuilder()
            var tokensCount = 0
            val streamStartTime = System.currentTimeMillis()
            var lastUiUpdateTime = 0L

            try {
                repository.streamChat(
                    baseUrl = config.baseUrl,
                    modelId = modelId,
                    systemPrompt = systemPrompt,
                    messages = history,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens,
                    maxContextMessages = config.maxContextMessages
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            tokensCount++
                            responseAccumulator.append(event.text)
                            val now = System.currentTimeMillis()
                            // Throttle UI recompositions to ~35ms intervals to keep the main thread smooth
                            if (now - lastUiUpdateTime > 35L) {
                                lastUiUpdateTime = now
                                updateAssistantMessage(
                                    convId = convId,
                                    msgId = assistantMsgId,
                                    content = responseAccumulator.toString(),
                                    status = MessageStatus.STREAMING,
                                    persistToDb = false
                                )
                            }
                        }
                        is StreamEvent.Completed -> {
                            val durationMs = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(1L)
                            val tokPerSec = (tokensCount.toFloat() / (durationMs / 1000f))

                            updateAssistantMessage(
                                convId = convId,
                                msgId = assistantMsgId,
                                content = responseAccumulator.toString(),
                                status = MessageStatus.COMPLETE,
                                tokensCount = tokensCount,
                                durationMs = durationMs,
                                tokPerSec = tokPerSec,
                                persistToDb = true
                            )
                            _uiState.value = _uiState.value.copy(isStreaming = false)
                        }
                        is StreamEvent.Error -> {
                            val durationMs = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(1L)
                            val errorText = event.throwable.localizedMessage ?: "Stream connection error"
                            val currentText = responseAccumulator.toString()
                            val finalizedText = if (currentText.isNotEmpty()) currentText else "Error: $errorText"
                            val tokPerSec = if (tokensCount > 0) (tokensCount.toFloat() / (durationMs / 1000f)) else null

                            updateAssistantMessage(
                                convId = convId,
                                msgId = assistantMsgId,
                                content = finalizedText,
                                status = MessageStatus.ERROR,
                                errorMessage = errorText,
                                tokensCount = if (tokensCount > 0) tokensCount else null,
                                durationMs = durationMs,
                                tokPerSec = tokPerSec,
                                persistToDb = true
                            )
                            _uiState.value = _uiState.value.copy(
                                isStreaming = false,
                                connectionStatus = ConnectionStatus.ERROR
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val durationMs = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(1L)
                updateAssistantMessage(
                    convId = convId,
                    msgId = assistantMsgId,
                    content = responseAccumulator.toString().ifEmpty { "Error: ${e.localizedMessage}" },
                    status = MessageStatus.ERROR,
                    errorMessage = e.localizedMessage,
                    tokensCount = if (tokensCount > 0) tokensCount else null,
                    durationMs = durationMs,
                    persistToDb = true
                )
                _uiState.value = _uiState.value.copy(isStreaming = false)
            }
        }
    }

    private suspend fun updateAssistantMessage(
        convId: String,
        msgId: String,
        content: String,
        status: MessageStatus,
        errorMessage: String? = null,
        tokensCount: Int? = null,
        durationMs: Long? = null,
        tokPerSec: Float? = null,
        persistToDb: Boolean = false
    ) {
        val updated = Message(
            id = msgId,
            conversationId = convId,
            role = MessageRole.ASSISTANT,
            content = content,
            status = status,
            errorMessage = errorMessage,
            tokensCount = tokensCount,
            generationDurationMs = durationMs,
            tokensPerSecond = tokPerSec
        )

        // Only persist to SQLite on Completion/Error on Dispatchers.IO to prevent main-thread ANR
        if (persistToDb) {
            withContext(Dispatchers.IO) {
                repository.saveMessage(updated)
            }
        }

        // Update in-memory state
        val currentMsgs = _uiState.value.messages.map {
            if (it.id == msgId) updated else it
        }
        _uiState.value = _uiState.value.copy(messages = currentMsgs)
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    fun regenerateResponse() {
        if (_uiState.value.isStreaming) return
        val convId = _uiState.value.currentConversationId ?: return
        val currentMsgs = _uiState.value.messages
        val lastAssistant = currentMsgs.lastOrNull { it.role == MessageRole.ASSISTANT } ?: return

        viewModelScope.launch {
            // Delete previous assistant response
            repository.deleteMessage(lastAssistant.id)

            // Create new assistant placeholder
            val assistantMsgId = UUID.randomUUID().toString()
            val newAssistantMsg = Message(
                id = assistantMsgId,
                conversationId = convId,
                role = MessageRole.ASSISTANT,
                content = "",
                status = MessageStatus.STREAMING
            )
            repository.saveMessage(newAssistantMsg)

            val updatedMsgs = currentMsgs.filter { it.id != lastAssistant.id } + newAssistantMsg
            _uiState.value = _uiState.value.copy(messages = updatedMsgs, isStreaming = true)

            startStreaming(convId, assistantMsgId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages.filter { it.id != messageId }
            )
        }
    }

    fun togglePinConversation(conversation: Conversation) {
        viewModelScope.launch {
            val updated = conversation.copy(isPinned = !conversation.isPinned)
            repository.saveConversation(updated)
            if (_uiState.value.currentConversationId == conversation.id) {
                _uiState.value = _uiState.value.copy(currentConversation = updated)
            }
        }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            repository.deleteConversation(convId)
            if (_uiState.value.currentConversationId == convId) {
                _uiState.value = _uiState.value.copy(
                    currentConversationId = null,
                    currentConversation = null,
                    messages = emptyList()
                )
            }
        }
    }
}
