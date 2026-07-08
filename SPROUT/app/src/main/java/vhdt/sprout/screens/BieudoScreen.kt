package vhdt.sprout.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vhdt.sprout.data.LichSuDiem
import vhdt.sprout.ui.theme.ChuPhu
import vhdt.sprout.ui.theme.DoNguyHiem
import vhdt.sprout.ui.theme.NenChinh
import vhdt.sprout.ui.theme.NenThe
import vhdt.sprout.ui.theme.VangCanhBao
import vhdt.sprout.ui.theme.XanhDuong
import vhdt.sprout.ui.theme.XanhLa
import vhdt.sprout.viewmodel.SproutViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BieudoScreen(vm: SproutViewModel) {
    val lichSuGoc by vm.lichSu.collectAsState()

    // State quản lý Loại lấy dữ liệu: "Ngày" | "Tuần" | "Tháng"
    var filterThoiGian by remember { mutableStateOf("Ngày") }

    // State quản lý ẩn/hiện 5 chỉ số
    var showNhietDo by remember { mutableStateOf(true) }
    var showDoAmKK by remember { mutableStateOf(true) }
    var showDoAmDat by remember { mutableStateOf(true) }
    var showMucNuoc by remember { mutableStateOf(true) }
    var showAnhSang by remember { mutableStateOf(true) }

    val isAllChecked = showNhietDo && showDoAmKK && showDoAmDat && showMucNuoc && showAnhSang

    // THUẬT TOÁN XỬ LÝ GOM NHÓM DỮ LIỆU CHUẨN ĐỊNH DẠNG "2026-06-28 15:44:00"
    val lichSuDaXuLy = remember(lichSuGoc, filterThoiGian) {
        if (lichSuGoc.isEmpty()) emptyList()
        else {
            try {
                when (filterThoiGian) {
                    "Ngày" -> {
                        // Tách lấy phần giờ từ chuỗi mẫu (ví dụ "15:44:00" -> lấy "15" -> thành "15h")
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
                                anhSang = cacDiemTrongGio.map { it.anhSang }.average()
                            )
                        }.sortedBy { it.thoiGian }

                        // CỨU CÁNH: Nếu gom xong chỉ có 1 điểm duy nhất, bê nguyên data gốc ra vẽ cho có đường nối
                        if (mapped.size < 2) {
                            lichSuGoc.map { it.copy(thoiGian = it.thoiGian.substringAfter(" ").take(5)) }
                        } else mapped
                    }
                    "Tuần" -> {
                        // Tách lấy phần "Tháng-Ngày" (ví dụ "2026-06-28" -> lấy "06-28")
                        val mapped = lichSuGoc.groupBy { diem ->
                            diem.thoiGian.take(10).substringAfter("-")
                        }.map { (ngayThang, cacDiemTrongNgay) ->
                            LichSuDiem(
                                thoiGian = ngayThang,
                                nhietDo = cacDiemTrongNgay.map { it.nhietDo }.average(),
                                doAmKK = cacDiemTrongNgay.map { it.doAmKK }.average(),
                                doAmDat = cacDiemTrongNgay.map { it.doAmDat }.average().toInt(),
                                mucNuoc = cacDiemTrongNgay.map { it.mucNuoc }.average().toInt(),
                                anhSang = cacDiemTrongNgay.map { it.anhSang }.average()
                            )
                        }.sortedBy { it.thoiGian }.takeLast(7)

                        // Nếu chưa test qua nhiều ngày (size < 2), cứu cánh bằng cách lấy dữ liệu thô vẽ luôn
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
                                anhSang = cacDiemTrongNgay.map { it.anhSang }.average()
                            )
                        }.sortedBy { it.thoiGian }.takeLast(30)

                        if (mapped.size < 2) {
                            lichSuGoc.map { it.copy(thoiGian = it.thoiGian.substringAfter(" ").take(5)) }
                        } else mapped
                    }
                    else -> lichSuGoc
                }
            } catch (e: Exception) {
                // Sụp bẫy format chuỗi thì lấy data gốc cắt ngắn chuỗi thời gian hiển thị tạm
                lichSuGoc.map { it.copy(thoiGian = it.thoiGian.takeLast(8)) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NenChinh),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 Biểu đồ dữ liệu hệ thống",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ================= KHỐI BỘ LỌC THỜI GIAN VÀ CHỈ SỐ =================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NenThe),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 1. Bộ lọc Khoảng thời gian
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NenChinh, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Ngày", "Tuần", "Tháng").forEach { thoiGian ->
                            val isSelected = filterThoiGian == thoiGian
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { filterThoiGian = thoiGian }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = thoiGian,
                                    color = if (isSelected) Color.White else ChuPhu,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Bộ lọc Checkbox đầy đủ 5 thông số
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isAllChecked,
                                onCheckedChange = { checked ->
                                    showNhietDo = checked
                                    showDoAmKK = checked
                                    showDoAmDat = checked
                                    showMucNuoc = checked
                                    showAnhSang = checked
                                },
                                colors = CheckboxDefaults.colors(checkmarkColor = Color.White)
                            )
                            Text("Tất cả", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showNhietDo,
                                onCheckedChange = { showNhietDo = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF6347))
                            )
                            Text("Nhiệt độ", fontSize = 13.sp, color = ChuPhu)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showDoAmKK,
                                onCheckedChange = { showDoAmKK = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4682B4))
                            )
                            Text("Độ ẩm KK", fontSize = 13.sp, color = ChuPhu)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showDoAmDat,
                                onCheckedChange = { showDoAmDat = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E8B57))
                            )
                            Text("Độ ẩm đất", fontSize = 13.sp, color = ChuPhu)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showMucNuoc,
                                onCheckedChange = { showMucNuoc = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E90FF))
                            )
                            Text("Mực nước", fontSize = 13.sp, color = ChuPhu)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = showAnhSang,
                                onCheckedChange = { showAnhSang = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700))
                            )
                            Text("Ánh sáng", fontSize = 13.sp, color = ChuPhu)
                        }
                    }
                }
            }
        }

        // ---- Khối Biểu đồ Canvas ----
        item {
            if (lichSuDaXuLy.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(NenThe, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có dữ liệu lịch sử phù hợp", color = ChuPhu, fontSize = 14.sp)
                }
            } else {
                SimpleLineChart(
                    data = lichSuDaXuLy,
                    showNhietDo = showNhietDo,
                    showDoAmKK = showDoAmKK,
                    showDoAmDat = showDoAmDat,
                    showMucNuoc = showMucNuoc,
                    showAnhSang = showAnhSang,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }

        // ---- Các chỉ số phụ hiển thị real-time điểm cuối ----
        item {
            Text(
                text = "📊 Chi tiết hiện tại",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val thongTinCuoi = lichSuGoc.lastOrNull()
                StatCard(
                    icon = "🌱",
                    label = "Độ ẩm đất",
                    value = thongTinCuoi?.let { "${it.doAmDat}%" } ?: "--",
                    color = if (thongTinCuoi != null && thongTinCuoi.doAmDat < 30) DoNguyHiem else XanhLa,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "💧",
                    label = "Mực nước",
                    value = thongTinCuoi?.let { "${it.mucNuoc}%" } ?: "--",
                    color = if (thongTinCuoi != null && thongTinCuoi.mucNuoc < 20) DoNguyHiem else XanhDuong,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val thongTinCuoi = lichSuGoc.lastOrNull()
                StatCard(
                    icon = "🚪",
                    label = "Cửa",
                    value = thongTinCuoi?.let { if (it.cuaMo) "Đang mở" else "Đã đóng" } ?: "--",
                    color = if (thongTinCuoi != null && thongTinCuoi.cuaMo) VangCanhBao else XanhLa,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "🌡️",
                    label = "Nhiệt độ",
                    value = thongTinCuoi?.let { "${it.nhietDo.toInt()}°C" } ?: "--",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NenThe.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(text = "$icon $label", fontSize = 12.sp, color = ChuPhu)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ================================================================
// BIỂU ĐỒ CANVAS 5 ĐƯỜNG ĐỘNG TOÀN DIỆN
// ================================================================

@Composable
fun SimpleLineChart(
    data: List<LichSuDiem>,
    showNhietDo: Boolean,
    showDoAmKK: Boolean,
    showDoAmDat: Boolean,
    showMucNuoc: Boolean,
    showAnhSang: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val labels = remember(data) { data.map { it.thoiGian } }

    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = TextStyle(color = Color.LightGray, fontSize = 9.sp)

    val tempMin = data.minOf { it.nhietDo }.toFloat()
    val tempMax = data.maxOf { it.nhietDo }.toFloat()
    val humMin = data.minOf { it.doAmKK }.toFloat()
    val humMax = data.maxOf { it.doAmKK }.toFloat()
    val soilMin = data.minOf { it.doAmDat }.toFloat()
    val soilMax = data.maxOf { it.doAmDat }.toFloat()
    val waterMin = data.minOf { it.mucNuoc }.toFloat()
    val waterMax = data.maxOf { it.mucNuoc }.toFloat()
    val lightMin = data.minOf { it.anhSang }.toFloat()
    val lightMax = data.maxOf { it.anhSang }.toFloat()

    val globalMin = minOf(tempMin, humMin, soilMin, waterMin, lightMin) * 0.9f
    val globalMax = maxOf(tempMax, humMax, soilMax, waterMax, lightMax) * 1.1f
    val range = (globalMax - globalMin).coerceAtLeast(1f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NenThe),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                if (showNhietDo) LegendItem(color = Color(0xFFFF6347), label = "Nhiệt độ")
                if (showDoAmKK) LegendItem(color = Color(0xFF4682B4), label = "Độ ẩm KK")
                if (showDoAmDat) LegendItem(color = Color(0xFF2E8B57), label = "Độ ẩm đất")
                if (showMucNuoc) LegendItem(color = Color(0xFF1E90FF), label = "Mực nước")
                if (showAnhSang) LegendItem(color = Color(0xFFFFD700), label = "Ánh sáng")
            }

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 70f
                val paddingRight = 30f
                val paddingTop = 20f
                val paddingBottom = 45f

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // Trục tọa độ
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

                // Lưới ngang
                for (i in 0..4) {
                    val y = paddingTop + (chartHeight * i / 4)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f
                    )

                    val value = globalMax - (range * i / 4)
                    val textLayoutResult = textMeasurer.measure(
                        text = String.format(Locale.US, "%.0f", value),
                        style = axisTextStyle
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(paddingLeft - textLayoutResult.size.width - 10f, y - textLayoutResult.size.height / 2)
                    )
                }

                // Hàm vẽ đường
                fun drawLineFor(values: List<Float>, color: Color) {
                    if (values.size < 2) return
                    val path = Path()
                    val step = chartWidth / (values.size - 1).coerceAtLeast(1)

                    values.forEachIndexed { index, value ->
                        val x = paddingLeft + index * step
                        val y = paddingTop + chartHeight - ((value - globalMin) / range * chartHeight)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )

                    values.forEachIndexed { index, value ->
                        val x = paddingLeft + index * step
                        val y = paddingTop + chartHeight - ((value - globalMin) / range * chartHeight)
                        drawCircle(
                            color = color,
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }
                }

                if (showNhietDo) drawLineFor(data.map { it.nhietDo.toFloat() }, Color(0xFFFF6347))
                if (showDoAmKK) drawLineFor(data.map { it.doAmKK.toFloat() }, Color(0xFF4682B4))
                if (showDoAmDat) drawLineFor(data.map { it.doAmDat.toFloat() }, Color(0xFF2E8B57))
                if (showMucNuoc) drawLineFor(data.map { it.mucNuoc.toFloat() }, Color(0xFF1E90FF))
                if (showAnhSang) drawLineFor(data.map { it.anhSang.toFloat() }, Color(0xFFFFD700))

                // Mốc trục X linh hoạt
                val labelCount = minOf(5, labels.size)
                val stepLabel = if (labels.size > 1) (labels.size - 1) / (labelCount - 1) else 1
                for (i in 0 until labelCount) {
                    val index = i * stepLabel
                    if (index < labels.size) {
                        val x = paddingLeft + (chartWidth * index / (labels.size - 1).coerceAtLeast(1))
                        val textLayoutResult = textMeasurer.measure(
                            text = labels[index],
                            style = axisTextStyle
                        )

                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(x - textLayoutResult.size.width / 2, height - paddingBottom + 10f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(50))
        )
        Text(text = label, fontSize = 11.sp, color = ChuPhu)
    }
}