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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.FriendRequest
import com.example.data.model.FriendUser
import com.example.data.model.UserData
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FriendsScreen(
    repository: FirebaseRepository,
    onStartChatWithFriend: (DirectConversation) -> Unit
) {
    val incomingRequests by repository.incomingFriendRequests.collectAsState()
    val friendsList by repository.friendsList.collectAsState()
    val allUsers by repository.allUsers.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf(
        "الأصدقاء (${friendsList.size})",
        "طلبات الصداقة (${incomingRequests.size})",
        "إضافة صديق"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkNavySurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "👥 الأصدقاء وطلبات الصداقة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
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
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) EmeraldPrimary else Color.Gray
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> FriendsListView(
                friends = friendsList,
                onStartChat = onStartChatWithFriend
            )
            1 -> FriendRequestsListView(
                requests = incomingRequests,
                onAccept = { repository.acceptFriendRequest(it) },
                onReject = { repository.rejectFriendRequest(it) }
            )
            2 -> AddFriendsView(
                allUsers = allUsers.filter { it.uid != repository.myUid },
                myFriends = friendsList.map { it.uid }.toSet(),
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onSendRequest = { targetUid, targetName ->
                    repository.sendFriendRequest(targetUid, targetName)
                }
            )
        }
    }
}

@Composable
private fun FriendsListView(
    friends: List<FriendUser>,
    onStartChat: (DirectConversation) -> Unit
) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📭 ليس لديك أصدقاء مضافون بعد",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "توجه إلى تبويب 'إضافة صديق' لإرسال طلبات الصداقة",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friends, key = { it.uid }) { friend ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = friend.avatar,
                            isOnline = friend.status == "online",
                            size = 46.dp,
                            isOwner = friend.role.lowercase() == "owner"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = friend.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(role = friend.role)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (friend.status == "online") "🟢 متصل الآن" else "⚪ غير متصل",
                                color = if (friend.status == "online") EmeraldPrimary else Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Message button
                        IconButton(
                            onClick = {
                                onStartChat(
                                    DirectConversation(
                                        otherUid = friend.uid,
                                        otherName = friend.name,
                                        otherAvatar = friend.avatar,
                                        otherRole = friend.role,
                                        isOnline = friend.status == "online"
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(EmeraldPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Chat",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestsListView(
    requests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onReject: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✨ لا توجد طلبات صداقة معلقة",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "عندما يرسل لك أي مستخدم طلب صداقة سيظهر هنا فوراً حتى لو كان التطبيق مغلقاً",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                val timeFormatted = remember(req.timestamp) {
                    if (req.timestamp == 0L) ""
                    else SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(req.timestamp))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = req.senderAvatar,
                            size = 48.dp,
                            isOwner = req.senderRole.lowercase() == "owner"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = req.senderName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(role = req.senderRole)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "طلب صداقة • $timeFormatted",
                                color = CyanAccent,
                                fontSize = 11.sp
                            )
                        }

                        // Accept Button
                        IconButton(
                            onClick = { onAccept(req) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Reject Button
                        IconButton(
                            onClick = { onReject(req) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFB71C1C), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reject",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendsView(
    allUsers: List<UserData>,
    myFriends: Set<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSendRequest: (String, String) -> Unit
) {
    val filteredUsers = remember(allUsers, searchQuery, myFriends) {
        allUsers.filter { user ->
            !user.hidden && (searchQuery.isEmpty() || user.name.contains(searchQuery, ignoreCase = true))
        }
    }

    var sentUids by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("بحث عن مستخدم بالاسم...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = DarkNavySurfaceCard,
                unfocusedContainerColor = DarkNavySurfaceCard
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredUsers, key = { it.uid }) { user ->
                val isFriend = myFriends.contains(user.uid)
                val isSent = sentUids.contains(user.uid)

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(role = user.role)
                            }
                            Text(
                                text = if (user.status == "online") "🟢 متصل" else "⚪ غير متصل",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        when {
                            isFriend -> {
                                Text(
                                    text = "صديق ✓",
                                    color = EmeraldPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isSent -> {
                                Text(
                                    text = "تم الطلب ⏳",
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        onSendRequest(user.uid, user.name)
                                        sentUids = sentUids + user.uid
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("طلب صداقة", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
