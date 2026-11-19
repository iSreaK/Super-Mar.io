package fr.julie.mariosprint.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@Serializable
internal class GoogleTokenRequest(@SerialName("id_token") val idToken: String, val nonce: String, val provider: String = "google")
@Serializable internal class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
@Serializable internal class AuthUser(val id: String, @SerialName("user_metadata") val metadata: JsonObject = JsonObject(emptyMap()))
/** Classes sans toString de données : les jetons ne doivent jamais apparaître dans les logs. */
@Serializable internal class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long = 0,
    val user: AuthUser,
)
@Serializable internal data class LevelRequest(@SerialName("p_level_id") val levelId: String)
@Serializable internal data class RunRequest(@SerialName("p_level_id") val levelId: String,
    @SerialName("p_level_version") val levelVersion: Int,
    @SerialName("p_time_ms") val timeMs: Long, @SerialName("p_coins") val coins: Int)
@Serializable internal data class RankingRemote(val uid: String,
    @SerialName("display_name") val displayName: String, @SerialName("time_ms") val timeMs: Long, val coins: Int)
@Serializable internal data class ProgressRemote(
    @SerialName("completed_levels") val completedLevels: List<String>,
    @SerialName("equipped_skin") val equippedSkin: String,
)
@Serializable internal data class SkinRequest(@SerialName("p_skin_id") val skinId: String)

internal interface SupabaseApi {
    @POST("auth/v1/token?grant_type=id_token") suspend fun signIn(@Body request: GoogleTokenRequest): AuthSession
    @POST("auth/v1/token?grant_type=refresh_token") suspend fun refresh(@Body request: RefreshRequest): AuthSession
    @POST("auth/v1/logout?scope=local") suspend fun logout(@Header("Authorization") authorization: String)
    @POST("rest/v1/rpc/get_leaderboard") suspend fun leaderboard(@Header("Authorization") authorization: String,
        @Body request: LevelRequest): List<RankingRemote>
    @POST("rest/v1/rpc/submit_run") suspend fun submit(@Header("Authorization") authorization: String, @Body request: RunRequest)
    @GET("rest/v1/rpc/get_player_progress") suspend fun progress(@Header("Authorization") authorization: String): ProgressRemote
    @POST("rest/v1/rpc/equip_skin") suspend fun equip(@Header("Authorization") authorization: String,
        @Body request: SkinRequest): ProgressRemote
    // Cette RPC assure aussi le profil : GET imposerait une transaction SQL en lecture seule.
    @Headers("Content-Type: application/json")
    @POST("rest/v1/rpc/get_friend_snapshot") suspend fun friends(@Header("Authorization") authorization: String): FriendsSnapshotRemote
    @POST("rest/v1/rpc/friend_action") suspend fun friendAction(@Header("Authorization") authorization: String,
        @Body request: FriendActionRequest): FriendsSnapshotRemote
    @POST("rest/v1/rpc/get_social_profile") suspend fun socialProfile(@Header("Authorization") authorization: String,
        @Body request: UserProfileRequest): UserProfileRemote
}
internal object SupabaseClient {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun create(url: String, publishableKey: String): SupabaseApi {
        val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS).retryOnConnectionFailure(false)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("apikey", publishableKey).build()) }
            .build()
        return Retrofit.Builder().baseUrl(url.trimEnd('/') + "/").client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(SupabaseApi::class.java)
    }
}
internal interface AccessTokenProvider {
    suspend fun tokenFor(uid: String, rejectedToken: String? = null): String
}
