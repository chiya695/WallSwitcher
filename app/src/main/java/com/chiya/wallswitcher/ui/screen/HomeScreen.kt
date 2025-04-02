package com.chiya.wallswitcher.ui.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.data.model.SwitchMode
import com.chiya.wallswitcher.ui.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.chiya.wallswitcher.util.LogUtils
import android.os.Build

/**
 * 主页界面
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val folderName by viewModel.folderName.collectAsState()
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val loading by viewModel.loading.collectAsState()
    
    // 存储权限
    val permissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    LaunchedEffect(Unit) {
        // LogUtils.log("HomeScreen: 检查权限")
        if (!permissionState.status.isGranted) {
            LogUtils.log("HomeScreen: 请求权限")
            permissionState.launchPermissionRequest()
        } else {
            LogUtils.log("HomeScreen: 权限检查已通过")
        }
    }
    
    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 获取持久权限
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            // 加载壁纸
            viewModel.loadWallpapersFromUri(uri)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(id = R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(id = R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 文件夹选择卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (permissionState.status.isGranted) {
                            folderPickerLauncher.launch(null)
                        } else {
                            Toast.makeText(
                                context,
                                R.string.toast_permission_required,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.home_select_folder))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(
                        id = R.string.home_current_folder,
                        if (folderName.isEmpty()) stringResource(id = R.string.home_no_folder_selected) else folderName
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = R.string.home_image_count, wallpapers.size)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 壁纸切换卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.switchRandomWallpaper() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = wallpapers.isNotEmpty() && !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(text = stringResource(id = R.string.home_switch_now))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(
                        id = R.string.home_service_status,
                        if (serviceRunning) stringResource(id = R.string.home_service_running)
                        else stringResource(id = R.string.home_service_stopped)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 服务控制按钮
                if (settings.switchMode == SwitchMode.SERVICE.ordinal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.startWallpaperService() },
                            enabled = !serviceRunning && wallpapers.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(id = R.string.home_start_service))
                        }
                        
                        Spacer(modifier = Modifier.padding(8.dp))
                        
                        OutlinedButton(
                            onClick = { viewModel.stopWallpaperService() },
                            enabled = serviceRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(id = R.string.home_stop_service))
                        }
                    }
                }
            }
        }
    }
} 