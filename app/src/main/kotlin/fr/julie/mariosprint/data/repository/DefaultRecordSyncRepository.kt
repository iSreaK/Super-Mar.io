package fr.julie.mariosprint.data.repository

import fr.julie.mariosprint.domain.repository.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultRecordSyncRepository(private val records: RecordsRepository, private val accounts: AccountRepository,
    private val ranking: RankingRepository) : RecordSyncRepository {
    private val mutex = Mutex()
    override suspend fun synchronize(): Boolean = mutex.withLock {
        var sent = false
        // Relire le compte et le record après chaque acquittement : une meilleure course peut arriver pendant l'envoi.
        while (true) {
            val account = accounts.account.value ?: break
            val pending = records.pending(account.uid) ?: break
            ranking.submit(account, pending)
            records.acknowledge(account.uid, pending)
            sent = true
        }
        sent
    }
}
