package vhdt.sprout.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import vhdt.sprout.component.BotBar
import vhdt.sprout.component.TopBarSprout
import vhdt.sprout.screens.AutoScreen
import vhdt.sprout.screens.SplashScreen
import vhdt.sprout.viewmodel.SproutViewModel
import vhdt.sprout.screens.TongQuanScreen


object NavRoutes {
    const val Splash   = "splash"
    const val TongQuan = "tong_quan"
    const val TuDong   = "tu_dong"
    const val BieuDo   = "bieu_do"
}

/**
 * Khung điều hướng chính: Splash -> 4 màn hình chính (Tổng quan / Loại cây /
 * Tự động / Biểu đồ). TopBar + BotBar chỉ hiện khi KHÔNG ở màn Splash.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    vm: SproutViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val showBottomBar = currentRoute == NavRoutes.TongQuan ||
            currentRoute == NavRoutes.TuDong ||
            currentRoute == NavRoutes.BieuDo

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = Color(0xFF0B0F0D),
            topBar = {
                if (showBottomBar) {
                    TopBarSprout()
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    BotBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                // Giữ nguyên startDestination gốc là Splash để mở app lên chạy splash
                startDestination = NavRoutes.Splash,
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                composable(NavRoutes.Splash) {
                    SplashRoute(navController)
                }

                composable(NavRoutes.TongQuan) {
                    TongQuanScreen(vm)
                }

                composable(NavRoutes.TuDong) {
                    AutoScreen(vm)
                }
//
//                composable(NavRoutes.BieuDo) {
//                    BieuDoScreen(vm)
//                }
            }
        }
    }
}

@Composable
private fun SplashRoute(
    navController: NavHostController
) {
    SplashScreen(onFinished = {
        navController.navigate(NavRoutes.TongQuan) {
            popUpTo(NavRoutes.Splash) { inclusive = true }
        }
    })
}