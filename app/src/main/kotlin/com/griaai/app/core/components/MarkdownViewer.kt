package com.griaai.app.core.components

import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import com.griaai.app.core.utils.GriaHaptic
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.theme.GriaTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Code(val language: String, val code: String) : MarkdownBlock()
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
}

object MarkdownParser {
    fun parse(content: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = content.lines()
        var inCodeBlock = false
        var codeLang = ""
        val codeBuffer = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(MarkdownBlock.Code(language = codeLang, code = codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    inCodeBlock = false
                    codeLang = ""
                } else {
                    inCodeBlock = true
                    codeLang = trimmed.removePrefix("```").trim()
                }
            } else if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
            } else if (trimmed.startsWith("### ")) {
                blocks.add(MarkdownBlock.Header(level = 3, text = trimmed.removePrefix("### ").trim()))
            } else if (trimmed.startsWith("## ")) {
                blocks.add(MarkdownBlock.Header(level = 2, text = trimmed.removePrefix("## ").trim()))
            } else if (trimmed.startsWith("# ")) {
                blocks.add(MarkdownBlock.Header(level = 1, text = trimmed.removePrefix("# ").trim()))
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                blocks.add(MarkdownBlock.BulletItem(text = trimmed.substring(2).trim()))
            } else if (trimmed.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(text = line))
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            blocks.add(MarkdownBlock.Code(language = codeLang, code = codeBuffer.toString().trimEnd()))
        }

        return blocks
    }

    fun parseInlineFormatting(
        text: String,
        primaryColor: Color,
        codeBg: Color,
        codeTextColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                if (text.startsWith("**", i) && text.indexOf("**", i + 2) != -1) {
                    val end = text.indexOf("**", i + 2)
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = primaryColor))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else if (text.startsWith("`", i) && text.indexOf("`", i + 1) != -1) {
                    val end = text.indexOf("`", i + 1)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            color = codeTextColor,
                            fontSize = 13.sp
                        )
                    )
                    append(" ${text.substring(i + 1, end)} ")
                    pop()
                    i = end + 1
                } else if (text.startsWith("*", i) && text.indexOf("*", i + 1) != -1) {
                    val end = text.indexOf("*", i + 1)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = primaryColor))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Composable
fun ThinkingBlock(
    thought: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGriaColors.current
    var isExpanded by remember { mutableStateOf(isStreaming) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceSubtle.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isStreaming) "💭 Thinking..." else "💭 Thought Process",
                    style = GriaTypography.labelMedium.copy(
                        color = if (isStreaming) colors.accent else colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isStreaming) {
                    val transition = rememberInfiniteTransition(label = "thinking_pulse")
                    val alpha by transition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = thought.trim(),
                        style = GriaTypography.bodySmall.copy(
                            color = colors.textSecondary,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownViewer(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalGriaColors.current.textPrimary,
    isStreaming: Boolean = false
) {
    val colors = LocalGriaColors.current
    val hasThink = remember(content) { content.contains("<think>") }
    val (thinkContent, mainContent, isThinkingComplete) = remember(content) {
        if (hasThink) {
            val think = content.substringAfter("<think>").substringBefore("</think>")
            val complete = content.contains("</think>")
            val rest = if (complete) content.substringAfter("</think>").trimStart() else ""
            Triple(think, rest, complete)
        } else {
            Triple(null, content, true)
        }
    }

    val blocks = remember(mainContent) { MarkdownParser.parse(mainContent) }

    val inlineContent = remember {
        mapOf(
            "STREAMING_CURSOR" to InlineTextContent(
                Placeholder(
                    width = 8.sp,
                    height = 20.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                StreamingCursor()
            }
        )
    }

    Column(modifier = modifier) {
        if (thinkContent != null) {
            ThinkingBlock(
                thought = thinkContent,
                isStreaming = isStreaming && !isThinkingComplete
            )
        }

        blocks.forEachIndexed { index, block ->
            val isLastBlock = index == blocks.size - 1
            when (block) {
                is MarkdownBlock.Header -> {
                    val style = when (block.level) {
                        1 -> GriaTypography.headlineLarge
                        2 -> GriaTypography.headlineMedium
                        else -> GriaTypography.headlineSmall
                    }
                    if (isStreaming && isLastBlock) {
                        val headerAnnotated = buildAnnotatedString {
                            append(block.text)
                            append(" ")
                            appendInlineContent("STREAMING_CURSOR", "[cursor]")
                        }
                        Text(
                            text = headerAnnotated,
                            inlineContent = inlineContent,
                            style = style.copy(color = colors.textPrimary),
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = block.text,
                            style = style.copy(color = colors.textPrimary),
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                    }
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 9.dp, end = 10.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                        )
                        val formatted = MarkdownParser.parseInlineFormatting(
                            text = block.text,
                            primaryColor = textColor,
                            codeBg = colors.surfaceSubtle,
                            codeTextColor = colors.textPrimary
                        )
                        if (isStreaming && isLastBlock) {
                            val bulletAnnotated = buildAnnotatedString {
                                append(formatted)
                                append(" ")
                                appendInlineContent("STREAMING_CURSOR", "[cursor]")
                            }
                            Text(
                                text = bulletAnnotated,
                                inlineContent = inlineContent,
                                style = GriaTypography.bodyLarge.copy(color = textColor)
                            )
                        } else {
                            Text(
                                text = formatted,
                                style = GriaTypography.bodyLarge.copy(color = textColor)
                            )
                        }
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    val formatted = MarkdownParser.parseInlineFormatting(
                        text = block.text,
                        primaryColor = textColor,
                        codeBg = colors.surfaceSubtle,
                        codeTextColor = colors.textPrimary
                    )
                    if (isStreaming && isLastBlock) {
                        val paragraphAnnotated = buildAnnotatedString {
                            append(formatted)
                            append(" ")
                            appendInlineContent("STREAMING_CURSOR", "[cursor]")
                        }
                        Text(
                            text = paragraphAnnotated,
                            inlineContent = inlineContent,
                            style = GriaTypography.bodyLarge.copy(color = textColor),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = formatted,
                            style = GriaTypography.bodyLarge.copy(color = textColor),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                is MarkdownBlock.Code -> {
                    CodeBlock(
                        language = block.language.ifEmpty { "code" },
                        code = block.code,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (isStreaming && isLastBlock) {
                        StreamingCursor(modifier = Modifier.padding(top = 2.dp, start = 2.dp))
                    }
                }
            }
        }

        // If blocks is empty but streaming is requested, show fallback cursor
        if (blocks.isEmpty() && isStreaming) {
            StreamingCursor(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

/**
 * ChatGPT / Claude style smooth blinking streaming caret.
 */
@Composable
fun StreamingCursor(modifier: Modifier = Modifier) {
    val colors = LocalGriaColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .size(width = 2.5.dp, height = 18.dp)
            .clip(RoundedCornerShape(1.2.dp))
            .background(colors.accent.copy(alpha = alpha))
    )
}

@Composable
fun CodeBlock(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalGriaColors.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val canPreview = remember(language, code) {
        val l = language.lowercase().trim()
        l in listOf("html", "htm", "svg") ||
                code.trimStart().startsWith("<!DOCTYPE", ignoreCase = true) ||
                code.trimStart().startsWith("<html", ignoreCase = true) ||
                code.trimStart().startsWith("<svg", ignoreCase = true)
    }
    var showPreview by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.codeBackground,
        modifier = modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.codeBorder)
    ) {
        Column {
            // Header bar: clean, minimalist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.codeHeader)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.lowercase(),
                    style = GriaTypography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Interactive Live Canvas tab toggle
                if (canPreview) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (showPreview) colors.surfaceSubtle else colors.codeBackground)
                            .clickable {
                                GriaHaptic.click(view)
                                showPreview = !showPreview
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showPreview) Icons.Default.Code else Icons.Default.Visibility,
                            contentDescription = if (showPreview) "View Code" else "Live Canvas",
                            tint = if (showPreview) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (showPreview) "Code" else "Canvas",
                            style = GriaTypography.labelSmall.copy(
                                color = if (showPreview) colors.accent else colors.textSecondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            GriaHaptic.confirm(view)
                            clipboardManager.setText(AnnotatedString(code))
                            copied = true
                            scope.launch {
                                delay(2000)
                                copied = false
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) colors.statusSuccess else colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = GriaTypography.labelSmall.copy(
                            color = if (copied) colors.statusSuccess else colors.textSecondary
                        )
                    )
                }
            }

            if (canPreview && showPreview) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webChromeClient = WebChromeClient()
                        }
                    },
                    update = { webView ->
                        val payload = if (language.lowercase() == "svg") {
                            "<!DOCTYPE html><html><body style='margin:0;padding:16px;display:flex;align-items:center;justify-content:center;background:#18181b;'>$code</body></html>"
                        } else {
                            code
                        }
                        webView.loadDataWithBaseURL(null, payload, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                // Code Content with Quiet Luxury Syntax Highlighting
                val highlightedCode = remember(code, language) {
                    CodeHighlighter.highlight(code, language, colors.codeText)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = highlightedCode,
                        color = colors.codeText,
                        style = GriaTypography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * High-performance, Quiet Luxury syntax highlighter for common programming languages.
 * Supports: Kotlin, Python, Bash, Rust, JavaScript/TypeScript, SQL, JSON.
 */
object CodeHighlighter {
    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
        "in", "interface", "is", "null", "object", "package", "return", "super", "this",
        "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
        "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally",
        "get", "import", "init", "param", "property", "receiver", "set", "setparam",
        "where", "actual", "abstract", "annotation", "companion", "const", "crossinline",
        "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
        "internal", "lateinit", "noinline", "open", "operator", "out", "override",
        "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg"
    )

    private val PYTHON_KEYWORDS = setOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
        "raise", "return", "True", "try", "while", "with", "yield"
    )

    private val RUST_KEYWORDS = setOf(
        "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false",
        "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut",
        "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait",
        "true", "type", "unsafe", "use", "where", "while", "async", "await", "dyn"
    )

    private val JS_KEYWORDS = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function", "if",
        "import", "in", "instanceof", "new", "return", "super", "switch", "this",
        "throw", "try", "typeof", "var", "void", "while", "with", "yield", "let",
        "static", "enum", "await", "async", "null", "undefined", "true", "false"
    )

    private val BASH_KEYWORDS = setOf(
        "if", "then", "else", "elif", "fi", "case", "esac", "for", "select", "while",
        "until", "do", "done", "in", "function", "time", "export", "source", "alias",
        "local", "return", "exit", "echo", "cd", "pwd", "grep", "cat", "chmod", "curl"
    )

    private val SQL_KEYWORDS = setOf(
        "select", "from", "where", "insert", "into", "values", "update", "set",
        "delete", "create", "table", "alter", "drop", "index", "join", "inner",
        "left", "right", "full", "outer", "on", "group", "by", "order", "asc",
        "desc", "having", "limit", "offset", "union", "distinct", "as", "and", "or", "not"
    )

    // Pre-allocated static SpanStyles to eliminate per-token heap allocations
    private val KEYWORD_STYLE = SpanStyle(color = Color(0xFF6B4BA3), fontWeight = FontWeight.SemiBold)
    private val STRING_STYLE = SpanStyle(color = Color(0xFF9E4B29))
    private val COMMENT_STYLE = SpanStyle(color = Color(0xFF768278), fontStyle = FontStyle.Italic)
    private val NUMBER_STYLE = SpanStyle(color = Color(0xFF2C687A))
    private val TYPE_STYLE = SpanStyle(color = Color(0xFF38557A), fontWeight = FontWeight.SemiBold)

    fun highlight(code: String, language: String, defaultColor: Color): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        if (code.length > 50000) return AnnotatedString(code)

        return try {
            val lang = language.lowercase().trim()
            val keywords = when (lang) {
                "kotlin", "kt" -> KOTLIN_KEYWORDS
                "python", "py" -> PYTHON_KEYWORDS
                "rust", "rs" -> RUST_KEYWORDS
                "javascript", "js", "typescript", "ts" -> JS_KEYWORDS
                "bash", "sh", "shell", "zsh" -> BASH_KEYWORDS
                "sql" -> SQL_KEYWORDS
                else -> KOTLIN_KEYWORDS + PYTHON_KEYWORDS + JS_KEYWORDS
            }

            val builder = AnnotatedString.Builder()
            val lines = code.lines()

            lines.forEachIndexed { lineIdx, line ->
                var i = 0
                val len = line.length

                while (i < len) {
                    // Check for single line comments: // or #
                    if ((line.startsWith("//", i) && lang != "python") ||
                        (line.startsWith("#", i) && (lang == "python" || lang == "bash" || lang == "sh" || lang == "shell" || lang == "yaml" || lang == "yml"))
                    ) {
                        val commentText = line.substring(i)
                        builder.pushStyle(COMMENT_STYLE)
                        builder.append(commentText)
                        builder.pop()
                        i = len
                        break
                    }

                    // Check for strings: "..." or '...'
                    val c = line[i]
                    if (c == '"' || c == '\'') {
                        val quote = c
                        var j = i + 1
                        var escaped = false
                        while (j < len) {
                            if (line[j] == '\\' && !escaped) {
                                escaped = true
                            } else if (line[j] == quote && !escaped) {
                                j++
                                break
                            } else {
                                escaped = false
                            }
                            j++
                        }
                        val strText = line.substring(i, j.coerceAtMost(len))
                        builder.pushStyle(STRING_STYLE)
                        builder.append(strText)
                        builder.pop()
                        i = j
                        continue
                    }

                    // Check for Numbers
                    if (c.isDigit() && (i == 0 || !line[i - 1].isLetterOrDigit())) {
                        var j = i
                        while (j < len && (line[j].isLetterOrDigit() || line[j] == '.')) {
                            j++
                        }
                        builder.pushStyle(NUMBER_STYLE)
                        builder.append(line.substring(i, j))
                        builder.pop()
                        i = j
                        continue
                    }

                    // Check for Words (Keywords, Types, Identifiers)
                    if (c.isLetter() || c == '_' || c == '@') {
                        var j = i
                        while (j < len && (line[j].isLetterOrDigit() || line[j] == '_')) {
                            j++
                        }
                        val word = line.substring(i, j)
                        when {
                            word.startsWith("@") -> {
                                builder.pushStyle(TYPE_STYLE)
                                builder.append(word)
                                builder.pop()
                            }
                            word.lowercase() in keywords -> {
                                builder.pushStyle(KEYWORD_STYLE)
                                builder.append(word)
                                builder.pop()
                            }
                            word.firstOrNull()?.isUpperCase() == true && word.length > 1 -> {
                                builder.pushStyle(TYPE_STYLE)
                                builder.append(word)
                                builder.pop()
                            }
                            else -> {
                                // Default color does not need any SpanStyle allocation - Text composable handles it
                                builder.append(word)
                            }
                        }
                        i = j
                        continue
                    }

                    // Batch consecutive whitespace / punctuation without SpanStyle allocations
                    val startPunct = i
                    while (i < len) {
                        val ch = line[i]
                        if (ch.isLetterOrDigit() || ch == '_' || ch == '@' || ch == '"' || ch == '\'') break
                        if (line.startsWith("//", i) || line.startsWith("#", i)) break
                        i++
                    }
                    if (i > startPunct) {
                        builder.append(line.substring(startPunct, i))
                    } else {
                        builder.append(c.toString())
                        i++
                    }
                }

                if (lineIdx < lines.size - 1) {
                    builder.append("\n")
                }
            }

            builder.toAnnotatedString()
        } catch (t: Throwable) {
            AnnotatedString(code)
        }
    }
}
