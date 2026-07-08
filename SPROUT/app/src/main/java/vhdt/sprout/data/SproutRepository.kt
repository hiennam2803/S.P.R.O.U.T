package vhdt.sprout.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Lớp giao tiếp duy nhất với Firebase Realtime Database.
 * Toàn bộ đường dẫn khớp với sprout_bridge_v3.py — KHÔNG đổi tên field ở đây
 * nếu không đồng bộ sửa lại bên Python.
 */
class SproutRepository(
    db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val goc = db.getReference("sprout")

    // ------------------------------------------------------------
    // ĐỌC DỮ LIỆU REAL-TIME (Flow)
    // ------------------------------------------------------------

    fun theoDoiCamBien(): Flow<CamBien> = docNode(goc.child("camBien")) { snap ->
        CamBien(
            nhietDo = snap.child("nhietDo").getValue(Double::class.java) ?: 0.0,
            doAmKK  = snap.child("doAmKK").getValue(Double::class.java) ?: 0.0,
            doAmDat = snap.child("doAmDat").getValue(Long::class.java)?.toInt() ?: 0,
            mucNuoc = snap.child("mucNuoc").getValue(Long::class.java)?.toInt() ?: 0,
            anhSang = snap.child("anhSang").getValue(Double::class.java) ?: 0.0,
            cuaMo   = snap.child("cuaMo").getValue(Boolean::class.java) ?: false
        )
    }

    fun theoDoiThietBi(): Flow<ThietBi> = docNode(goc.child("thietBi")) { snap ->
        ThietBi(
            quat   = snap.child("quat").getValue(Long::class.java)?.toInt() ?: 0,
            bom    = snap.child("bom").getValue(Long::class.java)?.toInt() ?: 0,
            suoi   = snap.child("suoi").getValue(Long::class.java)?.toInt() ?: 0,
            den    = snap.child("den").getValue(Long::class.java)?.toInt() ?: 0,
            hutAm  = snap.child("hutAm").getValue(Long::class.java)?.toInt() ?: 0,
            tangAm = snap.child("tangAm").getValue(Long::class.java)?.toInt() ?: 0
        )
    }

    fun theoDoiNguong(): Flow<Nguong> = docNode(goc.child("nguong")) { snap ->
        Nguong(
            nhietMin  = snap.child("nhietMin").getValue(Double::class.java) ?: 18.0,
            nhietMax  = snap.child("nhietMax").getValue(Double::class.java) ?: 30.0,
            amKKMin   = snap.child("amKKMin").getValue(Double::class.java) ?: 50.0,
            amKKMax   = snap.child("amKKMax").getValue(Double::class.java) ?: 85.0,
            datMin    = snap.child("datMin").getValue(Long::class.java)?.toInt() ?: 30,
            datMax    = snap.child("datMax").getValue(Long::class.java)?.toInt() ?: 70,
            nguongDen = snap.child("nguongDen").getValue(Double::class.java) ?: 300.0
        )
    }

    fun theoDoiTrangThai(): Flow<TrangThai> = docNode(goc.child("trangThai")) { snap ->
        TrangThai(
            online       = snap.child("online").getValue(Boolean::class.java) ?: false,
            arduinoReady = snap.child("arduinoReady").getValue(Boolean::class.java) ?: false,
            mode         = snap.child("mode").getValue(String::class.java) ?: "AUTO",
            nuocHet      = snap.child("nuocHet").getValue(Boolean::class.java) ?: false,
            aiMoiNhat    = snap.child("aiMoiNhat").getValue(String::class.java) ?: "",
            capNhatLuc   = snap.child("capNhatLuc").getValue(String::class.java) ?: ""
        )
    }

    fun theoDoiThongTinCay(): Flow<ThongTinCay?> = docNode(goc.child("thongTinCay")) { snap ->
        if (!snap.exists()) null else ThongTinCay(
            ten   = snap.child("ten").getValue(String::class.java) ?: "",
            nguon = snap.child("nguon").getValue(String::class.java) ?: "",
            nguong = NguongCay(
                nhietMin = snap.child("nguong/nhietMin").getValue(Double::class.java) ?: 0.0,
                nhietMax = snap.child("nguong/nhietMax").getValue(Double::class.java) ?: 0.0,
                amKKMin  = snap.child("nguong/amKKMin").getValue(Double::class.java) ?: 0.0,
                amKKMax  = snap.child("nguong/amKKMax").getValue(Double::class.java) ?: 0.0,
                datMin   = snap.child("nguong/datMin").getValue(Double::class.java) ?: 0.0,
                datMax   = snap.child("nguong/datMax").getValue(Double::class.java) ?: 0.0,
                luxMin   = snap.child("nguong/luxMin").getValue(Double::class.java) ?: 0.0
            ),
            ghiChu     = snap.child("ghiChu").getValue(String::class.java) ?: "",
            loi        = snap.child("loi").getValue(String::class.java),
            capNhatLuc = snap.child("capNhatLuc").getValue(String::class.java) ?: ""
        )
    }

    /** 50 điểm lịch sử gần nhất, đã sắp theo thời gian tăng dần — dùng cho biểu đồ */
    fun theoDoiLichSu(soDiem: Int = 50): Flow<List<LichSuDiem>> = callbackFlow {
        val ref = goc.child("lichSu").limitToLast(soDiem)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ds = snapshot.children.mapNotNull { c ->
                    try {
                        LichSuDiem(
                            thoiGian = c.child("thoiGian").getValue(String::class.java) ?: "",
                            nhietDo  = c.child("nhietDo").getValue(Double::class.java) ?: 0.0,
                            doAmKK   = c.child("doAmKK").getValue(Double::class.java) ?: 0.0,
                            doAmDat  = c.child("doAmDat").getValue(Long::class.java)?.toInt() ?: 0,
                            mucNuoc  = c.child("mucNuoc").getValue(Long::class.java)?.toInt() ?: 0,
                            anhSang  = c.child("anhSang").getValue(Double::class.java) ?: 0.0,
                            cuaMo    = c.child("cuaMo").getValue(Boolean::class.java) ?: false,
                            quat     = c.child("quat").getValue(Long::class.java)?.toInt() ?: 0,
                            bom      = c.child("bom").getValue(Long::class.java)?.toInt() ?: 0,
                            suoi     = c.child("suoi").getValue(Long::class.java)?.toInt() ?: 0,
                            den      = c.child("den").getValue(Long::class.java)?.toInt() ?: 0,
                            hutAm    = c.child("hutAm").getValue(Long::class.java)?.toInt() ?: 0,
                            tangAm   = c.child("tangAm").getValue(Long::class.java)?.toInt() ?: 0,
                            mode     = c.child("mode").getValue(String::class.java) ?: "AUTO"
                        )
                    } catch (e: Exception) { null }
                }
                trySend(ds)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** 20 cảnh báo gần nhất */
    fun theoDoiCanhBao(soLuong: Int = 20): Flow<List<CanhBaoItem>> =
        docList(goc.child("canhBao").limitToLast(soLuong)) { c ->
            CanhBaoItem(
                thoiGian = c.child("thoiGian").getValue(String::class.java) ?: "",
                noiDung  = c.child("noiDung").getValue(String::class.java) ?: ""
            )
        }

    /** log AI rule-based định kỳ gần nhất */
    fun theoDoiAiLog(soLuong: Int = 20): Flow<List<AiLogItem>> =
        docList(goc.child("aiLog").limitToLast(soLuong)) { c ->
            AiLogItem(
                thoiGian   = c.child("thoiGian").getValue(String::class.java) ?: "",
                tomTat     = c.child("tomTat").getValue(String::class.java) ?: "",
                khuyenNghi = c.child("khuyenNghi").getValue(String::class.java) ?: ""
            )
        }

    // ------------------------------------------------------------
    // GỬI LỆNH — ghi vào /sprout/lenh, bridge Python sẽ đọc & xóa đi
    // ------------------------------------------------------------

    /** AUTO / MANUAL */
    fun datCheDo(mode: String) {
        goc.child("lenh").setValue(mapOf("loai" to "SET_MODE", "giaTri" to mode))
    }

    /** Bật/tắt 1 thiết bị — CHỈ có tác dụng khi đang ở chế độ MANUAL */
    fun dieuKhienThietBi(thietBi: ThietBiId, bat: Boolean) {
        goc.child("lenh").setValue(
            mapOf("loai" to "CMD_${thietBi.maLenh}", "giaTri" to if (bat) "1" else "0")
        )
    }

    /** Chế độ "Personal" — người dùng tự tay chỉnh ngưỡng nhiệt độ */
    fun datNguongNhiet(min: Double, max: Double) {
        goc.child("lenh").setValue(mapOf("loai" to "SET_TEMP", "min" to min, "max" to max))
    }

    fun datNguongDoAmKK(min: Double, max: Double) {
        goc.child("lenh").setValue(mapOf("loai" to "SET_HUMI", "min" to min, "max" to max))
    }

    fun datNguongDoAmDat(min: Int, max: Int) {
        goc.child("lenh").setValue(mapOf("loai" to "SET_SOIL", "min" to min, "max" to max))
    }

    fun datNguongAnhSang(luxMin: Double) {
        goc.child("lenh").setValue(mapOf("loai" to "SET_LIGHT", "giaTri" to luxMin))
    }

    /** Chế độ "AI" — nhập tên cây, bridge tự tra cache / gọi Gemini nếu là cây mới */
    fun xacDinhLoaiCay(tenCay: String) {
        goc.child("lenh").setValue(mapOf("loai" to "SET_PLANT", "giaTri" to tenCay))
    }

    // ------------------------------------------------------------
    // HÀM TIỆN ÍCH DÙNG CHUNG
    // ------------------------------------------------------------

    private fun <T> docNode(
        ref: com.google.firebase.database.DatabaseReference,
        map: (DataSnapshot) -> T
    ): Flow<T> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(map(snapshot)) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun <T> docList(
        ref: com.google.firebase.database.Query,
        map: (DataSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.map { map(it) })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}