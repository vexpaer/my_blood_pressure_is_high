package io.github.vexpaer.mybp.ui.salt

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.components.SectionLabel
import io.github.vexpaer.mybp.ui.theme.AppTheme

/**
 * 少吃点盐：地图 + 附近餐厅 + 低盐友好度。
 * 无高德 Key → 精致的演示模式；有 Key → 隐私弹层 → 定位权限（按需）→ 地图与列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaltScreen(viewModel: SaltViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.onScreenEntered() }

    // 系统可能已授权过（上次会话），进入页面时同步一次状态
    LaunchedEffect(Unit) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        viewModel.onLocationPermissionResult(granted)
    }

    var privacyDeclined by remember { mutableStateOf(false) }
    var deniedOnce by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) deniedOnce = true
        viewModel.onLocationPermissionResult(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            "少吃点盐",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Spacer(Modifier.height(16.dp))

        if (state.hasKey) {
            // 地图：同意隐私后即初始化（Lazy init）；拿到定位前显示图面等待定位
            if (state.privacyAgreed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    SaltMapView(
                        center = state.mapCenter,
                        radius = state.radius,
                        pois = state.pois,
                        onPoiClick = viewModel::select,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (state.mapCenter == null && state.status == SaltStatus.Locating) {
                        Text(
                            "正在找你在哪…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                RadiusChips(
                    radius = state.radius,
                    enabled = state.status == SaltStatus.Ready || state.status is SaltStatus.Error,
                    onSelect = viewModel::setRadius,
                )
            } else {
                PrivacyIntroCard()
            }
        } else {
            DemoBanner()
        }

        when (val status = state.status) {
            is SaltStatus.Locating -> StatusLine("正在找你在哪…")
            is SaltStatus.Loading -> StatusLine("正在看附近的餐厅…")
            is SaltStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                StatusLine(status.message)
                TextButton(onClick = viewModel::retry) { Text("重试") }
            }
            else -> Unit
        }

        if (state.hasKey && state.privacyAgreed && !state.hasLocationPermission && state.status != SaltStatus.Loading) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (deniedOnce) "没有定位权限，可以稍后开启。" else "需要定位",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "用你的位置查附近的餐厅。只在使用这个页面时访问一次，不会保存位置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text("允许定位")
                        }
                        if (deniedOnce) {
                            FilledTonalButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null),
                                            ),
                                        )
                                    }
                                },
                                modifier = Modifier.height(44.dp),
                            ) {
                                Text("去系统设置")
                            }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            SectionLabel(if (state.hasKey) "附近餐厅" else "示例餐厅")
            Spacer(Modifier.weight(1f))
            if (state.pois.isNotEmpty()) {
                Text(
                    "${state.pois.size} 家",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.pois.forEach { scored ->
            RestaurantRow(poi = scored, onClick = { viewModel.select(scored) })
        }

        Text(
            "低盐友好度根据餐厅类型和常见菜品估算，不代表实际钠含量。",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.extended.inkFaint,
            modifier = Modifier.padding(top = 16.dp),
        )
    }

    state.selected?.let { selected ->
        RestaurantSheet(scored = selected, onDismiss = { viewModel.select(null) })
    }

    if (state.hasKey && !state.privacyAgreed && !privacyDeclined) {
        AlertDialog(
            onDismissRequest = { privacyDeclined = true },
            title = { Text("使用地图服务") },
            text = {
                Text(
                    "「少吃点盐」使用高德开放平台的地图与餐厅搜索服务。" +
                        "你的位置只用来查询附近餐厅，不会被上传或保存到任何服务器。",
                )
            },
            confirmButton = {
                Button(onClick = viewModel::agreePrivacy) { Text("同意并继续") }
            },
            dismissButton = {
                TextButton(onClick = { privacyDeclined = true }) { Text("先不用") }
            },
        )
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun PrivacyIntroCard() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("地图与附近餐厅", style = MaterialTheme.typography.titleSmall)
            Text(
                "同意使用高德地图服务后，这里会显示一张地图和附近的餐厅，" +
                    "并给每家餐厅标一个 0–10 的低盐友好度。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** 无 Key 演示模式：讲清楚怎么开启，同时用示例数据展示评分能力。 */
@Composable
private fun DemoBanner() {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_bowl),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "演示模式",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                "配置高德开放平台 Key 后，这里会显示你附近的真实餐厅和低盐友好度。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 6.dp),
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起" else "如何配置")
            }
            if (expanded) {
                Text(
                    "1. 在高德开放平台创建 Android Key（填包名 io.github.vexpaer.mybp 和应用签名的 SHA1）\n" +
                        "2. 在 GitHub 仓库的 Settings → Secrets 配置 AMAP_API_KEY\n" +
                        "3. 重新构建安装，地图即可用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
