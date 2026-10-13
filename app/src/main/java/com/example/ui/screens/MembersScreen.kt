package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseRepository
import com.example.data.model.DirectConversation
import com.example.data.model.UserData
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CrimsonBan
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldOwner

@Composable
fun MembersScreen(
    repository: FirebaseRepository,
    onStartChat: (DirectConversation) -> Unit
) {
    val allUsers by repository.allUsers.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedUserForDialog by remember { mutableStateOf<UserData?>(null) }
    var showSendAlertDialog by remember { mutableStateOf(false) }
    var alertText by remember { mutableStateOf("") }

    val isOwner = currentUser?.role?.lowercase() == "owner"
    val isManager = currentUser?.role?.lowercase() == "manager"
    val isMod = currentUser?.role?.lowercase() == "mod"
    val canAdmin = isOwner || isManager || isMod

    val onlineUsers = allUsers.filter { it.status == "online" && !it.hidden }
    val adminUsers = allUsers.filter { it.role.lowercase() in listOf("owner", "manager", "mod") }

    val displayList = when (selectedTabIndex) {
        0 -> allUsers.filter { !it.hidden }
        1 -> onlineUsers
        else -> adminUsers
    }

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
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "👥 الأعضاء (${allUsers.size})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))

            if (isOwner) {
                Button(
                    onClick = { showSendAlertDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بث تنبيه", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkNavySurface,
            contentColor = EmeraldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = EmeraldPrimary
                )
            }
        ) {
            listOf("الكل (${allUsers.size})", "المتصلون (${onlineUsers.size})", "الإدارة (${adminUsers.size})").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == idx) EmeraldPrimary else Color.Gray
                        )
                    }
                )
            }
        }

        // Users List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayList, key = { it.uid }) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedUserForDialog = user }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = user.avatar,
                            isOnline = user.status == "online",
                            size = 44.dp,
                            isOwner = user.role.lowercase() == "owner"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(role = user.role)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (user.status == "online") "🟢 متصل الآن" else "⚪ غير متصل",
                                color = if (user.status == "online") EmeraldPrimary else Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Message shortcut button
                        if (user.uid != repository.myUid) {
                            Button(
                                onClick = {
                                    onStartChat(
                                        DirectConversation(
                                            otherUid = user.uid,
                                            otherName = user.name,
                                            otherAvatar = user.avatar,
                                            otherRole = user.role,
                                            isOnline = user.status == "online"
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("خاص", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // User Profile & Admin Actions Dialog
    selectedUserForDialog?.let { user ->
        val isTargetSelf = user.uid == repository.myUid
        val isTargetOwner = user.role.lowercase() == "owner"

        AlertDialog(
            onDismissRequest = { selectedUserForDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(avatarUrl = user.avatar, size = 46.dp, isOwner = isTargetOwner)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        RoleBadge(role = user.role)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "معرف الجهاز: ${user.uid.take(16)}...",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    if (!isTargetSelf) {
                        Button(
                            onClick = {
                                repository.sendFriendRequest(user.uid, user.name)
                                selectedUserForDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال طلب صداقة", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onStartChat(
                                    DirectConversation(
                                        otherUid = user.uid,
                                        otherName = user.name,
                                        otherAvatar = user.avatar,
                                        otherRole = user.role,
                                        isOnline = user.status == "online"
                                    )
                                )
                                selectedUserForDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavySurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = CyanAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء محادثة خاصة", color = Color.White)
                        }
                    }

                    // Admin Management Tools
                    if (canAdmin && !isTargetSelf && !isTargetOwner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🛡️ أدوات الإدارة الصارمة:",
                            color = GoldOwner,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Role Elevation Buttons (if Owner)
                        if (isOwner) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        repository.changeUserRole(user.uid, "manager")
                                        selectedUserForDialog = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ترقية مدير", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        repository.changeUserRole(user.uid, "mod")
                                        selectedUserForDialog = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("مشرف", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        repository.changeUserRole(user.uid, "user")
                                        selectedUserForDialog = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("عادي", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }

                        // Mute button
                        Button(
                            onClick = {
                                repository.muteUser(user.uid, 60) // 1 hour
                                selectedUserForDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("كتم المستخدم لمدة ساعة", color = Color.White)
                        }

                        // Permanent Hardware Ban Button
                        Button(
                            onClick = {
                                repository.banUserPermanent(user.uid, user.name)
                                selectedUserForDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonBan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⛔ حظر عتادي نهائي (لا يمكن تجاوزه)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedUserForDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            },
            containerColor = DarkNavySurface
        )
    }

    // Broadcast Urgent Alert Dialog (For Owner)
    if (showSendAlertDialog) {
        AlertDialog(
            onDismissRequest = { showSendAlertDialog = false },
            title = {
                Text("بث تنبيه عاجل لجميع الأعضاء 🚨", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "سيظهر هذا التنبيه فوراً أعلى الدردشة وبإشعار رسمي عاجل لكافة الأجهزة.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = alertText,
                        onValueChange = { alertText = it },
                        placeholder = { Text("اكتب نص التنبيه...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonBan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (alertText.trim().isNotEmpty()) {
                            repository.sendBroadcastAlert(alertText.trim())
                            alertText = ""
                            showSendAlertDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonBan)
                ) {
                    Text("بث التنبيه الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSendAlertDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = DarkNavySurface
        )
    }
}
