package ir.coffevista.vista_native

import android.app.Application
import ir.coffevista.vista_native.core.di.AppContainer

class VistaApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}
