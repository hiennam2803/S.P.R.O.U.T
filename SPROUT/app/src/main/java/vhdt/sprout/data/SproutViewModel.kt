package vhdt.sprout.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vhdt.sprout.data.AiLogItem
import vhdt.sprout.data.CamBien
import vhdt.sprout.data.CanhBaoItem
import vhdt.sprout.data.LichSuDiem
import vhdt.sprout.data.Nguong
import vhdt.sprout.data.SproutRepository
import vhdt.sprout.data.ThietBi
import vhdt.sprout.data.ThietBiId
import vhdt.sprout.data.ThongTinCay
import vhdt.sprout.data.TrangThai

/**
 * ViewModel duy nhất dùng chung cho cả 4 màn hình (Tổng quan, Loại cây,
 * Tự động/Thủ công, Biểu đồ). Mọi màn hình chỉ đọc từ đây — không tự mở
 * kết nối Firebase riêng, tránh rò rỉ listener khi xoay màn hình / điều hướng.
 */
class SproutViewModel @JvmOverloads constructor(
    private val repo: SproutRepository = SproutRepository()
) : ViewModel() {

    private val _camBien = MutableStateFlow(CamBien())
    val camBien: StateFlow<CamBien> = _camBien.asStateFlow()

    private val _thietBi = MutableStateFlow(ThietBi())
    val thietBi: StateFlow<ThietBi> = _thietBi.asStateFlow()

    private val _nguong = MutableStateFlow(Nguong())
    val nguong: StateFlow<Nguong> = _nguong.asStateFlow()

    private val _trangThai = MutableStateFlow(TrangThai())
    val trangThai: StateFlow<TrangThai> = _trangThai.asStateFlow()

    private val _thongTinCay = MutableStateFlow<ThongTinCay?>(null)
    val thongTinCay: StateFlow<ThongTinCay?> = _thongTinCay.asStateFlow()

    private val _lichSu = MutableStateFlow<List<LichSuDiem>>(emptyList())
    val lichSu: StateFlow<List<LichSuDiem>> = _lichSu.asStateFlow()

    private val _canhBao = MutableStateFlow<List<CanhBaoItem>>(emptyList())
    val canhBao: StateFlow<List<CanhBaoItem>> = _canhBao.asStateFlow()

    private val _aiLog = MutableStateFlow<List<AiLogItem>>(emptyList())
    val aiLog: StateFlow<List<AiLogItem>> = _aiLog.asStateFlow()

    /** Đang gửi lệnh xác định cây (đang chờ bridge trả lời qua thongTinCay) */
    private val _dangXuLyCay = MutableStateFlow(false)
    val dangXuLyCay: StateFlow<Boolean> = _dangXuLyCay.asStateFlow()

    init {
        viewModelScope.launch { repo.theoDoiCamBien().collect { _camBien.value = it } }
        viewModelScope.launch { repo.theoDoiThietBi().collect { _thietBi.value = it } }
        viewModelScope.launch { repo.theoDoiNguong().collect { _nguong.value = it } }
        viewModelScope.launch { repo.theoDoiTrangThai().collect { _trangThai.value = it } }
        viewModelScope.launch {
            repo.theoDoiThongTinCay().collect {
                _thongTinCay.value = it
                _dangXuLyCay.value = false
            }
        }
        viewModelScope.launch { repo.theoDoiLichSu().collect { _lichSu.value = it } }
        viewModelScope.launch { repo.theoDoiCanhBao().collect { _canhBao.value = it } }
        viewModelScope.launch { repo.theoDoiAiLog().collect { _aiLog.value = it } }
    }

    // ---------------- Màn "Tự động / Thủ công" ----------------

    fun datCheDo(mode: String) = repo.datCheDo(mode)

    fun dieuKhienThietBi(thietBi: ThietBiId, bat: Boolean) =
        repo.dieuKhienThietBi(thietBi, bat)

    // ---------------- Màn "Loại cây" ----------------

    /** Chế độ AI: nhập tên cây, bridge tự tra cache / gọi Gemini nếu cây mới */
    fun xacDinhLoaiCay(tenCay: String) {
        if (tenCay.isBlank()) return
        _dangXuLyCay.value = true
        repo.xacDinhLoaiCay(tenCay.trim())
    }

    /** Chế độ Personal: người dùng tự áp ngưỡng, không qua AI */
    fun apDungNguongCaNhan(
        nhietMin: Double, nhietMax: Double,
        amKKMin: Double, amKKMax: Double,
        datMin: Int, datMax: Int,
        luxMin: Double
    ) {
        repo.datNguongNhiet(nhietMin, nhietMax)
        repo.datNguongDoAmKK(amKKMin, amKKMax)
        repo.datNguongDoAmDat(datMin, datMax)
        repo.datNguongAnhSang(luxMin)
    }
}