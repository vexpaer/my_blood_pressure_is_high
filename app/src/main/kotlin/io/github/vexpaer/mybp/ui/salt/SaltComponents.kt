package io.github.vexpaer.mybp.ui.salt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.core.salt.SaltScorer
import io.github.vexpaer.mybp.data.salt.Poi
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.theme.ChartTokens
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens

/** 分数徽标：数字即低盐友好度（0–10），颜色分档但不单独承载信息。 */
@Composable
fun ScoreBadge(score: Double, band: SaltScorer.Band, large: Boolean = false) {
    val color = when (band) {
        SaltScorer.Band.FRIENDLY -> AppTheme.extended.saltGood
        SaltScorer.Band.NEUTRAL -> AppTheme.extended.saltMid
        SaltScorer.Band.CAUTION -> AppTheme.extended.saltBad
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = String.format("%.1f", score),
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(
                horizontal = if (large) 14.dp else 10.dp,
                vertical = if (large) 8.dp else 6.dp,
            ),
        )
    }
}

fun bandLabel(band: SaltScorer.Band): String = when (band) {
    SaltScorer.Band.FRIENDLY -> "低盐友好"
    SaltScorer.Band.NEUTRAL -> "看情况点"
    SaltScorer.Band.CAUTION -> "谨慎选择"
}

/**
 * Score Ring —— 餐厅详情的视觉中心：圆环从 0 扫到分数（约 400ms），
 * 中央是分数本体（颜色不是唯一信息，数字永远在场）。
 */
@Composable
fun ScoreRing(score: Double, band: SaltScorer.Band, modifier: Modifier = Modifier) {
    val color = when (band) {
        SaltScorer.Band.FRIENDLY -> AppTheme.extended.saltGood
        SaltScorer.Band.NEUTRAL -> AppTheme.extended.saltMid
        SaltScorer.Band.CAUTION -> AppTheme.extended.saltBad
    }
    val hairline = AppTheme.extended.hairline
    val reducedMotion = LocalReducedMotion.current
    val target = (score / 10.0).toFloat()
    val progress = remember { Animatable(if (reducedMotion) target else 0f) }

    LaunchedEffect(score) {
        if (reducedMotion) {
            progress.snapTo(target)
        } else {
            progress.snapTo(0f)
            progress.animateTo(target, tween(400, easing = MotionTokens.Easing))
        }
    }

    Box(
        modifier = modifier
            .size(92.dp)
            .semantics { contentDescription = "低盐友好度 $score 分，满分 10 分。" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = ChartTokens.RingStrokeSmall.toPx()
            val inset = stroke
            val dim = size.minDimension - inset * 2
            drawArc(
                color = hairline,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(dim, dim),
                style = Stroke(stroke, cap = StrokeCap.Butt),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(dim, dim),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            String.format("%.1f", score),
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
    }
}

@Composable
fun RadiusChips(
    radius: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(500 to "500 m", 1000 to "1 km", 3000 to "3 km").forEach { (value, label) ->
            FilterChip(
                selected = radius == value,
                onClick = { onSelect(value) },
                enabled = enabled,
                label = { Text(label) },
            )
        }
    }
}

/** 餐厅列表行：徽标 + 名称 + 类型/距离。 */
@Composable
fun RestaurantRow(poi: ScoredPoi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreBadge(score = poi.verdict.score, band = poi.verdict.band)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                poi.poi.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${typeLabel(poi.poi)} · ${distanceLabel(poi.poi.distanceMeters)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "餐饮服务;中餐厅;四川菜馆" → "四川菜馆"（截到 8 字）。 */
fun typeLabel(poi: Poi): String =
    poi.typeDescr.split(";")
        .lastOrNull { it.isNotBlank() }
        ?.take(8)
        ?.ifBlank { "餐厅" }
        ?: "餐厅"

fun distanceLabel(meters: Int): String =
    if (meters >= 1000) String.format("%.1f km", meters / 1000.0) else "$meters m"
