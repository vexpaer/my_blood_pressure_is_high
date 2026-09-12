package io.github.vexpaer.mybp.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SettingsRepositoryTest {

    @Test
    fun `设置持久化 - 写入后重开仍可读出，恢复默认生效`() = runTest {
        val dir = Files.createTempDirectory("settings-test").toFile()
        val file = File(dir, "settings.preferences_pb")

        // 第一次“启动”：写入改名、主题与 onboarding
        val scope1 = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val store1 = PreferenceDataStoreFactory.create(scope = scope1) { file }
        val repo1 = SettingsRepository(store1)
        repo1.setTabName(0, "  早点睡  ")
        repo1.setThemeMode(ThemeMode.DARK)
        repo1.setOnboardingDone()
        store1.data.first() // 确保写盘完成
        scope1.cancel()

        // 第二次“启动”：全部读回
        val scope2 = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        val store2 = PreferenceDataStoreFactory.create(scope = scope2) { file }
        val repo2 = SettingsRepository(store2)
        assertEquals("早点睡", repo2.tabNames.first()[0])
        assertEquals(ThemeMode.DARK, repo2.themeMode.first())
        assertTrue(repo2.onboardingDone.first())

        // 恢复默认名称后，存储里的键被清掉
        repo2.resetTabNames()
        assertEquals(listOf<String?>(null, null, null, null), repo2.tabNames.first())
        // 主题不受影响
        assertEquals(ThemeMode.DARK, repo2.themeMode.first())
        scope2.cancel()
    }
}
