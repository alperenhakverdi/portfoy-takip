# Play Store Yayın Hazırlığı

Kullanıcı isteğiyle hazırlandı (2026-09-22): "ileride Play Store'da yayınlayabilirim, o altyapıya
uygun mu kontrol et". Yayın şu an **yapılmıyor** — kullanıcının kendi kararı ("4 tamam yayından önce
bakarız şu an sadece kendim için kullanacağım", bkz. [KARARLAR.md](KARARLAR.md) karar 26). Bu belge,
ne zaman yayınlanmak istenirse diye kod tarafında hazır olan ve olmayanları kaydeder.

## Özet

| Konu | Durum |
|---|---|
| İmzalı derleme | ✅ Hazır |
| Android App Bundle (`.aab`) | ✅ Doğrulandı (bu oturumda test edildi) |
| ProGuard/R8 | ✅ Hazır (M8) |
| İzinler | ✅ Minimal, sorun yok |
| Hedef SDK sürümü | 🟡 Şu an uygun, yayın anında tekrar kontrol edilmeli |
| **API anahtarları** | 🔴 **En büyük engel** — APK/AAB içinde açık, decompile edilebilir |
| Gizlilik politikası | 🔴 Yazılmadı (Play Console zorunlu tutuyor) |
| Veri Güvenliği formu | 🔴 Doldurulmadı |
| İçerik derecelendirme anketi | 🔴 Doldurulmadı |
| Mağaza görselleri (ikon, ekran görüntüsü, tanıtım grafiği) | 🔴 Yok |
| Google Play Geliştirici Hesabı | 🔴 Yok (tek seferlik 25 USD) |
| applicationId benzersizliği | 🟡 Kontrol edilmedi (Play Console'da doğrulanır) |

---

## 1. Kod tarafında hazır olanlar

- **İmzalama:** `app/keystore/portfoy-release.jks` (self-signed, RSA 2048, 30 yıl) Play App
  Signing'de **upload key** olarak doğrudan kullanılabilir — Google kendi imza anahtarıyla yeniden
  imzalayıp dağıtır, mevcut keystore'u değiştirmeye gerek yok.
- **Android App Bundle:** `./gradlew :app:bundleRelease` bu oturumda denendi, imzalı 5,2 MB'lık
  `.aab` sorunsuz üretildi. Play Store 2021'den beri yeni uygulamalarda yalnızca `.aab` kabul ediyor
  (düz `.apk` değil); altyapı buna hazır.
- **Küçültme:** `minifyEnabled` + `shrinkResources` açık, ProGuard kuralları (`app/proguard-rules.pro`)
  WorkManager worker'larını ve Room/model sınıflarını koruyor (M8'de test edildi).
- **İzinler:** yalnızca `INTERNET` ve `ACCESS_NETWORK_STATE` — Play Console'da ek açıklama
  gerektirmeyen, düşük riskli bir izin seti.
- **Kararlılık:** bu oturumda 8 varlıklı gerçekçi bir örnek portföy ve 126 varlıklı büyük bir
  portföyle test edildi; çökme ya da ANR yok.

## 2. Kod tarafında yapılması gerekenler

### 2.1. API anahtarları — en kritik konu

EVDS, Finnhub ve Twelve Data anahtarları `local.properties` → `BuildConfig` alanları üzerinden
derlemeye gömülüyor (`app/build.gradle.kts`). Bu, kişisel kullanımda sorun değil ama **herkese açık
bir Play Store yayınında** APK/AAB decompile edilerek anahtarlar çıkarılabilir — üçüncü kişiler bu
anahtarlarla kotayı tüketebilir ya da sağlayıcının kullanım şartlarını ihlal edebilir.

**Seçenekler (öncelik sırasıyla):**
1. **Küçük bir proxy sunucu** (ör. Cloudflare Workers, Vercel Edge Function — ücretsiz katmanları
   yeterli): uygulama anahtarları değil, kendi proxy adresini çağırır; anahtar yalnızca sunucuda durur.
   En temiz çözüm, orta büyüklükte bir iş (yeni bir modül gerekmez, yalnızca `core/network`'teki
   `baseUrl` parametreleri proxy adresine çevrilir).
2. **Anahtar kısıtlama:** Finnhub/Twelve Data/EVDS'nin Android paket adı ya da imza bazlı kısıtlama
   desteği olup olmadığı kontrol edilmeli (Google Maps API'deki gibi). Bu oturumda doğrulanmadı;
   büyük ihtimalle desteklenmiyor (çoğu ücretsiz finans API'si bu tür kısıtlamayı sunmuyor).
3. **Yalnız iç test (Internal Testing):** Play Console'un "Dahili test" kanalı mağaza incelemesi
   gerektirmez ve link yalnızca davet edilenlerle paylaşılır — herkese açık dağıtım riskini taşımadan
   kendi cihazları arasında kullanmak isteniyorsa bu yeterli olabilir, proxy gerekmez.

### 2.2. `applicationId` benzersizliği

`com.portfoy` şu anki id. Play Console'da bir id yalnızca bir kez kullanılabilir ve **ilk yayından
sonra değiştirilemez**. Yayın öncesi Play Console'da (ya da `https://play.google.com/store/apps/details?id=com.portfoy`
adresini ziyaret ederek) bu id'nin boş olduğu doğrulanmalı; çakışma riskini azaltmak için
`com.<kullanıcı-adı>.portfoytakip` gibi daha özgün bir id de düşünülebilir.

### 2.3. Hedef SDK sürümü

`targetSdk = 36`. Play Store, yeni gönderimlerde **son Android sürümünün en fazla bir yıl gerisini**
zorunlu kılan hareketli bir kural uyguluyor. Bu oturumda (2026-09-22) 36 muhtemelen yeterli ama
yayın anında Play Console'un güncel şartı tekrar kontrol edilmeli (kural her yıl güncelleniyor).

---

## 3. Kod dışı, Play Console'da yapılması gerekenler

Bunlar kod değişikliği değil, Play Console'da doldurulacak formlar / hazırlanacak metin ve görseller:

- **Google Play Geliştirici Hesabı:** tek seferlik 25 USD kayıt ücreti, ilk adım.
- **Gizlilik politikası:** URL olarak sunulması zorunlu. Bu uygulama sunucuya kullanıcı verisi
  göndermiyor (yalnızca piyasa fiyatı çekiyor, tüm portföy verisi cihazda kalıyor) — kısa, tek
  sayfalık bir metin yeterli. Basit bir statik sayfa (ör. GitHub Pages, ya da bu depodaki bir
  Artifact) olarak yayınlanabilir.
- **Veri Güvenliği (Data Safety) formu:** "Hangi veriler toplanıyor, paylaşılıyor mu" sorularına
  dürüst yanıt: uygulama kişisel veri toplamıyor/iletmiyor; yalnızca genel piyasa fiyatı için ağ
  isteği yapıyor. Hesap/kullanıcı sistemi olmadığı için "hesap silme" politikası da gerekmiyor —
  Play Console formunda bu açıkça belirtilebilir.
- **İçerik derecelendirme anketi (IARC):** finans takip uygulaması, saldırgan içerik yok →
  büyük ihtimalle "Herkes" / "3+" kategorisine düşer.
- **Finans kategorisi ek politikaları:** Play Store'un Finans kategorisi için ek açıklama isteyebilir
  ("bu uygulama yatırım tavsiyesi vermez, yalnızca kullanıcının kendi girdiği verileri takip eder,
  gerçek para hareketi yapmaz"). Mağaza açıklamasına böyle bir cümle eklenmesi önerilir.
- **Mağaza görselleri:**
  - 512×512 yüksek çözünürlüklü ikon (uygulama içindeki uyarlanabilir ikondan farklı, ayrı bir PNG).
  - Öne çıkan görsel (feature graphic), 1024×500.
  - En az 2 telefon ekran görüntüsü (bu oturumda emülatörden alınan ekran görüntüleri başlangıç
    noktası olabilir, ama mağaza için düzenlenmiş/çerçeveli olanı tercih edilir).
  - Kısa açıklama (≤80 karakter), tam açıklama (≤4000 karakter).
  - Bunların hepsi tasarım/metin işi; kod değişikliği gerektirmez, istenirse ayrı bir oturumda
    hazırlanabilir.
- **Sürüm kanalı önerisi:** doğrudan "Production"a değil, önce **Internal Testing** (inceleme
  gerektirmez, 100 test kullanıcısına kadar) ile başlanması önerilir — hem API anahtarı riskini
  hem de ilk yayın sürecinin karmaşıklığını azaltır.
- **`versionCode`/`versionName` stratejisi:** şu an `1` / `"0.1.0"`. Her Play Console gönderiminde
  `versionCode` kesinlikle artmalı; basit bir kural (ör. her yayın +1) yeterli, otomasyon şart değil.

---

## 4. Öneri sırası (yayına karar verilirse)

1. API anahtarı sorununu çöz (proxy ya da Internal Testing ile sınırlı kalma kararı).
2. `applicationId` benzersizliğini Play Console'da doğrula.
3. Google Play Geliştirici Hesabı aç.
4. Gizlilik politikası sayfasını yayınla (kısa, tek sayfa yeterli).
5. Mağaza görsellerini hazırla (ikon, ekran görüntüleri, açıklamalar).
6. Internal Testing kanalına `.aab` yükle, Veri Güvenliği ve içerik derecelendirme formlarını doldur.
7. Kendi cihazlarında test et, sorun yoksa Production'a geçişi değerlendir.

Bu adımların hiçbiri şu an yapılmadı — yalnızca kod tarafının hazır olduğu doğrulandı ve yol haritası
çıkarıldı, kullanıcının isteğiyle uyumlu ("4 tamam yayından önce bakarız").
