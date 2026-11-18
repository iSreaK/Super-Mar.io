package fr.julie.mariosprint.architecture

import android.content.Context
import androidx.room.Room
import fr.julie.mariosprint.BuildConfig
import fr.julie.mariosprint.data.local.*
import fr.julie.mariosprint.data.remote.*
import fr.julie.mariosprint.data.repository.*
import fr.julie.mariosprint.domain.repository.*

internal interface AppContainer {
    val records: RecordsRepository
    val accounts: AccountRepository
    val ranking: RankingRepository
    val sync: PlayerSyncRepository
    val progress: ProgressRepository
    val friends: FriendsRepository
    val webClientId: String
}
internal class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val database by lazy { Room.databaseBuilder(appContext, SprintDatabase::class.java, "mario-sprint.db")
        .addMigrations(DatabaseMigrations.FROM_1_TO_2, DatabaseMigrations.FROM_2_TO_3).build() }
    private val api by lazy { SupabaseClient.create(BuildConfig.SUPABASE_URL.ifBlank { "https://unconfigured.supabase.co" }, BuildConfig.SUPABASE_PUBLISHABLE_KEY) }
    override val records by lazy { RoomRecordsRepository(database, LegacyRecordsMigration(appContext, database)) }
    override val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    override val accounts by lazy { SupabaseAccountRepository(api, EncryptedSessionStore(appContext), BuildConfig.SUPABASE_CONFIGURED) }
    override val ranking by lazy { CachedRankingRepository(database, SupabaseRankingSource(api, accounts), accounts) }
    override val progress by lazy { RoomProgressRepository(database, SupabaseProgressSource(api, accounts)) }
    override val sync by lazy { DefaultPlayerSyncRepository(records, accounts, ranking, progress) }
    override val friends by lazy {
        CachedFriendsRepository(RoomFriendsLocalStore(database), SupabaseFriendsSource(api, accounts),
            SupabaseFriendsRealtimeSource(BuildConfig.SUPABASE_URL.ifBlank { "https://unconfigured.supabase.co" },
                BuildConfig.SUPABASE_PUBLISHABLE_KEY, accounts), accounts)
    }
}
