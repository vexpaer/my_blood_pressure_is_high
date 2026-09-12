package io.github.vexpaer.mybp.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.SystemClock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import io.github.vexpaer.mybp.data.salt.LatLon

/**
 * 系统定位（LocationManager 单次定位）：
 * 不引入额外定位 SDK，减少依赖与后台行为；只在用户进入「少吃点盐」并主动触发时调用。
 * 优先返回 10 分钟内的 lastKnownLocation，否则监听单次更新，超时返回 null。
 */
class FrameworkLocationSource(private val context: Context) {

    @SuppressLint("MissingPermission") // 调用方在 UI 层确认权限后才调用
    suspend fun currentLocation(timeoutMs: Long = 12_000L): LatLon? = withTimeoutOrNull(timeoutMs) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withTimeoutOrNull null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        // 先看最近一次已知位置（10 分钟内），够新就直接用，省一次定位等待
        val fresh = providers.firstNotNullOfOrNull { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()
        }?.takeIf { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos <= FRESH_NANOS }
        if (fresh != null) return@withTimeoutOrNull LatLon(fresh.latitude, fresh.longitude)
        if (providers.isEmpty()) return@withTimeoutOrNull null

        suspendCancellableCoroutine { cont ->
            val listener = android.location.LocationListener { loc ->
                if (cont.isActive) cont.resume(LatLon(loc.latitude, loc.longitude), null)
            }
            try {
                providers.forEach { lm.requestSingleUpdate(it, listener, null) }
            } catch (se: SecurityException) {
                if (cont.isActive) cont.resume(null, null)
                return@suspendCancellableCoroutine
            }
            cont.invokeOnCancellation {
                runCatching { providers.forEach { lm.removeUpdates(listener) } }
            }
        }
    }

    private companion object {
        val FRESH_NANOS = TimeUnit.MINUTES.toNanos(10)
    }
}
