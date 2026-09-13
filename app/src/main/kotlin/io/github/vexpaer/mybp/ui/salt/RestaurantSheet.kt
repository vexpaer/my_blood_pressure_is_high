package io.github.vexpaer.mybp.ui.salt

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.core.salt.SaltScorer
import io.github.vexpaer.mybp.ui.theme.AppTheme

/**
 * 餐厅详情：分数、理由（可解释）、这家店怎么点（推荐/谨慎）、点餐建议与免责。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RestaurantSheet(
    scored: ScoredPoi,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val verdict = scored.verdict
    val bandColor = when (verdict.band) {
        SaltScorer.Band.FRIENDLY -> AppTheme.extended.saltGood
        SaltScorer.Band.NEUTRAL -> AppTheme.extended.saltMid
        SaltScorer.Band.CAUTION -> AppTheme.extended.saltBad
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(score = verdict.score, band = verdict.band)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        scored.poi.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${typeLabel(scored.poi)} · ${distanceLabel(scored.poi.distanceMeters)}" +
                            if (scored.poi.address.isNotBlank()) " · ${scored.poi.address.take(14)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        bandLabel(verdict.band),
                        style = MaterialTheme.typography.titleSmall,
                        color = bandColor,
                    )
                }
            }

            Text(
                "为什么是这个分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                verdict.reasons.forEach { rule ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            if (rule.delta >= 0) rule.label else rule.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Row(Modifier.padding(top = 16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "来了可以点",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.extended.saltGood,
                    )
                    verdict.recommend.forEach {
                        Bullet(it, AppTheme.extended.saltGood)
                    }
                }
                Spacer(Modifier.padding(horizontal = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "尽量少碰",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.extended.saltMid,
                    )
                    verdict.caution.forEach {
                        Bullet(it, AppTheme.extended.saltMid)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "点餐建议",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(verdict.tip, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 2.dp))
                }
            }

            if (scored.poi.lat != 0.0 || scored.poi.lng != 0.0) {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:${scored.poi.lat},${scored.poi.lng}?q=${Uri.encode(scored.poi.name)}")),
                        )
                    }
                }) {
                    Text("用地图 App 导航过去")
                }
            }

            Text(
                "低盐友好度根据餐厅类型和常见菜品估算，不代表实际钠含量。",
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.extended.inkFaint,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun Bullet(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
