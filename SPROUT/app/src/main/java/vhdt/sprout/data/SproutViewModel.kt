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

    // --- TÊN CÂY (hiển thị ở Tổng quan) ---
    private val _tenCay = MutableStateFlow("S.P.R.O.U.T")
    val tenCay: StateFlow<String> = _tenCay

    private val _dangXuLyCay = MutableStateFlow(false)
    val dangXuLyCay: StateFlow<Boolean> = _dangXuLyCay.asStateFlow()

    init {
        viewModelScope.launch { repo.theoDoiCamBien().collect { _camBien.value = it } }
        viewModelScope.launch { repo.theoDoiThietBi().collect { _thietBi.value = it } }
        viewModelScope.launch { repo.theoDoiNguong().collect { _nguong.value = it } }
        viewModelScope.launch { repo.theoDoiTrangThai().collect { _trangThai.value = it } }

        // Lắng nghe thông tin cây và cập nhật cả _thongTinCay lẫn _tenCay
        viewModelScope.launch {
            repo.theoDoiThongTinCay().collect {
                _thongTinCay.value = it
                _dangXuLyCay.value = false
                // CẬP NHẬT TÊN CÂY CHO MÀN HÌNH TỔNG QUAN
                _tenCay.value = it?.ten ?: "S.P.R.O.U.T"
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

    fun xacDinhLoaiCay(tenCay: String) {
        if (tenCay.isBlank()) return
        _dangXuLyCay.value = true
        repo.xacDinhLoaiCay(tenCay.trim())
    }

    fun apDungNguongCaNhan(
        nhietMin: Double, nhietMax: Double,
        amKKMin: Double, amKKMax: Double,
        datMin: Int, datMax: Int,
        sangMin: Double, sangMax: Double
    ) {
        repo.datNguongNhiet(nhietMin, nhietMax)
        repo.datNguongDoAmKK(amKKMin, amKKMax)
        repo.datNguongDoAmDat(datMin, datMax)
        repo.datNguongAnhSang(sangMin, sangMax)
    }
}