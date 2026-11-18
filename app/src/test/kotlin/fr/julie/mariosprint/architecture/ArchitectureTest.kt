package fr.julie.mariosprint.architecture

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Garde les frontières demandées par le cours lors des futures modifications. */
class ArchitectureTest {
    private val source = File("src/main/kotlin/fr/julie/mariosprint")
    @Test fun domainHasNoPlatformDependencies() {
        val forbidden = Regex("import (android[.x]|com.google.firebase|io.github.jan.supabase|retrofit2|okhttp3|fr.julie.mariosprint.(data|ui|game)[.])")
        source.resolve("domain").walkTopDown().filter { it.extension == "kt" }.forEach {
            assertFalse(it.path, forbidden.containsMatchIn(it.readText()))
        }
    }
    @Test fun screensCannotAccessDataSourcesOrViewModels() {
        val forbidden = Regex("import .*([Vv]iew[Mm]odel|[.]data[.]|[.]repository[.])")
        source.resolve("ui/screens").walkTopDown().filter { it.extension == "kt" }.forEach {
            assertFalse(it.path, forbidden.containsMatchIn(it.readText()))
        }
    }
    @Test fun productionDoesNotUseBlockingCoroutineOrDatabaseEscapes() {
        source.walkTopDown().filter { it.extension == "kt" }.forEach {
            val text = it.readText()
            listOf("runBlocking", "GlobalScope", "allowMainThreadQueries", "fallbackToDestructiveMigration").forEach { forbidden ->
                assertFalse(it.path + ": " + forbidden, text.contains(forbidden))
            }
        }
    }
}
