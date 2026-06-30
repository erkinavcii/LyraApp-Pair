# LyraApp 🎵 - Spotify Benzeri Müzik Akış Uygulaması

LyraApp, Spotify benzeri modern bir müzik dinleme, oynatma ve akış (streaming) uygulaması referans tasarımıdır. Uygulama; **Jetpack Compose**, **Material Design 3** ve **MVI (Model-View-Intent)** mimarisi gibi güncel Android geliştirme standartları kullanılarak geliştirilmiştir. 

Uygulamanın temel amacı, kullanıcılara hem çevrimiçi (online) hem de tamamen internet bağlantısı olmadan çevrimdışı (offline) müzik dinleme deneyimi sunmaktır. Bu kapsamda, verilerini dinamik olarak `https://streaming-api.halitkalayci.com/` adresinde barındırılan REST API servisinden çeken uygulama, aynı zamanda yerel önbelleğe alma (caching) ve veri senkronizasyonu için Room veritabanını kullanmaktadır.

---

## 📸 Ekran Görüntüleri & Akışlar

*(Görsellerinizi eklemek için `docs/images/` klasörüne ilgili dosyaları kaydedip aşağıdaki bağlantıları güncelleyebilirsiniz.)*

| 1. Telefon Girişi (1/3) | 2. OTP Doğrulama (2/3) | 3. Bilgileri Tamamla (3/3) |
| :---: | :---: | :---: |
| ![Telefon Giriş Ekranı Görseli](docs/images/login_screen.png) <br> *[Buraya Giriş Ekranı Gelecek]* | ![OTP Ekranı Görseli](docs/images/otp_screen.png) <br> *[Buraya OTP Ekranı Gelecek]* | ![Profil Tamamlama Görseli](docs/images/complete_profile_screen.png) <br> *[Buraya Profil Tamamlama Gelecek]* |

| 4. Ana Sayfa (Home) | 5. Arama Ekranı (Search) | 6. Şimdi Çalıyor (Now Playing) |
| :---: | :---: | :---: |
| ![Ana Sayfa Görseli](docs/images/home_screen.png) <br> *[Buraya Ana Sayfa Gelecek]* | ![Arama Ekranı Görseli](docs/images/search_screen.png) <br> *[Buraya Arama Ekranı Gelecek]* | ![Şimdi Çalıyor Görseli](docs/images/now_playing_screen.png) <br> *[Buraya Şimdi Çalıyor Gelecek]* |

| 7. Kütüphane (Library) | 8. Çalma Listesi Detay | 9. Yeni Çalma Listesi Ekle |
| :---: | :---: | :---: |
| ![Kütüphane Görseli](docs/images/library_screen.png) <br> *[Buraya Kütüphane Gelecek]* | ![Playlist Detay Görseli](docs/images/playlist_detail_screen.png) <br> *[Buraya Playlist Detay Gelecek]* | ![Çalma Listesi Ekle Görseli](docs/images/new_playlist_screen.png) <br> *[Buraya Yeni Çalma Listesi Gelecek]* |

| 10. Profil & Premium | 11. Premium Plan Seçimi | 12. Kart Ödeme Formu |
| :---: | :---: | :---: |
| ![Profil Görseli](docs/images/profile_screen.png) <br> *[Buraya Profil Ekranı Gelecek]* | ![Plan Seçim Görseli](docs/images/premium_plans_screen.png) <br> *[Buraya Plan Seçimi Gelecek]* | ![Ödeme Formu Görseli](docs/images/payment_screen.png) <br> *[Buraya Ödeme Formu Gelecek]* |

| 13. Ödeme Başarılı | 14. Yenileme Hatırlatıcı |
| :---: | :---: |
| ![Ödeme Başarılı Görseli](docs/images/payment_success_screen.png) <br> *[Buraya Ödeme Başarılı Gelecek]* | ![Yenileme Popup Görseli](docs/images/renewal_popup.png) <br> *[Buraya Yenileme Hatırlatıcı Popup Gelecek]* |

---

## 🛠️ Teknolojiler ve Bağımlılıklar

*   **Core:** Kotlin, Coroutines, Flow.
*   **UI/UX:** Jetpack Compose, Material Design 3.
*   **Mimariler:** MVI (Model-View-Intent), Clean Architecture, Offline-First (Local-First).
*   **Veritabanı (Local Cache):** Room Database.
*   **Bağlantı/Ağ (API Client):** Retrofit 2, OkHttp 3, Gson.
*   **Dependency Injection:** Hilt (Dagger).
*   **Annotation Processing:** KSP (Kotlin Symbol Processing).
*   **Medya Yürütme (Playback):** Jetpack Media3 ExoPlayer.

---

## 📐 Mimari ve Tasarım Kararları

### 1. MVI (Model-View-Intent) Sunum Katmanı
Sunum katmanında durum yönetimi (state management) tutarlılığını sağlamak için **MVI** tercih edilmiştir. 
*   **UiState:** Ekranın herhangi bir andaki durumunu temsil eden tekil ve değiştirilemez (immutable) veri sınıfıdır.
*   **Intent:** Kullanıcı eylemlerini (tıklama, metin yazma vb.) ViewModel'e ileten sinyallerdir.
*   **Effect:** Navigasyon, toast mesajı gösterme gibi tek seferlik (one-shot) olayları temsil eden kanaldır.

### 2. Çevrimdışı-Öncelikli (Offline-First) Favori Yapısı
Kullanıcının beğendiği şarkılar yerel veritabanında (Room) saklanır ve arka planda sunucu çalma listesi API'si ile senkronize edilir:
*   Kalp butonuna basıldığı an, UI beklemeden yerel veritabanı güncellenir (anlık tepki).
*   Arka planda sunucuya istek gönderilir ve başarılı olunca veritabanındaki `isSynced` bayrağı güncellenir.
*   Kullanıcı çıkış yaptığında veya oturum değiştirdiğinde yerel önbellek ve önbellek çalma listesi ID'leri güvenli şekilde sıfırlanır.

### 3. Sunucu Yetkili Oynatma ve Reklam Yönetimi
*   Şarkı oynatılmadan önce doğrudan stream url'si çekmek yerine `POST /api/v1/me/playback/next` ucu tetiklenir.
*   Premium olmayan ücretsiz kullanıcılara sunucu tarafında her 3 şarkıda bir `type: "ad"` (reklam) dönülür. Reklam bittiğinde `/playback/ad-complete` raporlanır ve asıl şarkıya geçiş yapılır. Premium kullanıcılar reklam görmeden doğrudan müzik dinleyebilir.

---

## 📂 Proje Özellikleri (Features)

### 🔑 Giriş ve Onboarding Akışı (1/3, 2/3, 3/3)
1.  **Telefon Girişi (1/3):** 10 haneli telefon numarası alınarak SMS OTP doğrulama kodu istenir.
2.  **OTP Doğrulama (2/3):** Gönderilen 6 haneli kod girilir. Test amaçlı `123456` veya `000000` kodları kullanılabilir. İlk kez giren kullanıcılar profil ekranına, kayıtlı kullanıcılar ana sayfaya yönlendirilir.
3.  **Profil Tamamlama (3/3):** Eğer kullanıcı sisteme **ilk kez giriş yapıyorsa (kaydolmamışsa)**, karşısına bu adım gelir ve kullanıcıdan Ad, Soyad ve Doğum Tarihi bilgileri alınarak sisteme kaydı tamamlanır. Mevcut kullanıcılar bu adımı atlayarak doğrudan Ana Sayfa'ya yönlendirilir.

### 🏠 Ana Sayfa (Home) & Oynatıcı (Player)
*   **Hızlı Seçimler & Son Çalınanlar:** Dinleme geçmişiniz ve sizin için seçilen albümler/çalma listeleri listelenir.
*   **Şimdi Çalıyor (Player Screen):** Kapak görselinin renklerine göre dinamik olarak renk değiştiren gradyan arka plan, repeat/shuffle modları ve ExoPlayer ile müzik çalma.
*   **Arka Planda Oynatma (Background Playback):** Jetpack Media3 ExoPlayer entegrasyonu sayesinde uygulama arka planda veya ekran kapalıyken bile müzik çalmaya devam eder. Cihazın bildirim panelindeki medya kartından kontrol edilebilir.
*   **Aydınlık / Karanlık Tema (Light/Dark Mode):** Profil ekranından aydınlık ve karanlık modlar arasında geçiş yapılabilir. Tema tercihleri yerel hafızada kalıcı olarak saklanır.
*   **Progress Bar:** Sürükleme (scrubbing) esnasında ExoPlayer'ı yormayan yerel arabellek yönetimi ve parmak çekildiğinde yönlendirme (seekTo) yapan pürüzsüz kaydırma yapısı.

### 🔍 Arama (Search)
*   Şarkı, sanatçı veya albüm adına göre anlık filtreleme.
*   Pop, Elektronik, Akustik gibi müzik türlerine göre kategorize edilmiş listeleme kartları.

### 📚 Kütüphane & Çalma Listesi Yönetimi
*   **Beğenilen Şarkılar & İndirilenler:** Kütüphane ekranının en üstünde sabitlenmiş özel listeler.
*   **Çevrimdışı (Offline) Mod & İndirmeler:** Şarkılar yerel depolamaya indirilebilir ve internet bağlantısı kesildiğinde veya çevrimdışı mod aktifken doğrudan yerel dosyadan oynatılabilir.
*   **Yeni Çalma Listesi Oluşturma:** Çalma listesi adı, açıklaması ve listeye dahil edilecek şarkıların çoklu seçimi ile çalma listesi oluşturma.
*   **Çalma Listesi Silme:** Kullanıcının kendi oluşturduğu çalma listelerinin sağındaki 3 nokta (`MoreVert`) ikonuna tıklandığında açılan DropdownMenu içerisinden çalma listesini sunucu ve yerel hafızadan silme desteği.

### 💎 Premium Abonelik & Ödeme Sistemi
*   **Paywall (Plan Seçimi):** Reklamsız müzik, yüksek ses kalitesi ve sınırsız şarkı atlama gibi premium avantajların listelendiği, "Aylık" ve "Tek Seferlik" planların seçildiği ekran.
*   **Ödeme Ekranı:** Güvenli kart ödemesi simülasyonu.
    *   *Başarılı Test Kartı:* `4242 4242 4242 4242`
    *   *Reddedilen Test Kartı:* `4000 0000 0000 0002`
*   **Ödeme Başarılı Ekranı:** Satın alım sonrası Premium üyelik kutlaması ve ana sayfaya yönlendirme.
*   **Yenileme Hatırlatıcısı:** Üyeliğin bitmesine 3 gün veya daha az kaldığında ana sayfada otomatik beliren yenileme pop-up diyalogu.

---

## 🚀 Projeyi Çalıştırma

Projeyi yerelinizde derlemek ve çalıştırmak için:

1.  Projeyi klonlayın veya zip dosyasını çıkarın.
2.  **Android Studio (Ladybug veya üstü)** ile projeyi açın.
3.  Gradle senkronizasyonunun (Sync) tamamlanmasını bekleyin.
4.  Terminal üzerinden derleme kontrolü yapın:
    ```bash
    ./gradlew compileDebugSources
    ```
5.  Emülatör veya fiziksel cihaz bağlayarak **Run (Shift + F10)** butonuna basın.

---
*Geliştirici Ekibi: Turkcell & Partner pair-programming işbirliğiyle hazırlanmıştır.*
