# TayfNotes: Profesyonel Vektörel Sketch ve Tasarım Motoru Planı (v32)

Bu plan, TayfNotes'u basit bir çizim aracından çıkarıp; Figma, GoodNotes ve Procreate esintili gelişmiş bir "Vektörel Nesne Odaklı" tasarım merkezine dönüştürmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **Nesne Odaklı Canvas:** Çizilen her şekil veya kalem darbesi artık bir "nesne" (Object) olarak ele alınacak; çizimden sonra dahi seçilip taşınabilecek, rengi ve kalınlığı değiştirilebilecektir.
> **Premium Araçlar:** Dolma kalem (baskı duyarlı), kurşun kalem (dokulu) ve fosforlu kalem (Multiply Blend) modları eklenecektir.

## Proposed Changes

### 1. Vektörel Nesne Motoru (Object-Oriented Canvas)
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - Her `DrawPath` nesnesine `id`, `z-index`, `isSelected` alanlarının eklenmesi.
    - **Seçim Modu:** Çizilen öğelerin üzerine tıklandığında seçilmesi ve etrafında sınır kutusu (Bounding Box) oluşması.
    - **Bağlamsal Menü (Contextual Menu):** Seçili öğenin hemen üzerinde; Renk, Dolgu, Kalınlık ve Silme butonlarını içeren yüzen menü.

### 2. Premium UI/UX Geliştirmeleri
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - **Floating Glass Palette:** Sürüklenebilir, yarı saydam (Glassmorphism) hap şeklinde ana araç çubuğu.
    - **Kalem Dinamikleri:**
        - *Dolma Kalem:* Hıza bağlı uç kalınlığı değişimi (Baskı simülasyonu).
        - *Kurşun Kalem:* Hafif doku ve %80 opaklık.
        - *Fosforlu Kalem:* `BlendMode.Multiply` ile gerçekçi "soldurmayan" renk birleştirme.

### 3. Gelişmiş Şekil ve Alan Yönetimi
- [MODIFY] `ui/components/DrawingCanvas.kt`:
    - Şekillerin (Kare, Daire, Elips, Yay, Üçgen) çizildikten sonra köşelerinden tutularak boyutlandırılması (Resize) ve taşınması.
    - Karmaşık kesişim alanlarının doldurulması altyapısı.

### 4. Teknik Altyapı ve Veri Kaydı
- [MODIFY] `NoteViewModel.kt`: Vektörel nesne hiyerarşisini (Z-Index ve nesne ID'leri) koruyacak JSON serileştirme mantığı.

## Verification Plan

### Automated Verification
- `./gradlew :app:assembleDebug` ile build kontrolü.
- APK `TayfNotes_v01.32.apk` üretilecek.

### Manual Verification
- Bir kare çizilip daha sonra üzerine tıklanarak renginin değiştirildiği görülecek.
- Araç çubuğunun ekranın farklı yerlerine sürüklenebildiği test edilecek.
- Fosforlu kalemin alttaki yazıyı soldurmadan üzerine bindiği doğrulanacak.
- GitHub Actions yedekleme kontrolü yapılacak.
