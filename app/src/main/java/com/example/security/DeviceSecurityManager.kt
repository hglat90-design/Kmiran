package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaDrm
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * High-Security Device Hardware Fingerprinting & Clone Detection Engine.
 * Generates immutable hardware-bound identifiers and detects Virtual Spaces, Dual Space,
 * Parallel Space, and other App Cloners to prevent ban evasion.
 */
object DeviceSecurityManager {

    private const val PREFS_NAME = "vpro_secure_hardware_vault"
    private const val KEY_PERMANENT_ID = "perm_device_hw_id"
    private const val KEY_INITIAL_INSTALL_TS = "first_hw_init_ts"

    data class CloneDetectionResult(
        val isCloned: Boolean,
        val detectionReason: String
    )

    data class DeviceFingerprint(
        val primaryHardwareId: String,
        val widevineId: String,
        val androidId: String,
        val buildFingerprintHash: String,
        val cloneDetection: CloneDetectionResult
    )

    /**
     * Obtains the DRM MediaDrm Widevine Unique Device ID.
     * This ID is crypto-bound to the hardware chip and cannot be altered or spoofed
     * by Virtual Spaces, Dual Space, App Cloners, or data wiping.
     */
    fun getWidevineHardwareId(): String {
        return try {
            val widevineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            val mediaDrm = MediaDrm(widevineUuid)
            val bytes = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mediaDrm.close()
            } else {
                @Suppress("DEPRECATION")
                mediaDrm.release()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE).trim()
        } catch (e: Exception) {
            "wv_unavailable_${Build.HARDWARE}_${Build.BOARD}"
        }
    }

    /**
     * Reads the Android ID from Settings.Secure.
     */
    fun getAndroidId(context: Context): String {
        return try {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            id ?: "null_android_id"
        } catch (e: Exception) {
            "err_android_id"
        }
    }

    /**
     * Computes a SHA-256 hash of immutable hardware build properties.
     */
    fun getBuildHardwareHash(): String {
        val raw = buildString {
            append(Build.BOARD).append("|")
            append(Build.BOOTLOADER).append("|")
            append(Build.BRAND).append("|")
            append(Build.DEVICE).append("|")
            append(Build.HARDWARE).append("|")
            append(Build.MANUFACTURER).append("|")
            append(Build.MODEL).append("|")
            append(Build.PRODUCT).append("|")
            append(Build.FINGERPRINT)
        }
        return sha256(raw)
    }

    /**
     * Deep detection of Cloning Environments, Virtual Spaces, and Sandboxes.
     * Detects Dual Space, Parallel Space, Clone App, VirtualXposed, Multiple Accounts, etc.
     */
    fun detectCloningEnvironment(context: Context): CloneDetectionResult {
        // 1. Check data directories path anomalies
        val filesPath = context.filesDir.absolutePath
        val dataPath = context.applicationInfo.dataDir ?: ""
        val cloneKeywords = listOf(
            "com.lbe.parallel", "parallel", "dual", "clone", "virtual",
            "virtualxposed", "multiple", "double", "sandbox", "com.excelliance",
            "io.va.exposed", "com.morgoo.droidplugin", "io.virtualapp"
        )

        for (kw in cloneKeywords) {
            if (filesPath.contains(kw, ignoreCase = true) || dataPath.contains(kw, ignoreCase = true)) {
                return CloneDetectionResult(
                    isCloned = true,
                    detectionReason = "تم اكتشاف مسار ناسخ تطبيقات مشبوه ($kw)"
                )
            }
        }

        // 2. Check Multi-user User ID anomaly (most cloners use Android multi-user profiles e.g. 999)
        if (dataPath.contains("/user/999/") || dataPath.contains("/user/10/") || filesPath.contains("/999/")) {
            return CloneDetectionResult(
                isCloned = true,
                detectionReason = "تم اكتشاف ملف مستخدم فرعي لبرنامج ناسخ (User Profile Isolation)"
            )
        }

        // 3. Check for specific cloner package directory traces in /data/data/
        val suspiciousDirs = listOf(
            "/data/data/com.lbe.parallel",
            "/data/data/com.excelliance.dualaid",
            "/data/data/com.polestar.super.clone",
            "/data/data/com.applisto.appcloner",
            "/data/data/com.dualspace.multispace"
        )
        for (dir in suspiciousDirs) {
            if (File(dir).exists()) {
                return CloneDetectionResult(
                    isCloned = true,
                    detectionReason = "تم اكتشاف وجود برنامج ناسخ نشط على الجهاز"
                )
            }
        }

        // 4. Check class loader traces of known cloning frameworks
        val suspiciousClasses = listOf(
            "com.lbe.doubleagent.service.ProxyService",
            "com.excelliance.kx.hook.Hook",
            "io.va.exposed.Hook",
            "com.morgoo.droidplugin.PluginManager"
        )
        for (clazz in suspiciousClasses) {
            try {
                Class.forName(clazz)
                return CloneDetectionResult(
                    isCloned = true,
                    detectionReason = "تم رصد مكتبات محاكاة افتراضية (Virtual Hook)"
                )
            } catch (_: ClassNotFoundException) {
            }
        }

        return CloneDetectionResult(isCloned = false, detectionReason = "")
    }

    /**
     * Generates a composite, unforgeable hardware device fingerprint.
     */
    fun getFullDeviceFingerprint(context: Context): DeviceFingerprint {
        val widevineId = getWidevineHardwareId()
        val androidId = getAndroidId(context)
        val buildHash = getBuildHardwareHash()
        val cloneResult = detectCloningEnvironment(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var persistentId = prefs.getString(KEY_PERMANENT_ID, null)

        if (persistentId.isNullOrEmpty()) {
            persistentId = "hw_" + sha256("$widevineId-$androidId-$buildHash").substring(0, 24)
            prefs.edit()
                .putString(KEY_PERMANENT_ID, persistentId)
                .putLong(KEY_INITIAL_INSTALL_TS, System.currentTimeMillis())
                .apply()
        }

        return DeviceFingerprint(
            primaryHardwareId = persistentId,
            widevineId = widevineId,
            androidId = androidId,
            buildFingerprintHash = buildHash,
            cloneDetection = cloneResult
        )
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
