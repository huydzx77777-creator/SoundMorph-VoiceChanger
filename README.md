# SoundMorph - Voice Changer App for Android

Ứng dụng biến đổi giọng nói chuyên nghiệp cho hệ điều hành Android với 14+ hiệu ứng DSP đặc sắc, ghi âm chất lượng cao, xuất file WAV và chia sẻ nhanh qua mạng xã hội.

## 📁 Cấu trúc thư mục dự án

```
VoiceChangerApp/
├── HUONG_DAN_CAI_DAT_APK.md     # Hướng dẫn chi tiết cách tạo APK và cài đặt
├── .github/
│   └── workflows/
│       └── build-apk.yml        # CI/CD tự động build file APK trên GitHub Cloud
├── android/                     # Dự án Android Studio Native (Kotlin, Gradle)
│   ├── app/
│   │   ├── src/main/java/com/soundmorph/voicechanger/
│   │   │   ├── MainActivity.kt               # Giao diện chính & điều phối
│   │   │   ├── audio/
│   │   │   │   ├── AudioRecorder.kt          # Ghi âm PCM từ microphone
│   │   │   │   ├── VoiceEffectEngine.kt      # Xử lý DSP 14 hiệu ứng
│   │   │   │   └── WavUtils.kt               # Đọc/ghi cấu trúc file WAV
│   │   │   └── model/
│   │   │       └── VoiceEffect.kt            # Danh sách hiệu ứng
│   │   └── src/main/res/                     # Layout, Color, Theme, Icon
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── web_pwa/                     # Phiên bản Web PWA chạy trực tiếp trên điện thoại
    ├── index.html               # Giao diện Dark neon di động
    ├── style.css                # CSS thiết kế chuẩn Android
    ├── app.js                   # Xử lý âm thanh Web Audio API
    ├── manifest.json            # PWA manifest
    ├── sw.js                    # Service worker offline
    └── start-server.bat         # Chạy máy chủ thử nghiệm qua mạng Wi-Fi
```

## 🚀 Cách lấy file APK để cài lên máy
Xem chi tiết trong file [HUONG_DAN_CAI_DAT_APK.md](HUONG_DAN_CAI_DAT_APK.md).
1. **GitHub Actions (Khuyên dùng):** Đẩy mã nguồn lên GitHub, hệ thống tự động build ra file `app-debug.apk` trong 2 phút.
2. **Android Studio:** Mở thư mục `android/` -> Bấm `Build -> Build APK(s)`.
3. **PWA / PWABuilder:** Chạy trực tiếp từ trình duyệt trên điện thoại hoặc đóng gói bằng PWABuilder.
