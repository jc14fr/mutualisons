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
import java.net.URLEncoder
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
        val ownerName: String = "",
        val circles: List<String> = emptyList()
    )

    data class Circle(
        val id: String,
        val name: String,
        val creatorName: String = ""
    )

    data class Member(val id: String, val name: String)

    data class Me(val id: String, val name: String, val email: String)

    data class ActiveLoan(
        val id: String,
        val borrowerId: String,
        val lenderId: String,
        val borrowerName: String,
        val lenderName: String,
        val startDate: String,
        val dueDate: String,
        val started: Boolean
    )

    data class Req(
        val id: String,
        val itemTitle: String,
        val requesterId: String,
        val requesterName: String,
        val ownerId: String,
        val status: String
    )

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

    suspend fun currentUser(baseUrl: String, token: String): Me =
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
                val r = JSONObject(text).getJSONObject("record")
                Me(r.getString("id"), r.optString("name"), r.optString("email"))
            }
        }

    suspend fun updateProfile(baseUrl: String, token: String, userId: String, name: String, email: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("name", name.trim())
                .put("email", email.trim())
                .toString()
                .toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/collections/users/records/$userId")
                .header("Authorization", token)
                .patch(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun listItems(baseUrl: String, token: String): List<Item> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/items/records?perPage=100&sort=title&expand=owner&fields=id,title,category,status,owner,circles,expand")
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
                    val ownerName = o.optJSONObject("expand")?.optJSONObject("owner")?.optString("name").orEmpty()
                    Item(
                        id = o.getString("id"),
                        title = o.optString("title"),
                        category = o.optString("category"),
                        status = o.optString("status"),
                        owner = o.optString("owner"),
                        ownerName = ownerName,
                        circles = if (ca == null) emptyList() else List(ca.length()) { ca.getString(it) }
                    )
                }
            }
        }

    /** État réel des objets (available, reserved, lent, repair, hidden), calculé par le serveur. */
    suspend fun itemStates(baseUrl: String, token: String, ids: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext emptyMap()
            val request = Request.Builder()
                .url("$baseUrl/api/mutualisons/item-state?ids=${ids.joinToString(",")}")
                .header("Authorization", token)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                val json = JSONObject(text)
                json.keys().asSequence().associateWith { json.getJSONObject(it).getString("state") }
            }
        }

    suspend fun listCircles(baseUrl: String, token: String): List<Circle> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/circles/records?perPage=100&sort=name&expand=creator&fields=id,name,creator,expand")
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
                    val creator = o.optJSONObject("expand")?.optJSONObject("creator")?.optString("name").orEmpty()
                    Circle(id = o.getString("id"), name = o.optString("name"), creatorName = creator)
                }
            }
        }

    suspend fun listMembers(baseUrl: String, token: String, circleId: String): List<Member> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/circles/records/$circleId?expand=members&fields=id,members,expand")
                .header("Authorization", token)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                val arr = JSONObject(text).optJSONObject("expand")?.optJSONArray("members") ?: JSONArray()
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Member(id = o.getString("id"), name = o.optString("name").ifBlank { "Membre" })
                }
            }
        }

    suspend fun createCircle(baseUrl: String, token: String, name: String): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("name", name.trim()).toString().toRequestBody(JSON_TYPE)
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

    /** Prêt en cours ou à venir d'un objet (y compris en retard), ou null. */
    suspend fun activeLoanInfo(baseUrl: String, token: String, itemId: String): ActiveLoan? =
        withContext(Dispatchers.IO) {
            val filter = URLEncoder.encode("item=\"$itemId\" && returned_at=\"\"", "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/api/collections/loans/records?perPage=1&sort=start_date&expand=borrower,lender&filter=$filter")
                .header("Authorization", token)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                val arr = JSONObject(text).getJSONArray("items")
                if (arr.length() == 0) return@use null
                val o = arr.getJSONObject(0)
                val expand = o.optJSONObject("expand")
                val borrower = expand?.optJSONObject("borrower")
                val lender = expand?.optJSONObject("lender")
                val start = o.optString("start_date")
                ActiveLoan(
                    id = o.getString("id"),
                    borrowerId = o.optString("borrower"),
                    lenderId = o.optString("lender"),
                    borrowerName = borrower?.optString("name").orEmpty().ifBlank { "un membre" },
                    lenderName = lender?.optString("name").orEmpty().ifBlank { "un membre" },
                    startDate = start,
                    dueDate = o.optString("due_date"),
                    started = DateUtils.isPast(start)
                )
            }
        }

    suspend fun markReturned(baseUrl: String, token: String, loanId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("loan", loanId).toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/mutualisons/return")
                .header("Authorization", token)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun reopenLoan(baseUrl: String, token: String, loanId: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("loan", loanId).toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/mutualisons/reopen")
                .header("Authorization", token)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun createRequest(
        baseUrl: String,
        token: String,
        itemId: String,
        startUtc: String,
        endUtc: String
    ) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("item", itemId)
                .put("start_date", startUtc)
                .put("due_date", endUtc)
                .toString()
                .toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/collections/loan_requests/records")
                .header("Authorization", token)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun listRequests(baseUrl: String, token: String, filter: String): List<Req> =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/api/collections/loan_requests/records?perPage=100&sort=-created&expand=item,requester&filter=" +
                URLEncoder.encode(filter, "UTF-8")
            val request = Request.Builder().url(url).header("Authorization", token).get().build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
                val arr = JSONObject(text).getJSONArray("items")
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    val exp = o.optJSONObject("expand")
                    Req(
                        id = o.getString("id"),
                        itemTitle = exp?.optJSONObject("item")?.optString("title").orEmpty(),
                        requesterId = o.optString("requester"),
                        requesterName = exp?.optJSONObject("requester")?.optString("name").orEmpty(),
                        ownerId = o.optString("owner"),
                        status = o.optString("status")
                    )
                }
            }
        }

    suspend fun respondRequest(baseUrl: String, token: String, requestId: String, status: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("status", status).toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url("$baseUrl/api/collections/loan_requests/records/$requestId")
                .header("Authorization", token)
                .patch(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }

    suspend fun cancelRequest(baseUrl: String, token: String, requestId: String) =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/collections/loan_requests/records/$requestId")
                .header("Authorization", token)
                .delete()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.code == 401) throw AuthExpiredException()
                if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            }
        }
}