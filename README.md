# 👋🧩 heval patches

<p align="center">
  <a href="https://morphe.software/add-source?github=heval99/Heval-Morphe-Patches">
    <img src="https://img.shields.io/badge/%E2%9E%95%20%20Add%20to%20Morphe-1E5AA8?style=for-the-badge" height="46" alt="Add to Morphe">
  </a>
  <br>
  <sub>One tap adds this patch source to Morphe Manager</sub>
</p>

<p align="center">
  <a href="https://github.com/heval99/Heval-Morphe-Patches/releases/latest"><img src="https://img.shields.io/github/v/release/heval99/Heval-Morphe-Patches?label=stable&color=2ea44f" alt="Latest stable release"></a>
  <a href="https://github.com/heval99/Heval-Morphe-Patches/releases"><img src="https://img.shields.io/github/v/release/heval99/Heval-Morphe-Patches?include_prereleases&label=pre-release&color=orange" alt="Latest pre-release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPLv3-blue" alt="GPLv3"></a>
  <a href="https://ko-fi.com/heval99"><img src="https://img.shields.io/badge/Support%20me%20on-Ko--fi-72a4f2?logo=ko-fi&logoColor=white" alt="Support me on Ko-fi"></a>
  <a href="https://paypal.me/HevalXode"><img src="https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal&logoColor=white" alt="Donate with PayPal"></a>
</p>

Custom [Morphe](https://morphe.software) patches by heval99 for apps I use: ad removal,
telemetry blocking and premium/licence unlocks for sports, music, notes, email, maps and
utility apps.

## 🚀 Quick start

1. Install [Morphe Manager](https://morphe.software).
2. Tap **[Add to Morphe](https://morphe.software/add-source?github=heval99/Heval-Morphe-Patches)**,
   or add this repository as a patch source by hand:
   `https://github.com/heval99/Heval-Morphe-Patches`
3. Pick the app, choose the patches you want and patch.

**Stable or pre-release?** The stable channel follows the
[latest release](https://github.com/heval99/Heval-Morphe-Patches/releases/latest). Turn on
**Pre-release patches** for this source in Morphe Manager to get fixes and new apps a few days
earlier.

## 🩹 Patches list

Each app lists the versions its patches were verified on (shown in Morphe Manager). Other
versions may work but aren't guaranteed; if an app update breaks a patch,
[open an issue](https://github.com/heval99/Heval-Morphe-Patches/issues/new/choose).

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0](https://github.com/Abilash-22/Xerc/releases/tag/v1.0.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;**1** apps&nbsp;&nbsp;•&nbsp;&nbsp;**11** patches&nbsp;&nbsp;•&nbsp;&nbsp;updated 2026-11-05

🚫 ads&nbsp;&nbsp;🛡️ telemetry / tracking&nbsp;&nbsp;💎 premium / pro&nbsp;&nbsp;🔑 license check&nbsp;&nbsp;🔕 prompts / notifications&nbsp;&nbsp;🧪 experimental

⚽ Sports & scores

#### ⚽ Sports & scores <sub>1 app</sub>

| | App | Patches | Latest verified | Type | Google Play |
|:-:|---|---|---|:-:|:-:|
| <img src="assets/icons/in.cricketexchange.app.cricketexchange.png" width="20" height="20" alt=""> | **CREX** | 🚫 💎 🧩 11 | `26.10.09` | APKM | [Play](https://play.google.com/store/apps/details?id=in.cricketexchange.app.cricketexchange) |

### 🔍 Patch details

#### ⚽ Sports & scores

<details>
<summary><img src="assets/icons/in.cricketexchange.app.cricketexchange.png" width="16" height="16" alt=""> <b>CREX</b> &nbsp;·&nbsp; 11 patches &nbsp;·&nbsp; <code>in.cricketexchange.app.cricketexchange</code></summary>

Verified on: `26.10.09`

| Patch | What it does | Default |
|---|---|:-:|
| 🚫 **Disable ads** | Blocks every Google Mobile Ads load (banner, native, interstitial, rewarded, app-open and the networks mediated through it), CREX's own Parth house ads and Meta Audience Network banners. Rewarded videos never load, so their rewards are not granted; use with Enable Premium, which makes the app hide its ad slots. | ✅ |
| 💎 **Enable Premium** | Treats the CREX Premium subscription as active on this device, which removes ads (including in the floating score) wherever the app checks the plan. The CREX Premium menu entry still asks you to sign in and shows no plan, and server-checked content such as FanCode live streams stays locked. | ✅ |
| 🧩 **Hide Commentary poll** | Phase 1.1: collapses the poll cards (Who will win ...) in the Commentary tab. The rows are bound with a PollsViewHolder; they are hidden and given zero height instead of being bound. | ✅ |
| 🧩 **Hide For you extras** | Removes the Featured Series, More, Play Games and bottom search sections from the For you tab. The series and match list, the Follow card and the Fixtures button are not touched. | ✅ |
| 🧩 **Hide Info sections** | Phase 1.1: removes the Who will win poll, Series Stats, Team Comparison, Avg Scores on Venue, Recent Matches on Venue and Points Table sections from the match screen's Info tab, using the app's own state for matches that have no data for them. Venue Stats, weather, squads, umpires and the other sections are not touched. | ✅ |
| 🧩 **Hide Live tab poll, weather and head to head** | Phase 1.1: removes the Who will win poll, the Weather Conditions card and the Head to Head row from the match screen's Live tab (the pre-match list). The preview video and Top Headlines are not touched. | ✅ |
| 🧩 **Hide Live tab sections** | Phase 1.1: removes the quiz, Latest Videos and Gaming Zone sections from the match screen's Live tab (LiveMatchFragment). Top Headlines, weather and the other sections are not touched. | ✅ |
| 🧩 **Hide Matches extras** | Phase 1.1: removes the Trending Videos block (and its All Videos button) from the Matches Live tab, and the red NEW label and red dot over the bottom navigation. | ✅ |
| 🧩 **Hide match Discussions** | Phase 1.1: removes the Discussions tab and the floating Discuss button from the match screen by switching off the flag the app already uses when discussions are disabled for a match. | ✅ |
| 🧩 **Hide match tabs** | Phase 1.1: match screen keeps Info, Commentary, Scorecard and Points Table. Fantasy and Graphs are removed from the tab list. The Live tab (Match Summary on finished matches) and the Series Stats tab are kept in the pager but made invisible and moved last, because the rest of the screen calls into their fragments without null checks. | ✅ |
| 🧩 **Matches tabs in bottom navigation** | UI only. The Matches screen's top tabs (Live, For you, Upcoming, Finished) move into the bottom navigation bar and the top tab strip is hidden. The Matches item and the Settings item are gone from the bar; the Menu screen is reached through the top-right three-line button, which stays visible. The app always opens on Matches. The original Home, Social, Videos and Series items are reused for the four tabs (not removed) because HomeActivity looks them up by index. | ✅ |

</details>

<sub>✅ on by default&nbsp;&nbsp;➖ off by default (enable it in Morphe Manager)</sub>
<!-- PATCHES_END -->

### 🧪 Experimental patches

Some patches are marked **experimental** in Morphe Manager (for example Brave's *Disable
telemetry*, *Disable ads* and *Hide promotional prompts*). They are tested but newer, so
they are **off by default**; enable them yourself and report anything odd.

## ⚠️ Good to know

- **Only what the app decides locally can be unlocked.** Features served or checked by the
  developer's servers (cloud content, AI features, VPN servers, server-enforced limits) stay
  locked; patches don't fake them. Patch descriptions say when something is only partly
  unlocked.
- **Google sign-in may break.** Patched apps are re-signed, so "Sign in with Google" and some
  other Google services can stop working. Use email login where the app offers it.
- **Push notifications** in some apps need [MicroG](https://github.com/microg/GmsCore) with
  signature spoofing after patching.
- **Use the APK type Morphe Manager suggests.** Some apps are only published as split bundles
  (APKM); patch the full bundle, not just the base APK.

## 💬 Requests and bug reports

- **Something broke?** Open a
  [bug report](https://github.com/heval99/Heval-Morphe-Patches/issues/new/choose) with the app
  version, the patch bundle version and what you see.
- **Want an app patched?** Open a thread in the
  [Requests discussions](https://github.com/heval99/Heval-Morphe-Patches/discussions/categories/requests)
  (search first, and check the
  [blocklist](https://github.com/heval99/Heval-Morphe-Patches/blob/dev/docs/patch-blocklist.md)
  for apps that cannot be patched), or email hevalomevalo@gmail.com.

## 🤖 How the patches are made

> **AI-generated patches.** Every patch here is written with AI assistance: the code
> analysis, patch code and smoke tests are AI-generated. Before each release they are
> verified by applying them to the real APKs (smoke tests in `patches/src/test/`) and
> checking the patched code. Review before use; no warranty.

## 🛠️ Building

```
./gradlew :patches:buildAndroid
```

The bundle is written to `patches/build/libs/`. The Morphe Gradle plugin is downloaded from
GitHub Packages, so set `GITHUB_ACTOR` and `GITHUB_TOKEN` (a token with `read:packages`)
first. See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for
details.

## 🔗 Other projects

- **[Sideport](https://github.com/heval99/Sideport)**: a userscript that adds a **Get APK**
  button to every Google Play app page. It downloads directly from 6 sources with live
  availability checks and shows when Morphe patches exist for the app. No tracking.
  [Install it](https://github.com/heval99/sideport/raw/main/sideport.user.js) with
  Tampermonkey or Violentmonkey.

## 💙 Support

If these patches are useful to you, you can support the work:

[![Support me on Ko-fi](https://img.shields.io/badge/Support%20me%20on-Ko--fi-72a4f2?logo=ko-fi&logoColor=white&style=for-the-badge)](https://ko-fi.com/heval99)
[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal&logoColor=white&style=for-the-badge)](https://paypal.me/HevalXode)

## 📜 License

heval patches are licensed under the [GNU General Public License v3.0](LICENSE).
