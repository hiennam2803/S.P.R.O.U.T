#include <DHT.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <string.h>


// ================================================================
// KHAI BAO CHAN VAT LY
// ================================================================

#define chanDHT        4
#define chanCuaPA      2
#define chanDenBaoCuaPA 3
#define chanAnhSangPA  A0
#define chanDatPA      A1
#define chanNuocPA     A2

#define chanQuatPA     5
#define chanBomPA      6
#define chanSuoiPA     7
#define chanDenPA      8
#define chanTangAmPA   9
#define chanHutAmPA    10

const float LDR_VCC       = 5.0;
const float LDR_R_CODINH  = 10.0;


// ================================================================
// NGUONG DIEU KHIEN — nguoi dung cau hinh, Python chinh qua SET_*
// ================================================================

float nguong_nhietMin  = 18.0;
float nguong_nhietMax  = 30.0;
float nguong_amKKMin   = 50.0;
float nguong_amKKMax   = 85.0;
int   nguong_datMin    = 30;
int   nguong_datMax    = 70;
int   nguong_den       = 300;
int   nguong_sangMax   = 900;
int   nguong_nuocCan   = 10;


// ================================================================
// DU LIEU CAM BIEN HIEN TAI (PA — doc tu Proteus)
// ================================================================

float PA_nhietDo  = 0.0;
float PA_doAmKK   = 0.0;
int   PA_doAmDat  = 0;
int   PA_mucNuoc  = 0;
float PA_anhSang  = 0.0;   // LUX
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
char  tenCayHienTai[17] = "S.P.R.O.U.T"; // Mặc định


// ================================================================
// BIEN NOI BO
// ================================================================

LiquidCrystal_I2C lcd(0x3F, 20, 4);
DHT dht(chanDHT, DHT11);

unsigned long thoiDiemGuiCuoi = 0;
unsigned long thoiDiemLCDCuoi = 0;
const unsigned long CHU_KY_GUI = 2000;
const unsigned long CHU_KY_LCD = 2000;

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
  pinMode(chanDenBaoCuaPA, OUTPUT);
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

  Serial.println(F("SPROUT_READY,S.P.R.O.U.T v2.6 San sang"));
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
  PA_anhSang = docAnhSangLux();

  PA_cuaMo   = (digitalRead(chanCuaPA) == LOW);
  PA_nuocHet = (PA_mucNuoc < nguong_nuocCan);
}


float docAnhSangLux() {
  int   adc  = analogRead(chanAnhSangPA);
  float vOut = adc * (LDR_VCC / 1023.0);
  if (vOut < 0.02) vOut = 0.02;              // tranh chia cho 0 khi qua toi

  float rLdrKOhm = (LDR_VCC - vOut) * LDR_R_CODINH / vOut;
  if (rLdrKOhm < 0.05) rLdrKOhm = 0.05;

  float lux = 12518931.0 * pow(rLdrKOhm * 1000.0, -1.4059);
  if (lux < 0)      lux = 0;
  if (lux > 100000) lux = 100000;
  return lux;
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

  if (PA_doAmDat < nguong_datMin && !PA_nuocHet) {
    AP_bomBat = true;
  } else if (PA_doAmDat >= nguong_datMax || PA_nuocHet) {
    AP_bomBat = false;
  }
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
  digitalWrite(chanDenBaoCuaPA, PA_cuaMo ? HIGH : LOW);
}

void tatHetThietBi() {
  AP_quatBat = AP_bomBat = AP_suoiBat = false;
  AP_denBat  = AP_hutAmBat = AP_tangAmBat = false;
  xuatRaThietBi();
}


// ================================================================
// HIEN THI LCD
// ================================================================

void inDongLCD(int hang, String noiDung) {
  if (noiDung.length() > 20) noiDung = noiDung.substring(0, 20);
  while (noiDung.length() < 20) noiDung += ' ';
  lcd.setCursor(0, hang);
  lcd.print(noiDung);
}

bool kiemTraCayCoVanDe() {
  if (PA_nhietDo > nguong_nhietMax || PA_nhietDo < nguong_nhietMin) return true;
  if (PA_doAmKK  > nguong_amKKMax  || PA_doAmKK  < nguong_amKKMin)  return true;
  if (PA_doAmDat < nguong_datMin   || PA_doAmDat > nguong_datMax)    return true;
  if (PA_anhSang < nguong_den || PA_anhSang > nguong_sangMax) return true;
  if (PA_nuocHet || PA_cuaMo) return true;
  return false;
}

void hienThiLCD_BinhThuong() {
  inDongLCD(0, String(tenCayHienTai));

  String dong1 = "T:" + String((int)PA_nhietDo) + "C H:" +
                 String((int)PA_doAmKK) + "% D:" + String(PA_doAmDat) + "%";
  inDongLCD(1, dong1);

  String dong2 = "Nuoc:" + String(PA_mucNuoc) + "% S:" +
                 String((int)PA_anhSang) + "lux";
  inDongLCD(2, dong2);

  String dong3 = String(heThongAuto ? "AUTO " : "MANU ");
  dong3 += AP_quatBat   ? "QT" : "qt";
  dong3 += AP_bomBat    ? "BM" : "bm";
  dong3 += AP_suoiBat   ? "SU" : "su";
  dong3 += AP_denBat    ? "DN" : "dn";
  dong3 += AP_hutAmBat  ? "HA" : "ha";
  dong3 += AP_tangAmBat ? "TA" : "ta";
  inDongLCD(3, dong3);
}

void hienThiLCD_CanhBao() {
  inDongLCD(0, String(tenCayHienTai) + " !!");

  String dong1 = "";
  if (PA_nhietDo > nguong_nhietMax) {
    dong1 = "NONG:" + String((int)PA_nhietDo) + "C>" + String((int)nguong_nhietMax) + "C";
  } else if (PA_nhietDo < nguong_nhietMin) {
    dong1 = "LANH:" + String((int)PA_nhietDo) + "C<" + String((int)nguong_nhietMin) + "C";
  } else if (PA_doAmKK > nguong_amKKMax) {
    dong1 = "QUA AM:" + String((int)PA_doAmKK) + "%>" + String((int)nguong_amKKMax) + "%";
  } else if (PA_doAmKK < nguong_amKKMin) {
    dong1 = "QUA KHO:" + String((int)PA_doAmKK) + "%<" + String((int)nguong_amKKMin) + "%";
  } else if (PA_doAmDat < nguong_datMin) {
    dong1 = "DAT KHO:" + String(PA_doAmDat) + "%<" + String(nguong_datMin) + "%";
  } else if (PA_doAmDat > nguong_datMax) {
    dong1 = "DAT NGAP:" + String(PA_doAmDat) + "%>" + String(nguong_datMax) + "%";
  } else if (PA_anhSang < nguong_den) {
    dong1 = "TOI:" + String((int)PA_anhSang) + "lux<" + String(nguong_den);
  } else if (PA_anhSang > nguong_sangMax) {
    dong1 = "SANG QUA:" + String((int)PA_anhSang) + "lux>" + String(nguong_sangMax);
  }
  inDongLCD(1, dong1);

  bool dongMotLaNhiet = (PA_nhietDo > nguong_nhietMax || PA_nhietDo < nguong_nhietMin);
  String dong2 = "";
  if (dongMotLaNhiet && PA_doAmKK > nguong_amKKMax) {
    dong2 = "QUA AM:" + String((int)PA_doAmKK) + "%>" + String((int)nguong_amKKMax) + "%";
  } else if (dongMotLaNhiet && PA_doAmKK < nguong_amKKMin) {
    dong2 = "QUA KHO:" + String((int)PA_doAmKK) + "%<" + String((int)nguong_amKKMin) + "%";
  } else if (dongMotLaNhiet && PA_doAmDat < nguong_datMin) {
    dong2 = "DAT KHO:" + String(PA_doAmDat) + "%<" + String(nguong_datMin) + "%";
  } else if (dongMotLaNhiet && PA_doAmDat > nguong_datMax) {
    dong2 = "DAT NGAP:" + String(PA_doAmDat) + "%>" + String(nguong_datMax) + "%";
  } else if (dongMotLaNhiet && PA_anhSang < nguong_den) {
    dong2 = "TOI:" + String((int)PA_anhSang) + "lux<" + String(nguong_den);
  } else if (dongMotLaNhiet && PA_anhSang > nguong_sangMax) {
    dong2 = "SANG QUA:" + String((int)PA_anhSang) + "lux>" + String(nguong_sangMax);
  }
  inDongLCD(2, dong2);

  String dong3;
  if (PA_nuocHet && PA_cuaMo)      dong3 = "BON CAN! CUA MO!";
  else if (PA_nuocHet)             dong3 = ">>> BON NUOC CAN!";
  else if (PA_cuaMo)               dong3 = ">>> CUA BUONG MO!";
  else {
    dong3 = String(heThongAuto ? "AUTO " : "MANU ");
    dong3 += AP_quatBat   ? "QT" : "qt";
    dong3 += AP_bomBat    ? "BM" : "bm";
    dong3 += AP_suoiBat   ? "SU" : "su";
    dong3 += AP_denBat    ? "DN" : "dn";
    dong3 += AP_hutAmBat  ? "HA" : "ha";
    dong3 += AP_tangAmBat ? "TA" : "ta";
  }
  inDongLCD(3, dong3);
}

void capNhatLCD() {
  bool coVanDe = kiemTraCayCoVanDe();

  if (coVanDe != cayCoVanDe_cu) {
    lcd.clear();
    cayCoVanDe_cu = coVanDe;
  }

  if (coVanDe) hienThiLCD_CanhBao();
  else         hienThiLCD_BinhThuong();
}

void hienThiKhoiDong() {
  lcd.clear();
  lcd.setCursor(3, 0); lcd.print(F("S.P.R.O.U.T v2.5"));
  lcd.setCursor(0, 1); lcd.print(F("Smart Plant Tracker"));
  lcd.setCursor(2, 2); lcd.print(F("IoT Project 2025"));
  lcd.setCursor(4, 3); lcd.print(F("Dang khoi dong..."));
}


// ================================================================
// GUI DU LIEU LEN PYTHON (AP)
// ================================================================

void guiDuLieuLenPython() {
  if (PA_nuocHet)                    Serial.println(F("ALERT,NUOC_CAN"));
  if (PA_cuaMo)                      Serial.println(F("ALERT,CUA_MO"));
  if (PA_anhSang < nguong_den)       Serial.println(F("ALERT,ANH_SANG_YEU"));
  if (PA_anhSang > nguong_sangMax)   Serial.println(F("ALERT,ANH_SANG_MANH"));

  Serial.print(F("DATA,"));
  Serial.print(PA_nhietDo, 1);       Serial.print(F(","));
  Serial.print(PA_doAmKK,  1);       Serial.print(F(","));
  Serial.print(PA_doAmDat);          Serial.print(F(","));
  Serial.print(PA_mucNuoc);          Serial.print(F(","));
  Serial.print(PA_anhSang, 1);       Serial.print(F(","));
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
  Serial.print(nguong_sangMax);      Serial.print(F(","));
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
    int vMin = (int)layThamSoFloat(thamSo, 0);
    int vMax = (int)layThamSoFloat(thamSo, 1);
    bool minHopLe = (vMin >= 0 && vMin <= 100000);
    bool maxHopLe = (vMax >= 0 && vMax <= 100000 && vMax > vMin);
    if (minHopLe) nguong_den     = vMin;
    if (maxHopLe) nguong_sangMax = vMax;
    if (minHopLe && maxHopLe) {
      Serial.print(F("ACK,SET_LIGHT,")); Serial.print(nguong_den);
      Serial.print(F(","));              Serial.println(nguong_sangMax);
    } else {
      Serial.println(F("ERR,SET_LIGHT,GIA_TRI_PHAI_TRONG_0_100000_VA_MAX_LON_HON_MIN"));
    }
    return;
  }

  if (lenh == "AI_TUVAN") {
    thamSo.toCharArray(tuVanAI, sizeof(tuVanAI));
    Serial.println(F("ACK,AI_TUVAN"));
    return;
  }

  if (lenh == "SET_TENCAY") {
    thamSo.toCharArray(tenCayHienTai, sizeof(tenCayHienTai));
    tenCayHienTai[sizeof(tenCayHienTai)-1] = '\0';
    Serial.println(F("ACK,SET_TENCAY"));
    lcd.clear(); // Cập nhật ngay
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