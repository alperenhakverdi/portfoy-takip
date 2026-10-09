# Portföy Takip

Kişisel yatırım portföyünü (BIST hisseleri, ABD hisseleri, TEFAS fonları, altın/gümüş, döviz,
kripto para ve nakit TL) tek ekrandan takip etmek için yazılmış bir Android uygulaması. Kotlin ve
Jetpack Compose ile geliştirildi; fiyatlar ücretsiz halka açık kaynaklardan çekilir, veriler cihazda
(Room veritabanında) saklanır — sunucu veya hesap yok.

## Özellikler

- **Kategori bazlı portföy özeti:** BIST, ABD, Fon, Emtia, Döviz, Kripto, Nakit TL — her biri kendi
  doğal para biriminde gösterilir (ör. ABD ve Kripto varlıkları USD).
- **Dönemsel getiri:** Günlük, haftalık, aylık ve daha uzun dönemler için basit Dietz yöntemiyle
  hesaplanan yüzde ve TL bazlı kâr/zarar; toplam, kategori ve varlık seviyesinde.
- **Grafik ekranı:** Portföyün seçilen dönemdeki değer serisi, dokuz farklı zaman aralığı.
- **Dağılım grafiği:** Kategorilere göre halka (donut) grafiği, kılavuz çizgili etiketler.
- **Varlık yönetimi:** Alım/azaltma işlemleri, elle fiyat girişi (otomatik kaynağı olmayan varlıklar
  için), işlem geçmişi.
- **Çevrimdışı çalışma:** İnternet yokken son bilinen fiyatlarla devam eder.
- **Açık/koyu/sistem teması**, büyük portföylerde adaptif arka plan güncelleme sıklığı.

## Mimari ve teknoloji

- **Dil / UI:** Kotlin, Jetpack Compose, Material 3
- **DI:** Hilt
- **Veritabanı:** Room (yerel, cihaz üzerinde)
- **Eşzamanlılık:** Kotlin Coroutines + Flow
- **Arka plan işleri:** WorkManager (periyodik fiyat tazeleme)
- **Modüller:**
  - `app` — UI (Compose ekranları, ViewModel'ler)
  - `core:model` — paylaşılan veri modelleri
  - `core:calc` — saf hesaplama fonksiyonları (getiri, Dietz, dağılım yerleşimi, biçimlendirme) —
    bağımsız test edilebilir, Android'e bağımlı değil
  - `core:network` — dış veri kaynakları için HTTP istemcileri
  - `core:data` — Room veritabanı, repository katmanı

Hesaplama mantığı (`core:calc`) Android çalışma zamanından tamamen izole: tüm getiri/dağılım
fonksiyonları saf fonksiyonlardır ve JVM birim testleriyle doğrulanır.

## Veri kaynakları

Uygulama şu ücretsiz/halka açık kaynakları kullanır (hepsi en iyi çaba ile, resmi SLA'sı yok):

| Kategori | Kaynak |
|---|---|
| BIST | Yahoo Finance (resmi olmayan uç) |
| ABD hisseleri | Finnhub (anlık), Twelve Data (geçmiş seri) |
| TEFAS fonları | TEFAS web sitesi |
| Döviz kuru | TCMB EVDS, saat başı yedek servis |
| Altın/gümüş | TruncGil |
| Kripto (BTC, ETH) | Yahoo Finance |

Bu kaynaklardan biri geçici olarak çalışmazsa uygulama son bilinen fiyatla devam eder ve bunu
belirtir; veri uydurulmaz.

## Kurulum (geliştirici)

1. Android Studio (Narwhal veya üstü) ile projeyi açın, Gradle senkronizasyonunun bitmesini bekleyin.
2. Proje kökünde `local.properties` dosyası oluşturun (bu dosya `.gitignore`'da, repoya girmez):

   ```properties
   sdk.dir=/path/to/Android/Sdk

   EVDS_API_KEY=...
   FINNHUB_API_KEY=...
   TWELVEDATA_API_KEY=...
   ```

   - `EVDS_API_KEY`: [TCMB EVDS](https://evds2.tcmb.gov.tr/) — ücretsiz, anlık başvuru.
   - `FINNHUB_API_KEY`: [finnhub.io](https://finnhub.io/) — ücretsiz katman yeterli.
   - `TWELVEDATA_API_KEY`: [twelvedata.com](https://twelvedata.com/) — ücretsiz katman yeterli.
   - Anahtarsız da derlenir/çalışır; yalnızca ilgili kategorilerin fiyat çekimi devre dışı kalır.

3. Debug derlemesi için imzalama anahtarı gerekmez: `./gradlew assembleDebug`.
4. Release (imzalı) derleme isterseniz `local.properties`'e kendi keystore bilgilerinizi ekleyin:

   ```properties
   RELEASE_STORE_FILE=keystore/benim-anahtarim.jks
   RELEASE_STORE_PASSWORD=...
   RELEASE_KEY_ALIAS=...
   RELEASE_KEY_PASSWORD=...
   ```

   Sonra: `./gradlew assembleRelease`. `RELEASE_STORE_FILE` `app/` dizinine göre çözümlenir.

## Testler

```
./gradlew test
```

`core:calc` ve `core:data` modüllerindeki saf fonksiyon ve repository testleri çalışır
(`core:data` testleri Robolectric ile JVM üzerinde Room'u taklit eder).

## Durum

Kişisel kullanım için geliştirilen bir Faz 1 projesi; aktif olarak güncelleniyor. Geliştirme
günlüğü ve alınan kararlar için [GELISTIRME_PLANI.md](GELISTIRME_PLANI.md) ve
[KARARLAR.md](KARARLAR.md) dosyalarına bakabilirsiniz.

## Lisans

Henüz bir lisans belirlenmedi — şimdilik tüm hakları saklıdır. Kullanmak/katkıda bulunmak
isterseniz önce issue açıp sorun.
