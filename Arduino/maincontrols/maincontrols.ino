/*
  ================================================================
  DU AN: S.P.R.O.U.T v2.1
  Smart Plant Responsive & Optimized Unified Tracker
  ================================================================
  QUY UOC TEN BIEN:
    PA_     = Proteus -> Arduino  : gia tri cam bien doc tu mach Proteus
    AP_     = Arduino -> Python   : trang thai thiet bi gui len Python
    nguong_ = nguong nguoi dung cau hinh, Python co the chinh qua SET_*

  GIAO THUC SERIAL (9600 baud):
    Arduino -> Python (2s/lan):
      DATA,nhieDo,doAmKK,doAmDat,mucNuoc,anhSang,
           cua,quat,bom,suoi,den,hutAm,tangAm,
           nhietMin,nhietMax,amMin,amMax,datMin,datMax,nguongDen,mode

    Python -> Arduino:
      SET_MODE,AUTO|MANUAL
      SET_TEMP,min,max
      SET_SOIL,min,max
      SET_HUMI,min,max
      SET_LIGHT,nguong
      CMD_QUAT,0|1      (chi MANUAL)
      CMD_BOM,0|1       (chi MANUAL)
      CMD_SUOI,0|1      (chi MANUAL)
      CMD_DEN,0|1       (chi MANUAL)
      CMD_HUTAM,0|1     (chi MANUAL)
      CMD_TANGAM,0|1    (chi MANUAL)
      AI_TUVAN,<noi_dung>

  SO DO CHAN:
    D2  = chanCuaPA      (cam bien cua - INPUT_PULLUP)
    D4  = chanDHT        (DHT11)
    D5  = chanQuatPA     (relay quat hut nhiet)
    D6  = chanBomPA      (relay bom tuoi)
    D7  = chanSuoiPA     (relay lo suoi)
    D8  = chanDenPA      (relay den quang hop)
    D9  = chanTangAmPA   (relay quat tang am)
    D10 = chanHutAmPA    (relay quat hut am)
    A0  = chanAnhSangPA  (LDR)
    A1  = chanDatPA      (cam bien do am dat)
    A2  = chanNuocPA     (cam bien muc nuoc)
  ================================================================
*/

#include <DHT.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>


// ================================================================
// KHAI BAO CHAN VAT LY
// ================================================================

#define chanDHT        4
#define chanCuaPA      2
#define chanAnhSangPA  A0
#define chanDatPA      A1
#define chanNuocPA     A2

#define chanQuatPA     5
#define chanBomPA      6
#define chanSuoiPA     7
#define chanDenPA      8
#define chanTangAmPA   9
#define chanHutAmPA    10


// ================================================================
// NGUONG DIEU KHIEN — nguoi dung cau hinh, Python chinh qua SET_*
// ================================================================

float nguong_nhietMin  = 18.0;
float nguong_nhietMax  = 30.0;
float nguong_amKKMin   = 50.0;
float nguong_amKKMax   = 85.0;
int   nguong_datMin    = 30;
int   nguong_datMax    = 70;
int   nguong_den       = 40;   // bat den khi anh sang < nguong nay (%)
int   nguong_nuocCan   = 10;   // khoa bom khi muc nuoc < nguong nay (%)


// ================================================================
// DU LIEU CAM BIEN HIEN TAI (PA — doc tu Proteus)
// ================================================================

float PA_nhietDo  = 0.0;
float PA_doAmKK   = 0.0;
int   PA_doAmDat  = 0;
int   PA_mucNuoc  = 0;
int   PA_anhSang  = 0;
bool  PA_cuaMo    = false;
bool  PA_nuocHet  = false;


// ================================================================
// TRANG THAI THIET BI (AP — gui len Python, ghi ra chan)
// ================================================================

bool AP_quatBat    = false;
bool AP_bomBat     = false;
bool AP_suoiBat    = false;
bool AP_denBat     = false;
bool AP_hutAmBat   = false;
bool AP_tangAmBat  = false;


// ================================================================
// TRANG THAI HE THONG
// ================================================================

bool  heThongAuto  = true;
char  tuVanAI[42]  = "";


// ================================================================
// BIEN NOI BO
// ================================================================

LiquidCrystal_I2C lcd(0x3F, 20, 4);
DHT dht(chanDHT, DHT11);

unsigned long thoiDiemGuiCuoi = 0;
unsigned long thoiDiemLCDCuoi = 0;
const unsigned long CHU_KY_GUI = 2000;
const unsigned long CHU_KY_LCD = 2000;

// Luu gia tri cu de chong nhap nhay LCD
int   cu_nhiet = -99, cu_am = -99, cu_dat = -99, cu_nuoc = -99;
bool  cu_mode  = !heThongAuto;
bool  cayCoVanDe_cu = false;


// ================================================================
// SETUP
// ================================================================

void setup() {
  Serial.begin(9600);
  dht.begin();
  lcd.init();
  lcd.backlight();

  pinMode(chanCuaPA,    INPUT_PULLUP);
  pinMode(chanQuatPA,   OUTPUT);
  pinMode(chanBomPA,    OUTPUT);
  pinMode(chanSuoiPA,   OUTPUT);
  pinMode(chanDenPA,    OUTPUT);
  pinMode(chanTangAmPA, OUTPUT);
  pinMode(chanHutAmPA,  OUTPUT);

  tatHetThietBi();

  hienThiKhoiDong();
  delay(2000);
  lcd.clear();

  Serial.println(F("SPROUT_READY,S.P.R.O.U.T v2.1 San sang"));
}


// ================================================================
// VONG LAP CHINH
// ================================================================

void loop() {
  docCamBien();

  if (Serial.available()) nhanLenhTuPython();

  if (heThongAuto) chayLogicTuDong();

  // Bao ve bom: nuoc het thi khoa bom du o mode nao
  if (PA_nuocHet) AP_bomBat = false;

  xuatRaThietBi();

  if (millis() - thoiDiemLCDCuoi > CHU_KY_LCD) {
    capNhatLCD();
    thoiDiemLCDCuoi = millis();
  }

  if (millis() - thoiDiemGuiCuoi > CHU_KY_GUI) {
    guiDuLieuLenPython();
    thoiDiemGuiCuoi = millis();
  }
}


// ================================================================
// DOC CAM BIEN
// ================================================================

void docCamBien() {
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  if (!isnan(t)) PA_nhietDo = t;
  if (!isnan(h)) PA_doAmKK  = h;

  PA_doAmDat = map(analogRead(chanDatPA),     0, 1023, 0, 100);
  PA_mucNuoc = map(analogRead(chanNuocPA),    0, 1023, 0, 100);
  PA_anhSang = map(analogRead(chanAnhSangPA), 0, 1023, 0, 100);

  PA_cuaMo   = (digitalRead(chanCuaPA) == LOW);
  PA_nuocHet = (PA_mucNuoc < nguong_nuocCan);
}


// ================================================================
// LOGIC TU DONG (chi chay khi heThongAuto == true)
// ================================================================

void chayLogicTuDong() {

  // Nhiet do: quat va suoi hoat dong nguoc nhau
  if      (PA_nhietDo > nguong_nhietMax) { AP_quatBat  = true;  AP_suoiBat = false; }
  else if (PA_nhietDo < nguong_nhietMin) { AP_suoiBat  = true;  AP_quatBat = false; }
  else                                   { AP_quatBat  = false; AP_suoiBat = false; }

  // Do am khong khi: hai quat doc lap
  AP_hutAmBat  = (PA_doAmKK > nguong_amKKMax);
  AP_tangAmBat = (PA_doAmKK < nguong_amKKMin);

  // Do am dat: hysteresis tranh bom bat/tat lien tuc
  if      (PA_doAmDat < nguong_datMin && !PA_nuocHet) AP_bomBat = true;
  else if (PA_doAmDat > nguong_datMax)                AP_bomBat = false;

  // Anh sang
  AP_denBat = (PA_anhSang < nguong_den);
}


// ================================================================
// XUAT RA THIET BI
// ================================================================

void xuatRaThietBi() {
  digitalWrite(chanQuatPA,   AP_quatBat   ? HIGH : LOW);
  digitalWrite(chanBomPA,    AP_bomBat    ? HIGH : LOW);
  digitalWrite(chanSuoiPA,   AP_suoiBat   ? HIGH : LOW);
  digitalWrite(chanDenPA,    AP_denBat    ? HIGH : LOW);
  digitalWrite(chanHutAmPA,  AP_hutAmBat  ? HIGH : LOW);
  digitalWrite(chanTangAmPA, AP_tangAmBat ? HIGH : LOW);
}

void tatHetThietBi() {
  AP_quatBat = AP_bomBat = AP_suoiBat = false;
  AP_denBat  = AP_hutAmBat = AP_tangAmBat = false;
  xuatRaThietBi();
}


// ================================================================
// HIEN THI LCD
// ================================================================

bool kiemTraCayCoVanDe() {
  if (PA_nhietDo > nguong_nhietMax || PA_nhietDo < nguong_nhietMin) return true;
  if (PA_doAmKK  > nguong_amKKMax  || PA_doAmKK  < nguong_amKKMin)  return true;
  if (PA_doAmDat < nguong_datMin   || PA_doAmDat > nguong_datMax)    return true;
  if (PA_nuocHet || PA_cuaMo) return true;
  return false;
}

void hienThiLCD_BinhThuong() {
  lcd.setCursor(0, 0);
  lcd.print(F("=== S.P.R.O.U.T ==="));

  if ((int)PA_nhietDo != cu_nhiet || (int)PA_doAmKK != cu_am) {
    lcd.setCursor(0, 1);
    lcd.print(F("T:"));
    lcd.print((int)PA_nhietDo);
    lcd.print(F("C  H:"));
    lcd.print((int)PA_doAmKK);
    lcd.print(F("%       "));
    cu_nhiet = (int)PA_nhietDo;
    cu_am    = (int)PA_doAmKK;
  }

  if (PA_doAmDat != cu_dat || PA_mucNuoc != cu_nuoc) {
    lcd.setCursor(0, 2);
    lcd.print(F("Dat:"));
    lcd.print(PA_doAmDat);
    lcd.print(F("%  Nuoc:"));
    lcd.print(PA_mucNuoc);
    lcd.print(F("%  "));
    cu_dat  = PA_doAmDat;
    cu_nuoc = PA_mucNuoc;
  }

  if (heThongAuto != cu_mode) {
    lcd.setCursor(0, 3);
    lcd.print(heThongAuto ? F("AUTO ") : F("MANU "));
    lcd.print(AP_quatBat   ? F("QT") : F("qt"));
    lcd.print(AP_bomBat    ? F("BM") : F("bm"));
    lcd.print(AP_suoiBat   ? F("SU") : F("su"));
    lcd.print(AP_denBat    ? F("DN") : F("dn"));
    lcd.print(AP_hutAmBat  ? F("HA") : F("ha"));
    lcd.print(AP_tangAmBat ? F("TA") : F("ta"));
    lcd.print(PA_cuaMo     ? F(" CUA:MO") : F(" CUA:DG"));
    cu_mode = heThongAuto;
  }
}

void hienThiLCD_CanhBao() {
  lcd.setCursor(0, 0);
  lcd.print(F("!! CANH BAO CAY !!  "));

  // Xoa sach dong 1 va 2
  lcd.setCursor(0, 1); lcd.print(F("                    "));
  lcd.setCursor(0, 2); lcd.print(F("                    "));

  // Ghi canh bao thu nhat vao dong 1
  lcd.setCursor(0, 1);
  if (PA_nhietDo > nguong_nhietMax) {
    lcd.print(F("NONG:"));
    lcd.print((int)PA_nhietDo);
    lcd.print(F("C>"));
    lcd.print((int)nguong_nhietMax);
    lcd.print(F("C     "));
  } else if (PA_nhietDo < nguong_nhietMin) {
    lcd.print(F("LANH:"));
    lcd.print((int)PA_nhietDo);
    lcd.print(F("C<"));
    lcd.print((int)nguong_nhietMin);
    lcd.print(F("C     "));
  } else if (PA_doAmKK > nguong_amKKMax) {
    lcd.print(F("QUA AM:"));
    lcd.print((int)PA_doAmKK);
    lcd.print(F("%>"));
    lcd.print((int)nguong_amKKMax);
    lcd.print(F("%  "));
  } else if (PA_doAmKK < nguong_amKKMin) {
    lcd.print(F("QUA KHO:"));
    lcd.print((int)PA_doAmKK);
    lcd.print(F("%<"));
    lcd.print((int)nguong_amKKMin);
    lcd.print(F("%  "));
  } else if (PA_doAmDat < nguong_datMin) {
    lcd.print(F("DAT KHO:"));
    lcd.print(PA_doAmDat);
    lcd.print(F("%<"));
    lcd.print(nguong_datMin);
    lcd.print(F("%   "));
  } else if (PA_doAmDat > nguong_datMax) {
    lcd.print(F("DAT NGAP:"));
    lcd.print(PA_doAmDat);
    lcd.print(F("%>"));
    lcd.print(nguong_datMax);
    lcd.print(F("%  "));
  }

  // Ghi canh bao thu hai vao dong 2 (neu co them van de khac)
  lcd.setCursor(0, 2);
  bool dongMotLaNhiet = (PA_nhietDo > nguong_nhietMax || PA_nhietDo < nguong_nhietMin);
  if (dongMotLaNhiet && PA_doAmKK > nguong_amKKMax) {
    lcd.print(F("QUA AM:"));
    lcd.print((int)PA_doAmKK);
    lcd.print(F("%>"));
    lcd.print((int)nguong_amKKMax);
    lcd.print(F("%  "));
  } else if (dongMotLaNhiet && PA_doAmKK < nguong_amKKMin) {
    lcd.print(F("QUA KHO:"));
    lcd.print((int)PA_doAmKK);
    lcd.print(F("%<"));
    lcd.print((int)nguong_amKKMin);
    lcd.print(F("%  "));
  } else if (dongMotLaNhiet && PA_doAmDat < nguong_datMin) {
    lcd.print(F("DAT KHO:"));
    lcd.print(PA_doAmDat);
    lcd.print(F("%<"));
    lcd.print(nguong_datMin);
    lcd.print(F("%   "));
  } else if (dongMotLaNhiet && PA_doAmDat > nguong_datMax) {
    lcd.print(F("DAT NGAP:"));
    lcd.print(PA_doAmDat);
    lcd.print(F("%>"));
    lcd.print(nguong_datMax);
    lcd.print(F("%  "));
  }

  // Dong 3: nuoc/cua uu tien cao nhat, con lai hien thiet bi
  lcd.setCursor(0, 3);
  if      (PA_nuocHet && PA_cuaMo) lcd.print(F("BON CAN! CUA MO!    "));
  else if (PA_nuocHet)             lcd.print(F(">>> BON NUOC CAN!   "));
  else if (PA_cuaMo)               lcd.print(F(">>> CUA BUONG MO!   "));
  else {
    lcd.print(heThongAuto  ? F("AUTO ") : F("MANU "));
    lcd.print(AP_quatBat   ? F("QT") : F("qt"));
    lcd.print(AP_bomBat    ? F("BM") : F("bm"));
    lcd.print(AP_suoiBat   ? F("SU") : F("su"));
    lcd.print(AP_denBat    ? F("DN") : F("dn"));
    lcd.print(AP_hutAmBat  ? F("HA") : F("ha"));
    lcd.print(AP_tangAmBat ? F("TA") : F("ta"));
    lcd.print(F("        "));
  }
}

void capNhatLCD() {
  bool coVanDe = kiemTraCayCoVanDe();

  // Chuyen kieu hien thi thi xoa man hinh truoc
  if (coVanDe != cayCoVanDe_cu) {
    lcd.clear();
    cayCoVanDe_cu = coVanDe;
  }

  if (coVanDe) hienThiLCD_CanhBao();
  else         hienThiLCD_BinhThuong();
}

void hienThiKhoiDong() {
  lcd.clear();
  lcd.setCursor(3, 0); lcd.print(F("S.P.R.O.U.T v2.1"));
  lcd.setCursor(0, 1); lcd.print(F("Smart Plant Tracker"));
  lcd.setCursor(2, 2); lcd.print(F("IoT Project 2025"));
  lcd.setCursor(4, 3); lcd.print(F("Dang khoi dong..."));
}


// ================================================================
// GUI DU LIEU LEN PYTHON (AP)
// ================================================================

void guiDuLieuLenPython() {
  if (PA_nuocHet) Serial.println(F("ALERT,NUOC_CAN"));
  if (PA_cuaMo)   Serial.println(F("ALERT,CUA_MO"));

  Serial.print(F("DATA,"));
  Serial.print(PA_nhietDo, 1);       Serial.print(F(","));
  Serial.print(PA_doAmKK,  1);       Serial.print(F(","));
  Serial.print(PA_doAmDat);          Serial.print(F(","));
  Serial.print(PA_mucNuoc);          Serial.print(F(","));
  Serial.print(PA_anhSang);          Serial.print(F(","));
  Serial.print(PA_cuaMo      ? 1:0); Serial.print(F(","));
  Serial.print(AP_quatBat    ? 1:0); Serial.print(F(","));
  Serial.print(AP_bomBat     ? 1:0); Serial.print(F(","));
  Serial.print(AP_suoiBat    ? 1:0); Serial.print(F(","));
  Serial.print(AP_denBat     ? 1:0); Serial.print(F(","));
  Serial.print(AP_hutAmBat   ? 1:0); Serial.print(F(","));
  Serial.print(AP_tangAmBat  ? 1:0); Serial.print(F(","));
  Serial.print(nguong_nhietMin, 1);  Serial.print(F(","));
  Serial.print(nguong_nhietMax, 1);  Serial.print(F(","));
  Serial.print(nguong_amKKMin,  1);  Serial.print(F(","));
  Serial.print(nguong_amKKMax,  1);  Serial.print(F(","));
  Serial.print(nguong_datMin);       Serial.print(F(","));
  Serial.print(nguong_datMax);       Serial.print(F(","));
  Serial.print(nguong_den);          Serial.print(F(","));
  Serial.println(heThongAuto ? F("AUTO") : F("MANUAL"));
}


// ================================================================
// NHAN LENH TU PYTHON
// ================================================================

void nhanLenhTuPython() {
  String dong = Serial.readStringUntil('\n');
  dong.trim();
  if (dong.length() == 0) return;

  int viTriPhay = dong.indexOf(',');
  String lenh   = (viTriPhay == -1) ? dong : dong.substring(0, viTriPhay);
  String thamSo = (viTriPhay == -1) ? ""   : dong.substring(viTriPhay + 1);
  lenh.toUpperCase();

  if (lenh == "SET_MODE") {
    thamSo.toUpperCase();
    heThongAuto = (thamSo == "AUTO");
    if (!heThongAuto) tatHetThietBi();
    Serial.print(F("ACK,SET_MODE,"));
    Serial.println(heThongAuto ? F("AUTO") : F("MANUAL"));
    lcd.clear();
    return;
  }

  if (lenh == "SET_TEMP") {
    float vMin = layThamSoFloat(thamSo, 0);
    float vMax = layThamSoFloat(thamSo, 1);
    if (vMin < vMax) { nguong_nhietMin = vMin; nguong_nhietMax = vMax; }
    Serial.print(F("ACK,SET_TEMP,")); Serial.print(nguong_nhietMin);
    Serial.print(F(","));             Serial.println(nguong_nhietMax);
    return;
  }

  if (lenh == "SET_SOIL") {
    int vMin = (int)layThamSoFloat(thamSo, 0);
    int vMax = (int)layThamSoFloat(thamSo, 1);
    if (vMin < vMax) { nguong_datMin = vMin; nguong_datMax = vMax; }
    Serial.print(F("ACK,SET_SOIL,")); Serial.print(nguong_datMin);
    Serial.print(F(","));             Serial.println(nguong_datMax);
    return;
  }

  if (lenh == "SET_HUMI") {
    float vMin = layThamSoFloat(thamSo, 0);
    float vMax = layThamSoFloat(thamSo, 1);
    if (vMin < vMax) { nguong_amKKMin = vMin; nguong_amKKMax = vMax; }
    Serial.print(F("ACK,SET_HUMI,")); Serial.print(nguong_amKKMin);
    Serial.print(F(","));             Serial.println(nguong_amKKMax);
    return;
  }

  if (lenh == "SET_LIGHT") {
    int v = (int)layThamSoFloat(thamSo, 0);
    if (v > 0 && v < 100) nguong_den = v;
    Serial.print(F("ACK,SET_LIGHT,")); Serial.println(nguong_den);
    return;
  }

  if (lenh == "AI_TUVAN") {
    thamSo.toCharArray(tuVanAI, sizeof(tuVanAI));
    Serial.println(F("ACK,AI_TUVAN"));
    return;
  }

  // Lenh dieu khien thu cong — chi chap nhan khi MANUAL
  if (!heThongAuto) {
    bool trangThai = (thamSo == "1");

    if      (lenh == "CMD_QUAT")   AP_quatBat   = trangThai;
    else if (lenh == "CMD_SUOI")   AP_suoiBat   = trangThai;
    else if (lenh == "CMD_DEN")    AP_denBat    = trangThai;
    else if (lenh == "CMD_HUTAM")  AP_hutAmBat  = trangThai;
    else if (lenh == "CMD_TANGAM") AP_tangAmBat = trangThai;
    else if (lenh == "CMD_BOM") {
      if (trangThai && PA_nuocHet) {
        Serial.println(F("ERR,BOM_BI_KHOA,NUOC_HET"));
        return;
      }
      AP_bomBat = trangThai;
    }
    else { Serial.println(F("ERR,LENH_LA")); return; }

    Serial.print(F("ACK,")); Serial.println(lenh);
    return;
  }

  if (lenh.startsWith("CMD_")) Serial.println(F("ERR,DANG_AUTO_KHONG_NHAN_CMD"));
  else                         Serial.println(F("ERR,LENH_LA"));
}


// ================================================================
// HAM HO TRO
// ================================================================

// Lay gia tri float thu viTri trong chuoi "val0,val1,val2"
float layThamSoFloat(String chuoi, int viTri) {
  int batDau = 0;
  for (int i = 0; i < viTri; i++) {
    int pos = chuoi.indexOf(',', batDau);
    if (pos == -1) return 0.0;
    batDau = pos + 1;
  }
  int ketThuc = chuoi.indexOf(',', batDau);
  String phan = (ketThuc == -1) ? chuoi.substring(batDau) : chuoi.substring(batDau, ketThuc);
  return phan.toFloat();
}