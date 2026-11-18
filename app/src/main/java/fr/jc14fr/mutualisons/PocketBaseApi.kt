package fr.jc14fr.mutualisons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object PocketBaseApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun login(baseUrl: String, identity: String, password: String): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("identity", identity)
                .put("password", password)
                .toString()
                .toRequestBody(JSON_TYPE)

            val request = Request.Builder()
                .url("$baseUrl/api/collections/users/auth-with-password")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = try {
                        JSONObject(text).optString("message")
                    } catch (e: Exception) {
                        ""
                    }
                    throw IOException(msg.ifBlank { "erreur ${response.code}" })
                }
                JSONObject(text).getString("token")
            }
        }
}