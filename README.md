# 🌱 S.P.R.O.U.T

**Smart Plant Regulation & Optimization Using Technology**

> Hệ thống buồng nuôi cây thông minh — Đồ án môn IoT
> Nền tảng: Arduino UNO · Proteus · Python · Firebase · AI API

---

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tính năng](#tính-năng)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Phần cứng](#phần-cứng)
- [Giao thức Serial](#giao-thức-serial)
- [Cài đặt &amp; Chạy](#cài-đặt--chạy)
- [Chế độ hoạt động](#chế-độ-hoạt-động)
- [Thuật toán điều khiển AUTO](#thuật-toán-điều-khiển-auto)
- [Tích hợp AI](#tích-hợp-ai)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Kế hoạch mở rộng](#kế-hoạch-mở-rộng)

---

## Giới thiệu

S.P.R.O.U.T là một hệ thống IoT mô phỏng buồng nuôi cây khép kín, tự động duy trì môi trường lý tưởng cho cây trồng dựa trên dữ liệu cảm biến thời gian thực. Hệ thống tích hợp AI để phân tích sức khỏe cây và hỗ trợ người dùng ra quyết định trong cả hai chế độ tự động và thủ công.

Dự án giao tiếp qua **cổng COM ảo** (tạo bằng VSPE), cho phép mô phỏng hoàn toàn trên máy tính với Proteus mà không cần phần cứng thật.

---

## Tính năng

- **Giám sát 5 thông số môi trường**: nhiệt độ, độ ẩm không khí, độ ẩm đất, mực nước bồn, cường độ ánh sáng
- **6 thiết bị điều khiển độc lập**: quạt hút nhiệt, bơm tưới, lò sưởi, đèn quang hợp, quạt hút ẩm, quạt tăng ẩm
- **Chế độ AUTO**: Arduino tự cân bằng môi trường theo ngưỡng cấu hình
- **Chế độ MANUAL**: người dùng điều khiển từng thiết bị qua Python CLI
- **AI phân tích định kỳ**: đánh giá tình trạng cây, hiển thị kết quả lên LCD và ghi log Firebase
- **Ngưỡng điều chỉnh từ xa**: Python gửi lệnh `SET_*` để cập nhật tham số Arduino không cần nạp lại firmware
- **Bảo vệ phần cứng**: khóa bơm tự động khi bồn nước cạn (< 10%)
- **LCD 20×4**: hiển thị trạng thái real-time chống nhấp nháy
- **Cảnh báo Serial**: `ALERT,WATER_EMPTY` và `ALERT,DOOR_OPEN`

---

## Kiến trúc hệ thống

```
┌─────────────────────────────┐
│  Proteus — Arduino UNO      │
│  SPROUT_Arduino.ino         │
│  COM3 (ảo - VSPE)           │
└──────────────┬──────────────┘
               │  Serial 9600 baud
               │  Giao thức CSV text
┌──────────────▼──────────────┐
│  VSPE — COM Pair            │
│  COM3 <──────────> COM4     │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│  Python — sprout_controller │
│  sprout_controller.py       │
│  COM4 (ảo - VSPE)           │
└──────┬───────────────┬──────┘
       │               │
┌──────▼──────┐  ┌─────▼──────┐
│  AI API     │  │  Firebase  │
│  (OpenAI /  │  │  Realtime  │
│   Claude)   │  │  Database  │
└─────────────┘  └─────┬──────┘
                        │
               ┌────────▼───────┐
               │  Web App       │
               │  Android App   │
               └────────────────┘
```

---

## Phần cứng

### Sơ đồ chân Arduino UNO

| Chân       | Linh kiện                 | Vai trò                                 |
| ----------- | -------------------------- | ---------------------------------------- |
| `D2`      | Cảm biến cửa            | `INPUT_PULLUP` — LOW = cửa mở       |
| `D4`      | DHT11                      | Đọc nhiệt độ & độ ẩm không khí |
| `D5`      | Relay → Quạt hút nhiệt | Bật khi`temp > TEMP_MAX`              |
| `D6`      | Relay → Bơm tưới       | Bật khi`soil < SOIL_MIN`              |
| `D7`      | Relay → Lò sưởi        | Bật khi`temp < TEMP_MIN`              |
| `D8`      | Relay → Đèn quang hợp  | Bật khi`light < LIGHT_THR`            |
| `D9`      | Relay → Quạt tăng ẩm   | Bật khi`humi < HUMI_MIN`              |
| `D10`     | Relay → Quạt hút ẩm    | Bật khi`humi > HUMI_MAX`              |
| `A0`      | LDR                        | Đo cường độ ánh sáng              |
| `A1`      | Cảm biến độ ẩm đất  | Đo độ ẩm đất (0–100%)             |
| `A2`      | Cảm biến mực nước     | Đo mức nước bồn (0–100%)           |
| `SDA/SCL` | LCD I2C 20×4 (0x3F)       | Hiển thị trạng thái                  |

### Thư viện Arduino cần cài

```
DHT sensor library     (by Adafruit)
LiquidCrystal I2C      (by Frank de Brabander)
Wire                   (built-in)
```

---

## Giao thức Serial

Baudrate: **9600**. Mọi bản tin kết thúc bằng `\n`.

### Arduino → Python (mỗi 2 giây)

**Bản tin DATA chính:**

```
DATA,<temp>,<humi>,<soil>,<water>,<light>,<door>,
     <fan>,<pump>,<heat>,<light_dev>,<humiFan>,<cool>,
     <TEMP_MIN>,<TEMP_MAX>,<HUMI_MIN>,<HUMI_MAX>,
     <SOIL_MIN>,<SOIL_MAX>,<LIGHT_THR>,<mode>
```

Ví dụ:

```
DATA,28.5,62.0,45,80,30,0,0,0,0,1,0,0,18.0,30.0,50.0,85.0,30,70,40,AUTO
```

**Bản tin cảnh báo:**

```
ALERT,WATER_EMPTY
ALERT,DOOR_OPEN
```

**Phản hồi lệnh:**

```
ACK,SET_MODE,AUTO
ACK,CMD_PUMP
ERR,PUMP_BLOCKED,WATER_EMPTY
ERR,CMD_IN_AUTO_MODE
```

### Python → Arduino (theo yêu cầu)

| Lệnh                      | Mô tả                            | Ví dụ                      |
| -------------------------- | ---------------------------------- | ---------------------------- |
| `SET_MODE,<AUTO\|MANUAL>` | Chuyển chế độ                  | `SET_MODE,AUTO`            |
| `SET_TEMP,<min>,<max>`   | Ngưỡng nhiệt độ (°C)         | `SET_TEMP,18,30`           |
| `SET_SOIL,<min>,<max>`   | Ngưỡng độ ẩm đất (%)        | `SET_SOIL,30,70`           |
| `SET_HUMI,<min>,<max>`   | Ngưỡng độ ẩm KK (%)           | `SET_HUMI,50,85`           |
| `SET_LIGHT,<ngưỡng>`   | Ngưỡng bật đèn (%)            | `SET_LIGHT,40`             |
| `CMD_FAN,<0\|1>`          | **(Manual)** Quạt nhiệt    | `CMD_FAN,1`                |
| `CMD_PUMP,<0\|1>`         | **(Manual)** Bơm tưới     | `CMD_PUMP,1`               |
| `CMD_HEAT,<0\|1>`         | **(Manual)** Lò sưởi      | `CMD_HEAT,0`               |
| `CMD_LIGHT,<0\|1>`        | **(Manual)** Đèn           | `CMD_LIGHT,1`              |
| `CMD_HUMIFAN,<0\|1>`      | **(Manual)** Quạt hút ẩm  | `CMD_HUMIFAN,0`            |
| `CMD_COOL,<0\|1>`         | **(Manual)** Quạt tăng ẩm | `CMD_COOL,1`               |
| `AI_ADVICE,<text>`       | Gửi kết quả AI lên LCD         | `AI_ADVICE,CAY KHOE T:28C` |

> **Lưu ý:** Lệnh `CMD_*` chỉ có hiệu lực khi đang ở chế độ MANUAL. Gửi trong AUTO mode sẽ nhận `ERR,CMD_IN_AUTO_MODE`.

---

## Cài đặt & Chạy

### 1. Tạo COM pair với VSPE

1. Mở **VSPE** → **Device** → **Create**
2. Chọn kiểu **Pair** → đặt `COM3` và `COM4` → **Create**
3. Proteus/Arduino dùng **COM3**, Python dùng **COM4**

### 2. Nạp firmware Arduino (Proteus)

1. Biên dịch `SPROUT_Arduino.ino` trong Arduino IDE
2. Xuất file `.hex`
3. Trong Proteus: click đúp vào Arduino UNO → chọn file `.hex` → OK
4. Chạy simulation — cửa sổ Virtual Terminal hiển thị Serial log

### 3. Chạy Python controller

```bash
# Cài thư viện
pip install pyserial requests

# Chỉnh cổng COM trong file (mặc định COM4)
# SERIAL_PORT = "COM4"   ← dòng 22 trong sprout_controller.py

# Chạy
python sprout_controller.py
```

### 4. Lệnh CLI cơ bản

```
> auto              # Chuyển AUTO mode
> manual            # Chuyển MANUAL mode
> fan 1             # (Manual) Bật quạt nhiệt
> pump 0            # (Manual) Tắt bơm
> heat 1            # (Manual) Bật lò sưởi
> light 1           # (Manual) Bật đèn
> humifan 0         # (Manual) Tắt quạt hút ẩm
> cool 1            # (Manual) Bật quạt tăng ẩm
> thr temp 20 32    # Đặt ngưỡng nhiệt 20–32°C
> thr soil 25 75    # Đặt ngưỡng đất 25–75%
> thr humi 45 80    # Đặt ngưỡng ẩm KK 45–80%
> thr light 35      # Đặt ngưỡng đèn 35%
> status            # Xem trạng thái hiện tại
> ai                # Gọi AI phân tích ngay
> quit              # Thoát
```

---

## Chế độ hoạt động

### AUTO Mode

Arduino tự động điều chỉnh **4 vòng điều khiển độc lập**:

```
Nhiệt độ  ──► temp > TEMP_MAX  → FAN ON  (D5)
              temp < TEMP_MIN  → HEAT ON (D7)

Độ ẩm KK  ──► humi > HUMI_MAX → HUMI_FAN ON (D10)
              humi < HUMI_MIN → COOL ON    (D9)

Độ ẩm đất ──► soil < SOIL_MIN → PUMP ON  (D6)  [có hysteresis]
              soil > SOIL_MAX → PUMP OFF (D6)

Ánh sáng  ──► light < LIGHT_THR → LIGHT ON (D8)
```

Python nhận `DATA` định kỳ, gọi AI phân tích mỗi 30 giây, gửi kết quả `AI_ADVICE` về để hiển thị LCD. **Arduino không tự bật/tắt thiết bị theo lời khuyên AI** — AI chỉ đóng vai trò tư vấn và ghi log.

### MANUAL Mode

Arduino **không chạy `runAutoLogic()`**. Mọi thiết bị chỉ thay đổi khi Python gửi lệnh `CMD_*`. AI vẫn phân tích và hiển thị kết quả lên LCD để hướng dẫn người dùng ra quyết định thủ công.

Bảo vệ bơm (`waterEmpty`) vẫn hoạt động ở cả hai chế độ.

---

## Thuật toán điều khiển AUTO

### Ngưỡng mặc định

| Thông số           | Min | Max | Đơn vị               |
| -------------------- | --- | --- | ----------------------- |
| Nhiệt độ          | 18  | 30  | °C                     |
| Độ ẩm KK          | 50  | 85  | %                       |
| Độ ẩm đất       | 30  | 70  | %                       |
| Ngưỡng đèn       | —  | 40  | % (bật khi < ngưỡng) |
| Ngưỡng nước cạn | —  | 10  | % (khóa bơm)          |

### Vùng an toàn (Dead Band)

Độ ẩm không khí có vùng trung tính **50–85%** — không có thiết bị nào hoạt động. Đây là vùng tối ưu cho hầu hết cây trồng. Độ ẩm đất áp dụng **hysteresis** 30–70% tránh bơm bật/tắt liên tục.

---

## Tích hợp AI

### Mock AI (mặc định)

File `sprout_controller.py` đi kèm một **mock AI** viết bằng Python thuần, không cần API key. Mock này phân tích dữ liệu cảm biến theo luật (rule-based) và trả về JSON tương tự API thật.

### Bật AI thật (OpenAI)

Trong `sprout_controller.py`, chỉnh 2 dòng:

```python
USE_REAL_AI = True
OPENAI_KEY  = "sk-xxxxxxxxxxxxxxxx"
```

Hàm `_real_ai_analyze()` đã được viết sẵn — gửi dữ liệu cảm biến + ngưỡng hiện tại lên GPT-3.5-turbo và nhận về JSON phân tích.

### Định dạng JSON phân tích AI

```json
{
  "summary":           "Cây khỏe mạnh. T:28C H:62% Soil:45%",
  "advice":            "Tiếp tục theo dõi, độ ẩm đất ổn định",
  "lcd_msg":           "CAY KHOE! T:28C H:62%",
  "suggest_threshold": [],
  "auto_adjustments":  [],
  "raw_sensors":       { ... },
  "timestamp":         "2025-06-28T10:30:00"
}
```

### Tích hợp Firebase (tự thực hiện)

Trong code Python có comment `# TODO: luu result vao Firebase`. Cấu trúc Firebase đề xuất:

```
/sprout/
  ├── sensors/latest          ← ghi đè mỗi 2 giây
  ├── sensors/{timestamp}     ← lịch sử cảm biến
  ├── devices/latest          ← trạng thái thiết bị
  ├── ai_log/{timestamp}      ← kết quả phân tích AI
  └── alerts/{timestamp}      ← cảnh báo (nước cạn, cửa mở)
```

---

## Cấu trúc dự án

```
S.P.R.O.U.T/
├── SPROUT_Arduino.ino       # Firmware Arduino (Proteus)
├── sprout_controller.py     # Python controller + Mock AI
├── README.md                # Tài liệu này
│
├── (tự làm)
│   ├── web-app/             # Dashboard web (Firebase)
│   └── android-app/         # App Android (Firebase)
```

---

## Kế hoạch mở rộng

- [ ] Gắn API key AI thật (OpenAI / Claude / Gemini)
- [ ] Viết Firebase integration đầy đủ trong Python
- [ ] Dashboard web real-time (React + Firebase)
- [ ] App Android (Flutter + Firebase)
- [ ] Thêm cảm biến CO₂ và pH đất
- [ ] Lịch tưới nước theo lịch (scheduler)
- [ ] Thông báo push khi có cảnh báo

---

## Thành viên & Môn học

> Đồ án môn: **Internet of Things (IoT)**
> Nền tảng mô phỏng: **Proteus 8** + **VSPE**

---

*S.P.R.O.U.T — Grow smarter, not harder 🌿*
