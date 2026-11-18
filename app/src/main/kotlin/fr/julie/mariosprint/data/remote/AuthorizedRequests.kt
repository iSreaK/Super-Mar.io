package fr.julie.mariosprint.data.remote

import retrofit2.HttpException

/** Une seule relance sur 401 ; tokenFor vérifie l'identité et sérialise le renouvellement. */
internal suspend fun <T> AccessTokenProvider.authorized(uid: String, action: suspend (String) -> T): T {
    val token = tokenFor(uid)
    return try { action("Bearer $token") } catch (error: HttpException) {
        if (error.code() != 401) throw error
        action("Bearer " + tokenFor(uid, rejectedToken = token))
    }
}
