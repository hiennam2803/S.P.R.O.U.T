# 🌱 S.P.R.O.U.T

Smart Plant Regulation & Optimization Using Technology

> Dự án IoT mô phỏng buồng trồng cây thông minh, tích hợp Arduino, Proteus, Python, Firebase, Android và AI để theo dõi và điều khiển môi trường trồng cây một cách tự động.

---

## 1. Giới thiệu

S.P.R.O.U.T là một hệ thống IoT mô phỏng buồng trồng cây khép kín, giúp giám sát và điều chỉnh môi trường sống cho cây trồng bằng các cảm biến và thiết bị điều khiển. Dự án được xây dựng như một sản phẩm học tập/demo cho môn IoT, nhưng có cấu trúc đủ rõ ràng để mở rộng thành một hệ thống thực tế.

Hệ thống gồm 4 tầng chính:

1. Tầng cảm biến và điều khiển phần cứng: Arduino + Proteus
2. Tầng trung gian: bridge Python nhận dữ liệu từ Arduino, xử lý và đồng bộ lên Firebase
3. Tầng ứng dụng: Android app viết bằng Kotlin + Jetpack Compose
4. Tầng thông minh: AI phân tích tình trạng cây với Gemini để đưa ra gợi ý

Dự án này cho phép người dùng quan sát môi trường cây trồng theo thời gian thực, thay đổi ngưỡng điều khiển, chuyển đổi giữa chế độ tự động và thủ công, đồng thời lưu lịch sử và cảnh báo vào hệ thống trực tuyến.

---

## 2. Mục tiêu của dự án

S.P.R.O.U.T hướng tới các mục tiêu sau:

- Tạo một mô hình buồng trồng cây thông minh có thể tự động duy trì điều kiện môi trường phù hợp.
- Giảm việc can thiệp thủ công bằng cách dựa trên dữ liệu cảm biến để điều khiển thiết bị.
- Cho phép người dùng theo dõi trạng thái cây qua ứng dụng Android.
- Tạo nền tảng để học cách tích hợp phần cứng, phần mềm, mạng và AI trong một hệ thống IoT hoàn chỉnh.
- Mở rộng khả năng thành một giải pháp thực tế cho nhà kính nhỏ, vườn mini hoặc phòng trồng cây nội thất.

---

## 3. Tính năng chính

### 3.1 Giám sát môi trường

Hệ thống theo dõi các thông số sau:

- Nhiệt độ
- Độ ẩm không khí
- Độ ẩm đất
- Mực nước trong bồn
- Cường độ ánh sáng
- Trạng thái cửa (nếu có mô phỏng/ cảm biến)

### 3.2 Điều khiển thiết bị

Các thiết bị có thể được điều khiển thông qua hệ thống bao gồm:

- Quạt làm mát / hút nhiệt
- Bơm tưới nước
- Lò sưởi
- Đèn quang hợp
- Quạt hút ẩm
- Quạt tăng ẩm

### 3.3 Chế độ hoạt động

- Chế độ AUTO: hệ thống tự động quyết định bật/tắt thiết bị dựa trên ngưỡng cấu hình.
- Chế độ MANUAL: người dùng điều khiển từng thiết bị bằng ứng dụng hoặc hệ thống trung gian.

### 3.4 Theo dõi và lưu trữ

- Dữ liệu cảm biến được gửi tới Firebase Realtime Database.
- Ứng dụng Android có thể đọc và hiển thị dữ liệu thời gian thực.
- Hệ thống lưu lịch sử hoạt động, cảnh báo và phân tích AI.

### 3.5 Hỗ trợ AI

Bridge Python có thể gửi dữ liệu tới Gemini để phân tích tình trạng cây và đưa ra gợi ý ngưỡng hoặc hành động phù hợp.

---

## 4. Kiến trúc hệ thống

```text
[Proteus + Arduino UNO]
          │
          │ Serial / COM port
          ▼
[Python Bridge - sprout_bridge_v3.py]
          │
          ├── Firebase Realtime Database
          ├── Gemini AI
          └── Logic xử lý dữ liệu / cảnh báo
          │
          ▼
[Android App - SPROUT/]
```

### 4.1 Vai trò từng thành phần

- Arduino/Proteus: mô phỏng cảm biến và thiết bị điều khiển.
- Python bridge: đọc dữ liệu từ cổng Serial, làm trung gian, lưu dữ liệu và gửi cảnh báo.
- Firebase: lưu trạng thái hiện tại, lịch sử và cấu hình.
- Android app: giao diện người dùng để xem và điều khiển hệ thống.
- Gemini AI: hỗ trợ phân tích nhận định tình trạng cây.

---

## 5. Công nghệ sử dụng

### 5.1 Phần cứng

- Arduino UNO
- Proteus (mô phỏng linh kiện và giao tiếp Serial)
- Cảm biến / thiết bị mô phỏng như:
  - DHT11 (nhiệt độ và độ ẩm không khí)
  - Cảm biến độ ẩm đất
  - Cảm biến mực nước
  - LDR / cảm biến ánh sáng
  - Relay điều khiển thiết bị

### 5.2 Phần mềm

- Python 3
- Firebase Admin SDK
- Firebase Realtime Database
- Kotlin
- Jetpack Compose
- Gradle
- Android Studio
- Gemini API (tùy chọn)

---

## 6. Cấu trúc thư mục

```text
S.P.R.O.U.T/
├── Arduino/
│   └── maincontrols/
│       └── maincontrols.ino
├── Proteus/
│   ├── S.P.R.O.U.T.pdsprj
│   └── Project Backups/
├── Python/
│   ├── sprout_bridge_v3.py
│   ├── plant_profiles.json
│   └── current_plant.json
├── SPROUT/
│   ├── app/
│   │   ├── build.gradle.kts
│   │   ├── google-services.json
│   │   └── src/main/java/vhdt/sprout/
│   ├── build.gradle.kts
│   └── gradle/
├── sprout-3609f-firebase-adminsdk-fbsvc-eabd4ae960.json
└── README.md
```

### 6.1 Vai trò từng thư mục

- Arduino/: mã nguồn cho phần cứng mô phỏng.
- Proteus/: project mô phỏng điện tử và bản backup.
- Python/: bridge trung gian, cache hồ sơ cây, dữ liệu hiện tại.
- SPROUT/: ứng dụng Android.
- File JSON Firebase: credential dùng cho Python bridge.

---

## 7. Luồng hoạt động hệ thống

### 7.1 Từ cảm biến đến ứng dụng

1. Arduino nhận dữ liệu từ cảm biến và gửi qua Serial.
2. Python bridge đọc dữ liệu từ cổng COM.
3. Bridge xử lý dữ liệu, kiểm tra ngưỡng và cập nhật Firebase.
4. Android app đọc dữ liệu từ Firebase và hiển thị trạng thái cho người dùng.
5. Nếu bật AI, bridge có thể gửi dữ liệu tới Gemini và nhận gợi ý điều chỉnh.

### 7.2 Luồng điều khiển

- Người dùng có thể chọn chế độ AUTO hoặc MANUAL.
- Trong AUTO, hệ thống tự điều khiển thiết bị dựa trên ngưỡng.
- Trong MANUAL, người dùng điều khiển thiết bị trực tiếp.
- Trạng thái điều khiển cũng được đồng bộ lên Firebase.

---

## 8. Cài đặt và chạy dự án

### 8.1 Yêu cầu hệ thống

- Python 3.10+
- Android Studio
- Arduino IDE (nếu cần biên dịch firmware riêng)
- Proteus (nếu chạy mô phỏng phần cứng)
- Tài khoản Firebase
- API key Gemini (tùy chọn)

### 8.2 Cài đặt Python bridge

Cài đặt các thư viện cần thiết:

```bash
pip install pyserial firebase-admin requests
```

Sau đó chỉnh các tham số quan trọng trong file Python:

- `SERIAL_PORT`: cổng COM kết nối với Arduino/Proteus
- `FIREBASE_URL`: URL Realtime Database
- `SERVICE_KEY`: đường dẫn tới file credentials Firebase
- `GEMINI_API_KEY`: nếu dùng AI

Chạy bridge:

```bash
python Python/sprout_bridge_v3.py
```

### 8.3 Chạy ứng dụng Android

1. Mở thư mục SPROUT bằng Android Studio.
2. Chờ Gradle sync hoàn tất.
3. Chọn emulator hoặc thiết bị thật.
4. Chạy ứng dụng.

> File google-services.json đã nằm trong thư mục app để kết nối Firebase.

### 8.4 Chạy mô phỏng Arduino/Proteus

- Mở file Arduino trong thư mục Arduino/maincontrols.
- Mở dự án Proteus và chạy mô phỏng.
- Đảm bảo cổng COM và baud rate khớp với Python bridge.

---

## 9. Cấu hình dữ liệu Firebase

Dự án sử dụng Firebase Realtime Database để lưu:

- trạng thái cảm biến hiện tại
- trạng thái thiết bị
- ngưỡng cảnh báo
- lịch sử dữ liệu
- thông tin cây đang được chọn
- log AI và cảnh báo

Mỗi bản ghi thường được lưu theo cấu trúc phân tầng, ví dụ:

```text
/sprout/
  /trangThai
  /camBien
  /thietBi
  /nguong
  /lichSu
  /canhBao
  /aiLog
```

---

## 10. AI và phân tích cây

Bridge Python có thể kết nối với Gemini API để phân tích tình trạng cây dựa trên dữ liệu cảm biến. Mục đích của AI là:

- đánh giá tình trạng cây hiện tại
- gợi ý cách điều chỉnh môi trường
- hỗ trợ quyết định tự động hoặc thủ công
- cung cấp lời khuyên dễ hiểu cho người dùng

Nếu chưa cấu hình API key, hệ thống vẫn có thể chạy ở chế độ cơ bản mà không cần AI.

---

## 11. Mô hình điều khiển tự động

Trong chế độ AUTO, hệ thống thường làm việc theo logic ngưỡng:

- Nếu nhiệt độ quá cao → bật quạt làm mát / hút nhiệt
- Nếu nhiệt độ quá thấp → bật lò sưởi
- Nếu độ ẩm không khí thấp → bật quạt tăng ẩm
- Nếu độ ẩm không khí cao → bật quạt hút ẩm
- Nếu độ ẩm đất thấp → bật bơm tưới
- Nếu ánh sáng thấp → bật đèn quang hợp

Điều này có thể được điều chỉnh bằng các ngưỡng tùy chỉnh trong hệ thống.

---

## 12. Ghi chú quan trọng

- Đây là dự án mô phỏng học tập, nên có thể cần chỉnh lại cổng COM tùy máy.
- Nếu dùng AI, hãy chắc chắn đã điền đúng `GEMINI_API_KEY`.
- Nếu Firebase không kết nối, hãy kiểm tra file service account và URL của database.
- Với môi trường Windows, COM port có thể khác nhau giữa các lần chạy.

---

## 13. Khắc phục sự cố thường gặp

### Không thấy cổng COM

- Kiểm tra lại việc tạo cặp COM bằng VSPE hoặc phần mềm giả lập.
- Đảm bảo Arduino/Proteus đang chạy đúng cổng.

### Python bridge không kết nối Firebase

- Kiểm tra file service account.
- Kiểm tra URL database.
- Xác nhận rằng các package Python đã cài đúng.

### Android app không cập nhật dữ liệu

- Kiểm tra kết nối Firebase.
- Kiểm tra quyền mạng và cấu hình google-services.json.
- Kiểm tra dữ liệu có được ghi đúng vào Realtime Database không.

---

## 14. Kế hoạch phát triển

- Tối ưu giao diện Android.
- Thêm nhiều loại cảm biến như pH, CO₂ hoặc ánh sáng UV.
- Cải thiện logic điều khiển tự động bằng thuật toán tốt hơn.
- Thêm cảnh báo push và lịch tưới nước thông minh.
- Mở rộng thành hệ thống quản lý nhiều buồng trồng hoặc nhiều khu vực.
- Tích hợp dashboard web để theo dõi từ máy tính.

---

## 15. Kết luận

S.P.R.O.U.T là một dự án IoT mang tính giáo dục và demo, nhưng có đủ các thành phần cần thiết để thể hiện cách xây dựng một hệ thống buồng trồng cây thông minh từ đầu đến cuối: phần cứng, truyền thông, nền tảng dữ liệu, ứng dụng người dùng và trí tuệ nhân tạo.

Dự án này không chỉ giúp hiểu về IoT mà còn cho thấy cách một ý tưởng kỹ thuật có thể được chuyển hóa thành một hệ thống có thể quan sát, điều khiển và mở rộng trong thực tế.

---

S.P.R.O.U.T — Grow smarter, not harder 🌿
