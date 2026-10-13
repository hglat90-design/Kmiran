package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.firebase.FirebaseRepository
import com.example.data.media.CloudinaryUploader
import com.example.data.model.ChatMessage
import com.example.data.model.ReplyToData
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkNavySurfaceCard
import com.example.ui.theme.DarkNavySurfaceElevated
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldOwner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    repository: FirebaseRepository,
    onUserClick: (String, String, String) -> Unit,
    onOpenMediaViewer: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by repository.publicMessages.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val latestAlert by repository.latestAlert.collectAsState()
    val activeWarning by repository.activeWarning.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ReplyToData?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo & Video Picker using modern zero-permission photo picker
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
                    repository.sendPublicMessage(
                        text = if (isVideo) "🎥 مقطع فيديو" else "🖼️ صورة",
                        mediaUrl = uploadedUrl,
                        mediaType = if (isVideo) "video" else "image",
                        replyTo = replyingTo
                    )
                    replyingTo = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Urgent Admin Alert Banner
        latestAlert?.let { alert ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFB71C1C)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Alert",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🚨 تنبيه رسمي: ${alert.sender}",
                            color = GoldOwner,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = alert.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // System Warning Card
        activeWarning?.let { warning ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تحذير رسمي من الإدارة",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = warning.message,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { repository.acknowledgeWarning(warning.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فهمت", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(messages, key = { it.id }) { msg ->
                val isMine = msg.uid == repository.myUid
                val isOwner = msg.role.lowercase() == "owner"

                MessageItem(
                    message = msg,
                    isMine = isMine,
                    isOwner = isOwner,
                    canDelete = isMine || (currentUser?.role?.lowercase() in listOf("owner", "manager", "mod")),
                    onDelete = { repository.deletePublicMessage(msg.id) },
                    onReply = {
                        replyingTo = ReplyToData(
                            key = msg.id,
                            sender = msg.sender,
                            text = msg.text,
                            role = msg.role
                        )
                    },
                    onAvatarClick = { onUserClick(msg.uid, msg.sender, msg.avatar) },
                    onMediaClick = { url, isVideo -> onOpenMediaViewer(url, isVideo) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Reply quote indicator
        AnimatedVisibility(visible = replyingTo != null) {
            replyingTo?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkNavySurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Reply",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رد على: ${reply.sender}",
                            color = EmeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reply.text,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { replyingTo = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Uploading indicator
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
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جاري رفع الوسائط على السحابة...",
                    color = CyanAccent,
                    fontSize = 12.sp
                )
            }
        }

        // Bottom Chat Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attach Media Button
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
                    contentDescription = "Attach Media",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Text Field
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = {
                    Text(
                        "اكتب رسالة في الغرفة العامة...",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkNavySurfaceCard,
                    unfocusedContainerColor = DarkNavySurfaceCard
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (messageText.trim().isNotEmpty()) {
                        repository.sendPublicMessage(
                            text = messageText.trim(),
                            replyTo = replyingTo
                        )
                        messageText = ""
                        replyingTo = null
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
private fun MessageItem(
    message: ChatMessage,
    isMine: Boolean,
    isOwner: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onReply: () -> Unit,
    onAvatarClick: () -> Unit,
    onMediaClick: (String, Boolean) -> Unit
) {
    val bubbleBrush = when {
        isOwner -> Brush.linearGradient(listOf(Color(0xFF332A00), Color(0xFF594500)))
        isMine -> Brush.linearGradient(listOf(Color(0xFF004D40), Color(0xFF00332C)))
        else -> Brush.linearGradient(listOf(DarkNavySurfaceCard, DarkNavySurfaceElevated))
    }

    val borderModifier = if (isOwner) {
        Modifier.border(1.5.dp, GoldOwner, RoundedCornerShape(16.dp))
    } else Modifier

    val timeFormatted = remember(message.time) {
        SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(message.time))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        if (!isMine) {
            UserAvatar(
                avatarUrl = message.avatar,
                size = 38.dp,
                isOwner = isOwner,
                onClick = onAvatarClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .background(bubbleBrush, RoundedCornerShape(16.dp))
                .then(borderModifier)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header (Sender + Role)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.sender,
                        color = if (isOwner) GoldOwner else if (isMine) EmeraldPrimary else CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onAvatarClick() }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RoleBadge(role = message.role)
                    Spacer(modifier = Modifier.weight(1f))

                    // Quick Actions
                    IconButton(
                        onClick = onReply,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Reply",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Quote preview if reply
                message.replyTo?.let { reply ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .border(width = 2.dp, color = EmeraldPrimary, shape = RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                text = "↩ ${reply.sender}",
                                color = EmeraldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = reply.text,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }

                // Media Attachment
                if (!message.media.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Text Content
                if (message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // Timestamp
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
