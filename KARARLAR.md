# Kararlar — Gereksinim Dokümanına İşlenecek Değişiklikler

Kaynak: *Portföy Takip Uygulaması — Gereksinim Dokümanı (Faz 1)*, 2026-09-20.
Durum: **tüm maddeler karara bağlandı.** Açık madde kalmadı.

Bu dosya, dokümanın taranmasında çıkan çelişki ve eksiklerin nasıl kapatıldığını kaydeder.
Doküman bölüm 15'in kapsam kuralı gereği bunların gereksinim dokümanına işlenmesi gerekiyor;
sohbet içi mutabakat kapsam sayılmaz.

---

## Özet

| # | Konu | Karar | Dokümanda değişen |
|---|---|---|---|
| 1 | Dönemsel getiri formülü | Basit Dietz | 10.4 |
| 2 | Varlık/sembol listesi kaynağı | TEFAS bedava, ABD Finnhub, BIST gömülü JSON | 4.1, 9.1 |
| 3 | Alış tarihi | Formda sorulur, varsayılan bugün, en fazla 5 yıl geriye | 1. bölüm tablosu, 4.2, 10.6 |
| 4 | Eksik ekranlar | W10 + W11 eklendi, toplam 11 ekran | 16 |
| 5 | Tazeleme ritmi | Piyasa olayına bağlı + adaptif gün içi aralık | 9.2, 9.3 |
| 6 | Geçmiş seri kaynakları | USD/TRY zorunlu ve paylaşımlı; emtia türetilir; fon kısıtlı | 9.1, 11.2 |
| 7 | Arama sonucunda fiyat | Önbellekten; seçimde tek çağrı | 4.1 |
| 8 | ABD alış fiyatı | Forma USD→TL çevirici yardımcı | 4.2 |
| 9 | Manuel varlık yaşam döngüsü | `MANUEL` kaynak, elle güncelleme, seri oluşturur | 14 |
| 10 | Form durumu çelişkisi | Form sıfırlanır, arama korunur | 5.5 / 6.5 |
| 11 | "Toplam maliyet 0" satırı | Yeniden yazıldı | 14 |
| 12 | `XAU`/`XAG` birim karmaşası | `XAUGR` / `XAGGR` | 3 |
| 13 | "Son güncelleme" tarihi | Bugün değilse `GG.AA SS:DD` | 8.2, 9.2 |
| 14 | Snapshot saati | 00:30 TSİ, bir önceki günün tarihine | 11.2 |
| 15 | Tatil takvimi | İki takvim: `US_MARKET`, `TR_MARKET` | 9.2 |
| 16 | "Toplu çekim" kuralı | Çağıran taraf için toplu, adaptör içinde bölünebilir | 9.1 |
| 17 | Piyasa saatleri | Kendi zaman diliminde tanımlanır, TSİ sabit yazılmaz | 9.2 |
| 18 | EVDS / saat başı kur | Yedek değil, farklı işler | 9.1 |
| 19 | Silinen varlık | Performans listesinden tamamen çıkar | 7.3 |
| 20 | 5 yıllık saklama sınırı | Geçmiş seri çekimi 5 yılla sınırlı | 11.3 |

---

## 1. Dönemsel getiri: Basit Dietz

**Sorun.** 10.4/2 "dönem içi alım, dönem başı değere eklenerek baz düzeltilir" diyordu — katkının hangi
gün yapıldığına bakmadan. Portföyü 100.000 ₺ olan, ay %10 kazanmışken son gün 100.000 ₺ yatıran
kullanıcıda getiri `(210.000 − 200.000) / 200.000 = %5` çıkıyordu. Kullanıcı hiçbir şey kaybetmemişti.

**Karar.** Katkı, dönemde kaldığı gün oranınca paydaya girer:

```
                V_bitiş − V_başlangıç − ΣC
Getiri % = ─────────────────────────────────────────── × 100
            V_başlangıç + Σ(C_i × kalan_gün_i / dönem_gün)
```

Aynı örnek: payda `100.000 + 100.000×(1/30) = 103.333`, getiri **%9,68**.

| Katkı ne zaman | Ağırlık | Sonuç |
|---|---|---|
| Dönem başında | 1,00 | %5,00 — para tüm dönem piyasada; 10.000 TL kazanç 200.000 TL üzerinden hesaplanır |
| Ortasında | 0,50 | ~%6,67 |
| Son gün | 0,03 | %9,68 — geç giren para neredeyse hiç seyreltmez |
| Katkı yok | — | %10,00 (para eklenmemiş, 110.000 TL bitişte) |

Ek veri gerekmiyor; katkı tarihleri `transaction.islem_tarihi`'nde, dönem başı değer snapshot'ta.
Payda ≤ 0 ise `null` → ekranda `—`. 10.4/4'teki "maliyet bazlı yöntem zaman ağırlıklıdan sapar"
kabulü geçerliliğini koruyor; bu düzeltme o sapmayı değil, katkı zamanının yok sayılmasını gideriyor.

---

## 2. Varlık/sembol listesinin kaynağı

**Sorun.** 9.1 yalnızca **fiyat** kaynaklarını karara bağlamıştı. Fiyat kaynağı `getQuote("NVDA")`
şeklinde çalışır — kodu zaten bildiğini varsayar. Kullanıcı "Nvidia" yazdığında gereken
"Nvidia → NVDA, ABD" eşlemesi hiçbir fiyat API'sinden gelmez. 4.1/6 ayrıca listenin
**internetsiz** çalışmasını şart koşuyor.

**Karar.**

| Kategori | Liste kaynağı | Tazeleme |
|---|---|---|
| Fon | `fonGnlBlgSiraliGetir` kod + ad + fiyatı birlikte döndürür | Günlük fiyatla birlikte |
| Emtia / Nakit | 3 kayıt, pakete gömülü | — |
| ABD | Finnhub `/stock/symbol?exchange=US`, tek çağrıda tüm liste | Ayda 1 |
| BIST | Ücretsiz toplu liste yok → `assets/bist_symbols.json` (~600 kayıt) | Yılda 1, elle |

1. Liste pakete **gömülü** gelir; ilk açılışta indirilmez, yoksa internetsiz ilk açılışta arama boş döner.
2. Tazeleme çağrı bütçesinin **dışındadır**.
3. Ayrı adaptör: `SymbolCatalogSource`. Finnhub ucunun ücretsiz anahtarla çalıştığı M5 öncesi
   denemeyle doğrulanır; yedek Twelve Data `/stocks`.
4. Gömülü BIST listesinde olmayan yeni halka arz, W10 (manuel varlık) yoluyla eklenir.

---

## 3. Alış tarihi kullanıcıdan istenir

**Sorun.** Bölüm 11 "grafikler dolu görünsün" diye geriye dönük hesaplamayı seçmişti, ama 10.6/1
alış tarihini "eklendiği tarih" sayıyor, 11.2/5 de "alış tarihinden önce çizilmez" diyordu.
İkisi birlikte: bugün kuran kullanıcının hiçbir grafiği geriye çizilmiyordu.

**Karar.** W8 formuna **alış tarihi** alanı eklenir; zorunlu, varsayılan **bugün**, ileri tarih
engellenir (bölüm 14). Geçmiş seri çekimi alış tarihinden bugüne, **en fazla 5 yıl** geriye yapılır.
Daha eski tarih girilirse kayıt kabul edilir, maliyet ve toplam getiri doğru hesaplanır
(bunlar seri gerektirmez), grafik 5 yıl öncesinden başlar ve not düşülür.

---

## 4. Eksik ekranlar: W10 ve W11

**Sorun.** Doküman iki akışı zorunlu kılıyordu ama ekranları W1–W9'da yoktu: bölüm 14'ün
"manuel ekle" seçeneği ve 9.4/4'ün "BIST için **kalıcı yedek** elle fiyat girişi".
Faz 1 teslimatı wireframe olduğu için çizilmeyen ekran tasarlanmamış demektir.

**Karar.**

| # | Ekran | İçerik |
|---|---|---|
| W10 | Ekle — manuel varlık | Kod, ad, kategori, güncel fiyat; "fiyatı otomatik güncellenmez" uyarısı. W7'deki "bulunamadı" mesajından açılır |
| W11 | Elle fiyat girişi | Varlık başına fiyat girme; son girilen fiyat ve tarihi. W9'dan ve "veriler güncel değil" uyarısından erişilir |

Faz 1'de çizilecek ekran sayısı **dokuzdan on bire** çıkar.

---

## 5. Tazeleme ritmi: piyasa olayına bağlı + adaptif gün içi aralık

**Sorun.** Doküman her varlık türü için sabit "15 dakika" yazıyordu. ABD için bu 6,5 saatlik seansta
26 tur demek. USD/TRY için büsbütün anlamsız: TCMB saat başı yayınlıyor.

**Karar.** Anlık canlı veri hedeflenmez. Öncelik sırası: **kapanış > açılış > gün içi.**

Ayrıntılı tasarım geliştirme planının 2. bölümündedir. Özet:

| Varlık türü | Açılış turu | Gün içi | Kapanış turu |
|---|---|---|---|
| ABD hisseleri | Seans + 2 dk | Adaptif, varsayılan 30 dk | Seans sonrası 15 dk |
| BIST hisseleri | Seans + 2 dk | Adaptif, varsayılan 30 dk | Seans sonrası 15 dk |
| TEFAS fonları | — | — | 21:15, günde 1 |
| Altın / gümüş | — | 30 dk, Pzt–Cum | — |
| USD/TRY | — | Saat başı | — |
| Nakit TL | — | — | — |

Gün içi aralık portföy büyüklüğüne göre kendiliğinden uzar; bütçe yine de yetmezse gün içi turlar
tamamen kapanır ve yalnızca açılış/kapanış turları yapılır. Pull-to-refresh her zaman açıktır (1/dk).

---

## 6. Geçmiş seri kaynakları

**Sorun.** 9.1 geçmiş seriyi yalnızca ABD (Twelve Data) ve BIST (Yahoo) için karara bağlamıştı.
Bölüm 11'in geriye dönük hesaplaması ise dört kategoriyi de gerektiriyor. Özellikle **USD/TRY
geçmişi hiç şart koşulmamıştı** — oysa ABD varlıklarının geçmiş TL değeri her günün kendi kuruyla
hesaplanmak zorunda; bugünkü kurla geçmiş USD fiyatları çarpılırsa TL'nin değer kaybı yüzünden
geçmiş değerler olduğundan yüksek görünür ve getiri eksiye döner.

**Karar.**

| Kategori | Geçmiş seri |
|---|---|
| ABD | Twelve Data `/time_series` (mevcut karar) |
| BIST | Yahoo chart ucu (mevcut karar) |
| **USD/TRY** | **TCMB EVDS `TP.DK.USD.A.YTL`, tarih aralıklı. Varlık başına değil, `asset_id = USDTRY` altında tek sefer çekilir ve tüm ABD/emtia varlıklarınca paylaşılır; yeni varlıkta yalnızca eksik gün aralığı tamamlanır** |
| **Emtia** | **Türetilir: ONS geçmişi (Twelve Data `XAU/USD`, `XAG/USD`) × USD/TRY geçmişi ÷ 31,1035.** 9.1'in yedek formülünün geçmişe uygulanmış hali |
| **Fon** | **Kısıtlı.** Seçilen TEFAS ucu güncel listeyi döndürür; eski geçmiş ucu kapandı (9.4/1). M5 denemesinde tarih aralıklı bir uç aranır. Bulunamazsa fonlar kurulum öncesi döneme dahil edilmez; grafikte "fonlar GG.AA.YYYY tarihinden itibaren dahildir" notu yazılır |

Fon için uydurma veri üretilmez — maliyet değeriyle düz çizgi çizmek kullanıcıyı yanıltır ve reddedilmiştir.
Kurulumdan sonraki dönemde fonlar günlük snapshot'lar sayesinde grafiğe zaten girer.

---

## 7. Arama sonucunda fiyat: önbellekten

**Sorun.** 4.1/4 her sonuç satırında güncel fiyat istiyor, 4.1/2 sonuçların yazdıkça filtrelenmesini
istiyor, 9.3/1 tur başına 30 çağrı sınırı koyuyordu. "TH" yazınca 20 sonuç → 20 çağrı; "THY" yazınca
liste değişir ve çağrılar tekrarlanır.

**Karar.** Listede **önbellekteki son bilinen fiyat** gösterilir, yoksa `—`. Arama hiç çağrı yapmaz.
Kullanıcı bir sonuç seçtiğinde forma geçerken **tek çağrıyla** taze fiyat çekilir ve W8'de alış fiyatı
alanına varsayılan olarak önerilir. Maliyet: arama başına 0, seçim başına 1.

---

## 8. ABD alış fiyatı: forma USD→TL çevirici

**Sorun.** Kullanıcı AAPL'ı 230 USD'den aldığında aracı kurumda gördüğü sayı USD'dir; form ondan
TL birim maliyeti istiyor, yani alım günündeki kuru bulup çarpmasını bekliyordu.

**Karar.** Veri modeli **değişmez** (10.3/2 doğru; maliyet TL saklanır). Forma yalnızca yardımcı eklenir:
ABD kategorisinde "USD gir" seçeneğiyle kullanıcı USD fiyat girer, uygulama **alış tarihindeki** kurla
çarpıp TL alanını doldurur (alış tarihi artık formda — karar 3). Kullanıcı sonucu görüp değiştirebilir.

---

## 9. Manuel varlığın yaşam döngüsü

**Sorun.** 14. bölüm manuel eklemeyi tanımlıyor ama devamını konuşmuyordu: fiyat sonra nasıl
güncellenir, grafiklere nasıl katılır, toplama dahil mi?

**Karar.**

- `price_quote.kaynak = MANUEL`; otomatik tazelemeye girmez, çağrı bütçesi tüketmez.
- W11'den fiyat her güncellendiğinde **yeni bir `price_quote` satırı** yazılır — elle girilen fiyatlar
  zamanla bir seri oluşturur, grafik basamaklı da olsa çizilebilir.
- Portföy toplamına ve dağılıma **dahil edilir**.
- Her ekranda "elle girilen fiyat" etiketi görünür.
- Fiyat 7 günden eskiyse "fiyat güncel değil" uyarısı gösterilir.

Aynı kurallar 9.4/4'teki BIST elle fiyat girişi yedeği için de geçerlidir.

---

## 10. Form durumu: 6/5 kazanır

**Sorun.** 5/5 "sekmenin kendi durumu korunur", 6/5 "girilen değerler kaybolur" diyordu.

**Karar.** **Form alanları sıfırlanır, arama kutusundaki metin ve sonuç listesi korunur.**
Kullanıcı sekme değiştirip döndüğünde aramasını baştan yazmaz ama yarım kalmış bir formla karşılaşmaz.

---

## 11. "Toplam maliyet 0 (yalnızca nakit)" satırı

**Sorun.** Nakit TL'nin birim fiyatı 1,00 ₺ olduğu için (3. bölüm kural 2) yalnızca nakit içeren
portföyde toplam maliyet 0 değil, nakit tutarına eşittir; getiri %0 çıkar. Toplam maliyetin gerçekten
0 olduğu tek durum boş portföydür, o da W4'e düşer.

**Karar.** Satır **"Getiri hesaplanamayan durum (toplam maliyet = 0)"** olarak yeniden yazılır.
Kod her iki durumu da güvenli karşılar (`null` → `—`).

---

## 12. Emtia kodları: `XAUGR` / `XAGGR`

**Sorun.** `XAU` ve `XAG` uluslararası finans kodlamasında **troy ons** fiyatını ifade eder; doküman
bunları gram birimiyle eşliyordu. Truncgil'den gram, yedek kaynaktan ons gelince aynı kod iki farklı
birim taşır — **31 katlık, sessiz** bir hata riski.

**Karar.** Uygulama içi kodlar `XAUGR` / `XAGGR`. Adaptör birim dönüşümünü kendi içinde yapar ve
`price_quote`'a her zaman **gram** fiyatı yazar. Çıktı `asset.birim_tipi` ile tutarlılık testinden geçer.

---

## 13. "Son güncelleme" tarihi

**Sorun.** Yalnızca `SS:DD` gösteriliyordu; kullanıcı uygulamayı üç gün açmazsa "18:45" bugüne aitmiş
gibi görünür ve "veriler güncel değil" uyarısının amacını sessizce boşa çıkarır.

**Karar.** Zaman damgası bugüne aitse `SS:DD`, değilse `GG.AA SS:DD`.

---

## 14. Snapshot saati: 00:30, bir önceki güne

**Sorun.** Günün son fiyatı geç oluşuyor: TEFAS 21:00, ABD kapanışı TSİ 23:00 — **kış saatinde 00:00**.
Daha erken alınan snapshot kış aylarında ABD'yi seans ortasında yakalar.

**Karar.** Snapshot **00:30 TSİ**'de çalışır ve **bir önceki günün tarihine** yazılır.
Kural: **tetiklenme anı değil, kapsadığı gün esastır** (WorkManager tam saat garantisi vermez).
Bugünün değeri zaten canlı hesaplanıyor; snapshot geçmiş içindir.

---

## 15. Tatil takvimi: iki takvim

**Sorun.** 9.2/2 tek bir gömülü takvimden söz ediyordu. 4 Temmuz'da ABD kapalı BIST açık,
29 Ekim'de tersi. Tek takvimle her iki piyasada da yanlış günlerde çekim yapılır veya yapılmaz.

**Karar.** İki gömülü takvim: `US_MARKET` ve `TR_MARKET` (TEFAS, TR takvimini kullanır).
Altın/gümüş küresel olduğu için üçüncü takvim gerekmez; yalnızca hafta sonu kuralı uygulanır.
Her ikisi de yılda bir güncellenir.

---

## 16. "Toplu çekim" kuralı

**Sorun.** Finnhub'ın ücretsiz `/quote` ucu tek sembol alır; 9.1/3'ün "her varlık için ayrı istek
atılmaz" kuralı ABD kategorisinde harfi harfine sağlanamaz.

**Karar.** Kural yeniden yazılır: *"Fiyat çekimi çağıran taraf için topludur; kaynağın toplu sorgu
desteği yoksa adaptör isteği kendi içinde böler ve çağrı bütçesine gerçek istek sayısını bildirir."*

---

## 17. Piyasa saatleri kendi zaman diliminde

**Sorun.** 9.2 tablosu TSİ saatlerini sabit yazıyordu ("16:30–23:00, kış saatinde 17:30–00:00").
Türkiye kalıcı UTC+3, ABD yaz saati uyguluyor ve geçiş tarihleri her yıl değişiyor (Mart'ın 2. Pazarı,
Kasım'ın 1. Pazarı). Karar 5 tazelemeyi açılış/kapanış saatine bağladığı için bu daha da kritik.

**Karar.** Saatler kendi zaman diliminde tanımlanır — ABD `America/New_York` 09:30–16:00,
BIST `Europe/Istanbul` 10:00–18:00 — ve TSİ karşılığı `ZoneId` ile çalışma anında hesaplanır.
Hiçbir yere TSİ saati sabit yazılmaz.

---

## 18. EVDS ile saat başı kur servisi: yedek değil, farklı işler

**Sorun.** 9.1 saat başı servisi EVDS'nin yedeği yapmıştı. Oysa EVDS **günlük** resmî kurdur
(geçmiş seri için ideal, gün içi çevrim için değil), saat başı servis **gün içi** kuru verir.
Yedek gibi konumlandırılırsa EVDS çalışırken gün içi kur hiç tazelenmez.

**Karar.** Roller ayrılır:

| İş | Kaynak |
|---|---|
| Gün içi TL çevrimi (saat başı) | TCMB saat başı kur servisi |
| Geçmiş günlük kur serisi | TCMB EVDS `TP.DK.USD.A.YTL` |
| Gün sonu resmî kur (snapshot doğrulaması) | TCMB EVDS |

Yedek ilişkisi ayrıca tanımlanır: saat başı servis düşerse son EVDS kuru kullanılır.

---

## 19. Silinen varlık performans listesinden çıkar

**Sorun.** 7.3/4 "dönemin tamamında portföyde bulunmayan varlıklar listede kalır" diyor — bu dönem
ortasında **eklenen** varlıkları kastediyor. Dönem ortasında **silinen** varlığın durumu tanımsızdı.

**Karar.** Kalmaz. Faz 1'de satış kapsam dışı (4.4/4), dolayısıyla silme bir **düzeltme**dir, çıkış
değil — kullanıcı yanlış girdiği kaydı siler. Yanlış girilmiş bir kaydın geçmiş performansta
görünmesi anlamsızdır. Satış Faz 2'de eklendiğinde kural yeniden ele alınır.

---

## 20. Geçmiş seri çekimi 5 yılla sınırlı

**Sorun.** 11.3/1 varlık başına en fazla 5 yıl seri saklıyor; karar 3 ile kullanıcı 2015 tarihli bir
alım girebiliyor. 10 yıllık bir seri çekimi günlük 100 çağrılık geçmiş seri bütçesini (9.3/3)
tek varlıkta tüketebilir.

**Karar.** Geçmiş seri çekimi **5 yılla** sınırlanır. Daha eski alış tarihinde kayıt kabul edilir,
maliyet ve toplam getiri doğru hesaplanır, grafik 5 yıl öncesinden başlar ve
"grafik son 5 yılı gösteriyor" notu yazılır — 7.1/4'teki "portföy geçmişi X gün" notuyla aynı mekanizma.

---

## 21. Döviz ayrı bir kategori, nakit değil

**Sorun.** Portföyde tutulan USD/EUR nereye yazılacak? Nakit kategorisi TL'ye özgü (fiyatı sabit 1,00 ₺,
her dönemde %0 getiri); dövizi oraya koymak getiriyi yok sayardı. ABD kategorisine koymak da yanlış:
orası hisse, fiyatı USD gelip TL'ye çevriliyor.

**Karar.** `Category.DOVIZ` eklendi. Miktar para biriminin kendisidir (`UnitType.BIRIM`, ekranda "USD"),
fiyatı kurun TL karşılığıdır — yani kurun kendisi. Böylece kâr/zarar diğer varlıklarla aynı formülden
çıkar. `USDTRY` ve `EURTRY` her kurulumda bulunan sabit varlıklardır, katalog indirmesi gerektirmez.

Kaynaklar para birimine göre parametrik hâle getirildi (Yahoo `EURTRY=X`, Truncgil `EUR`, TCMB `Kod="EUR"`,
EVDS `TP.DK.EUR.A.YTL`), yani yeni bir kur eklemek için kod değil yalnız varlık kaydı gerekir.
Portföye USD eklenirse çevrim için zaten çekilen `FX_USDTRY_ID` serisi kopyalanır, ağdan ikinci kez istenmez.

## 22. Ekleme akışı kategoriden başlar

**Sorun.** + sekmesi doğrudan arama kutusu ve klavyeyle açılıyordu. Kullanıcı ne aradığını bilmiyorsa
(ör. "hangi emtialar var?") listeyi göremiyordu; ayrıca 30 bin kayıtlık tek bir havuzda arama yapılıyordu.

**Karar.** Önce kategori seçilir, arama o kategorinin içinde yapılır. Kayıt sayısı 600'ün altındaki
kategoriler (BIST, emtia, döviz) doğrudan listelenir; ABD (27 bin) ve fonda (2 bin) arama şarttır ve
klavye yalnız orada kendiliğinden açılır. Nakit TL ara adım olmadan forma gider.
"Son eklenenler" kısayolu kategori ekranında kalır: aynı varlığa tekrar alım girmek kısa yoldur.

## 23. Performans listesi kategori kırılımlı

**Sorun.** Varlık listesi düzdü; 20 varlıklı bir portföyde "ABD toplamda ne durumda?" sorusunun cevabı yoktu.

**Karar.** Liste akordeona çevrildi: kategori satırında dönem getirisi (% ve ₺), dokununca altında
varlıkları. Kategori yüzdesi varlık yüzdelerinin ortalaması **değildir** — kategorinin varlıkları birlikte
değerlenip aynı basit Dietz formülünden geçer, böylece kategori toplamları portföy toplamıyla tutarlı kalır.

---

## 24. Eskilik eşikleri tek yerde: `Tazelik`

**Sorun.** "Elle fiyat 7 günden eski" (karar 9) ve "kur eski, ABD değerleri güvenilmez" kuralları ayrı
ayrı ekranlara yazılsaydı ikisi zamanla farklı tanımlara kayabilirdi.

**Karar.** `core/calc/Tazelik.kt`: elle fiyat için 7 gün, kur için 2 gün (hafta sonu tatili sorun sayılmaz)
eşiği tek yerde tanımlı. Hem Portföy ekranı hem ileride başka bir ekran aynı kuralı kullanır.

## 25. Gün içi kapalı bilgisi, zamanlayıcının kendi kuralından okunur

**Sorun.** "Portföy çok büyük, gün içi güncelleme kapalı" notu (bölüm 14) ile zamanlayıcının hangi
portföy büyüklüğünde gün içi turları kapattığı (`AdaptiveInterval`) iki ayrı yerde tutulursa
ekrandaki uyarı zamanlayıcının gerçek davranışından sapabilir.

**Karar.** `TazelemeZamanlayici.gunIciKapali(kategori, varlıkSayısı)` companion fonksiyonu eklendi;
zamanlayıcı ve Portföy ekranı aynı hesaplamayı çağırır.
