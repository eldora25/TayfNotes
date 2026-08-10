# TayfNotes: Teknik Mükemmellik, Neon Zırh ve Gerçek Senkronizasyon Planı (v29)

Bu plan, TayfNotes uygulamasını piyasadaki en üst düzey (Microsoft To Do, Procreate, Google Keep) standartlara taşımayı, görsel kontrast sorunlarını Neon Zırh ile gidermeyi ve bulut senkronizasyonunu gerçek hale getirmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **Gerçek Senkronizasyon:** Google Drive ve Dropbox için gerçek kimlik doğrulama akışları ve kullanıcı kimlik yönetimi kurulacaktır.
> **Master-Detail:** Dikey modda dahi 0.4f/0.6f oranında sol liste ve sağ boş/detay panel ayrımı aktif edilecektir.
> **Sürükle-Bırak:** Not ve klasör sıralaması artık manuel olarak (Drag-and-Drop) yapılabilecektir.

## Proposed Changes

### 1. Görsel Zırh ve Neon İkonlar (Madde 2, 7)
- [MODIFY] `ui/theme/Theme.kt`: İkon ve menüleri siyah kapsül içinde, sarı ikonlu ve Neon ışıltılı (Glow) hale getiren `EditorNeonIcon` bileşeni.
- [MODIFY] `ui/ThemeSelectionScreen.kt`: Seçim yapıldığında anında güncellenen görsel önizleme alanı.

### 2. Akıllı Navigasyon ve Master-Detail (Madde 3)
- [MODIFY] `MainActivity.kt`: Dikey modda daima Master-Detail yapısını koruma (Sol: 0.4, Sağ: 0.6).
- [MODIFY] `ui/MainScreen.kt`: Not başlıklarının dar alana dinamik adaptasyonu.

### 3. Sürükle-Bırak ve Gelişmiş Klasörleme (Madde 4, 5, 6)
- [MODIFY] `ui/MainScreen.kt` & `ui/FoldersScreen.kt`:
    - `LazyColumn` üzerinde manuel sürükle-bırak desteği.
    - Klasörlere "Düzenlenme" ve "Oluşturulma" zamanı alanlarının eklenmesi.
    - Klasör oluştururken renk seçimi desteği.

### 4. Gelişmiş Sketch ve Tasarım (Madde 10, 11, 12, 14)
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - Tekil kayıt: Sadece "Tamam" denildiğinde tek döküman olarak saklama.
    - Gerçek Renk: Kalem/Fırça seçiliyken renk değiştirme anlık ve kalıcı.
    - Şekil Pro: Kenar (Wall) ve İç (Fill) renklerinin bağımsız ayarlanması ve kaydedilmesi.

### 5. Veri ve Gerçek Bulut (Madde 8, 9)
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`:
    - Gerçek Google/Dropbox hesap bağlama ve benzersiz cihaz kimliği ile çift taraflı eşitleme.
    - Çöp kutusu ve Arşiv fonksiyonlarının stabilizasyonu.

### 6. Kritik Hata Çözümleri (Madde 1, 11, 13)
- [FIX] **Kritik:** Notun `.txt` olarak dışa aktarılmasının (Share) geri eklenmesi.
- [FIX] Ses kaydı esnasındaki "Error" veren izin ve sessiz kayıt sorununun çözülmesi.
- [FIX] Resimlerin ve Sketch'lerin detay panelinde görünmemesi sorunu.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build kontrolü.
- APK `TayfNotes_v01.29.apk` üretilecek.

### Manual Verification
- Dikey modda sağ panelin boş kalıp not seçilince dolduğu görülecek.
- Sketch ekranında bir daire çizilip iç dolgusunun değiştiği test edilecek.
- Dropbox/Drive ile gerçek veri alışverişi denenecek.
- Notun .txt olarak paylaşılabildiği doğrulanacak.
