package com.griaai.app.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griaai.app.core.network.StreamEvent
import com.griaai.app.domain.model.AIModel
import com.griaai.app.domain.model.ConnectionStatus
import com.griaai.app.domain.model.Conversation
import com.griaai.app.domain.model.Message
import com.griaai.app.domain.model.MessageRole
import com.griaai.app.domain.model.MessageStatus
import com.griaai.app.domain.model.ServerConfig
import com.griaai.app.domain.repository.ChatRepository
import com.griaai.app.domain.model.Persona
import com.griaai.app.domain.model.PersonaPreset
import java.io.File
import kotlinx.coroutines.CancellationException
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
    val activePersona: Persona = Persona.DEFAULT_PERSONAS.first(),
    val availablePersonas: List<Persona> = Persona.DEFAULT_PERSONAS,
    val selectedImageUri: String? = null
) {
    val isOnDeviceSelected: Boolean
        get() = selectedModel.startsWith("ondevice:") || selectedModel == "ondevice:gemma"
}

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversationsFlow: StateFlow<List<Conversation>> = repository.getConversationsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var streamingJob: Job? = null

    init {
        // Load custom personas from database
        loadPersonas()

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

    fun enhancePrompt() {
        val current = _uiState.value.inputText.trim()
        if (current.isBlank()) return
        val enhanced = com.griaai.app.core.utils.PromptEnhancer.enhance(current)
        _uiState.value = _uiState.value.copy(inputText = enhanced)
    }

    fun toggleModelSelector(open: Boolean) {
        _uiState.value = _uiState.value.copy(isModelSelectorOpen = open)
    }

    fun toggleHistoryDrawer(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHistoryDrawerOpen = open)
    }

    fun selectModel(modelId: String) {
        stopStreaming()
        val isOnDevice = modelId.startsWith("ondevice:") || modelId == "ondevice:gemma"
        val newStatus = if (isOnDevice) ConnectionStatus.CONNECTED else _uiState.value.connectionStatus
        _uiState.value = _uiState.value.copy(
            selectedModel = modelId,
            connectionStatus = newStatus,
            isModelSelectorOpen = false
        )

        if (modelId.startsWith("ondevice:") && modelId != "ondevice:gemma") {
            val path = modelId.removePrefix("ondevice:")
            viewModelScope.launch(Dispatchers.IO) {
                val f = File(path)
                if (f.exists() && f.canRead()) {
                    repository.onDeviceEngine.loadModel(f)
                }
            }
        }

        viewModelScope.launch {
            runCatching {
                repository.setDefaultModel(modelId)
                _uiState.value.currentConversationId?.let { convId ->
                    _uiState.value.currentConversation?.let { conv ->
                        val updated = conv.copy(modelId = modelId)
                        repository.saveConversation(updated)
                        _uiState.value = _uiState.value.copy(currentConversation = updated)
                    }
                }
            }
        }
    }

    fun setPersona(persona: Persona) {
        _uiState.value = _uiState.value.copy(activePersona = persona)
        viewModelScope.launch {
            _uiState.value.currentConversation?.let { conv ->
                val updated = conv.copy(systemPrompt = persona.systemPrompt)
                repository.saveConversation(updated)
                _uiState.value = _uiState.value.copy(currentConversation = updated)
            }
        }
    }

    fun setPersona(preset: PersonaPreset) {
        setPersona(preset.toPersona())
    }

    fun loadPersonas() {
        viewModelScope.launch {
            val custom = repository.getCustomPersonas()
            val all = Persona.DEFAULT_PERSONAS + custom
            _uiState.value = _uiState.value.copy(availablePersonas = all)
        }
    }

    fun createCustomPersona(title: String, subtitle: String, systemPrompt: String) {
        if (title.isBlank() || systemPrompt.isBlank()) return
        val newPersona = Persona(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            subtitle = subtitle.trim().ifBlank { "Custom assistant" },
            systemPrompt = systemPrompt.trim(),
            isCustom = true
        )
        viewModelScope.launch {
            repository.saveCustomPersona(newPersona)
            loadPersonas()
            setPersona(newPersona)
        }
    }

    fun deleteCustomPersona(personaId: String) {
        viewModelScope.launch {
            repository.deleteCustomPersona(personaId)
            loadPersonas()
            if (_uiState.value.activePersona.id == personaId) {
                setPersona(Persona.DEFAULT_PERSONAS.first())
            }
        }
    }

    fun onImageSelected(uriString: String?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uriString)
    }

    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(selectedImageUri = null)
    }

    fun pingServer() {
        viewModelScope.launch {
            val config = _uiState.value.serverConfig
            val start = System.currentTimeMillis()
            val result = repository.testConnection(config.baseUrl)
            val latency = System.currentTimeMillis() - start
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    serverPingMs = latency,
                    connectionStatus = ConnectionStatus.CONNECTED
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    serverPingMs = null,
                    connectionStatus = if (_uiState.value.isOnDeviceSelected) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
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
            val remoteResult = repository.testConnection(config.baseUrl)
            val latency = System.currentTimeMillis() - start

            val modelsResult = repository.fetchModels(config.baseUrl)
            val models = modelsResult.getOrNull().orEmpty()

            val selected = if (_uiState.value.selectedModel.isNotEmpty() && models.any { it.id == _uiState.value.selectedModel }) {
                _uiState.value.selectedModel
            } else {
                models.firstOrNull()?.id ?: config.defaultModel
            }

            if (remoteResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    availableModels = models,
                    selectedModel = selected,
                    connectionStatus = ConnectionStatus.CONNECTED,
                    serverPingMs = latency,
                    errorMessage = null,
                    isRefreshingModels = false
                )
            } else {
                val isOnDevice = selected.startsWith("ondevice:") || selected == "ondevice:gemma"
                _uiState.value = _uiState.value.copy(
                    availableModels = models,
                    selectedModel = selected,
                    connectionStatus = if (isOnDevice) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED,
                    serverPingMs = null,
                    errorMessage = null,
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
        val attachedImageUri = _uiState.value.selectedImageUri
        if ((content.isBlank() && attachedImageUri == null) || _uiState.value.isStreaming) return

        val trimmedText = content.trim().ifBlank { if (attachedImageUri != null) "Describe this image" else "" }
        _uiState.value = _uiState.value.copy(inputText = "", selectedImageUri = null)

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
                status = MessageStatus.COMPLETE,
                imageUri = attachedImageUri
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
            } catch (ce: CancellationException) {
                // User intentionally cancelled or stopped generation
                Log.d("ChatViewModel", "Streaming generation cancelled cleanly")
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
        repository.onDeviceEngine.stop()

        val currentMsgs = _uiState.value.messages
        val lastMsg = currentMsgs.lastOrNull()
        if (lastMsg != null && lastMsg.status == MessageStatus.STREAMING) {
            val finalized = lastMsg.copy(
                status = MessageStatus.COMPLETE,
                content = lastMsg.content.ifEmpty { "Response stopped." }
            )
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repository.saveMessage(finalized) }
            }
            _uiState.value = _uiState.value.copy(
                isStreaming = false,
                messages = currentMsgs.dropLast(1) + finalized
            )
        } else {
            _uiState.value = _uiState.value.copy(isStreaming = false)
        }
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

    fun forkConversationAtMessage(messageId: String, newContent: String) {
        val currentMessages = _uiState.value.messages
        val index = currentMessages.indexOfFirst { it.id == messageId }
        if (index == -1) return

        val truncated = currentMessages.take(index)
        val messagesToDelete = currentMessages.drop(index)
        _uiState.value = _uiState.value.copy(messages = truncated)

        viewModelScope.launch {
            messagesToDelete.forEach { repository.deleteMessage(it.id) }
            sendMessage(newContent)
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
