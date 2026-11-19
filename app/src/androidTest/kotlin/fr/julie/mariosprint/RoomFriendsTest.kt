package fr.julie.mariosprint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import fr.julie.mariosprint.data.local.*
import fr.julie.mariosprint.data.local.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

/** Base en mémoire isolée. Compilation PC ; exécution uniquement avec accord appareil explicite. */
class RoomFriendsTest {
    private lateinit var database: SprintDatabase
    private lateinit var store: RoomFriendsLocalStore
    @Before fun prepare() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, SprintDatabase::class.java).build()
        store = RoomFriendsLocalStore(database)
    }
    @After fun cleanup() { database.close() }
    private fun cache(uid: String, revision: Long) = FriendsCacheEntity(uid, "ABCDEF123456", revision, 100)
    private fun incoming(uid: String, id: String = "r") = FriendRelationEntity(uid, "peer", id, "Peer", "luigi", "incoming")
    @Test fun snapshotsAreAtomicAndOldRepliesCannotOverwriteNewerRevisions() = runBlocking {
        assertEquals(1, store.replace(cache("alice", 5), listOf(incoming("alice"))))
        assertEquals(0, store.replace(cache("alice", 4), emptyList()))
        assertEquals(5L, store.observe("alice").first()?.cache?.revision)
        assertEquals(1, store.observe("alice").first()?.relations?.size)
        assertEquals(0, store.replace(cache("alice", 5), listOf(incoming("alice"))))
    }
    @Test fun cacheReplacementAndAlertsAreSeparatedByViewerUid() = runBlocking {
        store.replace(cache("alice", 1), listOf(incoming("alice")))
        assertNull(store.observe("bob").first())
        assertEquals(1, store.replace(cache("bob", 1), listOf(incoming("bob"))))
        store.replace(cache("alice", 2), emptyList())
        assertTrue(store.observe("alice").first()?.relations?.isEmpty() == true)
        assertEquals(1, store.observe("bob").first()?.relations?.size)
    }
    @Test fun onlyTheFiftyMostRecentProfilesAndTheirRecordsAreKeptPerAccount() = runBlocking {
        for (viewer in listOf("alice", "bob")) repeat(60) { number ->
            store.saveProfile(SocialProfileEntity(viewer, "peer-$number", "Peer", "luigi", null, number.toLong()),
                listOf(SocialProfileRecordEntity(viewer, "peer-$number", "prairie-1-v2", 42_000)))
        }
        assertTrue(store.observeProfile("alice", "peer-0").first().isEmpty())
        assertEquals(42_000L, store.observeProfile("bob", "peer-59").first().single().timeMs)
        assertTrue(store.observeProfile("unknown", "peer-59").first().isEmpty())
    }
}
