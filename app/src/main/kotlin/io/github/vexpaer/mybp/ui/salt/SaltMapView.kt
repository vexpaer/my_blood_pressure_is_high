package io.github.vexpaer.mybp.ui.salt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import io.github.vexpaer.mybp.core.salt.SaltScorer
import io.github.vexpaer.mybp.data.salt.LatLon
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BRAND = 0xFF31684E.toInt()

private fun zoomFor(radius: Int): Float = when (radius) {
    500 -> 15.5f
    1000 -> 14.5f
    else -> 12.5f
}

/**
 * 高德地图视图（进入页面并同意隐私后才会组合进来）：
 * - 低盐友好度徽标 Marker（数字 + 小箭头，锚点在箭头尖端）
 * - 首次定位：当前位置一次轻微 pulse
 * - 搜索完成：从圆心扩散一圈雷达波（只一次）
 * - 半径切换：搜索半径圈平滑变化
 * - Marker 错峰淡入（仅前 15 个）
 * 系统关闭动画时全部直接呈现最终状态。
 */
@Composable
fun SaltMapView(
    center: LatLon?,
    radius: Int,
    pois: List<ScoredPoi>,
    onPoiClick: (ScoredPoi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reducedMotion = LocalReducedMotion.current
    val mapView = remember { MapView(context) }
    val aMap = remember { mapView.map }
    val poisState = rememberUpdatedState(pois)
    val clickState = rememberUpdatedState(onPoiClick)
    var radiusCircle by remember { mutableStateOf<Circle?>(null) }
    var locationDot by remember { mutableStateOf<Circle?>(null) }
    var pulsedFor by remember { mutableStateOf<LatLon?>(null) }
    var wavedFor by remember { mutableStateOf<Int?>(null) } // pois.size + radius 标识

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { mapView.onResume() }
                Lifecycle.Event.ON_PAUSE -> runCatching { mapView.onPause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        runCatching { mapView.onCreate(null) }
        runCatching {
            aMap.uiSettings.isZoomControlsEnabled = false
            aMap.uiSettings.isZoomGesturesEnabled = true
            aMap.uiSettings.isRotateGesturesEnabled = false
            aMap.uiSettings.isTiltGesturesEnabled = false
        }
        aMap.setOnMarkerClickListener { marker ->
            marker.snippet?.let { id ->
                poisState.value.firstOrNull { it.poi.id == id }?.let { clickState.value(it) }
            }
            true
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onDestroy() }
        }
    }

    // 相机
    LaunchedEffect(center, radius) {
        center ?: return@LaunchedEffect
        runCatching {
            aMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(center.lat, center.lng), zoomFor(radius)),
            )
        }
    }

    // 搜索半径圈：半径变化时平滑过渡
    LaunchedEffect(center, radius, reducedMotion) {
        center ?: return@LaunchedEffect
        val target = radius.toDouble()
        runCatching {
            val circle = radiusCircle
            if (circle == null) {
                radiusCircle = aMap.addCircle(
                    CircleOptions()
                        .center(LatLng(center.lat, center.lng))
                        .radius(target)
                        .strokeWidth(2f)
                        .strokeColor(0x5531684E)
                        .fillColor(0x0F31684E),
                )
            } else if (reducedMotion) {
                circle.radius = target
            } else {
                animateOverlay(circle, from = circle.radius.toFloat(), to = target.toFloat(), durationMs = MotionTokens.Emphasized)
                circle.radius = target
            }
        }
    }

    // 当前位置：一次轻微 pulse + 常驻小圆点
    LaunchedEffect(center, reducedMotion) {
        val c = center ?: return@LaunchedEffect
        if (pulsedFor == c) return@LaunchedEffect
        pulsedFor = c
        runCatching {
            val ll = LatLng(c.lat, c.lng)
            locationDot?.remove()
            locationDot = aMap.addCircle(
                CircleOptions().center(ll).radius(14.0).strokeWidth(3f).strokeColor(AColor.WHITE).fillColor(BRAND),
            )
            if (!reducedMotion) {
                val pulse = aMap.addCircle(
                    CircleOptions().center(ll).radius(8.0).strokeWidth(4f).strokeColor(0x8831684E.toInt()).fillColor(0x00000000),
                )
                animateOverlay(
                    pulse,
                    from = 8f,
                    to = 110f,
                    durationMs = 650,
                    onUpdate = { value, fraction ->
                        pulse.strokeColor = lerpAlpha(0x8831684E.toInt(), 0x0031684E, fraction)
                    },
                )
                pulse.remove()
            }
        }
    }

    // 雷达波：一次搜索结果一圈（按 (pois, radius) 标识去重）
    LaunchedEffect(pois, center, reducedMotion) {
        val c = center ?: return@LaunchedEffect
        if (pois.isEmpty()) return@LaunchedEffect
        val key = pois.size * 31 + radius
        if (wavedFor == key) return@LaunchedEffect
        wavedFor = key
        if (reducedMotion) return@LaunchedEffect
        runCatching {
            val wave = aMap.addCircle(
                CircleOptions()
                    .center(LatLng(c.lat, c.lng))
                    .radius(30.0)
                    .strokeWidth(5f)
                    .strokeColor(0x6631684E)
                    .fillColor(0x00000000),
            )
            animateOverlay(
                wave,
                from = 30f,
                to = radius.toFloat(),
                durationMs = 700,
                onUpdate = { value, fraction ->
                    wave.strokeColor = lerpAlpha(0x6631684E, 0x0031684E, fraction)
                },
            )
            wave.remove()
        }
    }

    // Markers：箭头徽标 + 错峰淡入（前 15 个）
    LaunchedEffect(pois, reducedMotion) {
        runCatching {
            aMap.clear()
            radiusCircle = null
            locationDot = null
            val markers = pois.map { scored ->
                val poi = scored.poi
                val marker = aMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(poi.lat, poi.lng))
                        .icon(BitmapDescriptorFactory.fromBitmap(scoreBadgeBitmap(scored)))
                        .anchor(0.5f, 1f),
                )
                marker.snippet = poi.id
                marker
            }
            // 重画常驻覆盖层（clear 会清掉）
            center?.let { c ->
                radiusCircle = aMap.addCircle(
                    CircleOptions()
                        .center(LatLng(c.lat, c.lng))
                        .radius(radius.toDouble())
                        .strokeWidth(2f)
                        .strokeColor(0x5531684E)
                        .fillColor(0x0F31684E),
                )
                locationDot = aMap.addCircle(
                    CircleOptions()
                        .center(LatLng(c.lat, c.lng))
                        .radius(14.0)
                        .strokeWidth(3f)
                        .strokeColor(AColor.WHITE)
                        .fillColor(BRAND),
                )
            }
            markers.take(15).forEachIndexed { index, marker ->
                if (reducedMotion) {
                    marker.alpha = 1f
                } else {
                    marker.alpha = 0f
                    launch {
                        delay(MotionTokens.Stagger * index.toLong())
                        runCatching { marker.alpha = 1f }
                    }
                }
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** 0xAARRGGBB 从 from 到 to 的线性插值（用于波纹淡出）。 */
private fun lerpAlpha(from: Int, to: Int, fraction: Float): Int {
    val fa = (from ushr 24) and 0xFF
    val ta = (to ushr 24) and 0xFF
    val a = (fa + (ta - fa) * fraction.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
    return (a shl 24) or (from and 0x00FFFFFF)
}

/**
 * 地图覆盖层的一次性数值动画（ValueAnimator，主线程，finite）。
 * onUpdate 可拿到当前值与归一化进度。
 */
private fun animateOverlay(
    circle: Circle,
    from: Float,
    to: Float,
    durationMs: Int,
    onUpdate: ((value: Float, fraction: Float) -> Unit)? = null,
) {
    runCatching {
        android.animation.ValueAnimator.ofFloat(from, to).apply {
            this.duration = durationMs.toLong()
            interpolator = android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                circle.radius = value.toDouble()
                onUpdate?.invoke(value, animator.animatedFraction)
            }
            start()
        }
    }
}

/** 低盐友好度徽标：圆角胶囊 + 分数 + 小箭头，固定配色保证图面可读。 */
private fun scoreBadgeBitmap(scored: ScoredPoi): Bitmap {
    val colorLong = when (scored.verdict.band) {
        SaltScorer.Band.FRIENDLY -> 0xFF3E7C59
        SaltScorer.Band.NEUTRAL -> 0xFF9A7222
        SaltScorer.Band.CAUTION -> 0xFFBB4A32
    }
    val text = String.format("%.1f", scored.verdict.score)
    val w = 100
    val h = 68
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorLong.toInt()
        style = Paint.Style.FILL
    }
    // 胶囊主体
    canvas.drawRoundRect(RectF(4f, 2f, w - 4f, 50f), 16f, 16f, paint)
    // 小箭头
    val tail = Path().apply {
        moveTo(w / 2f - 9f, 46f)
        lineTo(w / 2f + 9f, 46f)
        lineTo(w / 2f, 64f)
        close()
    }
    canvas.drawPath(tail, paint)
    paint.color = AColor.WHITE
    paint.textSize = 30f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val baseline = 26f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, w / 2f, baseline, paint)
    return bitmap
}
