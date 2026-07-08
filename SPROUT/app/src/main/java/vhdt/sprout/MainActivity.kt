package vhdt.sprout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import vhdt.sprout.components.NavGraph
import vhdt.sprout.ui.theme.SproutTheme
import vhdt.sprout.viewmodel.SproutViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SproutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase đã được khởi tạo ở SproutApplication.onCreate() —
        // không gọi FirebaseDatabase.getInstance() ở đây nữa để tránh
        // race condition trên các ROM khởi tạo ContentProvider chậm.

        setContent {
            SproutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController, vm = viewModel)
                }
            }
        }
    }
}