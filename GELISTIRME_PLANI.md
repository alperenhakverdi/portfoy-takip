# Portföy Takip Uygulaması — Geliştirme Planı

Kaynak: *Portföy Takip Uygulaması — Gereksinim Dokümanı (Faz 1)*, 2026-09-20.
Durum: **son hâli.** Dokümandaki çelişki ve eksikler karara bağlandı; kararlar [KARARLAR.md](KARARLAR.md)'de.

## İlerleme (2026-09-22)

| # | Milestone | Durum | Not |
|---|---|---|---|
| M0 | İskelet | ✅ Bitti | Gradle, 5 modül, Compose + Hilt |
| M1 | Room veritabanı | ✅ Bitti | 5 tablo, 16+ DAO testi. Migration testi ilk şema değişikliğinde |
| M2 | Hesaplama çekirdeği | ✅ Bitti | Getiri, Dietz, dağılım, biçim, doğrulama |
| M3 | Kaynak katmanı | ✅ Bitti | Yönlendirici, bütçe, takvim, zamanlayıcı, fiyat deposu |
| M4 | Ekranlar (W1–W11) | ✅ Bitti | Emülatörde elle gezildi; grafikler M6 ile gerçek veriye bağlandı |
| M5 | Gerçek kaynaklar | ✅ Bitti | 7 adaptör, zamanlayıcı tazelemeye bağlandı, uygulamada canlı doğrulandı |
| M6 | Geçmiş seriler, snapshot, gerçek grafikler | ✅ Bitti | Emülatörde gerçek verilerle doğrulandı |
| M7 | Kenar durumlar, çevrimdışı | ✅ Bitti | 16 durumun tamamı test/emülatör kabuluyla doğrulandı |
| M8 | Sertleştirme, APK | ✅ Bitti | İmzalı release APK, büyük portföy testi, DST/kabul turu |
| M9 | Güncelleme paketi 1 | ✅ Bitti | Döviz kategorisi, kategoriden başlayan ekleme, kategori kırılımlı performans |
| M10 | Kategori ikonları | ✅ Bitti | Renkli minimalist ikonlar |
| M11 | Uygulama adı ve simgesi | ✅ Bitti | "Portföy Takip", uyarlanabilir simge |
| M12 | Bulunan hatalar ve tamamlama turu | ✅ Bitti | Fiyat/performans hatası, terk edilmiş seri temizliği, örnek portföy testi |
| M13 | Kaydırdıkça kademeli fiyat çekimi | ✅ Bitti | Emülatörde doğrulandı: BIST'te kaydırılan hisseler partiler hâlinde doldu |
| M14 | Tema: renk, tipografi, ikon, animasyon | ✅ Bitti | Açık/koyu tema, kategori ve getiri renkleri, ikonlar emülatörde doğrulandı |
| M15 | Cihaz uyumluluğu ve erişilebilirlik | ✅ Bitti | Küçük/tablet ekran, yazı ölçeği, dikey kilit, TalkBack; emülatörde doğrulandı |
| M16 | UI temizliği (kullanıcı geri bildirimi) | ✅ Bitti | "Son eklenenler" kaldırıldı, grafik kartı notları sadeleşti, Performans satırları kısaldı |
| M17 | Varlık yönetimi ekranı | ✅ Bitti | Tıklayınca ayrı tam ekran; alım/azaltma hareketleri (migration gerekmedi — kolon zaten vardı) |
| M18 | Tema tercihi: açık/koyu/sistem | ✅ Bitti | Portföy'de küçük ikon + 3 seçenekli diyalog; SharedPreferences ile kalıcı |
| M20 | Kripto kategorisi (BTC, ETH) | ✅ Bitti | Sabit 2 varlık, arama yok; Yahoo kaynağı, 24/7 tazeleme |
| M21 | Sekme birleştirme, Portföy detaylandırma | ✅ Bitti | Performans sekmesi kalktı, grafik ayrı ekran oldu; getiri üç seviyede `TL (yüzde)` + G/H/TÜM dönem düğmesi |
| M22 | Grafik ekranı: tam dönem seti, kısa etiketler | ✅ Bitti | Dokuz dönem zaten hazırdı; kısa etiket (1G, 1H, 1A…) eklendi |
| M23 | Para birimi: rakama dokununca TL ↔ USD | ✅ Bitti | Ayrı düğme yok; özet kartı + Grafik ekranı, kategori satırları TL kalıyor |
| M24 | Dağılım grafiği: yüzdeler halkanın kenarında | ✅ Bitti | Alttaki liste kalktı; her dilimden kılavuz çizgili etiket |
| M25 | ABD ve Kripto satırları: doğal para biriminde (USD) | ✅ Bitti | Kullanıcı tercihinden bağımsız, sabit kural; Portföy + Grafik |
| M26 | USD girişi Kripto'ya genişledi, varlık ekranı USD gösteriyor | ✅ Bitti | Ekle/Düzenle formları varsayılan USD; "+ Ekle" diyaloğundaki eski tutarsızlık da düzeltildi |
| M27 | Özet kartının dönemi kategorilere yayılıyor | ✅ Bitti | Üstteki G/H/TÜM değişince hepsi senkronlanır; tek tek dokunmak yine bağımsız |
| M28 | Dağılım grafiği: kılavuz çizgileri kesişmiyor | ✅ Bitti | Üç parçalı yönlendirme (kol → yatay → dikey), sütun dengeleme sonrası çaprazlaşma gitti |
| M29 | Dağılım yerleşimi: saf fonksiyonlara çıkarılıp birim testlendi | ✅ Bitti | 17 test: gerçek senaryo, aşırı eşitsizlik, 7 kategori, taşma — "büyüklükler değişince de doğru" artık kanıtlı |
| M30 | Ekle listesi ABD/Kripto USD, Nakit TL çerçevesi kaldırıldı | ✅ Bitti | Liste artık formla aynı fiyatı gösteriyor; kategori satırları birbirine eşit görünüyor |
| M31 | Bugün alınan varlıkta kâr/zarar ana ekranda 0,00/— görünüyordu | ✅ Bitti | `kazanc()`/`yuzde()` tek-günlük seride değer-maliyet farkına düşüyor; toplam/kategori/varlık hepsinde aynı anda düzeldi |
| M32 | "Veriler güncel değil" uyarısı ve "en az iki günlük veri gerekir" mesajı yumuşatıldı | ✅ Bitti | Gereksiz alarm metni kaldırıldı; tek günlük grafik durumu artık hata gibi görünmüyor |

Plan (M0–M12) tamamlandı. Kalan işler **isteğe bağlı, gelecek planlar** — bkz. bölüm 6. Ayrıntılar
aşağıdaki ilgili bölümlerde; kararların gerekçesi [KARARLAR.md](KARARLAR.md)'de.

**Geliştirme ortamı notu:** bu bilgisayardaki Avast antivirüsü HTTPS trafiğini kendi sertifikasıyla yeniden imzalıyor.
Java araçları için `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` gerekiyor; emülatörde ise Avast'ın
kök sertifikası test emülatörünün kullanıcı deposuna kuruldu ve **yalnızca debug** derlemesi kullanıcı sertifikalarına
güveniyor (`app/src/debug`). Yayın sürümü etkilenmez. Gerçek telefonda bu gerekmez.

**Ölçülen performans (emülatör, yavaş):** ilk açılışta 30 bin kayıtlık katalog yüklemesi 85 sn → 31–38 sn'ye indi
(hazır SQL ifadesi + 500'lük kısa işlemler). Ekran yükleme sırasında kilitlenmiyor. Gerçek telefonda birkaç saniye beklenir.

Plan, dokümandaki eksik ya da kendi içinde çelişen noktaları 27 kararla kapatır (bazıları kapsamı
genişletir, M9/M10 kullanıcı geri bildirimiyle eklendi); kararların gereksinim dokümanına geri
işlenmesi isteğe bağlıdır (bölüm 4).

---

## 0. Yaklaşımın özeti

Üç ilke planın tamamını belirliyor:

1. **Hesaplama çekirdeği UI'dan ve ağdan önce yazılır.** Dokümanın kendi ifadesiyle "getiri ve ortalama
   maliyet hatası en pahalı hatadır" (bölüm 13). Çekirdek saf Kotlin, Android bağımlılığı olmadan,
   BigDecimal ile ve testleriyle birlikte çıkar.
2. **Ağ katmanı en sona bırakılmaz ama en başa da alınmaz.** Ekranlar önce sahte (fake) adaptörle,
   mock veriyle çalışır. Gerçek API'ler tek tek ve birbirinden bağımsız devreye alınır; biri
   çalışmazsa diğerleri bloke olmaz.
3. **Her kaynak kırılgan kabul edilir.** Dokümanın 9.4'ü zaten üç kaynağın son 18 ayda kırıldığını
   yazıyor. Adaptör arayüzü ve elle fiyat girişi yedeği, isteğe bağlı bir güzellik değil, temel gereksinimdir.

**Sıralamanın gerekçesi:** ekranlar mock veriyle çizilebildiği için wireframe doğrulaması (W1–W11) ile
Room/çekirdek geliştirmesi paralel yürüyebilir. En uzun tedarik süreli iş — TCMB EVDS API anahtarı —
ilk günden başlatılır.

---

## 1. Milestone tablosu

| # | Milestone | Çıktı | Boyut |
|---|---|---|---|
| M0 | Proje iskeleti ve araç zinciri | Derlenen boş uygulama, bağımlılıklar, statik analiz | S |
| M1 | Veri modeli ve Room | 5 tablo, DAO'lar, migration testi | S |
| M2 | Hesaplama çekirdeği | Saf Kotlin modül + birim testleri | M |
| M3 | Adaptör katmanı, repository, tazeleme zamanlayıcısı | Arayüz, fake adaptör, önbellek, bütçe ve ritim | M |
| M4 | Wireframe ekranları (W1–W11) | Mock veriyle tüm akış, navigasyon | L |
| M5 | Gerçek kaynaklar | 6 fiyat adaptörü + sembol listesi + elle giriş yedeği | L |
| M6 | Geçmiş seriler, snapshot, gerçek grafikler | ✅ Bitti | Emülatörde gerçek verilerle doğrulandı (değer grafiği, getiri, varlık listesi) |
| M7 | Kenar durumlar ve çevrimdışı | ✅ Bitti | 16 durumun tamamı test veya emülatör kabuluyla doğrulandı |
| M8 | Sertleştirme ve teslim | ✅ Bitti | İmzalı release APK, büyük portföy testi, DST/kabul turu doğrulandı |
| M9 | Güncelleme paketi 1 | ✅ Bitti | Döviz kategorisi, kategoriden başlayan ekleme akışı, kategori kırılımlı performans |
| M10 | Kategori ikonları | ✅ Bitti | Renkli minimalist ikonlar: Ekle, Portföy ve Performans kategori satırları |
| M11 | Uygulama adı ve simgesi | ✅ Bitti | "Portföy Takip", uyarlanabilir simge (halka + kırmızı dilim motifi) |
| M12 | Bulunan hatalar ve tamamlama turu | ✅ Bitti | Fiyat/performans hatası, terk edilmiş seri temizliği, örnek portföy testi, Play Store incelemesi |
| M13 | Kaydırdıkça kademeli fiyat çekimi | ✅ Bitti | S–M |
| M14 | Tema: renk, tipografi, ikon, animasyon | ⏳ Planlandı | L |
| M15 | Cihaz uyumluluğu ve erişilebilirlik | ⏳ Planlandı | S |

**Süre tahmini yok, bilinçli olarak:** tek kişilik kişisel proje, deadline yok. Boyutlar göreli (S küçük, M orta, L büyük). En belirsiz kalem M5 — kaynak denemeleri önce yapılırsa netleşir.

---

## 2. Fiyat tazeleme stratejisi

Uygulama **anlık canlı veri hedeflemez.** Öncelik sırası nettir:

> **kapanış fiyatı > açılış fiyatı > gün içi hareket**

Kapanış fiyatı portföyün günlük kaydını (snapshot, geçmiş seri, grafik) belirlediği için hiçbir koşulda
atlanmaz. Gün içi tazeleme bir konfor özelliğidir ve ücretsiz katman sınırlarının **artanıyla** yapılır.

### 2.1. Piyasa saatleri

Saatler kendi zaman diliminde tanımlanır; TSİ karşılığı çalışma anında hesaplanır. ABD yaz saati
uyguladığı, Türkiye ise kalıcı UTC+3 kullandığı için TSİ saatleri hiçbir yere sabit yazılmaz —
yazılırsa yılda iki kez, birkaç hafta boyunca yanlış saatlerde çekim yapılır.

| Piyasa | Zaman dilimi | Seans | Takvim |
|---|---|---|---|
| ABD | `America/New_York` | 09:30–16:00 | `US_MARKET` |
| BIST | `Europe/Istanbul` | 10:00–18:00 | `TR_MARKET` |
| TEFAS | `Europe/Istanbul` | Fiyat 21:00'de açıklanır | `TR_MARKET` |
| Altın / gümüş | Küresel | Pzt–Cum | Yalnızca hafta sonu kuralı |
| USD/TRY | `Europe/Istanbul` | Pzt–Cum | `TR_MARKET` |

### 2.2. Tazeleme olayları

| Varlık türü | Açılış turu | Gün içi | Kapanış turu |
|---|---|---|---|
| ABD hisseleri | Seans başı + 2 dk | Adaptif, varsayılan 30 dk | Seans sonu + 15 dk |
| BIST hisseleri | Seans başı + 2 dk | Adaptif, varsayılan 30 dk | Seans sonu + 15 dk |
| TEFAS fonları | — | — | 21:15, günde 1 |
| Altın / gümüş | — | 30 dk, Pzt–Cum | — |
| USD/TRY | — | Saat başı | — |
| Nakit TL | — | — | — |

- **Açılışta +2 dk:** ilk işlemlerin oturması için. Seans açılışında gelen fiyat henüz gürültülüdür.
- **Kapanışta +15 dk:** kapanış fiyatının kesinleşmesi için. Bu tur **hiçbir koşulda atlanmaz**;
  bütçe hesabından önce ayrılır.
- **USD/TRY saat başı:** TCMB zaten saat başı yayınlıyor; daha sık çekmek yeni veri getirmez.

### 2.3. Günlük çağrı tavanları

Sağlayıcı limitleri (doküman 9.3) ve bunların altında kalan kendi tavanlarımız:

| Kaynak | Sağlayıcı limiti | Kendi tavanımız | Kullanım |
|---|---|---|---|
| Finnhub | dakikada 60 | günde 500 | ABD anlık |
| Yahoo Finance | resmî değil, limit yok | günde 300 | BIST anlık + geçmiş |
| Twelve Data | günde 800 | 100 geçmiş (9.3/3) + 100 yedek anlık | ABD ve emtia geçmişi |
| TCMB (EVDS + saat başı) | — | günde 50 | Kur |
| TEFAS | — | günde 5 | Fon |
| Truncgil | — | günde 50 | Gram altın/gümüş |

Yahoo için düşük tavan bilinçli: uç resmî değil, sık bozuluyor ve kullanım şartlarını ihlal edebiliyor
(9.4/4). Seyrek istek hem bozulma hem engellenme riskini azaltır.

### 2.4. Adaptif gün içi aralık

Doküman 9.3/1'in **tur başına 30 çağrı** kuralı, tek başına günlük tavanı büyük ölçüde garanti ediyor:

```
ABD, 6,5 saatlik seans, 30 dk aralık
  → 13 gün içi tur + açılış + kapanış = 15 tur
  → 15 × 30 (tur başı tavan) = 450 çağrı  <  500 tavan  ✔
```

Portföy 30 varlığı aşınca varlıklar gruplanıp dönüşümlü tazelenir (9.3/1'in kendi kuralı), yani
**toplam çağrı sabit kalır, varlık başına tazeleme aralığı uzar:**

| ABD varlık sayısı | Tur başına | Varlık başına gerçek aralık |
|---|---|---|
| ≤ 30 | hepsi | 30 dk |
| 31–60 | 30, dönüşümlü | 60 dk |
| 61–90 | 30, dönüşümlü | 90 dk |

BIST'te seans daha uzun (8 saat) ve tavan daha düşük olduğu için aralık kendiliğinden uzayabilir.
Algoritma:

```
aralık = 30 dk
açılış ve kapanış turları bütçeden önce ayrılır
while (öngörülen_günlük_çağrı > kaynak_tavanı):
    aralık = sonraki(45 dk → 60 dk)
    if aralık > 60 dk:
        gün içi turlar kapatılır
        yalnızca açılış + kapanış turu yapılır
        kullanıcıya bilgi satırı: "portföy büyük — gün içi güncelleme kapalı"
        break
```

Alt sınır 15 dakikadır (9.3/5: aynı varlık için 15 dk içinde ikinci istek atılmaz) ve hiçbir koşulda
altına inilmez. Pull-to-refresh her zaman açıktır ve dakikada 1 kez çalışır (9.2/4) — kullanıcı
istediğinde daima zorlayabilir.

### 2.5. Değişmeyen kurallar

- Piyasa kapalıyken çekim yok; son kapanış fiyatı ve "piyasa kapalı" etiketi (9.2/1).
- Tatil günlerinde çekim yok (9.2/2).
- Arka planda fiyat çekilmez; uygulama öne geldiğinde son çekimin üzerinden 15 dk geçmişse
  bir tazeleme yapılır (9.2/5).
- Çekilen fiyatlar önbelleğe alınır; uygulama internetsiz açılır ve son bilinen fiyatlarla
  tüm ekranları çizer (9.2/6).
- Geçmiş seri bütçesi ayrıdır: günde en fazla 100 çağrı (9.3/3), aşılırsa kalan varlıklar ertesi güne.

---

## M0 — Proje iskeleti ve araç zinciri

**Kapsam**

- Android Studio projesi, `minSdk 26`, Kotlin 2.x, tek `Activity`.
- Gradle version catalog (`libs.versions.toml`) — bağımlılık sürümleri tek yerde.
- Modül ayrımı:
  - `:app` — Compose ekranları, ViewModel'ler, Hilt bağlama, navigasyon.
  - `:core:calc` — **saf Kotlin/JVM modülü**, Android bağımlılığı yok. Getiri, ağırlıklı ortalama
    maliyet, kategori toplamları, yuvarlama.
  - `:core:data` — Room, repository, entity eşlemeleri.
  - `:core:network` — Retrofit servisleri, fiyat ve sembol listesi adaptörleri.
  - `:core:model` — ortak domain tipleri (`Asset`, `Transaction`, `Category`, `Money`).
- Hilt kurulumu, `@HiltAndroidApp`.
- ktlint/detekt + `./gradlew check` derleme kapısı.
- API anahtarları: `local.properties` → `BuildConfig`. **`local.properties` sürüm kontrolüne girmez.**
  (Doküman 9.1/4: anahtar pakete gömülmez; Faz 1'de tek kullanıcılı yerel kurulum kabul edilir,
  yayına çıkışta proxy zorunludur — bu plan yayını kapsamıyor.)

**Kabul kriteri:** boş uygulama cihazda açılıyor, `:core:calc` Android'siz JVM testiyle çalışıyor,
`./gradlew check` yeşil.

**Paralel başlatılacak iş:** TCMB EVDS ücretsiz API anahtarı başvurusu (doküman 16.1).
Onay süresi dışsal; ilk gün başlatılmalı.

---

## M1 — Veri modeli ve Room

Doküman bölüm 12'deki beş tablo:

| Tablo | Notlar |
|---|---|
| `asset` | `kategori` Room `TypeConverter` ile enum (`ABD, BIST, FON, EMTIA, NAKIT`), serbest metin değil. `birim_tipi` de enum |
| `transaction` | `tip` şimdilik yalnızca `ALIS`; enum olarak tutulur ki satış sonraki fazda migration'sız eklenebilsin |
| `price_quote` | `kaynak` alanı zorunlu; `MANUEL` da geçerli bir kaynaktır |
| `price_history` | `(asset_id, tarih)` birleşik benzersiz indeks. `asset_id = USDTRY` paylaşımlı kur serisini tutar |
| `portfolio_snapshot` | `tarih` birincil anahtar (günde tek kayıt), `kategori_dagilimi` JSON |

**Kararlar**

- **BigDecimal saklama:** parasal alanlar `TEXT` olarak `toPlainString()` ile yazılır. `REAL`
  kullanılmaz — kayan nokta hatası dokümanın 10.7/2 maddesiyle doğrudan çelişir. Bu alanlar üzerinde
  karşılaştırmalı sorgu gereksinimi yok, metin saklama sorun çıkarmaz.
- Şema sürümlenir, `exportSchema = true`, şema JSON'ları repoda tutulur (12/7: ileride kripto/döviz
  kategorisi eklenecek).
- Tohum verisi: Nakit TL tek `asset` kaydı olarak ilk açılışta yazılır (12/5). Emtia iki kayıt:
  `XAUGR`, `XAGGR` (karar 12).
- Gömülü sembol listeleri ilk açılışta `asset` tablosuna yazılır (karar 2).
- Çevrimdışı arama (4.1/6) için normalize `arama_adi` sütunu: küçük harfe indirilmiş, Türkçe
  karakterleri katlanmış (ı/i, ü/u, ş/s, ğ/g, ö/o, ç/c) hali. 4.1/3'ü `LIKE` ile SQL tarafında karşılar.

**Kabul kriteri:** DAO testleri in-memory Room üzerinde (Robolectric, Android 35) geçiyor — 16 test.
Migration altyapısı hazır: `exportSchema = true`, şema `core/data/schemas/.../1.json` olarak commitli.
**Migration testi henüz yazılmadı:** ikinci bir şema sürümü yok, sahte bir sürüm uydurmak yerine ilk gerçek
şema değişikliğinde (`MigrationTestHelper` ile v1→v2) yazılacak.

---

## M2 — Hesaplama çekirdeği

Projenin en kritik parçası. `:core:calc` içinde, tamamı saf fonksiyon.

**Formüller (doküman 10)**

- Toplam maliyet = Σ(alış fiyatı × adet + komisyon)
- Güncel değer = Σ(güncel fiyat × adet)
- Kâr/Zarar ₺ = güncel değer − toplam maliyet
- Getiri % = (güncel değer − toplam maliyet) / toplam maliyet × 100
- Birim maliyet = Σ(alış fiyatı × adet + komisyon) / Σ adet
- Kategori yüzdesi = kategori TL değeri / portföy toplam TL değeri
- ABD TL değeri = USD fiyat × güncel USD/TRY (maliyet tarafında kur çevrimi yok — 10.3/2)

**Dönemsel getiri: Basit Dietz** (karar 1)

```
                V_bitiş − V_başlangıç − ΣC
Getiri % = ─────────────────────────────────────────── × 100
            V_başlangıç + Σ(C_i × kalan_gün_i / dönem_gün)
```

Katkı, dönemde kaldığı gün oranınca paydaya girer. Dokümanın düz formülü, son gün yapılan bir alımda
getiriyi %10 yerine %5 gösteriyordu; Dietz bunu %9,68'e çekiyor. Katkı dönem başındaysa %5 çıkması doğrudur (para tüm dönem piyasada)
ve sapma yok. Fonksiyon adı `periodReturn(start, end, contributions)` — sapmanın nerede doğduğu
kodda görünür olmalı. Payda ≤ 0 ise `null` döner → ekranda `—`.

**Kurallar**

- Ara hesaplar tam hassasiyetle; yuvarlama yalnızca biçimlendirme katmanında (10.7/1).
  Bölmede `MathContext.DECIMAL128`.
- Yüzde dilimlerinin toplamı yuvarlama nedeniyle 100'ü aşarsa fark en büyük dilimden düşülür (8.1/7).
  Bu kural `allocationOf()` içinde yaşar.
- Toplam maliyet sıfırsa getiri `null` → `—` (karar 11).
- Tarihler gün bazına yuvarlanır (10.6/3).

**Biçimlendirme (bölüm 2)**

Ayrı bir `format` paketi: `tr-TR`, binlik nokta / ondalık virgül, tutar 2 hane, yüzde 2 hane,
adet/gram 4 hane, yön işareti ▲/▼. "Son güncelleme" bugüne aitse `SS:DD`, değilse `GG.AA SS:DD` (karar 13).

**Test stratejisi — burada cimrilik yapılmaz**

- Tek alım; çok alım; komisyonlu ve komisyonsuz ağırlıklı ortalama.
- Yalnızca nakit portföy (getiri %0, sıfıra bölme yok).
- Kur hareketi + hisse hareketi bileşkesi (10.3/3) — bilinen girdilerle beklenen çıktı.
- Yuvarlama: dilim toplamı 100,01 çıkan senaryo.
- **Dietz ağırlıklandırması:** katkı dönem başında (ağırlık 1), ortasında (0,5), son günde (1/30)
  ve hiç katkı yokken — dördü ayrı test.
- Portföy yaşı < seçilen dönem (7.1/4).
- Alış tarihi 5 yıldan eski: maliyet ve toplam getiri doğru, grafik 5 yıl öncesinden başlıyor (karar 20).

**Kabul kriteri:** `:core:calc` satır kapsamı ≥ %90; yukarıdaki yedi senaryonun her biri için
adlandırılmış bir test var.

---

## M3 — Adaptör katmanı, repository ve tazeleme zamanlayıcısı

**Adaptör arayüzleri**

```kotlin
interface PriceSource {
    val id: SourceId
    suspend fun getQuote(codes: List<String>): Result<List<Quote>>
    suspend fun getHistory(code: String, from: LocalDate, to: LocalDate): Result<List<Candle>>
}

interface SymbolCatalogSource {
    suspend fun listSymbols(): Result<List<AssetInfo>>
}
```

`getQuote` **liste alır** (9.1/3). Toplu isteği desteklemeyen kaynak — Finnhub `/quote`, tek sembol —
bunu adaptör içinde sıralı çağrıya açar ve **çağrı bütçesine gerçek istek sayısını bildirir** (karar 16).
Çağıran taraf farkı görmez.

**Yönlendirme katmanı (`PriceRepository`)**

- Kategori → (birincil, yedek) kaynak eşlemesi tek bir tablo olarak tanımlanır.
- Birincil kaynak arka arkaya **3 kez** başarısızsa yedeğe geçilir (9.1/2). Sayaç kaynak bazında tutulur,
  başarılı çağrıda sıfırlanır.
- İkisi de başarısızsa son bilinen fiyat + "veriler güncel değil" bayrağı.
- 15 dakika içinde aynı varlık için ikinci istek atılmaz; `price_quote.zaman_damgasi` üzerinden
  önbellekten okunur (9.3/5).
- Çağrı bütçesi sayaçları kaynak bazında (bölüm 2.3) + tur başına 30 (9.3/1) + günlük geçmiş seri 100 (9.3/3).
- Her kaynak için son başarılı çekim zamanı saklanır; 24 saat veri gelmezse tek seferlik uyarı (9.4/5).
- `MANUEL` kaynaklı varlıklar tazelemeye hiç girmez, bütçe tüketmez (karar 9).

**Tazeleme zamanlayıcısı**

Bölüm 2'deki tasarımın uygulaması: piyasa saati ve takvim kontrolü, açılış/kapanış turlarının
ayrılması, adaptif gün içi aralık hesabı, portföy 30'u aşınca dönüşümlü gruplama.
Piyasa saatleri `ZoneId` ile, iki gömülü takvim (`US_MARKET`, `TR_MARKET`) ile çalışır (kararlar 15, 17).

**Bu milestone'da gerçek API yok.** `FakePriceSource` deterministik mock veri üretir; M4 tamamen
bunun üzerinde çalışır ve testler ağdan bağımsız kalır.

**Kabul kriteri:** 3-başarısızlık→yedek geçişi, 15 dk önbellek, bütçe aşımında aralığın uzaması,
portföy 30'u aşınca dönüşümlü gruplama, piyasa kapalı ve tatil davranışları — hepsi fake adaptörle
testlerle doğrulanmış. Yaz/kış saati geçişi için `America/New_York` üzerinden iki ayrı tarihte test var.

---

## M4 — Wireframe ekranları (W1–W11)

Dokümanın Faz 1 teslimatı bu. Renk, tipografi, ikon seti ve animasyon **kapsam dışı** (15/1);
ayrım yalnızca gri tonu, çerçeve kalınlığı ve boşlukla yapılır. (Durum notu: bölüm 6'da —
M10/M11'de dar bir istisnayla yalnızca kategori ikonlarına ve uygulama simgesine renk eklendi,
geri kalanı hâlâ kapsam dışı.)

**Navigasyon (bölüm 5)**

- Boydan boya alt bar, 3 ikon sekmesi, metin etiketi yok.
- Varsayılan sekme 3 (Portföy).
- Sekme durumu (seçili dönem, açık akordiyonlar) korunur → durum ViewModel'de yaşar.
- **Ekle sekmesi istisnası:** form alanları sıfırlanır, arama kutusu ve sonuç listesi korunur (karar 10).
- Geri tuşu: sekmeler arası geçmiş tutulmaz; 3. sekme dışında geri → 3. sekme, 3. sekmede geri → çıkış (5/7).
- Alt bar gesture navigation alanının üstünde: `WindowInsets.navigationBars` padding'i.

**Ekranlar**

| # | Ekran | İçerik |
|---|---|---|
| W1 | Portföy — dolu | Donut (Compose Canvas `drawArc`), toplam değer, kapalı kategori satırları |
| W2 | Portföy — kategori açık | Akordiyon; yeni sayfa açılmaz (8.3/1). Çoklu açık kategori desteklenir |
| W3 | Portföy — toplam değer grafiği | Grafik ikonuna tıklama, dönem seçici, "son güncelleme" |
| W4 | Portföy — boş durum | Yönlendirme metni + 1. sekmeye giden buton |
| W5 | Performans | Dönem seçici, getiri grafiği (y ekseni **yüzde**), % ve ₺ kâr/zarar |
| W6 | Performans — varlık listesi | Sıralı tablo, %/₺ sıralama değiştirici |
| W7 | Ekle — arama | Anlık arama (≥2 karakter), gruplu sonuçlar, sabit Nakit TL satırı, önbellek fiyatı |
| W8 | Ekle — form | **Alış tarihi (varsayılan bugün)**, alış fiyatı, adet, komisyon, not, toplam maliyet önizlemesi; ABD'de **USD→TL çevirici** |
| W9 | Varlık detayı | Alım kayıtları, ortalama maliyet, güncel fiyat, düzenle ve sil |
| W10 | Ekle — manuel varlık | Kod, ad, kategori, güncel fiyat; "fiyatı otomatik güncellenmez" uyarısı |
| W11 | Elle fiyat girişi | Varlık başına fiyat girme; son girilen fiyat ve tarihi |

**Özellikle dikkat edilecek noktalar**

- W3'teki grafik portföyün **TL değerini**, W5'teki grafik **getiri yüzdesini** gösterir.
  Doküman bunların karıştırılmamasını özellikle yazıyor (8.2/6). İki ayrı composable, iki ayrı
  ViewModel durumu; ortak bir "grafik" bileşeninde birleştirilmez.
- Silme sonrası 5 saniyelik geri alma (4.4/3): kayıt anında silinmez, `Snackbar` süresi boyunca
  bekletilir ve süre dolunca uygulanır. Kullanıcı bu arada uygulamadan çıkarsa silme kesinleşir.
- W7'de fiyat **önbellekten** okunur, arama hiç çağrı yapmaz; seçim anında tek çağrıyla taze fiyat
  çekilir ve W8'e varsayılan olarak taşınır (karar 7).
- W10 ve W11'de "elle girilen fiyat" etiketi her yerde görünür; fiyat 7 günden eskiyse uyarı (karar 9).

**Kabul kriteri:** on bir ekran mock veriyle gezilebiliyor; akış
(Ara → Seç → Tarih, fiyat ve adet gir → Kaydet → 3. sekme) uçtan uca çalışıyor.
**Bu nokta dokümanın Faz 1 kapısıdır — buradan sonrası için wireframe onayı alınmalı.**

---

## M5 — Gerçek kaynaklar

Her kaynak ayrı bir alt iş; biri tıkanırsa diğerleri devam eder. Sıra bağımlılığa göre:

1. **TCMB — USD/TRY.** İki rol ayrı (karar 18): gün içi çevrim **saat başı kur servisi**nden
   (anahtar gerektirmez), geçmiş günlük seri **EVDS `TP.DK.USD.A.YTL`**'den. `evds3.tcmb.gov.tr`
   kullanılır; eski `evds2` adresleri kapandı (9.4/2). Önce bu, çünkü ABD ve emtia buna bağlı.
2. **Finnhub `/quote`** — ABD anlık fiyat. Geçmiş mum uçlarına ücretsiz anahtarla dokunulmaz,
   403 döner (9.4/3).
3. **Twelve Data `/time_series`** — ABD geçmiş seri (anahtar gerekir; demo anahtarla AAPL çalıştı). **Anahtar alınana kadar Yahoo chart ucu yedek değil birincil olarak kullanılabilir** (ABD, BIST, USD/TRY ve ONS geçmişi anahtarsız çalışıyor). ONS geçmişi Yahoo `GC=F` / `SI=F` ile alınır (Twelve Data `XAU/USD` demo anahtarla denenemedi).
   Yedek: Yahoo Finance chart ucu.
4. **Truncgil** — gram altın/gümüş anlık. Yedek: ONS × USD/TRY ÷ 31,1035. Adaptör her zaman
   **gram** fiyatı yazar (karar 12).
5. **TEFAS** — `tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir`. Bot koruması var (9.4/1); istek başlıkları,
   gerekirse oturum çerezi ve hata davranışı doğrulanır. Bu uç fon listesini de sağlar (karar 2).
   **Geçmiş fon fiyatı denendi ve çalışıyor:** `fonKodu` verilince, tek istekte en fazla 1 ay, en fazla 5 yıl geriye. 1 yıllık geçmiş ~13 istek; istekler arası ~10 sn bekleyerek arka planda çekilir. Fon türü (`YAT`/`BYF`/`EMK`...) varlık kaydında tutulur.
6. **BIST — Yahoo Finance chart ucu** (`THYAO.IS`), anlık ve geçmiş. En kırılgan halka (9.4/4);
   günlük tavanı bilinçli olarak düşük (bölüm 2.3).
7. **BIST elle fiyat girişi** — kalıcı yedek, isteğe bağlı değil (9.4/4). W11 üzerinden.

**Sembol listesi kaynakları** (karar 2) — fiyat adaptörlerinden ayrı `SymbolCatalogSource`:

| Kategori | Kaynak | Tazeleme |
|---|---|---|
| Fon | `fonGnlBlgSiraliGetir` kod + ad + fiyatı birlikte döndürür | Günlük fiyatla birlikte |
| Emtia / Nakit | 3 kayıt, pakete gömülü | — |
| ABD | Finnhub `/stock/symbol?exchange=US`, tek çağrı. Yedek: Twelve Data `/stocks` | Ayda 1 |
| BIST | `app/src/main/assets/bist_symbols.json` — **hazır, 537 kayıt** (MIT lisanslı GitHub listesinden üretildi, 2023 tarihli: yeni halka arzlar eksik olabilir → W10 ile manuel eklenir) | Yılda 1, elle |

Liste pakete **gömülü** gelir, ilk açılışta indirilmez (yoksa internetsiz ilk açılışta arama boş döner)
ve tazelemesi çağrı bütçesinin dışındadır.

**Her adaptör için önkoşul:** kaynak canlı denemeyle doğrulanmadan adaptör yazılmaz (16.1'in ilk maddesi).
Deneme çıktıları `docs/kaynak-denemeleri/` altında saklanır — kaynak bozulduğunda karşılaştırma noktası
olur ve fixture testlerini besler.

**Canlı deneme sonuçları (2026-09-21):** [docs/kaynak-denemeleri/2026-09-21.md](docs/kaynak-denemeleri/2026-09-21.md). Bu sonuçlara göre: anahtarsız çalışanlar Yahoo, TEFAS, Truncgil ve TCMB günlük kur; alınması gereken ücretsiz anahtarlar TCMB EVDS, Finnhub ve Twelve Data (üçü de zorunlu değil, Yahoo yedekleri var). Gün içi USD/TRY için Truncgil kullanılır, gram altınla aynı çağrıda gelir.

**Kabul kriteri:** her kaynak için adaptör + gerçek yanıttan üretilmiş fixture testi; yedeğe düşme
senaryosu en az bir kategoride canlı doğrulanmış; Finnhub `/stock/symbol` ve TEFAS geçmiş ucu
denenmiş ve sonucu kayda geçmiş.

---

## M6 — Geçmiş seri, snapshot ve grafikler

**İki yöntem birlikte (11.2/1)**

- Varlık eklendiğinde geçmiş fiyat serisi **bir kez** çekilir, `price_history`'e yazılır; her açılışta
  tekrarlanmaz. Çekim **alış tarihinden** bugüne, en fazla **5 yıl** geriye (kararlar 3, 20).
- Her gün bir `portfolio_snapshot` yazılır.
- Geçmiş seri günlük kapanış değerleriyle tutulur. "1 Gün" grafiği ayrı bir gün-içi (intraday) depoya
  ihtiyaç duymaz: dünün kapanışı `price_history`'den, bugünkü an ise zaten önbellekteki canlı kottan
  (`price_quote`) gelir (`GrafikDeposu`, `PortfolioHistory.portfolioSeries`). Bu yüzden 11.2/4'teki
  "gün içi veri 48 saat sonra silinir" maddesi ayrı bir uygulama gerektirmedi — **farklı bir
  mekanizmayla zaten karşılanıyor**, eksik değil.
  ✅ Uygulandı (M6).
- Hafta sonu ve tatil günleri son işlem gününün değeriyle düzleştirilir (11.2/6). ✅ Uygulandı (M6).
- Saklama sınırları (11.3): varlık başına 5 yıl (günlük iş, `deleteOlderThan`) ✅; snapshot sınırsız ✅;
  portföyden çıkan varlığın serisi 30 gün sonra temizlenir — `TerkedilenSeriTemizleyici`, günlük işe
  bağlı; kaldırılma anı ayrı saklanmaz, serinin son günü doğal olarak eskimesinden anlaşılır (karar 32).
  ✅ Uygulandı.

**Geçmiş seri kaynakları** (karar 6)

| Kategori | Geçmiş seri |
|---|---|
| ABD | Twelve Data `/time_series` |
| BIST | Yahoo chart ucu |
| **USD/TRY** | EVDS, tarih aralıklı. **Varlık başına değil, `asset_id = USDTRY` altında tek sefer** çekilir ve tüm ABD/emtia varlıklarınca paylaşılır; yeni varlıkta yalnızca eksik gün aralığı tamamlanır |
| **Emtia** | Türetilir: ONS geçmişi × USD/TRY geçmişi ÷ 31,1035 |
| **Fon** | TEFAS `fonGnlBlgSiraliGetir`, `fonKodu` ile, 28 günlük parçalar, en fazla 5 yıl (sunucu sınırı, bizimkiyle aynı). Yavaş: 5 yıl ≈ 61 istek ≈ 10 dk. Çekim arka planda, ilerleme göstergesiyle; bitene kadar fon grafiğe kısmi girer |

**USD/TRY geçmişi zorunludur.** ABD varlıklarının geçmiş TL değeri her günün kendi kuruyla hesaplanır;
bugünkü kurla geçmiş USD fiyatlarını çarpmak, TL'nin değer kaybı nedeniyle geçmiş değerleri olduğundan
yüksek gösterir ve getiriyi eksiye çevirir. Fon için uydurma veri üretilmez — maliyet değeriyle düz
çizgi çizmek kullanıcıyı yanıltır.

**WorkManager işleri**

| İş | Zamanlama | Kısıtlar |
|---|---|---|
| Günlük snapshot yazma | **00:30 TSİ, bir önceki günün tarihine** | — |
| Fiyat tazeleme | Bölüm 2'deki ritim | Ağ var, pil düşük değil |
| Sembol listesi tazeleme | Ayda 1, bütçe dışı | Ağ var |
| Saklama temizliği | Günde 1 | — |

Snapshot 00:30'da çünkü günün son fiyatı geç oluşuyor: TEFAS 21:00, ABD kapanışı TSİ 23:00 —
**kış saatinde 00:00**. Daha erken bir snapshot kış aylarında ABD'yi seans ortasında yakalar.
Kural: **tetiklenme anı değil, kapsadığı gün esastır** (karar 14).

**Grafikler**

- Çizgi grafikler Vico ile; basılı tutunca o tarihteki değer ve yüzde baloncuk olarak (7.2).
- Donut Compose Canvas `drawArc` ile — beş dilim için ek bağımlılık gereksiz.
- Portföy yaşı seçilen dönemden kısaysa yalnızca mevcut veri çizilir + "portföy geçmişi X gün" notu (7.1/4).
- Fon içeren portföyün "1 Gün" grafiği basamaklı görünür; beklenen davranış (11.2/7), hata değil.

**Kabul kriteri:** geriye dönük grafik hesaplaması tek bir ABD varlığı üzerinde, **geçmiş kur serisi
kullanılarak**, elle hesaplanmış beklenen değerlerle doğrulanmış (16.1'in son maddesi).

---

## M7 — Kenar durumlar ve çevrimdışı

Doküman bölüm 14'ün tamamı ayrı bir milestone; diğer işlerin arasına serpiştirilmez.

| Durum | Davranış |
|---|---|
| Varlık bulunamadı | Açıklayıcı mesaj + "manuel ekle" → W10 |
| Fiyat çekilemedi | "fiyat alınamadı" işareti, toplama son bilinen fiyatla dahil |
| İnternet yok | Uygulama açılır, son bilinen fiyatlar, üstte "veriler güncel değil" |
| API limiti aşıldı | Yedeğe geç; yoksa bir sonraki tura bekle, **kullanıcıya hata gösterme** |
| Fonun "1 Gün" getirisi %0 | Küçük açıklama notu |
| Piyasa kapalı | Son kapanış + "piyasa kapalı" etiketi |
| Adet veya fiyat ≤ 0 | Kayıt engellenir, alan altında hata metni |
| Gelecek tarihli işlem tarihi | Kayıt engellenir; tarih en fazla bugün |
| Aşırı büyük sayı girişi | Alan başına üst sınır, biçim bozulmaz |
| Portföy boş | Boş durum ekranı (W4) |
| Tek varlıklı portföy | Donut tek dilim %100, hata vermez |
| Getiri hesaplanamayan durum (toplam maliyet = 0) | `—`, sıfıra bölme yok (karar 11) |
| Kur verisi yok, hisse verisi var | Son bilinen kurla çevir + kurun eskiliğini belirt |
| Uygulama uzun süre açılmadı | İlk açılışta eksik günler tamamlanır, snapshot'lar geriye dönük yazılır |
| Elle girilen fiyat 7 günden eski | "fiyat güncel değil" uyarısı (karar 9) |
| Portföy çok büyük, gün içi güncelleme kapalı | Bilgi satırı; açılış/kapanış turları sürüyor (bölüm 2.4) |

**Çevrimdışı çalışma zorunludur** (13/3): uygulama internetsiz açılır ve son bilinen fiyatlarla
**tüm** ekranları çizer. Test cihazında uçak modunda kabul testiyle doğrulanır.

**Kabul kriteri:** tablodaki 16 durumun her biri için ya bir enstrümanlı test ya da kayıtlı bir
manuel kabul adımı var.

---

## M8 — Sertleştirme ve teslim — ✅ Bitti (2026-09-22)

- **Test kapsamı:** `jacoco` Avast'ın SSL araya girmesiyle çakıştığı için (agent indirilemedi,
  `SSLInitializationException: Windows-ROOT not found`) otomatik ölçüm yerine `:core:calc`'in 12
  dosyası elle satır satır tarandı, testlerle çapraz kontrol edildi. Tüm kritik dallar (sınır
  değerler, sıfıra bölme, yuvarlama, dönem hesapları) testli bulundu; eksik kalan birkaç düşük
  değerli dal (örn. `periodReturn`'de negatif gün sayısı — pratikte imkânsız girdi) atlandı.
  291 → günümüzde test sayısı `KARARLAR.md`'de M7 girdisinde kayıtlı.
- **Büyük portföy testi:** emülatörde 126 varlık (5 kategoriye dağıtılmış test verisi) seed edilip
  Portföy ve Performans sekmeleri açıldı, akordeon genişletildi, hızlı kaydırıldı. ANR ya da çökme
  yok; yalnızca sentetik (200 ms) kaydırma sırasında "33 kare atlandı" uyarısı — gerçek kullanıcı
  kaydırmasında sorun teşkil etmez. `AdaptiveInterval`/`TazelemeZamanlayici` için büyük portföy
  davranışı zaten `TazelemeTest.kt`'de testliydi (M7'de `gunIciKapali` eklendi). Test verisi
  temizlendi.
- **Yaz/kış saati:** `PiyasaTakvimiTest.kt`'de üç ayrı test zaten vardı (kış saati, yaz saati,
  sonbahar geçişi) — `ZoneId`'nin kendi DST kuralları kullanıldığı için ek koda gerek kalmadı.
- **Kabul turu:** uçak modu (M7'de doğrulandı), boş portföy, tek varlık %100 dilim, piyasa kapalı
  etiketi — hepsi ya testli ya emülatörde elle doğrulandı.
- **Release APK:** `app/keystore/portfoy-release.jks` (RSA 2048, 30 yıl geçerli, self-signed,
  yalnızca yerelde — `*.jks` gitignore'da) oluşturuldu; şifreler `local.properties`'e eklendi.
  `app/build.gradle.kts`'e `signingConfigs.release` ve `buildTypes.release` (minify + kaynak
  küçültme, ProGuard) eklendi. `app/proguard-rules.pro`: WorkManager worker'ları ve Room/model
  sınıfları için keep kuralları (reflection'la bulunuyorlar). `assembleRelease` başarılı, imza
  `apksigner`le doğrulandı, emülatöre kurulup arama → alım → Room yazma → hesaplama → grafik
  zinciri uçtan uca test edildi; çökme yok.
- Tek hedef Android sürümünde doğrulama; geniş cihaz uyumluluğu kapsam dışı (13/4).

---

## M9 — Güncelleme paketi 1 (kullanıcı geri bildirimi, 2026-09-22) — ✅ Bitti

Üç istek: **(1)** + sekmesi doğrudan aramayla değil kategorilerle açılsın, **(2)** yeni bir **Döviz**
kategorisi olsun (şimdilik USD/TRY ve EUR/TRY), **(3)** Performans sekmesindeki liste kategori
kırılımlı olsun, kategoriye tıklayınca altındaki varlıklar açılsın.

### M9.1 — Döviz kategorisi

- `Category.DOVIZ` eklenir; `UnitType.BIRIM` ile miktar "birim" olarak tutulur, ekranda varlığın
  para birimi yazılır ("1.000,00 USD"). Fiyat = TL karşılığı, yani kurun kendisi.
- `USDTRY` ve `EURTRY` her kurulumda bulunan sabit varlıklardır (gram altın/gümüş gibi); katalog
  indirmesi gerekmez, internetsiz de listede görünürler.
- Fiyat yönlendirmesi mevcut **FX rotası**na bağlanır. Bugüne kadar yalnız USD/TRY'ye bakan kaynaklar
  (Yahoo, Truncgil, TCMB, EVDS) para birimiyle parametrik hâle getirilir:
  `USDTRY=X`/`EURTRY=X`, Truncgil `USD`/`EUR`, TCMB `Kod="USD"`/`Kod="EUR"`,
  EVDS `TP.DK.USD.A.YTL`/`TP.DK.EUR.A.YTL`. `AssetRef.EURTRY` eklenir.
- Geçmiş seri `GecmisAnahtari.KUR` zincirinden (EVDS → Yahoo) gelir. USD/TRY zaten çevrim için
  `FX_USDTRY_ID` altında tek sefer çekiliyor; portföye USD eklenirse **aynı seri kopyalanır**,
  ikinci kez ağdan çekilmez.
- Tazeleme: `RefreshGroup.FX` grubuna bağlanır (09–19 arası saatlik).

### M9.2 — Ekleme akışı: önce kategori

- + sekmesi açılınca klavye değil **kategori listesi** gelir: ABD, BIST, Fon, Emtia, Döviz, Nakit TL.
  Altında "Son eklenenler" kısayolu kalır (sık yapılan iş hızlı olsun).
- Kategoriye girilince arama yalnız o kategoride yapılır. Varlık sayısı azsa (Emtia, Döviz, BIST)
  liste doğrudan gösterilir, aramaya gerek kalmaz; ABD ve Fon'da arama şart (27 bin / 2 bin kayıt).
- Nakit TL'ye basınca ara adım yok, doğrudan alım formu açılır.
- Geri tuşu: form → kategori listesi → sekmenin kendisi.
- Manuel ekleme (W10) girilen kategoriyle açılır; kategori seçimi formda tekrar sorulmaz.

### M9.3 — Performans: kategori kırılımı

- W6 listesi düz varlık listesi yerine **akordeon** olur (Portföy sekmesindeki gibi):
  kategori satırında o kategorinin dönem getirisi (% ve ₺), tıklayınca altında varlıkları.
- Kategori getirisi, varlık yüzdelerinin ortalaması değil; kategorinin varlıkları birlikte
  değerlenip aynı basit Dietz formülüyle hesaplanır. Böylece kategori toplamları portföy
  toplamıyla tutarlı kalır.
- "Sırala: Yüzde / TL" seçimi hem kategorileri hem de içlerindeki varlıkları sıralar.
- `GrafikDeposu.varlikGetirileri` tek geçişte kategori + varlık kırılımı döndürecek şekilde
  genişletilir; veri yalnızca bir kez yüklenir.

---

## M10 — Kategori ikonları — ✅ Bitti (2026-09-22)

Kullanıcı kararı: renkli ama minimalist. Her kategori tek renkli bir Material ikonla eşlendi
(BIST kırmızı ShowChart, ABD lacivert Public/globe, Fon mor Savings, Emtia altın rengi
MonetizationOn, Döviz yeşil AttachMoney). Nakit TL için hazır ikon yeterli görülmedi; sade bir
Türk bayrağı çizimi eklendi (`app/src/main/kotlin/com/portfoy/ui/bilesenler/KategoriIkonlari.kt`):
kırmızı zemin, iki dairenin üst üste binmesiyle oluşan hilal, yıldız tek noktaya indirgenmiş —
gerçek bayrağın ayrıntısı yerine tanınabilir en yalın hâli.

Renkler yalnızca kategori ikonuna özgü bilinçli bir istisna; wireframe temasının geri kalanı
(arka plan, çerçeve, metin) gri tonlarında kalmaya devam eder.

İkon üç yerde kullanılır: Ekle sekmesi kategori listesi ve arama başlığı, Portföy ve Performans
akordeon başlıkları. Emülatörde görsel olarak doğrulandı.

---

## M11 — Uygulama adı ve simgesi — ✅ Bitti (2026-09-22)

Ana ekran adı "Portföy Takip" (`res/values/strings.xml`). Simge: koyu gri zemin üzerinde beyaz
halka + kırmızı dilim — Portföy sekmesindeki dağılım grafiğiyle aynı motif, M10'un "renkli ama
minimalist" ilkesiyle tutarlı. `minSdk=26` olduğu için yalnızca uyarlanabilir simge yeterli
(`mipmap-anydpi-v26`), eski yoğunluk PNG'leri gerekmedi. Ayrıntı: karar 31.

---

## M12 — Bulunan hatalar ve tamamlama turu — ✅ Bitti (2026-09-22)

Kullanıcı geri bildirimiyle: Ekle sekmesinde fiyat/performans yalnızca elle seçilen varlıklarda
görünüyordu (kategori açılışında toplu tazeleme yoktu) → düzeltildi, karar 32. Ayrıca bu turda:

- Fiyatın altında günlük performans yüzdesi eklendi (kaynaklar zaten tek çağrıda veriyor, karar 28).
- ABD'de "öne çıkanlar" (12 büyük şirket) kısayolu eklendi, karar 30.
- İlk gerçek şema değişikliği: `price_quote.changePercent`, migration + test, karar 29.
- Doküman 11.3/3: portföyden çıkan varlığın serisi 30 gün sonra temizleniyor, karar 33.
- Gerçekçi bir örnek portföyle (6 kategori, 6 yıl geriye giden alım) uçtan uca test edildi; bu test
  Performans ekranında büyük yüzdelerde satır taşması hatasını buldu, düzeltildi (karar 34).
- `.aab` derlemesi denendi ve doğrulandı; kod tarafının Play Store'a hazırlığı incelendi
  ([PLAY_STORE_HAZIRLIK.md](PLAY_STORE_HAZIRLIK.md)).
- Derleme uyarıları (deprecated ikon, gereksiz `!!`) temizlendi.

305 test geçiyor. Ayrıntılar: [KARARLAR.md](KARARLAR.md) karar 28–34.

---

## M13 — Kaydırdıkça kademeli fiyat çekimi — ✅ Bitti (2026-09-22)

**Sorun.** Kategori açılışında yalnız ilk 30 varlık tazeleniyor (karar 32). BIST'te 537 hisse var;
kullanıcı aşağı kaydırdığında geri kalanı "—" görüyor. Tamamını açılışta çekmek ise günlük Yahoo
bütçesini (300) tek seferde tüketir ve portföyün kendi tazelemesini günün geri kalanında bozar.

**Çözüm.** Görünen öğeler kaydırıldıkça, **bütçe korumalı** kademeli çekim.

| Konu | Karar |
|---|---|
| Tetikleyici | `LazyListState.layoutInfo.visibleItemsInfo` → `snapshotFlow` ile izlenir |
| Gecikme | Kaydırma durduktan ~400 ms sonra (debounce); kaydırma sırasında istek atılmaz |
| Parti büyüklüğü | Görünen ama fiyatı olmayan/eskimiş en fazla `CallBudget.ROUND_CAP` (30) varlık |
| Tekrarı önleme | `PriceRepository.refresh(minAge)` zaten taze olanı atlar; ayrıca oturum içi "istendi" kümesi tutulur |
| **Gezinme bütçesi** | Ayrı bir günlük sayaç: `GunlukSayac(BROWSE_DAILY_CAP = 120)` — geçmiş seri çekiminde kullanılan sınıfın aynısı. Dolunca kaydırma çekimi o gün için susar; portföyün kendi tazelemesi etkilenmez |
| Bütçe dolduğunda | Sessiz (doküman 14: "API limiti aşıldı → kullanıcıya hata gösterme"); varlık elle seçilince yine tek çağrıyla çekilir |
| Kapsam | Ekle sekmesindeki hem kategori listesi hem arama sonuçları |

**Dokunulacak yerler:** `EkleViewModel` (görünürlük akışı + gezinme sayacı), `EkleEkrani`
(`rememberLazyListState` → ViewModel'e bildirim), `CallBudget` (yeni sabit).

**Test:** gezinme sayacının tükenmesi ve gün dönünce sıfırlanması birim testle; emülatörde BIST'te
kaydırıp fiyatların partiler hâlinde dolduğu görsel olarak.

**Boyut:** S–M.

---

## M14 — Tema: renk, tipografi, ikon, animasyon — ✅ Bitti (2026-09-23)

Faz 1'in bilinçli olarak dışarıda bıraktığı (15/1) dört madde birlikte ele alınır: wireframe gri
düzeninden gerçek bir uygulama görünümüne geçiş. **İlke: minimalizm.** Veri kahraman; renk yalnız
anlam taşıdığı yerde kullanılır, süs için değil.

### M14.1 — Renk sistemi

Üç katmanlı bir palet: **nötr** (yüzeyler ve metin), **semantik** (kazanç/kayıp), **kategori**
(yalnız ikon ve grafik dilimleri). Marka rengi bilinçli olarak mürekkep (koyu lacivert-gri) —
yeşil ve kırmızıyı semantik anlam için serbest bırakır.

| Rol | Açık tema | Koyu tema |
|---|---|---|
| Zemin | `#F6F7F9` | `#0E1116` |
| Yüzey (kart) | `#FFFFFF` | `#161B22` |
| Yüzey varyantı | `#EDEFF3` | `#1F2630` |
| Çerçeve | `#D6DAE0` | `#2C333D` |
| Ana metin | `#131A24` | `#E4E7EB` |
| İkincil metin | `#5C6672` | `#9AA3AE` |
| Birincil (mürekkep) | `#1A2332` | `#E4E7EB` |
| **Kazanç** | `#0E7A55` | `#35C88E` |
| **Kayıp** | `#B3261E` | `#FF6B61` |
| Nötr (sıfır getiri) | ikincil metin rengi | ikincil metin rengi |

Kontrast hedefi WCAG AA (metin için ≥ 4.5:1); seçilen yeşil ve kırmızı bu eşiği kendi zeminlerinde
karşılar (neon tonlardan kaçınılmasının sebebi budur).

### M14.2 — Getiri renkleri (kullanıcının asıl istediği)

Artı getiri yeşil, eksi kırmızı, sıfır nötr. Renk **tek başına** anlam taşımaz: mevcut ▲/▼ işaretleri
korunur (renk körlüğü ve gri tonlamalı ekran görüntüleri için).

- `ui/tema/Renkler.kt`: `@Composable fun getiriRengi(deger: BigDecimal?): Color` — işaret → renk.
- Uygulanacak 10 nokta: Portföy toplam kartı ve varlık satırı, Performans özeti/kategori/varlık
  satırları, Ekle sekmesi günlük değişim yüzdesi.
- Portföy ekranındaki iki satır metni birleştiriyor (`miktar • getiri`); `AnnotatedString` ile
  yalnız getiri kısmı renklenecek.
- Donut grafik dilimleri gri tonları yerine **kategori renklerini** kullanacak (aşağıda).

### M14.3 — Kategori renkleri

Semantik yeşil/kırmızıyla karışmayacak, birbirinden ayırt edilebilir altı ton. M10'da seçilen
BIST kırmızısı ve Döviz yeşili bu yüzden değişiyor.

| Kategori | Açık tema | Koyu tema |
|---|---|---|
| ABD | `#2563EB` mavi | `#60A5FA` |
| BIST | `#EA580C` turuncu | `#FB923C` |
| Fon | `#7C3AED` mor | `#A78BFA` |
| Emtia | `#CA8A04` altın | `#EAB308` |
| Döviz | `#0891B2` camgöbeği | `#22D3EE` |
| Nakit | `#64748B` nötr | `#94A3B8` |

Nakit TL ikonu Türk bayrağı çizimi olarak kalır (kendi doğal renkleriyle — bir rozet değil, resim);
donut dilimi ve legend karesi nötr tonu kullanır.

### M14.4 — Tipografi

Özel yazı tipi dosyası **eklenmez** (APK boyutu ve lisans yükü minimalizme aykırı); sistem yazı tipi
üzerinde bilinçli bir ölçek tanımlanır. Finans uygulamasında asıl kazanç **hizalı rakamlardır**:
sayısal stillerde `fontFeatureSettings = "tnum"` (tabular figures) ile rakamlar sütun hâlinde hizalanır.

| Slot | Kullanım | Boyut / ağırlık |
|---|---|---|
| `displaySmall` | Toplam portföy değeri | 34sp / SemiBold / tnum |
| `headlineSmall` | Dönem getirisi yüzdesi | 24sp / SemiBold / tnum |
| `titleMedium` | Kategori adı | 17sp / Medium |
| `titleSmall` | Varlık kodu | 15sp / Medium |
| `bodyMedium` | Değerler | 15sp / Regular / tnum |
| `bodySmall` | Varlık adı, notlar | 13sp / Regular |
| `labelMedium` | Etiketler ("piyasa kapalı") | 12sp / Medium |

`ui/tema/Tipografi.kt` olarak tanımlanır, `PortfoyTemasi`'na bağlanır.

### M14.5 — İkonlar

Tam bespoke bir set çizilmez (minimalizm + efor); yapılacak olan **tutarlılık**:

- Tek aile: Material Symbols **Outlined**; yalnız alt bardaki seçili sekme dolgulu.
- Tek ölçek: liste içi 20dp, alt bar 24dp.
- Anlamı zayıf iki ikon değişir: Fon için kumbara (`Savings`) yerine `AccountBalance`;
  Emtia için dolar sikkesi (`MonetizationOn`) yerine sade, elle çizilmiş külçe/sikke vektörü
  (Türk bayrağı ikonuyla aynı yaklaşım).
- Mevcut deprecated kullanım kalmadığı doğrulanır.

### M14.6 — Animasyon

Kısa, amaçlı, abartısız. Hepsi 150–250 ms, standart easing.

| Yer | Animasyon |
|---|---|
| Akordeon aç/kapa | `expandVertically` + fade, yaylanmasız spring |
| Sekme geçişi | Fade-through (150 ms) |
| Fiyat tazelenince | Değişen değerde kısa vurgu (kazanç/kayıp renginde 400 ms sönümlenen arka plan) |
| Donut ilk çizim | Sweep animasyonu (500 ms, easeOutCubic) |
| Erişilebilirlik | Sistemde "animasyonları azalt" açıksa tüm süreler 0 |

### Kabul kriteri

- Açık ve koyu temada üç sekmenin de ekran görüntüsü alınır ve gözle doğrulanır.
- Getiri renkleri: artı/eksi/sıfır üçü de gerçek veriyle görülür.
- Metin kontrastı AA eşiğini geçer; ▲/▼ işaretleri korunur.
- Yazı tipi ölçeği %130'a çıkarıldığında taşma olmaz.
- `getiriRengi` için birim test (artı → kazanç, eksi → kayıp, sıfır ve `null` → nötr).

**Boyut:** L (projenin en büyük tek görsel işi).

**Doğrulama sonucu (2026-09-23):** `core:calc` birim testleri (yön/renk eşlemesi dahil) ve
`app:compileDebugKotlin` yeşil. Emülatörde Portföy, Performans ve Ekle sekmeleri hem açık hem koyu
temada elle gezildi: kategori renkleri (ABD mavi, BIST turuncu, Fon mor, Emtia altın, Döviz camgöbeği,
Nakit nötr gri) ve getiri renkleri (artı yeşil, sıfır getiri kartında gözlemlendi) beklendiği gibi.
İkonlar tutarlı (Outlined aile, Fon→`AccountBalance`, Emtia→özel külçe vektörü). Yazı ölçeği %130
testi emülatörde `settings put system font_scale` sistem ayarının yeni pencereye yansımaması nedeniyle
görsel olarak doğrulanamadı (ortam kısıtı); kod tarafı `sp` birimleriyle yazıldığı ve `fontScale`
override'ı olmadığı için doğru ölçeklenmesi bekleniyor — kesin doğrulama gerçek cihazda M15 ile birlikte
yapılacak.

---

## M15 — Cihaz uyumluluğu ve erişilebilirlik turu — ✅ Bitti (2026-09-23)

En sona bırakıldı (kullanıcı: "7'de en son test edebiliriz"). Tema oturduktan sonra anlamlı.

| Kontrol | Nasıl | Sonuç |
|---|---|---|
| Küçük telefon (≈5.4") | `wm size`/`wm density` ile ana emülatörün ekranı küçültüldü, gerçek portföy verisiyle test edildi | ✅ Taşma/kırpılma yok; kategori adları, büyük TL rakamı, kart genişlikleri sığıyor |
| Büyük telefon | Mevcut Pixel profili | ✅ M14'te zaten doğrulandı |
| Tablet (≈10") | `wm size` ile 1600×2560 simüle edildi, gerçek veriyle test edildi | ✅ Taşma yok; ama tek sütun düzeni geniş ekranda yayılıyor, tablete özel düzen yok — bilinçli kapsam dışı (bölüm 6 backlog) |
| Yazı tipi ölçeği | `settings put system font_scale 1.3` | ✅ Doğrulandı — **emülatörde `am force-stop`+`am start` ile değişikliğin yansıması için tam bir cihaz reboot'u gerekli** (bilinen emülatör kısıtı); reboot sonrası kod doğru ölçeklendi. Gerçek cihazda bu adım gerekmez |
| Koyu tema | Tüm ekranlar | ✅ M14'te doğrulandı |
| Yatay yön | Karar verildi: **dikeye kilitlendi** (karar 37) | ✅ `MainActivity` → `android:screenOrientation="portrait"` |
| Erişilebilirlik | TalkBack açılıp `uiautomator` ile erişilebilirlik ağacı incelendi | ✅ Alt bar ("Ekle"/"Performans"/"Portföy") ve donut özeti (tam metin: "Portföy dağılımı: ABD %52,41, ...") düzgün etiketli. ⚠️ Kategori akordeon satırları alt metinleri (ör. "ABD", "288.002,06 ₺") okunuyor ama açık/kapalı durumu (expanded/collapsed) için ayrı bir `stateDescription` yok — backlog'a not düşüldü |

**Boyut:** S.

**Not:** yazı ölçeği testinde emülatöre özgü bir kısıt bulundu ve dokümante edildi; uygulama kodu
standart `sp` birimleri kullanıyor, gerçek cihazda ekstra adım gerektirmeden doğru ölçeklenmesi beklenir.

---

## M16 — UI temizliği (kullanıcı geri bildirimi, 2026-09-26) — ✅ Bitti

Örnek portföyle gerçek kullanımda görülen dört küçük fazlalık kaldırılır.

| # | Değişiklik | Dosya |
|---|---|---|
| 1 | Ekle ekranındaki "Son eklenenler" kısayol listesi tamamen kaldırılır | `EkleEkrani.kt` (`KisayolSatiri` composable'ı ve `EkleViewModel.sonEklenenler` alanı, `depo.observeRecentAssets()` bağlantısı dahil) |
| 2 | Grafik kartından "portföy geçmişi X gün" notu kaldırılır (Portföy ve Performans'ta aynı yapı) | `PortfoyEkrani.kt`, `PerformansEkrani.kt` |
| 3 | Performans kategori satırında "N varlık" alt yazısı yerine kategorinin toplam getiri yüzdesi yazılır (artı→yeşil, eksi→kırmızı, `getiriRengi`) | `PerformansEkrani.kt` |
| 4 | Performans varlık satırındaki uzun ad ("ALPHABET INC-CL A" gibi) kaldırılır, yalnızca kod kalır | `PerformansEkrani.kt` |

**Kabul kriteri:** emülatörde Ekle/Portföy/Performans gezilip dört maddenin de kalktığı, kalan
metinlerin taşmadığı doğrulanır.

**Boyut:** S — veri modeli değişmiyor, yalnızca UI.

---

## M17 — Varlık yönetimi ekranı (kullanıcı geri bildirimi, 2026-09-26) — ✅ Bitti

**Sorun.** Bir varlığa (Portföy'de veya Performans'ta) tıklayınca detay şu an aynı liste içinde
akordeon gibi açılıyor (`VarlikDetayi`, yalnızca Portföy'de var). Kullanıcı bunun yerine **ayrı, tam
ekran bir yönetim sayfası** istiyor — hem Portföy'den hem Performans'tan aynı sayfaya gidilsin.

**Çözüm.** Uygulamada zaten 3 sekme için bir `NavHost` var (`Uygulama.kt`); dördüncü bir rota eklemek
mimariye uyuyor.

### M17.1 — Yeni rota ve ekran
- `varlik/{assetId}` rotası, yeni `VarlikYonetimEkrani` composable'ı.
- Mevcut inline `VarlikDetayi` içeriği (ağırlıklı ortalama maliyet, güncel fiyat, toplam maliyet,
  toplam getiri, "Fiyatı elle güncelle", hareket listesi + düzenle/sil) buraya taşınır.
- Portföy'deki ve Performans'taki varlık satırları artık akordeon açmak yerine bu rotaya
  yönlendirir (`nav.navigate("varlik/${asset.id}")`); geri tuşu kaldığı sekmeye döner.

### M17.2 — "+ Ekle" / "− Azalt"
- **"+ Ekle":** mevcut alım formu (`AlimFormAlanlari`) yeniden kullanılır, yeni bir hareket
  (adet + fiyat + tarih + isteğe bağlı komisyon/not) eklenir. Yeni altyapı gerekmez.
- **"− Azalt":** aynı form, ters yönde — azaltılan adet + o anki fiyat girilir, **ayrı bir azaltma
  hareketi** olarak kaydedilir (var olan alım kayıtlarını değiştirmez; onlar için düzenle/sil zaten var).

### M17.3 — Veri modeli
- `transactions` tablosunda yön bilgisi **zaten vardı** (`TransactionEntity.type`, en baştan
  `TransactionType` olarak tanımlıydı, kod yorumu "Faz 1'de yalnızca alış vardır; satış sonraki
  fazda buraya eklenir" diyordu) — yalnızca enum'a `AZALTMA` değeri eklendi.
- Beklenenin aksine **migration gerekmedi**: Room enum'ları TEXT kolonda ad olarak saklıyor, yeni
  bir enum değeri şemayı değiştirmiyor. M1'de ayrılan "migration testi ilk şema değişikliğinde"
  notu bu turda devreye girmedi; ilk gerçek migration hâlâ ileride bir gün olacak.

### M17.4 — Hesaplama çekirdeği
- `core/calc`'taki ağırlıklı ortalama maliyet hesabı azaltmayı işler: **azaltma ortalama maliyeti
  değiştirmez**, yalnızca kalan adet ve toplam maliyeti orantılı düşürür.
- **Kapsam kararı:** gerçekleşen kâr/zarar bu turda hesaplanmaz/gösterilmez — yalnızca adet ve
  toplam maliyet düşer. Gerçekleşen kâr/zarar istenirse ayrı bir iş olarak bölüm 6'ya eklenir.
- Yeni senaryolar birim testle: azaltma sonrası ortalama maliyet sabit kalıyor mu, adet sıfıra
  inince varlık nasıl davranıyor (portföyden düşer mi, karar gerekebilir).

### M17.5 — Kabul turu (2026-09-26, tamamlandı)
Emülatörde uçtan uca doğrulandı: GOOGL'a 15 adet 6.500 ₺'den alım vardı; "− Azalt" ile 5 adet
20.000 ₺'den azaltıldı → kalan 10 adet, toplam maliyet 97.500→65.000 ₺, **ortalama maliyet 6.500 ₺
sabit kaldı** (beklenen davranış). Hareket listesinde azaltma "−5 × 20.000,00 ₺" olarak kalın
yazıyla ayırt ediliyor. Sil işlemi test edildi: azaltma kaydı silinince adet/maliyet önceki haline
geri döndü. Aynı ekrana hem Portföy'den hem Performans'tan gidildiğinde davranış özdeş. Alt bar bu
ekranda gizleniyor, geri tuşunda kaldığı sekmeye dönüyor.

**Boyut:** L — yeni ekran + hesaplama mantığı değişikliği (M14 seviyesinde); beklenenden küçük
çıktı çünkü migration gerekmedi.

---

## M18 — Tema tercihi: açık/koyu/sistem (kullanıcı geri bildirimi, 2026-09-26) — ✅ Bitti

**Sorun.** M14'te açık/koyu tema `isSystemInDarkTheme()` ile tamamen otomatik — kullanıcının
uygulama içinden elle seçebileceği bir yer yok. Uygulamada hiç "Ayarlar" yüzeyi de yok (alt bar
yalnızca Ekle/Performans/Portföy, M14'te bilinçli 3 ikonla sınırlı tutulmuştu).

**Karar (önerim, aksini söylemezsen bu şekilde ilerlerim):**
- Üç seçenekli tercih: **Sistem (varsayılan) / Açık / Koyu.** Varsayılan "Sistem" seçilerek mevcut
  otomatik davranış hiçbir kullanıcı için değişmeden korunur — yalnızca isteyen elle geçersiz kılar.
- Tam bir "Ayarlar" sekmesi **açılmaz** (3 ikonluk alt bar M14 kararına göre korunur). Bunun yerine
  Portföy ekranının üstüne küçük bir ikon konur (güneş/ay sembolü), tıklanınca üç seçenekli küçük bir
  diyalog açılır. İleride başka tercih eklenirse aynı diyalog büyür — ayrı bir ekrana gerek kalmaz.
- Saklama: `SharedPreferences` (`ArkaPlanIsleri`/`KatalogGuncelleyici`'de zaten kullanılan desenin
  aynısı) — yeni bir DataStore bağımlılığı gerekmez.

### Alt görevler
| # | İş |
|---|---|
| M18.1 | `TemaTercihi` (SİSTEM/AÇIK/KOYU) + `SharedPreferences` okuma/yazma katmanı |
| M18.2 | `PortfoyTemasi`'nda `isSystemInDarkTheme()` yerine tercihe göre karar veren mantık (SİSTEM ise eskisi gibi) |
| M18.3 | Portföy ekranının üstüne küçük ikon + 3 seçenekli diyalog (Sistem/Açık/Koyu, radyo düğmesi) |
| M18.4 | Emülatörde üç seçenek de denenir: Sistem seçiliyken cihaz temasını değiştirince uygulama takip ediyor mu, Açık/Koyu seçiliyken cihaz teması değişince uygulama sabit kalıyor mu |

**Kabul kriteri (doğrulandı, 2026-09-26):** Portföy'de sağ üstteki ikona basılıp "Koyu" seçilince
uygulama **anında** (yeniden başlatmadan) koyu temaya geçti — okuma (`PortfoyTemasi`) ve yazma
(diyalog) farklı ViewModel örnekleri kullansa da ikisi de aynı `TemaTercihiDeposu` singleton'ına
bağlı olduğu için state paylaşımı sorunsuz çalıştı. Uygulama tamamen kapatılıp yeniden açıldığında
tercih ("Koyu") korundu. Ardından "Sistem"e geri alındı, doğrulandı.

**Boyut:** S–M — yeni bir ekran değil, küçük bir tercih + saklama katmanı.

---

## M20 — Kripto kategorisi: yalnızca Bitcoin ve Ethereum (kullanıcı geri bildirimi, 2026-10-08) — ✅ Bitti

**İstek.** Yeni bir kategori: Kripto. Kapsam dar tutuluyor — yalnızca Bitcoin ve Ethereum, geniş bir
kripto borsası taraması yok. `Category` modeli bu türden genişlemeye zaten hazır (M1'den beri:
"ileride kripto/döviz" notu), yalnızca yeni bir dal eklemek yeterli.

**Karar (önerim, aksini söylemezsen bu şekilde ilerlerim):**
- Sabit 2 varlık: Döviz kategorisindeki USD/EUR ile birebir aynı desen (`VarsayilanVarliklar`'da sabit
  kayıt, arama gerekmez — kategori 600 varlık sınırının çok altında olduğu için Ekle ekranı zaten
  doğrudan listeler, özel bir kısayol mekanizması gerekmiyor).
- Fiyat kaynağı: Yahoo Finance (`BTC-USD`, `ETH-USD` — Yahoo'nun `chart` ucu bu sembolleri doğrudan
  destekliyor, ons altın/gümüşte kullanılan "ham sembol" deseniyle aynı yöntem). Başlangıçta **tek
  kaynak, yedeksiz** — ABD/BIST'teki gibi iki kademeli zincir değil. Kripto 24/7 işlem gördüğü için
  piyasa takvimi (açılış/kapanış) kavramı yok; sabit aralıklı tazeleme yeterli.
- Tazeleme: altın/gümüşle aynı mekanik (30 dakikada bir) ama hafta sonu kısıtı olmadan, günün her
  saati — `RefreshGroup.KRIPTO` yeni bir sabit-aralık grubu olarak eklenir.
- TL karşılığı: mevcut USD→TL çevrim mantığı zaten kaynağın para birimine bakıyor
  (`quote.currency == "USD"` ise kurla çarpılıyor) — ABD'ye özel değil, tek değişiklik bu kontrolün
  kur eksikken "atla" güvenliğinin (`fxMissing`) şu an yalnızca `RouteKey.US`'u kapsaması; Kripto da
  eklenmeli.
- Geçmiş seri: ABD'nin yaptığı gibi (USD seri × günlük USD/TRY kuru = TL) — `HistoryRepository`'deki
  dönüşüm ABD'ye özel yazılmış, Kripto için aynı yola eklenir.
- İkon/renk: `Icons.Filled.CurrencyBitcoin` (Material ikon seti içinde hazır var), M14.3 paletine yeni
  bir kategori rengi (ör. amber/turuncu — Bitcoin'in marka rengine yakın ama paletin geri kalanıyla
  tutarlı, mevcut renklerle çakışmayan bir ton seçilecek).

### Alt görevler
| # | İş |
|---|---|
| M20.1 | `core/model`: `Category.KRIPTO` eklenir |
| M20.2 | `VarsayilanVarliklar.kt`: BTC, ETH sabit kayıt olarak eklenir (kod "BTC"/"ETH", `UnitType.ADET`) |
| M20.3 | `network/sources/YahooSource.kt`: `sembol()`'e `Category.KRIPTO -> "${kod}-USD"` dalı |
| M20.4 | `SourceRouter.kt`: `RouteKey.CRYPTO` eklenir, `RouteKey.of()`'a `Category.KRIPTO -> CRYPTO` dalı; DI (`DepoModulu.kt`) rotası Yahoo'ya bağlanır |
| M20.5 | `PriceRepository.kt`: `fxMissing` koruması `RouteKey.US`'un yanına `RouteKey.CRYPTO`'yu da alır (kur yoksa atlanır, hata gösterilmez) |
| M20.6 | `RefreshSchedule.kt`/`TazelemeZamanlayici.kt`: `RefreshGroup.KRIPTO` — hafta sonu kısıtı olmadan 00:00–23:30 arası 30 dakikalık sabit aralık; `kategori()` eşlemesine `KRIPTO -> Category.KRIPTO` |
| M20.7 | `HistoryRouter.kt`: `GecmisAnahtari.KRIPTO`, `getHistory()`'e `Category.KRIPTO -> zincir(GecmisAnahtari.KRIPTO, ...)` dalı; `HistoryRepository.kt`'deki ABD'nin USD×kur dönüşümü Kripto'yu da kapsayacak şekilde genelleşir |
| M20.8 | UI: `KategoriIkonlari.kt` + `Tema.kt` (ikon/renk), `etiket()` ("Kripto"), `EkleEkrani.kt` kategori listesi + `aciklama()` ("Bitcoin, Ethereum") |
| M20.9 | `Category.piyasa` (Portföy ekranındaki takvim uzantısı) — Kripto `null` döner, zaten "piyasa kapalı" kavramı olmayan kategoriler gibi davranır |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-08):** Ekle ekranında Kripto kategorisi pembe Bitcoin
ikonuyla ve "Bitcoin, Ethereum" açıklamasıyla görünüyor; içine girince BTC ve ETH arama yapmadan
doğrudan listeleniyor; BTC'ye dokununca "BTC — Bitcoin • Kripto" başlıklı alım ekranı açılıp fiyat
çekimi doğru rotaya (Yahoo, `BTC-USD`) gidiyor. **Canlı fiyat çekimi bu oturumda doğrulanamadı** —
emülatörde önceden teşhis edilen Avast SSL kesintisi (bu kez Windows host'un tarayıcı güvenini değil,
Android emülatörünün kendi sertifika deposunu etkiliyor) o an TÜM kaynakları (TRUNCGIL, TCMB_DAILY,
YAHOO) aynı `Trust anchor for certification path not found` hatasıyla engelliyordu — Kripto'ya özgü
değil, o anki ağ erişiminin tamamını kapsayan ortam sorunu. Kod yolunun doğru çalıştığı (rota, yeniden
deneme, hata günlüğü) loglardan teyit edildi. Birim testlerin tamamı (yeni `RouteKey`/`Yahoo
sembol`/`RefreshSchedule` testleri dahil) geçti.

**Boyut:** M — yeni bir ekran yok ama fiyat/geçmiş/tazeleme katmanlarının üçüne de dokunuyor.

### Uygulama sırası (her aşamadan sonra derleme/test)
1. **Model + tohum veri** — `Category.KRIPTO`; `VarsayilanVarliklar.kt`'ye BTC/ETH (M20.1–M20.2). Bu
   aşamadan sonra proje derlenir ama kripto henüz fiyat çekmez (kaynak bağlı değil).
2. **Fiyat kaynağı + yönlendirme** — `YahooSource.sembol()`, `RouteKey.CRYPTO`, `DepoModulu.kt`'de rota
   kaydı (M20.3–M20.4). `core/network` testleri (`SourceRouter`/`YahooSource` testleri varsa) çalıştırılır.
3. **Tazeleme** — `RefreshGroup.KRIPTO`, `RefreshSchedule.slots()`, `TazelemeZamanlayici.kategori()`
   (M20.6). `RefreshSchedule`/`TazelemeZamanlayici` testleri çalıştırılır (yeni grup için en az bir test
   eklenir: 24 saat boyunca slot üretiyor mu, hafta sonu dahil mi).
4. **`fxMissing` düzeltmesi** — `PriceRepository.kt` (M20.5); mevcut `PriceRepository` testleri + yeni bir
   "kur yokken Kripto atlanır" testi.
5. **Geçmiş seri** — `GecmisAnahtari.KRIPTO`, `HistoryRouter.kt`, `HistoryRepository.kt`'deki USD×kur
   dönüşümünün genelleşmesi (M20.7).
6. **Arayüz** — ikon/renk/etiket, `EkleEkrani.kt` kategori listesi (M20.8–M20.9).
7. **Tam derleme + emülatör doğrulaması** — Ekle → Kripto → BTC/ETH ekle, fiyatın çekildiğini, Portföy'de
   göründüğünü doğrula. Not: yeni eklenen varlığın geçmiş serisi ilk günlerde boş olabilir (geçmiş
   doldurma `GecmisYoneticisi` arka planda zamanla tamamlar) — bu beklenen davranıştır, hata değildir.
8. Commit.

---

## M21 — Sekme birleştirme: Performans kalkıyor, Portföy detaylandırılıyor (kullanıcı geri bildirimi, 2026-10-08) — ✅ Bitti

**İstek.** İki sekmeli yapı (Performans + Portföy) tek sekmeye iniyor. Kullanıcının ifadesiyle "3.
sekme" = Portföy (yüzdelerin, kategori kırılımının olduğu ana ekran) bundan sonra uygulamanın tek
detay ekranı olacak:
- Performans sekmesi (dönem seçmeli grafik, yüzde/TL sıralama, kategori+varlık bazlı getiri listesi)
  kaldırılıyor; alt barda yalnızca **Ekle** ve **Portföy** kalıyor.
- Performans'taki detaylı grafik kaybolmuyor, **taşınıyor**: Portföy ekranındaki grafik artık satır
  içinde açılıp kapanmıyor (M19'da denenen inline genişleme modeli burada terk ediliyor), bunun
  yerine ayrı bir tam ekrana gidiyor — tıpkı M17'deki varlık yönetimi ekranı gibi, geri tuşuyla
  dönülen bağımsız bir sayfa. Bu yeni ekranın içeriği, Performans sekmesinin tamamı (dönem seçici,
  büyük getiri başlığı, çizgi grafik, Yüzde/TL sıralama, kategori+varlık kırılımı).
- Getiri rakamları **her seviyede** (toplam, kategori, varlık) aynı biçimde ve **dönem seçilebilir**
  şekilde gösterilecek — aşağıdaki tasarım bölümünde ayrıntısı var.
- Kategori satırlarındaki "piyasa kapalı" etiketi kaldırılıyor (`KategoriSatiri`'deki
  `piyasaKapali` kontrolü ve metni).

**Kapsam dışı:** TL/USD para birimi geçişi bu milestone'da değil, **M23**'te ele alınıyor (orada
tetikleyici ayrı bir düğme değil, rakamın kendisi oluyor).

**Karar (önerim, aksini söylemezsen bu şekilde ilerlerim):**
- Yeni rota: `grafik` (NavHost'ta `composable("grafik")`), Portföy ekranındaki grafik ikonuna/satırına
  basınca `nav.navigate("grafik")` ile açılır. Performans ekranının ViewModel'i (dönem/sıralama/açık
  kategori durumu) aynen bu yeni ekrana taşınır — hesaplama mantığında değişiklik yok, yalnızca
  nereden tetiklendiği ve nasıl göründüğü değişiyor (sekme değil, push edilen tam ekran).
- `PortfoyViewModel`'deki eski `grafik`/`degerGrafigiAcik`/`donem` durumu (satır içi TL grafiği için
  vardı) kaldırılır — o grafik artık yeni ekranın parçası, ayrıca tutulmasına gerek kalmıyor.

### Tasarım (kullanıcıyla netleşti, 2026-10-08)

Performans sekmesi kalkınca Portföy ekranı hem "ne kadar param var" (büyüklük) hem "nasıl gidiyor"
(getiri) sorularını tek ekranda cevaplamalı. Bunun için getiri **her seviyede aynı biçimde** ve
**dönem seçilebilir** gösterilir; böylece toplam → kategori → varlık boyunca tek bir okuma alışkanlığı
oluşur.

**Ortak biçim — kâr/zarar her yerde `TL (yüzde)`:**
```
▲ 6,01 ₺ (%0,09)
```
Soldaki rakam kâr/zararın TL karşılığı, parantez içindeki **aynı kârın** yüzdesi — ikisi aynı şeyin
iki birimi. Yön oku ve renk mevcut `getiriRengi` kuralıyla aynı. Bu biçim özet kartında, kategori
satırında ve varlık satırında birebir aynıdır.

**Ortak bileşen — dönem düğmesi:** Küçük, dokununca sırayla değişen bir düğme: **G → H → TÜM → G…**
(günlük / haftalık / tümü). Yanındaki getiri rakamları seçilen döneme göre yeniden yazılır. Her
satırın **kendi seçimi** vardır, biri diğerini etkilemez (ör. ABD günlük, Fon toplam görünebilir).
Üç dönem de önceden hesaplanıp hazır tutulduğu için geçişler anında olur, her dokunuşta yeniden
hesaplama beklenmez.

**1) Özet kartı (Blok 2):**
```
┌────────────────────────────────────────────┐
│  503.263,96 ₺                          📈  │  ← toplam değer + grafik ikonu (sağ üstte)
│  ▲ 61.200,00 ₺ (%12,34)           [ TÜM ]  │  ← getiri: TL solda, yüzde parantezde + dönem düğmesi
│  son güncelleme: 21.00                     │
└────────────────────────────────────────────┘
```
TL ile yüzdenin **yeri değişti**: eskiden yüzde soldaydı, artık TL solda. Grafik ikonu büyük rakamın
sağında, dönem düğmesi getiri satırının sağında — iki kontrol ayrı satırlarda durduğu için birbirine
girmez.

**2) Kategori satırı (`KategoriSatiri`):**
```
[v] [ikon] Fon                        100.000,00 ₺
                        ▲ 15.000,00 ₺ (%15,00)  [ G ]
```
Üstte kategorinin toplam değeri (mevcut), altında aynı `TL (yüzde)` biçiminde kâr/zarar ve kendi
dönem düğmesi. "piyasa kapalı" etiketi kaldırılır.

**3) Varlık satırı (`VarlikSatiri`, akordiyon içi):**
```
AFT                                     6.650,00 ₺
12,5 pay                     ▲ 6,01 ₺ (%0,09)
```
Aynı `TL (yüzde)` biçimi. Varlık satırları **bağlı oldukları kategorinin** dönem seçimini izler —
kategori G'deyse altındaki varlıklar da günlük gösterir. Varlık satırına ayrı düğme konmaz; tutarlılık
kendiliğinden sağlanır ve satır sade kalır.

**Dokunma hedefleri — çakışma notu:** Kategori satırının kendisi zaten dokununca açılıp kapanıyor;
dönem düğmesi bunun içinde ayrı bir dokunma hedefi olacak (Compose'da iç `clickable` olayı tüketir,
satır açılmaz). Bu yüzden kategori/varlık satırlarındaki rakamlara **para birimi için dokunma
eklenmez** — üçüncü bir davranış satırı belirsizleştirir. TL↔USD geçişi (M23) yalnızca özet kartı ve
grafik ekranındaki rakamlarda olur.

### Alt görevler
| # | İş |
|---|---|
| M21.1 | `Uygulama.kt`: `Sekme.PERFORMANS` ve ilgili `NavigationBarItem`/composable kaldırılır; alt bar yalnızca Ekle+Portföy |
| M21.2 | Performans ekranının içeriği yeni `grafik` rotasına taşınır (ör. `ui/grafik/GrafikEkrani.kt`); `onVarlikTikla` davranışı korunur |
| M21.3 | `PortfoyEkrani.kt` Blok 2: eski satır-içi `DegerGrafigi`/`AnimatedVisibility` kaldırılır; grafik ikonu büyük rakamın sağında, `nav` ile yeni ekrana gider |
| M21.4 | Yeni ortak bileşenler: `KarZararYazisi` (`TL (yüzde)` biçimi, renkli, yön oklu) ve `DonemDugmesi` (G/H/TÜM, dokununca sırayla değişir) |
| M21.5 | `PortfoyViewModel.kt`: G/H/TÜM üç dönemin kategori+varlık getirileri önceden hesaplanır (`kategoriGetirileri` üç dönem için); özet kartı ve her kategori satırı için **ayrı** dönem seçimi durumu tutulur |
| M21.6 | Blok 2 getiri satırı: TL sola alınır, yüzde parantez içine girer, sağına dönem düğmesi |
| M21.7 | `KategoriSatiri`: değerin altına `TL (yüzde)` + dönem düğmesi; `piyasaKapali` metni kaldırılır |
| M21.8 | `VarlikSatiri`: `TL (yüzde)` biçimine çevrilir, kategorinin dönem seçimini izler |
| M21.9 | Eski `PerformansViewModel`/`PerformansEkrani` dosyaları temizlenir (taşındıktan sonra tekrar eden kod kalmaz) |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-08):** Alt barda yalnızca 2 sekme (Ekle, Portföy) var;
Portföy'de grafik ikonuna basınca ayrı bir tam ekran ("← Grafik") açılıp geri tuşuyla durum korunarak
dönülüyor; özet kartı, kategori ve varlık satırlarının üçü de `TL (yüzde)` biçiminde (ör. "▲ 10.499,31
₺ (%4,23)"); dönem düğmeleri G/H/TÜM arasında anında geçiyor (önceden hesaplandığı için bekleme yok)
ve her satır kendi seçimini koruyor (ABD "H" iken Emtia "G" kalabiliyor); varlık satırı (GOOGL)
kategorisinin ("H") dönemini doğru izliyor; "piyasa kapalı" hiçbir yerde yok; Nakit'in sıfır getirisi
nötr gri renkte, kayıp (BIST örneğinde) kırmızı ▼ ile doğru gösteriliyor; eski Performans
işlevlerinin (dönem seçimi, sıralama, kategori/varlık kırılımı) hiçbiri kaybolmadan yeni ekranda
çalışıyor; çökme yok.

**Not — bilinçli bir hesaplama değişikliği:** Özet kartındaki "tüm zamanlar" getirisi artık (dönem
seçilebilir olduğu için) G/H ile **aynı yöntemi** kullanıyor: zaman ağırlıklı (Dietz) dönem getirisi
(`GrafikDeposu.hesapla`/`kategoriGetirileri`, Performans sekmesinin hep kullandığı hesap). M21
öncesinde Blok 2'deki "tüm zamanlar" rakamı **farklı bir yöntemle** hesaplanıyordu: basit maliyet-bazlı
getiri (`güncel değer − toplam maliyet`, `PortfolioSummary.returnPercent`, geçmiş seriye hiç
bakmıyordu). İkisi genelde yakın ama **aynı değil** — Dietz, parayı ne zaman yatırdığına göre ağırlıklandırır,
basit hesap yatırım zamanlamasını hiç dikkate almaz. Üç dönem arasında tek, tutarlı bir yöntem olsun
diye (kullanıcının "tam bir sistem" isteğiyle uyumlu) Dietz'i seçtim; `PortfolioSummary.returnPercent`
artık Blok 2'de kullanılmıyor (varlık/kategori satırlarının güncel değeri hâlâ ondan geliyor, yalnızca
getiri rakamı değişti). Farklı görünürse sebebi budur — istenirse TÜM seçiliyken eski basit hesaba
geri dönülebilir.

**Boyut:** L — navigasyon değişikliği + iki ekranın birleşimi + üç seviyede yeni getiri gösterimi.

### Uygulama sırası (her aşamadan sonra derleme/test; önce M20 bitmiş olmalı — Kripto kategorisi kategori satırı listesine zaten dahil olsun diye)
1. **Grafik ekranını taşı** — Performans ekranının dosyalarını (`PerformansEkrani.kt`,
   `PerformansViewModel.kt`) yeni bir `ui/grafik` paketine taşı/yeniden adlandır; davranışta değişiklik
   yok, yalnızca konum ve (gerekirse) isim (M21.2). Derle.
2. **Navigasyon** — `Uygulama.kt`'de `Sekme.PERFORMANS` ve alt bar girişi kaldırılır; `composable("grafik")`
   rotası eklenir (M21.1). Portföy'den `nav.navigate("grafik")` ile geçici bir test düğmesiyle erişimi
   doğrula (henüz eski inline grafik kaldırılmadan, iki yol bir arada kısa süreliğine var olabilir).
3. **Portföy ekranını sadeleştir** — eski satır içi `DegerGrafigi`/`AnimatedVisibility`/`donem` durumu
   `PortfoyViewModel`'den ve `PortfoyEkrani.kt`'den kaldırılır; grafik ikonu artık yalnızca `grafik`
   rotasına gider (M21.3). Derle, test düğmesi kaldırılır.
4. **Ortak bileşenler** — `KarZararYazisi` ve `DonemDugmesi` yazılır (M21.4); önce tek başına, sahte
   veriyle önizlenebilir hâlde. Biçim kararı (`TL (yüzde)`) tek yerde durur, üç seviye de bunu kullanır.
5. **Veri katmanı** — `PortfoyViewModel`'de G/H/TÜM üç dönemin getirileri hesaplanıp hazır tutulur,
   özet kartı ve her kategori için ayrı seçim durumu eklenir (M21.5). Üç dönemin aynı anda
   hesaplanması ölçülür; yavaşlarsa `Dispatchers.Default` üzerinde zaten çalıştığı için ekran
   kilitlenmez ama ilk gösterim gecikebilir — gerekirse TÜM önce, G/H arkadan gelir.
6. **Üç seviyenin arayüzü** — sırayla özet kartı (M21.6), kategori satırı (M21.7), varlık satırı
   (M21.8). Her biri ayrı ayrı emülatörde bakılır; `piyasaKapali` bu adımda kaldırılır.
7. **Temizlik** — artık kullanılmayan eski Performans dosyaları/testleri silinir, kalıntı import/kod
   kalmadığından emin olunur (M21.9).
8. **Tam derleme + emülatör doğrulaması** — yukarıdaki kabul kriterinin tamamı tek tek denenir.
9. Commit.

---

## M22 — Grafik ekranı: tam dönem seti ve kısa etiketler (kullanıcı geri bildirimi, 2026-10-08) — ✅ Bitti

**İstek.** Grafik ekranında 1 gün, 1 hafta, 1 ay, 3 ay, 6 ay, YTD, 1 yıl ve tümü arasında geçiş
yapılabilsin; getiriler hem yüzde hem TL yazsın; etiketler kısaltılsın (1G, 1H, 1A, 3A, 6A…).

**Mevcut durum — işin büyük kısmı zaten hazır.** `Donem` enum'ında dokuz dönem **zaten tanımlı**
(1 Gün, 1 Hafta, 1 Ay, 3 Ay, 6 Ay, YTD, 1 Yıl, 3 Yıl, Tümü) ve hepsinin `baslangic()` hesabı yazılmış
durumda; `DonemSecici` de `Donem.entries`in tamamını yatay kaydırmalı çip olarak gösteriyor. Getiri
hem yüzde hem TL olarak ekranda zaten var. Yani eksik olan tek şey **etiketlerin uzunluğu**: uzun
etiketler yüzünden çipler ekrana sığmıyor, kullanıcı yatay kaydırmadan hepsini göremiyor.

**Karar:** `Donem`'e `kisaEtiket` alanı eklenir (1G, 1H, 1A, 3A, 6A, YTD, 1Y, 3Y, Tümü), `DonemSecici`
bunu kullanır. Kısa etiketlerle çipler büyük ölçüde tek ekrana sığar. **3 Yıl listede kalır** —
kullanıcının saydığı listede yoktu ama zaten çalışıyor ve kısa etiketle fazladan tek çip yer kaplıyor;
çıkarmak için sebep yok.

### Alt görevler
| # | İş |
|---|---|
| M22.1 | `Donem.kisaEtiket` eklenir (uzun `etiket` erişilebilirlik açıklaması olarak kalır) |
| M22.2 | `DonemSecici` kısa etiketi gösterir; dokuz çipin sığdığı doğrulanır (küçük ekran dahil, M15 cihaz profilleriyle) |
| M22.3 | Dokuz dönemin de doğru seri/getiri ürettiği emülatörde denenir (özellikle YTD ve Tümü) |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-08):** Grafik ekranında dokuz dönem kısa etiketlerle
(1G, 1H, 1A, 3A, 6A, YTD, 1Y, 3Y, Tümü) görünüyor, kaydırınca hepsi sığıyor; bir döneme (ör. 1Y)
basınca çip seçili hâle geliyor ve getiri rakamları/grafik güncelleniyor; çökme yok.

**Boyut:** S — hesaplama tarafı hazır, iş yalnızca etiket ve yerleşim.

---

## M23 — Para birimi: rakama dokununca TL ↔ USD (kullanıcı geri bildirimi, 2026-10-08) — ✅ Bitti

**İstek.** Ayrı bir TL/USD düğmesi olmasın; getiri rakamının (ör. `%2,50`) üstüne dokununca sistem
dolara geçsin.

**M19'un geri dönüşü.** M19'da yapılıp geri alınan para birimi katmanı burada yeniden canlanıyor;
tek fark, tetikleyicinin ayrı bir düğme değil **rakamın kendisi** olması (M19'daki sağ üst köşe
düğmesi kullanıcı tarafından istenmedi). Geri alınan kod `24e9cf7` commit'inde duruyor ve oradan
alınabilir: `ParaBirimiTercihi`, `ParaBirimiTercihiDeposu` (SharedPreferences ile kalıcı, M18'deki
tema deseninin aynısı), `tryToUsd()`, `TrFormat.money(birim=)`, `PortfolioData.usdTryRate`.

**Karar:**
- Tercih **genel ve kalıcı**: bir yerde dolara geçilince uygulama genelinde dolar kalır, uygulama
  kapanıp açılınca korunur.
- Dokunma yalnızca **özet kartı** ve **grafik ekranı** rakamlarında. Kategori/varlık satırlarında
  **değil** — o satırlarda zaten aç/kapa ve dönem düğmesi var, üçüncü bir dokunma davranışı
  belirsizlik yaratır (M21 tasarım notu).
- Kur yoksa sessizce TL'de kalınır, hata gösterilmez (M19'daki davranış).
- Dokunulabilirliğin görünürlüğü: rakamın dokunulabilir olduğu dışarıdan belli olmadığı için ilk
  gösterimde küçük bir ipucu gerekebilir (ör. rakamın yanında soluk `₺/$` işareti). Uygulama sırasında
  emülatörde bakılıp karar verilecek.

### Alt görevler
| # | İş |
|---|---|
| M23.1 | `24e9cf7`'den para birimi katmanı geri alınır (`git show 24e9cf7` ile dosya dosya) |
| M23.2 | Özet kartındaki rakamlar dokunulabilir olur; dokunma TL↔USD çevirir |
| M23.3 | Grafik ekranındaki getiri rakamları aynı davranışı alır |
| M23.4 | Dokunulabilirlik ipucu (gerekiyorsa) eklenir |
| M23.5 | Emülatör doğrulaması: geçiş anında oluyor, kalıcı, kur yokken çökmüyor |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-08):** Özet kartındaki büyük değere dokununca anında
TL→USD'ye geçiyor ("503.263,96 ₺" → "10.225,66 USD"), getiri satırı da aynı anda çeviriliyor (yüzde
değişmiyor, yalnızca TL/USD tutarı); kategori/varlık satırları TL'de sabit kalıyor (ABD, Emtia, Nakit
hiç etkilenmiyor); Grafik ekranındaki üst getiri rakamı da aynı tercihi paylaşıyor (aynı
`SharedPreferences`), kategori kırılımı orada da TL kalıyor; uygulama tamamen kapatılıp açıldığında
USD tercihi korundu; tekrar dokununca TL'ye dönüyor; çökme yok.

**Not — dokunulabilirlik ipucu eklenmedi (M23.4 bilinçli olarak atlandı):** Rakamın dokunulabilir
olduğunu gösteren bir işaret (ör. soluk ₺/$ simgesi) koymadım — kullanıcının isteği zaten "sadece
rakam yazıyor, ona dokununca değişsin" şeklindeydi, ekstra bir görsel ipucu bu sadeliğe aykırı
olurdu. Kullanımda rakamı bulmak zor geliyorsa ayrı bir iyileştirme olarak eklenebilir.

**Boyut:** S–M — kodun çoğu `24e9cf7`'de hazır, iş tetikleyiciyi değiştirmek.

---

## M24 — Dağılım grafiği: yüzdeler halkanın kenarında (kullanıcı geri bildirimi, 2026-10-08) — ✅ Bitti

**İstek.** Dağılım kartında kategoriler halkanın altında alt alta listeleniyordu (renkli nokta +
kategori adı + yüzde). Kullanıcı bunun yerine yüzdelerin **grafiğin kendi üzerinde**, her dilimden
çıkan birer kılavuz çizgisinin ucunda görünmesini istedi ("ok çıkarılsın o kısımdan"); alttaki liste
tamamen kalkacak.

**Karar:**
- Alttaki açıklama listesi ve onunla birlikte artık kullanılmayan `DilimIsareti` bileşeni kaldırıldı.
- Etiketler `Canvas` içinde `TextMeasurer`/`drawText` ile çiziliyor: her dilimin orta açısından halka
  kenarına bir nokta, oradan dışarı kısa bir dirsek, oradan etiketin yanına yatay bir kuyruk.
- **Kılavuz çizgisi, çıktığı dilimin kategori rengini taşıyor** (kullanıcı isteği). Böylece kaldırılan
  listedeki renkli noktanın işi çizgiye devredilmiş oluyor: hangi etiketin hangi dilime ait olduğu
  hem çizginin gittiği yerden hem renginden okunuyor.
- **Sayı her zaman halkaya yakın tarafta:** sağdaki etiketlerde önce yüzde (`%51,41 ABD`), soldakilerde
  önce kategori adı (`Emtia %25,94`). Referans görseldeki düzenin mantığı bu; sol sütun sağa, sağ sütun
  sola yaslandığı için sayı iki tarafta da halkaya bakar.
- **Çakışma ayıklaması:** aynı taraftaki etiketler, doğal dikey yerlerinden başlayıp önce yukarıdan
  aşağı, alta taşarlarsa geri yukarı itilerek ayrıştırılıyor (klasik pasta-etiketi algoritması).
- **Sütun dengeleme (ikinci tur düzeltme):** etiketi yalnız bulunduğu yarım daireye göre yerleştirmek
  yetmedi. Dilimler büyükten küçüğe sıralandığı için küçükler hep yan yana geliyor ve altı etiketin
  beşi sol sütuna yığılıyordu; kılavuz çizgileri uzayıp halkanın tepesinde demet hâline geliyordu.
  Artık sütunlar eşitleniyor: taşan taraftan, dikey eksene en yakın (yatayda en az yer kaplayan)
  etiket karşı sütuna geçiyor. 5–1 yerine 3–3 dağılım çıkıyor, çizgiler kısalıyor.
- **Değişken dirsek boyu:** tepeye/dibe yakın dilimlerin kırılma noktası daha dışarıda
  (`dirsek × (1 + 0,9 × (1 − |cos|))`). Bu etiketler yandaki sütuna kadar uzun bir yol kat ettiği
  için, sabit kısa dirsekte çizgi halkanın kenarını sıyırıyordu.
- **Halka boyutu sabit oran değil, ölçülen metin genişliğine göre:** önce etiketler ölçülüp iki yandaki
  en geniş metin bulunuyor, halka geri kalan yere sığdırılıyor. İlk denemede sabit %26'lık bir yan
  boşluk kullanmıştım; "Nakit %9,94" gibi etiketler sığmayıp sol kenara yapışmıştı.
- Erişilebilirlik: görsel liste kalktı ama tüm dağılım grafiğin `contentDescription`'ında yazılı
  kalıyor, TalkBack eskisi gibi hepsini okuyor (M15 ilkesi korundu).

**Kabul kriteri (doğrulandı, emülatör, 2026-10-08):** Altı kategori de halkanın kenarında kılavuz
çizgisiyle görünüyor (Fon %1,32, Döviz %2,93, BIST %8,46, Nakit %9,94, Emtia %25,94 solda; %51,41 ABD
sağda); alttaki liste tamamen kalktı; etiketler kırpılmıyor ve üst üste binmiyor; hem koyu hem açık
temada okunuyor; kart yüksekliği 180 dp'ye indirilerek alttaki ölü boşluk giderildi.

**Boyut:** S–M — tek bir çizim bileşeni, yeni veri ya da hesaplama yok.

---

## M25 — ABD ve Kripto satırları doğal para biriminde: USD (kullanıcı geri bildirimi, 2026-10-08) — ✅ Bitti

**İstek.** Kategori/varlık satırlarında ABD ve Kripto hep TL'de gösteriliyordu (M23'te bilinçli kapsam
dışı bırakılmıştı). Kullanıcı bu iki kategorinin kendi doğal para biriminde — USD — gösterilmesini
istedi: zaten dolar cinsinden fiyatlanıyorlar, TL'ye çevirmek yerine dolar göstermek daha doğru.

**Karar:** M23'teki genel TL/USD tercihinden (özet kartı, kullanıcı dokunuşuyla değişen) tamamen ayrı,
**sabit bir kural**: `Category.ABD` ve `Category.KRIPTO` satırları (kategori toplamı + altındaki her
varlık, hem Portföy hem Grafik ekranında) her zaman USD gösterir — kullanıcı tercihine bağlı değil,
kullanıcı TL'yi seçmiş olsa bile bu iki kategori USD kalır. Kur yoksa (`usdTryRate == null`) mevcut
`cevrilmisTutar()` zaten sessizce TL'ye düşüyor, aynı davranış burada da geçerli.

`usdDogalMi(kategori): Boolean` ortak fonksiyonu `ui/para/ParaBirimiTercihi.kt`'ye eklendi (Portföy ve
Grafik ekranlarının ikisi de kullanıyor, mantık tek yerde duruyor).

### Alt görevler
| # | İş |
|---|---|
| M25.1 | `ui/para/ParaBirimiTercihi.kt`: `usdDogalMi()` eklendi |
| M25.2 | `PortfoyEkrani.kt` `KategoriSatiri`/`VarlikSatiri`: ABD/Kripto için değer ve getiri USD'ye çevrilip gösteriliyor |
| M25.3 | `GrafikEkrani.kt` kategori/varlık kırılımı: aynı kural |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-08):** Portföy'de ABD kategori satırı "5.224,65 USD",
altındaki GOOGL de "5.224,65 USD" (tek varlık olduğu için eşit); getiri satırı da USD
("▼ 19,96 USD (%0,38)"); Emtia/Nakit/BIST/Döviz/Fon TL kalıyor. Grafik ekranının kategori
kırılımında da ABD "201,99 USD" gösteriyor, diğerleri TL. Çökme yok.

**Boyut:** S — mevcut `cevrilmisTutar()` altyapısının (M23) yeniden kullanımı, yeni hesaplama yok.

---

## M26 — USD girişi Kripto'ya da genişletildi, varlık ekranı USD gösteriyor (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**İstek.** İki parça: (1) Kripto kategorisi Portföy'de hâlâ TL gösteriyordu (M25 kodu doğruydu ama
portföyde hiç Kripto varlığı olmadığı için hiç doğrulanmamıştı); (2) "+" ile eklerken hem ABD hem
Kripto için fiyat alanı dolar olarak görünmeli — önceden yalnızca ABD'de, kapalı (opt-in) bir anahtar
vardı.

**M25'in doğrulanması.** Portföye gerçekten bir Kripto varlığı (BTC) eklenip test edildi: kategori
satırı "816,72 USD" gösterdi — kod zaten doğruydu, yalnızca test verisi eksikti. Doğrulamadan sonra
test kaydı silindi, örnek portföy eski hâline döndü.

**Karar:**
- `AlimFormu.kt`: `abd: Boolean` parametresi `usd: Boolean` olarak genelleşti; `AlimFormDurumu`'na
  `usdModu` başlangıç değeri eklendi (artık dışarıdan açık başlatılabiliyor).
- `EkleEkrani.kt`: ABD ve Kripto için form **varsayılan USD modunda** açılıyor (`usdModu = usdDogal`);
  kur güncelleme tetikleyicisi (`kurGuncelle`) de ikisini kapsıyor; önerilen fiyat (canlı çekilen, TL)
  USD moddaysa USD'ye çevrilip o alana yazılıyor.
- `VarlikYonetimEkrani.kt` — önceden gözden kaçmış bir tutarsızlık da düzeltildi: "+ Ekle"/"− Azalt"/
  düzenle diyalogları `abd = false, kur = null` olarak **sabit** kodlanmıştı, yani ABD'de bile USD
  girişi hiç çalışmıyordu. `VarlikYonetimViewModel`'e `kurGuncelle()` eklendi (EkleViewModel'deki
  aynı desen), üç diyalog da artık kategoriye göre USD moduyla açılıyor.
- Aynı ekranın üst özet kısmı (büyük değer, getiri, ağırlıklı maliyet, güncel fiyat, toplam maliyet)
  de ABD/Kripto için USD gösterecek şekilde güncellendi — tutarlılık için (kullanıcı bunu özellikle
  istemedi ama her yer USD'yken bu ekranın TL kalması tutarsız olurdu).
- **Hareketler listesindeki geçmiş kayıtlar bilinçli olarak TL bırakıldı**: o satır "o tarihte gerçekte
  ne ödendiği"nin TL kaydı; bugünün kuruyla USD'ye çevirmek yanıltıcı bir yaklaşık değer üretirdi
  (doğru çevrim için işlem tarihindeki kur gerekir, bu kapsam dışında bırakıldı).

### Alt görevler
| # | İş |
|---|---|
| M26.1 | `AlimFormu.kt`: `abd` → `usd`, `AlimFormDurumu(usdModu=)` başlangıç parametresi |
| M26.2 | `EkleEkrani.kt`: `usdDogal = ABD veya KRIPTO`, form varsayılan USD, önerilen fiyat USD'ye çevriliyor |
| M26.3 | `VarlikYonetimViewModel.kt`: `kurGuncelle()` eklendi |
| M26.4 | `VarlikYonetimEkrani.kt`: üç diyalog (`AlimDuzenleDialog`, iki `HareketDialog`) + `Ozet` composable'ı USD'ye bağlandı |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-09):** Ekle → Kripto → BTC açıldığında "Fiyatı USD
olarak gir" anahtarı **varsayılan açık**, "Alış fiyatı (USD)" canlı fiyatla dolu geliyor; aynı davranış
ABD'de de var. Gerçek bir BTC alımı (0,01 adet) eklenince Portföy'de Kripto satırı "816,72 USD"
gösterdi; varlık ekranına girince "+ Ekle" diyaloğu da varsayılan USD; üst özet (değer, getiri,
ortalama maliyet, güncel fiyat, toplam maliyet) hepsi USD. Test kaydı silindi, çökme yok.

**Boyut:** S — mevcut USD altyapısının (M19/M23/M25) ABD'den Kripto'ya ve ikinci bir ekrana genişlemesi.

---

## M27 — Özet kartının dönemi kategorilere yayılıyor (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**İstek.** Özet kartında G/H/TÜM arasında geçince alttaki kategoriler de otomatik aynı döneme geçsin;
ama kategoriye tek tek dokunmak yine bağımsız çalışmaya devam etsin (mevcut sistem bozulmasın).

**Karar:** `PortfoyDurumu.kategoriDonemi()`'nin varsayılanı sabit `GUNLUK` yerine artık `ozetDonemi`'ni
izliyor — kategori haritada kendi kaydı yoksa özet kartını takip eder. `ozetDonemiDegistir()` özet
kartının dönemini değiştirirken `kategoriDonemleri` haritasını da sıfırlıyor, yani önceden elle
ayrılmış kategoriler dahil **hepsi** yeni döneme döner. Bir kategoriye tek tek dokunmak
`kategoriDonemiDegistir()`'i çağırmaya devam ediyor — bu, haritaya yalnızca o kategori için bir kayıt
düşürüyor ve bir sonraki özet kartı değişikliğine kadar o kategoriyi bağımsızlaştırıyor.

### Alt görevler
| # | İş |
|---|---|
| M27.1 | `kategoriDonemi()` varsayılanı `GUNLUK` → `ozetDonemi` |
| M27.2 | `ozetDonemiDegistir()`: `kategoriDonemleri = emptyMap()` eklendi |

**Kabul kriteri (doğrulandı, emülatör, 2026-10-09):** Uygulama açılışında özet kartı TÜM, tüm
kategoriler de TÜM gösteriyor (senkron başlangıç). Özet kartına basınca (TÜM→G) tüm kategoriler G'ye
geçiyor. Emtia'ya tek başına dokununca yalnızca o H'ye geçiyor, diğerleri G'de kalıyor (bağımsız
çalışma korunmuş). Özet kartına tekrar basınca (G→H) Emtia'nın bağımsız H'si dahil **hepsi** yeniden
senkronize oluyor. Çökme yok.

**Boyut:** XS — iki satırlık değişiklik, yeni hesaplama yok.

---

## M28 — Dağılım grafiği: kılavuz çizgileri artık kesişmiyor (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**Sorun (iki aşamada ortaya çıktı).** M24'teki sütun dengeleme (büyükten küçüğe sıralı dilimlerin 5-1
yerine 3-3 dağılması) bir dilimi doğal yönünün tersi tarafa atadığında, kılavuz çizgisi hâlâ dilimin
**gerçek açısında** dışarı çıkıp doğrudan (tek çapraz çizgiyle) karşı taraftaki metne gidiyordu. Fon ve
Döviz ikisi de üstte, ikisi de sağa atanınca, iki çapraz çizgi birbirini kesiyordu (1. düzeltme: kolu
dilimin kendi açısında tutup yatay/dikey olarak sütuna yönlendirdim). Ama Fon ve Döviz'in açıları
birbirine o kadar yakın (ikisi de tepeye yakın) ki kolun "dilimin doğal yüksekliği" hâlâ neredeyse
aynı çıkıyor, iki paralel çizgi görsel olarak tek çizgiymiş gibi üst üste biniyordu — kullanıcı bunu
ikinci turda fark etti.

**Son karar:** Kolun yüksekliği artık dilimin **kendi açısı** değil, çakışmayı önlemek için zaten
ayrıştırılmış **[merkezY]** (metnin düştüğü son satır). Fon ve Döviz'in açıları ne kadar yakın olursa
olsun, satırları (dolayısıyla kolları) her zaman en az bir metin yüksekliği + boşluk kadar ayrık —
iki kol asla aynı satıra düşmez, çizgiler net şekilde ayrı iki paralel hat olarak görünür. Kol artık
tam radyal değil (dilimin gerçek açısından metnin satırına doğru hafif eğik) ama bu fark gözle fark
edilmeyecek kadar küçük ve pasta grafiği kütüphanelerinde (ör. Highcharts) standart bir teknik.

**Kabul kriteri (doğrulandı, emülatör, 2026-10-09):** Fon ve Döviz'in çizgileri artık **net ayrı iki**
paralel hat olarak gidiyor — ne kesişiyor ne üst üste biniyor. Hem koyu hem açık temada doğrulandı.
Çökme yok.

**Boyut:** XS — tek bir çizim bloğu, geometri düzeltmesi (iki turda tamamlandı).

---

## M29 — Dağılım grafiği yerleşimi: saf fonksiyonlara çıkarılıp birim testlendi (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**Soru.** M24/M28'de dağılım grafiğinin kenar etiketleri birkaç turda düzeltildi (sütun dengeleme,
çakışma giderme, kılavuz çizgisi yönlendirmesi) ama her seferinde yalnızca **o anki** örnek portföyün
(6 kategori, belirli yüzdeler) ekran görüntüsüyle doğrulandı. Kullanıcı haklı bir soru sordu: "büyüklükler
değiştiğinde yine doğru olacak mı emin miyiz?" — başka bir deyişle, bu güvence tek bir veri noktasına mı
dayanıyor, yoksa genel olarak mı doğru?

**Karar.** Yerleşim matematiğinin üç parçası (orta açı hesabı, sağ/sol sütun dengeleme, dikey çakışma
giderme) `Canvas`/`TextMeasurer`'dan bağımsız, saf Kotlin fonksiyonları olarak `core/calc/DonutYerlesimi.kt`'ye
çıkarıldı; `Grafikler.kt` artık kendi kopyasını tutmuyor, doğrudan bunları çağırıyor — yani test edilen
kod ile ekranda çalışan kod **aynı**, paralel bir yeniden-uygulama değil. 17 birim testi şunları
kapsıyor: bugüne kadarki gerçek regresyon senaryosu (ekrandaki tam dağılım, 3-3 dengeyi ve Fon/Döviz'in
ayrıştığını doğrudan sabitler), aşırı eşitsiz dağılım (70/10/8/6/4/2), Kripto eklenince 7 kategori, tek
kategori, iki kategoride doğal simetri, birbirine neredeyse özdeş açılı 10 dilim (döngü güvenle
sonlanıyor mu), taşma durumunda bile minimum aralığın korunduğu, ve dokuz farklı dağılımı tek seferde
tarayan bir "büyüklükler değişince de fark hep ≤1 kalır" testi.

**Kabul kriteri (doğrulandı, 2026-10-09):** `./gradlew :core:calc:test` 17/17 yeşil. `Grafikler.kt`
çakışma/sütun kodunu tamamen bu fonksiyonlara devretti, derleme temiz. Emülatörde (koyu tema) görsel
sonuç refactor öncesiyle birebir aynı — davranış değişmedi, yalnızca artık regresyon testiyle kilitli.
Çökme yok.

**Boyut:** S — davranış değişikliği yok, mevcut mantığın çıkarılması + test.

---

## M30 — Ekle listesinde ABD/Kripto fiyatları USD, Nakit TL kalın çerçevesi kaldırıldı (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**Sorun (iki parça).** (1) M26 yalnızca Ekle formundaki (bir varlığa dokununca açılan) fiyat alanını
USD'ye bağlamıştı; kategori açılınca gelen **liste** (ör. "AAPL 16.797 TL") hâlâ TL gösteriyordu —
kullanıcı bunu daha önce istemişti ama liste gözden kaçmıştı. (2) Ekle ekranındaki kategori listesinde
"Nakit TL" satırı kalın beyaz çerçeveyle diğerlerinden ayrı duruyordu; kullanıcı hepsinin aynı
görünmesini istedi.

**Karar:**
- `EkleViewModel.kategoriSec()`: kategori açılınca kur da çekiliyor (`depo.latestUsdTry()`) — önceden
  yalnızca bir varlık seçilince (`sec()`) çekiliyordu, liste aşamasında `kur` hep `null` kalıyordu.
- `EkleEkrani.kt` `SonucSatiri`: `usdDogalMi(kategori)` true ise (ABD, Kripto) fiyat `cevrilmisTutar()`
  ile USD'ye çevriliyor; "öne çıkanlar" ve arama sonuçları aynı bileşeni kullandığı için tek değişiklik
  ikisini de kapsıyor.
- `KategoriGorunumu`: Nakit TL satırındaki `kalinCerceve = kategori == Category.NAKIT` kaldırıldı,
  artık diğer altı kategoriyle birebir aynı görünüyor.

**Kabul kriteri (doğrulandı, emülatör, 2026-10-09):** ABD listesinde AAPL "337,42 USD", MSFT/GOOGL/AMZN
aynı şekilde USD; Kripto listesinde BTC "82.474,92 USD", ETH "2.490,50 USD". Bir varlığa dokununca
açılan form ile liste artık **aynı** fiyatı gösteriyor (önceden liste TL, form USD gösterip
tutarsızlık yaratıyordu). Nakit TL satırı diğerleriyle aynı, kalın çerçeve yok. Çökme yok (TRUNCGIL
kaynağının bilinen, yedekli JSON ayrıştırma uyarısı dışında).

**Boyut:** XS — mevcut USD altyapısının (M23/M25/M26) bir ekran daha kapsaması + bir stil kaldırma.

---

## M31 — Bugün alınan varlıkta ana ekran kâr/zararı "0,00 (—)" gösteriyordu (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**Sorun.** Kullanıcı bir BTC alımı yaptıktan sonra Portföy ekranındaki Kripto satırı "H" (haftalık)
dönemde "0,00 USD (—)" gösterdi; aynı anda varlık detay ekranı doğru değeri ("▼%0,51 ▼0,10 USD")
gösteriyordu. İlk şüphe geçmiş fiyat dolumunun (arka plan `GecmisYoneticisi.tamamla()`) henüz
bitmemiş olmasıydı (kullanıcı da "kapatıp açınca güncellendi" diyerek bunu işaret etti), ama bu bir
kırmızı balık çıktı.

**Gerçek kök neden.** `PortfolioHistory.portfolioSeries()` her gün için bir `DailyPoint` üretir, ama
yalnızca o gün toplam maliyet > 0 olan günlerde — alım tarihinden önceki günler hiç üretilmez. Bugün
alınan bir varlık için bu, dönem penceresi içinde **tam olarak tek** gün üretilmesi demek (geçmiş
fiyat serisi ne kadar dolu olursa olsun). `GrafikDeposu`'nun `kazanc()`/`yuzde()` fonksiyonları ise
`noktalar.size >= 2` gerektiriyordu (basit Dietz: dönem başı vs dönem sonu karşılaştırması) — tek
noktalı seride anlamlı bir "başı/sonu" yok, fonksiyonlar `null`/`ZERO` döndürüyordu. Bu üç seviyede de
aynı fonksiyonlar kullanıldığından (toplam, kategori, varlık) sorun hepsini aynı anda etkiliyordu.

**Karar:**
- `kazanc()`/`yuzde()`'ye `noktalar.size == 1` özel durumu eklendi: bu durumda basit Dietz yerine
  tek günün kendi içindeki değer-maliyet farkına bakılıyor (`returnPercent()`, varlık detay
  ekranındaki anlık getiriyle **aynı** yöntem, aynı sayı).
- İlk denemede (`yukle()`'de senkronik mum ekleme) iki mevcut test kırıldı ve yeni regresyon testi
  bile geçmedi — gerçek kısıt `portfolioSeries()`'in gün-bazlı yapısındaydı, seri ekleme değil. Bu
  deneme tamamen geri alındı; düzeltme doğrudan `kazanc()`/`yuzde()`'ye taşındı.

**Kabul kriteri (doğrulandı):** Yeni birim testi (`GecmisVeGrafikTest`, M31) — canlı fiyatı olan ama
geçmiş serisi henüz dolmamış bugün alınan varlıkta toplam getiri anında hesaplanıyor (%10,00,
10,00 TL). Tüm test paketi `BUILD SUCCESSFUL`. Emülatörde gerçek BTC alımı ile doğrulandı: Kripto
satırı kayıttan **hemen sonra** "▲3,92 USD (%0,48)" gösterdi (bekleme yok), TÜM/G/H dönem
düğmeleri arasında tutarlı kaldı. Bu doğrulama sırasında Yahoo geçmiş veri çekimi SSL hatasıyla
tamamen başarısız oldu, ama düzeltme yine de doğru çalıştı — arka plan dolumu hiç bitmese de sorun
yaşanmıyor. Çökme yok.

**Boyut:** S — iki saf fonksiyonda özel durum + bir regresyon testi; kök neden analizi (iki yanlış
hipotez elendi) asıl zaman alan kısımdı.

---

## M32 — "Veriler güncel değil" uyarısı ve tek günlük grafik mesajı yumuşatıldı (kullanıcı geri bildirimi, 2026-10-09) — ✅ Bitti

**Sorun (iki parça).** (1) Ana ekranda "son güncelleme: SS:DD" satırının altında, herhangi bir fiyat
sorunu varsa kalın "veriler güncel değil" satırı da çıkıyordu; kullanıcı buna gerek olmadığını,
saatin zaten yeterli olduğunu belirtti. (2) Grafik ekranında bugün alınan bir varlık için "Grafik
için en az iki günlük veri gerekir." metni bir hata/sorun gibi görünüyordu — oysa bu M31'de
açıklanan yapısal bir durum (tek günlük Dietz serisinden çizgi çizilemez) ve üstteki anlık getiri
rakamı zaten doğru.

**Karar:**
- `PortfoyEkrani.kt` `GuncellemeBilgisi()`: `eski` bayrağı ve bağlı "veriler güncel değil" `Text`
  bloğu tamamen kaldırıldı; artık yalnızca son güncelleme saati gösteriliyor. Kullanılmayan `ekran`
  parametresi de kaldırıldı.
- `GrafikEkrani.kt`: tek günlük durumdaki mesaj "Bu dönemde henüz tek günlük veri var; grafik
  yarından sonra çizilmeye başlar. Getiri yukarıda güncel." olarak değiştirildi — bir sorun değil,
  beklenen bir durum olarak anlatılıyor.

**Kabul kriteri:** Tüm test paketi (`core:calc`, `core:data`, `app`) `BUILD SUCCESSFUL`. Release APK
derlendi, emülatöre kuruldu, UI akışları (Ekle formu, Kripto listesi, Portföy boş durumu) çökmeden
çalıştı. Bu oturumda emülatörün ağ katmanında SSL sertifika doğrulama sorunu vardı (sanal ortamın
kendi ağ kısıtlaması — TRUNCGIL/TCMB canlı fiyat ve kur çekimi başarısız oldu), bu yüzden gerçek
veriyle uçtan uca görsel doğrulama bu oturumda yapılamadı; kullanıcının fiziksel telefonunda (gerçek
ağ bağlantısıyla) daha önce doğrulanan veri çekimi bu değişiklikten etkilenmedi (yalnızca UI metni
değişti, veri çekim kodu dokunulmadı).

**Boyut:** XS — iki ekranda metin/koşul değişikliği.

---

## 3. Risk kaydı

| Risk | Etki | Önlem |
|---|---|---|
| **BIST ücretsiz kaynağı** — Yahoo uçları resmî değil, sık bozulur, kullanım şartlarını ihlal edebilir | BIST fiyatları tamamen kesilir | W11 elle fiyat girişi M5'te zorunlu teslim; BIST adaptörü izole; günlük tavan bilinçli olarak düşük |
| **TEFAS bot koruması** — site Nisan 2026'da baştan yazıldı | Fon fiyatı ve fon listesi gelmez | Yedek sarmalayıcı baştan tanımlanır; kaynak denemesi M5 öncesi |
| **TEFAS geçmiş ucu bulunamaz** | Fonlar kurulum öncesi grafiğe giremez | Karar 6: bu kabul edilir, grafiğe not düşülür, uydurma veri üretilmez |
| **TCMB EVDS anahtar gecikmesi** | Geçmiş kur serisi çekilemez | M0'da başvuru; gün içi çevrim anahtarsız saat başı servisle zaten çalışır |
| **Finnhub geçmiş veri 403** | ABD grafikleri boş | Planlı: anlık Finnhub, geçmiş Twelve Data |
| **Çağrı bütçesi** | Büyük portföyde veri eksik kalır | Bölüm 2.4'teki adaptif aralık; en kötü durumda gün içi kapanır, açılış/kapanış korunur |
| **Dönemsel getiri karmaşıklığı** | Yanlış getiri = uygulamanın temel vaadi çöker | M2'de saf fonksiyon + 7 senaryo testi; UI'dan önce doğrulanır |
| **Yaz/kış saati geçişi** | Yılda iki kez, birkaç hafta yanlış saatte çekim | Karar 17: saatler `ZoneId` ile, TSİ hiçbir yere sabit yazılmaz; M8'de geçiş tarihi testi |
| **Faz 1 kapsam kayması** | Takvim sapar | Doküman 15'in kapsam kuralı: yazılmayan her şey kapsam dışı |

---

## 4. Gereksinim dokümanına işlenecek değişiklikler

Plan 27 karar alıyor; hepsi gerekçeleriyle [KARARLAR.md](KARARLAR.md)'de. Dokümanda değişen bölümler:

| Doküman bölümü | Değişiklik |
|---|---|
| 1 — Karar tablosu | "Alış tarihi" satırı: formda sorulur, varsayılan bugün |
| 3 — Varlık türleri | Emtia kodları `XAUGR` / `XAGGR`; **Döviz** kategorisi eklendi (M9, `USDTRY`/`EURTRY`) |
| 4.1 — Arama | Sembol listesi kaynakları; sonuç fiyatı önbellekten |
| 4.2 — Ekleme formu | Alış tarihi alanı; ABD'de USD→TL çevirici |
| 5.5 / 6.5 — Form durumu | Çelişki giderildi: form sıfırlanır, arama korunur |
| 7.3 — Performans listesi | Silinen varlık listeden çıkar |
| 8.2 / 9.2 — Son güncelleme | Bugün değilse tarih de yazılır |
| 9.1 — Kaynaklar | Sembol listesi satırları; geçmiş seri satırları; EVDS rol ayrımı; toplu çekim kuralı |
| 9.2 — Tazeleme ve piyasa saatleri | Piyasa olayına bağlı ritim; iki tatil takvimi; saatler zaman dilimiyle |
| 9.3 — Çağrı bütçesi | Kaynak bazlı günlük tavanlar; adaptif aralık |
| 10.4 — Dönemsel getiri | Basit Dietz formülü |
| 10.6 — Alış tarihi | Kullanıcıdan istenir |
| 11.2 — Geçmiş seri | USD/TRY paylaşımlı serisi; emtia türetimi; fon kısıtı; snapshot saati |
| 11.3 — Saklama | Geçmiş seri çekimi 5 yılla sınırlı |
| 14 — Kenar durumlar | "Toplam maliyet 0" satırı düzeltildi; manuel varlık yaşam döngüsü; iki yeni satır |
| 16 — Ekran listesi | W10 ve W11 eklendi, toplam 11 ekran |
| 4.1 — Arama | M9: + sekmesi doğrudan aramayla değil kategoriden başlar; arama seçilen kategoriyle sınırlanır |
| 7.3 — Performans listesi | M9: liste kategori kırılımlı (akordeon), kategori getirisi ayrı hesaplanır |
| — (yeni) | M10: kategori satırlarında renkli, tek tip minimalist ikon |

---

## 5. Çalışma sırası (yalnızca sıra, tarih taahhüdü değil)

```
1. adım  M0 + EVDS anahtar başvurusu  →  M1
2. adım  M2 (hesaplama çekirdeği + testler)
3. adım  M3 (adaptör katmanı + tazeleme zamanlayıcısı, fake kaynakla)
4-5. adım  M4 (W1–W11 mock veriyle)  ──►  WIREFRAME ONAY KAPISI
6. adım  Kaynakların canlı denenmesi  →  M5 adaptörleri
7. adım  M5 devam  →  M6 (geçmiş seri, snapshot, grafikler)
8. adım  M6 devam  →  M7 (kenar durumlar, çevrimdışı)
9. adım  M8 (sertleştirme, APK, kabul turu)
10. adım  M9–M12 (kullanıcı geri bildirimiyle sonradan eklendi, teslimden sonra)
```

Wireframe onay kapısı dokümanın Faz 1 teslimatıyla örtüşür: o noktaya kadar üretilen her şey —
hesaplama çekirdeği, Room şeması, adaptör arayüzü, tazeleme zamanlayıcısı — tasarım kararları
değişse de ayakta kalır.

**Durum (2026-09-22):** M0–M12 tamamlandı; uygulama kişisel kullanım için hazır (release derlemesi
sıfırdan kurulup doğrulandı). Kullanıcı isteğiyle **M13–M15 planlandı**: kaydırdıkça fiyat çekimi,
tema (getiri renkleri dahil) ve cihaz uyumluluğu turu. Sıra: M13 → M14 → M15.

---

## 6. Gelecek planlar (backlog)

M13–M15 artık **planlandı** (yukarıdaki bölümler) — bu tabloda yalnızca hâlâ kapsam dışı olanlar kalır.

| Madde | Ne zaman gündeme gelir | Not |
|---|---|---|
| **Play Store yayını** | Kullanıcı karar verirse ("belki hiç olmayabilir") | Kod tarafı incelendi, `.aab` derlemesi doğrulandı. En büyük engel: API anahtarlarının istemcide açık olması (proxy sunucu gerektirir). Ayrıntı: [PLAY_STORE_HAZIRLIK.md](PLAY_STORE_HAZIRLIK.md). |
| **Özel yazı tipi (ör. Inter)** | Sistem yazı tipi yetersiz görülürse | M14.4 bilinçli olarak sistem yazı tipinde kalıyor (APK boyutu + lisans). Marka kimliği istenirse ayrı bir adım olarak eklenebilir. |
| **Tam bespoke ikon seti** | Marka kimliği çalışması yapılırsa | M14.5 tutarlılıkla yetiniyor (tek aile + iki özel çizim). Her ikonun elle çizilmesi ayrı bir tasarım işi. |
| **Widget / ana ekran kısayolu** | İstenirse | Hiç konuşulmadı, fikir olarak burada durur: portföy toplamını ana ekranda gösteren bir widget. |
| **Tablete özel düzen** | Tablet kullanımı gerçek ihtiyaç olursa | M15'te tablet boyutunda taşma yok ama tek sütun düzeni geniş ekranı optimize kullanmıyor (ör. iki sütunlu düzen). |
| **Akordeon açık/kapalı erişilebilirlik durumu** | Erişilebilirlik önceliklenirse | M15'te TalkBack testinde görüldü: kategori satırları içerik olarak okunuyor ama "genişletildi/daraltıldı" durumu ayrıca anons edilmiyor (`stateDescription` eksik). |
| **Gerçekleşen kâr/zarar (azaltma/satıştan)** | İstenirse | M17.4'te bilinçli olarak kapsam dışı bırakıldı: "azaltma" hareketi adet/toplam maliyeti düşürür ama satıştan doğan kâr/zararı ayrıca hesaplayıp göstermez. |

Yeni bir istek ya da fikir geldiğinde buraya eklenir; hayata geçirildiğinde ilgili milestone'a taşınır.
