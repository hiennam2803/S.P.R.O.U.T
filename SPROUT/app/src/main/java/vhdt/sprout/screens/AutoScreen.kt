package vhdt.sprout.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vhdt.sprout.R
import vhdt.sprout.data.ThietBiId
import vhdt.sprout.components.HangThietBi
import vhdt.sprout.components.KhungMo
import vhdt.sprout.components.TieuDeMuc
import vhdt.sprout.ui.theme.ChuMo
import vhdt.sprout.ui.theme.ChuPhu
import vhdt.sprout.ui.theme.DoNguyHiem
import vhdt.sprout.ui.theme.NenTheVien
import vhdt.sprout.ui.theme.TimGemini
import vhdt.sprout.ui.theme.VangCanhBao
import vhdt.sprout.ui.theme.XanhDuong
import vhdt.sprout.ui.theme.XanhLa
import vhdt.sprout.viewmodel.SproutViewModel

/**
 * Màn hình GỘP: TỰ ĐỘNG + LOẠI CÂY
 *
 * - AUTO   -> Arduino tự vận hành theo ngưỡng của loại cây. Hiện ô nhập tên
 *             cây (AI/cache), thiết bị hiển thị CHỈ XEM.
 * - MANUAL -> người dùng tự kiểm soát hoàn toàn. Hiện công tắc thiết bị +
 *             ngưỡng cá nhân (Personal) để tự kéo tay.
 *
 * Toàn bộ input (tên cây đang gõ, các thanh trượt Personal) dùng
 * rememberSaveable — GHI NHỚ giá trị khi chuyển sang tab khác rồi quay lại,
 * không bị reset về mặc định.
 *
 * Icon cần thêm (ngoài bộ đã dùng ở màn Tổng quan):
 *   settings_24px, touch_app_24px, auto_awesome_24px, edit_24px, save_24px,
 *   thermostat_24px, humidity_percentage_24px, grass_24px, light_mode_24px
 */
@Composable
fun AutoScreen(vm: SproutViewModel) {
    val tb by vm.thietBi.collectAsState()
    val tt by vm.trangThai.collectAsState()
    val dangManual = tt.mode == "MANUAL"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TieuDeMuc("Chế độ vận hành") }

        item {
            CongTacCheDo(
                dangManual = dangManual,
                onChon = { chonManual -> vm.datCheDo(if (chonManual) "MANUAL" else "AUTO") }
            )
        }

        item {
            KhungMo {
                Text(
                    text = if (dangManual)
                        "Đang ở chế độ THỦ CÔNG — tự bật/tắt thiết bị và tự đặt ngưỡng cá nhân bên dưới."
                    else
                        "Đang ở chế độ TỰ ĐỘNG — Arduino tự vận hành theo ngưỡng của loại cây bạn nhập bên dưới.",
                    fontSize = 12.sp,
                    color = ChuPhu
                )
            }
        }

        item { TieuDeMuc("Thiết bị") }

        items(ThietBiId.values().toList()) { thietBi ->
            val dangBat = when (thietBi) {
                ThietBiId.QUAT   -> tb.quat == 1
                ThietBiId.BOM    -> tb.bom == 1
                ThietBiId.SUOI   -> tb.suoi == 1
                ThietBiId.DEN    -> tb.den == 1
                ThietBiId.HUTAM  -> tb.hutAm == 1
                ThietBiId.TANGAM -> tb.tangAm == 1
            }
            HangThietBi(
                iconRes = iconChoThietBi(thietBi),
                ten = thietBi.nhan,
                dangBat = dangBat,
                choPhepChinh = dangManual,
                onDoiTrangThai = { bat -> vm.dieuKhienThietBi(thietBi, bat) }
            )
        }

        item {
            TieuDeMuc(if (dangManual) "Ngưỡng cá nhân (Personal)" else "Loại cây (AI đề xuất)")
        }
        item {
            if (dangManual) KhoiCheDoPersonal(vm) else KhoiCheDoAi(vm)
        }
    }
}

private fun iconChoThietBi(thietBi: ThietBiId): Int = when (thietBi) {
    ThietBiId.QUAT   -> R.drawable.cyclone_24px
    ThietBiId.BOM    -> R.drawable.rainy_24px
    ThietBiId.SUOI   -> R.drawable.fireplace_24px
    ThietBiId.DEN    -> R.drawable.light_mode_24px
    ThietBiId.HUTAM  -> R.drawable.windshield_defrost_front_24px
    ThietBiId.TANGAM -> R.drawable.cool_to_dry_24px
}

@Composable
private fun CongTacCheDo(dangManual: Boolean, onChon: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NenTheVien, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        LuaChonCheDo(
            iconRes = R.drawable.robot_2_24px,
            nhan = "AUTO",
            dangChon = !dangManual,
            mau = XanhDuong,
            modifier = Modifier.weight(1f),
            onClick = { onChon(false) }
        )
        LuaChonCheDo(
            iconRes = R.drawable.edit_note_24px,
            nhan = "MANUAL",
            dangChon = dangManual,
            mau = VangCanhBao,
            modifier = Modifier.weight(1f),
            onClick = { onChon(true) }
        )
    }
}

@Composable
private fun LuaChonCheDo(
    iconRes: Int,
    nhan: String,
    dangChon: Boolean,
    mau: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .background(
                if (dangChon) mau.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (dangChon) mau else ChuPhu,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "  $nhan",
            color = if (dangChon) mau else ChuPhu,
            fontWeight = if (dangChon) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// ================================================================
// KHỐI AUTO — nhập loại cây, bridge tự tra cache / gọi Gemini nếu cây mới
// tenCay dùng rememberSaveable -> gõ dở rồi chuyển tab khác vẫn còn nguyên
// ================================================================

@Composable
private fun KhoiCheDoAi(vm: SproutViewModel) {
    var tenCay by rememberSaveable { mutableStateOf("") }
    val dangXuLy by vm.dangXuLyCay.collectAsState()
    val thongTin by vm.thongTinCay.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        KhungMo {
            Text(
                "Nhập tên loại cây bạn đang trồng. Nếu là cây mới, hệ thống sẽ hỏi " +
                        "Gemini AI đúng 1 lần rồi lưu lại — các lần sau dùng ngay kết quả cũ, " +
                        "không tốn thêm token.",
                fontSize = 12.sp, color = ChuPhu
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = tenCay,
                onValueChange = { tenCay = it },
                placeholder = { Text("VD: cà chua, xà lách...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { vm.xacDinhLoaiCay(tenCay) },
                enabled = tenCay.isNotBlank() && !dangXuLy,
                colors = ButtonDefaults.buttonColors(containerColor = XanhLa, contentColor = Color.Black)
            ) {
                Text("Xác định")
            }
        }

        if (dangXuLy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = XanhLa)
                Text("Đang tra cứu (cache trước, Gemini nếu là cây mới)...", fontSize = 12.sp, color = ChuPhu)
            }
        }

        KhungMo {
            if (thongTin == null) {
                Text("Chưa chọn loại cây — hệ thống đang dùng ngưỡng mặc định.", fontSize = 13.sp, color = ChuPhu)
            } else {
                val tt = thongTin!!
                if (tt.loi != null) {
                    Text("⚠ ${tt.loi}", color = DoNguyHiem, fontSize = 13.sp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tt.ten, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        NhanNguon(tt.nguon)
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        DongThongSo(R.drawable.thermometer_24px, "${tt.nguong.nhietMin}–${tt.nguong.nhietMax}°C")
                        DongThongSo(R.drawable.humidity_high_24px, "Độ ẩm KK ${tt.nguong.amKKMin}–${tt.nguong.amKKMax}%")
                        DongThongSo(R.drawable.grass_24px, "Độ ẩm đất ${tt.nguong.datMin}–${tt.nguong.datMax}%")
                        DongThongSo(R.drawable.light_mode_24px, "Bật đèn khi dưới ${tt.nguong.luxMin} lux")
                    }

                    if (tt.ghiChu.isNotBlank()) {
                        Text("“${tt.ghiChu}”", fontSize = 12.sp, color = ChuMo, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DongThongSo(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = ChuPhu,
            modifier = Modifier.size(15.dp)
        )
        Text(text, fontSize = 12.sp, color = ChuPhu)
    }
}

@Composable
private fun NhanNguon(nguon: String) {
    val laGemini = nguon == "gemini"
    val text = if (laGemini) "Gemini (mới hỏi)" else "Cache"
    val mau = if (laGemini) TimGemini else XanhLa
    val iconRes = if (laGemini) R.drawable.auto_awesome_mosaic_24px else R.drawable.save_24px

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(mau.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = mau,
            modifier = Modifier.size(12.dp)
        )
        Text(text = text, fontSize = 10.sp, color = mau)
    }
}

// ================================================================
// KHỐI MANUAL — ngưỡng Personal, tự tay kéo, áp thẳng, không qua AI.
// Mỗi giá trị tách thành 1 Float rememberSaveable riêng (Float lưu được
// vào Bundle, còn ClosedFloatingPointRange thì KHÔNG) -> đảm bảo ghi nhớ
// thật sự khi chuyển tab qua lại, không chỉ lúc còn trong bộ nhớ RAM.
// ================================================================

@Composable
private fun KhoiCheDoPersonal(vm: SproutViewModel) {
    val ng by vm.nguong.collectAsState()

    var daKhoiTao by rememberSaveable { mutableStateOf(false) }
    var nhietMin by rememberSaveable { mutableFloatStateOf(18f) }
    var nhietMax by rememberSaveable { mutableFloatStateOf(30f) }
    var amKKMin by rememberSaveable { mutableFloatStateOf(50f) }
    var amKKMax by rememberSaveable { mutableFloatStateOf(85f) }
    var datMin by rememberSaveable { mutableFloatStateOf(30f) }
    var datMax by rememberSaveable { mutableFloatStateOf(70f) }
    var luxMin by rememberSaveable { mutableFloatStateOf(300f) }

    // Chỉ đồng bộ 1 LẦN DUY NHẤT từ Firebase lúc mở màn lần đầu.
    // Sau đó mọi chỉnh sửa của người dùng được giữ nguyên (ghi nhớ),
    // kể cả khi chuyển sang tab khác rồi quay lại.
    LaunchedEffect(ng, daKhoiTao) {
        if (!daKhoiTao) {
            nhietMin = ng.nhietMin.toFloat()
            nhietMax = ng.nhietMax.toFloat()
            amKKMin = ng.amKKMin.toFloat()
            amKKMax = ng.amKKMax.toFloat()
            datMin = ng.datMin.toFloat()
            datMax = ng.datMax.toFloat()
            luxMin = ng.nguongDen.toFloat()
            daKhoiTao = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        KhungMo {
            Text(
                "Tự tay đặt ngưỡng môi trường cho cây — bỏ qua AI, áp dụng ngay lập tức " +
                        "xuống Arduino. Phù hợp khi bạn đã biết rõ nhu cầu của cây.",
                fontSize = 12.sp, color = ChuPhu
            )
        }

        KhoiNguong(
            iconRes = R.drawable.thermometer_24px,
            nhan = "Nhiệt độ (°C)",
            gtHienThi = "${nhietMin.toInt()}° – ${nhietMax.toInt()}°"
        ) {
            RangeSlider(
                value = nhietMin..nhietMax,
                onValueChange = { nhietMin = it.start; nhietMax = it.endInclusive },
                valueRange = 0f..50f,
                colors = sliderMauXanh()
            )
        }

        KhoiNguong(
            iconRes = R.drawable.humidity_high_24px,
            nhan = "Độ ẩm không khí (%)",
            gtHienThi = "${amKKMin.toInt()}% – ${amKKMax.toInt()}%"
        ) {
            RangeSlider(
                value = amKKMin..amKKMax,
                onValueChange = { amKKMin = it.start; amKKMax = it.endInclusive },
                valueRange = 0f..100f,
                colors = sliderMauXanh()
            )
        }

        KhoiNguong(
            iconRes = R.drawable.grass_24px,
            nhan = "Độ ẩm đất (%)",
            gtHienThi = "${datMin.toInt()}% – ${datMax.toInt()}%"
        ) {
            RangeSlider(
                value = datMin..datMax,
                onValueChange = { datMin = it.start; datMax = it.endInclusive },
                valueRange = 0f..100f,
                colors = sliderMauXanh()
            )
        }

        KhoiNguong(
            iconRes = R.drawable.light_mode_24px,
            nhan = "Ngưỡng bật đèn (lux)",
            gtHienThi = "< ${luxMin.toInt()} lux"
        ) {
            Slider(
                value = luxMin,
                onValueChange = { luxMin = it },
                valueRange = 0f..2000f,
                colors = sliderMauXanh()
            )
        }

        Button(
            onClick = {
                vm.apDungNguongCaNhan(
                    nhietMin = nhietMin.toDouble(),
                    nhietMax = nhietMax.toDouble(),
                    amKKMin = amKKMin.toDouble(),
                    amKKMax = amKKMax.toDouble(),
                    datMin = datMin.toInt(),
                    datMax = datMax.toInt(),
                    luxMin = luxMin.toDouble()
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = XanhLa, contentColor = Color.Black)
        ) {
            Text("Áp dụng ngưỡng cá nhân")
        }
    }
}

@Composable
private fun KhoiNguong(
    iconRes: Int,
    nhan: String,
    gtHienThi: String,
    thanhTruot: @Composable () -> Unit
) {
    KhungMo {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = ChuPhu,
                    modifier = Modifier.size(16.dp)
                )
                Text(nhan, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(gtHienThi, fontSize = 13.sp, color = XanhLa, fontWeight = FontWeight.Bold)
        }
        thanhTruot()
    }
}

@Composable
private fun sliderMauXanh() = SliderDefaults.colors(
    thumbColor = XanhLa,
    activeTrackColor = XanhLa,
    inactiveTrackColor = NenTheVien
)