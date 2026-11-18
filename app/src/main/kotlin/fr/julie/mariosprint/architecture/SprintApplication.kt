package fr.julie.mariosprint.architecture

import android.app.Application

open class SprintApplication : Application() {
    internal open val container: AppContainer by lazy { DefaultAppContainer(this) }
}
