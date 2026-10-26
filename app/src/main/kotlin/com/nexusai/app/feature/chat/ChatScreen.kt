package com.nexusai.app.feature.chat

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusai.app.core.components.ConnectionBadge
import com.nexusai.app.core.components.ConnectionErrorCard
import com.nexusai.app.core.components.ConversationHistoryDrawer
import com.nexusai.app.core.components.MessageBubble
import com.nexusai.app.core.components.MessageComposer
import com.nexusai.app.core.components.ModelSelectorBottomSheet
import com.nexusai.app.core.components.WelcomeHero
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography
import com.nexusai.app.core.utils.NexusHaptic
import com.nexusai.app.domain.model.ConnectionStatus
import com.nexusai.app.domain.model.MessageStatus
import com.nexusai.app.domain.model.PersonaPreset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToConnection: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversationsFlow.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val view = LocalView.current

    // Smooth animation when a new message is added
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // High-performance direct pinning to bottom during token streaming without animation fight
    LaunchedEffect(uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.isStreaming && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    // Tactical completion pulse when streaming finishes (Feature 4)
    LaunchedEffect(uiState.isStreaming) {
        if (!uiState.isStreaming && uiState.messages.lastOrNull()?.status == MessageStatus.COMPLETE) {
            NexusHaptic.completionPulse(context)
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible < uiState.messages.size - 1
        }
    }

    // Clean display model name
    val displayModelName = remember(uiState.selectedModel) {
        val raw = uiState.selectedModel
        when {
            raw.isEmpty() -> "Nexus"
            raw.contains("qwen", ignoreCase = true) -> "Nexus · Qwen3"
            raw.contains("llama", ignoreCase = true) -> "Nexus · Llama3"
            raw.contains("deepseek", ignoreCase = true) -> "Nexus · DeepSeek"
            raw.contains("mistral", ignoreCase = true) -> "Nexus · Mistral"
            else -> "Nexus · " + raw.take(12)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationHistoryDrawer(
                conversations = conversations,
                currentConversationId = uiState.currentConversationId,
                onSelectConversation = { convId -> viewModel.selectConversation(convId) },
                onNewConversation = { viewModel.createNewConversation() },
                onPinToggle = { conv -> viewModel.togglePinConversation(conv) },
                onDeleteConversation = { convId -> viewModel.deleteConversation(convId) },
                onOpenSettings = onNavigateToSettings,
                onOpenAbout = onNavigateToAbout,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            topBar = {
                // Minimal Top Bar: Small sidebar on left, Centered Model dropdown + Latency badge, Minimal new chat on right
                Surface(
                    color = colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small sidebar icon on the left
                            IconButton(
                                onClick = {
                                    NexusHaptic.click(view)
                                    scope.launch { drawerState.open() }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Sidebar",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Centered model name with discreet dropdown chevron & server ping
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        NexusHaptic.click(view)
                                        viewModel.toggleModelSelector(true)
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayModelName,
                                    style = NexusTypography.titleMedium.copy(color = colors.textPrimary),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select model",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                ServerLatencyBadge(
                                    status = uiState.connectionStatus,
                                    pingMs = uiState.serverPingMs,
                                    isOnDevice = uiState.isOnDeviceSelected,
                                    onClick = {
                                        NexusHaptic.click(view)
                                        viewModel.pingServer()
                                    }
                                )
                            }

                            // Minimal search icon
                            IconButton(
                                onClick = {
                                    NexusHaptic.click(view)
                                    onNavigateToSearch()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Minimal new-chat icon on the right
                            IconButton(
                                onClick = {
                                    NexusHaptic.tick(view)
                                    viewModel.createNewConversation()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New chat",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Thin separator only where necessary
                        HorizontalDivider(
                            color = colors.borderSubtle,
                            thickness = 0.5.dp
                        )

                        // Feature 3: Persona / Mode Presets bar
                        PersonaSelectorBar(
                            activePersona = uiState.activePersona,
                            onSelectPersona = { preset ->
                                viewModel.setPersona(preset)
                            }
                        )
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .background(colors.background)
                ) {
                    MessageComposer(
                        text = uiState.inputText,
                        onTextChange = { viewModel.onInputTextChanged(it) },
                        onSend = { viewModel.sendMessage() },
                        onStop = { viewModel.stopStreaming() },
                        isStreaming = uiState.isStreaming,
                        placeholder = "Message Nexus..."
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.background)
            ) {
                if (uiState.messages.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.connectionStatus == ConnectionStatus.ERROR) {
                            ConnectionErrorCard(
                                errorMessage = uiState.errorMessage.orEmpty(),
                                onRetry = { viewModel.refreshModels() },
                                onEditSettings = onNavigateToConnection
                            )
                        }
                        WelcomeHero(
                            onSelectPrompt = { prompt ->
                                viewModel.sendMessage(prompt)
                            },
                            modelName = displayModelName
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isStreaming = uiState.isStreaming,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                },
                                onShare = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, message.content)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Message")
                                    context.startActivity(shareIntent)
                                },
                                onEdit = {
                                    viewModel.onInputTextChanged(message.content)
                                },
                                onRegenerate = {
                                    viewModel.regenerateResponse()
                                },
                                onDelete = {
                                    viewModel.deleteMessage(message.id)
                                }
                            )
                        }
                    }
                }

                // Scroll to Bottom Button
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(uiState.messages.size - 1)
                            }
                        },
                        containerColor = colors.surfaceElevated,
                        contentColor = colors.textPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Model Selector Bottom Sheet
    if (uiState.isModelSelectorOpen) {
        ModelSelectorBottomSheet(
            models = uiState.availableModels,
            selectedModelId = uiState.selectedModel,
            onSelectModel = { modelId -> viewModel.selectModel(modelId) },
            onRefresh = { viewModel.refreshModels() },
            onDismiss = { viewModel.toggleModelSelector(false) },
            isLoading = uiState.isRefreshingModels
        )
    }
}

/**
 * Feature 2: Live Server Latency & Health Indicator.
 * Displays a minimal green/amber/red status dot and ping time in milliseconds.
 */
@Composable
fun ServerLatencyBadge(
    status: ConnectionStatus,
    pingMs: Long?,
    isOnDevice: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    val (dotColor, text) = when {
        isOnDevice -> colors.statusSuccess to "local"
        status == ConnectionStatus.CONNECTED -> {
            val ping = pingMs ?: 0L
            val color = if (ping < 150) colors.statusSuccess else Color(0xFFD97706)
            color to if (pingMs != null) "${pingMs}ms" else "online"
        }
        status == ConnectionStatus.CONNECTING -> colors.textTertiary to "..."
        else -> colors.statusError to "offline"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = NexusTypography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = colors.textTertiary
            )
        )
    }
}

/**
 * Feature 3: Persona / Mode Presets Bar.
 * Quick-switch between Personal AI, Senior Coder, Security Analyst, and Synthesizer.
 */
@Composable
fun PersonaSelectorBar(
    activePersona: PersonaPreset,
    onSelectPersona: (PersonaPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonaPreset.entries.forEach { preset ->
            val isSelected = preset == activePersona
            val bg = if (isSelected) colors.surfaceSubtle else colors.background
            val borderCol = if (isSelected) colors.accent else colors.borderSubtle
            val textCol = if (isSelected) colors.accent else colors.textSecondary
            val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bg,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        NexusHaptic.click(view)
                        onSelectPersona(preset)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.title,
                        style = NexusTypography.labelSmall.copy(
                            color = textCol,
                            fontWeight = fontWeight
                        )
                    )
                }
            }
        }
    }
}

