package fr.jc14fr.mutualisons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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

    // ---------- Modèles ----------

    data class Item(
        val id: String,
        val title: String,
        val category: String,
        val status: String,
        val owner: String = "",
        val ownerName: String = "",
        val circles: List<String> = emptyList()
    )

    data class Circle(val id: String, val name: String, val creatorName: String = "")
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

    // ---------- Appel HTTP générique ----------

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

    /**
     * Appel HTTP unique : renvoie le JSON de la réponse, ou null si elle est vide.
     * Lève AuthExpiredException sur 401, IOException avec le message serveur sinon.
     */
    private suspend fun call(
        baseUrl: String,
        method: String,
        path: String,
        token: String? = null,
        body: JSONObject? = null,
        query: String? = null
    ): JSONObject? = withContext(Dispatchers.IO) {
        val url = if (query == null) "$baseUrl$path" else "$baseUrl$path?$query"
        val reqBody: RequestBody? = when {
            body != null -> body.toString().toRequestBody(JSON_TYPE)
            method == "POST" || method == "PATCH" -> "".toRequestBody(JSON_TYPE)
            else -> null
        }
        val builder = Request.Builder().url(url)
        if (token != null) builder.header("Authorization", token)
        builder.method(method, reqBody)
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 401) throw AuthExpiredException()
            if (!response.isSuccessful) throw IOException(messageOf(text, response.code))
            if (text.isBlank()) null else JSONObject(text)
        }
    }

    // ---------- Compte ----------

    suspend fun login(baseUrl: String, identity: String, password: String): String =
        call(
            baseUrl, "POST", "/api/collections/users/auth-with-password",
            body = JSONObject().put("identity", identity).put("password", password)
        )!!.getString("token")

    suspend fun signup(baseUrl: String, email: String, name: String, password: String) {
        call(
            baseUrl, "POST", "/api/collections/users/records",
            body = JSONObject()
                .put("email", email.trim())
                .put("name", name.trim())
                .put("password", password)
                .put("passwordConfirm", password)
        )
    }

    suspend fun currentUser(baseUrl: String, token: String): Me {
        val r = call(baseUrl, "POST", "/api/collections/users/auth-refresh", token = token)!!
            .getJSONObject("record")
        return Me(r.getString("id"), r.optString("name"), r.optString("email"))
    }

    suspend fun currentUserId(baseUrl: String, token: String): String =
        currentUser(baseUrl, token).id

    suspend fun updateProfile(baseUrl: String, token: String, userId: String, name: String, email: String) {
        call(
            baseUrl, "PATCH", "/api/collections/users/records/$userId", token = token,
            body = JSONObject().put("name", name.trim()).put("email", email.trim())
        )
    }

    // ---------- Objets ----------

    suspend fun listItems(baseUrl: String, token: String): List<Item> {
        val arr = call(
            baseUrl, "GET", "/api/collections/items/records", token = token,
            query = "perPage=100&sort=title&expand=owner&fields=id,title,category,status,owner,circles,expand"
        )!!.getJSONArray("items")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val ca = o.optJSONArray("circles")
            Item(
                id = o.getString("id"),
                title = o.optString("title"),
                category = o.optString("category"),
                status = o.optString("status"),
                owner = o.optString("owner"),
                ownerName = o.optJSONObject("expand")?.optJSONObject("owner")?.optString("name").orEmpty(),
                circles = if (ca == null) emptyList() else List(ca.length()) { ca.getString(it) }
            )
        }
    }

    suspend fun createItem(
        baseUrl: String, token: String, title: String, category: String,
        circleIds: List<String>, userId: String
    ): String = call(
        baseUrl, "POST", "/api/collections/items/records", token = token,
        body = JSONObject()
            .put("owner", userId)
            .put("circles", JSONArray(circleIds))
            .put("title", title)
            .put("category", category)
            .put("status", "available")
    )!!.getString("id")

    suspend fun updateItemCircles(baseUrl: String, token: String, itemId: String, circleIds: List<String>) {
        call(
            baseUrl, "PATCH", "/api/collections/items/records/$itemId", token = token,
            body = JSONObject().put("circles", JSONArray(circleIds))
        )
    }

    suspend fun setItemStatus(baseUrl: String, token: String, itemId: String, status: String) {
        call(
            baseUrl, "PATCH", "/api/collections/items/records/$itemId", token = token,
            body = JSONObject().put("status", status)
        )
    }

    suspend fun deleteItem(baseUrl: String, token: String, itemId: String) {
        call(baseUrl, "DELETE", "/api/collections/items/records/$itemId", token = token)
    }

    /** État réel des objets (available, reserved, lent, repair, hidden), calculé par le serveur. */
    suspend fun itemStates(baseUrl: String, token: String, ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val json = call(
            baseUrl, "GET", "/api/mutualisons/item-state", token = token,
            query = "ids=${ids.joinToString(",")}"
        )!!
        return json.keys().asSequence().associateWith { json.getJSONObject(it).getString("state") }
    }

    // ---------- Prêts ----------

    /** Prêt en cours ou à venir d'un objet (y compris en retard), ou null. */
    suspend fun activeLoanInfo(baseUrl: String, token: String, itemId: String): ActiveLoan? {
        val filter = URLEncoder.encode("item=\"$itemId\" && returned_at=\"\"", "UTF-8")
        val arr = call(
            baseUrl, "GET", "/api/collections/loans/records", token = token,
            query = "perPage=1&sort=start_date&expand=borrower,lender&filter=$filter"
        )!!.getJSONArray("items")
        if (arr.length() == 0) return null
        val o = arr.getJSONObject(0)
        val expand = o.optJSONObject("expand")
        val start = o.optString("start_date")
        return ActiveLoan(
            id = o.getString("id"),
            borrowerId = o.optString("borrower"),
            lenderId = o.optString("lender"),
            borrowerName = expand?.optJSONObject("borrower")?.optString("name").orEmpty().ifBlank { "un membre" },
            lenderName = expand?.optJSONObject("lender")?.optString("name").orEmpty().ifBlank { "un membre" },
            startDate = start,
            dueDate = o.optString("due_date"),
            started = DateUtils.isPast(start)
        )
    }

    suspend fun markReturned(baseUrl: String, token: String, loanId: String) {
        call(
            baseUrl, "POST", "/api/mutualisons/return", token = token,
            body = JSONObject().put("loan", loanId)
        )
    }

    suspend fun reopenLoan(baseUrl: String, token: String, loanId: String) {
        call(
            baseUrl, "POST", "/api/mutualisons/reopen", token = token,
            body = JSONObject().put("loan", loanId)
        )
    }

    // ---------- Demandes de prêt ----------

    suspend fun createRequest(baseUrl: String, token: String, itemId: String, startUtc: String, endUtc: String) {
        call(
            baseUrl, "POST", "/api/collections/loan_requests/records", token = token,
            body = JSONObject().put("item", itemId).put("start_date", startUtc).put("due_date", endUtc)
        )
    }

    suspend fun listRequests(baseUrl: String, token: String, filter: String): List<Req> {
        val arr = call(
            baseUrl, "GET", "/api/collections/loan_requests/records", token = token,
            query = "perPage=100&sort=-created&expand=item,requester&filter=" +
                URLEncoder.encode(filter, "UTF-8")
        )!!.getJSONArray("items")
        return List(arr.length()) { i ->
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

    suspend fun respondRequest(baseUrl: String, token: String, requestId: String, status: String) {
        call(
            baseUrl, "PATCH", "/api/collections/loan_requests/records/$requestId", token = token,
            body = JSONObject().put("status", status)
        )
    }

    suspend fun cancelRequest(baseUrl: String, token: String, requestId: String) {
        call(baseUrl, "DELETE", "/api/collections/loan_requests/records/$requestId", token = token)
    }

    // ---------- Cercles ----------

    suspend fun listCircles(baseUrl: String, token: String): List<Circle> {
        val arr = call(
            baseUrl, "GET", "/api/collections/circles/records", token = token,
            query = "perPage=100&sort=name&expand=creator&fields=id,name,creator,expand"
        )!!.getJSONArray("items")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Circle(
                id = o.getString("id"),
                name = o.optString("name"),
                creatorName = o.optJSONObject("expand")?.optJSONObject("creator")?.optString("name").orEmpty()
            )
        }
    }

    suspend fun listMembers(baseUrl: String, token: String, circleId: String): List<Member> {
        val arr = call(
            baseUrl, "GET", "/api/collections/circles/records/$circleId", token = token,
            query = "expand=members&fields=id,members,expand"
        )!!.optJSONObject("expand")?.optJSONArray("members") ?: JSONArray()
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Member(o.getString("id"), o.optString("name").ifBlank { "Membre" })
        }
    }

    suspend fun createCircle(baseUrl: String, token: String, name: String): String =
        call(
            baseUrl, "POST", "/api/collections/circles/records", token = token,
            body = JSONObject().put("name", name.trim())
        )!!.getString("name")

    suspend fun leaveCircle(baseUrl: String, token: String, circleId: String) {
        call(
            baseUrl, "POST", "/api/mutualisons/leave", token = token,
            body = JSONObject().put("circle", circleId)
        )
    }

    suspend fun joinCircle(baseUrl: String, token: String, code: String): String =
        call(
            baseUrl, "POST", "/api/mutualisons/join", token = token,
            body = JSONObject().put("code", code.trim().uppercase())
        )!!.optString("name")

    suspend fun createInvitation(baseUrl: String, token: String, circleId: String, userId: String): String {
        val code = (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        call(
            baseUrl, "POST", "/api/collections/invitations/records", token = token,
            body = JSONObject()
                .put("circle", circleId)
                .put("code", code)
                .put("created_by", userId)
                .put("max_uses", 5)
                .put("uses", 0)
        )
        return code
    }
}