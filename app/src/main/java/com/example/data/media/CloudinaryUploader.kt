package com.example.data.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object CloudinaryUploader {

    private const val CLOUD_NAME = "nfg03aku"
    private const val UPLOAD_PRESET = "daufafvn"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        onProgress: ((Float) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext null

            val buffer = ByteArrayOutputStream()
            val data = ByteArray(16384)
            var nRead: Int
            while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            buffer.flush()
            val bytes = buffer.toByteArray()
            inputStream.close()

            val mimeType = contentResolver.getType(uri) ?: if (isVideo) "video/mp4" else "image/jpeg"
            val endpointType = if (isVideo) "video" else "image"
            val uploadUrl = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$endpointType/upload"

            val mediaType = mimeType.toMediaTypeOrNull()
            val filePart = bytes.toRequestBody(mediaType)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("file", "upload_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}", filePart)
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                json.optString("secure_url", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
