# RAHAYU RC TIMER

Aplikasi Timer untuk Remote Control dengan fitur:
- 10 Unit Remote Control
- Timer countdown dengan alarm suara
- Background service (tetap jalan saat HP dikunci)
- Tema Earthy & Premium (Coklat & Emas)

## Cara Build APK

### Otomatis via GitHub Actions:
1. Push kode ke repository GitHub Anda
2. GitHub Actions akan otomatis build APK
3. Download APK dari tab "Actions" → Pilih workflow terakhir → Download artifact

### Manual via Android Studio:
1. Buka proyek di Android Studio
2. Klik Build → Build Bundle(s) / APK(s) → Build APK(s)
3. APK akan tersedia di: `app/build/outputs/apk/debug/app-debug.apk`

## Teknologi
- Java
- Android SDK 24+
- Material Design Components
- RecyclerView (Grid Layout)
- Foreground Service
- MediaPlayer (Looping Audio)
