"""
S.P.R.O.U.T — Python Bridge v3.0
Nâng cấp từ v2.0: schema Firebase đồng bộ với Web 3D real-time

Pipeline: Proteus → VSPE (COM ảo) → Serial → Firebase RTDB → Web 3D

Cấu trúc Firebase (web 3D đọc từ đây):
  /sprout/
    camBien/        <- cảm biến real-time từ Arduino
    thietBi/        <- trạng thái relay
    nguong/         <- ngưỡng điều khiển hiện tại
    lenh/           <- webapp ghi vào đây để điều khiển
    canhBao/        <- lịch sử cảnh báo (push)
    aiLog/          <- kết quả phân tích AI (push)
    trangThai/      <- online/offline, mode, aiMoiNhat, canhBaoMoi
"""

import serial
import serial.tools.list_ports
import threading
import time
import json
import sys
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

AI_INTERVAL  = 30            # Gọi AI phân tích mỗi N giây
HISTORY_INTERVAL = 30        # Ghi lịch sử mỗi N giây


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
            "mucNuoc": 0,   "anhSang": 0,   "cuaMo": False
        }
        self.thietBi = {
            "quat": 0, "bom": 0, "suoi": 0,
            "den": 0,  "hutAm": 0, "tangAm": 0
        }
        self.nguong = {
            "nhietMin": 18.0, "nhietMax": 30.0,
            "amKKMin":  50.0, "amKKMax":  85.0,
            "datMin":   30,   "datMax":   70,
            "nguongDen": 40
        }
        self.mode        = "AUTO"
        self.nuocHet     = False
        self.lastAiTime  = 0
        self.lastHistory = 0

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
                        "anhSang": int(parts[5]),
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
                        "nguongDen": int(parts[19])
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
