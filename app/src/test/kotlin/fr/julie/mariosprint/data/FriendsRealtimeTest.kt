package fr.julie.mariosprint.data

import fr.julie.mariosprint.data.remote.*
import fr.julie.mariosprint.domain.model.FriendsLiveEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Vrais sockets locaux, sans Supabase distant, avec délais raccourcis injectés. */
class FriendsRealtimeTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val token = AtomicReference("jwt-one")
    private val renewals = AtomicInteger()
    @Before fun prepare() { server = MockWebServer().apply { start() }; client = OkHttpClient() }
    @After fun cleanup() { client.dispatcher.cancelAll(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown(); server.shutdown() }
    private fun source(timing: FriendsRealtimeTiming = FriendsRealtimeTiming(100, 80, 1000, 20, 100)) =
        SupabaseFriendsRealtimeSource(server.url("/").toString(), "sb_publishable_test", object : AccessTokenProvider {
            override suspend fun tokenFor(uid: String, rejectedToken: String?): String {
                check(uid == "alice")
                if (rejectedToken != null) { renewals.incrementAndGet(); token.set("jwt-two") }
                return token.get()
            }
        }, client, timing)
    private fun message(event: String, topic: String = "realtime:friends:alice", payload: JsonObject, ref: String? = null) = buildJsonObject {
        put("event", event); put("topic", topic); put("payload", payload)
        ref?.let { put("ref", it) }
        if (topic != "phoenix") put("join_ref", "1")
    }.toString()
    private class SocketProbe(val onFrame: (WebSocket, JsonObject) -> Unit = { _, _ -> }) : WebSocketListener() {
        val joined = CompletableDeferred<JsonObject>()
        val socket = CompletableDeferred<WebSocket>()
        val stopped = CompletableDeferred<Unit>()
        override fun onOpen(webSocket: WebSocket, response: Response) { socket.complete(webSocket) }
        override fun onMessage(webSocket: WebSocket, text: String) {
            val frame = SupabaseClient.json.parseToJsonElement(text).jsonObject
            if (frame["event"]?.jsonPrimitive?.content == "phx_join") {
                joined.complete(frame)
                webSocket.send(buildJsonObject {
                    put("event", "phx_reply"); put("topic", frame.getValue("topic")); put("ref", "1"); put("join_ref", "1")
                    putJsonObject("payload") { put("status", "ok"); putJsonObject("response") {} }
                }.toString())
            }
            if (frame["event"]?.jsonPrimitive?.content == "heartbeat") webSocket.send(buildJsonObject {
                put("event", "phx_reply"); put("topic", "phoenix"); put("ref", frame.getValue("ref"))
                putJsonObject("payload") { put("status", "ok") }
            }.toString())
            onFrame(webSocket, frame)
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { stopped.complete(Unit) }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null); stopped.complete(Unit) }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { stopped.complete(Unit) }
    }
    @Test fun privateJoinContainsTheUsersJwtAndOnlyValidOwnTopicInvalidationsAreAccepted() = runBlocking {
        val probe = SocketProbe(); server.enqueue(MockResponse().withWebSocketUpgrade(probe))
        val seen = mutableListOf<FriendsLiveEvent>()
        val connected = CompletableDeferred<Unit>(); val changed = CompletableDeferred<Unit>()
        val job = launch { source().events("alice").collect { seen += it; if (it == FriendsLiveEvent.CONNECTED) connected.complete(Unit); if (it == FriendsLiveEvent.CHANGED) changed.complete(Unit) } }
        try {
            val join = withTimeout(5000) { probe.joined.await() }
            assertEquals("realtime:friends:alice", join["topic"]?.jsonPrimitive?.content)
            val payload = join.getValue("payload").jsonObject
            assertEquals("jwt-one", payload["access_token"]?.jsonPrimitive?.content)
            assertEquals(true, payload.getValue("config").jsonObject["private"]?.jsonPrimitive?.boolean)
            withTimeout(5000) { connected.await() }
            val socket = probe.socket.await()
            socket.send("not-json"); socket.send("""{"event":{},"payload":{}}""")
            socket.send(message("broadcast", "realtime:friends:bob", buildJsonObject { put("event", "friends_changed") }))
            socket.send(message("broadcast", payload = buildJsonObject { put("event", "friends_changed"); putJsonObject("payload") { put("version", 1) } }))
            withTimeout(5000) { changed.await() }
            assertEquals(listOf(FriendsLiveEvent.CONNECTED, FriendsLiveEvent.CHANGED), seen)
        } finally { job.cancelAndJoin() }
    }
    @Test fun heartbeatsAndRotatedJwtAreSentWithoutRejoiningTheChannel() = runBlocking {
        val heartbeat = CompletableDeferred<JsonObject>(); val renewed = CompletableDeferred<JsonObject>()
        val probe = SocketProbe { _, frame ->
            when (frame["event"]?.jsonPrimitive?.content) {
                "heartbeat" -> heartbeat.complete(frame)
                "access_token" -> renewed.complete(frame)
            }
        }
        server.enqueue(MockResponse().withWebSocketUpgrade(probe))
        val job = launch { source().events("alice").collect() }
        try {
            withTimeout(5000) { probe.joined.await() }; token.set("jwt-two")
            assertEquals("phoenix", withTimeout(5000) { heartbeat.await() }["topic"]?.jsonPrimitive?.content)
            val frame = withTimeout(5000) { renewed.await() }
            assertEquals("jwt-two", frame.getValue("payload").jsonObject["access_token"]?.jsonPrimitive?.content)
            assertEquals("1", frame["join_ref"]?.jsonPrimitive?.content)
            assertEquals(1, server.requestCount)
        } finally { job.cancelAndJoin() }
    }
    @Test fun cancellationClosesTheSocketAndStopsAllRetryAndRenewalWork() = runBlocking {
        val probe = SocketProbe(); server.enqueue(MockResponse().withWebSocketUpgrade(probe))
        val connected = CompletableDeferred<Unit>()
        val job = launch { source().events("alice").collect { if (it == FriendsLiveEvent.CONNECTED) connected.complete(Unit) } }
        withTimeout(5000) { connected.await() }
        job.cancelAndJoin()
        withTimeout(5000) { probe.stopped.await() }
        delay(150)
        assertEquals(1, server.requestCount)
    }
    @Test fun anUnauthorizedHandshakeRenewsOnceBeforeReconnectingPrivately() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        val probe = SocketProbe(); server.enqueue(MockResponse().withWebSocketUpgrade(probe))
        val connected = CompletableDeferred<Unit>()
        val job = launch { source().events("alice").collect { if (it == FriendsLiveEvent.CONNECTED) connected.complete(Unit) } }
        try {
            withTimeout(5000) { connected.await() }
            val payload = probe.joined.await().getValue("payload").jsonObject
            assertEquals("jwt-two", payload["access_token"]?.jsonPrimitive?.content)
            assertEquals(1, renewals.get()); assertEquals(2, server.requestCount)
        } finally { job.cancelAndJoin() }
    }
    @Test fun aChannelThatNeverAcknowledgesJoinTimesOutAndReleasesItsSocket() = runBlocking {
        val stopped = CompletableDeferred<Unit>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { stopped.complete(Unit) }
        }))
        val result = withTimeout(5000) { source(FriendsRealtimeTiming(100, 80, 80, 20, 100)).events("alice").first() }
        assertEquals(FriendsLiveEvent.DISCONNECTED, result)
        withTimeout(5000) { stopped.await() }
    }
    @Test fun serverClosureCreatesANewSubscriptionSoTheCallerCanRecoverMissedRequests() = runBlocking {
        val first = SocketProbe(); val second = SocketProbe()
        server.enqueue(MockResponse().withWebSocketUpgrade(first)); server.enqueue(MockResponse().withWebSocketUpgrade(second))
        val initial = CompletableDeferred<Unit>(); val resumed = CompletableDeferred<Unit>()
        var joins = 0
        val job = launch { source().events("alice").collect {
            if (it == FriendsLiveEvent.CONNECTED) { joins++; if (joins == 1) initial.complete(Unit) else resumed.complete(Unit) }
        } }
        try {
            withTimeout(5000) { initial.await() }
            first.socket.await().close(1000, "Reconnect test")
            withTimeout(5000) { resumed.await() }
            assertEquals(2, joins); assertEquals(2, server.requestCount)
        } finally { job.cancelAndJoin() }
    }
    @Test fun anExpiredJwtReportedByTheChannelRenewsBeforeTheNextPrivateJoin() = runBlocking {
        val first = SocketProbe(); val second = SocketProbe()
        server.enqueue(MockResponse().withWebSocketUpgrade(first)); server.enqueue(MockResponse().withWebSocketUpgrade(second))
        val initial = CompletableDeferred<Unit>(); val resumed = CompletableDeferred<Unit>()
        var joins = 0
        val job = launch { source().events("alice").collect {
            if (it == FriendsLiveEvent.CONNECTED) { joins++; if (joins == 1) initial.complete(Unit) else resumed.complete(Unit) }
        } }
        try {
            withTimeout(5000) { initial.await() }
            first.socket.await().send(message("system", payload = buildJsonObject { put("status", "error"); put("message", "JWT expired") }))
            withTimeout(5000) { resumed.await() }
            assertEquals("jwt-two", second.joined.await().getValue("payload").jsonObject["access_token"]?.jsonPrimitive?.content)
            assertEquals(1, renewals.get())
        } finally { job.cancelAndJoin() }
    }
    @Test fun renewalTimeoutClosesTheOldSocketInsteadOfLeavingItsHeartbeatRunning() = runBlocking {
        val probe = SocketProbe(); server.enqueue(MockResponse().withWebSocketUpgrade(probe))
        val calls = AtomicInteger()
        val source = SupabaseFriendsRealtimeSource(server.url("/").toString(), "sb_publishable_test", object : AccessTokenProvider {
            override suspend fun tokenFor(uid: String, rejectedToken: String?): String {
                if (calls.incrementAndGet() > 1) withTimeout(20) { awaitCancellation() }
                return "jwt-one"
            }
        }, client, FriendsRealtimeTiming(1000, 30, 1000, 20, 100))
        assertEquals(FriendsLiveEvent.DISCONNECTED, withTimeout(5000) { source.events("alice").first { it == FriendsLiveEvent.DISCONNECTED } })
        withTimeout(5000) { probe.stopped.await() }
        assertEquals(2, calls.get())
    }
    @Test fun initialTokenTimeoutCanRecoverWithoutAnAppRestart() = runBlocking {
        val probe = SocketProbe(); server.enqueue(MockResponse().withWebSocketUpgrade(probe))
        val calls = AtomicInteger()
        val source = SupabaseFriendsRealtimeSource(server.url("/").toString(), "sb_publishable_test", object : AccessTokenProvider {
            override suspend fun tokenFor(uid: String, rejectedToken: String?): String {
                if (calls.incrementAndGet() == 1) withTimeout(20) { awaitCancellation() }
                return "jwt-one"
            }
        }, client, FriendsRealtimeTiming(1000, 1000, 1000, 20, 100))
        val seen = withTimeout(5000) { source.events("alice").take(2).toList() }
        assertEquals(listOf(FriendsLiveEvent.DISCONNECTED, FriendsLiveEvent.CONNECTED), seen)
        assertEquals(2, calls.get()); assertEquals(1, server.requestCount)
    }
}
