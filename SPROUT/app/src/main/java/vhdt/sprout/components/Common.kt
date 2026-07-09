package vhdt.sprout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vhdt.sprout.ui.theme.ChuMo
import vhdt.sprout.ui.theme.ChuPhu
import vhdt.sprout.ui.theme.DoNguyHiem
import vhdt.sprout.ui.theme.VangCanhBao
import vhdt.sprout.ui.theme.XanhLa

// ============================
// 🪟 GLASS CARD – dùng Card Material3
// ============================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
    borderColor: Color = Color(0xAE5FFF52),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// ============================
// 🏷️ GLASS TAG – nhãn nhỏ
// ============================
@Composable
fun GlassTag(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ============================
// 📡 THẺ CẢM BIẾN
// ============================
@Composable
fun TheCamBien(
    iconRes: Int,
    nhan: String,
    giaTri: String,
    donVi: String,
    trangThaiCanhBao: TrangThaiCanhBao = TrangThaiCanhBao.BINH_THUONG,
    modifier: Modifier = Modifier
) {
    val mauGiaTri = when (trangThaiCanhBao) {
        TrangThaiCanhBao.BINH_THUONG -> MaterialTheme.colorScheme.onSurface
        TrangThaiCanhBao.CANH_BAO    -> VangCanhBao
        TrangThaiCanhBao.NGUY_HIEM   -> DoNguyHiem
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = nhan,
                    tint = ChuPhu,
                    modifier = Modifier.size(15.dp)
                )
                Text(text = nhan, fontSize = 12.sp, color = ChuPhu)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = giaTri,
                    fontSize = 24.sp,
                    color = mauGiaTri,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " $donVi",
                    fontSize = 12.sp,
                    color = ChuMo,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

enum class TrangThaiCanhBao {
    BINH_THUONG, CANH_BAO, NGUY_HIEM
}

// ============================
// 🟢 NHÃN TRẠNG THÁI (chấm tròn)
// ============================
@Composable
fun NhanTrangThai(text: String, mau: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(mau.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(mau, RoundedCornerShape(50))
        )
        Text(text = text, fontSize = 12.sp, color = mau, fontWeight = FontWeight.Medium)
    }
}

// ============================
// ⚙️ HÀNG THIẾT BỊ
// ============================
@Composable
fun HangThietBi(
    iconRes: Int,
    ten: String,
    dangBat: Boolean,
    choPhepChinh: Boolean,
    onDoiTrangThai: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = ten,
                    tint = if (dangBat) XanhLa else ChuPhu,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(text = ten, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (dangBat) "Đang bật" else "Đang tắt",
                        fontSize = 11.sp,
                        color = if (dangBat) XanhLa else ChuMo
                    )
                }
            }
            Switch(
                checked = dangBat,
                onCheckedChange = onDoiTrangThai,
                enabled = choPhepChinh,
                colors = SwitchDefaults.colors(checkedTrackColor = XanhLa)
            )
        }
    }
}

// ============================
// 📝 TIÊU ĐỀ MỤC
// ============================
@Composable
fun TieuDeMuc(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

// ============================
// 📦 KHUNG MỜ (alias cho GlassCard)
// ============================
@Composable
fun KhungMo(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(modifier = modifier, content = content)
}