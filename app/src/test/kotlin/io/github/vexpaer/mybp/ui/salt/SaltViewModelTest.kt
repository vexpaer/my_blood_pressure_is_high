package io.github.vexpaer.mybp.ui.salt

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.vexpaer.mybp.core.settings.SettingsRepository
import io.github.vexpaer.mybp.data.salt.LatLon
import io.github.vexpaer.mybp.data.salt.Poi
import io.github.vexpaer.mybp.data.salt.PoiDataSource
import io.github.vexpaer.mybp.data.location.LocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private class FakePoiDataSource : PoiDataSource {
    var result: List<Poi>? = null
    var calls = 0
    var lastRadius: Int? = null
    override suspend fun searchFoodAround(center: LatLon, radiusMeters: Int, page: Int): List<Poi>? {
        calls++
        lastRadius = radiusMeters
        return result
    }
}

private class FakeLocationSource : LocationSource {
    var result: LatLon? = null
    var calls = 0
    override suspend fun currentLocation(timeoutMs: Long): LatLon? {
        calls++
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SaltViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var settings: SettingsRepository
    private lateinit var scope: CoroutineScope
    private lateinit var poi: FakePoiDataSource
    private lateinit var location: FakeLocationSource

    private val beijing = LatLon(39.909, 116.397)
    private val poiA = Poi("a", "青禾轻食沙拉", "餐饮服务;轻食", "", 39.91, 116.40, 320)
    private val poiB = Poi("b", "李记麻辣烫", "餐饮服务;麻辣烫", "", 39.92, 116.41, 1100)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        scope = CoroutineScope(UnconfinedTestDispatcher() + Job())
        val file: File = tmp.newFile("settings.preferences_pb")
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { file },
        )
        poi = FakePoiDataSource()
        location = FakeLocationSource()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        scope.cancel()
    }

    private fun vm(hasKey: Boolean = true) = SaltViewModel(settings, poi, location, hasKey = hasKey)

    private suspend fun grantAndLocate(viewModel: SaltViewModel) {
        viewModel.onScreenEntered()
        viewModel.agreePrivacy()
        viewModel.onLocationPermissionResult(true)
    }

    @Test
    fun `无 Key 进入演示模式 - 不调用定位与搜索`() = runTest(dispatcher.scheduler) {
        val viewModel = vm(hasKey = false)
        viewModel.onScreenEntered()
        assertEquals(SaltStatus.Ready, viewModel.state.first().status)
        assertEquals(4, viewModel.state.first().pois.size)
        assertEquals(0, location.calls)
        assertEquals(0, poi.calls)
    }

    @Test
    fun `用户拒绝隐私协议 - 地图数据源完全不初始化`() = runTest(dispatcher.scheduler) {
        val viewModel = vm()
        viewModel.onScreenEntered()
        viewModel.onLocationPermissionResult(true) // 权限给了，但隐私未同意
        assertEquals(0, location.calls)
        assertEquals(0, poi.calls)
        assertEquals(false, viewModel.state.first().privacyAgreed)
    }

    @Test
    fun `同意隐私且定位授权后 - 定位并搜索评分排序`() = runTest(dispatcher.scheduler) {
        location.result = beijing
        poi.result = listOf(poiB, poiA)
        val viewModel = vm()
        grantAndLocate(viewModel)
        val state = viewModel.state.first()
        assertEquals(SaltStatus.Ready, state.status)
        assertEquals(beijing, state.mapCenter)
        assertEquals(listOf("青禾轻食沙拉", "李记麻辣烫"), state.pois.map { it.poi.name })
        assertEquals(320, state.pois.first().poi.distanceMeters)
        assertTrue(state.pois.first().verdict.score > state.pois[1].verdict.score)
    }

    @Test
    fun `定位失败 - 显示友好错误且不搜索`() = runTest(dispatcher.scheduler) {
        location.result = null
        val viewModel = vm()
        grantAndLocate(viewModel)
        val state = viewModel.state.first()
        assertTrue(state.status is SaltStatus.Error)
        assertEquals(0, poi.calls)
    }

    @Test
    fun `定位成功但搜索失败 - 错误态可重试`() = runTest(dispatcher.scheduler) {
        location.result = beijing
        poi.result = null
        val viewModel = vm()
        grantAndLocate(viewModel)
        assertTrue(viewModel.state.first().status is SaltStatus.Error)

        poi.result = listOf(poiA)
        viewModel.retry()
        assertEquals(SaltStatus.Ready, viewModel.state.first().status)
        assertEquals(1, viewModel.state.first().pois.size)
    }

    @Test
    fun `附近没有餐厅 - 空态而不是错误`() = runTest(dispatcher.scheduler) {
        location.result = beijing
        poi.result = emptyList()
        val viewModel = vm()
        grantAndLocate(viewModel)
        val state = viewModel.state.first()
        assertEquals(SaltStatus.Ready, state.status)
        assertTrue(state.pois.isEmpty())
    }

    @Test
    fun `切换半径 - 用上次位置重新搜索`() = runTest(dispatcher.scheduler) {
        location.result = beijing
        poi.result = listOf(poiA)
        val viewModel = vm()
        grantAndLocate(viewModel)
        assertEquals(1000, poi.lastRadius)
        viewModel.setRadius(3000)
        assertEquals(3000, poi.lastRadius)
        assertEquals(2, poi.calls)
        assertEquals(1, location.calls) // 没有重新定位
    }

    @Test
    fun `定位权限拒绝 - 不崩溃也不请求`() = runTest(dispatcher.scheduler) {
        val viewModel = vm()
        viewModel.onScreenEntered()
        viewModel.agreePrivacy()
        viewModel.onLocationPermissionResult(false)
        assertEquals(SaltStatus.Idle, viewModel.state.first().status)
        assertNull(viewModel.state.first().mapCenter)
    }
}
