package com.chiya.wallswitcher.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.ui.viewmodel.MainViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

/**
 * 设置界面
 */
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentSettings by viewModel.settings.collectAsState()
    var settings by remember { mutableStateOf(currentSettings) }
    
    // 当 currentSettings 变化时更新 settings
    if (currentSettings != settings) {
        settings = currentSettings
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 标题
        Text(
            text = stringResource(id = R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 壁纸设置
        SettingsSection(title = stringResource(id = R.string.settings_wallpaper_section)) {
            // 切换方式
            SettingItem(
                title = stringResource(id = R.string.settings_switch_method),
                description = if (settings.switchMode == 0) 
                    stringResource(id = R.string.settings_switch_method_auto)
                else 
                    stringResource(id = R.string.settings_switch_method_manual)
            ) {
                Box(modifier = Modifier.width(180.dp)) {
                    SwitchModeSelector(
                        selectedMode = settings.switchMode,
                        onModeSelected = { 
                            settings = settings.copy(switchMode = it)
                            viewModel.updateSettings(settings)
                        }
                    )
                }
            }
            
            // 切换顺序
            SettingItem(
                title = stringResource(id = R.string.settings_switch_order),
                description = if (settings.switchOrder == 0) 
                    stringResource(id = R.string.settings_switch_order_random)
                else 
                    stringResource(id = R.string.settings_switch_order_sequential)
            ) {
                Box(modifier = Modifier.width(180.dp)) {
                    SwitchOrderSelector(
                        selectedOrder = settings.switchOrder,
                        onOrderSelected = { 
                            settings = settings.copy(switchOrder = it)
                            viewModel.updateSettings(settings)
                        }
                    )
                }
            }
            
            // 避免重复
            SettingItem(
                title = stringResource(id = R.string.settings_avoid_repeat),
                description = stringResource(id = R.string.settings_avoid_repeat_desc)
            ) {
                Switch(
                    checked = settings.avoidRepeat,
                    onCheckedChange = { 
                        settings = settings.copy(avoidRepeat = it)
                        viewModel.updateSettings(settings)
                    }
                )
            }
            
            // 切换间隔
            SettingItem(
                title = stringResource(id = R.string.settings_interval),
                description = ""
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 间隔值
                    IntervalValueInput(
                        value = settings.intervalValue,
                        onValueChange = { 
                            settings = settings.copy(intervalValue = it)
                            viewModel.updateSettings(settings)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 间隔单位
                    IntervalUnitSelector(
                        selectedUnit = settings.intervalUnit,
                        onUnitSelected = { 
                            settings = settings.copy(intervalUnit = it)
                            viewModel.updateSettings(settings)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示设置
        SettingsSection(title = stringResource(id = R.string.settings_display_section)) {
            // 同时设置锁屏壁纸
            SettingItem(
                title = stringResource(id = R.string.settings_set_lock_screen),
                description = stringResource(id = R.string.settings_set_lock_screen_desc)
            ) {
                Switch(
                    checked = settings.setLockScreen,
                    onCheckedChange = { 
                        settings = settings.copy(setLockScreen = it)
                        viewModel.updateSettings(settings)
                    }
                )
            }
            
            // 显示切换通知
            SettingItem(
                title = stringResource(id = R.string.settings_show_toast),
                description = stringResource(id = R.string.settings_show_toast_desc)
            ) {
                Switch(
                    checked = settings.showToast,
                    onCheckedChange = { 
                        settings = settings.copy(showToast = it)
                        viewModel.updateSettings(settings)
                    }
                )
            }
            
            // 裁剪方式
            SettingItem(
                title = stringResource(id = R.string.settings_crop_method),
                description = when (settings.cropMethod) {
                    0 -> stringResource(id = R.string.settings_crop_method_none)
                    1 -> stringResource(id = R.string.settings_crop_method_center)
                    2 -> stringResource(id = R.string.settings_crop_method_top)
                    else -> stringResource(id = R.string.settings_crop_method_bottom)
                }
            ) {
                Box(modifier = Modifier.width(180.dp)) {
                    CropMethodSelector(
                        selectedMethod = settings.cropMethod,
                        onMethodSelected = { 
                            settings = settings.copy(cropMethod = it)
                            viewModel.updateSettings(settings)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 图片质量设置
        ImageQualitySettings(viewModel)
        
        // 其他设置
        SettingsSection(title = stringResource(id = R.string.settings_advanced_section)) {
            // 启用日志记录
            SettingItem(
                title = stringResource(id = R.string.settings_enable_logging),
                description = stringResource(id = R.string.settings_enable_logging_desc)
            ) {
                Switch(
                    checked = settings.enableLogging,
                    onCheckedChange = { 
                        settings = settings.copy(enableLogging = it)
                        viewModel.updateSettings(settings)
                    }
                )
            }
            
            // 清除日志
            SettingItem(
                title = stringResource(id = R.string.settings_clear_logs),
                description = stringResource(id = R.string.settings_clear_logs_desc)
            ) {
                Button(
                    onClick = { viewModel.clearLogs() },
                    enabled = settings.enableLogging
                ) {
                    Text(text = stringResource(id = R.string.settings_clear_logs))
                }
            }
            
            // 分享日志
            SettingItem(
                title = stringResource(id = R.string.settings_share_logs),
                description = stringResource(id = R.string.settings_share_logs_desc)
            ) {
                Button(
                    onClick = { viewModel.shareLogs() },
                    enabled = settings.enableLogging
                ) {
                    Text(text = stringResource(id = R.string.settings_share_logs))
                }
            }
        }
    }
}

/**
 * 设置区域
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

/**
 * 设置项
 */
@Composable
fun SettingItem(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            content()
        }
        
        Divider(
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * 切换方式选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchModeSelector(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = if (selectedMode == 0) 
                stringResource(id = R.string.settings_switch_method_auto)
            else 
                stringResource(id = R.string.settings_switch_method_manual),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_switch_method_auto)) },
                onClick = {
                    onModeSelected(0)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_switch_method_manual)) },
                onClick = {
                    onModeSelected(1)
                    expanded = false
                }
            )
        }
    }
}

/**
 * 切换顺序选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchOrderSelector(
    selectedOrder: Int,
    onOrderSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = if (selectedOrder == 0) 
                stringResource(id = R.string.settings_switch_order_random)
            else 
                stringResource(id = R.string.settings_switch_order_sequential),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_switch_order_random)) },
                onClick = {
                    onOrderSelected(0)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_switch_order_sequential)) },
                onClick = {
                    onOrderSelected(1)
                    expanded = false
                }
            )
        }
    }
}

/**
 * 间隔值输入
 */
@Composable
fun IntervalValueInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(value.toString()) }
    var isError by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            try {
                val newValue = newText.toInt()
                if (newValue > 0) {
                    onValueChange(newValue)
                    isError = false
                } else {
                    isError = true
                }
            } catch (e: NumberFormatException) {
                isError = true
            }
        },
        label = { Text(stringResource(id = R.string.settings_interval_value)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(id = R.string.settings_interval_invalid)) }
        } else null,
        modifier = modifier
    )
}

/**
 * 间隔单位选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalUnitSelector(
    selectedUnit: Int,
    onUnitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = when (selectedUnit) {
                0 -> stringResource(id = R.string.settings_interval_unit_minute)
                1 -> stringResource(id = R.string.settings_interval_unit_hour)
                else -> stringResource(id = R.string.settings_interval_unit_day)
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.settings_interval_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_interval_unit_minute)) },
                onClick = {
                    onUnitSelected(0)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_interval_unit_hour)) },
                onClick = {
                    onUnitSelected(1)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_interval_unit_day)) },
                onClick = {
                    onUnitSelected(2)
                    expanded = false
                }
            )
        }
    }
}

/**
 * 裁剪方式选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropMethodSelector(
    selectedMethod: Int,
    onMethodSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = when (selectedMethod) {
                0 -> stringResource(id = R.string.settings_crop_method_none)
                1 -> stringResource(id = R.string.settings_crop_method_center)
                2 -> stringResource(id = R.string.settings_crop_method_top)
                else -> stringResource(id = R.string.settings_crop_method_bottom)
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_crop_method_none)) },
                onClick = {
                    onMethodSelected(0)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_crop_method_center)) },
                onClick = {
                    onMethodSelected(1)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_crop_method_top)) },
                onClick = {
                    onMethodSelected(2)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.settings_crop_method_bottom)) },
                onClick = {
                    onMethodSelected(3)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun ImageQualitySettings(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "图片质量设置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 启用图片压缩开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "启用图片压缩",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Switch(
                    checked = settings.enableImageCompression,
                    onCheckedChange = { viewModel.updateImageCompression(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 图片质量选择器（仅在启用压缩时显示）
            AnimatedVisibility(visible = settings.enableImageCompression) {
                Column {
                    Text(
                        text = "图片质量",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 质量选择单选按钮组
                    Column {
                        QualityRadioButton(
                            selected = settings.imageQuality == 0,
                            onClick = { viewModel.updateImageQuality(0) },
                            text = "高质量 (占用更多内存)"
                        )
                        
                        QualityRadioButton(
                            selected = settings.imageQuality == 1,
                            onClick = { viewModel.updateImageQuality(1) },
                            text = "平衡 (推荐)"
                        )
                        
                        QualityRadioButton(
                            selected = settings.imageQuality == 2,
                            onClick = { viewModel.updateImageQuality(2) },
                            text = "省内存 (质量较低)"
                        )
                    }
                    
                    Text(
                        text = "注意：高质量模式可能导致内存占用增加，在处理大量图片时可能出现卡顿",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // 当禁用压缩时显示提示
            AnimatedVisibility(visible = !settings.enableImageCompression) {
                Text(
                    text = "禁用压缩将使用原始图片质量，可能导致内存占用增加和加载速度变慢",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun QualityRadioButton(selected: Boolean, onClick: () -> Unit, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
} 