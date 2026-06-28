"""
S.P.R.O.U.T — Python Bridge v2.0
Nhiem vu: doc Serial tu Arduino, day len Firebase Realtime Database
          nhan lenh tu Firebase, gui xuong Arduino

Cau truc Firebase:
  /sprout/
    camBien/        <- cap nhat lien tuc tu Arduino
    thietBi/        <- trang thai relay
    nguong/         <- nguong dieu khien hien tai
    lenh/           <- webapp ghi vao day de dieu khien
    canhBao/        <- lich su canh bao
    aiLog/          <- ket qua phan tich AI
    trangThai/      <- online/offline, mode
"""

import serial
import threading
import time
import json
from datetime import datetime

import firebase_admin
from firebase_admin import credentials, db


# ================================================================
# CAU HINH — CHINH O DAY
# ================================================================

SERIAL_PORT   = "COM4"       # Cong COM pair phia Python (VSPE)
BAUD_RATE     = 9600
FIREBASE_URL  = "https://sprout-3609f-default-rtdb.asia-southeast1.firebasedatabase.app/"
SERVICE_KEY   = "sprout-3609f-firebase-adminsdk-fbsvc-eabd4ae960.json"  # File key tai tu Firebase

AI_INTERVAL   = 30           # Goi AI phan tich moi N giay


# ================================================================
# KHOI TAO FIREBASE
# ================================================================

def khoiTaoFirebase():
    cred = credentials.Certificate(SERVICE_KEY)
    firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_URL})
    print("[Firebase] Ket noi thanh cong")


# ================================================================
# BRIDGE CLASS
# ================================================================

class SproutBridge:
    def __init__(self):
        self.ser      = None
        self.running  = False
        self.lock     = threading.Lock()

        # Du lieu hien tai doc tu Arduino
        self.camBien  = {
            "nhietDo": 0.0, "doAmKK": 0.0, "doAmDat": 0,
            "mucNuoc": 0,   "anhSang": 0,   "cuaMo": False
        }
        self.thietBi  = {
            "quat": 0, "bom": 0, "suoi": 0,
            "den": 0,  "hutAm": 0, "tangAm": 0
        }
        self.nguong   = {
            "nhietMin": 18.0, "nhietMax": 30.0,
            "amKKMin":  50.0, "amKKMax":  85.0,
            "datMin":   30,   "datMax":   70,
            "nguongDen": 40
        }
        self.mode          = "AUTO"
        self.lastAiTime    = 0
        self.nuocHet       = False

    # ----------------------------------------------------------------
    # KET NOI SERIAL
    # ----------------------------------------------------------------

    def ketNoi(self):
        try:
            self.ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
            time.sleep(2)
            print(f"[Serial] Ket noi {SERIAL_PORT} @ {BAUD_RATE}")

            # Bao Firebase online
            db.reference("/sprout/trangThai").update({
                "online": True,
                "capNhatLuc": thoiGianHienTai()
            })
            return True
        except serial.SerialException as e:
            print(f"[Serial] LOI: {e}")
            return False

    def ngatKetNoi(self):
        self.running = False
        db.reference("/sprout/trangThai").update({
            "online": False,
            "capNhatLuc": thoiGianHienTai()
        })
        if self.ser and self.ser.is_open:
            self.ser.close()
        print("[Serial] Da dong ket noi")

    # ----------------------------------------------------------------
    # VONG LAP DOC SERIAL
    # ----------------------------------------------------------------

    def vongLapDocSerial(self):
        while self.running:
            try:
                if self.ser.in_waiting:
                    raw = self.ser.readline().decode("utf-8", errors="ignore").strip()
                    if raw:
                        self.xuLyDuLieuArduino(raw)
            except serial.SerialException:
                print("[Serial] Mat ket noi!")
                self.running = False
            time.sleep(0.05)

    def xuLyDuLieuArduino(self, dong: str):
        phan = dong.split(",")
        if not phan:
            return

        loai = phan[0].upper()

        # --- Goi DATA chinh tu Arduino ---
        # DATA,nhiet,amKK,dat,nuoc,sang,cua,
        #      quat,bom,suoi,den,hutam,tangam,
        #      nhietMin,nhietMax,amMin,amMax,datMin,datMax,nguongDen,mode
        if loai == "DATA" and len(phan) >= 21:
            with self.lock:
                self.camBien = {
                    "nhietDo": float(phan[1]),
                    "doAmKK":  float(phan[2]),
                    "doAmDat": int(phan[3]),
                    "mucNuoc": int(phan[4]),
                    "anhSang": int(phan[5]),
                    "cuaMo":   phan[6] == "1"
                }
                self.thietBi = {
                    "quat":   int(phan[7]),
                    "bom":    int(phan[8]),
                    "suoi":   int(phan[9]),
                    "den":    int(phan[10]),
                    "hutAm":  int(phan[11]),
                    "tangAm": int(phan[12])
                }
                self.nguong = {
                    "nhietMin":  float(phan[13]),
                    "nhietMax":  float(phan[14]),
                    "amKKMin":   float(phan[15]),
                    "amKKMax":   float(phan[16]),
                    "datMin":    int(phan[17]),
                    "datMax":    int(phan[18]),
                    "nguongDen": int(phan[19])
                }
                self.mode    = phan[20].strip().upper()
                self.nuocHet = self.camBien["mucNuoc"] < 10

            self.dayLenFirebase()
            self.kiemTraGoi_AI()

        # --- Canh bao ---
        elif loai == "ALERT":
            noiDung = ",".join(phan[1:])
            self.ghiCanhBao(noiDung)

        # --- Arduino xac nhan lenh ---
        elif loai == "ACK":
            print(f"[ACK] {','.join(phan[1:])}")

        elif loai == "ERR":
            print(f"[Arduino ERR] {','.join(phan[1:])}")

        elif loai == "SPROUT_READY":
            print(f"[Arduino] {dong}")
            db.reference("/sprout/trangThai").update({"arduinoReady": True})

        else:
            print(f"[RAW] {dong}")

    # ----------------------------------------------------------------
    # DAY DU LIEU LEN FIREBASE
    # ----------------------------------------------------------------

    def dayLenFirebase(self):
        ts = thoiGianHienTai()
        try:
            ref = db.reference("/sprout")

            # Cap nhat gia tri hien tai (ghi de, webapp doc lien tuc)
            ref.child("camBien").set({**self.camBien, "capNhatLuc": ts})
            ref.child("thietBi").set({**self.thietBi, "capNhatLuc": ts})
            ref.child("nguong").set({**self.nguong,   "capNhatLuc": ts})
            ref.child("trangThai").update({
                "mode":    self.mode,
                "nuocHet": self.nuocHet,
                "online":  True,
                "capNhatLuc": ts
            })

            # Ghi lich su moi 30 giay (tranh doc Firebase)
            if int(time.time()) % 30 == 0:
                ref.child("lichSu").push({
                    "thoiGian": ts,
                    **self.camBien,
                    **self.thietBi,
                    "mode": self.mode
                })

        except Exception as e:
            print(f"[Firebase] Loi ghi: {e}")

    def ghiCanhBao(self, noiDung: str):
        ts = thoiGianHienTai()
        print(f"[!!!] CANH BAO: {noiDung}")
        try:
            db.reference("/sprout/canhBao").push({
                "thoiGian": ts,
                "noiDung":  noiDung,
                "camBien":  self.camBien
            })
            db.reference("/sprout/trangThai").update({
                "canhBaoMoi":    noiDung,
                "canhBaoLuc":    ts
            })
        except Exception as e:
            print(f"[Firebase] Loi ghi canh bao: {e}")

    # ----------------------------------------------------------------
    # LANG NGHE LENH TU FIREBASE (webapp ghi xuong /sprout/lenh)
    # ----------------------------------------------------------------

    def langNgheLenh(self):
        """
        Webapp ghi lenh vao /sprout/lenh theo dang:
          { "loai": "SET_MODE", "giaTri": "MANUAL", "thoiGian": "..." }
          { "loai": "CMD_QUAT", "giaTri": "1" }
          { "loai": "SET_TEMP", "min": 20, "max": 32 }
        Python doc, gui xuong Arduino, xoa lenh di.
        """
        def xuLyLenh(event):
            if event.data is None:
                return
            try:
                lenh = event.data
                loai = lenh.get("loai", "").upper()

                if loai == "SET_MODE":
                    self.guiArduino(f"SET_MODE,{lenh['giaTri']}")

                elif loai == "SET_TEMP":
                    self.guiArduino(f"SET_TEMP,{lenh['min']},{lenh['max']}")

                elif loai == "SET_SOIL":
                    self.guiArduino(f"SET_SOIL,{lenh['min']},{lenh['max']}")

                elif loai == "SET_HUMI":
                    self.guiArduino(f"SET_HUMI,{lenh['min']},{lenh['max']}")

                elif loai == "SET_LIGHT":
                    self.guiArduino(f"SET_LIGHT,{lenh['giaTri']}")

                elif loai in ("CMD_QUAT","CMD_BOM","CMD_SUOI",
                              "CMD_DEN","CMD_HUTAM","CMD_TANGAM"):
                    self.guiArduino(f"{loai},{lenh['giaTri']}")

                # Xoa lenh sau khi xu ly
                db.reference("/sprout/lenh").delete()

            except Exception as e:
                print(f"[Firebase] Loi xu ly lenh: {e}")

        db.reference("/sprout/lenh").listen(xuLyLenh)

    # ----------------------------------------------------------------
    # GUI LENH XUONG ARDUINO
    # ----------------------------------------------------------------

    def guiArduino(self, lenh: str):
        if self.ser and self.ser.is_open:
            self.ser.write((lenh.strip() + "\n").encode("utf-8"))
            print(f"[-> Arduino] {lenh}")
        else:
            print("[Serial] Chua ket noi, khong gui duoc!")

    # ----------------------------------------------------------------
    # AI PHAN TICH (MOCK — doi API key thi doi ham nay)
    # ----------------------------------------------------------------

    def kiemTraGoi_AI(self):
        if time.time() - self.lastAiTime < AI_INTERVAL:
            return
        self.lastAiTime = time.time()
        threading.Thread(target=self.chayPhanTichAI, daemon=True).start()

    def chayPhanTichAI(self):
        with self.lock:
            cb = dict(self.camBien)
            ng = dict(self.nguong)
            mode = self.mode

        ketQua = self.mockAI(cb, ng)

        # Gui ket qua ngan len LCD Arduino
        self.guiArduino(f"AI_TUVAN,{ketQua['lcdMsg'][:40]}")

        # Luu day du len Firebase
        ts = thoiGianHienTai()
        try:
            db.reference("/sprout/aiLog").push({
                "thoiGian":  ts,
                "tomTat":    ketQua["tomTat"],
                "khuyenNghi":ketQua["khuyenNghi"],
                "camBien":   cb,
                "mode":      mode
            })
            db.reference("/sprout/trangThai").update({
                "aiMoiNhat":    ketQua["tomTat"],
                "aiCapNhatLuc": ts
            })
        except Exception as e:
            print(f"[Firebase] Loi ghi AI log: {e}")

        print(f"[AI] {ketQua['tomTat']}")
        print(f"[AI] Khuyen nghi: {ketQua['khuyenNghi']}")

    def mockAI(self, cb, ng):
        vanDe    = []
        khuyenNghi = []

        if cb["nhietDo"] > ng["nhietMax"]:
            vanDe.append(f"Nhiet do cao {cb['nhietDo']:.1f}C")
            khuyenNghi.append("Tang thong gio, giam nhiet")
        elif cb["nhietDo"] < ng["nhietMin"]:
            vanDe.append(f"Nhiet do thap {cb['nhietDo']:.1f}C")
            khuyenNghi.append("Bat lo suoi")

        if cb["doAmKK"] > ng["amKKMax"]:
            vanDe.append(f"Qua am {cb['doAmKK']:.1f}%")
            khuyenNghi.append("Tang thong gio hut am")
        elif cb["doAmKK"] < ng["amKKMin"]:
            vanDe.append(f"Qua kho {cb['doAmKK']:.1f}%")
            khuyenNghi.append("Phun suong tang am")

        if cb["doAmDat"] < ng["datMin"]:
            vanDe.append(f"Dat kho {cb['doAmDat']}%")
            khuyenNghi.append("Can tuoi nuoc ngay")
        elif cb["doAmDat"] > ng["datMax"]:
            vanDe.append(f"Dat qua am {cb['doAmDat']}%")
            khuyenNghi.append("Giam tuoi, kiem tra thoat nuoc")

        if cb["mucNuoc"] < 20:
            vanDe.append(f"Bon nuoc sap can {cb['mucNuoc']}%")
            khuyenNghi.append("Them nuoc vao bon ngay")

        if not vanDe:
            tomTat = f"Cay khoe. T:{cb['nhietDo']:.1f}C H:{cb['doAmKK']:.1f}% Dat:{cb['doAmDat']}%"
            lcdMsg = f"CAY KHOE T:{int(cb['nhietDo'])}C H:{int(cb['doAmKK'])}%"
        else:
            tomTat = "Van de: " + "; ".join(vanDe)
            lcdMsg = vanDe[0][:40]

        return {
            "tomTat":     tomTat,
            "khuyenNghi": "; ".join(khuyenNghi) if khuyenNghi else "Tiep tuc theo doi",
            "lcdMsg":     lcdMsg
        }

    # ----------------------------------------------------------------
    # CHAY
    # ----------------------------------------------------------------

    def chay(self):
        if not self.ketNoi():
            return

        self.running = True

        # Thread doc serial
        threading.Thread(target=self.vongLapDocSerial, daemon=True).start()

        # Thread lang nghe lenh tu Firebase
        threading.Thread(target=self.langNgheLenh, daemon=True).start()

        print("\n[S.P.R.O.U.T Bridge] Dang chay... Nhan Ctrl+C de dung\n")
        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n[INFO] Dung bridge...")
        finally:
            self.ngatKetNoi()


# ================================================================
# HAM HO TRO
# ================================================================

def thoiGianHienTai():
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


# ================================================================
# MAIN
# ================================================================

if __name__ == "__main__":
    print("=" * 50)
    print("  S.P.R.O.U.T Bridge — Serial <-> Firebase")
    print("=" * 50)

    khoiTaoFirebase()
    bridge = SproutBridge()
    bridge.chay()