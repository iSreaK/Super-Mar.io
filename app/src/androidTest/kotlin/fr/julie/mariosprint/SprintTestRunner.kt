package fr.julie.mariosprint

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.room.Room
import fr.julie.mariosprint.architecture.*
import fr.julie.mariosprint.data.local.*
import fr.julie.mariosprint.data.repository.*
import fr.julie.mariosprint.domain.game.RunResult
import fr.julie.mariosprint.domain.model.*
import fr.julie.mariosprint.domain.repository.*
import kotlinx.coroutines.flow.*

class SprintTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, QaSprintApplication::class.java.name, context)
}

/** Même UI, mais aucune écriture dans les scores personnels ou Supabase. */
class QaSprintApplication : SprintApplication() {
    internal override val container: AppContainer by lazy {
        val database = Room.inMemoryDatabaseBuilder(this, SprintDatabase::class.java).build()
        val isolatedContext = object : android.content.ContextWrapper(this) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("ui-test-$name", mode)
        }
        object : AppContainer {
            override val records = RoomRecordsRepository(database, LegacyRecordsMigration(isolatedContext, database))
            override val accounts = object : AccountRepository {
                override val configured = false
                override val account = MutableStateFlow<PlayerAccount?>(null).asStateFlow()
                override suspend fun signIn(idToken: String, nonce: String) = error("Supabase désactivé en test UI")
                override suspend fun signOut() = Unit
            }
            override val ranking = object : RankingRepository {
                override fun observeEntries(uid: String, levelId: String) = flowOf(emptyList<LeaderboardEntry>())
                override suspend fun refresh(uid: String, levelId: String) = Unit
                override suspend fun submit(account: PlayerAccount, result: RunResult) = Unit
            }
            override val progress = object : ProgressRepository {
                override fun observe(uid: String?) = flowOf(PlayerProgress())
                override suspend fun equip(uid: String?, skin: CharacterSkin) = Unit
                override suspend fun synchronize(account: PlayerAccount) = Unit
            }
            override val sync = DefaultPlayerSyncRepository(records, accounts, ranking, progress)
            override val friends = object : FriendsRepository {
                override val incomingRequests = emptyFlow<IncomingFriendRequests>()
                override fun observe(uid: String) = flowOf(FriendsSnapshot(uid))
                override fun observeProfile(viewerUid: String, playerUid: String) = flowOf<UserProfile?>(null)
                override suspend fun refresh(uid: String) = 0
                override suspend fun execute(uid: String, command: FriendCommand) = Unit
                override suspend fun refreshProfile(viewerUid: String, playerUid: String) = Unit
                override fun liveEvents(uid: String) = emptyFlow<FriendsLiveEvent>()
            }
            override val webClientId = ""
        }
    }
}
