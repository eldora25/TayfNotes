# TayfNotes: Premium Arayüz, Gelişmiş Etkileşim ve Teknik Mükemmellik Planı (v31)

Bu plan, TayfNotes'u en üst düzey premium standartlara taşımayı, "Neon Zırh" ile görsel kusursuzluğu sağlamayı ve gerçek dünya senkronizasyon altyapısını kurmayı hedefler.

## User Review Required

> [!IMPORTANT]
> **Master-Detail:** Üst ve Alt barlar tam ekran genişliğinde kalırken, orta içerik alanı dikey ve yatayda 0.4 (Liste) / 0.6 (Detay) olarak bölünecektir.
> **Sürükle-Bırak:** Manuel sıralama modu seçildiğinde notlar ve klasörler basılı tutularak yer değiştirilebilecektir.
> **Gerçek Senkronizasyon:** Google/Dropbox hesap bağlama ekranları ve gerçek veri transferi protokolleri kurulacaktır.

## Proposed Changes

### 1. Neon Zırh ve Premium Kontrast (Madde 1, 2, 7)
- [MODIFY] `ui/theme/Theme.kt`: `EditorNeonIcon` bileşenini siyah dolgulu, sarı ikonlu ve dış neon ışıltılı olacak şekilde yenileme.
- [MODIFY] `ui/ThemeSelectionScreen.kt`: Seçim anında tüm arayüzü güncelleyen "Live Preview" yapısı.
- [MODIFY] `ui/components/NoteGridItem.kt`: "Accent Bar" ve %10 yansımalı premium kart tasarımı.

### 2. Akıllı Ekran Yerleşimi (Madde 1, 3)
- [MODIFY] `MainActivity.kt`: `Scaffold` yapısını barlar tam genişlikte kalacak, sadece `content` alanı 0.4/0.6 bölünecek şekilde refaktör etme.
- [MODIFY] `ui/MainScreen.kt`: Not başlıklarının dar alanda `Ellipsis` ile dinamik sığdırılması.

### 3. Sürükle-Bırak ve Gelişmiş Klasörleme (Madde 4, 5, 6)
- [MODIFY] `ui/viewmodel/NoteViewModel.kt`: Pozisyon verilerini (`Int`) saklayacak ve güncelleyecek mantık.
- [MODIFY] `ui/MainScreen.kt` & `ui/FoldersScreen.kt`: Uzun basım (Long Press) ile tetiklenen manuel yer değiştirme etkileşimi.

### 4. Sketch Pro ve Gelişmiş Canvas (Madde 9, 10, 12)
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - **Wall & Fill:** Şekillerin kenar ve iç renklerinin bağımsız yönetimi.
    - **Kalıcı Renk:** Kalem/Fırça geçişlerinde renk hafızasının korunması.
    - **Tekil Kayıt:** Sadece "Bitti" tıklandığında Room'a nihai dökümanın gönderilmesi.

### 5. Gerçek Bulut ve Ses Çözümü (Madde 8, 11)
- [MODIFY] `util/AudioRecorder.kt`: Ses dosyasının yazılmama ve çalınmama sorununu gideren `MediaRecorder` konfigürasyon fixi.
- [MODIFY] `shared/src/.../SyncManager.kt`: Gerçek OAuth2 akışlarını tetikleyen kimlik doğrulama katmanı.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile APK `v01.31` üretimi.
- Unit testler ile position update mantığının kontrolü.

### Manual Verification
- Editör ikonlarının her renkte sarı/neon olarak parladığı görülecek.
- Notu sola kaydırınca (Swipe) silme aksiyonu test edilecek.
- Tablet dikey modda sağ panelin boş kalıp notla dolduğu doğrulanacak.
- Ses kaydı yapılıp detay panelinden dinlenecek.
