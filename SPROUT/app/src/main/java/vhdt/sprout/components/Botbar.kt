package vhdt.sprout.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import vhdt.sprout.R
import vhdt.sprout.components.NavRoutes

/**
 * 4 tab chính của app — icon dùng vector Material Symbols dạng "_24px"
 * (tự export từ fonts.google.com/icons rồi bỏ vào res/drawable, cùng kiểu
 * đặt tên với calendar_today_24px/settings_24px bên WIIS).
 */
sealed class BotBarScreen(val route: String, val title: String, val iconRes: Int) {
    object TongQuan : BotBarScreen(NavRoutes.TongQuan, "Tổng quan", R.drawable.home_24px)
    object TuDong   : BotBarScreen(NavRoutes.TuDong, "Tự động", R.drawable.auto_awesome_mosaic_24px)
    object BieuDo   : BotBarScreen(NavRoutes.BieuDo, "Biểu đồ", R.drawable.chart_data_24px)
}

@Composable
fun BotBar(navController: NavController) {
    val screens = listOf(
        BotBarScreen.TongQuan,
        BotBarScreen.TuDong,
        BotBarScreen.BieuDo
    )

    // Gradient xanh lá — đồng bộ tông màu nhà kính thông minh của Sprout
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF16A34A),
            Color(0xFF0B3D24)
        )
    )

    NavigationBar(
        modifier = Modifier
            .background(gradientBrush),
        containerColor = Color.Transparent
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                selected = isSelected,
                label = {
                    Text(
                        text = screen.title,
                        fontSize = if (isSelected) 13.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.W800 else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(id = screen.iconRes),
                        contentDescription = screen.title
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFFFFFFF),
                    selectedTextColor = Color(0xFFFFFFFF),
                    unselectedIconColor = Color(0xFFFFFFFF).copy(alpha = 0.7f),
                    unselectedTextColor = Color(0xFFFFFFFF).copy(alpha = 0.7f),
                    indicatorColor = Color(0xFFFFFFFF).copy(alpha = 0.25f)
                ),
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(NavRoutes.TongQuan) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun BotBarPreview() {
    BotBar(navController = rememberNavController())
}