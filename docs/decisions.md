# decisions.md

> Projede verilen bütün mimarisel-teknik kararları ve karar geçmişini içeren dökümantasyondur.

---

### Dependency Injection Kütüphanesi

- Seçim*: **Hilt**

- Son Güncelleme Tarihi*: 04.06.2026

- Alternatifler: **Koin**

- Sebep: **Opsiyonel**


### Navigasyon

- Seçim: **Compose Navigation**

- Son Güncelleme Tarihi: 09.06.2026

- Bağımlılık: `androidx.navigation:navigation-compose` **2.9.5** (version catalog: `navigationCompose`).

- Uygulama: Tek `NavHost` (`ui/navigation/LyraNavHost.kt`) Auth grafiğini barındırır (başlangıç
  hedefi Login). Navigasyon MVI ile uyumlu kurulur: ViewModel'de navigasyon API'si yoktur
  (bkz. [architecture/mvi-viewmodel-rules.md](architecture/mvi-viewmodel-rules.md) §6); navigasyon
  `Intent → Effect` üzerinden akar, `Route` Effect'i tüketip `NavHost`'tan gelen lambda'ları çağırır.


### Sunum Katmanı Mimarisi

- Seçim: **MVI (Model-View-Intent)**

- Son Güncelleme Tarihi: 09.06.2026

- Kapsam: Her ekran State + Intent + Effect sözleşmesiyle yazılır. Detaylı kurallar ve
  referans implementasyon (Login) için bkz. [architecture/mvi-overview.md](architecture/mvi-overview.md).

- Sebep: Tek yönlü veri akışı, durumsuz UI, test edilebilirlik.


### Hilt Annotation Processing

- Seçim: **KSP** (kapt değil)

- Son Güncelleme Tarihi: 09.06.2026

- Sürümler: Hilt **2.59.2**, KSP **2.2.10-2.0.2** (Kotlin 2.2.10 ile birebir uyumlu).

- Compose'da ViewModel: `androidx.hilt:hilt-lifecycle-viewmodel-compose` (`hiltViewModel()`).
  Compose Navigation henüz kurulmadığından navigation-compose bağımlılığı eklenmemiştir.

- Sebep: KSP, kapt'a göre belirgin biçimde hızlıdır ve Kotlin 2.2 ile uyumludur.


### AGP 9 Built-in Kotlin + KSP Uyumu

- Karar: `gradle.properties` içinde **`android.disallowKotlinSourceSets=false`** zorunludur.

- Son Güncelleme Tarihi: 09.06.2026

- Sebep: AGP 9 built-in Kotlin kullanır; KSP'nin ürettiği kaynak dizinlerini eklemesi bu bayrak
  olmadan derlemeyi kırar. Bayrak deneysel (experimental) olarak işaretlidir ancak gereklidir.


### Now Playing Ekrani

- Karar: MVI mimarisi; calınan sarki bilgisi singleton `PlayerRepository` uzerinden paylasılır.

- Son Güncelleme Tarihi: 13.06.2026

- Kapsam: `data/player/` (NowPlayingTrack, PlayerRepository, InMemoryPlayerRepository),
  `ui/nowplaying/` (NowPlayingContract, NowPlayingViewModel, NowPlayingScreen), `di/PlayerModule`.
  `HomeContract`, `HomeViewModel`, `HomeScreen` guncellendi.

- Neden PlayerRepository: Navigation argument yerine singleton state kullanilir;
  HomeScreen MiniPlayer ve NowPlayingScreen ayni StateFlow'u gozlemler, gecis sirasinda
  sarki bilgisi kaybolmaz.

- Tiklanabilir kartlar: QuickPick, RecentlyPlayed, PlaylistForYou tiklamalari
  `HomeIntent.PlayTrack(track, queue)` uretir; ViewModel `playerRepository.playQueue()` cagirir
  ve `HomeEffect.NavigateToNowPlaying` gonderir. `queue`, tiklanilan section'ın tum track
  listesidir (QuickPicks tiklandıgında queue = tum QuickPicks, vs.).

- Sonraki/Onceki: `PlayerRepository.skipNext()` / `skipPrevious()` index kaydırarak
  `_currentTrack`'i gunceller. `NowPlayingViewModel` bu cagriyi yapar; UI otomatik guncellenir.

- MiniPlayer: Yalnizca `state.currentTrack != null` iken gozukur; tiklandıgında
  `HomeIntent.OpenNowPlaying` → `HomeEffect.NavigateToNowPlaying` akar.

- Sebep: Backend hazır degil; oynatma mantigi (konum, sure) mock degerlerle simule edilir.
  Gercek API geldiginde yalnizca `InMemoryPlayerRepository` degisir.


### Ara Ekrani (Search Screen)

- Karar: MVI mimarisi ile implementasyon; stub repository deseni.

- Son Güncelleme Tarihi: 13.06.2026

- Kapsam: `data/search/` (Genre, SearchFeed, SearchRepository, MockSearchRepository),
  `ui/search/` (SearchContract, SearchViewModel, SearchScreen), `di/SearchModule`.

- Tur filtresi: `selectedFilter: String?` UiState'te tutulur; `null` = Hepsi.
  `filteredGenres` turetilen alan olarak ViewModel'de hesaplanir.
  Tur karti tiklamasi ve chip tiklamasi ayni `FilterSelected` Intent'ini tetikler.

- Sebep: Backend hazir degil; genre listesi MockSearchRepository'de statik olarak tanimlanir.
  Gercek API geldiginde yalnizca MockSearchRepository ve SearchModule degisir.


### Favoriler Ekrani

- Karar: MVI mimarisi; paylasilan singleton `FavoritesRepository` uzerinden favori durumu yonetilir.

- Son Güncelleme Tarihi: 17.06.2026

- Kapsam: `data/favorites/` (FavoritesRepository, InMemoryFavoritesRepository),
  `ui/favorites/` (FavoritesContract, FavoritesViewModel, FavoritesScreen), `di/FavoritesModule`.
  `NowPlayingViewModel` guncellendi: FavoritesRepository inject edildi, `ToggleFavorite` Intent
  artik repository uzerinden calisir; `isFavorited` state favorites Flow'u izlenerek senkronize tutulur.

- Singleton tasarimi: `FavoritesRepository` `@Singleton` olarak baglanir; herhangi bir ekran
  `NowPlayingTrack` vererek `toggleFavorite()` cagirabilir. Favoriler ekrani ayni StateFlow'u
  gozlemler, degisiklikler aninda yansir.

- Bos durum: Liste bos oldugunda aciklayici hint gosterilir; Cal/Karistir butonlari devre disi kalir.

- Sebep: Backend hazir degil; `InMemoryFavoritesRepository` uygulama yasam suresi boyunca state tutar.
  Gercek API geldiginde yalnizca implementasyon ve DI baglamasi degisir.


### Profil Ekrani

- Karar: MVI mimarisi; kullanici adi `AuthRepository.getLoggedInUserName()` ile alinir, tema durumu singleton `ThemeRepository` uzerinden izlenir.

- Son Guncelleme Tarihi: 17.06.2026

- Kapsam: `ui/profile/` (ProfileContract, ProfileViewModel, ProfileScreen).
  `LyraNavhost.kt` guncellendi: Profile rotasi artik `ProfileRoute`'u render eder.
  `LyraIcons.kt` guncellendi: `Settings` ve `HelpOutline` ikonlari eklendi.

- Tema toggle akisi: `ProfileIntent.ToggleTheme` → `ProfileViewModel` → `ThemeRepository.toggleTheme()`
  → `MainActivity` recompose → `LyraAppTheme(darkTheme)` → `ProfileViewModel.init` collect ile state senkronize.
  NavHost uzerinden lambda gecilmez; ViewModel dogrudan ThemeRepository inject eder.

- Gercek veri: Yalnizca kullanici adi (`displayName`, `initials`). Diger alanlar (takipci, takip) placeholder.
  Calma listesi sayisi `playlistCount: Int = 0` olarak UiState'te tutulur; Kutuphane ekrani tamamlandiginda guncellenir.

- Placeholder satirlar: Ses kalitesi, Cevrimdisi indirme, Bildirimler, Gizlilik, Yardim ve destek
  MVI kurallarina uygun sekilde Intent tanimlidir; su an no-op olarak islenirler.

- Sebep: Backend hazir degil; gercek API geldiginde yalnizca ViewModel icindeki repository cagrisi degisir.


### Backend Hazır Değilken Veri Katmanı

- Karar: **Stub repository** deseni — Repository interface + `Fake<X>Repository` implementasyonu.

- Son Güncelleme Tarihi: 09.06.2026

- Sebep: Backend REST API sözleşmesi tanımlı değil (`agents.md` §2.2 uydurmak yasak). Gerçek API
  geldiğinde yalnızca implementasyon ve DI bağlaması değişir; ViewModel/Contract etkilenmez.