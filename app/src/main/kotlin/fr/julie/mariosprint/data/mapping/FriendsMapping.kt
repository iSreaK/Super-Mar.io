package fr.julie.mariosprint.data.mapping

import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.domain.model.*

internal fun FriendsCacheWithRelations?.toDomain(uid: String): FriendsSnapshot {
    if (this == null) return FriendsSnapshot(uid)
    require(cache.viewerUid == uid && relations.all { it.viewerUid == uid })
    return FriendsSnapshot(uid, cache.friendCode, relations.map {
        Friendship(it.relationId, SocialPlayer(it.peerUid, it.displayName, CharacterSkin.find(it.skinId) ?: CharacterSkin.LUIGI),
            when (it.status) {
                "incoming" -> FriendshipStatus.INCOMING
                "outgoing" -> FriendshipStatus.OUTGOING
                "friend" -> FriendshipStatus.FRIEND
                else -> throw FriendsException(FriendsFailure.LOCAL)
            })
    }, cache.refreshedAtMs)
}

internal fun List<SocialProfileRow>.toDomain(viewerUid: String, playerUid: String): UserProfile? {
    val profile = firstOrNull()?.profile ?: return null
    require(all { it.profile.viewerUid == viewerUid && it.profile.profileUid == playerUid })
    return UserProfile(SocialPlayer(playerUid, profile.displayName, CharacterSkin.find(profile.skinId) ?: CharacterSkin.LUIGI),
        profile.friendCode, mapNotNull { row -> row.levelId?.let { id -> row.timeMs?.let { ProfileRecord(id, it) } } })
}
