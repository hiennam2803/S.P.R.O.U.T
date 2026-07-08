package vhdt.sprout.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vhdt.sprout.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSprout(
    onMenuClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                    .background(Color.White, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_vhdt),
                    contentDescription = "S.P.R.O.U.T",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.padding(6.dp)
                )
            }
        },
        navigationIcon = {},
//        actions = {
//            IconButton(onClick = onMenuClick) {
//                Icon(
//                    painter = painterResource(id = R.drawable.logo_vhdt),
//                    contentDescription = "Thông tin",
//                    tint = Color(0xFF16A34A),
//                    modifier = Modifier.size(26.dp)
//                )
//            }
//        },
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = Color.Transparent,
//            scrolledContainerColor = Color.Transparent
//        )
    )
}