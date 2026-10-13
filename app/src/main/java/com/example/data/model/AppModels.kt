package com.example.data.model

data class UserData(
    val uid: String = "",
    val name: String = "",
    val avatar: String = "",
    val role: String = "user",
    val status: String = "offline",
    val lastSeen: Long = 0L,
    val verifiedPhone: String? = null,
    val createdAt: Long = 0L,
    val hidden: Boolean = false,
    val isBanned: Boolean = false,
    val isMuted: Boolean = false,
    val muteEndTime: Long = 0L,
    val hardwareFingerprint: String = ""
)

data class ReplyToData(
    val key: String = "",
    val sender: String = "",
    val text: String = "",
    val role: String = "user"
)

data class ChatMessage(
    val id: String = "",
    val uid: String = "",
    val sender: String = "",
    val text: String = "",
    val media: String? = null,
    val type: String = "text", // "text", "image", "video", "youtube"
    val youtubeId: String? = null,
    val role: String = "user",
    val avatar: String = "",
    val time: Long = 0L,
    val replyTo: ReplyToData? = null
)

data class StatusStory(
    val id: String = "",
    val uid: String = "",
    val sender: String = "",
    val avatar: String = "",
    val text: String = "",
    val media: String? = null,
    val type: String = "text", // "text", "image", "video"
    val time: Long = 0L,
    val seenBy: Map<String, StatusViewInfo> = emptyMap()
)

data class StatusViewInfo(
    val name: String = "",
    val avatar: String = "",
    val time: Long = 0L
)

data class DirectMessage(
    val id: String = "",
    val from: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    val to: String = "",
    val toName: String = "",
    val text: String = "",
    val media: String? = null,
    val type: String = "text", // "text", "image", "video"
    val time: Long = 0L,
    val read: Boolean = false,
    val replyTo: ReplyToData? = null
)

data class DirectConversation(
    val otherUid: String = "",
    val otherName: String = "",
    val otherAvatar: String = "",
    val otherRole: String = "user",
    val lastMessage: String = "",
    val lastTime: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val senderRole: String = "user",
    val status: String = "pending", // "pending", "accepted", "rejected"
    val timestamp: Long = 0L
)

data class FriendUser(
    val uid: String = "",
    val name: String = "",
    val avatar: String = "",
    val role: String = "user",
    val status: String = "offline",
    val lastSeen: Long = 0L,
    val addedAt: Long = 0L
)

data class ChatAlert(
    val id: String = "",
    val sender: String = "",
    val senderUid: String = "",
    val senderAvatar: String = "",
    val senderRole: String = "owner",
    val text: String = "",
    val target: String = "all", // "all", "user"
    val targetUid: String? = null,
    val targetName: String? = null,
    val time: Long = 0L
)

data class SystemWarning(
    val id: String = "",
    val message: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val senderRole: String = "owner",
    val targetType: String = "all", // "all", "role", "user", "new_users"
    val targetValue: String? = null,
    val time: Long = 0L,
    val ack: Map<String, Any> = emptyMap()
)

data class ReportItem(
    val key: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val reportType: String = "message", // "message", "status"
    val reason: String = "",
    val messageKey: String? = null,
    val statusKey: String? = null,
    val status: String = "pending", // "pending", "resolved", "rejected"
    val createdAt: Long = 0L
)
