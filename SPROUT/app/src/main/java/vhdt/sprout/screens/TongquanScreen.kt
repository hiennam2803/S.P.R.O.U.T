package vhdt.sprout.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vhdt.sprout.R
import vhdt.sprout.data.ThietBi
import vhdt.sprout.components.HangThietBi
import vhdt.sprout.components.KhungMo
import vhdt.sprout.components.NhanTrangThai
import vhdt.sprout.components.TheCamBien
import vhdt.sprout.components.TieuDeMuc
import vhdt.sprout.components.TrangThaiCanhBao
import vhdt.sprout.ui.theme.ChuMo
import vhdt.sprout.ui.theme.ChuPhu
import vhdt.sprout.ui.theme.DoNguyHiem
import vhdt.sprout.ui.theme.VangCanhBao
import vhdt.sprout.ui.theme.XanhDuong
import vhdt.sprout.ui.theme.XanhLa
import vhdt.sprout.viewmodel.SproutViewModel

/**
 * Màn hình 1/4 — TỔNG QUAN
 * Hiển thị toàn bộ thông số: cảm biến, trạng thái thiết bị, chế độ hệ thống,
 * tư vấn AI mới nhất và cảnh báo gần nhất — tất cả trên 1 màn hình duy nhất.
 *
 * TẤT CẢ icon dùng vector trong res/drawable (Material Symbols "_24px"),
 * cần có sẵn các file sau trong res/drawable/:
 *   thermostat_24px, humidity_percentage_24px, grass_24px, water_24px,
 *   light_mode_24px, sensor_door_24px, lock_24px, mode_fan_24px,
 *   water_drop_24px, local_fire_department_24px, lightbulb_24px,
 *   dry_24px, humidity_high_24px
 */
@Composable
fun TongQuanScreen(vm: SproutViewModel) {
    val cb by vm.camBien.collectAsState()
    val tb by vm.thietBi.collectAsState()
    val ng by vm.nguong.collectAsState()
    val tt by vm.trangThai.collectAsState()
    val aiLog by vm.aiLog.collectAsState()
    val canhBao by vm.canhBao.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Header: trạng thái kết nối + chế độ ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NhanTrangThai(
                    text = if (tt.online) "Trực tuyến" else "Mất kết nối",
                    mau = if (tt.online) XanhLa else ChuMo
                )
                NhanTrangThai(
                    text = if (tt.mode == "AUTO") "AUTO" else "MANUAL",
                    mau = if (tt.mode == "AUTO") XanhDuong else VangCanhBao
                )
                if (tt.nuocHet) {
                    NhanTrangThai(text = "Hết nước", mau = DoNguyHiem)
                }
            }
        }

        // ---- Lưới cảm biến ----
        item { TieuDeMuc("Cảm biến") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TheCamBien(
                        iconRes = R.drawable.thermometer_24px, nhan = "Nhiệt độ",
                        giaTri = "%.1f".format(cb.nhietDo), donVi = "°C",
                        trangThaiCanhBao = phanLoai(cb.nhietDo, ng.nhietMin, ng.nhietMax),
                        modifier = Modifier.weight(1f)
                    )
                    TheCamBien(
                        iconRes = R.drawable.snowing_heavy_24px, nhan = "Độ ẩm KK",
                        giaTri = "%.1f".format(cb.doAmKK), donVi = "%",
                        trangThaiCanhBao = phanLoai(cb.doAmKK, ng.amKKMin, ng.amKKMax),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TheCamBien(
                        iconRes = R.drawable.grass_24px, nhan = "Độ ẩm đất",
                        giaTri = cb.doAmDat.toString(), donVi = "%",
                        trangThaiCanhBao = phanLoai(cb.doAmDat.toDouble(), ng.datMin.toDouble(), ng.datMax.toDouble()),
                        modifier = Modifier.weight(1f)
                    )
                    TheCamBien(
                        iconRes = R.drawable.humidity_high_24px, nhan = "Mực nước",
                        giaTri = cb.mucNuoc.toString(), donVi = "%",
                        trangThaiCanhBao = if (cb.mucNuoc < 15) TrangThaiCanhBao.NGUY_HIEM
                        else if (cb.mucNuoc < 30) TrangThaiCanhBao.CANH_BAO
                        else TrangThaiCanhBao.BINH_THUONG,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TheCamBien(
                        iconRes = R.drawable.light_mode_24px, nhan = "Ánh sáng",
                        giaTri = "%.0f".format(cb.anhSang), donVi = "lux",
                        trangThaiCanhBao = if (cb.anhSang < ng.nguongDen) TrangThaiCanhBao.CANH_BAO
                        else TrangThaiCanhBao.BINH_THUONG,
                        modifier = Modifier.weight(1f)
                    )
                    TheCamBien(
                        iconRes = if (cb.cuaMo) R.drawable.door_open_24px else R.drawable.door_front_24px,
                        nhan = "Cửa",
                        giaTri = if (cb.cuaMo) "Đang mở" else "Đã đóng", donVi = "",
                        trangThaiCanhBao = if (cb.cuaMo) TrangThaiCanhBao.CANH_BAO else TrangThaiCanhBao.BINH_THUONG,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- Thiết bị ----
        item { TieuDeMuc("Thiết bị") }
        item { LuoiThietBi(tb) }

        // ---- AI tư vấn (mặc định, rule-based) ----
        item { TieuDeMuc("AI tư vấn (mặc định)") }
        item {
            KhungMo {
                val moiNhat = aiLog.lastOrNull()
                if (moiNhat == null) {
                    Text("Đang chờ dữ liệu phân tích...", color = ChuPhu, fontSize = 13.sp)
                } else {
                    Text(moiNhat.tomTat, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(moiNhat.khuyenNghi, color = ChuPhu, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(moiNhat.thoiGian, color = ChuMo, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        // ---- Cảnh báo gần nhất ----
        item { TieuDeMuc("Cảnh báo gần nhất") }
        item {
            KhungMo {
                val moiNhat = canhBao.lastOrNull()
                if (moiNhat == null) {
                    Text("Không có cảnh báo nào.", color = ChuPhu, fontSize = 13.sp)
                } else {
                    Text(moiNhat.noiDung, color = VangCanhBao, fontSize = 13.sp)
                    Text(moiNhat.thoiGian, color = ChuMo, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun LuoiThietBi(tb: ThietBi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HangThietBi(R.drawable.cyclone_24px, "Quạt thông gió", tb.quat == 1, choPhepChinh = false, onDoiTrangThai = {})
        HangThietBi(R.drawable.rainy_24px, "Bơm tưới", tb.bom == 1, choPhepChinh = false, onDoiTrangThai = {})
        HangThietBi(R.drawable.fireplace_24px, "Sưởi ấm", tb.suoi == 1, choPhepChinh = false, onDoiTrangThai = {})
        HangThietBi(R.drawable.light_mode_24px, "Đèn quang hợp", tb.den == 1, choPhepChinh = false, onDoiTrangThai = {})
        HangThietBi(R.drawable.windshield_defrost_front_24px, "Hút ẩm", tb.hutAm == 1, choPhepChinh = false, onDoiTrangThai = {})
        HangThietBi(R.drawable.cool_to_dry_24px, "Tăng ẩm", tb.tangAm == 1, choPhepChinh = false, onDoiTrangThai = {})
    }
}

/** Phân loại giá trị so với khoảng [min,max] để tô màu cảnh báo */
private fun phanLoai(giaTri: Double, min: Double, max: Double): TrangThaiCanhBao {
    val bienDo = (max - min).coerceAtLeast(0.01)
    return when {
        giaTri < min - bienDo * 0.15 || giaTri > max + bienDo * 0.15 -> TrangThaiCanhBao.NGUY_HIEM
        giaTri < min || giaTri > max -> TrangThaiCanhBao.CANH_BAO
        else -> TrangThaiCanhBao.BINH_THUONG
    }
}