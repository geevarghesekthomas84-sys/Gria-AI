package com.nexusai.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography
import com.nexusai.app.domain.model.Conversation
import java.util.Calendar

@Composable
fun ConversationHistoryDrawer(
    conversations: List<Conversation>,
    currentConversationId: String?,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onPinToggle: (Conversation) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHardwareMonitor: (() -> Unit)? = null,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val pinnedChats = remember(filtered) { filtered.filter { it.isPinned } }
    val unpinnedChats = remember(filtered) { filtered.filter { !it.isPinned } }

    val groupedChats = remember(unpinnedChats) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        val startOfYesterday = startOfToday - 86400000L
        val startOfPastWeek = startOfToday - (7 * 86400000L)

        val today = mutableListOf<Conversation>()
        val yesterday = mutableListOf<Conversation>()
        val pastWeek = mutableListOf<Conversation>()
        val older = mutableListOf<Conversation>()

        unpinnedChats.forEach { conv ->
            when {
                conv.updatedAt >= startOfToday -> today.add(conv)
                conv.updatedAt >= startOfYesterday -> yesterday.add(conv)
                conv.updatedAt >= startOfPastWeek -> pastWeek.add(conv)
                else -> older.add(conv)
            }
        }

        listOf(
            "Today" to today,
            "Yesterday" to yesterday,
            "Previous 7 Days" to pastWeek,
            "Older" to older
        ).filter { it.second.isNotEmpty() }
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Header: Brand & Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NexusLogo(size = 28.dp, showContainer = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gria-AI",
                        style = NexusTypography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // New Chat action button (quiet luxury pill)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNewConversation()
                        onCloseDrawer()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "New chat",
                        style = NexusTypography.labelMedium.copy(color = colors.textPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Minimal search field
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search conversations...",
                                style = NexusTypography.bodySmall.copy(color = colors.textTertiary)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = NexusTypography.bodySmall.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conversation Lists
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Pinned section
                if (pinnedChats.isNotEmpty()) {
                    item {
                        Text(
                            text = "PINNED",
                            style = NexusTypography.labelSmall.copy(
                                color = colors.textTertiary,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(pinnedChats, key = { "pinned_${it.id}" }) { conv ->
                        ConversationDrawerRow(
                            conversation = conv,
                            isSelected = conv.id == currentConversationId,
                            onSelect = {
                                onSelectConversation(conv.id)
                                onCloseDrawer()
                            },
                            onPinToggle = { onPinToggle(conv) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                }

                // Chronological sections
                groupedChats.forEach { (sectionTitle, convs) ->
                    item {
                        Text(
                            text = sectionTitle.uppercase(),
                            style = NexusTypography.labelSmall.copy(
                                color = colors.textTertiary,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(convs, key = { it.id }) { conv ->
                        ConversationDrawerRow(
                            conversation = conv,
                            isSelected = conv.id == currentConversationId,
                            onSelect = {
                                onSelectConversation(conv.id)
                                onCloseDrawer()
                            },
                            onPinToggle = { onPinToggle(conv) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Settings & About at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onOpenSettings()
                            onCloseDrawer()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        style = NexusTypography.labelMedium.copy(color = colors.textPrimary)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onOpenHardwareMonitor != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onOpenHardwareMonitor()
                                onCloseDrawer()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Hardware",
                            style = NexusTypography.labelMedium.copy(color = colors.accent)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onOpenAbout()
                            onCloseDrawer()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "About",
                        style = NexusTypography.labelMedium.copy(color = colors.textSecondary)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationDrawerRow(
    conversation: Conversation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalNexusColors.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.surfaceElevated else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtle selected indicator dot
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = conversation.title.ifEmpty { "New conversation" },
                style = NexusTypography.bodySmall.copy(
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onPinToggle,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (conversation.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if (conversation.isPinned) colors.accent else colors.textTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = colors.textTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
