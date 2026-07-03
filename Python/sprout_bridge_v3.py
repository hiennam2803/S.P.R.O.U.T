"""
S.P.R.O.U.T — Python Bridge v3.1
Nâng cấp từ v3.0: tích hợp Gemini AI để tự động đề xuất ngưỡng theo loại cây

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
      file cache plant_profiles.json). Nếu cây đã có trong cache, hệ thống
      dùng lại kết quả cũ, KHÔNG gọi API -> tiết kiệm token tối đa.
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
            "mucNuoc": 0,   "anhSang": 0.0, "cuaMo": False   # anhSang: LUX
        }
        self.thietBi = {
            "quat": 0, "bom": 0, "suoi": 0,
            "den": 0,  "hutAm": 0, "tangAm": 0
        }
        self.nguong = {
            "nhietMin": 18.0, "nhietMax": 30.0,
            "amKKMin":  50.0, "amKKMax":  85.0,
            "datMin":   30,   "datMax":   70,
            "nguongDen": 300.0     # LUX
        }
        self.mode        = "AUTO"
        self.nuocHet     = False
        self.lastAiTime  = 0
        self.lastHistory = 0

        # --- Cache hồ sơ cây (tránh gọi Gemini lặp lại) ---
        self.plant_cache = load_plant_cache()
        self.currentPlant = None
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
        #      nhietMin,nhietMax,amMin,amMax,datMin,datMax,nguongDen,mode
        if kind == "DATA" and len(parts) >= 21:
            try:
                with self.lock:
                    self.camBien = {
                        "nhietDo": round(float(parts[1]), 1),
                        "doAmKK":  round(float(parts[2]), 1),
                        "doAmDat": int(parts[3]),
                        "mucNuoc": int(parts[4]),
                        "anhSang": round(float(parts[5]), 1),   # LUX
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
                        "nguongDen": round(float(parts[19]), 1)   # LUX
                    }
                    self.mode    = parts[20].strip().upper()
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
                log("CMD", f"Nhận lệnh từ Firebase: {cmd}")

                if loai == "SET_MODE":
                    self.send("SET_MODE," + cmd["giaTri"])

                elif loai == "SET_TEMP":
                    self.send(f"SET_TEMP,{cmd['min']},{cmd['max']}")

                elif loai == "SET_SOIL":
                    self.send(f"SET_SOIL,{cmd['min']},{cmd['max']}")

                elif loai == "SET_HUMI":
                    self.send(f"SET_HUMI,{cmd['min']},{cmd['max']}")

                elif loai == "SET_LIGHT":
                    self.send(f"SET_LIGHT,{cmd['giaTri']}")

                elif loai in ("CMD_QUAT", "CMD_BOM", "CMD_SUOI",
                              "CMD_DEN",  "CMD_HUTAM", "CMD_TANGAM"):
                    self.send(f"{loai},{cmd['giaTri']}")

                elif loai == "SET_PLANT":
                    # Xử lý trong thread riêng vì có thể phải gọi Gemini (mạng chậm)
                    ten_cay = str(cmd.get("giaTri", "")).strip()
                    if ten_cay:
                        threading.Thread(
                            target=self.xu_ly_loai_cay, args=(ten_cay,), daemon=True
                        ).start()
                    # Không gửi lệnh Serial ở đây — xu_ly_loai_cay() tự gửi
                    # sau khi có ngưỡng (từ cache hoặc từ Gemini)
                    db.reference("/sprout/lenh").delete()
                    return

                else:
                    log("CMD", f"Lệnh không nhận dạng được: {loai}")
                    return

                # Xóa node lệnh sau khi gửi xong
                db.reference("/sprout/lenh").delete()

            except Exception as e:
                log("Firebase", f"Lỗi xử lý lệnh: {e}")

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
    # AI PHÂN TÍCH (rule-based, thay bằng Claude API nếu muốn)
    # ----------------------------------------------------------------

    def maybe_ai(self):
        if time.time() - self.lastAiTime < AI_INTERVAL:
            return
        self.lastAiTime = time.time()
        threading.Thread(target=self.run_ai, daemon=True).start()

    def run_ai(self):
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
        chứa ngưỡng nhiệt độ/độ ẩm KK/độ ẩm đất/ánh sáng (lux) phù hợp.
        Trả về dict hoặc None nếu lỗi.
        """
        if not GEMINI_API_KEY or "DAN_API_KEY" in GEMINI_API_KEY:
            log("Gemini", "Chưa cấu hình GEMINI_API_KEY — bỏ qua gọi API")
            return None

        prompt = (
            "Bạn là chuyên gia nông nghiệp/thực vật học. Với loại cây "
            f"\"{ten_cay_hienthi}\", hãy đề xuất ngưỡng môi trường lý tưởng "
            "để trồng trong buồng trồng cây thông minh có cảm biến. "
            "CHỈ trả về một object JSON DUY NHẤT, không thêm chữ nào khác, "
            "đúng các khóa sau (số thực, đơn vị như mô tả):\n"
            "{\n"
            '  "nhietMin": nhiệt độ thấp nhất chấp nhận được (°C),\n'
            '  "nhietMax": nhiệt độ cao nhất chấp nhận được (°C),\n'
            '  "amKKMin": độ ẩm không khí thấp nhất (%),\n'
            '  "amKKMax": độ ẩm không khí cao nhất (%),\n'
            '  "datMin": độ ẩm đất thấp nhất (%),\n'
            '  "datMax": độ ẩm đất cao nhất (%),\n'
            '  "luxMin": ngưỡng ánh sáng tối thiểu (lux) trước khi cần bật đèn quang hợp,\n'
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
            can_co = ["nhietMin", "nhietMax", "amKKMin", "amKKMax",
                      "datMin", "datMax", "luxMin"]
            if not all(k in ket_qua for k in can_co):
                log("Gemini", f"Thiếu trường trong kết quả: {ket_qua}")
                return None
            if ket_qua["nhietMin"] >= ket_qua["nhietMax"]:
                return None

            ket_qua.setdefault("ghiChu", "")
            return ket_qua

        except (requests.RequestException, KeyError, IndexError,
                json.JSONDecodeError, ValueError) as e:
            log("Gemini", f"Lỗi gọi API: {e}")
            return None

    def xu_ly_loai_cay(self, ten_cay_goc: str):
        """
        Nhận tên cây từ webapp -> tra cache trước -> chỉ gọi Gemini nếu là
        cây MỚI (chưa có trong plant_profiles.json) -> áp ngưỡng xuống
        Arduino -> ghi thông tin lên Firebase cho web hiển thị.
        """
        key = chuan_hoa_ten_cay(ten_cay_goc)
        if not key:
            return

        if key in self.plant_cache:
            ho_so = self.plant_cache[key]
            nguon = "cache"
            log("Cây", f"'{ten_cay_goc}' đã có trong cache — dùng lại, KHÔNG gọi Gemini")
        else:
            log("Cây", f"'{ten_cay_goc}' là loại cây MỚI — gọi Gemini 1 lần để lấy chỉ số...")
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

        self.currentPlant = key

        # --- Áp ngưỡng xuống Arduino (mỗi lệnh cách nhau 1 chút cho chắc) ---
        self.send(f"SET_TEMP,{ho_so['nhietMin']},{ho_so['nhietMax']}")
        time.sleep(0.15)
        self.send(f"SET_HUMI,{ho_so['amKKMin']},{ho_so['amKKMax']}")
        time.sleep(0.15)
        self.send(f"SET_SOIL,{ho_so['datMin']},{ho_so['datMax']}")
        time.sleep(0.15)
        self.send(f"SET_LIGHT,{ho_so['luxMin']}")

        # --- Ghi lên Firebase để web hiển thị ---
        try:
            db.reference("/sprout/thongTinCay").set({
                "ten": ho_so.get("tenHienThi", ten_cay_goc),
                "nguon": nguon,          # "cache" hoặc "gemini"
                "nguong": {
                    "nhietMin": ho_so["nhietMin"], "nhietMax": ho_so["nhietMax"],
                    "amKKMin":  ho_so["amKKMin"],  "amKKMax":  ho_so["amKKMax"],
                    "datMin":   ho_so["datMin"],   "datMax":   ho_so["datMax"],
                    "luxMin":   ho_so["luxMin"]
                },
                "ghiChu": ho_so.get("ghiChu", ""),
                "capNhatLuc": now()
            })
        except Exception as e:
            log("Firebase", f"Lỗi ghi thongTinCay: {e}")

        log("Cây", f"Đã áp ngưỡng cho '{ten_cay_goc}' (nguồn: {nguon})")

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
    print("  S.P.R.O.U.T Bridge v3.0 — Serial (VSPE) <-> Firebase")
    print("=" * 55)

    if not init_firebase():
        sys.exit(1)

    SproutBridge().run()