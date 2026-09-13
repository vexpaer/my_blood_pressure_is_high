package io.github.vexpaer.mybp.data.salt

import android.content.Context
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** 一次餐厅搜索结果里的一条 POI。 */
data class Poi(
    val id: String,
    val name: String,
    val typeDescr: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Int,
)

data class LatLon(val lat: Double, val lng: Double)

/** 周边餐饮搜索的数据源抽象（便于用假实现测试 ViewModel）。 */
interface PoiDataSource {
    /** 失败/超时返回 null；成功但附近没有餐厅返回空列表。 */
    suspend fun searchFoodAround(center: LatLon, radiusMeters: Int, page: Int = 0): List<Poi>?
}

/**
 * 高德搜索 SDK 的周边餐饮查询。
 * 仅在用户同意隐私说明并进入「少吃点盐」时调用；Key 缺失时上层走演示模式。
 */
class AmapPoiDataSource(private val context: Context) : PoiDataSource {

    override suspend fun searchFoodAround(
        center: LatLon,
        radiusMeters: Int,
        page: Int,
    ): List<Poi>? = withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine { cont ->
            runCatching {
                AmapPrivacy.ensure(context)
                val query = PoiSearch.Query("", "餐饮服务")
                query.pageNum = page
                query.pageSize = PAGE_SIZE
                val search = PoiSearch(context, query)
                search.bound = PoiSearch.SearchBound(LatLonPoint(center.lat, center.lng), radiusMeters)
                search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                        if (cont.isActive) {
                            // rCode == 1000 才是成功；失败/无 Key/隐私未同意等一律按 null 处理
                            val pois = if (rCode == 1000) {
                                result?.pois.orEmpty().mapNotNull { item ->
                                    val point = item.latLonPoint ?: return@mapNotNull null
                                    Poi(
                                        id = item.poiId ?: item.title ?: return@mapNotNull null,
                                        name = item.title ?: "",
                                        typeDescr = item.typeDes ?: "",
                                        address = item.snippet ?: "",
                                        lat = point.latitude,
                                        lng = point.longitude,
                                        distanceMeters = item.distance,
                                    )
                                }
                            } else {
                                null
                            }
                            cont.resume(pois, null)
                        }
                    }

                    override fun onPoiItemSearched(item: com.amap.api.services.core.PoiItem?, rCode: Int) {
                        // 单条详情，v0.1.x 不用
                    }
                })
                search.searchPOIAsyn()
            }.onFailure {
                if (cont.isActive) cont.resume(null, null)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 25
    }
}

/**
 * 高德隐私合规（按当前 SDK 版本要求的官方 API，在创建任何 MapView / PoiSearch 之前调用）：
 * - 地图 SDK：MapsInitializer.updatePrivacyShow / updatePrivacyAgree
 * - 搜索 SDK：ServiceSettings.updatePrivacyShow / updatePrivacyAgree
 * 用户在 App 内的隐私弹层点「同意并继续」之后才会调用。
 */
object AmapPrivacy {
    fun ensure(context: Context) {
        runCatching { MapsInitializer.updatePrivacyShow(context, true, true) }
        runCatching { MapsInitializer.updatePrivacyAgree(context, true) }
        runCatching { ServiceSettings.updatePrivacyShow(context, true, true) }
        runCatching { ServiceSettings.updatePrivacyAgree(context, true) }
    }
}
