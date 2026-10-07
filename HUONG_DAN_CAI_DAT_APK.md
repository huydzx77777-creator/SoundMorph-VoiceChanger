# 📱 HƯỚNG DẪN TẠO FILE APK & CÀI ĐẶT LÊN ĐIỆN THOẠI ANDROID

Chào bạn! Ứng dụng đổi giọng nói **SoundMorph** đã được xây dựng hoàn chỉnh với đầy đủ mã nguồn native cho Android và bộ xử lý 14 hiệu ứng âm thanh DSP.

Dưới đây là 3 cách dễ nhất để bạn lấy file `.apk` và cài đặt lên điện thoại Android của mình.

---

## ⚡ CÁCH 1: TỰ ĐỘNG XUẤT FILE APK MIỄN PHÍ QUA GITHUB ACTIONS (KHUYÊN DÙNG - KHÔNG CẦN CÀI ANDROID STUDIO)

Dự án đã tích hợp sẵn kịch bản biên dịch đám mây tại `.github/workflows/build-apk.yml`. Bạn không cần cài đặt bất kỳ phần mềm nặng nào trên máy tính.

### Các bước thực hiện:
1. Đăng nhập vào [GitHub](https://github.com) (tạo tài khoản miễn phí nếu chưa có).
2. Tạo một Repository mới (ví dụ đặt tên: `voice-changer-android`).
3. Tải toàn bộ thư mục `VoiceChangerApp` này lên repository đó (bằng Git hoặc kéo thả trên web GitHub).
4. Ngay khi code được đẩy lên, GitHub sẽ tự động chạy tiến trình **Actions**:
   - Tự chuẩn bị Java 17 và Android SDK.
   - Tự động biên dịch mã nguồn thành file APK.
5. Sau khoảng 2 - 3 phút:
   - Vào tab **Actions** trên GitHub repository của bạn.
   - Bấm vào lần chạy mới nhất (Build Android APK).
   - Kéo xuống mục **Artifacts** ở dưới cùng -> bấm tải về file **`SoundMorph-VoiceChanger-APK.zip`**.
6. Giải nén file zip, bạn sẽ có ngay file `app-debug.apk` để gửi vào điện thoại và cài đặt!

---

## 🛠️ CÁCH 2: DÙNG ANDROID STUDIO TRÊN MÁY TÍNH (1-CLICK BUILD)

Nếu máy tính của bạn đã có hoặc cài đặt **Android Studio**:

1. Mở **Android Studio**.
2. Chọn **File** -> **Open...** -> trỏ đến thư mục:
   ```
   C:\Users\DELL\.gemini\antigravity\scratch\VoiceChangerApp\android
   ```
3. Chờ Android Studio tải các thư viện Gradle lần đầu (mất khoảng 1 - 2 phút).
4. Trên thanh menu, chọn:
   ```
   Build  ->  Build Bundle(s) / APK(s)  ->  Build APK(s)
   ```
5. Khi hoàn tất, một thông báo nhỏ ở góc dưới bên phải màn hình sẽ hiện lên:
   - Bấm vào chữ **`locate`** màu xanh.
   - Thư mục chứa file **`app-debug.apk`** sẽ lập tức mở ra!

---

## 🌐 CÁCH 3: TRẢI NGHIỆM TRỰC TIẾP TRÊN ĐIỆN THOẠI HOẶC XUẤT APK QUA PWABUILDER

Thư mục `web_pwa` chứa phiên bản di động siêu nhẹ, có thể cài trực tiếp lên điện thoại Android mà không cần file APK:

### 1. Dùng thử ngay trên điện thoại qua Wi-Fi:
1. Bấm đúp chuột vào file:
   ```
   C:\Users\DELL\.gemini\antigravity\scratch\VoiceChangerApp\web_pwa\start-server.bat
   ```
2. Màn hình đen sẽ hiện ra địa chỉ IP, ví dụ: `http://192.168.1.5:8080`.
3. Dùng điện thoại Android kết nối cùng mạng Wi-Fi, mở trình duyệt **Chrome** và gõ địa chỉ trên.
4. Bấm vào nút 3 chấm của Chrome -> chọn **"Cài đặt ứng dụng"** hoặc **"Thêm vào Màn hình chính"**.
   - Biểu tượng ứng dụng SoundMorph sẽ xuất hiện trên màn hình điện thoại như một app Android thực thụ!

### 2. Đóng gói thành file APK bằng PWABuilder:
- Đưa thư mục `web_pwa` lên một hosting miễn phí (như GitHub Pages, Vercel hoặc Netlify).
- Truy cập [PWABuilder](https://www.pwabuilder.com).
- Dán đường link web của bạn -> Bấm **Package for Android** -> Tải file `.apk` về máy!

---

## 📲 HƯỚNG DẪN CÀI ĐẶT FILE APK TRÊN ĐIỆN THOẠI ANDROID

Khi bạn đã có file `app-debug.apk`:

1. **Chuyển file APK vào điện thoại:**
   - Bạn có thể gửi file qua Zalo (gửi dạng File), Telegram, tải lên Google Drive rồi mở trên điện thoại, hoặc cắm cáp USB chép vào.
2. **Tiến hành cài đặt:**
   - Chạm vào file `.apk` trên điện thoại.
   - Nếu điện thoại báo *"Bảo mật: Bạn chưa cho phép cài đặt ứng dụng không rõ nguồn gốc"*:
     - Bấm **Cài đặt** (Settings).
     - Bật công tắc **"Cho phép từ nguồn này"** (Allow from this source).
     - Quay lại và bấm **Cài đặt** (Install).
3. **Mở ứng dụng & Sử dụng:**
   - Mở app **SoundMorph** vừa cài đặt.
   - Khi app hỏi quyền truy cập Micro (Microphone), chọn **"Trong khi dùng ứng dụng"** (While using the app).
   - Chạm vào nút Micro tròn màu đỏ để bắt đầu ghi âm giọng nói của bạn, bấm dừng và chọn bất kỳ hiệu ứng nào để biến đổi giọng!

---

## 🎙️ DANH SÁCH 15 HIỆU ỨNG ĐỔI GIỌNG CÓ TRONG APP:
1. 🎧 **Giọng Gốc** - Âm thanh nguyên bản
2. 🎀 **Nữ Điệu Dẹo** - Giọng nữ nũng nịu, ngọt ngào, điệu đà (vibrato luyến láy + pitch cao ấm)
3. 🐿️ **Sóc Chuột (Helium)** - Giọng chói tai, hài hước, pitch cao
4. 👹 **Quái Vật (Monster)** - Âm trầm cực sâu, rùng rợn, uy lực
5. 🤖 **Người Máy (Robot)** - Hiệu ứng ring-modulation âm hưởng kim loại
6. 👽 **Người Ngoài Hành Tinh (Alien)** - Biến điệu tần số rung rinh phong cách UFO
7. 🦇 **Tiếng Vang (Echo)** - Phản xạ âm thanh phòng lớn
8. 🗻 **Hang Động (Cave Reverb)** - Vang sâu thẳm giữa vách đá
9. 🔄 **Tua Ngược (Reverse)** - Đọc ngược từ đuôi lên đầu cực lạ
10. ⚡ **Tia Chớp (Fast)** - Tốc độ nói nhanh 1.4x
11. 🐢 **Say Xỉn (Slow)** - Tốc độ chậm chạp, lề mề 0.7x
12. 🌊 **Dưới Nước (Underwater)** - Lọc dải tần trầm, âm thanh như ngập dưới đáy biển
13. 📻 **Bộ Đàm / Radio** - Dải tần cổ điển, rè nhẹ phong cách quân đội
14. 📢 **Loa Phóng Thanh (Megaphone)** - Tiếng loa phường chói chang vang xa
15. 👻 **Bóng Ma (Ghost)** - Âm hưởng u ám, ma mị rợn người
