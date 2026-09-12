package io.github.vexpaer.mybp

import android.app.Application
import io.github.vexpaer.mybp.data.SettingsRepository

/** 手工依赖容器：规模小，不引入 DI 框架（依赖适度）。 */
class AppContainer(context: Application) {
    val settingsRepository = SettingsRepository(context)
}

class MyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
