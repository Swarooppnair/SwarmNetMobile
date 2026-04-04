package com.bitchat.android.features.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val BG = Color(0xFF0B0B0B)
private val CardBG = Color(0xFF1A1A1A)
private val SurfaceBG = Color(0xFF222222)
private val Accent = Color(0xFFFF3B30)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)
private val IncomingBubble = Color(0xFF1E1E1E)
private val OutgoingBubble = Color(0xFF2C2C2E)

@Composable
fun SurvivalAssistantScreen(
    survivalViewModel: SurvivalViewModel = viewModel()
) {
    val messages by survivalViewModel.messages.collectAsStateWithLifecycle()
    val isLoading by survivalViewModel.isLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .systemBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Survival Assistant",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Offline · Local knowledge base",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.5.dp)

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.hashCode() + it.text.length }) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(200)) + slideInVertically(
                        animationSpec = tween(250, easing = EaseOutCubic),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    if (message.isUser) {
                        UserBubble(text = message.text)
                    } else if (message.isLoading) {
                        LoadingBubble()
                    } else {
                        AssistantBubble(text = message.text)
                    }
                }
            }
        }

        // Input bar
        SurvivalInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            isLoading = isLoading,
            onSend = {
                val text = inputText.trim()
                if (text.isNotEmpty() && !isLoading) {
                    survivalViewModel.sendMessage(text)
                    inputText = ""
                }
            }
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = IncomingBubble,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = parseMarkdown(text),
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = OutgoingBubble,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = IncomingBubble
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot_$i")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = i * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TextSecondary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
private fun SurvivalInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBG)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Ask a survival question...", color = TextSecondary, fontSize = 14.sp)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceBG,
                unfocusedContainerColor = SurfaceBG,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Accent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.width(10.dp))

        val canSend = value.isNotBlank() && !isLoading
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (canSend) Accent else SurfaceBG)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (canSend) Color.White else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Minimal markdown parser: handles **bold**, *italic*, and plain text */
private fun parseMarkdown(text: String) = buildAnnotatedString {
    val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    val italicRegex = Regex("\\*(.+?)\\*")

    var cursor = 0
    val combined = Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*")
    combined.findAll(text).forEach { match ->
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        when {
            match.groupValues[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                append(match.groupValues[1])
            }
            match.groupValues[2].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFCCCCCC))) {
                append(match.groupValues[2])
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}
