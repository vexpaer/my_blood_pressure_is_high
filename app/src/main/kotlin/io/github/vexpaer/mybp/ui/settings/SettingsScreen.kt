package io.github.vexpaer.mybp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vexpaer.mybp.BuildConfig
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.core.settings.TabNames
import io.github.vexpaer.mybp.core.settings.ThemeMode
import io.github.vexpaer.mybp.ui.MainViewModel
import io.github.vexpaer.mybp.ui.components.SectionLabel
import io.github.vexpaer.mybp.ui.nav.Tab
import io.github.vexpaer.mybp.ui.theme.AppTheme

private const val REPO_URL = "https://github.com/vexpaer/my_blood_pressure_is_high"

/** 设置：改 Tab 名称、主题、关于与隐私说明。 */
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val rawNames by viewModel.rawTabNames.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            "设置",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )

        // —— 底部导航名称 ——
        SectionLabel("底部导航名称")
        Text(
            "改成你顺口的叫法，最多 6 个字；留空就用默认名称。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Tab.entries.forEachIndexed { index, tab ->
            NameField(
                index = index,
                iconRes = tab.iconRes,
                placeholder = tab.defaultName,
                value = rawNames.getOrElse(index) { null },
                onSave = { viewModel.setTabName(index, it) },
            )
        }
        val isDefault = TabNames.areDefaults(rawNames)
        TextButton(
            onClick = viewModel::resetTabNames,
            enabled = !isDefault,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_undo),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("恢复默认名称")
        }

        // —— 主题 ——
        SectionLabel("主题")
        ThemeRow("跟随系统", themeMode == ThemeMode.SYSTEM) { viewModel.setThemeMode(ThemeMode.SYSTEM) }
        ThemeRow("浅色", themeMode == ThemeMode.LIGHT) { viewModel.setThemeMode(ThemeMode.LIGHT) }
        ThemeRow("深色", themeMode == ThemeMode.DARK) { viewModel.setThemeMode(ThemeMode.DARK) }

        // —— 关于 ——
        SectionLabel("关于")
        Spacer(Modifier.height(4.dp))
        Text("我有高血压", style = MaterialTheme.typography.titleMedium)
        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { uriHandler.openUri(REPO_URL) }) {
            Text("GitHub 仓库")
        }

        SectionLabel("隐私")
        PrivacyLine("睡眠估算、运动记录与设置只保存在这台手机上。没有账号，没有上传。")
        PrivacyLine("「少吃点盐」只在你使用它的时候访问一次定位，用于查找附近餐厅。")
        PrivacyLine("不收集、不上传、不跟踪。")

        Spacer(Modifier.height(32.dp))
        Text(
            "「我有高血压」不是医疗诊断 App，也不能替代专业医疗建议。它只是提醒你：少盐、早睡、动一动。",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.extended.inkFaint,
        )
    }
}

@Composable
private fun NameField(
    index: Int,
    iconRes: Int,
    placeholder: String,
    value: String?,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(index) { mutableStateOf(value ?: "") }
    var wasFocused by remember { mutableStateOf(false) }
    TextField(
        value = text,
        onValueChange = { text = it.take(TabNames.MAX_LENGTH) },
        placeholder = { Text(placeholder, color = AppTheme.extended.inkFaint) },
        leadingIcon = {
            Icon(
                ImageVector.vectorResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = AppTheme.extended.hairline,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { st ->
                if (wasFocused && !st.isFocused) onSave(text)
                wasFocused = st.isFocused
            },
    )
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrivacyLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
