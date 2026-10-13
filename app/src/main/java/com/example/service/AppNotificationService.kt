package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.security.DeviceSecurityManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class AppNotificationService : Service() {

    companion object {
        const val CHANNEL_FRIENDS = "vpro_channel_friends"
        const val CHANNEL_DM = "vpro_channel_dm"
        const val CHANNEL_ALERTS = "vpro_channel_alerts"
        const val CHANNEL_SERVICE = "vpro_channel_service"

        const val NOTIF_ID_FOREGROUND = 1001

        fun start(context: Context) {
            val intent = Intent(context, AppNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var myUid: String = ""

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        startForegroundServiceNotification()

        val fingerprint = DeviceSecurityManager.getFullDeviceFingerprint(applicationContext)
        myUid = fingerprint.primaryHardwareId

        initFirebaseAndListeners()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Friend Requests Channel
            val friendsChannel = NotificationChannel(
                CHANNEL_FRIENDS,
                "طلبات الصداقة (Friend Requests)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات طلبات الصداقة الجديدة حتى عند إغلاق التطبيق"
                enableVibration(true)
                enableLights(true)
                setSound(defaultSound, null)
            }

            // Private Messages Channel
            val dmChannel = NotificationChannel(
                CHANNEL_DM,
                "الرسائل الخاصة (Direct Messages)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات المحادثات والرسائل الخاصة الفورية"
                enableVibration(true)
                enableLights(true)
                setSound(defaultSound, null)
            }

            // Admin Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "التنبيهات والتحذيرات (System Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الإدارة والتحذيرات الرسمية العاجلة"
                enableVibration(true)
                enableLights(true)
                setSound(defaultSound, null)
            }

            // Ongoing Background Sync Channel (Low importance)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "خدمة المزامنة الفورية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "مزامنة الإشعارات والرسائل بالخلفية"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(friendsChannel)
            notificationManager.createNotificationChannel(dmChannel)
            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setContentTitle("فيسي برو متصل")
            .setContentText("نظام الإشعارات الفورية والمزامنة قيد التشغيل")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIF_ID_FOREGROUND, notification)
    }

    private fun initFirebaseAndListeners() {
        try {
            val existingApp = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
            if (existingApp == null) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyD9qPQHL_BBAeJ-fXtgoRr5rXPMetvN3W8")
                    .setApplicationId("1:870807919125:web:4426d8b27054e7f3428c1c")
                    .setDatabaseUrl("https://sory-1ab08-default-rtdb.firebaseio.com")
                    .setProjectId("sory-1ab08")
                    .setStorageBucket("sory-1ab08.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(applicationContext, options)
            }

            val database = FirebaseDatabase.getInstance("https://sory-1ab08-default-rtdb.firebaseio.com")
            val dbRef = database.reference

            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                101,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 1. Listen for Incoming Friend Requests
            dbRef.child("friend_requests").child(myUid).addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleFriendRequestSnapshot(snapshot, pendingIntent)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleFriendRequestSnapshot(snapshot, pendingIntent)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })

            // 2. Listen for Private DMs in Inbox
            dbRef.child("dm_inbox").child(myUid).addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleDmInboxSnapshot(snapshot, pendingIntent)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleDmInboxSnapshot(snapshot, pendingIntent)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })

            // 3. Listen for Urgent Broadcast Alerts
            dbRef.child("chat_alerts").limitToLast(1).addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val alertId = snapshot.child("id").getValue(String::class.java) ?: return
                    val sender = snapshot.child("sender").getValue(String::class.java) ?: "الإدارة"
                    val senderUid = snapshot.child("senderUid").getValue(String::class.java) ?: ""
                    val target = snapshot.child("target").getValue(String::class.java) ?: "all"
                    val targetUid = snapshot.child("targetUid").getValue(String::class.java)
                    val text = snapshot.child("text").getValue(String::class.java) ?: ""

                    if (senderUid == myUid) return
                    if (target == "user" && targetUid != myUid) return

                    val notif = NotificationCompat.Builder(this@AppNotificationService, CHANNEL_ALERTS)
                        .setContentTitle("🚨 تنبيه رسمي من $sender")
                        .setContentText(text)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setVibrate(longArrayOf(0, 300, 150, 300))
                        .build()

                    notificationManager.notify(alertId.hashCode(), notif)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleFriendRequestSnapshot(snapshot: DataSnapshot, pendingIntent: PendingIntent) {
        val status = snapshot.child("status").getValue(String::class.java) ?: "pending"
        val fromUid = snapshot.key ?: return
        val senderName = snapshot.child("senderName").getValue(String::class.java) ?: "مستخدم"

        if (status == "pending" && fromUid != myUid) {
            val notif = NotificationCompat.Builder(this, CHANNEL_FRIENDS)
                .setContentTitle("👤 طلب صداقة جديد!")
                .setContentText("أرسل لك $senderName طلب صداقة في فيسي برو")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .build()

            notificationManager.notify(fromUid.hashCode() + 2000, notif)
        }
    }

    private fun handleDmInboxSnapshot(snapshot: DataSnapshot, pendingIntent: PendingIntent) {
        val count = snapshot.child("count").getValue(Int::class.java) ?: 0
        val fromName = snapshot.child("fromName").getValue(String::class.java) ?: "مستخدم"
        val lastMessage = snapshot.child("lastMessage").getValue(String::class.java) ?: "رسالة جديدة"
        val senderUid = snapshot.key ?: return

        if (count > 0 && senderUid != myUid) {
            val notif = NotificationCompat.Builder(this, CHANNEL_DM)
                .setContentTitle("💌 رسالة خاصة من $fromName")
                .setContentText(lastMessage)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 200, 100, 200))
                .build()

            notificationManager.notify(senderUid.hashCode() + 3000, notif)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
