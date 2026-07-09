package vhdt.sprout.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vhdt.sprout.R
import vhdt.sprout.components.GlassCard
import vhdt.sprout.components.GlassTag
import vhdt.sprout.components.TheCamBien
import vhdt.sprout.components.TrangThaiCanhBao
import vhdt.sprout.data.LichSuDiem
import vhdt.sprout.ui.theme.ChuPhu
import vhdt.sprout.ui.theme.NenChinh
import vhdt.sprout.ui.theme.NenThe
import vhdt.sprout.ui.theme.XanhDuong
import vhdt.sprout.ui.theme.XanhLa
import vhdt.sprout.viewmodel.SproutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ================================================================
// MÀU CHỈ SỐ
// ================================================================
private val MauNhiet = Color(0xFFFF6347)
private val MauAmKK = Color(0xFF4682B4)
private val MauAmDat = Color(0xFF2E8B57)
private val MauNuoc = Color(0xFF1E90FF)
private val MauSang = Color(0xFFFFD700)

private data class ChiSoBoLoc(
    val key: String,
    val label: String,
    val color: Color,
    val iconRes: Int
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BieudoScreen(vm: SproutViewModel) {
    val lichSuGoc by vm.lichSu.collectAsState()
    val nguong by vm.nguong.collectAsState()

    var filterThoiGian by remember { mutableStateOf("Giờ") }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(filterThoiGian) {
        selectedIndex = null
        val soDiemCanTai = when (filterThoiGian) {
            "Giờ"   -> 150
            "Ngày"  -> 3000
            "Tháng" -> 5000
            else    -> 150
        }
        vm.datSoDiemLichSu(soDiemCanTai)
    }

    var showNhietDo by remember { mutableStateOf(true) }
    var showDoAmKK by remember { mutableStateOf(true) }
    var showDoAmDat by remember { mutableStateOf(true) }
    var showMucNuoc by remember { mutableStateOf(true) }
    var showAnhSang by remember { mutableStateOf(true) }

    val danhSachChiSo = listOf(
        ChiSoBoLoc("nhiet", "Nhiệt độ", MauNhiet, R.drawable.thermometer_24px),
        ChiSoBoLoc("amkk", "Độ ẩm KK", MauAmKK, R.drawable.snowing_heavy_24px),
        ChiSoBoLoc("amdat", "Độ ẩm đất", MauAmDat, R.drawable.grass_24px),
        ChiSoBoLoc("nuoc", "Mực nước", MauNuoc, R.drawable.humidity_high_24px),
        ChiSoBoLoc("sang", "Ánh sáng", MauSang, R.drawable.light_mode_24px)
    )

    fun trangThaiCua(key: String): Boolean = when (key) {
        "nhiet" -> showNhietDo
        "amkk" -> showDoAmKK
        "amdat" -> showDoAmDat
        "nuoc" -> showMucNuoc
        "sang" -> showAnhSang
        else -> false
    }

    fun bamChon(key: String) {
        when (key) {
            "nhiet" -> showNhietDo = !showNhietDo
            "amkk" -> showDoAmKK = !showDoAmKK
            "amdat" -> showDoAmDat = !showDoAmDat
            "nuoc" -> showMucNuoc = !showMucNuoc
            "sang" -> showAnhSang = !showAnhSang
        }
    }

    val lichSuDaXuLy = remember(lichSuGoc, filterThoiGian) {
        if (lichSuGoc.isEmpty()) emptyList()
        else {
            try {
                val dinhDangDayDu = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val dinhDangNhanGio = SimpleDateFormat("HH:mm", Locale.US)

                fun parseMillis(chuoi: String): Long? = try {
                    dinhDangDayDu.parse(chuoi)?.time
                } catch (e: Exception) { null }

                when (filterThoiGian) {
                    "Giờ" -> {
                        val diemCoMillis = lichSuGoc.mapNotNull { diem ->
                            parseMillis(diem.thoiGian)?.let { millis -> millis to diem }
                        }
                        if (diemCoMillis.isEmpty()) {
                            lichSuGoc.map { it.copy(thoiGian = it.thoiGian.substringAfter(" ").take(5)) }
                        } else {
                            val moiNhat = diemCoMillis.maxOf { it.first }
                            val moc1Gio = moiNhat - 60 * 60 * 1000L
                            val trongVong1Gio = diemCoMillis.filter { it.first >= moc1Gio }

                            val mapped = trongVong1Gio
                                .groupBy { (millis, _) -> millis - (millis % (2 * 60 * 1000L)) }
                                .toSortedMap()
                                .map { (mocThoiGian, cacDiem) ->
                                    val ds = cacDiem.map { it.second }
                                    LichSuDiem(
                                        thoiGian = dinhDangNhanGio.format(Date(mocThoiGian)),
                                        nhietDo = ds.map { it.nhietDo }.average(),
                                        doAmKK = ds.map { it.doAmKK }.average(),
                                        doAmDat = ds.map { it.doAmDat }.average().toInt(),
                                        mucNuoc = ds.map { it.mucNuoc }.average().toInt(),
                                        anhSang = ds.map { it.anhSang }.average(),
                                        cuaMo = ds.last().cuaMo
                                    )
                                }
                            if (mapped.size < 2) {
                                trongVong1Gio.map { (_, d) -> d.copy(thoiGian = d.thoiGian.substringAfter(" ").take(5)) }
                            } else mapped
                        }
                    }
                    "Ngày" -> {
                        val mapped = lichSuGoc.groupBy { diem ->
                            val phanGio = diem.thoiGian.substringAfter(" ").take(2)
                            if (phanGio.all { it.isDigit() }) "${phanGio}h" else "00h"
                        }.map { (khungGio, cacDiemTrongGio) ->
                            LichSuDiem(
                                thoiGian = khungGio,
                                nhietDo = cacDiemTrongGio.map { it.nhietDo }.average(),
                                doAmKK = cacDiemTrongGio.map { it.doAmKK }.average(),
                                doAmDat = cacDiemTrongGio.map { it.doAmDat }.average().toInt(),
                                mucNuoc = cacDiemTrongGio.map { it.mucNuoc }.average().toInt(),
                                anhSang = cacDiemTrongGio.map { it.anhSang }.average(),
                                cuaMo = cacDiemTrongGio.last().cuaMo
                            )
                        }.sortedBy { it.thoiGian }
                        if (mapped.size < 2) {
                            lichSuGoc.map { it.copy(thoiGian = it.thoiGian.substringAfter(" ").take(5)) }
                        } else mapped
                    }
                    "Tháng" -> {
                        val mapped = lichSuGoc.groupBy { diem ->
                            diem.thoiGian.take(10).substringAfter("-")
                        }.map { (ngayThang, cacDiemTrongNgay) ->
                            LichSuDiem(
                                thoiGian = ngayThang,
                                nhietDo = cacDiemTrongNgay.map { it.nhietDo }.average(),
                                doAmKK = cacDiemTrongNgay.map { it.doAmKK }.average(),
                                doAmDat = cacDiemTrongNgay.map { it.doAmDat }.average().toInt(),
                                mucNuoc = cacDiemTrongNgay.map { it.mucNuoc }.average().toInt(),
                                anhSang = cacDiemTrongNgay.map { it.anhSang }.average(),
                                cuaMo = cacDiemTrongNgay.last().cuaMo
                            )
                        }.sortedBy { it.thoiGian }.takeLast(30)
                        if (mapped.size < 2) {
                            lichSuGoc.map { it.copy(thoiGian = it.thoiGian.substringAfter(" ").take(5)) }
                        } else mapped
                    }
                    else -> lichSuGoc
                }
            } catch (e: Exception) {
                lichSuGoc.map { it.copy(thoiGian = it.thoiGian.takeLast(8)) }
            }
        }
    }

    val diemDangXem: LichSuDiem? =
        selectedIndex?.let { idx -> lichSuDaXuLy.getOrNull(idx) } ?: lichSuGoc.lastOrNull()
    val dangXemDiemChon = selectedIndex != null && lichSuDaXuLy.getOrNull(selectedIndex ?: -1) != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NenChinh),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- HEADER: dropdown khoảng thời gian ----
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
            ) {
                GlassDropdownHeader(
                    options = listOf("Giờ", "Ngày", "Tháng"),
                    selected = filterThoiGian,
                    onSelect = { filterThoiGian = it }
                )
            }
        }

        // ---- BỘ LỌC CHỈ SỐ — 6 Ô NHỎ TRONG 1 BOX TO ----
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Chỉ số hiển thị",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                val isAllChecked = danhSachChiSo.all { trangThaiCua(it.key) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Hàng 1: 3 ô đầu
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        danhSachChiSo.take(3).forEach { cs ->
                            MetricToggleBox(
                                label = cs.label,
                                iconRes = cs.iconRes,
                                color = cs.color,
                                selected = trangThaiCua(cs.key),
                                onClick = { bamChon(cs.key) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Hàng 2: 2 ô còn lại + ô "Tất cả"
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        danhSachChiSo.drop(3).forEach { cs ->
                            MetricToggleBox(
                                label = cs.label,
                                iconRes = cs.iconRes,
                                color = cs.color,
                                selected = trangThaiCua(cs.key),
                                onClick = { bamChon(cs.key) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MetricToggleBox(
                            label = "Tất cả",
                            iconRes = R.drawable.clear_all_24px,
                            color = MaterialTheme.colorScheme.onSurface,
                            selected = isAllChecked,
                            onClick = {
                                val muc = !isAllChecked
                                showNhietDo = muc; showDoAmKK = muc; showDoAmDat = muc
                                showMucNuoc = muc; showAnhSang = muc
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ---- BIỂU ĐỒ ----
        item {
            if (lichSuDaXuLy.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có dữ liệu lịch sử phù hợp", color = ChuPhu, fontSize = 14.sp)
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SimpleLineChart(
                        data = lichSuDaXuLy,
                        showNhietDo = showNhietDo,
                        showDoAmKK = showDoAmKK,
                        showDoAmDat = showDoAmDat,
                        showMucNuoc = showMucNuoc,
                        showAnhSang = showAnhSang,
                        selectedIndex = selectedIndex,
                        onPointSelected = { idx -> selectedIndex = idx },
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                    )
                }
            }
        }

        // ---- TIÊU ĐỀ KHỐI CHI TIẾT ----
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.info_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (dangXemDiemChon) "Chi tiết lúc ${diemDangXem?.thoiGian}" else "Chi tiết hiện tại",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (dangXemDiemChon) {
                    GlassTag(
                        text = "Xem mới nhất",
                        color = XanhLa,
                        modifier = Modifier.clickable { selectedIndex = null }
                    )
                }
            }
        }

        // ---- LƯỚI CHI TIẾT — DÙNG TheCamBien Y HỆT MÀN TỔNG QUAN ----
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TheCamBien(
                            iconRes = R.drawable.thermometer_24px,
                            nhan = "Nhiệt độ",
                            giaTri = diemDangXem?.let { "%.1f".format(it.nhietDo) } ?: "--",
                            donVi = "°C",
                            trangThaiCanhBao = diemDangXem?.let { phanLoai(it.nhietDo, nguong.nhietMin, nguong.nhietMax) }
                                ?: TrangThaiCanhBao.BINH_THUONG,
                            modifier = Modifier.weight(1f)
                        )
                        TheCamBien(
                            iconRes = R.drawable.snowing_heavy_24px,
                            nhan = "Độ ẩm KK",
                            giaTri = diemDangXem?.let { "%.1f".format(it.doAmKK) } ?: "--",
                            donVi = "%",
                            trangThaiCanhBao = diemDangXem?.let { phanLoai(it.doAmKK, nguong.amKKMin, nguong.amKKMax) }
                                ?: TrangThaiCanhBao.BINH_THUONG,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TheCamBien(
                            iconRes = R.drawable.grass_24px,
                            nhan = "Độ ẩm đất",
                            giaTri = diemDangXem?.doAmDat?.toString() ?: "--",
                            donVi = "%",
                            trangThaiCanhBao = diemDangXem?.let {
                                phanLoai(it.doAmDat.toDouble(), nguong.datMin.toDouble(), nguong.datMax.toDouble())
                            } ?: TrangThaiCanhBao.BINH_THUONG,
                            modifier = Modifier.weight(1f)
                        )
                        TheCamBien(
                            iconRes = R.drawable.humidity_high_24px,
                            nhan = "Mực nước",
                            giaTri = diemDangXem?.mucNuoc?.toString() ?: "--",
                            donVi = "%",
                            trangThaiCanhBao = when {
                                diemDangXem == null -> TrangThaiCanhBao.BINH_THUONG
                                diemDangXem.mucNuoc < 15 -> TrangThaiCanhBao.NGUY_HIEM
                                diemDangXem.mucNuoc < 30 -> TrangThaiCanhBao.CANH_BAO
                                else -> TrangThaiCanhBao.BINH_THUONG
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TheCamBien(
                            iconRes = R.drawable.light_mode_24px,
                            nhan = "Ánh sáng",
                            giaTri = diemDangXem?.let { "%.0f".format(it.anhSang) } ?: "--",
                            donVi = "lux",
                            trangThaiCanhBao = diemDangXem?.let {
                                if (it.anhSang < nguong.nguongDen || it.anhSang > nguong.nguongSangMax)
                                    TrangThaiCanhBao.CANH_BAO else TrangThaiCanhBao.BINH_THUONG
                            } ?: TrangThaiCanhBao.BINH_THUONG,
                            modifier = Modifier.weight(1f)
                        )
                        TheCamBien(
                            iconRes = if (diemDangXem?.cuaMo == true) R.drawable.door_open_24px else R.drawable.door_front_24px,
                            nhan = "Cửa",
                            giaTri = diemDangXem?.let { if (it.cuaMo) "Đang mở" else "Đã đóng" } ?: "--",
                            donVi = "",
                            trangThaiCanhBao = if (diemDangXem?.cuaMo == true) TrangThaiCanhBao.CANH_BAO else TrangThaiCanhBao.BINH_THUONG,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// ================================================================
// Ô TOGGLE CHỈ SỐ — vuông nhỏ, gọn, dùng cho lưới 6 ô
// ================================================================
@Composable
private fun MetricToggleBox(
    label: String,
    iconRes: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) color.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.03f))
            .border(
                width = 1.dp,
                color = if (selected) color.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (selected) color else ChuPhu,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) color else ChuPhu,
            maxLines = 1
        )
    }
}

// ================================================================
// DROPDOWN THỜI GIAN
// ================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassDropdownHeader(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.calendar_today_24px),
                contentDescription = null,
                tint = ChuPhu,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Khoảng thời gian",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            GlassTag(text = selected, color = XanhDuong)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.chevron_line_up_24px),
                contentDescription = null,
                tint = ChuPhu,
                modifier = Modifier.size(16.dp).rotate(if (expanded) 180f else 0f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(NenThe)
        ) {
            options.forEach { tuyChon ->
                val dangChon = tuyChon == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = tuyChon,
                            color = if (dangChon) XanhLa else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (dangChon) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(tuyChon)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ================================================================
// PHÂN LOẠI CẢNH BÁO THEO NGƯỠNG
// ================================================================
private fun phanLoai(giaTri: Double, min: Double, max: Double): TrangThaiCanhBao {
    val bienDo = (max - min).coerceAtLeast(0.01)
    return when {
        giaTri < min - bienDo * 0.15 || giaTri > max + bienDo * 0.15 -> TrangThaiCanhBao.NGUY_HIEM
        giaTri < min || giaTri > max -> TrangThaiCanhBao.CANH_BAO
        else -> TrangThaiCanhBao.BINH_THUONG
    }
}

// ================================================================
// BIỂU ĐỒ CANVAS 5 ĐƯỜNG — MỖI CHỈ SỐ TỰ CHUẨN HOÁ (NORMALIZE) RIÊNG
// ================================================================
@Composable
fun SimpleLineChart(
    data: List<LichSuDiem>,
    showNhietDo: Boolean,
    showDoAmKK: Boolean,
    showDoAmDat: Boolean,
    showMucNuoc: Boolean,
    showAnhSang: Boolean,
    selectedIndex: Int?,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val labels = remember(data) { data.map { it.thoiGian } }
    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = TextStyle(color = Color.LightGray, fontSize = 9.sp)

    val paddingLeft = 60f
    val paddingRight = 30f
    val paddingTop = 20f
    val paddingBottom = 45f

    // CHUẨN HOÁ TỪNG CHỈ SỐ VỀ 0..1 THEO MIN-MAX CỦA CHÍNH NÓ
    // -> mỗi đường tự co giãn hết chiều cao biểu đồ, không bị chỉ số có
    // biên độ lớn (vd ánh sáng 0-1000 lux) lấn át chỉ số biên độ nhỏ (vd % ẩm).
    fun chuanHoa(values: List<Float>): List<Float> {
        val mn = values.min()
        val mx = values.max()
        val bienDo = (mx - mn).coerceAtLeast(0.0001f)
        return values.map { (it - mn) / bienDo }
    }

    Column(modifier = modifier) {
        FlowRowLegend(showNhietDo, showDoAmKK, showDoAmDat, showMucNuoc, showAnhSang)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (data.size > 1 && chartWidth > 0f) {
                            val step = chartWidth / (data.size - 1)
                            val idx = ((tapOffset.x - paddingLeft) / step)
                                .roundToInt()
                                .coerceIn(0, data.size - 1)
                            onPointSelected(idx)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, paddingTop),
                end = Offset(paddingLeft, height - paddingBottom),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, height - paddingBottom),
                end = Offset(width - paddingRight, height - paddingBottom),
                strokeWidth = 2f
            )

            // Lưới ngang — hiển thị theo % biên độ tương đối (vì đã chuẩn hoá)
            for (i in 0..4) {
                val y = paddingTop + (chartHeight * i / 4)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1f
                )
                val phanTram = 100 - (i * 25)
                val textLayoutResult = textMeasurer.measure(
                    text = "$phanTram%",
                    style = axisTextStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(paddingLeft - textLayoutResult.size.width - 10f, y - textLayoutResult.size.height / 2)
                )
            }

            if (selectedIndex != null && data.size > 1) {
                val step = chartWidth / (data.size - 1)
                val x = paddingLeft + selectedIndex.coerceIn(0, data.size - 1) * step
                drawLine(
                    color = XanhLa.copy(alpha = 0.6f),
                    start = Offset(x, paddingTop),
                    end = Offset(x, height - paddingBottom),
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            fun drawLineFor(rawValues: List<Float>, color: Color) {
                if (rawValues.size < 2) return
                val values = chuanHoa(rawValues) // <-- normalize riêng cho từng chỉ số
                val path = Path()
                val step = chartWidth / (values.size - 1).coerceAtLeast(1)

                values.forEachIndexed { index, value ->
                    val x = paddingLeft + index * step
                    val y = paddingTop + chartHeight - (value * chartHeight)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path = path, color = color, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

                values.forEachIndexed { index, value ->
                    val x = paddingLeft + index * step
                    val y = paddingTop + chartHeight - (value * chartHeight)
                    val dangChon = index == selectedIndex
                    drawCircle(color = color, radius = if (dangChon) 6.5f else 4f, center = Offset(x, y))
                    if (dangChon) {
                        drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, y))
                    }
                }
            }

            if (showNhietDo) drawLineFor(data.map { it.nhietDo.toFloat() }, MauNhiet)
            if (showDoAmKK) drawLineFor(data.map { it.doAmKK.toFloat() }, MauAmKK)
            if (showDoAmDat) drawLineFor(data.map { it.doAmDat.toFloat() }, MauAmDat)
            if (showMucNuoc) drawLineFor(data.map { it.mucNuoc.toFloat() }, MauNuoc)
            if (showAnhSang) drawLineFor(data.map { it.anhSang.toFloat() }, MauSang)

            val labelCount = minOf(5, labels.size)
            val stepLabel = if (labels.size > 1) (labels.size - 1) / (labelCount - 1) else 1
            for (i in 0 until labelCount) {
                val index = i * stepLabel
                if (index < labels.size) {
                    val x = paddingLeft + (chartWidth * index / (labels.size - 1).coerceAtLeast(1))
                    val textLayoutResult = textMeasurer.measure(text = labels[index], style = axisTextStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(x - textLayoutResult.size.width / 2, height - paddingBottom + 10f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowLegend(
    showNhietDo: Boolean,
    showDoAmKK: Boolean,
    showDoAmDat: Boolean,
    showMucNuoc: Boolean,
    showAnhSang: Boolean
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        if (showNhietDo) LegendItem(color = MauNhiet, label = "Nhiệt độ")
        if (showDoAmKK) LegendItem(color = MauAmKK, label = "Độ ẩm KK")
        if (showDoAmDat) LegendItem(color = MauAmDat, label = "Độ ẩm đất")
        if (showMucNuoc) LegendItem(color = MauNuoc, label = "Mực nước")
        if (showAnhSang) LegendItem(color = MauSang, label = "Ánh sáng")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        Text(text = label, fontSize = 11.sp, color = ChuPhu)
    }
}