package com.chiya.wallswitcher.ui.screen

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.ui.viewmodel.MainViewModel
import java.io.File
import androidx.compose.ui.draw.clip

/**
 * 图库界面
 */
@Composable
fun GalleryScreen(viewModel: MainViewModel) {
    val wallpapers by viewModel.wallpapers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = stringResource(id = R.string.gallery_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 内容
        if (loading) {
            // 加载中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(id = R.string.gallery_loading),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else if (wallpapers.isEmpty()) {
            // 空列表
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.gallery_empty),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 图片网格 - 使用LazyVerticalGrid的懒加载特性
            Text(
                text = stringResource(id = R.string.gallery_wallpaper_count, wallpapers.size),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wallpapers) { wallpaper ->
                    WallpaperItem(
                        wallpaper = wallpaper,
                        onClick = { viewModel.setWallpaper(wallpaper) }
                    )
                }
            }
        }
    }
}

/**
 * 壁纸项
 */
@Composable
fun WallpaperItem(wallpaper: Wallpaper, onClick: () -> Unit) {
    val context = LocalContext.current

    // 根据路径类型构建不同的请求
    val imageRequest = if (wallpaper.path.startsWith("content://")) {
        ImageRequest.Builder(LocalContext.current)
            .data(Uri.parse(wallpaper.path))
            .crossfade(true)
            // 添加缩略图和缓存策略
            .size(200)  // 限制加载尺寸
            .memoryCacheKey(wallpaper.path)
            .diskCacheKey(wallpaper.path)
            .build()
    } else {
        ImageRequest.Builder(LocalContext.current)
            .data(wallpaper.path)
            .crossfade(true)
            // 添加缩略图和缓存策略
            .size(200)  // 限制加载尺寸
            .memoryCacheKey(wallpaper.path)
            .diskCacheKey(wallpaper.path)
            .build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
    ) {
        Box {
            // 图片
            AsyncImage(
                model = imageRequest,
                contentDescription = wallpaper.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
            )
        }
    }
} 