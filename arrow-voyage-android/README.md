# Arrow Voyage — Android app (Play Store)

এই ফোল্ডারটা একটা সম্পূর্ণ Android Studio প্রজেক্ট। গেমটা `app/src/main/assets/www/` এ offline আছে (fonts সহ), আর একটা ছোট Kotlin WebView wrapper (`MainActivity.kt`) সেটা full-screen এ চালায়। গেম নিজে offline চলে; INTERNET permission শুধু AdMob ad এর জন্য (`Ads.kt`)।

Package name: `com.arrowvoyage.game` · versionCode 2 · versionName 1.1.0 · minSdk 24 (Android 7) · targetSdk 35

## 1. যা লাগবে (একবার)
1. **Android Studio** (সর্বশেষ): https://developer.android.com/studio — install করার সময় Android SDK আর SDK Platform 35 সাথে নেয়।
2. **JDK 17** Android Studio এর সাথেই আসে, আলাদা কিছু লাগবে না।
3. **Google Play Console account** ($25 একবার): https://play.google.com/console

## 2. Upload key বানাও (একবার, এটা হারিও না)
Terminal এ এই ফোল্ডারে গিয়ে:
```bash
keytool -genkeypair -v -keystore arrow-voyage-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
Password দাও এবং মনে রাখো। তারপর:
```bash
cp keystore.properties.example keystore.properties
```
`keystore.properties` এ password গুলো বসাও। `.jks` আর `keystore.properties` কখনো share/commit করবে না (`.gitignore` এ আছে)।

## 3. AAB build করো
**Android Studio দিয়ে:** File → Open → এই ফোল্ডার। Sync শেষ হলে Build → Generate Signed App Bundle / APK → Android App Bundle → তোমার keystore → release → Create. 
**অথবা terminal এ:** `./build-release.sh`

Output: `app/build/outputs/bundle/release/app-release.aab`

Phone এ test করতে চাইলে: Android Studio এ Run ▶ (debug build, phone USB debugging on থাকতে হবে)।

## AdMob (ad থেকে আয়)
গেমে দুই রকম ad আছে, দুটোই Google Mobile Ads SDK দিয়ে (`app/src/main/kotlin/com/arrowvoyage/game/Ads.kt`), গেমের সাথে JavaScript bridge `window.ArrowAds` দিয়ে যুক্ত:
- **Rewarded video (hint):** hint শেষ হলে hint বাটনে চাপ দিলে "Watch ad" আসে। পুরো ad দেখলে +1 hint পায় এবং সাথে সাথে hint দেখায়।
- **Interstitial (ad break):** level 15 এর পরে প্রতি ৩টা level জেতার পর একটা ad (প্রথমটা level 18, তারপর 21, 24 …)। Result screen এ "Next level"/"Replay" চাপলে ad দেখায়, ad বন্ধ হলে পরের level শুরু হয়। Ad load না হলে গেম ad ছাড়াই চলে যায়।
- EU/UK user দের জন্য Google এর consent form (UMP) নিজে থেকে আসে, আর Settings এ "Ad privacy" বাটন দেখায়।
- Browser এ (web version) কোনো ad নেই; সব ad call চুপচাপ কিছু করে না।

### ⚠️ ID বদলানোর জায়গা: শুধু একটা ফাইল
**`app/src/main/res/values/admob.xml`** — এখানে এখন Google এর **test ID** বসানো আছে (test ad দেখায়, ক্লিক করলেও সমস্যা নেই, কিন্তু টাকা আসে না)। Publish এর আগে তিনটা লাইনই নিজের ID দিয়ে বদলাও:
- `admob_app_id` → তোমার App ID (`ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy`, মাঝে `~`)
- `admob_interstitial_id` → Interstitial ad unit ID (মাঝে `/`)
- `admob_rewarded_id` → Rewarded ad unit ID (মাঝে `/`)

Manifest বা Kotlin এ কিছু বদলাতে হবে না। **নিজের আসল ad নিজে কখনো ক্লিক করবে না**, তাতে AdMob account ban হতে পারে। নিজের phone এ test করার সময় test ID রাখো, অথবা AdMob → Settings → Test devices এ নিজের phone যোগ করো।

### AdMob account (একবার)
1. https://admob.google.com এ Google account দিয়ে sign up, দেশ Bangladesh, timezone আর currency দাও।
2. **Apps → Add app** → Android → "Is the app listed on a supported app store?" প্রথমবার **No** (Play Store এ live হলে পরে link করবে) → নাম "Arrow Voyage"।
3. App ID কপি করে `admob.xml` এর `admob_app_id` এ বসাও।
4. **Ad units → Add ad unit → Interstitial** → নাম "Level break" → ID কপি করে `admob_interstitial_id` এ।
5. **Ad units → Add ad unit → Rewarded** → নাম "Hint" → reward 1, item "hint" → ID কপি করে `admob_rewarded_id` এ।
6. **Payments**: ঠিকানা, নাম আর পরে bank account (টাকা $100 হলে পাঠায়)। একটা PIN চিঠি ডাকে আসবে ঠিকানা verify করতে।
7. **Privacy & messaging → GDPR**: একটা message বানিয়ে publish করো (ছাড়া EU user রা ad পাবে না)। এখানে privacy policy URL লাগবে।
8. App Play Store এ live হলে AdMob এ app টা store এর সাথে link করো, আর **app-ads.txt**: তোমার developer website এর root এ (`https://yoursite.com/app-ads.txt`) AdMob এর দেওয়া এক লাইন রাখো, আর Play Console এ সেই website টা দাও। না রাখলে আয় কমে যায়।
9. নতুন account এ প্রথম কয়েক দিন ad কম আসতে পারে ("limited ad serving"), এটা স্বাভাবিক।

## 4. Play Console এ publish
1. Play Console → **Create app** → নাম "Arrow Voyage", Game, Free.
2. **Set up your app** checklist পুরো করো: Privacy policy (নীচে দেখো), App access (All functionality available without restrictions), **Ads: Yes, my app contains ads** (store এ "Contains ads" লেখা দেখাবে), **Advertising ID: Yes** (purpose: Advertising or marketing; SDK নিজেই `AD_ID` permission যোগ করে), Content rating questionnaire (Puzzle game → সাধারণত "Everyone"; ad আছে কিনা জিজ্ঞেস করলে Yes), Target audience (**13+ বেছে নাও**; 13 এর নিচে বাছলে Families policy লাগে, তখন শুধু family-certified ad দেওয়া যায় আর কাজ অনেক বেশি), Data safety (নীচে দেখো), Government apps: No, Financial features: No, Health: No.
3. **Store listing**: `store/icon-512.png`, `store/feature-graphic-1024x500.png`, আর phone screenshots কমপক্ষে 2টা (phone এ গেম চালিয়ে screenshot নাও)। Short description (80 chars) আর full description নীচে দেওয়া আছে।
4. **Testing → Internal testing** → Create release → `app-release.aab` upload → testers add করে নিজে test করো।
5. **Production** → Create release → একই AAB → Review → Start rollout. Review এ সাধারণত 1–7 দিন লাগে।

### Data safety (AdMob থাকায়)
"Does your app collect or share any of the required user data types?" → **Yes**। Google এর AdMob guide অনুযায়ী টিক দাও:
- **Location → Approximate location** (IP থেকে) — Collected + Shared, purpose: Advertising, Analytics, Fraud prevention
- **App activity → App interactions** — Collected + Shared, purpose: Advertising, Analytics
- **App info and performance → Crash logs, Diagnostics** — Collected + Shared, purpose: Analytics, Fraud prevention
- **Device or other IDs** (Advertising ID) — Collected + Shared, purpose: Advertising, Analytics, Fraud prevention
- "Is data encrypted in transit?" → Yes · "Can users request deletion?" → No (গেম নিজে কিছু রাখে না; ad data Google এর নিয়মে)
- Data "processed ephemerally"? → No · Collection optional? → No (ad এর জন্য লাগে)
গেমের progress শুধু phone এ থাকে, সেটা "collected" না।

### Privacy policy
Play Store এ একটা public URL লাগে। সহজ টেক্সট (একটা free page, যেমন GitHub Pages/Notion/Google Sites এ রাখো):
> Arrow Voyage saves game progress only on your device and does not ask for any personal information. The app shows ads through Google AdMob. To show and measure ads, Google may collect and process your device's advertising ID, approximate location (from IP address), app interactions and diagnostic data, as described in Google's policy: https://policies.google.com/technologies/partner-sites. Users in the EEA and UK are asked for consent and can change it any time from Settings → Ad privacy. You can reset or delete your advertising ID in your Android settings. Contact: <your email>

### Store text
**Short description:** Arrow mazes where only one arrow can fly. Find it, clear the board, reveal the picture.

**Full description:**
Arrow Voyage is a calm but brain-teasing puzzle. Every board is a picture made of bent arrows. Tap an arrow to fire it; it flies only if nothing stands in front of its head. Most of the time just one arrow in the whole maze is free, and it hides in plain sight. Find the chain, clear every arrow, and the picture lights up.

- 100 hand-tuned levels: hearts, cats, rockets, crowns, and giant boss mazes
- Usually only one arrow can move: real thinking, not guessing
- Pinch to zoom on huge boards
- Daily challenge, picture gallery, three-star ratings, hints
- Three themes, soft sound effects, plays offline
- Free, with optional video ads for extra hints

## Versions
পরের update এ `app/build.gradle.kts` এ `versionCode` 1 করে বাড়াও (2, 3, …) আর `versionName` বদলাও। গেমের HTML আপডেট করতে হলে শুধু `app/src/main/assets/www/index.html` replace করো।
