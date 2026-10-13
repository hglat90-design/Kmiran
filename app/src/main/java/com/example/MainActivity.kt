package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseRepository
import com.example.data.model.DirectConversation
import com.example.service.AppNotificationService
import com.example.ui.screens.BanScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DirectMessagesScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = FirebaseRepository(applicationContext)

        // Start background notification service
        try {
            AppNotificationService.start(this)
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme {
                // Arabic RTL Layout direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppMainEntry(repository = repository)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repository.updateUserPresence(true)
    }

    override fun onPause() {
        super.onPause()
        repository.updateUserPresence(false)
    }
}

@Composable
fun AppMainEntry(repository: FirebaseRepository) {
    val context = LocalContext.current
    val isBanned by repository.isBanned.collectAsState()
    val banReason by repository.banReason.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AppNotificationService.start(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Unbypassable Ban Screen check
    if (isBanned) {
        BanScreen(
            banReason = banReason,
            deviceFingerprint = repository.deviceFingerprint
        )
        return
    }

    // Login Screen check
    if (currentUser == null) {
        LoginScreen(
            repository = repository,
            onLoginSuccess = {}
        )
        return
    }

    // Main App with Bottom Navigation
    MainDashboard(repository = repository)
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MainDashboard(repository: FirebaseRepository) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var userToChatDirectly by remember { mutableStateOf<DirectConversation?>(null) }
    var fullscreenMediaUrl by remember { mutableStateOf<String?>(null) }
    var isMediaVideo by remember { mutableStateOf(false) }

    val directConversations by repository.directConversations.collectAsState()
    val incomingFriendRequests by repository.incomingFriendRequests.collectAsState()

    val totalUnreadDMs = remember(directConversations) {
        directConversations.sumOf { it.unreadCount }
    }

    val navItems = listOf(
        NavItem("الغرفة", Icons.Default.Chat),
        NavItem("الخاص", Icons.Default.Mail, totalUnreadDMs),
        NavItem("الحالات", Icons.Default.PhotoCamera),
        NavItem("الأصدقاء", Icons.Default.People, incomingFriendRequests.size),
        NavItem("الأعضاء", Icons.Default.Group)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkNavySurface,
                contentColor = Color.White
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedNavIndex == index,
                        onClick = {
                            selectedNavIndex = index
                            if (index != 1) userToChatDirectly = null
                        },
                        icon = {
                            if (item.badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.Black
                                        ) {
                                            Text(
                                                text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedNavIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavIndex) {
                0 -> ChatScreen(
                    repository = repository,
                    onUserClick = { uid, name, avatar ->
                        if (uid != repository.myUid) {
                            userToChatDirectly = DirectConversation(
                                otherUid = uid,
                                otherName = name,
                                otherAvatar = avatar
                            )
                            selectedNavIndex = 1
                        }
                    },
                    onOpenMediaViewer = { url, isVideo ->
                        fullscreenMediaUrl = url
                        isMediaVideo = isVideo
                    }
                )
                1 -> DirectMessagesScreen(
                    repository = repository,
                    selectedUserToChat = userToChatDirectly,
                    onCloseChat = { userToChatDirectly = null },
                    onOpenMediaViewer = { url, isVideo ->
                        fullscreenMediaUrl = url
                        isMediaVideo = isVideo
                    }
                )
                2 -> StatusScreen(
                    repository = repository
                )
                3 -> FriendsScreen(
                    repository = repository,
                    onStartChatWithFriend = { conv ->
                        userToChatDirectly = conv
                        selectedNavIndex = 1
                    }
                )
                4 -> MembersScreen(
                    repository = repository,
                    onStartChat = { conv ->
                        userToChatDirectly = conv
                        selectedNavIndex = 1
                    }
                )
            }

            // Fullscreen Media Viewer Overlay
            fullscreenMediaUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { fullscreenMediaUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { fullscreenMediaUrl = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isMediaVideo) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
