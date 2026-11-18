package fr.julie.mariosprint.ui.formatting

import java.util.Locale

internal fun formatRaceTime(timeMs: Long?): String = if (timeMs == null) "—:—.———" else
    String.format(Locale.ROOT, "%02d:%02d.%03d", timeMs / 60_000, timeMs / 1000 % 60, timeMs % 1000)
