package com.ttkk0000.meowcircle.client

import com.ttkk0000.meowcircle.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/** Platform implementations must use Keystore / Keychain, never ordinary preference plaintext. */
interface MobileSecretStorage {
    @Throws(Exception::class)
    fun read(): String?
    @Throws(Exception::class)
    fun write(value: String?)
}

class MobileApiException(val status: Int, message: String, val code: String = "", val requestId: String = "") : Exception(message)
class MobileStorageException : Exception("Secure storage unavailable")

fun Throwable.mobileMessage(): String = when (this) {
	 is OperationResultUnknown -> "提交结果待核对，输入已保留；请先核对原操作，勿重复提交"
    is MobileStorageException -> "设备安全存储暂不可用，请解锁设备后重试"
    is LocalDraftException -> reason
    is MobileApiException -> when (code) {
        "phone_verification_required" -> "请先验证当前绑定手机号，已保留输入"
        "age_verification_required" -> "请先完成年龄资格核验，已保留输入"
        "guardian_consent_required" -> "需要有效的监护授权，已保留输入"
        "adult_required" -> "商业发布和线下签约需要成年人使用本人账号办理"
        "purpose_grant_required", "purpose_grant_invalid" -> "请重新核对本次操作并完成用途验证"
        "idempotency_conflict" -> "此请求编号对应的内容已改变，请核对原操作结果"
        "publication_declaration_required" -> "内容或照片已改变，请重新确认来源声明"
        "effective_contract_required", "contract_change_pending" -> "请先完成双方对同一版本条款的确认"
        "city_rule_unavailable", "city_action_unavailable" -> "当前城市或物种的业务规则暂未开放"
        else -> when (status) {
        400 -> "请检查填写内容后再试"
        401 -> "登录已过期，请重新登录"
        403 -> "当前账号没有此操作权限"
        404 -> "内容已撤回或你已没有访问权限"
        409 -> "内容已更新，请重新载入并确认后再操作"
        429 -> "操作较频繁，请稍后再试"
        503 -> "此服务暂未配置或暂时不可用"
        else -> "服务暂时不可用，请稍后重试"
        }
    }
    is SerializationException -> "服务响应与客户端不一致，请稍后重试"
    else -> "暂时无法连接，请检查网络后重试"
}

private fun mobileJson() = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

class MobileApi internal constructor(baseUrl: String, private val secrets: MobileSecretStorage, private val device: String,
    private val http: HttpClient) {
    constructor(baseUrl: String, secrets: MobileSecretStorage, device: String) :
        this(baseUrl, secrets, device, createPlatformHttpClient(mobileJson()))
    private val root = baseUrl.trim().trimEnd('/')
    val storageScope: String get() = root
    internal val json = mobileJson()
    private val sessionMutex = Mutex()
    private var storageUnavailable = false
    private var vault: ClientVault = try { readVault(secrets.read()) }
        catch (_: Exception) { storageUnavailable = true; ClientVault() }
    private var session: LoginSession? = vault.session
    // 缓存身份只用于恢复私有输入；交流资格仍须重新从服务端读取。
    private val _user = MutableStateFlow<MobileUser?>(session?.user)
    val user = _user.asStateFlow()
    private var revision = 0L
    private var accountGeneration = 0L

    init { require(root.startsWith("https://") || root.startsWith("http://")) { "Invalid API URL" } }

    private fun readVault(raw: String?): ClientVault {
        if (raw == null) return ClientVault()
        val obj = json.parseToJsonElement(raw).jsonObject
        return if ("schema" in obj) json.decodeFromString<ClientVault>(raw).also { require(it.schema == 1) }
            else ClientVault(session = json.decodeFromString<LoginSession>(raw))
    }

    private fun persist(next: ClientVault) {
        try { secrets.write(json.encodeToString(next)) }
        catch (_: Exception) { throw MobileStorageException() }
        vault = next
    }

    private fun save(value: LoginSession?, refresh: Boolean = false) {
        persist(vault.copy(session = value))
        if (!refresh) accountGeneration++
        session = value
        revision++
        _user.value = value?.user
    }

    suspend fun restore(): MobileProfile? {
        ensureStorage()
        if (session == null) return null
        return try {
            profile().also { _user.value = it.user }
        } catch (e: MobileApiException) {
            if (e.status == 401) null else throw e
        }
    }

    private fun ensureStorage() {
        if (!storageUnavailable) return
        try {
            vault = readVault(secrets.read())
            session = vault.session
            storageUnavailable = false
        } catch (_: Exception) { throw MobileStorageException() }
    }

    private suspend fun response(path: String, method: HttpMethod, token: String?, body: Any?, query: Map<String, String>, extraHeaders: Map<String, String> = emptyMap()): HttpResponse =
        http.request("$root/api/v2/$path") {
            this.method = method
            header(HttpHeaders.Accept, ContentType.Application.Json)
            if (token != null) bearerAuth(token)
            extraHeaders.forEach { (name, value) -> header(name, value) }
            url { query.forEach { (key, value) -> parameters.append(key, value) } }
            if (body != null) {
                contentType(if (body is MultiPartFormDataContent) body.contentType else ContentType.Application.Json)
                setBody(body)
            }
        }

    private suspend fun checked(path: String, method: HttpMethod, body: Any?, query: Map<String, String>, authenticated: Boolean,
        extraHeaders: Map<String, String> = emptyMap()): HttpResponse {
        val epoch = revision
        val original = if (authenticated) session else null
        var usedToken = original?.token
        var result = response(path, method, original?.token, body, query, extraHeaders)
        if (result.status.value == 401 && original != null) {
            result.bodyAsText() // release the first response before another request
            val fresh = sessionMutex.withLock {
                if (revision != epoch) {
                    session ?: throw MobileApiException(401, "session changed")
                } else {
                    val refresh = response("auth/refresh", HttpMethod.Post, null,
                        json.encodeToString(RefreshInput(original.token, original.refreshToken, device)), emptyMap())
                    try {
                        decode<LoginSession>(refresh).also { save(it,refresh = true) }
                    } catch (e: MobileApiException) {
                        if (e.status == 401) save(null)
                        throw e
                    }
                }
            }
            // A logout or account switch must not replay an old account's command.
            if (fresh.user.id != original.user.id) throw MobileApiException(401, "account changed")
            result = response(path, method, fresh.token, body, query, extraHeaders)
            usedToken = fresh.token
        }
        if (authenticated && original != null && session?.user?.id != original.user.id) {
            result.bodyAsText()
            throw MobileApiException(401, "account changed")
        }
        if (result.status.value == 401 && original != null) sessionMutex.withLock {
            if (session?.token == usedToken) save(null)
        }
        return result
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        val value = json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (!response.status.isSuccess() || value["ok"]?.jsonPrimitive?.booleanOrNull != true) {
            throw MobileApiException(response.status.value, value["error"]?.jsonPrimitive?.contentOrNull ?: "request failed",
                value["error_code"]?.jsonPrimitive?.contentOrNull ?: "", value["request_id"]?.jsonPrimitive?.contentOrNull ?: "")
        }
        return json.decodeFromJsonElement(value["data"] ?: JsonNull)
    }

    internal suspend inline fun <reified T> get(path: String, query: Map<String, String> = emptyMap(), authenticated: Boolean = true, headers: Map<String,String> = emptyMap()): T {
        val account = session?.user?.id
        val result = decode<T>(checked(path, HttpMethod.Get, null, query, authenticated, headers))
        if (authenticated && account != session?.user?.id) throw MobileApiException(401, "account changed")
        return result
    }

    internal suspend inline fun <reified T, reified B> post(path: String, body: B, authenticated: Boolean = true, headers: Map<String,String> = emptyMap()): T {
        val account = session?.user?.id
        val result = decode<T>(checked(path, HttpMethod.Post, json.encodeToString(body), emptyMap(), authenticated, headers))
        if (authenticated && account != session?.user?.id) throw MobileApiException(401, "account changed")
        return result
    }
    internal suspend inline fun <reified T, reified B> put(path: String, body: B): T {
        val account = session?.user?.id
        val result = decode<T>(checked(path,HttpMethod.Put,json.encodeToString(body),emptyMap(),true))
        if (account != session?.user?.id) throw MobileApiException(401,"account changed")
        return result
    }

    internal suspend inline fun <reified T, reified B> patch(path: String, body: B): T {
        val account = session?.user?.id
        val result = decode<T>(checked(path, HttpMethod.Patch, json.encodeToString(body), emptyMap(), true))
        if (account != session?.user?.id) throw MobileApiException(401, "account changed")
        return result
    }

    suspend fun removeResource(kind: String, id: Long, version: Long) {
        require(kind in listOf("pets", "offers", "entries", "entry_comments", "drafts") && id > 0)
        val account = session?.user?.id
        decode<JsonElement>(checked("$kind/$id", HttpMethod.Delete, json.encodeToString(StateInput(version)), emptyMap(), true))
        if (account != session?.user?.id) throw MobileApiException(401, "account changed")
    }

    suspend fun capabilities(): LoginCapabilities = get("auth/capabilities", authenticated = false)
    suspend fun identityMethods(): IdentityMethods = get("auth/methods")
    suspend fun phoneLogin(input: PhoneLoginInput): LoginSession = sessionMutex.withLock {
        ensureStorage();post<LoginSession,PhoneLoginInput>("auth/phone",input.copy(device = device),false).also { save(it) }
    }
    suspend fun recoverPhone(input: PhoneRecoveryInput): JsonElement = post("auth/recover",input,false)
    suspend fun startProvider(input: ProviderStart): ProviderChallenge = post("auth/oauth-start",input,session != null)
    suspend fun finishProvider(input: ProviderFinish): ProviderResult {
        val epoch = accountGeneration
        val result = post<ProviderResult,ProviderFinish>("auth/oauth-finish",input,session != null)
        if (result.login != null) sessionMutex.withLock {
            if (accountGeneration != epoch) throw MobileApiException(401,"session changed")
            save(result.login)
        }
        return result
    }
    suspend fun providerLinkScope(token: String): PurposeScope = post("auth/oauth-link-scope",ProviderResolve(token))
    suspend fun resolveProvider(input: ProviderResolve): LoginSession {
        val epoch = accountGeneration
        val result = post<LoginSession,ProviderResolve>("auth/oauth-resolve",input.copy(device = device),!input.create)
        sessionMutex.withLock { if (accountGeneration != epoch) throw MobileApiException(401,"session changed");save(result) }
        return result
    }
    fun limitedRequestAccess(): List<RestrictedAccess> = vault.limitedAccess.toList()
    internal suspend fun safetySessionSecure(): SafetyAccess {
        val access = post<SafetyAccess,JsonElement>("safety/session",buildJsonObject {},false)
        sessionMutex.withLock { if (vault.limitedAccess.size >= 32) throw MobileStorageException();persist(vault.copy(limitedAccess = vault.limitedAccess + RestrictedAccess("safety_session",newRequestKey(),access.token))) }
        return access
    }
    internal suspend fun safetySubmitSecure(token: String,input: RightsInput,authenticated: Boolean): RightsReceipt {
        sessionMutex.withLock {
            val old = vault.limitedAccess.firstOrNull { it.type == "safety_ticket" && it.clientRequestId == input.clientRequestId }
            if (old != null && old.token != token) throw MobileApiException(409,"safety capability changed","idempotency_conflict")
            if (old == null) { if (vault.limitedAccess.size >= 32) throw MobileStorageException();persist(vault.copy(limitedAccess = vault.limitedAccess + RestrictedAccess("safety_ticket",input.clientRequestId,token))) }
        }
        try {
            val result = post<RightsReceipt,RightsInput>("safety/tickets",input,authenticated,mapOf("X-Safety-Token" to token))
            sessionMutex.withLock { persist(vault.copy(limitedAccess = vault.limitedAccess.map { if (it.type == "safety_ticket" && it.clientRequestId == input.clientRequestId) it.copy(id = result.id) else it })) }
            return result
        } catch (e: CancellationException) { throw e }
        catch (e: MobileApiException) { if (e.status < 500 && e.status != 408) throw e; throw OperationResultUnknown(input.clientRequestId) }
        catch (_: Exception) { throw OperationResultUnknown(input.clientRequestId) }
    }
    internal suspend fun submitDataRequestSecure(input: DataRequestInput): MobileRecord<DataRequestBody> {
        val generation = accountGeneration
        sessionMutex.withLock {
            val old = vault.limitedAccess.firstOrNull { it.type == "data_request" && it.clientRequestId == input.clientRequestId }
            if (old != null && old.token != input.queryToken) throw MobileApiException(409,"query capability changed","idempotency_conflict")
            if (old == null) {
                if (vault.limitedAccess.size >= 32) throw MobileStorageException()
                persist(vault.copy(limitedAccess = vault.limitedAccess + RestrictedAccess("data_request",input.clientRequestId,input.queryToken)))
            }
        }
        try {
            val result = post<MobileRecord<DataRequestBody>,DataRequestInput>("data-requests",input)
            sessionMutex.withLock {
                persist(vault.copy(limitedAccess = vault.limitedAccess.map { if (it.type == "data_request" && it.clientRequestId == input.clientRequestId) it.copy(id = result.id) else it }))
                if (input.type == "close_account" && accountGeneration == generation) save(null)
            }
            return result
        } catch (e: CancellationException) { throw e }
        catch (e: MobileApiException) { if (e.status < 500 && e.status != 408) throw e; throw OperationResultUnknown(input.clientRequestId) }
        catch (_: Exception) { throw OperationResultUnknown(input.clientRequestId) }
    }
    suspend fun requestPhoneOTP(phone: String, purpose: String): OTPReceipt = post("auth/otp", PhoneOTPInput(phone, purpose), purpose != "login" && purpose != "recover")
    suspend fun verifyCurrentPhone(code: String): JsonElement = post("auth/verify-phone", buildJsonObject { put("code", code) })
    suspend fun requestEmailOTP(email: String, purpose: String): OTPReceipt = post("auth/email-otp", EmailOTPInput(email,purpose), purpose == "email_bind")
    suspend fun emailLogin(input: EmailAuthInput, register: Boolean = false): LoginSession = sessionMutex.withLock {
        ensureStorage()
        post<LoginSession,EmailAuthInput>(if (register) "auth/register" else "auth/email", input.copy(device = device), false).also { save(it) }
    }
    suspend fun recoverEmail(input: EmailAuthInput): JsonElement = post("auth/email-recover",input, false)
    suspend fun reauthenticate(input: ReauthInput): PurposeGrant = post("auth/reauth", input)
    suspend fun accountScope(action: String, input: AccountInput): PurposeScope = post("auth/account/$action/scope",input)
    suspend fun changeAccount(action: String, input: AccountInput) {
        val owner = session?.user?.id ?: throw MobileApiException(401,"authentication required")
        val generation = accountGeneration
        val previous = sessionMutex.withLock { vault.commands.firstOrNull { it.owner == owner && it.id == input.clientRequestId } }
        if (previous?.phase in listOf("sent","result_unknown")) throw OperationResultUnknown(input.clientRequestId)
        if (previous?.phase == "committed") return
        val scope = accountScope(action,input)
        sessionMutex.withLock {
            val kept = vault.commands.filterNot { it.phase in listOf("committed","rejected") }
            if (kept.size >= 16) throw MobileStorageException()
            // Scope contains a server HMAC-bound digest. No password, OTP or grant is saved.
            persist(vault.copy(commands = kept + DurableCommand(owner,input.clientRequestId,"auth/account/$action",json.encodeToString(scope),"sent",replayable = false)))
        }
        try {
            post<JsonElement,AccountInput>("auth/account/$action",input)
            withContext(NonCancellable) { sessionMutex.withLock {
                persist(vault.copy(commands = vault.commands.map { if (it.owner == owner && it.id == input.clientRequestId) it.copy(phase = "committed") else it }))
                if (accountGeneration == generation && session?.user?.id == owner) save(null)
            } }
        } catch (e: Exception) {
            val known = e is MobileApiException && e.status in listOf(400,401,403,404,409,429)
            withContext(NonCancellable) { sessionMutex.withLock {
                try { persist(vault.copy(commands = vault.commands.map { if (it.owner == owner && it.id == input.clientRequestId) it.copy(phase = if (known) "rejected" else "result_unknown") else it })) }
                catch (_: MobileStorageException) { }
            } }
            if (e is CancellationException || known) throw e
            throw OperationResultUnknown(input.clientRequestId)
        }
    }
    suspend fun eligibility(): Eligibility = get("eligibility")
    suspend fun operation(key: String): OperationReceipt = get("operations/$key")
    fun recoverableCommands(): List<CommandRecovery> = vault.commands.filter { it.owner == session?.user?.id && it.phase !in listOf("committed", "rejected") }
        .map { CommandRecovery(it.id, it.path, it.phase, it.receipt,it.replayable) }

    fun commandStatus(key: String): CommandRecovery? = vault.commands.firstOrNull { it.owner == session?.user?.id && it.id == key }
        ?.let { CommandRecovery(it.id, it.path, it.phase, it.receipt, it.replayable) }

    suspend fun retryOriginal(key: String): OperationReceipt {
        val owner = session?.user?.id ?: throw MobileApiException(401,"authentication required")
        val command = sessionMutex.withLock {
            val old = vault.commands.firstOrNull { it.owner == owner && it.id == key } ?: throw MobileApiException(404,"command not found")
            if (!old.replayable) throw MobileApiException(403,"fresh scoped verification required","purpose_grant_required")
            if (old.phase !in listOf("sent","result_unknown","prepared")) throw MobileApiException(409,"command is already resolved")
            persist(vault.copy(commands = vault.commands.map { if (it.owner == owner && it.id == key) it.copy(phase = "prepared") else it }))
            old
        }
        // Explicit retry of the original bytes and key; never invent a new intent after 404.
        durableRequest(command.path,command.body,command.id)
        return sessionMutex.withLock { vault.commands.first { it.owner == owner && it.id == key }.receipt ?: throw OperationResultUnknown(key) }
    }

    suspend fun reconcileOperation(key: String): OperationReceipt {
        val owner = session?.user?.id ?: throw MobileApiException(401, "authentication required")
        val receipt = operation(key) // A 404 leaves result_unknown intact: the original request may still arrive.
        sessionMutex.withLock {
            if (session?.user?.id != owner) throw MobileApiException(401, "account changed")
            persist(vault.copy(commands = vault.commands.map { if (it.owner == owner && it.id == key) it.copy(phase = receipt.state, receipt = receipt) else it }))
        }
        return receipt
    }

    private fun logicalBody(body: String): JsonObject = JsonObject(json.parseToJsonElement(body).jsonObject.filterKeys { it !in listOf("client_request_id", "idempotency_key") })
    internal suspend fun durableRequest(path: String, body: String, key: String, sensitive: Boolean = false): JsonElement {
        require(key.length in 8..100)
        val snapshot = if (sensitive) JsonObject(json.parseToJsonElement(body).jsonObject.filterKeys { it !in listOf("reauth_token","password","code") }).toString() else body
        val owner = session?.user?.id ?: throw MobileApiException(401, "authentication required")
        val command = sessionMutex.withLock {
            val existing = vault.commands.firstOrNull { it.owner == owner && it.id == key }
            if (existing != null && (existing.path != path || existing.body != snapshot)) throw MobileApiException(409, "request body changed", "idempotency_conflict")
            val unresolved = vault.commands.firstOrNull { it.owner == owner && it.path == path && logicalBody(it.body) == logicalBody(snapshot) && it.phase !in listOf("committed", "rejected") }
            val selected = existing ?: unresolved ?: DurableCommand(owner, key, path, snapshot,replayable = !sensitive)
            if (selected.phase in listOf("sent", "result_unknown")) throw OperationResultUnknown(selected.id)
            if (selected.phase == "committed") return@withLock selected
            val kept = vault.commands.filterNot { it.owner == owner && it.id == selected.id }
                .filterNot { it.phase in listOf("committed", "rejected") }
            if (kept.size >= 16) throw MobileApiException(409, "reconcile pending commands before continuing", "pending_operations_limit")
            persist(vault.copy(commands = kept + selected.copy(phase = "sent")))
            selected
        }
        if (command.phase == "committed") {
            val receipt = command.receipt ?: operation(command.id)
            return get("${receipt.resultKind}/${receipt.resultId}")
        }
        try {
            val result = decode<JsonElement>(checked(command.path, HttpMethod.Post, if (sensitive) body else command.body, emptyMap(), true))
            val obj = result.jsonObject
            val kind = when {
                path.endsWith("/comments") -> "entry_comments"
                path.endsWith("/messages") -> if (path.startsWith("groups/")) "group_messages" else "messages"
                else -> path.substringBefore('/')
            }
            val receipt = OperationReceipt(command.id, path, 0, kind, obj.getValue("id").jsonPrimitive.long, obj.getValue("version").jsonPrimitive.long, "committed")
            withContext(NonCancellable) { sessionMutex.withLock {
                persist(vault.copy(commands = vault.commands.map { if (it.owner == owner && it.id == command.id) it.copy(phase = "committed", receipt = receipt) else it }))
            } }
            return result
        } catch (e: Exception) {
            val knownRejection = e is MobileApiException && e.status in listOf(400, 401, 403, 404, 409, 422, 429)
            withContext(NonCancellable) { sessionMutex.withLock {
                try { persist(vault.copy(commands = vault.commands.map { if (it.owner == owner && it.id == command.id) it.copy(phase = if (knownRejection) "rejected" else "result_unknown") else it })) }
                catch (_: MobileStorageException) { /* The previously persisted sent record remains recoverable. */ }
            } }
            if (e is CancellationException || knownRejection) throw e
            throw OperationResultUnknown(command.id)
        }
    }
    internal suspend inline fun <reified T, reified B> durablePost(path: String, body: B, key: String): T =
        json.decodeFromJsonElement(durableRequest(path, json.encodeToString(body), key))
    internal suspend inline fun <reified T, reified B> sensitivePost(path: String, body: B, key: String): T =
        json.decodeFromJsonElement(durableRequest(path,json.encodeToString(body),key,true))
    suspend fun password(username: String, password: String) = sessionMutex.withLock {
        ensureStorage()
        val identifier = username.trim()
        val phone = identifier.filter { it.isDigit() }
        val phoneIdentifier = phone.length in 7..15 && identifier.all { it.isDigit() || it in "+ ()-" }
        post<LoginSession, PasswordLoginInput>("auth/password", PasswordLoginInput(
            username = if (phoneIdentifier || identifier.contains('@')) "" else identifier, phone = if (phoneIdentifier) identifier else "",
            email = if (identifier.contains('@')) identifier else "",
            password = password, device = device), false).also { save(it) }
    }
    suspend fun logout(all: Boolean = false) {
        // Do not erase a failed server revocation and present it as successful logout.
        post<JsonElement, LogoutInput>("auth/logout", LogoutInput(all))
        sessionMutex.withLock { save(null) }
    }
    suspend fun forgetExpiredSession() = sessionMutex.withLock { save(null) }
    suspend fun profile(): MobileProfile = get("profile")
    suspend fun editProfile(input: ProfileEdit): MobileProfile {
        val profile = patch<MobileProfile,ProfileEdit>("profile",input)
        sessionMutex.withLock {
            val current = session ?: throw MobileApiException(401,"account changed")
            if (current.user.id != profile.user.id) throw MobileApiException(401,"account changed")
            save(current.copy(user = profile.user))
        }
        return profile
    }
    suspend fun entries(cursor: Long = 0, space: String = "community"): MobilePage<EntryBody> = get("entries", mapOf("cursor" to "$cursor", "space" to space))
    suspend fun entry(id: Long): MobileRecord<EntryBody> = get("entries/$id")
    suspend fun metrics(ids: List<Long>): List<EntryMetrics> = if (ids.isEmpty()) emptyList() else get("entry-metrics", mapOf("ids" to ids.take(50).joinToString(",")))
    suspend fun comments(id: Long, cursor: Long = 0): MobilePage<CommentBody> = get("entry_comments", mapOf("reference_id" to "$id", "cursor" to "$cursor"))
    suspend fun comment(id: Long, text: String, key: String): MobileRecord<CommentBody> = durablePost("entries/$id/comments", CommentInput(MessageBody(text), key), key)
    suspend fun react(id: Long, liked: Boolean): JsonElement = post("entries/$id/reaction", ReactionInput(liked))
    suspend fun previewPublication(input: EntryInput): PublicationPreview = post("entries/preview", input)
    suspend fun preparePublication(input: EntryInput): EntryInput {
        val owner = session?.user?.id ?: throw MobileApiException(401, "authentication required")
        return sessionMutex.withLock {
            if (session?.user?.id != owner) throw MobileApiException(401, "account changed")
            require(input.clientRequestId.length in 8..100 && input.declaration.confirmed)
            val body = json.encodeToString(input)
            val existing = vault.commands.firstOrNull { it.owner == owner && it.id == input.clientRequestId }
            if (existing != null && (existing.path != "entries" || existing.body != body)) {
                throw MobileApiException(409, "request body changed", "idempotency_conflict")
            }
            val unresolved = vault.commands.firstOrNull { it.owner == owner && it.path == "entries" &&
                logicalBody(it.body) == logicalBody(body) && it.phase !in listOf("committed", "rejected") }
            val selected = existing ?: unresolved ?: DurableCommand(owner, input.clientRequestId, "entries", body)
            if (selected.phase in listOf("sent", "result_unknown")) throw OperationResultUnknown(selected.id)
            if (existing == null && unresolved == null) {
                if (vault.commands.count { it.phase !in listOf("committed", "rejected") } >= 16) {
                    throw MobileApiException(409, "reconcile pending commands before continuing", "pending_operations_limit")
                }
                persist(vault.copy(commands = vault.commands + selected))
            }
            json.decodeFromString<EntryInput>(selected.body)
        }
    }
    suspend fun publish(input: EntryInput): MobileRecord<EntryBody> = durablePost("entries", input, input.clientRequestId)
    suspend fun pets(cursor: Long = 0, owner: Long? = null): MobilePage<PetBody> = get("pets", buildMap {
        put("cursor", "$cursor"); if (owner != null) put("owner_id", "$owner")
    })
    suspend fun pet(id: Long): MobileRecord<PetBody> = get("pets/$id")
    suspend fun savePet(input: PetInput): MobileRecord<PetBody> {
        val command = if (input.clientRequestId.isEmpty()) input.copy(clientRequestId = newRequestKey()) else input
        return durablePost("pets",command,command.clientRequestId)
    }
    suspend fun apply(id: Long, message: String, key: String = newRequestKey()): MobileRecord<AdoptionBody> = durablePost("adoptions", AdoptionInput(id, message, key), key)
    suspend fun adoptions(cursor: Long = 0): MobilePage<AdoptionBody> = get("adoptions", mapOf("cursor" to "$cursor"))
    suspend fun adoption(id: Long): MobileRecord<AdoptionBody> = get("adoptions/$id")
    suspend fun adoptionAction(id: Long, action: String, input: AdoptionActionInput): MobileRecord<AdoptionBody> {
        val command = if (input.clientRequestId.isEmpty()) input.copy(clientRequestId = newRequestKey()) else input
        return durablePost("adoptions/$id/$action", command, command.clientRequestId)
    }
    suspend fun cases(cursor: Long = 0, city: String = ""): MobilePage<CaseBody> = get("cases", mapOf("cursor" to "$cursor", "city" to city))
    suspend fun case(id: Long): MobileRecord<CaseBody> = get("cases/$id")
    suspend fun conversations(cursor: Long = 0): MobilePage<ConversationBody> = get("conversations", mapOf("cursor" to "$cursor"))
    suspend fun groups(cursor: Long = 0): MobilePage<GroupBody> = get("groups", mapOf("cursor" to "$cursor"))
    suspend fun conversation(id: Long): MobileRecord<ConversationBody> = get("conversations/$id")
    suspend fun startConversation(user: Long, key: String = newRequestKey()): MobileRecord<ConversationBody> = durablePost("conversations", ConversationInput(user, key), key)
    suspend fun conversationAction(id: Long, version: Long, action: String): MobileRecord<ConversationBody> = post("conversations/$id/$action", StateInput(version))
    suspend fun messages(id: Long, group: Boolean, cursor: Long = 0): MobilePage<MessageBody> = get(if (group) "group_messages" else "messages", mapOf("reference_id" to "$id", "cursor" to "$cursor"))
    suspend fun send(id: Long, group: Boolean, text: String, key: String): MobileRecord<MessageBody> = durablePost("${if (group) "groups" else "conversations"}/$id/messages", SendMessageInput(MessageBody(text), key), key)
    suspend fun readMessages(id: Long, group: Boolean, through: Long): JsonElement = post("${if (group) "groups" else "conversations"}/$id/read", ReadInput(through))
    suspend fun inbox(): InboxSummary = get("inbox-summary")
    suspend fun notifications(cursor: Long = 0): MobilePage<NotificationBody> = get("notifications", mapOf("cursor" to "$cursor"))
    suspend fun readNotification(id: Long, version: Long): MobileRecord<NotificationBody> = post("notifications/$id/read", StateInput(version))
    suspend fun expressions(): ExpressionCatalog = get("expression-catalog", authenticated = false)
    suspend fun drafts(cursor: Long = 0): MobilePage<DraftBody> = get("drafts", mapOf("cursor" to "$cursor"))
    internal suspend fun saveComposeDraft(version: Long, title: String, text: String, ids: List<Long>, entry: EntryBody? = null): MobileRecord<DraftBody> {
        val account = session?.user?.id
        val record = decode<MobileRecord<DraftBody>>(checked("drafts", HttpMethod.Put,
            json.encodeToString(DraftInput(version, DraftBody("mnd.compose.community.v1", json.encodeToString(ComposeDraftContent(title = title, text = text, entry = entry))), mediaIds = ids)), emptyMap(), true))
        if (account != session?.user?.id) throw MobileApiException(401, "account changed")
        return record
    }

    suspend fun upload(photo: SelectedPhoto,key: String): MobileRecord<MediaBody> = uploadScoped(photo,key,null)
    suspend fun uploadPrivate(photo: SelectedPhoto,key: String): MobileRecord<MediaBody> = uploadScoped(photo,key,null,"private")
    suspend fun uploadSafetyEvidence(photo: SelectedPhoto,key: String,safetyToken: String): MobileRecord<MediaBody> = uploadScoped(photo,key,safetyToken)
    private suspend fun uploadScoped(photo: SelectedPhoto, key: String, safetyToken: String?, mediaScope: String = "public"): MobileRecord<MediaBody> {
        val account = session?.user?.id
        require(photo.mime in listOf("image/jpeg", "image/png") && photo.bytes.size in 1..10 * 1024 * 1024)
        val multipart = MultiPartFormDataContent(formData {
            append("scope", if (safetyToken == null) mediaScope else "private")
            append("file", photo.bytes, Headers.build {
                append(HttpHeaders.ContentType, photo.mime)
                append(HttpHeaders.ContentDisposition, "filename=\"${if (photo.mime == "image/png") "photo.png" else "photo.jpg"}\"")
            })
        })
        val record = decode<MobileRecord<MediaBody>>(checked(if (safetyToken == null) "media" else "safety/media", HttpMethod.Post, multipart, emptyMap(), safetyToken == null, buildMap { put("Idempotency-Key",key); if (safetyToken != null) put("X-Safety-Token",safetyToken) }))
        if (safetyToken == null && account != session?.user?.id) throw MobileApiException(401, "account changed")
        return record
    }

    suspend fun media(id: Long): ByteArray {
        val account = session?.user?.id
        val result = checked("media/$id/content", HttpMethod.Get, null, emptyMap(), true)
        if (!result.status.isSuccess()) throw MobileApiException(result.status.value, "media unavailable")
        val bytes = result.body<ByteArray>()
        if (account != session?.user?.id) throw MobileApiException(401, "account changed")
        return bytes
    }

    fun close() { http.close() }
}
