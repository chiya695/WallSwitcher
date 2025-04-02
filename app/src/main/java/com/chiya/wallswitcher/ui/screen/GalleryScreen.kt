package com.chiya.wallswitcher.ui.screen

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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.ui.viewmodel.MainViewModel
import java.io.File

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
                        modifier = Modifier.padding(top = 16.dp)
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
            // 图片网格
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
    ) {
        Box {
            // 图片
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(wallpaper.path))
                    .crossfade(true)
                    .build(),
                contentDescription = wallpaper.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
        }
    }
} 