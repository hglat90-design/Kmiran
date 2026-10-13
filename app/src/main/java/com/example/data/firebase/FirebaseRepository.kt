package com.example.data.firebase

import android.content.Context
import com.example.data.model.ChatAlert
import com.example.data.model.ChatMessage
import com.example.data.model.DirectConversation
import com.example.data.model.DirectMessage
import com.example.data.model.FriendRequest
import com.example.data.model.FriendUser
import com.example.data.model.ReplyToData
import com.example.data.model.StatusStory
import com.example.data.model.StatusViewInfo
import com.example.data.model.SystemWarning
import com.example.data.model.UserData
import com.example.security.DeviceSecurityManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned.asStateFlow()

    private val _banReason = MutableStateFlow("تم حظر جهازك نهائياً من استخدام التطبيق")
    val banReason: StateFlow<String> = _banReason.asStateFlow()

    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserData>>(emptyList())
    val allUsers: StateFlow<List<UserData>> = _allUsers.asStateFlow()

    private val _publicMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val publicMessages: StateFlow<List<ChatMessage>> = _publicMessages.asStateFlow()

    private val _statuses = MutableStateFlow<List<StatusStory>>(emptyList())
    val statuses: StateFlow<List<StatusStory>> = _statuses.asStateFlow()

    private val _directConversations = MutableStateFlow<List<DirectConversation>>(emptyList())
    val directConversations: StateFlow<List<DirectConversation>> = _directConversations.asStateFlow()

    private val _activeDirectMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val activeDirectMessages: StateFlow<List<DirectMessage>> = _activeDirectMessages.asStateFlow()

    private val _incomingFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingFriendRequests: StateFlow<List<FriendRequest>> = _incomingFriendRequests.asStateFlow()

    private val _friendsList = MutableStateFlow<List<FriendUser>>(emptyList())
    val friendsList: StateFlow<List<FriendUser>> = _friendsList.asStateFlow()

    private val _latestAlert = MutableStateFlow<ChatAlert?>(null)
    val latestAlert: StateFlow<ChatAlert?> = _latestAlert.asStateFlow()

    private val _activeWarning = MutableStateFlow<SystemWarning?>(null)
    val activeWarning: StateFlow<SystemWarning?> = _activeWarning.asStateFlow()

    val database: FirebaseDatabase
    private val dbRef: DatabaseReference

    val deviceFingerprint = DeviceSecurityManager.getFullDeviceFingerprint(context)
    val myUid: String = deviceFingerprint.primaryHardwareId

    private var currentDmChatId: String? = null
    private var currentDmListener: ValueEventListener? = null

    init {
        // Initialize Firebase with the explicit project configuration from the provided code
        val existingApp = try {
            FirebaseApp.getInstance()
        } catch (_: Exception) {
            null
        }

        if (existingApp == null) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyD9qPQHL_BBAeJ-fXtgoRr5rXPMetvN3W8")
                .setApplicationId("1:870807919125:web:4426d8b27054e7f3428c1c")
                .setDatabaseUrl("https://sory-1ab08-default-rtdb.firebaseio.com")
                .setProjectId("sory-1ab08")
                .setStorageBucket("sory-1ab08.firebasestorage.app")
                .build()
            FirebaseApp.initializeApp(context, options)
        }

        database = FirebaseDatabase.getInstance("https://sory-1ab08-default-rtdb.firebaseio.com")
        try {
            database.setPersistenceEnabled(true)
        } catch (_: Exception) {}
        dbRef = database.reference

        // Initialize listeners
        startBanVerification()
        listenUsers()
        listenPublicMessages()
        listenStatuses()
        listenDirectInbox()
        listenFriendRequests()
        listenFriends()
        listenAlerts()
        listenWarnings()
    }

    /**
     * Continuous multi-identifier unbypassable ban verification.
     * Checks primary hardware ID, widevine hardware ID, android ID, and cloning environment.
     */
    private fun startBanVerification() {
        // Check local clone detection immediately
        if (deviceFingerprint.cloneDetection.isCloned) {
            _isBanned.value = true
            _banReason.value = "تم حظرك فوراً: ${deviceFingerprint.cloneDetection.detectionReason}"
            // Report to security logs
            dbRef.child("security_logs").child("cloning_attempts").child(myUid).setValue(
                mapOf(
                    "primaryHardwareId" to deviceFingerprint.primaryHardwareId,
                    "widevineId" to deviceFingerprint.widevineId,
                    "androidId" to deviceFingerprint.androidId,
                    "reason" to deviceFingerprint.cloneDetection.detectionReason,
                    "timestamp" to ServerValue.TIMESTAMP
                )
            )
            // Auto-ban this hardware fingerprint
            dbRef.child("bans").child(myUid).setValue(true)
            dbRef.child("bans").child(deviceFingerprint.widevineId).setValue(true)
            return
        }

        // Listen for ban on any hardware identifier
        dbRef.child("bans").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isBannedByHardware = snapshot.child(myUid).exists() ||
                        snapshot.child(deviceFingerprint.widevineId).exists() ||
                        snapshot.child(deviceFingerprint.androidId).exists()

                if (isBannedByHardware) {
                    _isBanned.value = true
                    _banReason.value = "تم حظر هذا الجهاز نهائياً من قِبل إدارة التطبيق"
                } else {
                    _isBanned.value = false
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * User registration or login session.
     */
    suspend fun registerOrUpdateUser(name: String, avatar: String): Boolean {
        if (_isBanned.value) return false
        val userMap = mapOf(
            "name" to name,
            "avatar" to avatar,
            "status" to "online",
            "lastSeen" to ServerValue.TIMESTAMP,
            "createdAt" to ServerValue.TIMESTAMP,
            "hardwareFingerprint" to deviceFingerprint.primaryHardwareId,
            "widevineId" to deviceFingerprint.widevineId
        )
        return try {
            dbRef.child("users").child(myUid).updateChildren(userMap).await()
            // Set disconnect handler for presence
            dbRef.child("users").child(myUid).child("status").onDisconnect().setValue("offline")
            dbRef.child("users").child(myUid).child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateUserPresence(online: Boolean) {
        if (_isBanned.value) return
        val status = if (online) "online" else "offline"
        dbRef.child("users").child(myUid).updateChildren(
            mapOf(
                "status" to status,
                "lastSeen" to ServerValue.TIMESTAMP
            )
        )
    }

    private fun listenUsers() {
        dbRef.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserData>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val avatar = child.child("avatar").getValue(String::class.java) ?: ""
                    val role = child.child("role").getValue(String::class.java) ?: "user"
                    val status = child.child("status").getValue(String::class.java) ?: "offline"
                    val lastSeen = child.child("lastSeen").getValue(Long::class.java) ?: 0L
                    val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                    val hidden = child.child("hidden").getValue(Boolean::class.java) ?: false
                    val u = UserData(
                        uid = uid,
                        name = name,
                        avatar = avatar,
                        role = role,
                        status = status,
                        lastSeen = lastSeen,
                        createdAt = createdAt,
                        hidden = hidden
                    )
                    list.add(u)
                    if (uid == myUid) {
                        _currentUser.value = u
                    }
                }
                _allUsers.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenPublicMessages() {
        dbRef.child("messages").limitToLast(150).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val uid = child.child("uid").getValue(String::class.java) ?: ""
                    val sender = child.child("sender").getValue(String::class.java) ?: ""
                    val text = child.child("text").getValue(String::class.java) ?: ""
                    val media = child.child("media").getValue(String::class.java)
                    val type = child.child("type").getValue(String::class.java) ?: "text"
                    val youtubeId = child.child("youtubeId").getValue(String::class.java)
                    val role = child.child("role").getValue(String::class.java) ?: "user"
                    val avatar = child.child("avatar").getValue(String::class.java) ?: ""
                    val time = child.child("time").getValue(Long::class.java) ?: 0L

                    var reply: ReplyToData? = null
                    val replySnap = child.child("replyTo")
                    if (replySnap.exists()) {
                        reply = ReplyToData(
                            key = replySnap.child("key").getValue(String::class.java) ?: "",
                            sender = replySnap.child("sender").getValue(String::class.java) ?: "",
                            text = replySnap.child("text").getValue(String::class.java) ?: "",
                            role = replySnap.child("role").getValue(String::class.java) ?: "user"
                        )
                    }

                    list.add(
                        ChatMessage(
                            id = id,
                            uid = uid,
                            sender = sender,
                            text = text,
                            media = media,
                            type = type,
                            youtubeId = youtubeId,
                            role = role,
                            avatar = avatar,
                            time = time,
                            replyTo = reply
                        )
                    )
                }
                _publicMessages.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendPublicMessage(text: String, mediaUrl: String? = null, mediaType: String = "text", replyTo: ReplyToData? = null) {
        if (_isBanned.value) return
        val user = _currentUser.value ?: return
        val newRef = dbRef.child("messages").push()
        val msgMap = mutableMapOf<String, Any>(
            "uid" to myUid,
            "sender" to user.name,
            "avatar" to user.avatar,
            "role" to user.role,
            "text" to text,
            "type" to mediaType,
            "time" to ServerValue.TIMESTAMP
        )
        if (!mediaUrl.isNullOrEmpty()) {
            msgMap["media"] = mediaUrl
        }
        if (replyTo != null) {
            msgMap["replyTo"] = mapOf(
                "key" to replyTo.key,
                "sender" to replyTo.sender,
                "text" to replyTo.text,
                "role" to replyTo.role
            )
        }
        newRef.setValue(msgMap)
    }

    fun deletePublicMessage(messageId: String) {
        dbRef.child("messages").child(messageId).removeValue()
    }

    private fun listenStatuses() {
        dbRef.child("statuses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StatusStory>()
                val now = System.currentTimeMillis()
                val oneDayAgo = now - 24 * 60 * 60 * 1000L

                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val time = child.child("time").getValue(Long::class.java) ?: 0L
                    if (time < oneDayAgo) continue // 24h expiration

                    val uid = child.child("uid").getValue(String::class.java) ?: ""
                    val sender = child.child("sender").getValue(String::class.java) ?: ""
                    val avatar = child.child("avatar").getValue(String::class.java) ?: ""
                    val text = child.child("text").getValue(String::class.java) ?: ""
                    val media = child.child("media").getValue(String::class.java)
                    val type = child.child("type").getValue(String::class.java) ?: "text"

                    val seenMap = mutableMapOf<String, StatusViewInfo>()
                    for (seenChild in child.child("seenBy").children) {
                        val sUid = seenChild.key ?: continue
                        val sName = seenChild.child("name").getValue(String::class.java) ?: ""
                        val sAvatar = seenChild.child("avatar").getValue(String::class.java) ?: ""
                        val sTime = seenChild.child("time").getValue(Long::class.java) ?: 0L
                        seenMap[sUid] = StatusViewInfo(sName, sAvatar, sTime)
                    }

                    list.add(
                        StatusStory(
                            id = id,
                            uid = uid,
                            sender = sender,
                            avatar = avatar,
                            text = text,
                            media = media,
                            type = type,
                            time = time,
                            seenBy = seenMap
                        )
                    )
                }
                _statuses.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun postStatus(text: String, mediaUrl: String? = null, mediaType: String = "text") {
        if (_isBanned.value) return
        val user = _currentUser.value ?: return
        val newRef = dbRef.child("statuses").push()
        val data = mutableMapOf<String, Any>(
            "uid" to myUid,
            "sender" to user.name,
            "avatar" to user.avatar,
            "text" to text,
            "type" to mediaType,
            "time" to ServerValue.TIMESTAMP,
            "seenBy" to emptyMap<String, Any>()
        )
        if (!mediaUrl.isNullOrEmpty()) {
            data["media"] = mediaUrl
        }
        newRef.setValue(data)
    }

    fun markStatusSeen(statusId: String) {
        val user = _currentUser.value ?: return
        dbRef.child("statuses").child(statusId).child("seenBy").child(myUid).setValue(
            mapOf(
                "name" to user.name,
                "avatar" to user.avatar,
                "time" to ServerValue.TIMESTAMP
            )
        )
    }

    fun deleteStatus(statusId: String) {
        dbRef.child("statuses").child(statusId).removeValue()
    }

    private fun listenDirectInbox() {
        dbRef.child("dm_inbox").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<DirectConversation>()
                for (child in snapshot.children) {
                    val otherUid = child.key ?: continue
                    val otherName = child.child("fromName").getValue(String::class.java) ?: "مستخدم"
                    val otherAvatar = child.child("fromAvatar").getValue(String::class.java) ?: ""
                    val lastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastTime = child.child("lastTime").getValue(Long::class.java) ?: 0L
                    val unreadCount = child.child("count").getValue(Int::class.java) ?: 0

                    val userObj = _allUsers.value.find { it.uid == otherUid }
                    val isOnline = userObj?.status == "online"
                    val role = userObj?.role ?: "user"

                    list.add(
                        DirectConversation(
                            otherUid = otherUid,
                            otherName = otherName,
                            otherAvatar = otherAvatar,
                            otherRole = role,
                            lastMessage = lastMessage,
                            lastTime = lastTime,
                            unreadCount = unreadCount,
                            isOnline = isOnline
                        )
                    )
                }
                _directConversations.value = list.sortedByDescending { it.lastTime }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun openDirectChat(otherUid: String) {
        val chatId = listOf(myUid, otherUid).sorted().joinToString("_")
        if (currentDmChatId == chatId) return

        currentDmListener?.let {
            currentDmChatId?.let { id ->
                dbRef.child("private_chats").child(id).child("messages").removeEventListener(it)
            }
        }

        currentDmChatId = chatId
        // Clear unread badge
        dbRef.child("dm_inbox").child(myUid).child(otherUid).child("count").setValue(0)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = mutableListOf<DirectMessage>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val from = child.child("from").getValue(String::class.java) ?: ""
                    val fromName = child.child("fromName").getValue(String::class.java) ?: ""
                    val fromAvatar = child.child("fromAvatar").getValue(String::class.java) ?: ""
                    val to = child.child("to").getValue(String::class.java) ?: ""
                    val toName = child.child("toName").getValue(String::class.java) ?: ""
                    val text = child.child("text").getValue(String::class.java) ?: ""
                    val media = child.child("media").getValue(String::class.java)
                    val type = child.child("type").getValue(String::class.java) ?: "text"
                    val time = child.child("time").getValue(Long::class.java) ?: 0L
                    val read = child.child("read").getValue(Boolean::class.java) ?: false

                    msgs.add(
                        DirectMessage(
                            id = id,
                            from = from,
                            fromName = fromName,
                            fromAvatar = fromAvatar,
                            to = to,
                            toName = toName,
                            text = text,
                            media = media,
                            type = type,
                            time = time,
                            read = read
                        )
                    )
                }
                _activeDirectMessages.value = msgs
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        currentDmListener = listener
        dbRef.child("private_chats").child(chatId).child("messages").addValueEventListener(listener)
    }

    fun sendDirectMessage(otherUid: String, otherName: String, text: String, mediaUrl: String? = null, mediaType: String = "text") {
        if (_isBanned.value) return
        val user = _currentUser.value ?: return
        val chatId = listOf(myUid, otherUid).sorted().joinToString("_")
        val newRef = dbRef.child("private_chats").child(chatId).child("messages").push()

        val msgMap = mutableMapOf<String, Any>(
            "from" to myUid,
            "fromName" to user.name,
            "fromAvatar" to user.avatar,
            "to" to otherUid,
            "toName" to otherName,
            "text" to text,
            "type" to mediaType,
            "time" to ServerValue.TIMESTAMP,
            "read" to false
        )
        if (!mediaUrl.isNullOrEmpty()) {
            msgMap["media"] = mediaUrl
        }
        newRef.setValue(msgMap)

        val preview = if (!mediaUrl.isNullOrEmpty()) "📎 وسائط" else text
        // Update recipient inbox with increment
        dbRef.child("dm_inbox").child(otherUid).child(myUid).updateChildren(
            mapOf(
                "fromName" to user.name,
                "fromAvatar" to user.avatar,
                "lastMessage" to preview,
                "lastTime" to ServerValue.TIMESTAMP,
                "count" to ServerValue.increment(1)
            )
        )
        // Update sender inbox
        dbRef.child("dm_inbox").child(myUid).child(otherUid).updateChildren(
            mapOf(
                "fromName" to otherName,
                "fromAvatar" to (_allUsers.value.find { it.uid == otherUid }?.avatar ?: ""),
                "lastMessage" to preview,
                "lastTime" to ServerValue.TIMESTAMP,
                "count" to 0
            )
        )
    }

    // -------------------------------------------------------------
    // Friend Request System (نظام طلبات الصداقة)
    // -------------------------------------------------------------
    private fun listenFriendRequests() {
        dbRef.child("friend_requests").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FriendRequest>()
                for (child in snapshot.children) {
                    val fromUid = child.key ?: continue
                    val status = child.child("status").getValue(String::class.java) ?: "pending"
                    val senderName = child.child("senderName").getValue(String::class.java) ?: "مستخدم"
                    val senderAvatar = child.child("senderAvatar").getValue(String::class.java) ?: ""
                    val senderRole = child.child("senderRole").getValue(String::class.java) ?: "user"
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (status == "pending") {
                        list.add(
                            FriendRequest(
                                id = fromUid,
                                fromUid = fromUid,
                                toUid = myUid,
                                senderName = senderName,
                                senderAvatar = senderAvatar,
                                senderRole = senderRole,
                                status = status,
                                timestamp = timestamp
                            )
                        )
                    }
                }
                _incomingFriendRequests.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenFriends() {
        dbRef.child("friends").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FriendUser>()
                for (child in snapshot.children) {
                    val fUid = child.key ?: continue
                    val addedAt = child.child("addedAt").getValue(Long::class.java) ?: 0L
                    val u = _allUsers.value.find { it.uid == fUid }
                    list.add(
                        FriendUser(
                            uid = fUid,
                            name = u?.name ?: (child.child("name").getValue(String::class.java) ?: "صديق"),
                            avatar = u?.avatar ?: (child.child("avatar").getValue(String::class.java) ?: ""),
                            role = u?.role ?: "user",
                            status = u?.status ?: "offline",
                            lastSeen = u?.lastSeen ?: 0L,
                            addedAt = addedAt
                        )
                    )
                }
                _friendsList.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendFriendRequest(targetUid: String, targetName: String) {
        if (_isBanned.value || targetUid == myUid) return
        val user = _currentUser.value ?: return

        dbRef.child("friend_requests").child(targetUid).child(myUid).setValue(
            mapOf(
                "senderName" to user.name,
                "senderAvatar" to user.avatar,
                "senderRole" to user.role,
                "status" to "pending",
                "timestamp" to ServerValue.TIMESTAMP
            )
        )
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val user = _currentUser.value ?: return
        // Mark request accepted
        dbRef.child("friend_requests").child(myUid).child(request.fromUid).child("status").setValue("accepted")

        // Add to my friends
        dbRef.child("friends").child(myUid).child(request.fromUid).setValue(
            mapOf(
                "name" to request.senderName,
                "avatar" to request.senderAvatar,
                "addedAt" to ServerValue.TIMESTAMP
            )
        )

        // Add to sender's friends
        dbRef.child("friends").child(request.fromUid).child(myUid).setValue(
            mapOf(
                "name" to user.name,
                "avatar" to user.avatar,
                "addedAt" to ServerValue.TIMESTAMP
            )
        )
    }

    fun rejectFriendRequest(request: FriendRequest) {
        dbRef.child("friend_requests").child(myUid).child(request.fromUid).removeValue()
    }

    // -------------------------------------------------------------
    // Alerts and System Warnings
    // -------------------------------------------------------------
    private fun listenAlerts() {
        dbRef.child("chat_alerts").limitToLast(1).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val alert = child.getValue(ChatAlert::class.java)
                    if (alert != null) {
                        if (alert.target == "all" || alert.targetUid == myUid) {
                            _latestAlert.value = alert
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenWarnings() {
        dbRef.child("warnings").child("current").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    _activeWarning.value = null
                    return
                }
                val id = snapshot.child("id").getValue(String::class.java) ?: ""
                val msg = snapshot.child("message").getValue(String::class.java) ?: ""
                val sName = snapshot.child("senderName").getValue(String::class.java) ?: "المالك"
                val sUid = snapshot.child("senderUid").getValue(String::class.java) ?: ""
                val targetType = snapshot.child("target").child("type").getValue(String::class.java) ?: "all"
                val targetValue = snapshot.child("target").child("value").getValue(String::class.java)
                val ack = snapshot.child("ack").child(myUid).exists()

                if (!ack && (targetType == "all" || (targetType == "user" && targetValue == myUid))) {
                    _activeWarning.value = SystemWarning(
                        id = id,
                        message = msg,
                        senderName = sName,
                        senderUid = sUid,
                        targetType = targetType,
                        targetValue = targetValue
                    )
                } else {
                    _activeWarning.value = null
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun acknowledgeWarning(warningId: String) {
        val user = _currentUser.value ?: return
        dbRef.child("warnings").child("current").child("ack").child(myUid).setValue(
            mapOf(
                "name" to user.name,
                "avatar" to user.avatar,
                "time" to ServerValue.TIMESTAMP
            )
        )
        dbRef.child("warnings").child("history").child(warningId).child("ack").child(myUid).setValue(
            mapOf(
                "name" to user.name,
                "avatar" to user.avatar,
                "time" to ServerValue.TIMESTAMP
            )
        )
        _activeWarning.value = null
    }

    // -------------------------------------------------------------
    // Admin Actions
    // -------------------------------------------------------------
    fun banUserPermanent(targetUid: String, targetName: String) {
        val userObj = _allUsers.value.find { it.uid == targetUid }
        dbRef.child("bans").child(targetUid).setValue(true)
        userObj?.hardwareFingerprint?.let { hw ->
            if (hw.isNotEmpty()) dbRef.child("bans").child(hw).setValue(true)
        }
        dbRef.child("admin_logs").push().setValue(
            mapOf(
                "type" to "ban_permanent",
                "actorUid" to myUid,
                "actorName" to (_currentUser.value?.name ?: "المالك"),
                "targetUid" to targetUid,
                "targetName" to targetName,
                "timestamp" to ServerValue.TIMESTAMP
            )
        )
    }

    fun unbanUser(targetUid: String) {
        dbRef.child("bans").child(targetUid).removeValue()
    }

    fun muteUser(targetUid: String, minutes: Long) {
        val endTime = if (minutes >= 999999) 9999999999999L else System.currentTimeMillis() + (minutes * 60 * 1000L)
        dbRef.child("mutes").child(targetUid).setValue(
            mapOf(
                "endTime" to endTime,
                "by" to (_currentUser.value?.name ?: "المالك"),
                "at" to ServerValue.TIMESTAMP
            )
        )
    }

    fun changeUserRole(targetUid: String, newRole: String) {
        dbRef.child("users").child(targetUid).child("role").setValue(newRole)
    }

    fun clearAllChat() {
        dbRef.child("messages").removeValue()
    }

    fun sendBroadcastAlert(text: String, targetUid: String? = null) {
        val user = _currentUser.value ?: return
        val newRef = dbRef.child("chat_alerts").push()
        val data = mapOf(
            "id" to newRef.key.orEmpty(),
            "sender" to user.name,
            "senderUid" to myUid,
            "senderAvatar" to user.avatar,
            "senderRole" to user.role,
            "text" to text,
            "target" to (if (targetUid != null) "user" else "all"),
            "targetUid" to targetUid,
            "time" to ServerValue.TIMESTAMP
        )
        newRef.setValue(data)
    }
}
