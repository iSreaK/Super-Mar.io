package fr.julie.mariosprint.data.remote

import fr.julie.mariosprint.domain.model.FriendsLiveEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal interface FriendsRealtimeSource {
    fun events(uid: String): Flow<FriendsLiveEvent>
}

internal data class FriendsRealtimeTiming(val heartbeatMs: Long = 20_000, val tokenRefreshMs: Long = 30_000,
    val joinTimeoutMs: Long = 10_000, val retryMinMs: Long = 1_000, val retryMaxMs: Long = 30_000)

/** Protocole Supabase/Phoenix v1, via la version OkHttp déjà utilisée par Retrofit. */
internal class SupabaseFriendsRealtimeSource(
    url: String, publishableKey: String, private val tokens: AccessTokenProvider,
    private val client: OkHttpClient = OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
    private val timing: FriendsRealtimeTiming = FriendsRealtimeTiming(),
) : FriendsRealtimeSource {
    private val socketUrl = url.trimEnd('/').toHttpUrl().newBuilder().addPathSegments("realtime/v1/websocket")
        .addQueryParameter("apikey", publishableKey).addQueryParameter("vsn", "1.0.0").build()

    override fun events(uid: String): Flow<FriendsLiveEvent> = flow {
        var retryMs = timing.retryMinMs
        var forcedRefresh = false
        while (currentCoroutineContext().isActive) {
            try {
                val token = tokens.tokenFor(uid)
                connection(uid, token).collect { event ->
                    retryMs = timing.retryMinMs
                    forcedRefresh = false
                    emit(event)
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                emit(FriendsLiveEvent.DISCONNECTED)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                emit(FriendsLiveEvent.DISCONNECTED)
                if (error is RealtimeAuthRejected && !forcedRefresh) {
                    forcedRefresh = true
                    try { tokens.tokenFor(uid, error.rejectedToken) }
                    catch (_: TimeoutCancellationException) { currentCoroutineContext().ensureActive() }
                    catch (canceled: CancellationException) { throw canceled }
                    catch (_: Exception) { /* La prochaine tentative reste bornée par le backoff. */ }
                }
            }
            delay(retryMs)
            retryMs = (retryMs * 2).coerceAtMost(timing.retryMaxMs)
        }
    }.conflate()

    private fun connection(uid: String, initialToken: String): Flow<FriendsLiveEvent> = callbackFlow {
        val topic = "realtime:friends:$uid"
        val joined = CompletableDeferred<Unit>()
        val currentToken = AtomicReference(initialToken)
        val heartbeatRef = AtomicReference<String?>(null)
        val nextRef = AtomicLong(2)
        fun frame(event: String, payload: JsonObject, ref: String, channel: String = topic): String = buildJsonObject {
            put("topic", channel); put("event", event); put("payload", payload); put("ref", ref)
            if (channel != "phoenix") put("join_ref", "1")
        }.toString()
        fun reject(payload: JsonObject): IOException {
            val reason = payload.toString().lowercase()
            return if (reason.contains("jwt") || reason.contains("token") || reason.contains("expired"))
                RealtimeAuthRejected(currentToken.get()) else IOException("Realtime channel unavailable")
        }
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val payload = buildJsonObject {
                    put("access_token", initialToken)
                    putJsonObject("config") {
                        put("private", true)
                        putJsonObject("broadcast") { put("ack", false); put("self", false) }
                        putJsonObject("presence") { put("enabled", false) }
                        putJsonArray("postgres_changes") {}
                    }
                }
                if (!webSocket.send(frame("phx_join", payload, "1"))) close(IOException("Realtime join failed"))
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.length > 16_384) return
                val message = try { SupabaseClient.json.parseToJsonElement(text).jsonObject } catch (_: Exception) { return }
                val event = message["event"].textValue ?: return
                val messageTopic = message["topic"].textValue
                val ref = message["ref"].textValue
                val payload = message["payload"] as? JsonObject ?: return
                if (messageTopic == "phoenix" && event == "phx_reply" && ref != null) {
                    heartbeatRef.compareAndSet(ref, null)
                    return
                }
                if (messageTopic != topic || message["join_ref"].textValue?.let { it != "1" } == true) return
                when (event) {
                    "phx_reply" -> if (ref == "1") {
                        if (payload["status"].textValue == "ok") {
                            joined.complete(Unit); trySend(FriendsLiveEvent.CONNECTED)
                        } else close(reject(payload))
                    }
                    "broadcast" -> if (joined.isCompleted && payload["event"].textValue == "friends_changed")
                        trySend(FriendsLiveEvent.CHANGED)
                    "phx_error", "phx_close" -> close(reject(payload))
                    "system" -> if (payload["status"].textValue == "error") close(reject(payload))
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(if (response?.code == 401) RealtimeAuthRejected(currentToken.get()) else IOException("Realtime disconnected"))
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null); close(IOException("Realtime closed"))
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { close(IOException("Realtime closed")) }
        }
        val socket = client.newWebSocket(Request.Builder().url(socketUrl).build(), listener)
        val joinWatch = launch {
            if (withTimeoutOrNull(timing.joinTimeoutMs) { joined.await() } == null) close(IOException("Realtime join timeout"))
        }
        val heartbeat = launch {
            joined.await()
            while (isActive) {
                delay(timing.heartbeatMs)
                if (heartbeatRef.get() != null) { close(IOException("Realtime heartbeat timeout")); break }
                val ref = nextRef.getAndIncrement().toString()
                heartbeatRef.set(ref)
                if (!socket.send(frame("heartbeat", JsonObject(emptyMap()), ref, "phoenix"))) {
                    close(IOException("Realtime heartbeat failed")); break
                }
            }
        }
        val renewal = launch {
            joined.await()
            while (isActive) {
                delay(timing.tokenRefreshMs)
                val token = try { tokens.tokenFor(uid) }
                catch (_: TimeoutCancellationException) { close(IOException("Realtime renewal timeout")); break }
                if (token != currentToken.getAndSet(token) && !socket.send(frame("access_token",
                        buildJsonObject { put("access_token", token) }, nextRef.getAndIncrement().toString()))) {
                    close(IOException("Realtime token update failed")); break
                }
            }
        }
        awaitClose {
            joinWatch.cancel(); heartbeat.cancel(); renewal.cancel()
            socket.cancel()
        }
    }.buffer(Channel.CONFLATED)

    private class RealtimeAuthRejected(val rejectedToken: String) : IOException("Realtime authentication rejected")
}

private val JsonElement?.textValue: String? get() = (this as? JsonPrimitive)?.contentOrNull
