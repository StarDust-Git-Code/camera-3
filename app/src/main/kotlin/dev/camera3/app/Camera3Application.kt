package dev.camera3.app

import android.app.Application
import dev.camera3.camera.CameraDiscovery
import dev.camera3.diagnostics.AppLogger
import dev.camera3.media.PhotoStore
import dev.camera3.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Camera3Application : Application() {
    lateinit var services: AppServices
        private set
    override fun onCreate() {
        super.onCreate()
        services = AppServices(this)
    }
}

class AppServices(application: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val logger = AppLogger(application, scope)
    val settings = SettingsRepository(application)
    val photoStore = PhotoStore(application)
    val discovery = CameraDiscovery(application, logger)
}
