/*
聊天功能暂不实现
 */

package world.accera.dawn

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import world.accera.dawn.data.chat_data.MessageContent
import world.accera.dawn.data.chat_data.Role

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val userAvatar = Icons.Filled.Person
    val aiAvatar = Icons.Filled.Face

    Scaffold(
        topBar = {
            ChatTopBar(
                title = "晓向出行",
                onNavigationIconClick = { viewModel.onPersonalCenterClick() },
                navigationIcon = Icons.Filled.AccountCircle,
                navigationIconContentDescription = "个人中心"
            )
        },
        bottomBar = {
            InputBarArea(
                inputText = viewModel.inputText,
                onInputTextChange = { viewModel.updateInputText(it) },
                onSendMessage = { viewModel.sendTextMessage() },
                isVoiceMode = viewModel.isVoiceInputMode,
                onToggleInputMode = { viewModel.toggleInputMode() },
                isFunctionPanelExpanded = viewModel.isFunctionPanelExpanded,
                onToggleFunctionPanel = { viewModel.toggleFunctionPanel() },
                onSendVoiceMessage = { voiceData ->
                    viewModel.sendVoiceMessage(voiceData = voiceData)
                }
            )
        }
    ) { paddingValues ->
        MessageListArea(
            messages = viewModel.messages,
            userAvatar = userAvatar,
            aiAvatar = aiAvatar,
            modifier = Modifier.padding(paddingValues)
        )
    }//到这里
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    onNavigationIconClick: () -> Unit,
    navigationIcon: ImageVector,
    navigationIconContentDescription: String
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer // Or onPrimary
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigationIconClick,
                modifier = Modifier.semantics { contentDescription = navigationIconContentDescription }
            ) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = null, // Description handled by IconButton's semantics
                    tint = MaterialTheme.colorScheme.onPrimaryContainer // Or onPrimary
                )
            }
        },
        actions = {
            // Empty as per requirement (title bar right side is empty)
            // Spacer(modifier = Modifier.width(48.dp)) // To balance the navigation icon space if title isn't perfectly centered.
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // Or primary
        )
    )
}

@Composable
fun MessageListArea(
    messages: List<MessageContent>,
    userAvatar: ImageVector, // Can be Painter or ImageVector
    aiAvatar: ImageVector,   // Can be Painter or ImageVector
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageItem(
                message = message,
                userAvatar = userAvatar,
                aiAvatar = aiAvatar
            )
        }
    }
}

@Composable
fun MessageItem(
    message: MessageContent,
    userAvatar: ImageVector,
    aiAvatar: ImageVector
) {
    val isUserMessage = message.role == Role.USER
    val avatar = if (isUserMessage) userAvatar else aiAvatar
    val horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUserMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val bubbleShape = if (isUserMessage) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }
    val itemContentDescription = if (isUserMessage) "我的消息: ${message.messageContent}" else "AI回复: ${message.messageContent}"


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = itemContentDescription },
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUserMessage) {
            AvatarImage(imageVector = avatar, description = "AI头像")
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            tonalElevation = 1.dp // Adds a subtle shadow
        ) {
            Text(
                text = message.messageContent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (isUserMessage) {
            Spacer(modifier = Modifier.width(8.dp))
            AvatarImage(imageVector = userAvatar, description = "我的头像")
        }
    }
}

@Composable
fun AvatarImage(imageVector: ImageVector, description: String) {
    Image(
        imageVector = imageVector,
        contentDescription = description,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentScale = ContentScale.Crop
    )
}
/* Example using painterResource if you have drawables
@Composable
fun AvatarImage(painter: Painter, description: String) {
    Image(
        painter = painter,
        contentDescription = description,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}
*/


@Composable
fun InputBarArea(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isVoiceMode: Boolean,
    onToggleInputMode: () -> Unit,
    isFunctionPanelExpanded: Boolean,
    onToggleFunctionPanel: () -> Unit,
    onSendVoiceMessage: (String) -> Unit // String for placeholder
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }
    val voiceButtonFocusRequester = remember { FocusRequester() }
    val hapticFeedback = LocalHapticFeedback.current
    val hasStartedPressAndRecord = remember { mutableStateOf(false) }

    LaunchedEffect(!isVoiceMode && !isFunctionPanelExpanded) {
        if (!isVoiceMode && !isFunctionPanelExpanded) {
            textFieldFocusRequester.requestFocus()
            keyboardController?.show()
            Log.d("ChatScreen", "键盘显示")
        } else if (isVoiceMode) {
            focusManager.clearFocus()
            keyboardController?.hide()
            Log.d("ChatScreen", "键盘隐藏，准备请求焦点")
            kotlinx.coroutines.delay(50)
            voiceButtonFocusRequester.requestFocus()
            Log.d("ChatScreen", "文本语音切换按钮请求焦点")
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Surface(
        tonalElevation = 3.dp, // Give some elevation to distinguish from messages
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语音/键盘切换按钮
                IconButton(
                    onClick = {
                        Log.d(
                            "ChatScreen",
                            "点击了文本语音切换按钮，isVoiceMode值改变之前: $isVoiceMode"
                        )
                        onToggleInputMode()
                        Log.d(
                            "ChatScreen",
                            "点击了文本语音切换按钮，isVoiceMode值改变之后: $isVoiceMode"
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (isVoiceMode) "切换到文本输入模式" else "切换到语音输入模式"
                    }
                ) {
                    Icon(
                        imageVector = if (isVoiceMode) Icons.Filled.KeyboardArrowUp else Icons.Filled.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 编辑框/语音条
                if (isVoiceMode) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    Log.d("ChatScreen", "在LaunchedEffect外部观察长按isPressed: $isPressed")
                    LaunchedEffect(isPressed) {
                        Log.d("ChatScreen", "在LaunchedEffect内部观察长按isPressed: $isPressed")
                        if (isPressed) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            hasStartedPressAndRecord.value = true
                            Log.d("ChatScreen", "开始录音")
                        } else {
                            // 松手发送
                            if (hasStartedPressAndRecord.value) {
                                hasStartedPressAndRecord.value = false
                                onSendVoiceMessage("语音消息")
                                Log.d("ChatScreen", "发送语音消息结束")
                            }
                        }
                    }

                    Button(
                        onClick = { /*Log.d("ChatScreen", "在button的onClick中观察长按isPressed: $isPressed")*/ },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(voiceButtonFocusRequester)
                            .onFocusChanged { focusState ->
                                Log.d(
                                    "VoiceBtnFocusDebug",
                                    "按住说话按钮 - isFocused: ${focusState.isFocused}, hasFocus: ${focusState.hasFocus}"
                                )
                            }
                            .height(TextFieldDefaults.MinHeight),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        interactionSource = interactionSource
                    ) {
                        Text("按住说话", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    OutlinedTextField( // 文本输入模式
                        value = inputText,
                        onValueChange = onInputTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(textFieldFocusRequester),
                        placeholder = { Text("输入消息...") },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            // Using new TextFieldDefaults for M3
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 右侧“+”按钮
                IconButton(
                    onClick = onToggleFunctionPanel,
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (isFunctionPanelExpanded) "收起功能面板" else "展开功能面板"
                    }
                ) {
                    Icon(
                        imageVector = if (isFunctionPanelExpanded) Icons.Filled.AccountCircle else Icons.Filled.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // “发送”按钮
                if (!isVoiceMode && inputText.isNotBlank()) {
                    Log.d("ChatScreen", "显示“发送”按钮")
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSendMessage,
                        modifier = Modifier.semantics { contentDescription = "发送消息" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 功能面板
            AnimatedVisibility(visible = isFunctionPanelExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp), // 高度根据功能入口再定义
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Slightly different background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "功能开发中",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        }
}