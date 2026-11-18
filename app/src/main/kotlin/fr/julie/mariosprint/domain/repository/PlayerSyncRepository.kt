package fr.julie.mariosprint.domain.repository

interface PlayerSyncRepository {
    /** Retourne true si un temps a été envoyé ; aucun polling ou réessai infini. */
    suspend fun synchronize(): Boolean
}
