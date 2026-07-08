"""
S.P.R.O.U.T — Python Bridge v3.5
Nâng cấp từ v3.4: THÊM NGƯỠNG "QUÁ SÁNG" (sangMax)

  THAY ĐỔI SO VỚI v3.4:
    - Trước đây ánh sáng chỉ có 1 ngưỡng (sangMin/nguongDen) để bật đèn
      quang hợp khi thiếu sáng, trong khi nhiệt độ/độ ẩm KK/độ ẩm đất đều
      có CẶP ngưỡng Min-Max đầy đủ. Hệ quả: Gemini không có chỗ đề xuất
      mức "quá sáng" (vd cây bị cháy lá dưới nắng gắt), hệ thống cũng
      không cảnh báo trường hợp này.
    - Đã thêm field "sangMax" vào hồ sơ cây (Gemini + cache + Firebase),
      thêm "nguongSangMax" vào self.nguong, tăng số field DATA từ Arduino
      lên 22 (thêm nguongSangMax trước "mode").
    - Lệnh SET_LIGHT đổi từ 1 tham số sang 2 tham số (min, max) — khớp
      với thay đổi bên .ino. Web/Firebase gửi lệnh SET_LIGHT giờ cần cả
      "min" và "max" (giống SET_TEMP/SET_SOIL/SET_HUMI).
    - LƯU Ý: phần cứng KHÔNG có thiết bị che nắng, nên sangMax chỉ dùng
      để CẢNH BÁO (giống cách datMax cảnh báo "ngập" mà không có máy hút
      nước riêng), không điều khiển relay nào.

  (Các ghi chú của v3.4 vẫn giữ nguyên bên dưới)
  ------------------------------------------------------------
  THAY ĐỔI SO VỚI v3.3:
    - FIX LỖI NGHIÊM TRỌNG: Arduino tính PA_anhSang và nguong_den theo
      LUX THỰC TẾ (0-100000, xem docAnhSangLux() bên .ino), nhưng toàn bộ
      phần Python + prompt Gemini trước đây lại giả định đơn vị là %
      (0-100). Hệ quả: khi Gemini trả về "sangMin" (nghĩ là %, ví dụ 70),
      giá trị đó được gửi thẳng xuống Arduino qua lệnh SET_LIGHT,70 —
      nhưng Arduino hiểu đó là 70 LUX (rất khác ý nghĩa ban đầu). Điều
      này khiến ngưỡng "thiếu sáng" bị lệch hoàn toàn so với thực tế,
      gây cảnh báo sai (báo thiếu sáng dù ánh sáng đo được vẫn ổn, hoặc
      ngược lại). Đã đổi toàn bộ pipeline (Python + web) sang dùng LUX
      thống nhất với Arduino — không còn quy đổi % ở đâu cả.
    - Prompt Gemini giờ yêu cầu "sangMin" theo LUX (có gợi ý khoảng giá
      trị tham khảo) thay vì %.
    - nguongDen mặc định đổi từ 40.0 (%) -> 300.0 (lux) cho khớp với
      giá trị mặc định nguong_den = 300 bên .ino.

  (Các ghi chú của v3.3 vẫn giữ nguyên bên dưới)
  ------------------------------------------------------------
  THAY ĐỔI SO VỚI v3.2:
    - FIX LỖI NGHIÊM TRỌNG: xu_ly_loai_cay() trước đây khi lấy hồ sơ cây
      từ cache KHÔNG kiểm tra xem cache có đủ field hay không. Nếu 1 cây
      trong plant_profiles.json bị thiếu field (do lưu từ phiên bản cũ,
      hoặc do lỗi ghi file) thì dòng self.send(f"SET_LIGHT,{ho_so['sangMin']}")
      sẽ crash với KeyError -> làm chết thread giữa chừng -> các lệnh
      sau đó (SET_TENCAY, ghi Firebase thongTinCay) KHÔNG BAO GIỜ chạy
      -> LCD và web không cập nhật tên cây/ngưỡng dù log báo "đã áp dụng".
      Đã thêm kiểm tra: nếu cache thiếu field bắt buộc thì tự động xóa
      cache lỗi đó và gọi lại Gemini để lấy hồ sơ mới, đầy đủ.
    - Bọc toàn bộ phần áp ngưỡng xuống Arduino + ghi Firebase trong
      try/except để nếu có lỗi bất ngờ khác cũng không làm chết thread
      một cách âm thầm — sẽ log rõ ràng và ghi lỗi lên Firebase để web
      hiển thị cho người dùng biết thay vì im lặng "treo".

  (Các ghi chú của v3.2 vẫn giữ nguyên bên dưới)
  ------------------------------------------------------------
  THAY ĐỔI SO VỚI v3.1:
    - GHI NHỚ cây đang trồng: lưu vào current_plant.json, mỗi lần bridge
      khởi động lại sẽ tự áp lại đúng cây/ngưỡng cũ (không cần nhập lại)
    - Gửi tên cây (đã bỏ dấu) xuống Arduino bằng lệnh SET_TENCAY để LCD
      vật lý cũng hiển thị đúng cây đang trồng

Pipeline: Proteus → VSPE (COM ảo) → Serial → Firebase RTDB → Web 3D

Cấu trúc Firebase (web 3D đọc từ đây):
  /sprout/
    camBien/        <- cảm biến real-time từ Arduino (anhSang tính theo LUX)
    thietBi/        <- trạng thái relay
    nguong/         <- ngưỡng điều khiển hiện tại (nguongDen tính theo LUX)
    lenh/           <- webapp ghi vào đây để điều khiển (gồm cả SET_PLANT)
    canhBao/        <- lịch sử cảnh báo (push)
    aiLog/          <- kết quả PHÂN TÍCH ĐỊNH KỲ (rule-based, KHÔNG dùng Gemini
                        để tránh tốn token — đây là "thông báo mặc định")
    thongTinCay/    <- thông tin loại cây hiện tại + ngưỡng do Gemini đề xuất
    trangThai/      <- online/offline, mode, aiMoiNhat, canhBaoMoi

  GHI CHÚ VỀ AI:
    - Phân tích/tư vấn định kỳ mỗi AI_INTERVAL giây: RULE-BASED, chạy offline,
      KHÔNG gọi API ngoài. Đây là hành vi MẶC ĐỊNH của hệ thống.
    - Gemini CHỈ được gọi khi người dùng nhập một LOẠI CÂY MỚI (chưa có trong
      file cache plant_profiles.json, hoặc cache của cây đó bị lỗi/thiếu
      field). Nếu cây đã có trong cache và hợp lệ, hệ thống dùng lại kết
      quả cũ, KHÔNG gọi API -> tiết kiệm token tối đa.
"""

import serial
import serial.tools.list_ports
import threading
import time
import json
import sys
import os
import re
import unicodedata
import requests
from datetime import datetime

import firebase_admin
from firebase_admin import credentials, db


# ================================================================
# CẤU HÌNH — CHỈNH Ở ĐÂY
# ================================================================

SERIAL_PORT  = "COM4"        # Cổng COM phía Python trong cặp VSPE
                              # (Proteus dùng COM3, Python dùng COM4 hoặc ngược lại)
BAUD_RATE    = 9600
FIREBASE_URL = "https://sprout-3609f-default-rtdb.asia-southeast1.firebasedatabase.app/"
SERVICE_KEY  = "sprout-3609f-firebase-adminsdk-fbsvc-eabd4ae960.json"

AI_INTERVAL  = 30            # Chu kỳ phân tích rule-based mặc định (giây) — KHÔNG gọi Gemini
HISTORY_INTERVAL = 30        # Ghi lịch sử mỗi N giây

# Trần lux thực tế mà LDR trong mạch Proteus demo này đo được (~1000 lux,
# do đặc tính linh kiện/nguồn sáng mô phỏng — KHÔNG phải giới hạn công thức).
# Dùng để ghim ngưỡng sáng do Gemini đề xuất, không dùng thang lux thật
# ngoài trời (có thể lên tới hàng chục nghìn lux) cho bản demo Proteus.
PROTEUS_LUX_MAX = 1000

# ----------------------------------------------------------------
# CẤU HÌNH GEMINI AI — chỉ gọi khi người dùng nhập LOẠI CÂY MỚI
# ----------------------------------------------------------------
GEMINI_API_KEY  = ""   # Lấy tại https://aistudio.google.com/apikey
GEMINI_MODEL    = "gemini-2.5-flash"                      # Model rẻ, nhanh, đủ dùng cho tác vụ này
GEMINI_ENDPOINT = (
    f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent"
)

# File cache local — lưu ngưỡng đã hỏi Gemini để KHÔNG hỏi lại (tiết kiệm token)
PLANT_CACHE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "plant_profiles.json")

# File lưu "cây đang trồng hiện tại" — để bridge khởi động lại vẫn nhớ,
# không cần người dùng nhập lại tên cây mỗi lần bật hệ thống
CURRENT_PLANT_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "current_plant.json")

# Các field BẮT BUỘC phải có trong 1 hồ sơ cây hợp lệ (dùng để validate
# cả kết quả mới từ Gemini LẪN dữ liệu đọc từ cache cũ trên đĩa)
PLANT_REQUIRED_FIELDS = ["nhietMin", "nhietMax", "amKKMin", "amKKMax",
                          "datMin", "datMax", "sangMin", "sangMax"]


# ================================================================
# TIỆN ÍCH
# ================================================================

def now() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

def log(tag: str, msg: str):
    print(f"[{now()}] [{tag}] {msg}", flush=True)

def list_com_ports():
    ports = serial.tools.list_ports.comports()
    if ports:
        log("COM", "Các cổng COM hiện có:")
        for p in ports:
            log("COM", f"  {p.device} — {p.description}")
    else:
        log("COM", "Không tìm thấy cổng COM nào!")


def ho_so_hop_le(ho_so: dict) -> bool:
    """Kiểm tra 1 hồ sơ cây (dù lấy từ cache hay từ Gemini) có đủ toàn bộ
    field bắt buộc hay không. Dùng chung cho cả 2 nguồn để tránh lặp code
    và tránh sót trường hợp — đây chính là chỗ đã thiếu ở bản v3.2 khiến
    cache lỗi lọt qua và làm crash thread."""
    if not isinstance(ho_so, dict):
        return False
    return all(k in ho_so for k in PLANT_REQUIRED_FIELDS)


# ================================================================
# CHUẨN HÓA TÊN CÂY & CACHE JSON (để hạn chế gọi Gemini)
# ================================================================

def chuan_hoa_ten_cay(ten: str) -> str:
    """Đưa tên cây về dạng khóa thống nhất: bỏ dấu, chữ thường, gộp khoảng
    trắng — để 'Cà Chua', 'ca chua', 'CÀ CHUA ' đều trỏ vào cùng 1 cache."""
    ten = ten.strip().lower()
    ten = unicodedata.normalize("NFD", ten)
    ten = "".join(c for c in ten if unicodedata.category(c) != "Mn")
    ten = ten.replace("đ", "d")
    ten = re.sub(r"\s+", " ", ten).strip()
    return ten


def load_plant_cache() -> dict:
    if os.path.exists(PLANT_CACHE_FILE):
        try:
            with open(PLANT_CACHE_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            log("Cache", f"Lỗi đọc {PLANT_CACHE_FILE}: {e} — bắt đầu cache rỗng")
    return {}


def save_plant_cache(cache: dict):
    try:
        with open(PLANT_CACHE_FILE, "w", encoding="utf-8") as f:
            json.dump(cache, f, ensure_ascii=False, indent=2)
    except OSError as e:
        log("Cache", f"Lỗi ghi {PLANT_CACHE_FILE}: {e}")


def load_current_plant():
    """Đọc cây đang trồng đã lưu từ lần chạy trước (nếu có)."""
    if os.path.exists(CURRENT_PLANT_FILE):
        try:
            with open(CURRENT_PLANT_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            log("Cache", f"Lỗi đọc {CURRENT_PLANT_FILE}: {e}")
    return None


def save_current_plant(ten_cay_goc: str):
    """Ghi lại tên cây gốc (chưa chuẩn hóa) đang được chọn, để lần khởi
    động sau bridge tự áp lại đúng cây này mà không cần nhập lại."""
    try:
        with open(CURRENT_PLANT_FILE, "w", encoding="utf-8") as f:
            json.dump({"tenGoc": ten_cay_goc, "capNhatLuc": now()}, f, ensure_ascii=False, indent=2)
    except OSError as e:
        log("Cache", f"Lỗi ghi {CURRENT_PLANT_FILE}: {e}")


def bo_dau_lcd(ten: str, gioi_han: int = 16) -> str:
    """Bỏ dấu tiếng Việt (giữ nguyên hoa/thường) để hiển thị lên màn hình
    LCD (LCD HD44780 không hiển thị được ký tự có dấu). Cũng bỏ dấu phẩy
    vì đó là ký tự phân tách trong giao thức Serial."""
    ten = unicodedata.normalize("NFD", ten)
    ten = "".join(c for c in ten if unicodedata.category(c) != "Mn")
    ten = ten.replace("đ", "d").replace("Đ", "D")
    ten = ten.replace(",", " ")
    ten = re.sub(r"\s+", " ", ten).strip()
    return ten[:gioi_han]


# ================================================================
# KHỞI TẠO FIREBASE
# ================================================================

def init_firebase():
    try:
        cred = credentials.Certificate(SERVICE_KEY)
        firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_URL})
        log("Firebase", "Kết nối thành công")
        return True
    except Exception as e:
        log("Firebase", f"LỖI khởi tạo: {e}")
        return False


# ================================================================
# BRIDGE CLASS
# ================================================================

class SproutBridge:

    def __init__(self):
        self.ser     = None
        self.running = False
        self.lock    = threading.Lock()

        # --- Trạng thái hiện tại (đồng bộ với Arduino) ---
        self.camBien = {
            "nhietDo": 0.0, "doAmKK": 0.0, "doAmDat": 0,
            "mucNuoc": 0,   "anhSang": 0.0, "cuaMo": False   # anhSang: LUX (khong phai %)
        }
        self.thietBi = {
            "quat": 0, "bom": 0, "suoi": 0,
            "den": 0,  "hutAm": 0, "tangAm": 0
        }
        self.nguong = {
            "nhietMin": 18.0, "nhietMax": 30.0,
            "amKKMin":  50.0, "amKKMax":  85.0,
            "datMin":   30,   "datMax":   70,
            "nguongDen": 300.0,     # LUX (khop voi mac dinh nguong_den ben .ino)
            "nguongSangMax": 900.0  # LUX — gioi han theo LDR Proteus (~1000 lux max),
                                    # khop voi mac dinh nguong_sangMax ben .ino
        }
        self.mode        = "AUTO"
        self.nuocHet     = False
        self.lastAiTime  = 0
        self.lastHistory = 0
        self.ai_running  = False   # Cờ để không chạy AI chồng chéo

        # --- Cache hồ sơ cây (tránh gọi Gemini lặp lại) ---
        self.plant_cache = load_plant_cache()
        self.currentPlant = None
        self.plant_lock = threading.Lock()   # Khóa xử lý cây
        log("Cache", f"Đã nạp {len(self.plant_cache)} hồ sơ cây từ {PLANT_CACHE_FILE}")

    # ----------------------------------------------------------------
    # KẾT NỐI SERIAL
    # ----------------------------------------------------------------

    def connect(self) -> bool:
        list_com_ports()
        try:
            self.ser = serial.Serial(
                port=SERIAL_PORT,
                baudrate=BAUD_RATE,
                timeout=1,
                bytesize=serial.EIGHTBITS,
                parity=serial.PARITY_NONE,
                stopbits=serial.STOPBITS_ONE
            )
            time.sleep(2)  # Chờ Arduino reset
            log("Serial", f"Kết nối {SERIAL_PORT} @ {BAUD_RATE} baud — OK")

            db.reference("/sprout/trangThai").update({
                "online": True,
                "capNhatLuc": now()
            })
            return True

        except serial.SerialException as e:
            log("Serial", f"LỖI mở cổng {SERIAL_PORT}: {e}")
            log("Serial", "Gợi ý: kiểm tra VSPE đã tạo COM pair chưa, "
                          "Proteus đang dùng cổng nào?")
            return False

    def disconnect(self):
        self.running = False
        try:
            db.reference("/sprout/trangThai").update({
                "online": False,
                "capNhatLuc": now()
            })
        except Exception:
            pass
        if self.ser and self.ser.is_open:
            self.ser.close()
        log("Serial", "Đã đóng kết nối")

    # ----------------------------------------------------------------
    # VÒNG LẶP ĐỌC SERIAL
    # ----------------------------------------------------------------

    def read_loop(self):
        buf = ""
        while self.running:
            try:
                if self.ser.in_waiting:
                    chunk = self.ser.read(self.ser.in_waiting).decode("utf-8", errors="ignore")
                    buf += chunk
                    while "\n" in buf:
                        line, buf = buf.split("\n", 1)
                        line = line.strip()
                        if line:
                            self.handle_arduino(line)
            except serial.SerialException:
                log("Serial", "Mất kết nối Serial!")
                self.running = False
                break
            except Exception as e:
                log("Serial", f"Lỗi đọc: {e}")
            time.sleep(0.02)

    def handle_arduino(self, line: str):
        parts = line.split(",")
        if not parts:
            return
        kind = parts[0].upper()

        # ---- DATA chính ----
        # DATA,nhiet,amKK,dat,nuoc,sang,cua,
        #      quat,bom,suoi,den,hutam,tangam,
        #      nhietMin,nhietMax,amMin,amMax,datMin,datMax,
        #      nguongDen,nguongSangMax,mode
        if kind == "DATA" and len(parts) >= 22:
            try:
                with self.lock:
                    self.camBien = {
                        "nhietDo": round(float(parts[1]), 1),
                        "doAmKK":  round(float(parts[2]), 1),
                        "doAmDat": int(parts[3]),
                        "mucNuoc": int(parts[4]),
                        "anhSang": round(float(parts[5]), 1),   # LUX (khong phai %)
                        "cuaMo":   parts[6] == "1"
                    }
                    self.thietBi = {
                        "quat":   int(parts[7]),
                        "bom":    int(parts[8]),
                        "suoi":   int(parts[9]),
                        "den":    int(parts[10]),
                        "hutAm":  int(parts[11]),
                        "tangAm": int(parts[12])
                    }
                    self.nguong = {
                        "nhietMin":  round(float(parts[13]), 1),
                        "nhietMax":  round(float(parts[14]), 1),
                        "amKKMin":   round(float(parts[15]), 1),
                        "amKKMax":   round(float(parts[16]), 1),
                        "datMin":    int(parts[17]),
                        "datMax":    int(parts[18]),
                        "nguongDen": round(float(parts[19]), 1),   # LUX (khong phai %)
                        "nguongSangMax": round(float(parts[20]), 1)  # LUX
                    }
                    self.mode    = parts[21].strip().upper()
                    self.nuocHet = self.camBien["mucNuoc"] < 10

                self.push_firebase()
                self.maybe_ai()

            except (ValueError, IndexError) as e:
                log("Parse", f"Lỗi parse DATA: {e} | line={line}")

        # ---- ALERT ----
        elif kind == "ALERT":
            content = ",".join(parts[1:])
            self.write_alert(content)

        # ---- ACK từ Arduino ----
        elif kind == "ACK":
            log("ACK", ",".join(parts[1:]))

        elif kind == "ERR":
            log("Arduino ERR", ",".join(parts[1:]))

        elif kind == "SPROUT_READY":
            log("Arduino", line)
            try:
                db.reference("/sprout/trangThai").update({"arduinoReady": True})
            except Exception:
                pass

        else:
            log("RAW", line)

    # ----------------------------------------------------------------
    # ĐẨY DỮ LIỆU LÊN FIREBASE
    # ----------------------------------------------------------------

    def push_firebase(self):
        ts = now()
        try:
            ref = db.reference("/sprout")

            # Ghi đè real-time (web 3D đọc liên tục)
            ref.child("camBien").set({**self.camBien, "capNhatLuc": ts})
            ref.child("thietBi").set({**self.thietBi, "capNhatLuc": ts})
            ref.child("nguong").set({**self.nguong,   "capNhatLuc": ts})
            ref.child("trangThai").update({
                "mode":    self.mode,
                "nuocHet": self.nuocHet,
                "online":  True,
                "capNhatLuc": ts
            })

            # Lịch sử (không ghi quá dày)
            t = time.time()
            if t - self.lastHistory >= HISTORY_INTERVAL:
                self.lastHistory = t
                ref.child("lichSu").push({
                    "thoiGian": ts,
                    **self.camBien,
                    **self.thietBi,
                    "mode": self.mode
                })

        except Exception as e:
            log("Firebase", f"Lỗi ghi: {e}")

    def write_alert(self, content: str):
        ts = now()
        log("CẢNH BÁO", content)
        try:
            db.reference("/sprout/canhBao").push({
                "thoiGian": ts,
                "noiDung":  content,
                "camBien":  self.camBien
            })
            db.reference("/sprout/trangThai").update({
                "canhBaoMoi": content,
                "canhBaoLuc": ts
            })
        except Exception as e:
            log("Firebase", f"Lỗi ghi cảnh báo: {e}")

    # ----------------------------------------------------------------
    # LẮNG NGHE LỆNH TỪ FIREBASE (webapp/web 3D ghi xuống)
    # ----------------------------------------------------------------

    def listen_commands(self):
        """
        Web 3D ghi lệnh vào /sprout/lenh:
          { "loai": "SET_MODE", "giaTri": "MANUAL" }
          { "loai": "CMD_QUAT", "giaTri": "1" }
          { "loai": "SET_TEMP", "min": 20, "max": 32 }
        Python đọc → gửi xuống Arduino → xóa node lệnh.
        """
        def on_cmd(event):
            if event.data is None:
                return
            try:
                cmd  = event.data
                loai = cmd.get("loai", "").upper()
                if not loai:
                    log("CMD", "Lệnh thiếu trường 'loai'")
                    db.reference("/sprout/lenh").delete()
                    return
                log("CMD", f"Nhận lệnh từ Firebase: {cmd}")

                if loai == "SET_MODE":
                    gia_tri = cmd.get("giaTri", "AUTO")
                    self.send("SET_MODE," + gia_tri)

                elif loai == "SET_TEMP":
                    min_val = cmd.get("min", 18)
                    max_val = cmd.get("max", 30)
                    self.send(f"SET_TEMP,{min_val},{max_val}")

                elif loai == "SET_SOIL":
                    min_val = cmd.get("min", 30)
                    max_val = cmd.get("max", 70)
                    self.send(f"SET_SOIL,{min_val},{max_val}")

                elif loai == "SET_HUMI":
                    min_val = cmd.get("min", 50)
                    max_val = cmd.get("max", 85)
                    self.send(f"SET_HUMI,{min_val},{max_val}")

                elif loai == "SET_LIGHT":
                    # min/max o day la LUX, khop truc tiep voi Arduino
                    # (KHONG con quy doi % nua — xem ghi chu dau file v3.4/v3.5)
                    min_val = cmd.get("min", cmd.get("giaTri", 300))
                    max_val = cmd.get("max", 900)
                    self.send(f"SET_LIGHT,{min_val},{max_val}")

                elif loai in ("CMD_QUAT", "CMD_BOM", "CMD_SUOI",
                              "CMD_DEN",  "CMD_HUTAM", "CMD_TANGAM"):
                    gia_tri = cmd.get("giaTri", "0")
                    self.send(f"{loai},{gia_tri}")

                elif loai == "SET_PLANT":
                    ten_cay = str(cmd.get("giaTri", "")).strip()
                    if ten_cay:
                        threading.Thread(
                            target=self.xu_ly_loai_cay, args=(ten_cay,), daemon=True
                        ).start()
                    else:
                        log("CMD", "SET_PLANT thiếu tên cây")
                    db.reference("/sprout/lenh").delete()
                    return

                else:
                    log("CMD", f"Lệnh không nhận dạng được: {loai}")
                    db.reference("/sprout/lenh").delete()
                    return

                # Xóa node lệnh sau khi gửi xong
                db.reference("/sprout/lenh").delete()

            except Exception as e:
                log("Firebase", f"Lỗi xử lý lệnh: {e}")
                try:
                    db.reference("/sprout/lenh").delete()
                except Exception:
                    pass

        db.reference("/sprout/lenh").listen(on_cmd)

    # ----------------------------------------------------------------
    # GỬI LỆNH XUỐNG ARDUINO
    # ----------------------------------------------------------------

    def send(self, cmd: str):
        if self.ser and self.ser.is_open:
            full = cmd.strip() + "\n"
            self.ser.write(full.encode("utf-8"))
            log("->Arduino", cmd)
        else:
            log("Serial", "Chưa kết nối, không gửi được!")

    # ----------------------------------------------------------------
    # AI PHÂN TÍCH (rule-based)
    # ----------------------------------------------------------------

    def maybe_ai(self):
        if time.time() - self.lastAiTime < AI_INTERVAL:
            return
        if self.ai_running:
            log("AI", "Đang có AI thread chạy, bỏ qua lần này")
            return
        self.lastAiTime = time.time()
        self.ai_running = True
        threading.Thread(target=self.run_ai, daemon=True).start()

    def run_ai(self):
        try:
            with self.lock:
                cb = dict(self.camBien)
                ng = dict(self.nguong)
                mode = self.mode

            result = self.analyze(cb, ng)

            # Gửi tin nhắn ngắn lên LCD Arduino
            self.send(f"AI_TUVAN,{result['lcdMsg'][:40]}")

            ts = now()
            try:
                db.reference("/sprout/aiLog").push({
                    "thoiGian":   ts,
                    "tomTat":     result["tomTat"],
                    "khuyenNghi": result["khuyenNghi"],
                    "camBien":    cb,
                    "mode":       mode
                })
                db.reference("/sprout/trangThai").update({
                    "aiMoiNhat":    result["tomTat"],
                    "aiCapNhatLuc": ts
                })
            except Exception as e:
                log("Firebase", f"Lỗi ghi AI log: {e}")

            log("AI", result["tomTat"])
        finally:
            self.ai_running = False

    def analyze(self, cb: dict, ng: dict) -> dict:
        issues = []
        advice = []

        if cb["nhietDo"] > ng["nhietMax"]:
            issues.append(f"Nóng {cb['nhietDo']}°C > {ng['nhietMax']}°C")
            advice.append("Tăng thông gió, kiểm tra quạt")
        elif cb["nhietDo"] < ng["nhietMin"]:
            issues.append(f"Lạnh {cb['nhietDo']}°C < {ng['nhietMin']}°C")
            advice.append("Bật lò sưởi")

        if cb["doAmKK"] > ng["amKKMax"]:
            issues.append(f"Quá ẩm {cb['doAmKK']}% > {ng['amKKMax']}%")
            advice.append("Bật quạt hút ẩm")
        elif cb["doAmKK"] < ng["amKKMin"]:
            issues.append(f"Quá khô {cb['doAmKK']}% < {ng['amKKMin']}%")
            advice.append("Bật quạt tăng ẩm")

        if cb["doAmDat"] < ng["datMin"]:
            issues.append(f"Đất khô {cb['doAmDat']}% < {ng['datMin']}%")
            advice.append("Cần tưới nước ngay")
        elif cb["doAmDat"] > ng["datMax"]:
            issues.append(f"Đất ngập {cb['doAmDat']}% > {ng['datMax']}%")
            advice.append("Giảm tưới, kiểm tra thoát nước")

        if cb["anhSang"] < ng["nguongDen"]:
            issues.append(f"Thiếu sáng {cb['anhSang']} lux < {ng['nguongDen']} lux")
            advice.append("Bật đèn quang hợp bổ sung")
        elif cb["anhSang"] > ng.get("nguongSangMax", 100000):
            issues.append(f"Quá sáng {cb['anhSang']} lux > {ng.get('nguongSangMax')} lux")
            advice.append("Che bớt nắng / di chuyển cây vào bóng râm")

        if cb["mucNuoc"] < 20:
            issues.append(f"Bồn nước sắp cạn: {cb['mucNuoc']}%")
            advice.append("Thêm nước vào bồn ngay")

        if cb["cuaMo"]:
            issues.append("Cửa buồng đang mở")
            advice.append("Đóng cửa để giữ nhiệt/ẩm")

        if not issues:
            tom  = (f"Cây khỏe. T:{cb['nhietDo']}°C "
                    f"H:{cb['doAmKK']}% Đất:{cb['doAmDat']}%")
            lcd  = f"CAY KHOE T:{int(cb['nhietDo'])}C H:{int(cb['doAmKK'])}%"
            kn   = "Tiếp tục theo dõi"
        else:
            tom  = "Vấn đề: " + "; ".join(issues)
            lcd  = issues[0][:40]
            kn   = "; ".join(advice) if advice else "Kiểm tra thủ công"

        return {"tomTat": tom, "khuyenNghi": kn, "lcdMsg": lcd}

    # ----------------------------------------------------------------
    # GEMINI AI — CHỈ GỌI KHI GẶP LOẠI CÂY MỚI (chưa có trong cache)
    # ----------------------------------------------------------------

    def goi_gemini_lay_chi_so(self, ten_cay_hienthi: str):
        """
        Gọi Gemini đúng 1 lần cho 1 loại cây mới, yêu cầu trả về JSON thuần
        chứa ngưỡng nhiệt độ/độ ẩm KK/độ ẩm đất/ánh sáng (0-100%) phù hợp.
        Trả về dict hoặc None nếu lỗi.
        """
        if not GEMINI_API_KEY or "DAN_API_KEY" in GEMINI_API_KEY:
            log("Gemini", "Chưa cấu hình GEMINI_API_KEY — bỏ qua gọi API")
            return None

        prompt = (
            "Bạn là chuyên gia nông nghiệp/thực vật học. Với loại cây "
            f"\"{ten_cay_hienthi}\", hãy đề xuất ngưỡng môi trường lý tưởng "
            "để trồng trong buồng trồng cây thông minh có cảm biến. "
            "LƯU Ý QUAN TRỌNG: đây là hệ thống DEMO mô phỏng trên Proteus, "
            "cảm biến ánh sáng (LDR) trong mạch mô phỏng CHỈ đo được tối đa "
            "khoảng 1000 lux (không phải lux thật ngoài trời), nên ngưỡng "
            "ánh sáng đề xuất PHẢI nằm trong khoảng 0-1000 lux, KHÔNG dùng "
            "thang lux thực tế ngoài trời. "
            "CHỈ trả về một object JSON DUY NHẤT, không thêm chữ nào khác, "
            "đúng các khóa sau (số thực, đơn vị như mô tả):\n"
            "{\n"
            '  "nhietMin": nhiệt độ thấp nhất chấp nhận được (°C),\n'
            '  "nhietMax": nhiệt độ cao nhất chấp nhận được (°C),\n'
            '  "amKKMin": độ ẩm không khí thấp nhất (%),\n'
            '  "amKKMax": độ ẩm không khí cao nhất (%),\n'
            '  "datMin": độ ẩm đất thấp nhất (%),\n'
            '  "datMax": độ ẩm đất cao nhất (%),\n'
            '  "sangMin": ngưỡng CƯỜNG ĐỘ ÁNH SÁNG tối thiểu tính theo LUX '
            '(thang mô phỏng 0-1000 lux, KHÔNG phải lux thật ngoài trời và '
            'KHÔNG phải %) trước khi cần bật đèn quang hợp bổ sung — tham '
            'khảo: cây ưa bóng/trong nhà khoảng 50-150 lux, cây ưa sáng bán '
            'phần khoảng 150-400 lux, cây ưa nắng trực tiếp khoảng 400-700 lux,\n'
            '  "sangMax": ngưỡng CƯỜNG ĐỘ ÁNH SÁNG tối đa tính theo LUX (cùng '
            'thang mô phỏng 0-1000 lux) mà cây còn chịu được trước khi bị '
            'stress/cháy lá do quá nắng (PHẢI lớn hơn sangMin, và PHẢI nhỏ '
            'hơn hoặc bằng 1000) — tham khảo: cây ưa bóng khoảng 300-500 lux, '
            'cây ưa sáng bán phần khoảng 500-800 lux, cây ưa nắng trực tiếp '
            'khoảng 800-1000 lux,\n'
            '  "ghiChu": ghi chú ngắn gọn (dưới 60 ký tự) bằng tiếng Việt\n'
            "}"
        )

        body = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.2
            }
        }

        try:
            resp = requests.post(
                GEMINI_ENDPOINT,
                headers={
                    "Content-Type": "application/json",
                    "x-goog-api-key": GEMINI_API_KEY
                },
                json=body,
                timeout=20
            )
            resp.raise_for_status()
            data = resp.json()
            text = data["candidates"][0]["content"]["parts"][0]["text"]
            ket_qua = json.loads(text)

            # Kiểm tra hợp lệ tối thiểu trước khi tin dùng
            if not ho_so_hop_le(ket_qua):
                log("Gemini", f"Thiếu trường trong kết quả: {ket_qua}")
                return None
            if ket_qua["nhietMin"] >= ket_qua["nhietMax"]:
                return None
            if not (0 < ket_qua["sangMin"] <= 100000):
                log("Gemini", f"sangMin ngoài khoảng lux hợp lệ (0-100000): "
                              f"{ket_qua['sangMin']} — bỏ ngưỡng đèn")
                return None
            if not (0 < ket_qua["sangMax"] <= 100000):
                log("Gemini", f"sangMax ngoài khoảng lux hợp lệ (0-100000): "
                              f"{ket_qua['sangMax']} — bỏ ngưỡng đèn")
                return None
            if ket_qua["sangMin"] >= ket_qua["sangMax"]:
                log("Gemini", f"sangMin ({ket_qua['sangMin']}) >= sangMax "
                              f"({ket_qua['sangMax']}) — bỏ ngưỡng đèn")
                return None

            # Ghim (clamp) về trần thực tế của LDR trong mạch Proteus demo
            # (~1000 lux) phòng khi Gemini lỡ bỏ qua gợi ý trong prompt và
            # trả về giá trị theo lux thật ngoài trời. Không bỏ cả hồ sơ,
            # chỉ hạ giá trị ánh sáng cho khớp phần cứng đang dùng.
            if ket_qua["sangMin"] > PROTEUS_LUX_MAX or ket_qua["sangMax"] > PROTEUS_LUX_MAX:
                log("Gemini", f"sangMin/sangMax ({ket_qua['sangMin']}/"
                              f"{ket_qua['sangMax']}) vượt trần LDR Proteus "
                              f"(~{PROTEUS_LUX_MAX} lux) — ghim lại cho khớp mô phỏng")
                ty_le = ket_qua["sangMin"] / ket_qua["sangMax"] if ket_qua["sangMax"] else 0.5
                ket_qua["sangMax"] = min(ket_qua["sangMax"], PROTEUS_LUX_MAX)
                ket_qua["sangMin"] = min(ket_qua["sangMin"],
                                          max(1, int(ket_qua["sangMax"] * ty_le)))
                if ket_qua["sangMin"] >= ket_qua["sangMax"]:
                    ket_qua["sangMin"] = max(1, ket_qua["sangMax"] - 1)

            ket_qua.setdefault("ghiChu", "")
            return ket_qua

        except (requests.RequestException, KeyError, IndexError,
                json.JSONDecodeError, ValueError) as e:
            log("Gemini", f"Lỗi gọi API: {e}")
            return None

    def xu_ly_loai_cay(self, ten_cay_goc: str):
        """
        Nhận tên cây từ webapp -> tra cache trước -> chỉ gọi Gemini nếu là
        cây MỚI (chưa có trong plant_profiles.json) HOẶC nếu hồ sơ cache
        của cây đó bị lỗi/thiếu field -> áp ngưỡng xuống Arduino -> ghi
        thông tin lên Firebase cho web hiển thị.

        FIX v3.3: trước đây nếu cache có sẵn nhưng thiếu field (ví dụ do
        lưu từ bản cũ hơn) thì code sẽ dùng thẳng và crash KeyError ở
        bước gửi SET_LIGHT, làm chết thread giữa chừng -> SET_TENCAY và
        ghi Firebase không bao giờ chạy. Giờ đã validate cache TRƯỚC khi
        dùng, nếu lỗi thì tự xóa và gọi lại Gemini.
        """
        with self.plant_lock:
            key = chuan_hoa_ten_cay(ten_cay_goc)
            if not key:
                return

            ho_so = self.plant_cache.get(key)

            # --- Validate cache: nếu thiếu field thì coi như cache-miss ---
            if ho_so is not None and not ho_so_hop_le(ho_so):
                log("Cây", f"Cache của '{ten_cay_goc}' bị THIẾU FIELD (hỏng) "
                           f"— xóa cache lỗi và gọi lại Gemini")
                del self.plant_cache[key]
                save_plant_cache(self.plant_cache)
                ho_so = None

            if ho_so is not None:
                nguon = "cache"
                log("Cây", f"'{ten_cay_goc}' đã có trong cache — dùng lại, KHÔNG gọi Gemini")
            else:
                log("Cây", f"'{ten_cay_goc}' là loại cây MỚI (hoặc cache vừa bị xóa do lỗi) "
                           f"— gọi Gemini để lấy chỉ số...")
                ho_so = self.goi_gemini_lay_chi_so(ten_cay_goc)
                if ho_so is None:
                    log("Cây", f"Không lấy được chỉ số cho '{ten_cay_goc}' — giữ nguyên ngưỡng hiện tại")
                    try:
                        db.reference("/sprout/thongTinCay").update({
                            "ten": ten_cay_goc,
                            "loi": "Không lấy được dữ liệu từ Gemini, kiểm tra API key/mạng",
                            "capNhatLuc": now()
                        })
                    except Exception:
                        pass
                    return
                ho_so["tenHienThi"] = ten_cay_goc
                self.plant_cache[key] = ho_so
                save_plant_cache(self.plant_cache)   # ghi file NGAY để lần sau không gọi lại
                nguon = "gemini"

            # --- Từ đây trở đi ho_so CHẮC CHẮN hợp lệ (đã validate ở trên) ---
            # Vẫn bọc try/except để nếu có lỗi bất ngờ khác (serial mất kết
            # nối, Firebase lỗi mạng...) thì báo rõ thay vì làm chết thread
            # một cách âm thầm không dấu vết.
            try:
                self.currentPlant = key

                # --- Áp ngưỡng xuống Arduino (mỗi lệnh cách nhau 1 chút cho chắc) ---
                self.send(f"SET_TEMP,{ho_so['nhietMin']},{ho_so['nhietMax']}")
                time.sleep(0.15)
                self.send(f"SET_HUMI,{ho_so['amKKMin']},{ho_so['amKKMax']}")
                time.sleep(0.15)
                self.send(f"SET_SOIL,{ho_so['datMin']},{ho_so['datMax']}")
                time.sleep(0.15)
                self.send(f"SET_LIGHT,{ho_so['sangMin']},{ho_so['sangMax']}")
                time.sleep(0.15)

                # --- Gửi tên cây (đã bỏ dấu) để LCD vật lý hiển thị đúng cây ---
                ten_lcd = bo_dau_lcd(ho_so.get("tenHienThi", ten_cay_goc))
                self.send(f"SET_TENCAY,{ten_lcd}")

                # --- Ghi nhớ cây đang chọn để lần khởi động sau tự áp lại ---
                save_current_plant(ten_cay_goc)

                # --- Ghi lên Firebase để web hiển thị ---
                try:
                    db.reference("/sprout/thongTinCay").set({
                        "ten": ho_so.get("tenHienThi", ten_cay_goc),
                        "nguon": nguon,
                        "nguong": {
                            "nhietMin": ho_so["nhietMin"], "nhietMax": ho_so["nhietMax"],
                            "amKKMin":  ho_so["amKKMin"],  "amKKMax":  ho_so["amKKMax"],
                            "datMin":   ho_so["datMin"],   "datMax":   ho_so["datMax"],
                            "sangMin":  ho_so["sangMin"],  "sangMax":  ho_so["sangMax"]
                        },
                        "ghiChu": ho_so.get("ghiChu", ""),
                        "capNhatLuc": now()
                    })
                except Exception as e:
                    log("Firebase", f"Lỗi ghi thongTinCay: {e}")

                log("Cây", f"Đã áp ngưỡng cho '{ten_cay_goc}' (nguồn: {nguon})")

            except Exception as e:
                log("Cây", f"LỖI khi áp ngưỡng cho '{ten_cay_goc}': {e}")
                try:
                    db.reference("/sprout/thongTinCay").update({
                        "ten": ten_cay_goc,
                        "loi": f"Lỗi khi áp ngưỡng xuống Arduino: {e}",
                        "capNhatLuc": now()
                    })
                except Exception:
                    pass

    # ----------------------------------------------------------------
    # GHI NHỚ CÂY: khôi phục cây đã chọn ở lần chạy trước
    # ----------------------------------------------------------------

    def khoi_phuc_cay_da_chon(self):
        """Chạy 1 lần khi bridge vừa khởi động: nếu trước đó người dùng đã
        chọn một loại cây, tự động áp lại đúng cây đó (và ngưỡng của nó)
        xuống Arduino + LCD mà KHÔNG cần người dùng nhập lại. Nếu cache
        của cây này bị lỗi, xu_ly_loai_cay() sẽ tự phát hiện và gọi lại
        Gemini (chỉ tốn thêm đúng 1 lần cho lần khởi động này)."""
        saved = load_current_plant()
        if not saved or not saved.get("tenGoc"):
            log("Cây", "Chưa có cây nào được lưu từ trước — dùng ngưỡng mặc định")
            return
        ten_goc = saved["tenGoc"]
        log("Cây", f"Khôi phục cây đã chọn trước đó: '{ten_goc}'")
        self.xu_ly_loai_cay(ten_goc)

    # ----------------------------------------------------------------
    # CHẠY
    # ----------------------------------------------------------------

    def run(self):
        if not self.connect():
            return

        self.running = True

        # Thread đọc serial
        threading.Thread(target=self.read_loop, daemon=True).start()

        # Thread lắng nghe lệnh Firebase
        threading.Thread(target=self.listen_commands, daemon=True).start()

        # Chờ Arduino ổn định rồi mới áp lại cây đã lưu (nếu có), chạy
        # trong thread riêng để không chặn vòng lặp chính
        def cho_roi_khoi_phuc():
            time.sleep(3)
            self.khoi_phuc_cay_da_chon()
        threading.Thread(target=cho_roi_khoi_phuc, daemon=True).start()

        log("SPROUT", "Bridge đang chạy — nhấn Ctrl+C để dừng")
        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            log("INFO", "Dừng bridge...")
        finally:
            self.disconnect()


# ================================================================
# MAIN
# ================================================================

if __name__ == "__main__":
    print("=" * 55)
    print("  S.P.R.O.U.T Bridge v3.5 — Serial (VSPE) <-> Firebase")
    print("=" * 55)

    if not init_firebase():
        sys.exit(1)

    SproutBridge().run()