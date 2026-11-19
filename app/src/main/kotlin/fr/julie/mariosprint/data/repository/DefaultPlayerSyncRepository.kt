package fr.julie.mariosprint.data.repository

import fr.julie.mariosprint.domain.repository.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultPlayerSyncRepository(private val records: RecordsRepository, private val accounts: AccountRepository,
    private val ranking: RankingRepository, private val progress: ProgressRepository) : PlayerSyncRepository {
    private val mutex = Mutex()
    override suspend fun synchronize(): Boolean = mutex.withLock {
        var sent = false
        val account = accounts.account.value ?: return@withLock false
        // Relire le compte et le record après chaque acquittement : une meilleure course peut arriver pendant l'envoi.
        while (true) {
            if (accounts.account.value?.uid != account.uid) break
            val pending = records.pending(account.uid) ?: break
            ranking.submit(account, pending)
            records.acknowledge(account.uid, pending)
            sent = true
        }
        if (accounts.account.value?.uid == account.uid) progress.synchronize(account)
        sent
    }
}
