# Patch candidates - save for later

Shortlist of apps worth patching next, re-checked 2026-10-06 against the 926-package /
215-bundle community aggregate (`community_coverage.json`, rebuilt from
morphe-patches.software's `data/bundles.json`). None of the open candidates below are
on `docs/patch-blocklist.md`. Community coverage is informational only (owner decision
2026-10-07): an app covered by another bundle is still fair game.

Before starting one, follow the normal workflow in `AGENTS.md`: download the latest APK,
triage for PairIP/shields/server-side gates, find a stable anchor, then patch + smoke
test + decompile-verify.

## Tier 1 - ads only, easiest wins (GMA/AppLovin init+load kills)

| App | Package | Latest seen | Notes |
|---|---|---|---|
| Shazam | `com.shazam.android` | 16.62.1 | **Shipped 2026-10-05** - "Disable telemetry" (FirebaseAnalytics.logEvent + public Crashlytics surface). **2026-10-11** - "Open in Morphe YouTube apps" (issue #48: remaps the YouTube / YT Music "Open in" rows to the Morphe packages, falling back to stock; first extension module `extensions/shazam`). No ad SDK, billing or shields in the dex; no premium gate (free app) |
| Simple Radio | `com.streema.simpleradio` | 6.2.0 | **Shipped 2026-09-29** - "Enable Premium" (local `iab_premium` pref gate + `isPremium()`) and "Disable ads" (GMA init + MAX interstitial load/show) |
| WiFi Analyzer | `com.farproc.wifi.analyzer` | 3.11.2 | **Shipped 2026-09-29** - "Disable ads" (single `Settings` show-ad gate on the `next_show_ad_time_millisec` pref) |
| SoundHound | `com.melodis.midomiMusicIdentifier.freemium` | 10.5.8 | **Shipped 2026-10-07** - "Disable ads" (GMA init + startPreload, every load/loadAd/loadAds on banner/native/interstitial/rewarded/app-open/Ad Manager classes, Meta AN init). NOTE the package: the earlier `com.melodis.soundhound.android` guess does not exist. Premium (Play Billing, obfuscated) parked |
| Dictionary.com | `com.dictionary` | 12.1.3 | **Parked 2026-10-07, needs deep dive.** Package/version verified against the APK (677). No IAP surface in app code (the billingclient bytes come from AppsFlyer's SDK). Ads are the problem: R8 stripped/inlined the GMA lite wrappers - only `MobileAds` + `nativead.NativeAd` remain public; banner/native/interstitial load through an R8-renamed Amazon-APS pipeline (Q4.d slots, P5.i/M5 flows, Q5.b placement enum with `/1027916/ANDROID_*` GAM units) and the readable `com.dictionary.foundation.ads.AdInitializer` bootstrap only covers the startup path, not the on-demand composable path (Q6.v). Candidate anchors for a future attempt: AdInitializer.b(Context) preamble, P5.i.b(Q5.a), GMA ClientApi located by its creator strings |
| Moon+ Reader | `com.flyersoft.moonreader` | 10.7 | **Shipped 2026-10-06** - "Disable ads" (MrAd.disableAds() forced true; the only app class touching the ad SDK) |

## Tier 2 - ads + local Pro/Premium

| App | Package | Latest seen | Notes |
|---|---|---|---|
| Pi Music Player | `com.Project100Pi.themusicplayer` | 3.2.0.0 | **Shipped 2026-09-29** - "Enable Premium" (static flag + 5-list in R8 holder; a()/b() forced, temp-ad-free resetter neutered, flag seeded in clinit) and "Disable ads" (GMA init/loads + MAX interstitial load/show) |
| TuneIn Radio | `tunein.player` | 42.5 | **Blocked 2026-10-05** - full PairIP shield; see blocklist |
| Podcast Republic | `com.podcast.podcasts` | 9.17.0 | **Shipped 2026-10-05** - "Disable ads" (GMA init/loads, MAX load/show, Meta AN + InMobi init). No premium gate: ad-free is Firebase invite state only |
| Castbox | `fm.castbox.audiobook.radio.podcast` | 11.26.1 | **Shipped 2026-10-05** - "Disable ads" (GMA init/loads, MAX load/show, Meta AN + InMobi init). No premium gate: vip lists are server-synced, purchases RSA-verified |
| Smart AudioBook Player | `ak.alizandro.smartaudiobookplayer` | 11.8.2 | Full-version unlock is a single local `Billings$LicenseType` getter (two other bundles force it). Known caveat: re-signing breaks Google sign-in. Package corrected 2026-10-07 (was `a.a.smartplayer.app`). **Next to attempt** |
| Cube ACR | `com.catalinagroup.callrecorder` | 2.4.281 | Local Play Billing purchase state + rewarded-video premium timers; ~7 methods on obfuscated classes. Package corrected 2026-10-07 (was `com.cubil.msgs.callrecorder`) |
| Today Weather | `mobi.lockdown.weather` | 2.5.0 | Ads + premium upgrade; local purchase flags. Re-signing breaks the Google Maps key (needs a resource step). Package corrected 2026-10-07 (was `com.todayweather.dev`) |
| Alarm Clock Xtreme | `com.alarmclock.xtreme.free`? | ? | Ads + PRO via Play Billing; not covered by any bundle. Package from secondary sources only - verify before starting |
| Radio Garden | `com.jonathanpuckey.radiogarden`? | ? | Fits the Simple Radio / Podcast Republic cluster; IAP removes marketing messages. Package from secondary sources only (`radio.garden` looks wrong) - verify before starting |
| UEFA Champions League app | `com.uefa.ucl` | ? | Football-adjacent (Sofascore/FotMob/Livescore cluster); needs a triage for ad SDK vs sponsor content. Uncovered 2026-10-06 |

## Tier 3 - simple Pro unlocks

| App | Package | Latest seen | Notes |
|---|---|---|---|
| Device Info HW | `ru.andr7e.deviceinfohw` | 5.27.1 | **Blocked 2026-09-29** - no Pro boolean exists in this build; see blocklist |
| Xplore File Manager | `com.lonelycatgames.Xplore` | 4.49.10 | **Blocked 2026-10-10** - PairIP VM + native tamper check; see blocklist |
| DartCounter | `com.dartcounter.mobile` | 9.7.5 | **Blocked 2026-10-11** - PairIP VM + native tamper check; Ultimate entitlement is server-side; see blocklist |
| spacedesk | `ph.spacedesk.beta` | 2.1.38 | **Blocked 2026-10-11** - free licence has the same features as paid; paid tier is a commercial business licence, and forcing it would spoof one to the PC driver (owner decision); see blocklist |

## Also covered by other bundles (informational, not a blocker)

MacroDroid (3 bundles), Pocket Casts, Hevy, CapCut, InShot, VN Video Editor, PicsArt,
Photomath, MX Player (`.ad`), AccuWeather, Alarmy, Strong, Lifesum, ReadEra, Automate,
Poweramp, Twitch (6 bundles), Stremio.

Checked 2026-10-07 against the 926-package rebuild:
- Flud (`com.delphicoder.flud`, not the earlier `com.delphic.flud` guess) - Morning
  Entree ("Flud Ad-Free & Bypasses"), rushiranpise and shaun-the-sheep ("Remove Ads")
- CX File Explorer (`com.cxinventor.file.explorer`) - hxreborn/morphe-patches
  ("Unlock premium" + themes)

Moved from the blocklist on 2026-10-07 (checked against the 777-package aggregate,
September 2026): SD Maid SE (`eu.darken.sdmse`), Poweramp, Solid Explorer, Podcast
Addict, Windy, Hermit, Calimoto, Nova Launcher, Sleep as Android, MiXplorer.

## Not suggested (checked, rejected or deferred)

- myTuner Radio (`com.mytuner.mobile`), Forza Football (`com.forzafootball`), The Free
  Dictionary (`com.tfd.mobile.TfdSearch`) - not on APKMirror; try APKPure/Aptoide first.
- Squid (`com.steadfastinnovation.android.projectpapyrus`) - premium validated server-side.
- Root Explorer (`com.speedsoftware.rootexplorer`), Titanium Backup
  (`com.keramidas.TitaniumBackup`), Torque (`org.prowl.torquefree`), Symfonium
  (`com.symfonion.symfonium`) - paid/trial apps; works like Tasker but it is straight
  piracy. Only on explicit request.
- Online games on the target device (Jawaker Tarneeb, Racify, UEFA Eurofantasy, Sorare)
  - online-server games: training/economy state is server-authoritative (the same wall
  the UFM 27 mod hit; see `apks/games/UFM 27/MODDING-NOTES.md`). Skip the whole category.

## Queued by owner (2026-10-11)

Triaged 2026-10-12; status per row.

| App | Package | Notes |
|---|---|---|
| AZ Screen Recorder | `com.hecorat.screenrecorder.free` | **Blocked 2026-10-12** - full PairIP still ships on 6.9.9 (VMRunner, libpairipcore.so); see blocklist |
| Spark Mail | `com.readdle.spark` | **Blocked 2026-10-12** - Premium/Plus/Pro are server-side (Readdle backend returns BillingInfo); see blocklist |
| BlueMail | `me.bluemail.mail` | **Shipped 2026-10-12** - "Disable ads" (2.2.408; ads via react-native-google-mobile-ads). Premium/ad-free entitlement is server-side and not patched. Not runtime-tested (ARM-only libs, extractNativeLibs=false; stock also fails on the x86_64 emulator) |
| PDF Reader | `pdf.pdfreader.viewer.editor.free` | **Pending 2026-10-12** - only APK found is an unverified Aptoide 2.1.6 (Play shows 2.1.5), signed by a self-signed "Jx Clarynx" cert that does not match the Play developer ("QR Code Scanner."). APKMirror and APKPure do not carry it. **2026-10-14: confirmed adware repack** (same signer and injected launcher-activity payload as the fake Futbin "27.5"); quarantined, never use it. Waiting for the genuine Play build from the owner's phone |
| SmartWOD Timer | `net.smartwod.timer` | **Shipped 2026-10-12** - "Enable Premium" (RNIapModule.getAvailableItemsByType @ReactMethod hooked via `extensions/smartwod` to report lifetime (Premium+) and ad-free as owned; premium is decided in JS from react-native-iap results checking only productId) and "Disable ads" (1.46.9). Runtime-verified on a Play-certified emulator (Premium+ / Lifetime, no ads) |

## Recommended first batch

Shazam + Simple Radio + Device Info HW + Pi Music Player: two trivial ad kills and two
local pro gates, all verified available on APKMirror.

## Status (2026-09-19)

Tier 1 progress, all checked against the latest APKMirror builds:

| App | Status |
|---|---|
| Textra SMS | **Shipped** - "Enable Pro" (license pref class located structurally; l() -> true, k() -> false) |
| jetAudio | **Shipped** - "Enable Pro" (purchase info class located by its inline literal; leaf getters -> true) |
| Unified Remote | **Shipped** - "Enable Pro" (RevenueCat `EntitlementInfo.isActive()` -> true) |
| AZ Screen Recorder | **Blocked** - full PairIP (`SignatureCheck`, `VMRunner`, `VmDecryptor`); see patch-blocklist |
| ACR Phone | Parked - readable billing prefs bridge but the premium computation is obfuscated and a periodic purchase-refresh worker can overwrite cached state |
| Car Scanner ELM OBD2 | Parked - fully obfuscated app code, no RevenueCat; billing path not located yet |
| JuiceSSH | Parked - purchases stored in an ORMLite `purchase` table, premium gate in obfuscated code |
| Business Calendar 2 | **Patched 2026-10-11** - "Disable ads" (readable app code; `AdsUtil.showAdsForUser(Context)` gates the mediation SDK start and interstitials). Pro untouched |

## Requested on Reddit (v1.14.0-dev.5 release post, 2026-10-14)

From the comments on [the release post](https://www.reddit.com/r/MorpheApp/comments/1x54qjm/release_heval_morphe_patches_v1140dev5_new_apps/).
In queue order. Verdicts are first impressions; nothing has been triaged yet.

**Bug report (fix first)**

| App | Package | Notes |
|---|---|---|
| AiScore | `com.onesports.score` | **Bug, 3 reports:** goal alerts never arrive on the patched app, even with every notification permission granted (FotMob works for the same users). Likely Firebase push rejecting the re-signed app, as with 365Scores/Sofascore (fixed there with Spoof signature / GmsCore support) |

**App requests**

| # | App | Package | Requested | Notes |
|---|---|---|---|---|
| 1 | Brevent | `me.piebridge.brevent` | Unlock all features / premium | Most upvoted request (7); owner replied "i will try" |
| 2 | WeMuslim (Athan & Qibla) | `com.fyxtech.muslim` | Ads + analytics, premium, hide Premium tab and upgrade banners | **Triaged 2026-10-14 (3.5.0, vc 141, APKMirror, developer signer CN=athan O=yalla):** premium is server-side (membership comes from the server over protobuf; purchases confirmed server-side). Ads-only is possible but large: Pangle, AppLovin MAX, Unity, GMA, Meta AN, Amazon behind obfuscated gating. No Premium bottom tab in 3.5.0 (nav: Home, Prayer, Discover, Messages, Me); upgrade banners are anchorable by layout id. **Blocked 2026-10-14** on 3.5.4 (vc 149): ads/telemetry/banner patches built and green (smoke + CLI) but the native `libagora-ey.so` JNI_OnLoad rejects re-signed builds (runtime-tested, differential control); see blocklist. Parked on `wip/wemuslim`. |
| 3 | Interval Timer | `cc.dreamspark.intervaltimer` | Remove ads ("ads mess with the UI") | **Shipped 2026-10-14 (2.3.30)** - Disable ads + Disable telemetry; emulator: no ads or gaps, workouts run |
| 4 | Camera (Ocean Galaxy, "Timemark") | `com.oceangalaxy.camera.new` | Not specified | **Blocked 2026-10-14** (v10.0.270) - SecNeo packer plus libInnoSecure native protection |
| 5 | Tennis app ("TNNS Live") | `com.tennisrn` | Not specified | **Not now (2026-10-14, 8.4.1)** - React Native; ad-free decided in JS from RevenueCat (server); blocking ads would mean ~12 ad networks |
| 6 | VesselFinder | `com.astrapaging.vff` | Not specified | **In progress 2026-10-14 (6.7.8)** - ads-only (Capacitor AdvertPlugin + GMA); premium is server-side (account roles); PairIP licence check only |
| 7 | Textpand | unknown | Not specified | Find the package first |
| 8 | Moneycontrol | `com.divum.MoneyControl` | Not specified | **In progress 2026-10-14 (14.6.2)** - ads-only; Pro content and entitlements are server-side; bundles Akamai bot protection (runtime check needed) |
| 9 | A Better Routeplanner | `com.iternio.abrpapp` | Not specified | **Server-side (2026-10-14, 7.1.8)** - no ads; purchases verified server-side; premium data is server-backed |
| 10 | The Wall Street Journal (+ NYT, Washington Post, Financial Times, Guardian) | `wsj.reader_sp`, ... | Paywall / ads | Subscriptions are server-side; ad removal at most |
| 11 | Ultimate Guitar, WiFi Map Pro, The Pioneer | various | Not specified | **2026-10-14:** Ultimate Guitar blocked (full PairIP VM); WiFi Map blocked (DexProtector) and premium via Adapty server-side; The Pioneer (`de.gstmb.podcasts`) server-side (RevenueCat membership, no ads) |
| 12 | Backdrops | `com.backdrops.wallpapers` | Newer patch | Already in another Morphe bundle, and wallpapers update regardless; low priority |
| 13 | X-VPN | unknown | Premium | **Skipped 2026-10-14** (owner decision) |
| 14 | Twitch | `tv.twitch.android.app` | Remove ads | **Skipped 2026-10-14** (owner decision; ads are mostly server-inserted, Xtra covers it) |
| 15 | Cronometer | `com.cronometer.android` | Not specified | Already on the blocklist (APKMirror 403, likely server-side) |
| 16 | UniTV | not on Google Play | Full version | Off-Play IPTV service from a download site; skip |

| 17 | LuckyClick (auto clicker) | `com.peter890331.luckyclick.app` | Remove ads ("really long and unskippable") | **Triaged 2026-10-14 (3.28, vc 68):** **Shipped 2026-10-14** - Disable ads (app's own AD_FREE_PERMANENT flag + GMA backstop) and Disable license check (PairIP licence check only). Emulator: no ads, Start goes straight to capture |
| 18 | Car Scanner ELM OBD2 | `com.ovz.carscanner` | Unlock premium | Already parked (fully obfuscated, billing path not located; see "Also covered" list). Genuine APK in apks/carscanner |
| 19 | Cherrygram (Telegram fork) | github.com/arslan4k1390/Cherrygram | Premium | Open source; "premium" is Telegram Premium, which is server-side. Likely skip |
| 20 | Lovora | unknown | Not specified | Find the package first |
| 21 | VidMate | not on Google Play | Not specified | Off-Play downloader from download sites (repack risk); skip |

**Community note:** a Korean user worries about patching KakaoTalk, which handles payments and certificates in Korea. A re-signed KakaoTalk may break Kakao Pay and certificate features; consider a warning in the patch description or the README.

## Requested on hoo-dles discussions (2026-10-15)

From the App Requests category of [hoo-dles/morphe-patches discussions](https://github.com/hoo-dles/morphe-patches/discussions) (559 requests, 2026-04-14 to 2026-10-15). Excluded: 102 apps another bundle on [morphe-patches.software](https://morphe-patches.software/#apps) already patches (its bundles.json, fetched 2026-10-15), 6 apps we already ship and 7 already in this file or the blocklist. EasyNotes (#347) gave a truncated package for an app we ship. Duplicate requests for one package are merged. Sorted by demand (upvotes x2 + comments + duplicate requests). "Closed" means hoo-dles closed the discussion; it does not mean the app is covered. Nothing here is triaged yet.

| # | App | Package | Requested | Votes | Discussion |
|---|---|---|---|---|---|
| 1 | MT Manager | `bin.mt.plus` | Unlock VIP access (more features) - Login removal / bypass (only if possible, since some f… | 28 | [#550](https://github.com/hoo-dles/morphe-patches/discussions/550) |
| 2 | WhatsApp | `com.whatsapp` | remove ads (for regions that have them) - remove updates and communities | 15 | [#436](https://github.com/hoo-dles/morphe-patches/discussions/436) |
| 3 | ElevenReader - Voice Reader | `io.elevenlabs.readerapp` | Unlock Premium | 13 | [#731](https://github.com/hoo-dles/morphe-patches/discussions/731) |
| 4 | Musicolet Music Player | `in.krosbits.musicolet` | Unlock Pro features | 14 | [#340](https://github.com/hoo-dles/morphe-patches/discussions/340) |
| 5 | VSCO | `com.vsco.cam` | VSCO X Subscription Unlock | 13 | [#373](https://github.com/hoo-dles/morphe-patches/discussions/373) |
| 6 | TiviMate IPTV Player | `ar.tvplayer.tv` | Unlock Premium | 12 | [#531](https://github.com/hoo-dles/morphe-patches/discussions/531) |
| 7 | Brilliant | `org.brilliant.android` | Unlock premiun | 10 | [#375](https://github.com/hoo-dles/morphe-patches/discussions/375) |
| 8 | dynamicSpot - Dynamic Island | `com.jamworks.dynamicspot` | Unlock Premium - Remove Ads | 11 | [#433](https://github.com/hoo-dles/morphe-patches/discussions/433) |
| 9 | App Cloner | `com.applisto.appcloner` | The link :  It's the app to do tons of modifications in APKs easily, but most features are… | 11 | [#341](https://github.com/hoo-dles/morphe-patches/discussions/341) |
| 10 | Google Health | `com.fitbit.FitbitMobile` | Avoid tracking / increased privacy - Unlock Premium -  - food registration, calorie goals,… | 10 | [#482](https://github.com/hoo-dles/morphe-patches/discussions/482) |
| 11 | Clip Studio Paint | `jp.co.celsys.clipstudiopaint.googleplay` | Unlock CSP PRO or EX | 9 | [#633](https://github.com/hoo-dles/morphe-patches/discussions/633) |
| 12 | Surfshark: Secure VPN service | `com.surfshark.vpnclient.android` | Unlock Surfshark One | 8 | [#670](https://github.com/hoo-dles/morphe-patches/discussions/670), [#690](https://github.com/hoo-dles/morphe-patches/discussions/690), [#781](https://github.com/hoo-dles/morphe-patches/discussions/781) |
| 13 | Gmail | `com.google.android.gm` | Remove ads - Block telemetry | 9 | [#849](https://github.com/hoo-dles/morphe-patches/discussions/849) |
| 14 | Hyperion Launcher | `projekt.launcher` | Unlock premium | 7 | [#441](https://github.com/hoo-dles/morphe-patches/discussions/441), [#902](https://github.com/hoo-dles/morphe-patches/discussions/902) |
| 15 | Poweramp Equalizer | `com.maxmpz.equalizer` | Unlock Premium | 7 | [#827](https://github.com/hoo-dles/morphe-patches/discussions/827) |
| 16 | Du Chinese: Read&Learn Chinese | `org.sinamon.duchinese` | 1. Unlock premium 2. Remove ads 3. Block telemetry | 6 | [#563](https://github.com/hoo-dles/morphe-patches/discussions/563) |
| 17 | Headspace | `com.getsomeheadspace.android` | Unlock premium | 8 | [#517](https://github.com/hoo-dles/morphe-patches/discussions/517) |
| 18 | NordVPN - fast VPN for privacy | `com.nordvpn.android` | Lifetime Premium subscription | 7 | [#666](https://github.com/hoo-dles/morphe-patches/discussions/666), [#782](https://github.com/hoo-dles/morphe-patches/discussions/782) |
| 19 | Beeper | `com.beeper.android` | Unlock account limit Ads and messages from devs | 7 | [#900](https://github.com/hoo-dles/morphe-patches/discussions/900), [#1043](https://github.com/hoo-dles/morphe-patches/discussions/1043) |
| 20 | ChatGPT | `com.openai.chatgpt` | Remove in - feed ads. | 7 | [#935](https://github.com/hoo-dles/morphe-patches/discussions/935) |
| 21 | Mullvad VPN | `net.mullvad.mullvadvpn` | Unlock premium Remove ads Block Telemetry | 7 | [#667](https://github.com/hoo-dles/morphe-patches/discussions/667) |
| 22 | ReadEra | `org.readera.premium` | Backgorund play - Section: Dictionary <img width="576" height="1280" alt="photo_2026 - 07 … | 7 | [#585](https://github.com/hoo-dles/morphe-patches/discussions/585) (closed) |
| 23 | Infinite Painter (Version 7.2.9/Latest) | `com.brakefield.painter` | Unlock Premium | 7 | [#600](https://github.com/hoo-dles/morphe-patches/discussions/600) |
| 24 | Jio tv | `com.jio.jioplay.tv` | Remove ads | 6 | [#405](https://github.com/hoo-dles/morphe-patches/discussions/405) (closed) |
| 25 | Flitsmeister | `nl.flitsmeister` | Unlock pro, disable store update | 5 | [#629](https://github.com/hoo-dles/morphe-patches/discussions/629) |
| 26 | Locket Widget | `com.locket.Locket` | Unlock Locket Gold features (such as uploading photos directly from the camera roll, addin… | 6 | [#582](https://github.com/hoo-dles/morphe-patches/discussions/582) |
| 27 | GOWOD – Mobility & Stretching | `eu.gowod` | Unlock Premium | 5 | [#393](https://github.com/hoo-dles/morphe-patches/discussions/393) |
| 28 | Network Analyzer Pro | `net.techet.netanalyzer.an` | Unlock Pro | 5 | [#584](https://github.com/hoo-dles/morphe-patches/discussions/584) |
| 29 | Pleco Chinese Dictionary | `com.pleco.chinesesystem` | 1. Unlock paid add - ons/features 2. Block telemetry | 5 | [#565](https://github.com/hoo-dles/morphe-patches/discussions/565) |
| 30 | Wanderlog | `com.wanderlog.app` | Unlock premium | 5 | [#388](https://github.com/hoo-dles/morphe-patches/discussions/388) (closed) |
| 31 | 18Birdies Golf GPS Rangefinder | `com.squarevalley.i8birdies` | Unlock premium | 5 | [#473](https://github.com/hoo-dles/morphe-patches/discussions/473) |
| 32 | ExitLAG | `com.exitlag.gamebooster` | Not specified | 5 | [#430](https://github.com/hoo-dles/morphe-patches/discussions/430) |
| 33 | Magic Earth | `com.generalmagic.magicearth` | Unlock Premium | 4 | [#628](https://github.com/hoo-dles/morphe-patches/discussions/628), [#947](https://github.com/hoo-dles/morphe-patches/discussions/947) |
| 34 | Pocket FM | `com.radio.pocketfm` | 🎧 Listening features Ad - free listening Background playback Sleep timer with custom durat… | 5 | [#824](https://github.com/hoo-dles/morphe-patches/discussions/824) |
| 35 | PowerDirector | `com.cyberlink.powerdirector.DRA140225_01` | unlocl premium | 5 | [#415](https://github.com/hoo-dles/morphe-patches/discussions/415) |
| 36 | Rain Rain Sleep Sounds | `com.timgostony.rainrain` | Unlock Premium (allow the usage of subscription sounds for free) This app is really great … | 4 | [#406](https://github.com/hoo-dles/morphe-patches/discussions/406) |
| 37 | RP Diet | `com.rp.rpdiet` | > Unlock Premium | 5 | [#359](https://github.com/hoo-dles/morphe-patches/discussions/359) |
| 38 | Airlearn | `com.unacademy.antonio` | Unlock pro Unlock ultra <img width="1080" height="2400" alt="Screenshot_2026 - 09 - 11 - 1… | 3 | [#606](https://github.com/hoo-dles/morphe-patches/discussions/606), [#921](https://github.com/hoo-dles/morphe-patches/discussions/921) (closed) |
| 39 | DeepL | `com.deepl.mobiletranslator` | I don't know how to use GitHub, but I wanted to ask if you could make patches to have the … | 4 | [#1049](https://github.com/hoo-dles/morphe-patches/discussions/1049) |
| 40 | Drops: Language Learning Games Drops | `com.languagedrops.drops.international` | Unlock premium - Block telemetry - Hide ads | 4 | [#842](https://github.com/hoo-dles/morphe-patches/discussions/842) |
| 41 | Oasis | `com.liveoasis.oasis` | all Premium features unlocked 🔓 | 3 | [#404](https://github.com/hoo-dles/morphe-patches/discussions/404), [#873](https://github.com/hoo-dles/morphe-patches/discussions/873) |
| 42 | PureVPN: Fast VPN & Secure VPN | `com.gaditek.purevpnics` | Unlock premium Remove ads Block Telemetry | 4 | [#677](https://github.com/hoo-dles/morphe-patches/discussions/677) |
| 43 | Sololearn | `com.sololearn` | Unlock premium | 4 | [#414](https://github.com/hoo-dles/morphe-patches/discussions/414) |
| 44 | Astrology Master | `com.astrology.master` | Unlock Pro | 3 | [#970](https://github.com/hoo-dles/morphe-patches/discussions/970) |
| 45 | Feeld: Open-minded dating app | `co.feeld` | Unlock Premium - See likes | 3 | [#572](https://github.com/hoo-dles/morphe-patches/discussions/572) |
| 46 | Memorize By Heart | `com.memorize_by_heart` | Unlock Premium | 3 | [#743](https://github.com/hoo-dles/morphe-patches/discussions/743) |
| 47 | Speedify | `com.speedify.speedifyandroid` | Unlock unlimited quota | 3 | [#401](https://github.com/hoo-dles/morphe-patches/discussions/401) |
| 48 | TruckSim GPS - ETS2 & ATS Map | `com.cocoasandwich.trucksimgps` | unlock premium - remove ads - remove telemetry (if exist) | 3 | [#399](https://github.com/hoo-dles/morphe-patches/discussions/399) |
| 49 | Turbo VPN - Secure VPN Proxy | `free.vpn.unblock.proxy.turbovpn` | Unlock premium Remove ads Block Telemetry | 3 | [#665](https://github.com/hoo-dles/morphe-patches/discussions/665) |
| 50 | Chineasy | `com.chineasy.cards` | 1. Unlock premium 2. Remove ads 3. Block telemetry | 3 | [#562](https://github.com/hoo-dles/morphe-patches/discussions/562) |
| 51 | Data Eraser - iShredder | `com.projectstar.ishredder.android.standard` | `Unlock Military Edition` to unlock military and business grade file shredder. | 3 | [#370](https://github.com/hoo-dles/morphe-patches/discussions/370) |
| 52 | Download Twitter Videos - GIF | `tweeter.gif.twittervideodownloader` | Unlock Premium | 1 | [#523](https://github.com/hoo-dles/morphe-patches/discussions/523) |
| 53 | Elsa Speak | `us.nobarriers.elsa` | Not specified | 3 | [#335](https://github.com/hoo-dles/morphe-patches/discussions/335) |
| 54 | ExpressVPN: VPN Fast & Secure | `com.expressvpn.vpn` | Unlock premium Remove ads Block Telemetry | 3 | [#669](https://github.com/hoo-dles/morphe-patches/discussions/669) |
| 55 | Franco Kernel Manager | `com.franco.kernel` | Unlock all features | 3 | [#554](https://github.com/hoo-dles/morphe-patches/discussions/554) |
| 56 | GPS Map Camera : Geo Tagging | `com.gpsmapcamera.geotagginglocationonphoto` | Remove ads | 3 | [#854](https://github.com/hoo-dles/morphe-patches/discussions/854) |
| 57 | Leghe Fantacalcio® Serie A Enilive | `it.quadronica.leghe` | Unlock Premium - Remove ads (banners and full - screen interstitials) - Block analytics/te… | 3 | [#942](https://github.com/hoo-dles/morphe-patches/discussions/942) |
| 58 | Loklok: TV&Shows&Short | `com.woden.celestis` | <img width="1080" height="1426" alt="Screenshot_20260716 - 113427_Loklok" src=" - at | 3 | [#637](https://github.com/hoo-dles/morphe-patches/discussions/637) |
| 59 | Moises | `ai.moises` | Not specified | 3 | [#699](https://github.com/hoo-dles/morphe-patches/discussions/699) |
| 60 | Moviebox | `movieboxonline.org` | Unlock premium Remove ads | 2 | [#573](https://github.com/hoo-dles/morphe-patches/discussions/573) (closed) |
| 61 | Musescore | `com.musescore.playerlite` | unlock premium - remove popup offers - remove constant onboarding process | 3 | [#821](https://github.com/hoo-dles/morphe-patches/discussions/821) |
| 62 | Myscript Notes:AI Note taking | `com.myscript.nebo` | Unlock premium/full version | 3 | [#754](https://github.com/hoo-dles/morphe-patches/discussions/754) |
| 63 | Notes - iOS Style Notepad | `com.inotesapp.inotes` | Unlock all features Disable telemetry Make it fully offline | 3 | [#850](https://github.com/hoo-dles/morphe-patches/discussions/850) |
| 64 | Noteshelf 3: Digital Notes | `com.fluidtouch.noteshelf3` | Unlock Premium | 3 | [#680](https://github.com/hoo-dles/morphe-patches/discussions/680) |
| 65 | Polarsteps | `com.polarsteps` | Unlock premium: all card layers | 3 | [#642](https://github.com/hoo-dles/morphe-patches/discussions/642) |
| 66 | TCB / Read Chinese to Learn | `uk.co.metapps.thechairmansbao` | 1. Unlock premium 2. Remove ads 3. Block telemetry | 3 | [#564](https://github.com/hoo-dles/morphe-patches/discussions/564) |
| 67 | Temp Mail Temporary Email | `Com.tempmail` | Unlock premium - Remove ads - Block telemetry | 3 | [#933](https://github.com/hoo-dles/morphe-patches/discussions/933) |
| 68 | Textnow | `com.enflick.android.TextNow` | Remove ads Remove paywalls Remove call ads | 3 | [#1009](https://github.com/hoo-dles/morphe-patches/discussions/1009) |
| 69 | TrainerDay | `trainerday.turbo` | Unlock premium | 3 | [#588](https://github.com/hoo-dles/morphe-patches/discussions/588) |
| 70 | TunnelBear VPN | `com.tunnelbear.android` | Unlock premium Remove ads Block Telemetry | 3 | [#674](https://github.com/hoo-dles/morphe-patches/discussions/674) |
| 71 | Background Eraser | `com.handycloset.android.eraser` | Unlock Pro | 2 | [#522](https://github.com/hoo-dles/morphe-patches/discussions/522) |
| 72 | Chordify | `net.chordify.chordify` | unlock premium, - remove ads, - block telemetry | 2 | [#1068](https://github.com/hoo-dles/morphe-patches/discussions/1068) |
| 73 | Honeydew | `com.redhoney.eatlist` | Unlock Premium | 2 | [#860](https://github.com/hoo-dles/morphe-patches/discussions/860), [#1047](https://github.com/hoo-dles/morphe-patches/discussions/1047) |
| 74 | Mango launcher | `app.pinya.mango` | * Unlock premium * Remove telemetry & trackers if available | 2 | [#742](https://github.com/hoo-dles/morphe-patches/discussions/742) |
| 75 | Mediately | `com.mediately.drugs.it` | Unlock premium | 2 | [#993](https://github.com/hoo-dles/morphe-patches/discussions/993) |
| 76 | MT Capsule | `com.pryshedko.mtisland` | Not specified | 2 | [#732](https://github.com/hoo-dles/morphe-patches/discussions/732) (closed) |
| 77 | MuscleWiki | `com.musclewiki.macro` | Unlock premium | 2 | [#650](https://github.com/hoo-dles/morphe-patches/discussions/650) |
| 78 | Outdooractive | `com.outdooractive.Outdooractive` | Unlock premium: all card layers available | 2 | [#641](https://github.com/hoo-dles/morphe-patches/discussions/641) |
| 79 | Patreon | `com.patreon.android` | > Hide Home tab (from home page) > Hide recommend creators in home page > Feature to disab… | 2 | [#620](https://github.com/hoo-dles/morphe-patches/discussions/620) |
| 80 | TimeTree | `works.jubilee.timetree` | Unlock Premium features (upload files, pin events, vertical display) - Hide ads | 2 | [#983](https://github.com/hoo-dles/morphe-patches/discussions/983) |
| 81 | Animash | `com.abstractsoft.animalsmash` | `Remove ads` to hide all ads and removes `pop - up buy` to remove ads `Enable unlimited sh… | 2 | [#371](https://github.com/hoo-dles/morphe-patches/discussions/371) |
| 82 | Applock | `com.sp.protector.free` | Remove ads without disabling the lock screen and look into a way to force the fingerprint … | 2 | [#880](https://github.com/hoo-dles/morphe-patches/discussions/880) |
| 83 | Autoresponder | `7857280643314172854` | unlock full premium please | 2 | [#624](https://github.com/hoo-dles/morphe-patches/discussions/624) |
| 84 | Betternet: Secure VPN Hotspot | `com.freevpnintouch` | Unlock premium Remove ads Block Telemetry | 2 | [#662](https://github.com/hoo-dles/morphe-patches/discussions/662) |
| 85 | Blindstory: Story Saver & View | `blindstory.saver.forinstagram` | Request: - Unlock Premium | 2 | [#986](https://github.com/hoo-dles/morphe-patches/discussions/986) |
| 86 | Bunpro Japanese SRS | `bunpro.jp.bunpro_srs` | Unlock premium | 2 | [#940](https://github.com/hoo-dles/morphe-patches/discussions/940) |
| 87 | Chessis: Chess Analysis | `com.chessimprovement.chessis` | Unlock pro | 2 | [#525](https://github.com/hoo-dles/morphe-patches/discussions/525) |
| 88 | Coddy | `tech.coddy.coddyapp` | Unlocks premium | 2 | [#839](https://github.com/hoo-dles/morphe-patches/discussions/839) |
| 89 | Couply: The App for Couples | `io.couply.android` | unlock premium | 2 | [#723](https://github.com/hoo-dles/morphe-patches/discussions/723) |
| 90 | djay - DJ App & AI Mixer | `com.algoriddim.djay_free` | Unlock premium Remove ads Block Telemetry | 2 | [#658](https://github.com/hoo-dles/morphe-patches/discussions/658) |
| 91 | DuoCards | `com.duocards.app` | Unlock Premium subscription. | 2 | [#790](https://github.com/hoo-dles/morphe-patches/discussions/790) |
| 92 | ESET VPN | `com.eset.vpn` | Unlock premium Remove ads Block Telemetry | 2 | [#668](https://github.com/hoo-dles/morphe-patches/discussions/668) |
| 93 | Fapless: Quit Addiction | `com.RebirthGames.Fapless` | Unlock premium | 2 | [#859](https://github.com/hoo-dles/morphe-patches/discussions/859) |
| 94 | Firefox fast & private browser | `org.mozilla.firefox` | Please provide a patch to fix its reloading tabs issue while switching between the apps in… | 2 | [#777](https://github.com/hoo-dles/morphe-patches/discussions/777) |
| 95 | Gaminik: Auto Screen Translate | `com.gaminik.i18n` | Unlock premium: all premium features available | 2 | [#955](https://github.com/hoo-dles/morphe-patches/discussions/955) |
| 96 | Glance Weather | `com.glanceweather.app` | Unlock Premium - Be Functional | 2 | [#491](https://github.com/hoo-dles/morphe-patches/discussions/491) |
| 97 | Habitify: Habit Tracker | `co.unstatic.habitify` | Unlock premium - Block telemetry | 2 | [#883](https://github.com/hoo-dles/morphe-patches/discussions/883) |
| 98 | Headway – Daily Micro Learning | `com.headway.books` | Unlock Headway Premium features - Access premium book summaries and content - Remove premi… | 2 | [#800](https://github.com/hoo-dles/morphe-patches/discussions/800) |
| 99 | Healthify | `com.healthifyme.basic` | Unlock premium - Remove ads - Remove telemetry | 2 | [#343](https://github.com/hoo-dles/morphe-patches/discussions/343) |
| 100 | HuffPost - News & Politics | `com.huffingtonpost.android` | Unlock premium (Gold) Gold tier includes "Unlimited reading + ad - free access on the Huff… | 2 | [#437](https://github.com/hoo-dles/morphe-patches/discussions/437) |
| 101 | Innovative Language Learning | `com.innovativelanguage.innovativelanguage101` | unlock premium | 2 | [#799](https://github.com/hoo-dles/morphe-patches/discussions/799) |
| 102 | IPVanish VPN: Fast & Secure | `com.ixolit.ipvanish` | Unlock premium Remove ads Block Telemetry | 2 | [#672](https://github.com/hoo-dles/morphe-patches/discussions/672) |
| 103 | IVPN | `net.ivpn.client` | Unlock premium Remove ads Block Telemetry | 2 | [#671](https://github.com/hoo-dles/morphe-patches/discussions/671) |
| 104 | Johns Hopkins Antibiotic Guide | `com.unboundmedicine.jhopkins` | Unlock paywall | 2 | [#545](https://github.com/hoo-dles/morphe-patches/discussions/545) |
| 105 | JumpJumpVPN: Fast & Secure | `app.jumpjumpvpn.jumpjumpvpn` | Unlock premium Remove ads Block Telemetry | 2 | [#664](https://github.com/hoo-dles/morphe-patches/discussions/664) |
| 106 | KLitColors | `com.kitcolorspro` | Unlock premium features - Unlimited inventory - Unlimited wishlist - Unlimited paint schem… | 2 | [#427](https://github.com/hoo-dles/morphe-patches/discussions/427) |
| 107 | Kuli Kuli - Travel Translator | `io.iftech.bistrobridge` | Unlock premium | 2 | [#683](https://github.com/hoo-dles/morphe-patches/discussions/683) |
| 108 | Lazada | `com.lazada.android` | bypass buy now button to checkout immediately | 2 | [#1062](https://github.com/hoo-dles/morphe-patches/discussions/1062) |
| 109 | Marinetraffic | `com.marinetraffic.android` | Unlock premium Remove ads | 2 | [#905](https://github.com/hoo-dles/morphe-patches/discussions/905) |
| 110 | Medical Dictionary by Farlex | `com.farlex.dictionary.medical` | Premium unlock and remove ads | 2 | [#542](https://github.com/hoo-dles/morphe-patches/discussions/542) |
| 111 | mewatch: Watch Video, Movies | `sg.mediacorp.android` | activate premium as shown here: <img width="1920" height="1080" alt="image" src=" - attach… | 2 | [#389](https://github.com/hoo-dles/morphe-patches/discussions/389) |
| 112 | Microsoft PowerPoint | `com.microsoft.office` | unlock premium remove license | 2 | [#889](https://github.com/hoo-dles/morphe-patches/discussions/889) |
| 113 | Multiple Accounts: Dual Space | `com.excelliance.multiaccounts` | Unlock VIP - Remove ads | 2 | [#583](https://github.com/hoo-dles/morphe-patches/discussions/583) |
| 114 | MYNDIFY | `com.app.mynder.app` | Unlock [premium]( features | 2 | [#400](https://github.com/hoo-dles/morphe-patches/discussions/400) |
| 115 | NymVPN – Anonymous No-Log VPN | `net.nymtech.nymvpn` | Unlock premium Remove ads Block Telemetry | 2 | [#673](https://github.com/hoo-dles/morphe-patches/discussions/673) |
| 116 | Osmosis - Medical School Study | `org.osmosis.med` | Unlock Prime | 2 | [#559](https://github.com/hoo-dles/morphe-patches/discussions/559) |
| 117 | Pokemon TCG Pocket | `jp.pokemon.pokemontcgp` | Unlock Premium | 2 | [#580](https://github.com/hoo-dles/morphe-patches/discussions/580) |
| 118 | PrivadoVPN - VPN App & Proxy | `io.privado.android` | Unlock premium Remove ads Block Telemetry | 2 | [#676](https://github.com/hoo-dles/morphe-patches/discussions/676) |
| 119 | Private Internet Access VPN | `com.privateinternetaccess.android` | Unlock premium Remove ads Block Telemetry | 2 | [#675](https://github.com/hoo-dles/morphe-patches/discussions/675) |
| 120 | Radio Javan | `com.radiojavan.androidradio` | Unlock premium Remove ads Block Telemetry | 1 | [#652](https://github.com/hoo-dles/morphe-patches/discussions/652) |
| 121 | Reshoot 360 1.17.0 | `com.loicmartin.reshoot360` | Unlock premium | 2 | [#379](https://github.com/hoo-dles/morphe-patches/discussions/379) |
| 122 | Reuters - Breaking World News | `com.thomsonreuters.reuters` | Unlock premium | 2 | [#769](https://github.com/hoo-dles/morphe-patches/discussions/769) |
| 123 | Sanford Guide Antimicrobial | `com.sanford` | Unlock paywall | 2 | [#546](https://github.com/hoo-dles/morphe-patches/discussions/546) |
| 124 | School Planner | `daldev.android.gradehelper` | Unlock premium features - Remove ads | 2 | [#413](https://github.com/hoo-dles/morphe-patches/discussions/413) |
| 125 | scribd | `com.scribd.app.reader0.docs` | Unlock premium, remove ads | 2 | [#893](https://github.com/hoo-dles/morphe-patches/discussions/893) |
| 126 | Series Graph: Episode Ratings | `com.henri.seriesgraph` | Unlock Plus Membership - Remove Ads - MicroG Integration | 2 | [#575](https://github.com/hoo-dles/morphe-patches/discussions/575) |
| 127 | Songsterr | `com.songsterr` | Unlock Premium | 2 | [#875](https://github.com/hoo-dles/morphe-patches/discussions/875) |
| 128 | Subnautica | `com.unknownworlds.subnautica` | Possibility to replace one of the game's existing language with those missing compared to … | 2 | [#726](https://github.com/hoo-dles/morphe-patches/discussions/726) |
| 129 | Swish by NBN23 | `com.nbn23.fansapp` | Unlock premium | 2 | [#1031](https://github.com/hoo-dles/morphe-patches/discussions/1031) |
| 130 | The Great Courses (previously known as G | `com.tgc.greatcoursesplus` | Unlock Premium | 2 | [#541](https://github.com/hoo-dles/morphe-patches/discussions/541) |
| 131 | USB Audio Player PRO | `com.extreamsd.usbaudioplayerpro` | Unlock premium Remove ads Block Telemetry | 2 | [#656](https://github.com/hoo-dles/morphe-patches/discussions/656) |
| 132 | Via Browser
 
 #### Package Name
 
 mark | `mark.via.gp` | Telemetry & Analytics Removal: Completely strip internal app measurement, metrics, and tra… | 2 | [#720](https://github.com/hoo-dles/morphe-patches/discussions/720) |
| 133 | VPN 360: Private & Secure Wifi | `co.infinitysoft.vpn360` | Unlock premium Remove ads Block Telemetry | 2 | [#663](https://github.com/hoo-dles/morphe-patches/discussions/663) |
| 134 | Vpn australia | `vpn.australia_tap2free` | Unlock premium Remove ads | 2 | [#576](https://github.com/hoo-dles/morphe-patches/discussions/576) |
| 135 | VPN Hotspot Shield: Proxy VPN | `hotspotshield.android.vpn` | Unlock premium Remove ads Block Telemetry | 2 | [#661](https://github.com/hoo-dles/morphe-patches/discussions/661) |
| 136 | Web Video Caster | `com.instantbits.cast.webvideo` | Unlock Premium This should unlock the following features: - No in - app advertising - Bloc… | 2 | [#745](https://github.com/hoo-dles/morphe-patches/discussions/745) |
| 137 | WeChat | `com.tencent.mm` | locally keep the contents such as texts, pictures, videos that others recalled. | 2 | [#840](https://github.com/hoo-dles/morphe-patches/discussions/840) |
| 138 | YuSpeak | `com.yuspeak` | Unlock premium features | 2 | [#914](https://github.com/hoo-dles/morphe-patches/discussions/914) |
| 139 | Adguard | `cơm.adguard.android` | Please update patches adguard stable 4.13. Patches 4.12.81 not working in this version. Th… | 1 | [#693](https://github.com/hoo-dles/morphe-patches/discussions/693) (closed) |
| 140 | Babbel | `com.babbel.mobile.android.en` | unlock premium | 1 | [#950](https://github.com/hoo-dles/morphe-patches/discussions/950) (closed) |
| 141 | DJI Lightcut | `com.lightcut.videoeditor` | Spoof application to support Pixel 11 Pro and Pixel 10 Pro in the same way as currently su… | 1 | [#830](https://github.com/hoo-dles/morphe-patches/discussions/830) |
| 142 | FilterBox Notification Manager | `com.catchingnow.np` | Unlock premium features | 1 | [#898](https://github.com/hoo-dles/morphe-patches/discussions/898) |
| 143 | Flowx | `com.enzuredigital.weatherbomb` | Unlock premium | 1 | [#881](https://github.com/hoo-dles/morphe-patches/discussions/881) |
| 144 | HIFI Video Player | `io.hifi.video` | Unlock The app In Apk File Download | 1 | [#931](https://github.com/hoo-dles/morphe-patches/discussions/931) |
| 145 | Odido | `nl.oberon.tmobile.my` | automated daily GB extention | 1 | [#521](https://github.com/hoo-dles/morphe-patches/discussions/521) |
| 146 | Relane | `xorsand.relane` | Unlock Premium | 1 | [#681](https://github.com/hoo-dles/morphe-patches/discussions/681) (closed) |
| 147 | Rhythm Tabla & Tanpura PREMIUM | `com.psslabs.rhythmpaid` | * Unlock Premium * Remove ads * Remove telemetry | 1 | [#725](https://github.com/hoo-dles/morphe-patches/discussions/725) (closed) |
| 148 | Time Clock: Work Time Tracker | `co.timesquared.timetracker` | Premium code. Mod Keyword 👉 PurchasesOrchestrator; - >isAnonymous()Z | 1 | [#858](https://github.com/hoo-dles/morphe-patches/discussions/858) |
| 149 | Voloco: Auto Vocal Tune Studio | `com.jazarimusic.voloco` | Unlock premium Remove ads Block Telemetry | 1 | [#657](https://github.com/hoo-dles/morphe-patches/discussions/657) |
| 150 | Youtube Music | `com.google.android.apps.youtube.music` | Add Light Mode | 1 | [#678](https://github.com/hoo-dles/morphe-patches/discussions/678) (closed) |
| 151 | Zann WordUp: Build Vocabulary | `co.wordupapp.app` | unlock pro (there's a subscription and a lifetime tier) | 1 | [#1010](https://github.com/hoo-dles/morphe-patches/discussions/1010) |
| 152 | 12min: Book Summaries Daily | `com.br.twelvemin` | Unlock Premium features - Access the full book summary library - Access premium content an… | 1 | [#802](https://github.com/hoo-dles/morphe-patches/discussions/802) |
| 153 | 2 Player games : no wifi games | `com.JindoBlu.TwoPlayerGamesChallenge` | Remove watching ads for bonus actions in games | 1 | [#1045](https://github.com/hoo-dles/morphe-patches/discussions/1045) |
| 154 | 4k Wallpapers | `hd.uhd.wallpapers.best.quality` | Unlock Premium - Remove Ads - Block Telemetry | 1 | [#703](https://github.com/hoo-dles/morphe-patches/discussions/703) |
| 155 | Adobe Photoshop: Photo Editor | `com.adobe.photoshop.retail` | Unlock premium Remove ads Block Telemetry | 1 | [#653](https://github.com/hoo-dles/morphe-patches/discussions/653) |
| 156 | Aeromatic: Aeropress Recipes | `com.awardsense.aeromatic` | Unlock Premium - Allow launching app without needing to have Google play store enabled | 1 | [#567](https://github.com/hoo-dles/morphe-patches/discussions/567) |
| 157 | Al Content Detector - ForgeSpy | `io.zyur.forgespy` | Unlock Premium - Infinite credits | 1 | [#847](https://github.com/hoo-dles/morphe-patches/discussions/847) |
| 158 | Alpenglow:Sunset Predictions | `com.andydev.alpenglowapp` | Unlock Alpenglow Pro | 1 | [#1016](https://github.com/hoo-dles/morphe-patches/discussions/1016) |
| 159 | Alux: Build Wealth Faster | `com.alux` | Unlock Premium features - Unlock the full library of lessons and coaching sessions - Acces… | 1 | [#804](https://github.com/hoo-dles/morphe-patches/discussions/804) |
| 160 | Annecy Météo | `com.annecymeteoappv3` | Unlock premium (Club Annecy Météo) - Block telemetry (optional) | 1 | [#989](https://github.com/hoo-dles/morphe-patches/discussions/989) |
| 161 | Approach | `com.pjamalama.pickupart` | Unlock premium | 1 | [#766](https://github.com/hoo-dles/morphe-patches/discussions/766) |
| 162 | ATG | `com.atgonlinecoaching.clientapp` | unkock greyed out programs that are unlocked once you purchase a monthly sub. <img width="… | 1 | [#780](https://github.com/hoo-dles/morphe-patches/discussions/780) |
| 163 | Audio Evolution Mobile TRIAL | `com.extreamsd.aemobiledemo` | Unlock premium Remove ads Block telemetry | 1 | [#706](https://github.com/hoo-dles/morphe-patches/discussions/706) |
| 164 | Avast One | `com.avast.android.mobilesecurity` | Disable Ads - Unlock all client - sided features | 1 | [#626](https://github.com/hoo-dles/morphe-patches/discussions/626) |
| 165 | Beaky News | `net.smartlauncher.beakynews` | Unlock Premium | 1 | [#975](https://github.com/hoo-dles/morphe-patches/discussions/975) |
| 166 | BeReal. | `com.bereal.ft` | Allow download of posts. | 1 | [#856](https://github.com/hoo-dles/morphe-patches/discussions/856) |
| 167 | Bet365 Canada | `com.bet365SportsCA.Bet365_Application` | I can't use the app when I have developer mode turned on, and it's extremely annoying to k… | 1 | [#774](https://github.com/hoo-dles/morphe-patches/discussions/774) |
| 168 | Bikecontrol: virtual shifting | `de.jonasbark.swiftcontrol` | unlock pro or unlock unlimited time/day. | 1 | [#1014](https://github.com/hoo-dles/morphe-patches/discussions/1014) |
| 169 | Bilingual Books:  Parallel Read | `com.kursx.smartbook` | Unlock Pro, which includes export to ankidroid for spaces repetition. | 1 | [#934](https://github.com/hoo-dles/morphe-patches/discussions/934) |
| 170 | Binauri | `com.binauri.app` | Unlock Binauri Plus+ and all of its features. - If possible, remove the need for log - in. | 1 | [#778](https://github.com/hoo-dles/morphe-patches/discussions/778) |
| 171 | Bitepal | `com.pookies.food.ai` | Unlock premium | 1 | [#770](https://github.com/hoo-dles/morphe-patches/discussions/770) |
| 172 | BLE Scanner (Connect & Notify) | `com.macdom.ble.blescanner` | Unlock premium Remove ads Block Telemetry | 1 | [#659](https://github.com/hoo-dles/morphe-patches/discussions/659) |
| 173 | Bluetooth Finder and Scanner | `com.pzolee.bluetoothscanner` | Unlock premium Remove ads Block Telemetry | 1 | [#660](https://github.com/hoo-dles/morphe-patches/discussions/660) |
| 174 | Bumpy internationell dating | `app.bumpy.android` | Unlock all pro features | 1 | [#926](https://github.com/hoo-dles/morphe-patches/discussions/926) |
| 175 | C5 Now | `com.fourthwall.wla.channel5` | Not specified | 1 | [#1012](https://github.com/hoo-dles/morphe-patches/discussions/1012) |
| 176 | Car Nebula Launcher | `com.khaledisim.carlauncher` | Unlock all Features / offline Features From the app | 1 | [#1084](https://github.com/hoo-dles/morphe-patches/discussions/1084) |
| 177 | Carb Manager | `com.wombatapps.carbmanager` | Unlock Premium | 1 | [#702](https://github.com/hoo-dles/morphe-patches/discussions/702) |
| 178 | CardPointers | `com.cardpointers.app` | Unlock premium - Remove ads - Block telemetry | 1 | [#1027](https://github.com/hoo-dles/morphe-patches/discussions/1027) |
| 179 | Carista OBD2 Car Scanner | `com.prizmos.carista` | Unlock Premium Disable License check | 1 | [#1044](https://github.com/hoo-dles/morphe-patches/discussions/1044) |
| 180 | Carrom Pool: Disc Game | `com.miniclip.carrom` | unlock premium - ads free rewards - unlimited gems and coins - auto AIM | 1 | [#791](https://github.com/hoo-dles/morphe-patches/discussions/791) |
| 181 | chargemap | `com.chargemap_beta.android` | Unlock premium | 1 | [#772](https://github.com/hoo-dles/morphe-patches/discussions/772) |
| 182 | Class Dojo | `com.classdojo.android` | Unlock premium - Block ads | 1 | [#939](https://github.com/hoo-dles/morphe-patches/discussions/939) |
| 183 | CLZ Games: video game database | `com.collectorz.javamobile.android.games` | Unlock subscription | 1 | [#707](https://github.com/hoo-dles/morphe-patches/discussions/707) |
| 184 | Cronometer | `com.cronometer.android.gold_4.58.1` | Unlock premium | 1 | [#831](https://github.com/hoo-dles/morphe-patches/discussions/831) (closed) |
| 185 | Cubasis LE 3 Trial | `com.steinberg.cubasisle3` | Unlock premium Remove ads Block Telemetry | 1 | [#655](https://github.com/hoo-dles/morphe-patches/discussions/655) |
| 186 | DailyArt - Daily Dose of Art | `com.moiseum.dailyart2` | Unlock premium - Remove ads | 1 | [#571](https://github.com/hoo-dles/morphe-patches/discussions/571) |
| 187 | DC UNIVERSE INFINITE | `com.wb.goog.dcuniverse` | Unlock premium, block ads | 1 | [#899](https://github.com/hoo-dles/morphe-patches/discussions/899) |
| 188 | Deepstash: Smarter Every Day! | `com.deepstash` | Unlock Premium features - Access premium ideas and content - Remove subscription restricti… | 1 | [#801](https://github.com/hoo-dles/morphe-patches/discussions/801) |
| 189 | Discovery Plus Android TV | `com.discovery.discoveryplus.firetv_17.12.5` | Unlock premium Ad block | 1 | [#823](https://github.com/hoo-dles/morphe-patches/discussions/823) |
| 190 | Driver Card Reader Pro+ | `eu.lobol.drivercardreader_proplus` | Unlock premium | 1 | [#909](https://github.com/hoo-dles/morphe-patches/discussions/909) |
| 191 | DzynMode | `com.dzynmode` | Not specified | 1 | [#819](https://github.com/hoo-dles/morphe-patches/discussions/819) |
| 192 | Earthcam 3 | `com.earthcam.earthcamtv.android` | Unlock premium to bypass time restriction | 1 | [#963](https://github.com/hoo-dles/morphe-patches/discussions/963) |
| 193 | Elevate - Brain Training Games | `com.wonder` | Unlock premium | 1 | [#709](https://github.com/hoo-dles/morphe-patches/discussions/709) |
| 194 | EverythingWeather | `com.corymottice.wiseweather` | Unlock premium | 1 | [#1022](https://github.com/hoo-dles/morphe-patches/discussions/1022) |
| 195 | exFAT/NTFS for USB by Paragon | `com.paragon.tcplugins_ntfs_ro` | Unlock premium | 1 | [#1075](https://github.com/hoo-dles/morphe-patches/discussions/1075) |
| 196 | Feedly | `com.devhd.feedly` | Unlock premium (allow for more categories, sort by popularity) | 1 | [#1078](https://github.com/hoo-dles/morphe-patches/discussions/1078) |
| 197 | Firefox Focus: No Fuss Browser | `org.mozilla.focus` | add uBlock Origin | 1 | [#1081](https://github.com/hoo-dles/morphe-patches/discussions/1081) |
| 198 | FitHero | `com.fnp.fithero` | Unlock Premium - Block Ads - Disable Telemetry | 1 | [#755](https://github.com/hoo-dles/morphe-patches/discussions/755) |
| 199 | Flirtmetrics: Cold Approach | `com.flirtmetrics.app` | Unlock premium | 1 | [#765](https://github.com/hoo-dles/morphe-patches/discussions/765) |
| 200 | Fluent Weather Komponent KWGT | `dev.fluentkomp.astudio` | Paid app unlock | 1 | [#811](https://github.com/hoo-dles/morphe-patches/discussions/811) |
| 201 | Frez (formerly ClimbHarder) | `com.holdtight.climbharder` | Unlock Frez Pro | 1 | [#1057](https://github.com/hoo-dles/morphe-patches/discussions/1057) |
| 202 | Galaxy MaxHz | `com.tribalfs.gmh` | unlock premium | 1 | [#1085](https://github.com/hoo-dles/morphe-patches/discussions/1085) |
| 203 | getAbstract: Book Summaries | `com.soxes.getAbstract` | Unlock Premium features - Access the full 25,000+ summary library - Unlock text and audio … | 1 | [#803](https://github.com/hoo-dles/morphe-patches/discussions/803) |
| 204 | GGPO | `com.ggpoz.app` | Remove savestate limit. - Remove ads. - Possibly being able to view match history and chec… | 1 | [#649](https://github.com/hoo-dles/morphe-patches/discussions/649) |
| 205 | H2D DAB | `dabpumps.h2d` | Unlock premium | 1 | [#768](https://github.com/hoo-dles/morphe-patches/discussions/768) |
| 206 | Hiiker | `com.waymarkedtrails.hiiker` | Unlock premium Access to all features. | 1 | [#646](https://github.com/hoo-dles/morphe-patches/discussions/646) |
| 207 | Horos — Birth chart & astrology app | `com.horos.horos` | Unlock premium Remove telemetry & trackers if available | 1 | [#965](https://github.com/hoo-dles/morphe-patches/discussions/965) |
| 208 | Hush | `com.nickdegs.hush` | *  Hush (Anonymous Chat) * **Play Store | 1 | [#1053](https://github.com/hoo-dles/morphe-patches/discussions/1053) |
| 209 | IMAIOS e-Anatomy | `net.imaios.eanatomy` | unlock premium | 1 | [#999](https://github.com/hoo-dles/morphe-patches/discussions/999) |
| 210 | IrCode finder | `ru.wasiliysoft.ircodefindernec` | unlock premium - remove ads | 1 | [#739](https://github.com/hoo-dles/morphe-patches/discussions/739) |
| 211 | iShala | `com.swarsystems.ishala` | Not specified | 1 | [#326](https://github.com/hoo-dles/morphe-patches/discussions/326) |
| 212 | Japster | `com.sudeepbudhamagar.japanesetest` | Since, doesnot have in - app purchases. - Bypass Ads to view contents | 1 | [#796](https://github.com/hoo-dles/morphe-patches/discussions/796) |
| 213 | JLPT N1-N5 AI Mock Exam & Prep | `com.appkey.jlpt` | Unlock Premium | 1 | [#795](https://github.com/hoo-dles/morphe-patches/discussions/795) |
| 214 | JLPT Test Prep: N5 to N1 | `com.nextkhit.jlpt` | Unlock Premium | 1 | [#792](https://github.com/hoo-dles/morphe-patches/discussions/792) |
| 215 | JLPT Test: N5 - N1 | `com.truatvl.jlpttest` | Unlock Premium | 1 | [#793](https://github.com/hoo-dles/morphe-patches/discussions/793) |
| 216 | Keep Me Out | `vikesh.dass.lockmeout` | Unlock premium - Remove ads | 1 | [#1002](https://github.com/hoo-dles/morphe-patches/discussions/1002) |
| 217 | Kira | `com.kiramyapp.kira` | Unlock premium | 1 | [#560](https://github.com/hoo-dles/morphe-patches/discussions/560) |
| 218 | Kiwi.ai | `com.kiwinote.app` | Unlock Kiwi Pro | 1 | [#978](https://github.com/hoo-dles/morphe-patches/discussions/978) |
| 219 | Kontto: Finanças Pessoais | `com.finapp.app` | Unlock Premium | 1 | [#1055](https://github.com/hoo-dles/morphe-patches/discussions/1055) |
| 220 | L'ÉQUIPE - actu, SCORE, match | `lequipe.fr` | Removes ads, block telemetry | 1 | [#601](https://github.com/hoo-dles/morphe-patches/discussions/601) |
| 221 | Langua: AI language learning | `com.strukturedkaos.languatalkapp` | Unlock Langua Pro subscription | 1 | [#705](https://github.com/hoo-dles/morphe-patches/discussions/705) |
| 222 | language learning Aloa | `com.getaloa.app` | All pro features | 1 | [#925](https://github.com/hoo-dles/morphe-patches/discussions/925) |
| 223 | Larvel Player: Music Player | `com.larvelplayer.app` | Unlock all features. (In official app lot features and settings can be only accessed or en… | 1 | [#910](https://github.com/hoo-dles/morphe-patches/discussions/910) |
| 224 | Liftoff - Ranked gym workouts | `Com.gymbros.app` | Unlock premium features if possible Complete body rank access Unlimited data history Ad fr… | 1 | [#953](https://github.com/hoo-dles/morphe-patches/discussions/953) |
| 225 | Liftosaur | `com.liftosaur.www.twa` | Unlock Premium | 1 | [#897](https://github.com/hoo-dles/morphe-patches/discussions/897) |
| 226 | Linus | `com.actorskit.linus` | Unlock Premium | 1 | [#684](https://github.com/hoo-dles/morphe-patches/discussions/684) |
| 227 | Live Wallpapers by Horizon | `com.seresk.horizon.realtime.live.wallpapers` | Unlock all wallpapers/IAP. | 1 | [#887](https://github.com/hoo-dles/morphe-patches/discussions/887) |
| 228 | LoveLust: Sexual health | `h_0.8.0_apkcombo.com.xapk` | Unlock premium | 1 | [#1029](https://github.com/hoo-dles/morphe-patches/discussions/1029) |
| 229 | mail.com; email app and cloud | `com.mail.mobile.android.mail` | Please remove ads. | 1 | [#1003](https://github.com/hoo-dles/morphe-patches/discussions/1003) |
| 230 | ManaBox | `skilldevs.com.manabox` | Unlock Pro | 1 | [#912](https://github.com/hoo-dles/morphe-patches/discussions/912) |
| 231 | Mango | `com.wheat.mango` | Free diamond - level up | 1 | [#984](https://github.com/hoo-dles/morphe-patches/discussions/984) |
| 232 | Mapquest | `com.mapquest.android.ace` | unlock premium | 1 | [#951](https://github.com/hoo-dles/morphe-patches/discussions/951) |
| 233 | Metronome Beats | `com.andymstone.metronome` | Block Telemetry if any is found, and Unlock Premium, including: - Remove Ads - Save your s… | 1 | [#973](https://github.com/hoo-dles/morphe-patches/discussions/973) |
| 234 | Microsoft Outlook | `com.microsoft.office.outlook` | Disable Intune (com.microsoft.windowsintune.companyportal) dependency completely - Disable… | 1 | [#1042](https://github.com/hoo-dles/morphe-patches/discussions/1042) |
| 235 | Mindroid | `com.urbandroid.mind` | Unlock Premium - Remove Ads | 1 | [#1037](https://github.com/hoo-dles/morphe-patches/discussions/1037) |
| 236 | MobileSheets | `com.zubersoft.mobilesheetspro` | Unlock Pro Features | 1 | [#937](https://github.com/hoo-dles/morphe-patches/discussions/937) |
| 237 | MoreSpace | `com.morespace.app` | Unlock Lifetime Access | 1 | [#974](https://github.com/hoo-dles/morphe-patches/discussions/974) |
| 238 | Motoscan | `de.wgsoft.motoscan` | unlock premium | 1 | [#949](https://github.com/hoo-dles/morphe-patches/discussions/949) |
| 239 | My Lightning Tracker & Alerts | `com.jrustonapps.mylightningtracker` | Hide ads | 1 | [#442](https://github.com/hoo-dles/morphe-patches/discussions/442) |
| 240 | My Wallpapers | `dev.altmann.mywallpapers` | Unlock Full Version: unlock unlimited photo imports | 1 | [#696](https://github.com/hoo-dles/morphe-patches/discussions/696) |
| 241 | MyNissan Canada | `ca.nissan.nissanconnectservices` | Unlock all premium features | 1 | [#1058](https://github.com/hoo-dles/morphe-patches/discussions/1058) |
| 242 | Nerva | `com.mindsethealth.ibs` | Unlock subscription | 1 | [#788](https://github.com/hoo-dles/morphe-patches/discussions/788) |
| 243 | NetShort | `com.netshort.abroad` | Unlock premium - Remove ads | 1 | [#936](https://github.com/hoo-dles/morphe-patches/discussions/936) |
| 244 | Network Analyzer | `net.techet.netanalyzerlite.an` | Unlock premium - Remove ads - Block Telemetry | 1 | [#651](https://github.com/hoo-dles/morphe-patches/discussions/651) (closed) |
| 245 | Notebook - note taking & AI | `com.zoho.notebook` | unlock premium features This is the only feature i would like to request. These features a… | 1 | [#500](https://github.com/hoo-dles/morphe-patches/discussions/500) |
| 246 | Notifybuddy | `com.xander.android.notifybuddy` | unlock premium | 1 | [#946](https://github.com/hoo-dles/morphe-patches/discussions/946) |
| 247 | Nxsha | `nxsha.app` | Unlock Subscription - Remove All Ads - Remove Unwanted PopUp | 1 | [#682](https://github.com/hoo-dles/morphe-patches/discussions/682) |
| 248 | OLED Control | `com.tully.ohledcontrol` | Unlock Premium | 1 | [#982](https://github.com/hoo-dles/morphe-patches/discussions/982) |
| 249 | Oto Music | `com.piyush.music` | Unlock Oto Music+ | 1 | [#807](https://github.com/hoo-dles/morphe-patches/discussions/807) |
| 250 | Oxford Advanced Learner's Dictionary | `com.oup.elt.oald10_gp` | Paid Features Unlocked - AOSP Compatible / No Google - Block Telemetry | 1 | [#1064](https://github.com/hoo-dles/morphe-patches/discussions/1064) |
| 251 | PedalPro - Fast Bike Fit | `at.steinbacher.posiclist` | Unlock premium access | 1 | [#762](https://github.com/hoo-dles/morphe-patches/discussions/762) |
| 252 | PhonoFilm | `phonofilm.net` | Unlock premium Remkve ads | 1 | [#764](https://github.com/hoo-dles/morphe-patches/discussions/764) |
| 253 | Physics Wallah | `xyz.penpencil.physicswala` | Not specified | 1 | [#307](https://github.com/hoo-dles/morphe-patches/discussions/307) |
| 254 | Plotra | `com.plotra.app` | Unlock Premium - Remove Ads - Block Telemetry | 1 | [#730](https://github.com/hoo-dles/morphe-patches/discussions/730) |
| 255 | Prezzi Benzina | `org.vernazza.androidfuel` | Unlock premium | 1 | [#852](https://github.com/hoo-dles/morphe-patches/discussions/852) |
| 256 | Pro Launcher | `app.prolauncher` | unlock premium | 1 | [#1007](https://github.com/hoo-dles/morphe-patches/discussions/1007) |
| 257 | Pushbullet | `com.pushbullet.android` | Unlock premium (Dark Mode) | 1 | [#605](https://github.com/hoo-dles/morphe-patches/discussions/605) |
| 258 | R Discovery: Academic Research | `com.rdiscovery` | Unlock Premium (Prime) Bypass register/login requirement to unlock access to all free feat… | 1 | [#920](https://github.com/hoo-dles/morphe-patches/discussions/920) |
| 259 | Radarbot | `com.vialsoft.radarbot_free` | Remove ads, unlock premium | 1 | [#1076](https://github.com/hoo-dles/morphe-patches/discussions/1076) |
| 260 | Radio Adfree | `de.beffy.radiohero` | Unlock premium | 1 | [#1046](https://github.com/hoo-dles/morphe-patches/discussions/1046) |
| 261 | radio.net PRIME | `de.radio.android.prime` | Unlock Premium - Remove Ads - Block Telemetry | 1 | [#519](https://github.com/hoo-dles/morphe-patches/discussions/519) |
| 262 | Rosetta Stone Sapphire | `com.rosettastone.sapphire` | Unlock the paid lessons and language content without a subscription. | 1 | [#806](https://github.com/hoo-dles/morphe-patches/discussions/806) |
| 263 | S3Drive | `com.s3.drive.file.explorer.storage.cloud.manager` | Unlock clint side features unlock - lest user to login in all reclone servers. - Remove an… | 1 | [#829](https://github.com/hoo-dles/morphe-patches/discussions/829) |
| 264 | say budget app | `com.moments.expenses` | unlock premium | 1 | [#691](https://github.com/hoo-dles/morphe-patches/discussions/691) |
| 265 | Scrabble GO - Fun with Words | `com.pieyel.scrabble` | Remove ads | 1 | [#960](https://github.com/hoo-dles/morphe-patches/discussions/960) |
| 266 | Screen Suite | `com.cozyme.app.screenoff` | Unlock Premium - Block Telemetry | 1 | [#1018](https://github.com/hoo-dles/morphe-patches/discussions/1018) |
| 267 | Seneca - KS3, GCSE & A Level | `com.senecalearning.app` | unlock premium | 1 | [#977](https://github.com/hoo-dles/morphe-patches/discussions/977) |
| 268 | Setanta sports: Sports TV app | `com.setanta.app` | Unlock Premium Remove Ads Block Telemetry | 1 | [#927](https://github.com/hoo-dles/morphe-patches/discussions/927) |
| 269 | Shifter - shift calendar | `com.idevband.shiftcalendar` | Unlock premium - Remove ads - Block telemetry | 1 | [#841](https://github.com/hoo-dles/morphe-patches/discussions/841) |
| 270 | SimpleMind Pro | `com.modelmakertools.simplemindpro` | Unlocking premium - Device sync | 1 | [#945](https://github.com/hoo-dles/morphe-patches/discussions/945) |
| 271 | Simurelay | `com.mordiastudio.simurelayfree` | Unlock Premium | 1 | [#794](https://github.com/hoo-dles/morphe-patches/discussions/794) |
| 272 | sleep² sleeptracker & analysis | `com.nukkuaa.app` | detailed sleep analysis & reports - individual sleep goals & progress tracking | 1 | [#700](https://github.com/hoo-dles/morphe-patches/discussions/700) |
| 273 | SmartyMe | `com.app.smartyme` | Not specified | 1 | [#746](https://github.com/hoo-dles/morphe-patches/discussions/746) |
| 274 | Solid Starts | `com.digitas.solidstarts` | Unlock Premium - Enable dark mode - Remember Allergens Filter | 1 | [#1059](https://github.com/hoo-dles/morphe-patches/discussions/1059) |
| 275 | soul knight | `com.ChillyRoom.DungeonShooter` | Not sure if possible or not but - make the game offline (skip the login bullshit) - unlock… | 1 | [#826](https://github.com/hoo-dles/morphe-patches/discussions/826) |
| 276 | Sport Track Merger | `com.bvic.sporttrackmerger` | Unlock Premium | 1 | [#635](https://github.com/hoo-dles/morphe-patches/discussions/635) |
| 277 | Sports Tracker | `com.stt.android` | Unlock Premium | 1 | [#1051](https://github.com/hoo-dles/morphe-patches/discussions/1051) |
| 278 | Stay Focused: Site/App Blocker | `com.stayfocused` | Unlock Premium | 1 | [#1074](https://github.com/hoo-dles/morphe-patches/discussions/1074) |
| 279 | Studocu | `com.studocu.app` | Not specified | 1 | [#904](https://github.com/hoo-dles/morphe-patches/discussions/904) |
| 280 | substack | `com.substack.app` | Unblock premium Remove ads Remove teletary Remove for money or pledge Follow with one butt… | 1 | [#918](https://github.com/hoo-dles/morphe-patches/discussions/918) |
| 281 | Sync.me - Spam Call Blocker | `com.syncme.syncmeapp` | Unlock premium | 1 | [#1048](https://github.com/hoo-dles/morphe-patches/discussions/1048) |
| 282 | Taber's Medical Dictionary (Unbound Medi | `com.taber.medical.dictionary` | Unlock behind paywall | 1 | [#543](https://github.com/hoo-dles/morphe-patches/discussions/543) (closed) |
| 283 | Tabers Medical Dictionary | `com.unboundmedicine.taber` | Unlock paywall | 1 | [#544](https://github.com/hoo-dles/morphe-patches/discussions/544) |
| 284 | Tenmin | `ai.tenmin.app` | Unlock premium - Block Premium Upsell | 1 | [#901](https://github.com/hoo-dles/morphe-patches/discussions/901) |
| 285 | Tezza pro version | `org.tezza` | Unlock Premium - Remove payment Option on everything | 1 | [#594](https://github.com/hoo-dles/morphe-patches/discussions/594) |
| 286 | The guardian UK & world news | `com.guardian` | unlock the article limit on the app - not subscribed limited to few articles | 1 | [#599](https://github.com/hoo-dles/morphe-patches/discussions/599) |
| 287 | The School of Life | `com.theschooloflife.app` | Unlock Premium content - Access the full library of audio, podcasts, videos and essays - U… | 1 | [#805](https://github.com/hoo-dles/morphe-patches/discussions/805) |
| 288 | TheaterEars | `TheaterEars.com` | Remove ads Unlock premium | 1 | [#1054](https://github.com/hoo-dles/morphe-patches/discussions/1054) |
| 289 | Tiny Decisions | `com.nixwang.tinydecisions` | Unlock Pro - Remove Ads | 1 | [#786](https://github.com/hoo-dles/morphe-patches/discussions/786) |
| 290 | Tracker Network | `network.tracker.app` | Unlock premium - Remove ads - Block telemetry | 1 | [#1030](https://github.com/hoo-dles/morphe-patches/discussions/1030) |
| 291 | Tracking MY | `my.com.tracking.app` | Removes all ads and premium features if possible Thanks! | 1 | [#882](https://github.com/hoo-dles/morphe-patches/discussions/882) |
| 292 | Trailforks | `com.pinkbike.trailforks` | Unlock PRO or infinite Trail period | 1 | [#906](https://github.com/hoo-dles/morphe-patches/discussions/906) |
| 293 | Transparent clock and weather | `com.droid27.transparentclockweather` | unlock premium - block ads - block telemetry | 1 | [#1033](https://github.com/hoo-dles/morphe-patches/discussions/1033) |
| 294 | True Phone | `com.hb.dialer.free` | Remove ads (lifetime) | 1 | [#1060](https://github.com/hoo-dles/morphe-patches/discussions/1060) |
| 295 | Universal file Opener | `com.krauq.universalfileopener` | unlock premium | 1 | [#1032](https://github.com/hoo-dles/morphe-patches/discussions/1032) |
| 296 | Vetcalculators | `com.vetcalculators.android` | unlock premium | 1 | [#903](https://github.com/hoo-dles/morphe-patches/discussions/903) |
| 297 | VidHub: Video Player & Library | `com.oumi.utility.media.hub` | Unlock Pro/Premium | 1 | [#1072](https://github.com/hoo-dles/morphe-patches/discussions/1072) |
| 298 | Water Tracker: WaterMinder app | `com.funnmedia.waterminder` | Unlock Premium | 1 | [#771](https://github.com/hoo-dles/morphe-patches/discussions/771) |
| 299 | WaveEditor Record & Edit Audio | `io.sbaud.wavstudio` | Unlock Pro features - Remove ads - Remove billing - Remove Google stuff | 1 | [#692](https://github.com/hoo-dles/morphe-patches/discussions/692) |
| 300 | WeatherBug: Weather Forecast | `com.aws.android` | Remove ads | 1 | [#704](https://github.com/hoo-dles/morphe-patches/discussions/704) |
| 301 | WorldBox | `com.mkarpenko.worldbox` | Not specified | 1 | [#1004](https://github.com/hoo-dles/morphe-patches/discussions/1004) |
| 302 | Xeropan | `com.application.xeropan` | Unlock Pro version | 1 | [#916](https://github.com/hoo-dles/morphe-patches/discussions/916) |
| 303 | Yahoo Sports: Scores and News | `com.yahoo.mobile.client.android.sportacular` | Remove ads, including in articles. Remove references to gambling (odds, references, etc). … | 1 | [#886](https://github.com/hoo-dles/morphe-patches/discussions/886) |
| 304 | Yoga Studio: Poses & Classes | `com.gaiam.yogastudio` | Unlock Premium/Pro - Piad membership | 1 | [#587](https://github.com/hoo-dles/morphe-patches/discussions/587) |
| 305 | Zuckoff | `com.szydlowsky.zuckoff` | unlock premium | 1 | [#948](https://github.com/hoo-dles/morphe-patches/discussions/948) |

**Package not given (73, find the package first):** [Deezer](https://github.com/hoo-dles/morphe-patches/discussions/236), [TradingView](https://github.com/hoo-dles/morphe-patches/discussions/281) (closed), [Apple Music](https://github.com/hoo-dles/morphe-patches/discussions/262), [Lightroom](https://github.com/hoo-dles/morphe-patches/discussions/249) (closed), [Microsoft Excel](https://github.com/hoo-dles/morphe-patches/discussions/247) (closed), [JioHotstar](https://github.com/hoo-dles/morphe-patches/discussions/234) (closed), [LingQ](https://github.com/hoo-dles/morphe-patches/discussions/499), [Microsoft Teams](https://github.com/hoo-dles/morphe-patches/discussions/266) (closed), [Grindr](https://github.com/hoo-dles/morphe-patches/discussions/299), [Backdrops - Wallpapers](https://github.com/hoo-dles/morphe-patches/discussions/263) (closed), [Pimsleur](https://github.com/hoo-dles/morphe-patches/discussions/260), [Notewise](https://github.com/hoo-dles/morphe-patches/discussions/244), [Babbel](https://github.com/hoo-dles/morphe-patches/discussions/391) (closed), [Bend: Stretching & Flexibility](https://github.com/hoo-dles/morphe-patches/discussions/256) (closed), [ZEE5](https://github.com/hoo-dles/morphe-patches/discussions/238), [Character.AI](https://github.com/hoo-dles/morphe-patches/discussions/302), [Oxford Dictionary & Thesaurus](https://github.com/hoo-dles/morphe-patches/discussions/235), [Ground News](https://github.com/hoo-dles/morphe-patches/discussions/329), [Bluetooth Keyboard and Mouse](https://github.com/hoo-dles/morphe-patches/discussions/320) (closed), [Wibuku](https://github.com/hoo-dles/morphe-patches/discussions/233), [OpenSnow](https://github.com/hoo-dles/morphe-patches/discussions/917), [AppLock Pro](https://github.com/hoo-dles/morphe-patches/discussions/328) (closed), [Zfont 3](https://github.com/hoo-dles/morphe-patches/discussions/322), [AppBlock](https://github.com/hoo-dles/morphe-patches/discussions/305), [Text Expander - Texpand](https://github.com/hoo-dles/morphe-patches/discussions/288), [The Hindu](https://github.com/hoo-dles/morphe-patches/discussions/275), [Tandem](https://github.com/hoo-dles/morphe-patches/discussions/257), [Xtream Player](https://github.com/hoo-dles/morphe-patches/discussions/1006), [Aloha Browser](https://github.com/hoo-dles/morphe-patches/discussions/848), [MEMRISE](https://github.com/hoo-dles/morphe-patches/discussions/447), [vFlat Scanner Premium](https://github.com/hoo-dles/morphe-patches/discussions/333), [MuscleWiki](https://github.com/hoo-dles/morphe-patches/discussions/291), [Mark - Auto Screenshot deleter](https://github.com/hoo-dles/morphe-patches/discussions/283) (closed), [India Map Quick](https://github.com/hoo-dles/morphe-patches/discussions/280), [Times of India](https://github.com/hoo-dles/morphe-patches/discussions/274), [Zoom Earth](https://github.com/hoo-dles/morphe-patches/discussions/261), [Stay Focused](https://github.com/hoo-dles/morphe-patches/discussions/1073) (closed), [Bing](https://github.com/hoo-dles/morphe-patches/discussions/1065), [Spaceteam](https://github.com/hoo-dles/morphe-patches/discussions/1005), [Notein](https://github.com/hoo-dles/morphe-patches/discussions/959), [Stream player 1.7.20](https://github.com/hoo-dles/morphe-patches/discussions/958), [Patch request for Apple Music](https://github.com/hoo-dles/morphe-patches/discussions/943) (closed), [ScoreBox](https://github.com/hoo-dles/morphe-patches/discussions/919), [Flowmarket](https://github.com/hoo-dles/morphe-patches/discussions/911), [OnSkin](https://github.com/hoo-dles/morphe-patches/discussions/836), [CLZ Comics](https://github.com/hoo-dles/morphe-patches/discussions/833), [GOWOD - Mobility & Stretching](https://github.com/hoo-dles/morphe-patches/discussions/733) (closed), [EF Hello](https://github.com/hoo-dles/morphe-patches/discussions/643), [SpanishDictionary.com](https://github.com/hoo-dles/morphe-patches/discussions/506), [Dreaming - Language Learning](https://github.com/hoo-dles/morphe-patches/discussions/498), [Disable Play Store updates (port from Mo](https://github.com/hoo-dles/morphe-patches/discussions/472) (closed), [Pimsleur / Language Learning](https://github.com/hoo-dles/morphe-patches/discussions/459) (closed), [Status AI](https://github.com/hoo-dles/morphe-patches/discussions/448), [Premium patch for InShot Photo Editor Po](https://github.com/hoo-dles/morphe-patches/discussions/392) (closed), [Mediaset Infinity](https://github.com/hoo-dles/morphe-patches/discussions/338), [DoorVi](https://github.com/hoo-dles/morphe-patches/discussions/334), [BAM! - Imposter & Party Games](https://github.com/hoo-dles/morphe-patches/discussions/327), [TaskForge](https://github.com/hoo-dles/morphe-patches/discussions/315), [Brave Browser Origin](https://github.com/hoo-dles/morphe-patches/discussions/314) (closed), [PayRange](https://github.com/hoo-dles/morphe-patches/discussions/310), [Sush](https://github.com/hoo-dles/morphe-patches/discussions/303), [Leo Dictionary](https://github.com/hoo-dles/morphe-patches/discussions/300), [Insight Timer](https://github.com/hoo-dles/morphe-patches/discussions/293), [PinSaver](https://github.com/hoo-dles/morphe-patches/discussions/282), [Chineasy](https://github.com/hoo-dles/morphe-patches/discussions/279) (closed), [SplitMate](https://github.com/hoo-dles/morphe-patches/discussions/277), [Box Box Club](https://github.com/hoo-dles/morphe-patches/discussions/276) (closed), [Cinexplore](https://github.com/hoo-dles/morphe-patches/discussions/271), [Now Playing](https://github.com/hoo-dles/morphe-patches/discussions/267), [Driversnote](https://github.com/hoo-dles/morphe-patches/discussions/265), [Gaana](https://github.com/hoo-dles/morphe-patches/discussions/246), [ClassDojo](https://github.com/hoo-dles/morphe-patches/discussions/243), [Tubi](https://github.com/hoo-dles/morphe-patches/discussions/239) (closed)


## Open GitHub issues (2026-10-14)

From [the issue tracker](https://github.com/heval99/Heval-Morphe-Patches/issues). Truecaller (shipped 2026-10-13) has four of the five, so it goes first.

| Issue | App | Type | Notes |
|---|---|---|---|
| [#54](https://github.com/heval99/Heval-Morphe-Patches/issues/54) | Truecaller | Bug | Repeated "your premium expired" notifications, random freezes and crashes; after a clean install, spam calls are still not blocked |
| [#52](https://github.com/heval99/Heval-Morphe-Patches/issues/52) | Truecaller | Bug | Spam Protection falls back from Max to Basic; "Servers can't be reached" toasts; Premium tab stuck loading; Frequently called contacts enabled with its setting hidden (`isFrequentlyCalledVisible` false, per a commenter); **T9 dialer search finds no saved contacts**; unasked contact-UI, fraud-protection and SMS-tab changes. Likely the server treating the patched app as non-premium or modified (shared cause with #54) plus separate client bugs (T9, frequently called) |
| [#53](https://github.com/heval99/Heval-Morphe-Patches/issues/53) | Truecaller | Request | Hide the Blocking, Family invite and Premium tabs; block the tracking that flags the app as modified ("install from Play Store"). Owner replied "Looking into it" |
| [#50](https://github.com/heval99/Heval-Morphe-Patches/issues/50) | Truecaller | Request | **Declined 2026-10-14** - Truecaller hard-codes `content://com.truecaller` and the `com.truecaller.account` type, registration is attested by package, and dialer/call-screening roles have one holder. Replied; our broken GmsCore "Clone" value is being removed for Truecaller |
| [#55](https://github.com/heval99/Heval-Morphe-Patches/issues/55) | BoxBox | Bug | "Neither Disable ads nor Enable Premium works" after a clean install. Matches the 5.4.17 breakage fixed in 71bb868 (dev pre-release v1.14.0-dev.5 and later; emulator-verified); stable v1.13.0 still ships the old patches. Confirm the reporter's bundle and BoxBox versions |
| [#51](https://github.com/heval99/Heval-Morphe-Patches/issues/51) | Duplila - Mirror Screen | Request | **Shipped 2026-10-14 (5.1)** - Enable Premium, Disable ads, Disable license check. Not runtime-tested (armeabi-v7a only) |
