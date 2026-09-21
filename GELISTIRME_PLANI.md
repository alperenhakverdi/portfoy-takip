# Portföy Takip Uygulaması — Geliştirme Planı

Kaynak: *Portföy Takip Uygulaması — Gereksinim Dokümanı (Faz 1)*, 2026-09-20.
Durum: **son hâli.** Dokümandaki çelişki ve eksikler karara bağlandı; kararlar [KARARLAR.md](KARARLAR.md)'de.

## İlerleme (2026-09-22)

| # | Milestone | Durum | Not |
|---|---|---|---|
| M0 | İskelet | ✅ Bitti | Gradle, 5 modül, Compose + Hilt |
| M1 | Room veritabanı | ✅ Bitti | 5 tablo, 16+ DAO testi. Migration testi ilk şema değişikliğinde |
| M2 | Hesaplama çekirdeği | ✅ Bitti | Getiri, Dietz, dağılım, biçim, doğrulama: 59 test |
| M3 | Kaynak katmanı | ✅ Bitti | Yönlendirici, bütçe, takvim, zamanlayıcı, fiyat deposu |
| M4 | Ekranlar (W1–W11) | ✅ Bitti | Emülatörde elle gezildi; grafik verileri hâlâ **örnek** (M6) |
| M5 | Gerçek kaynaklar | 🟡 Büyük kısmı bitti | 7 adaptör, 89 fixture testi, 8 canlı test geçti, uygulamaya bağlı. Kalan: aşağıya bak |
| M6 | ✅ Geçmiş seriler, snapshot, gerçek grafikler ve getiri hesabı (emülatörde doğrulandı) |
| M7 | Kenar durumlar, çevrimdışı | ⏳ | |
| M8 | Sertleştirme, APK | ⏳ | |

**M5'te kalanlar:** zamanlayıcının (piyasa saati, açılış/kapanış turu, adaptif aralık) tazelemeye bağlanması ve
WorkManager işleri (M6 ile birlikte); şu an tazeleme uygulama öne gelince ve aşağı çekince çalışıyor.

**Geliştirme ortamı notu:** bu bilgisayardaki Avast antivirüsü HTTPS trafiğini kendi sertifikasıyla yeniden imzalıyor.
Java araçları için `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` gerekiyor; emülatörde ise Avast'ın
kök sertifikası test emülatörünün kullanıcı deposuna kuruldu ve **yalnızca debug** derlemesi kullanıcı sertifikalarına
güveniyor (`app/src/debug`). Yayın sürümü etkilenmez. Gerçek telefonda bu gerekmez.

**Ölçülen performans (emülatör, yavaş):** ilk açılışta 30 bin kayıtlık katalog yüklemesi 85 sn → 31–38 sn'ye indi
(hazır SQL ifadesi + 500'lük kısa işlemler). Ekran yükleme sırasında kilitlenmiyor. Gerçek telefonda birkaç saniye beklenir.

Plan, dokümandaki eksik ya da
kendi içinde çelişen noktaları 20 kararla kapatır (bazıları kapsamı genişletir); kararların gereksinim dokümanına geri işlenmesi gerekir.

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
| M6 | ✅ Geçmiş seriler, snapshot, gerçek grafikler ve getiri hesabı (emülatörde doğrulandı) |
| M7 | Kenar durumlar ve çevrimdışı | Bölüm 14'ün tamamı | M |
| M8 | Sertleştirme ve teslim | Test kapsamı, APK, kabul testi | S |

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
ayrım yalnızca gri tonu, çerçeve kalınlığı ve boşlukla yapılır.

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
- Geçmiş seri günlük kapanış değerleriyle tutulur; gün içi veri yalnızca "1 Gün" grafiği için saklanır
  ve 48 saat sonra silinir (11.2/4).
- Hafta sonu ve tatil günleri son işlem gününün değeriyle düzleştirilir (11.2/6).
- Saklama sınırları (11.3): varlık başına 5 yıl; snapshot sınırsız; portföyden çıkan varlığın serisi
  30 gün sonra temizlenir.

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

## M8 — Sertleştirme ve teslim

- `:core:calc` kapsamı ≥ %90; repository ve ViewModel testleri Turbine ile.
- Büyük portföy (100+ varlık) ile adaptif aralık davranışı ve liste performansı ölçümü.
- Uçak modu, piyasa kapalı, boş portföy, tek varlık senaryolarıyla el ile kabul turu.
- Yaz/kış saati geçiş tarihlerinde piyasa saati hesabının doğrulanması.
- Release APK imzalanıp doğrudan kurulum (Play Store yayını sonraki fazın işi).
- Tek hedef Android sürümünde doğrulama; geniş cihaz uyumluluğu kapsam dışı (13/4).

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

Plan 20 karar alıyor; hepsi gerekçeleriyle [KARARLAR.md](KARARLAR.md)'de. Dokümanda değişen bölümler:

| Doküman bölümü | Değişiklik |
|---|---|
| 1 — Karar tablosu | "Alış tarihi" satırı: formda sorulur, varsayılan bugün |
| 3 — Varlık türleri | Emtia kodları `XAUGR` / `XAGGR` |
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
```

Wireframe onay kapısı dokümanın Faz 1 teslimatıyla örtüşür: o noktaya kadar üretilen her şey —
hesaplama çekirdeği, Room şeması, adaptör arayüzü, tazeleme zamanlayıcısı — tasarım kararları
değişse de ayakta kalır.
