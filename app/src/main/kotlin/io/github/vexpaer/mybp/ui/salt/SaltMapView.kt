package io.github.vexpaer.mybp.ui.salt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import io.github.vexpaer.mybp.data.salt.LatLon

private fun zoomFor(radius: Int): Float = when (radius) {
    500 -> 15.5f
    1000 -> 14.5f
    else -> 12.5f
}

/**
 * 高德地图视图 + 分数徽标 Marker。
 * 地图颜色与 App 主题解耦：Marker 用固定的分档色（浅色系）保证在图面上可读。
 */
@Composable
fun SaltMapView(
    center: LatLon,
    radius: Int,
    pois: List<ScoredPoi>,
    onPoiClick: (ScoredPoi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val aMap = remember { mapView.map }
    val poisState = rememberUpdatedState(pois)
    val clickState = rememberUpdatedState(onPoiClick)

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

    LaunchedEffect(center, radius) {
        runCatching {
            aMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(center.lat, center.lng), zoomFor(radius)),
            )
        }
    }

    LaunchedEffect(pois) {
        runCatching {
            aMap.clear()
            pois.forEach { scored ->
                val poi = scored.poi
                val marker: Marker = aMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(poi.lat, poi.lng))
                        .icon(BitmapDescriptorFactory.fromBitmap(scoreBadgeBitmap(scored)))
                        .anchor(0.5f, 0.5f),
                )
                marker.snippet = poi.id
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** 在地图上的低盐友好度徽标（圆角胶囊 + 分数），固定配色。 */
private fun scoreBadgeBitmap(scored: ScoredPoi): Bitmap {
    val colorLong = when (scored.verdict.band) {
        io.github.vexpaer.mybp.core.salt.SaltScorer.Band.FRIENDLY -> 0xFF3E7C59
        io.github.vexpaer.mybp.core.salt.SaltScorer.Band.NEUTRAL -> 0xFF9A7222
        io.github.vexpaer.mybp.core.salt.SaltScorer.Band.CAUTION -> 0xFFBB4A32
    }
    val text = String.format("%.1f", scored.verdict.score)
    val w = 96
    val h = 56
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = colorLong.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(RectF(2f, 2f, w - 2f, h - 2f), 14f, 14f, paint)
    paint.color = AColor.WHITE
    paint.textSize = 30f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    val baseline = h / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, w / 2f, baseline, paint)
    return bitmap
}
