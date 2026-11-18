package fr.julie.mariosprint.data.remote

import fr.julie.mariosprint.domain.model.CharacterSkin

internal class SupabaseProgressSource(private val api: SupabaseApi, private val tokens: AccessTokenProvider) {
    suspend fun load(uid: String): ProgressRemote = tokens.authorized(uid) { api.progress(it) }

    suspend fun equip(uid: String, skin: CharacterSkin): ProgressRemote = tokens.authorized(uid) {
        api.equip(it, SkinRequest(skin.value))
    }

}
