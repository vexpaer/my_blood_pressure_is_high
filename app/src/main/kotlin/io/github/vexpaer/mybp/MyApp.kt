package io.github.vexpaer.mybp

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import io.github.vexpaer.mybp.core.settings.SettingsRepository
import io.github.vexpaer.mybp.data.db.AppDatabase
import io.github.vexpaer.mybp.data.location.FrameworkLocationSource
import io.github.vexpaer.mybp.data.move.ExerciseRepository
import io.github.vexpaer.mybp.data.salt.AmapPoiDataSource
import io.github.vexpaer.mybp.data.sleep.SleepRepository
import io.github.vexpaer.mybp.data.usage.UsageStatsSource

/** 手工依赖容器：规模小，不引入 DI 框架（依赖适度）。 */
class AppContainer(context: Application) {
    val settingsRepository = SettingsRepository(context.settingsStore)

    val database: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "mybp.db")
        .fallbackToDestructiveMigration()
        .build()

    val sleepRepository = SleepRepository(database, UsageStatsSource(context))

    val amapPoiDataSource = AmapPoiDataSource(context)
    val locationSource = FrameworkLocationSource(context)

    val exerciseRepository = ExerciseRepository(database)
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
