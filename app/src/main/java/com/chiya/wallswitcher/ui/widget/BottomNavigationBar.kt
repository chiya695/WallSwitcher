package com.chiya.wallswitcher.ui.widget

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.chiya.wallswitcher.R

/**
 * 底部导航栏
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem(
            route = "home",
            titleResId = R.string.tab_home,
            iconResId = R.drawable.ic_home
        ),
        NavigationItem(
            route = "gallery",
            titleResId = R.string.tab_gallery,
            iconResId = R.drawable.ic_gallery
        ),
        NavigationItem(
            route = "settings",
            titleResId = R.string.tab_settings,
            iconResId = R.drawable.ic_settings
        )
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(painterResource(id = item.iconResId), contentDescription = null) },
                label = { Text(text = stringResource(id = item.titleResId)) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * 导航项
 */
data class NavigationItem(
    val route: String,
    val titleResId: Int,
    val iconResId: Int
) 