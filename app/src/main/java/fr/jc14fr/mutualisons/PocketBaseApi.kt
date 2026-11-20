package fr.jc14fr.mutualisons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthExpiredException : IOException("session expirée")

object PocketBaseApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    data class Item(
        val id: String,
        val title: String,
        val category: String,
        val status: String,
        val owner: String = "",
        val circles: List<String> = emptyList()
    )

    data class Circle(val id: String, val name: String)

    private fun messageOf(text: String, code: Int): String {
        return try {
            val json = JSONObject(text)
            val data = json.optJSONObject("data")
            var detail: String? = null
            if (data != null) {
                val k = data.keys().asSequence().firstOrNull()
                if (k != null) detail = data.getJSONObject(k).optString("message")
            }
            if (!detail.isNullOrBlank()) detail
            else json.optString("message").ifBlank { "erreur $code" }
        } catch (e: Exception) {
            "erreur $code"
        }
    }

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
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                JSONObject(text).getString("token")
            }
        }

    suspend fun signup(baseUrl: String, email: String, name: String, password: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("email", email.trim())
                .put("name", name.trim())
                .put("password", password)
                .put("passwordConfirm", password)
                .toString()
                .toRequestBody(JSON_TYPE)

            val request = Request.Builder()
                .url("$baseUrl/api/collections/users/records")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun listItems(baseUrl: String, token: String): List<Item> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/items/records?perPage=100&sort=title&fields=id,title,category,status,owner,circles")
                .header("Authorization", token)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException("erreur ${response.code}")
                val arr = JSONObject(text).getJSONArray("items")
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val ca = o.optJSONArray("circles")
                    Item(
                        id = o.getString("id"),
                        title = o.optString("title"),
                        category = o.optString("category"),
                        status = o.optString("status"),
                        owner = o.optString("owner"),
                        circles = if (ca == null) emptyList() else List(ca.length()) { ca.getString(it) }
                    )
                }
            }
        }

    suspend fun listCircles(baseUrl: String, token: String): List<Circle> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/circles/records?perPage=100&sort=name&fields=id,name")
                .header("Authorization", token)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException("erreur ${response.code}")
                val arr = JSONObject(text).getJSONArray("items")
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Circle(id = o.getString("id"), name = o.optString("name"))
                }
            }
        }

    suspend fun leaveCircle(baseUrl: String, token: String, circleId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("circle", circleId).toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/mutualisons/leave")
                .header("Authorization", token)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun joinCircle(baseUrl: String, token: String, code: String): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("code", code.trim().uppercase()).toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/mutualisons/join")
                .header("Authorization", token)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                JSONObject(text).optString("name")
            }
        }

    suspend fun currentUserId(baseUrl: String, token: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/users/auth-refresh")
                .header("Authorization", token)
                .post("".toRequestBody(JSON_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                JSONObject(text).getJSONObject("record").getString("id")
            }
        }

    suspend fun createInvitation(baseUrl: String, token: String, circleId: String, userId: String): String =
        withContext(Dispatchers.IO) {
            val code = (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
            val body = JSONObject()
                .put("circle", circleId)
                .put("code", code)
                .put("created_by", userId)
                .put("max_uses", 5)
                .put("uses", 0)
                .toString()
                .toRequestBody(JSON_TYPE)

            val request = Request.Builder()
                .url("$baseUrl/api/collections/invitations/records")
                .header("Authorization", token)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                JSONObject(text).getString("code")
            }
        }

    suspend fun createItem(
        baseUrl: String,
        token: String,
        title: String,
        category: String,
        circleIds: List<String>,
        userId: String
    ): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("owner", userId)
                .put("circles", JSONArray(circleIds))
                .put("title", title)
                .put("category", category)
                .put("status", "available")
                .toString()
                .toRequestBody(JSON_TYPE)

            val request = Request.Builder()
                .url("$baseUrl/api/collections/items/records")
                .header("Authorization", token)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                JSONObject(text).getString("id")
            }
        }

    suspend fun updateItemCircles(baseUrl: String, token: String, itemId: String, circleIds: List<String>) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("circles", JSONArray(circleIds)).toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/collections/items/records/$itemId")
                .header("Authorization", token)
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun deleteItem(baseUrl: String, token: String, itemId: String) =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/items/records/$itemId")
                .header("Authorization", token)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }
            suspend fun createCircle(baseUrl: String, token: String, name: String): String =
        withContext(Dispatchers.IO) {
            // Pas de champ members : le serveur y met le créateur automatiquement
            val body = JSONObject()
                .put("name", name.trim())
                .toString()
                .toRequestBody(JSON_TYPE)

            val request = Request.Builder()
                .url("$baseUrl/api/collections/circles/records")
                .header("Authorization", token)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                JSONObject(text).getString("name")
            }
        }
}