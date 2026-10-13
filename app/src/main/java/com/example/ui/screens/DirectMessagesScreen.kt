package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseRepository
import com.example.data.media.CloudinaryUploader
import com.example.data.model.DirectConversation
import com.example.data.model.DirectMessage
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceCard
import com.example.ui.theme.DarkNavySurfaceElevated
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectMessagesScreen(
    repository: FirebaseRepository,
    selectedUserToChat: DirectConversation? = null,
    onCloseChat: () -> Unit,
    onOpenMediaViewer: (String, Boolean) -> Unit
) {
    var activeChatUser by remember { mutableStateOf<DirectConversation?>(selectedUserToChat) }

    LaunchedEffect(selectedUserToChat) {
        if (selectedUserToChat != null) {
            activeChatUser = selectedUserToChat
            repository.openDirectChat(selectedUserToChat.otherUid)
        }
    }

    if (activeChatUser != null) {
        BackHandler {
            activeChatUser = null
            onCloseChat()
        }

        DirectChatView(
            repository = repository,
            otherUser = activeChatUser!!,
            onBack = {
                activeChatUser = null
                onCloseChat()
            },
            onOpenMediaViewer = onOpenMediaViewer
        )
    } else {
        DirectConversationsList(
            repository = repository,
            onSelectConversation = { conv ->
                activeChatUser = conv
                repository.openDirectChat(conv.otherUid)
            }
        )
    }
}

@Composable
fun DirectConversationsList(
    repository: FirebaseRepository,
    onSelectConversation: (DirectConversation) -> Unit
) {
    val conversations by repository.directConversations.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkNavySurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "💌 الرسائل الخاصة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📭 لا توجد محادثات خاصة بعد",
                        color = Color.Gray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك بدء محادثة بالضغط على صورة أي مستخدم من قائمة الأعضاء أو الأصدقاء",
                        color = Color.DarkGray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(conversations, key = { it.otherUid }) { conv ->
                    ConversationItem(
                        conversation = conv,
                        onClick = { onSelectConversation(conv) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: DirectConversation,
    onClick: () -> Unit
) {
    val timeFormatted = remember(conversation.lastTime) {
        if (conversation.lastTime == 0L) ""
        else SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(conversation.lastTime))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = conversation.otherAvatar,
            isOnline = conversation.isOnline,
            size = 48.dp,
            isOwner = conversation.otherRole.lowercase() == "owner"
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.otherName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                RoleBadge(role = conversation.otherRole)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage.ifEmpty { "ابدأ المحادثة الآن..." },
                color = if (conversation.unreadCount > 0) EmeraldPrimary else Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeFormatted,
                color = Color.Gray,
                fontSize = 11.sp
            )
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DirectChatView(
    repository: FirebaseRepository,
    otherUser: DirectConversation,
    onBack: () -> Unit,
    onOpenMediaViewer: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by repository.activeDirectMessages.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val isVideo = mimeType.startsWith("video")
            isUploading = true
            scope.launch {
                val uploadedUrl = CloudinaryUploader.uploadMedia(context, uri, isVideo)
                isUploading = false
                if (uploadedUrl != null) {
                    repository.sendDirectMessage(
                        otherUid = otherUser.otherUid,
                        otherName = otherUser.otherName,
                        text = if (isVideo) "🎥 مقطع فيديو" else "🖼️ صورة",
                        mediaUrl = uploadedUrl,
                        mediaType = if (isVideo) "video" else "image"
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkNavySurface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            UserAvatar(
                avatarUrl = otherUser.otherAvatar,
                isOnline = otherUser.isOnline,
                size = 40.dp,
                isOwner = otherUser.otherRole.lowercase() == "owner"
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = otherUser.otherName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RoleBadge(role = otherUser.otherRole)
                }
                Text(
                    text = if (otherUser.isOnline) "🟢 متصل الآن" else "⚪ غير متصل",
                    color = if (otherUser.isOnline) EmeraldPrimary else Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { msg ->
                val isMine = msg.from == repository.myUid
                DirectMessageBubble(
                    message = msg,
                    isMine = isMine,
                    onMediaClick = { url, isVideo -> onOpenMediaViewer(url, isVideo) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        if (isUploading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkNavySurfaceCard)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = EmeraldPrimary,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("جاري رفع الوسائط...", color = EmeraldPrimary, fontSize = 12.sp)
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(DarkNavySurfaceCard, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("اكتب رسالة خاصة...", fontSize = 13.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkNavySurfaceCard,
                    unfocusedContainerColor = DarkNavySurfaceCard
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (messageText.trim().isNotEmpty()) {
                        repository.sendDirectMessage(
                            otherUid = otherUser.otherUid,
                            otherName = otherUser.otherName,
                            text = messageText.trim()
                        )
                        messageText = ""
                    }
                },
                enabled = messageText.trim().isNotEmpty(),
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (messageText.trim().isNotEmpty()) EmeraldPrimary else Color.DarkGray,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (messageText.trim().isNotEmpty()) Color.Black else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectMessageBubble(
    message: DirectMessage,
    isMine: Boolean,
    onMediaClick: (String, Boolean) -> Unit
) {
    val bubbleColor = if (isMine) Color(0xFF004D40) else DarkNavySurfaceElevated
    val timeFormatted = remember(message.time) {
        SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(message.time))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!message.media.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .clickable {
                                onMediaClick(message.media, message.type == "video")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = message.media,
                            contentDescription = "Media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (message.type == "video") {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (message.read) "✓✓" else "✓",
                            color = if (message.read) EmeraldPrimary else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
