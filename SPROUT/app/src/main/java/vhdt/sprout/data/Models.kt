package vhdt.sprout.data


/** /sprout/camBien — cảm biến real-time (anhSang tính theo LUX) */
data class CamBien(
    val nhietDo: Double = 0.0,
    val doAmKK: Double = 0.0,
    val doAmDat: Int = 0,
    val mucNuoc: Int = 0,
    val anhSang: Double = 0.0,   // LUX
    val cuaMo: Boolean = false
)

/** /sprout/thietBi — trạng thái relay (0/1) */
data class ThietBi(
    val quat: Int = 0,
    val bom: Int = 0,
    val suoi: Int = 0,
    val den: Int = 0,
    val hutAm: Int = 0,
    val tangAm: Int = 0
)

/** /sprout/nguong — ngưỡng điều khiển hiện tại (nguongDen tính theo LUX) */
data class Nguong(
    val nhietMin: Double = 18.0,
    val nhietMax: Double = 30.0,
    val amKKMin: Double = 50.0,
    val amKKMax: Double = 85.0,
    val datMin: Int = 30,
    val datMax: Int = 70,
    val nguongDen: Double = 300.0
)

/** /sprout/trangThai — trạng thái tổng quan hệ thống */
data class TrangThai(
    val online: Boolean = false,
    val arduinoReady: Boolean = false,
    val mode: String = "AUTO",       // "AUTO" | "MANUAL"
    val nuocHet: Boolean = false,
    val aiMoiNhat: String = "",
    val capNhatLuc: String = ""
)

/** Ngưỡng đề xuất theo loại cây (nằm lồng trong ThongTinCay) */
data class NguongCay(
    val nhietMin: Double = 0.0,
    val nhietMax: Double = 0.0,
    val amKKMin: Double = 0.0,
    val amKKMax: Double = 0.0,
    val datMin: Double = 0.0,
    val datMax: Double = 0.0,
    val luxMin: Double = 0.0
)

/** /sprout/thongTinCay — kết quả AI Gemini hoặc cache cho loại cây hiện tại */
data class ThongTinCay(
    val ten: String = "",
    val nguon: String = "",          // "gemini" | "cache"
    val nguong: NguongCay = NguongCay(),
    val ghiChu: String = "",
    val loi: String? = null,
    val capNhatLuc: String = ""
)

/** 1 điểm dữ liệu trong /sprout/lichSu (push list, ghi mỗi 30s) */
data class LichSuDiem(
    val thoiGian: String = "",
    val nhietDo: Double = 0.0,
    val doAmKK: Double = 0.0,
    val doAmDat: Int = 0,
    val mucNuoc: Int = 0,
    val anhSang: Double = 0.0,
    val cuaMo: Boolean = false,
    val quat: Int = 0,
    val bom: Int = 0,
    val suoi: Int = 0,
    val den: Int = 0,
    val hutAm: Int = 0,
    val tangAm: Int = 0,
    val mode: String = "AUTO"
)

/** /sprout/canhBao — lịch sử cảnh báo (push list) */
data class CanhBaoItem(
    val thoiGian: String = "",
    val noiDung: String = ""
)

/** /sprout/aiLog — kết quả phân tích rule-based định kỳ (push list) */
data class AiLogItem(
    val thoiGian: String = "",
    val tomTat: String = "",
    val khuyenNghi: String = ""
)

/** Danh sách metric có thể vẽ ở màn hình biểu đồ */
enum class MetricBieuDo(val nhan: String, val donVi: String) {
    NHIET_DO("Nhiệt độ", "°C"),
    DO_AM_KK("Độ ẩm KK", "%"),
    DO_AM_DAT("Độ ẩm đất", "%"),
    MUC_NUOC("Mực nước", "%"),
    ANH_SANG("Ánh sáng", "lux")
}

/** Tên thiết bị dùng để gửi lệnh CMD_* (map sang tiếng Việt hiển thị) */
enum class ThietBiId(val maLenh: String, val nhan: String, val icon: String) {
    QUAT("QUAT", "Quạt thông gió", "💨"),
    BOM("BOM", "Bơm tưới", "💧"),
    SUOI("SUOI", "Sưởi ấm", "🔥"),
    DEN("DEN", "Đèn quang hợp", "💡"),
    HUTAM("HUTAM", "Hút ẩm", "🌬"),
    TANGAM("TANGAM", "Tăng ẩm", "🌫")
}