package io.github.vexpaer.mybp.data.salt

import android.content.Context
import com.amap.api.maps.MapsInitializer
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

/**
 * 高德搜索 SDK 的周边餐饮查询。
 * 仅在用户使用「少吃点盐」时调用；Key 缺失时上层直接走演示模式，不会请求。
 */
class AmapPoiDataSource(private val context: Context) {

    /**
     * 周边搜索「餐饮服务」，返回按距离排序的 POI；失败/超时返回 null。
     */
    suspend fun searchFoodAround(
        center: LatLon,
        radiusMeters: Int,
        page: Int = 0,
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
                        // 单条详情，v0.1.0 不用
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
 * 高德隐私合规（SDK 9.x 起强制）：用户在 App 内同意后调用一次。
 * 全部 runCatching：不同 SDK 版本方法签名略有差异，失败不阻断 App。
 */
object AmapPrivacy {
    fun ensure(context: Context) {
        runCatching { MapsInitializer.updatePrivacyShow(context, true, true) }
        runCatching { MapsInitializer.updatePrivacyAgree(context, true) }
    }
}
