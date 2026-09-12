package io.github.vexpaer.mybp.ui.salt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.vexpaer.mybp.BuildConfig
import io.github.vexpaer.mybp.MyApp
import io.github.vexpaer.mybp.core.settings.SettingsRepository
import io.github.vexpaer.mybp.core.salt.SaltScorer
import io.github.vexpaer.mybp.data.location.FrameworkLocationSource
import io.github.vexpaer.mybp.data.salt.AmapPoiDataSource
import io.github.vexpaer.mybp.data.salt.LatLon
import io.github.vexpaer.mybp.data.salt.Poi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 一家餐厅 + 它的低盐友好度结论。 */
data class ScoredPoi(val poi: Poi, val verdict: SaltScorer.Verdict)

sealed interface SaltStatus {
    data object Idle : SaltStatus
    data object Locating : SaltStatus
    data object Loading : SaltStatus
    data object Ready : SaltStatus
    data class Error(val message: String) : SaltStatus
}

data class SaltUiState(
    val hasKey: Boolean = false,
    val privacyAgreed: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val radius: Int = 1000,
    val status: SaltStatus = SaltStatus.Idle,
    val pois: List<ScoredPoi> = emptyList(),
    val selected: ScoredPoi? = null,
    val mapCenter: LatLon? = null,
)

/**
 * 少吃点盐：定位（按需）→ 高德周边餐饮 → 本地低盐评分。
 * 没有 Key 时进入演示模式：App 完全可用，用示例餐厅展示能力。
 */
class SaltViewModel(
    private val settings: SettingsRepository,
    private val poiSource: AmapPoiDataSource,
    private val locationSource: FrameworkLocationSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SaltUiState(hasKey = BuildConfig.AMAP_API_KEY.isNotBlank()))
    val state: StateFlow<SaltUiState> = _state.asStateFlow()

    private var lastLocation: LatLon? = null
    private var searchJob: Job? = null

    /** 首次进入页面：读取隐私同意状态；已同意且有 Key 就直接开始。 */
    fun onScreenEntered() {
        viewModelScope.launch {
            val agreed = settings.saltPrivacyAgreed.first()
            _state.update { it.copy(privacyAgreed = agreed) }
            if (demoMode) {
                _state.update { it.copy(status = SaltStatus.Ready, pois = demoPois) }
            } else if (agreed) {
                maybeLocate()
            }
        }
    }

    /** 用户在隐私弹层点了「同意并继续」。 */
    fun agreePrivacy() {
        viewModelScope.launch {
            settings.setSaltPrivacyAgreed()
            _state.update { it.copy(privacyAgreed = true) }
            if (demoMode) {
                _state.update { it.copy(status = SaltStatus.Ready, pois = demoPois) }
            } else {
                maybeLocate()
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) maybeLocate()
    }

    fun setRadius(radius: Int) {
        _state.update { it.copy(radius = radius) }
        lastLocation?.let { search(it) } ?: maybeLocate()
    }

    fun retry() {
        lastLocation = null
        maybeLocate()
    }

    fun select(poi: ScoredPoi?) {
        _state.update { it.copy(selected = poi) }
    }

    private val demoMode get() = !_state.value.hasKey

    private fun maybeLocate() {
        if (!_state.value.hasLocationPermission) return
        _state.update { it.copy(status = SaltStatus.Locating) }
        viewModelScope.launch {
            val location = locationSource.currentLocation()
            if (location == null) {
                _state.update {
                    it.copy(status = SaltStatus.Error("没拿到定位。打开系统定位服务后点「重试」。"))
                }
            } else {
                lastLocation = location
                search(location)
            }
        }
    }

    private fun search(center: LatLon) {
        searchJob?.cancel()
        _state.update { it.copy(status = SaltStatus.Loading) }
        searchJob = viewModelScope.launch {
            val radius = _state.value.radius
            val pois = poiSource.searchFoodAround(center, radius)
            val scored = pois
                ?.map { ScoredPoi(it, SaltScorer.score(it.name, it.typeDescr)) }
                ?.sortedBy { it.poi.distanceMeters }
            _state.update {
                it.copy(
                    status = if (pois == null) SaltStatus.Error("附近的餐厅加载失败了，稍后再试。") else SaltStatus.Ready,
                    pois = scored.orEmpty(),
                    mapCenter = center,
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApp
                SaltViewModel(
                    settings = app.container.settingsRepository,
                    poiSource = app.container.amapPoiDataSource,
                    locationSource = app.container.locationSource,
                )
            }
        }

        // 演示数据：没有配置高德 Key 时展示（评分用真实引擎计算）
        private val demoPois = listOf(
            Poi("demo-1", "青禾轻食沙拉", "餐饮服务;轻食;沙拉", "示例门店 · 配置 Key 后看真实的", 0.0, 0.0, 320),
            Poi("demo-2", "老王粥铺", "餐饮服务;中餐厅;粥粉店", "示例门店 · 配置 Key 后看真实的", 0.0, 0.0, 540),
            Poi("demo-3", "清蒸海鲜坊", "餐饮服务;中餐厅;海鲜酒楼", "示例门店 · 配置 Key 后看真实的", 0.0, 0.0, 920),
            Poi("demo-4", "李记麻辣烫", "餐饮服务;中餐厅;麻辣烫", "示例门店 · 配置 Key 后看真实的", 0.0, 0.0, 1100),
        ).map { ScoredPoi(it, SaltScorer.score(it.name, it.typeDescr)) }
    }
}
