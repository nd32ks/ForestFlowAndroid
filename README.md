# Forest Flow (元气森林 Chi Forest) — Android

A native Android app (Kotlin + Jetpack Compose) for Chi Forest smart vending machines on the XJTLU campuses.
Students find chilled-drink nodes on a real campus map, lock a coil in escrow, show a QR code at the
machine, get restock alerts and use a VIP pass. Suppliers watch telemetry, dispatch runners, approve
clearance prices and confirm restocks.

Everything runs **on the phone, offline**. Data lives in an on-device **SQLite database**.

---

## Run it on your phone (one button)

1. Open **Android Studio** → **File ▸ Open…** → choose this `ForestFlowAndroid` folder.
   Wait for "Gradle sync" to finish (first time downloads dependencies, a few minutes).
2. On the phone: **Settings ▸ About phone ▸ tap _Build number_ 7 times**, then
   **Settings ▸ System ▸ Developer options ▸ USB debugging = On**. Plug the phone in by USB and tap **Allow**.
   (Or use *Wireless debugging* → Android Studio ▸ *Pair Devices Using Wi-Fi*.)
3. Pick your phone (or the *ForestFlow Pixel API 36* emulator) in the device drop-down at the top of Android Studio and press **▶ Run 'app'**.
   Android Studio compiles, installs and opens Forest Flow on the phone.

Requirements: Android 8.0 (API 26) or newer. Android Studio 2026.1 or newer (uses its bundled JDK 21).

## Demo logins

| Path | How |
|---|---|
| WeChat one-tap | Tick the agreement, tap the green WeChat button (demo guest session) |
| SMS | Any valid mainland number (e.g. `13800138000`) → **获取验证码** → code `482915` auto-fills → Sign In |
| **XJTLU CAS (real accounts in SQLite)** | `student@xjtlu.test` / `forest123` (Student) · `supplier@xjtlu.test` / `forest123` (Supplier). Or tap *Create account* |
| Visitor | Visitor tile (demo guest session) |

Choose **Student** or **Supplier** with the switch at the top of the login screen first. A CAS account only
opens the app that matches its role ("This is a Supplier account" otherwise).

## Demo-day script

* **Student** → Inventory: *Release* / *Lock* the escrow, *Notify Me* on Osmanthus, *I'm Here*, *Route*.
* Campus Map: pinch, pan, double-tap, flick; search `library`, `AIAC`, `图书馆`, `CB`, `Design`;
  switch **Taicang 太仓 / SIP 园区**; tap a pin → *Directions* (dashed walking line) → *View Stock*;
  *Layers* → Entrances / Points of interest; compass resets the view; locate button finds *You Are Here*.
* Notifications: filters, *Navigate & Hold*, *Re-order Now*, unsubscribe, preferences.
* VIP Pass: live claim countdown, real scannable QR (`chiforest://escrow/N01-3-CF9204`), *COPY* backup code,
  *Redeem Points* (deducts points).
* **Supplier** → Telemetry: tap coil **C3** → *Restock*; tap bars; *Dispatch Runner*.
  Dispatch: *Ping*, *Approve Price Cut*, *Simulate Foot-Traffic Surge*, *Confirm Restock Loaded*.
  Alerts: badge clears on open, filter chips, mute switch, new events rise into the feed.
* Avatar (top right) → profile sheet: **Recent activity read from the SQLite `activity_log` table**,
  *Reset demo data*, *Sign out*.

State survives closing the app (kill it and reopen: same tab, same escrow countdown, same points).

## The database (backend)

`forestflow.db` (SQLite, Android framework API, no extra libraries):

| Table | Holds |
|---|---|
| `users` | Local accounts. Passwords stored only as **PBKDF2-HMAC-SHA256** (random 16-byte salt, 120 000 iterations). |
| `app_state` | One-row JSON snapshot of the whole app state, written after every change |
| `activity_log` | Append-only audit: logins, locks, releases, redemptions, restocks, surges … |

See it live: Android Studio ▸ **View ▸ Tool Windows ▸ App Inspection ▸ Database Inspector** while the app runs.

### Shared accounts for everyone (Firebase)
Without Firebase every phone keeps its **own** account database: anyone can create an account, but only on
that phone. To let an account made on one phone sign in on any other phone, connect the free Firebase backend
(the code is already in the app and verified, see *Tests*). The two demo accounts are created in the cloud
automatically the first time someone signs in with them, so they work on every phone too.

1. Create a free Firebase project (Spark plan).
2. Add an Android app with package `com.chiforest.forestflow`.
3. Enable **Authentication ▸ Email/Password** and create a **Cloud Firestore** database.
4. Download `google-services.json` into `app/`.
5. Sync Gradle and Run. CAS sign-in now uses Firebase; roles live in Firestore `users/{uid}`.
   Suggested Firestore rule: `match /users/{uid} { allow read, write: if request.auth.uid == uid; }`

Without that file the app builds and runs fully offline.

## Campus map

Real XJTLU maps: Taicang (`tc_campus.png`, from the official illustrated map) and SIP (`sip_campus.png`,
rendered from *XJTLU-SIP-Campus-Map-March2026-v2.pdf*). Buildings, entrances, points of interest and the
11 vending nodes are in `app/src/main/assets/maps/map_data.json` as fractions of image width/height —
edit that file to move a machine. Node locations are **demo placeholders** (`"demo": true`).

Directions draw a **straight** dashed line from *You Are Here* to the node with the walking time: the maps
are illustrations without a path network, so there is no turn-by-turn routing.

## Tests

* `./gradlew test` — 60 JVM tests: every reducer action, map geometry (clamp, rubber-band, momentum
  projection), bilingual search, password hashing, and that every vending node sits on its own building in `map_data.json`.
* `./gradlew connectedDebugAndroidTest` (phone or emulator attached) — 11 instrumented tests on the real
  SQLite backend (tables, auth, persistence across restarts, escrow expiry after reopen, role mismatch) and
  end-to-end UI flows that tap through every Student and Supplier feature, and real multi-touch on the
  map (double-tap zoom, pinch in/out, pan, an over-the-edge fling that must settle inside the image).
* `./gradlew lint` — no issues.

* `FirebaseBackendTest` (4 tests) runs when the app is built with a `google-services.json`. It was run against the
  local **Firebase Emulator Suite** (project `demo-forestflow`): an account created by one app instance signs in
  from a second instance with the same role and name, the app's own CAS login path works on Firebase, wrong
  passwords and duplicate e-mails are rejected, and the demo accounts auto-provision. Reproduce with
  `firebase emulators:start --only auth,firestore --project demo-forestflow`, a demo `app/google-services.json`, and
  `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.firebaseEmulator=10.0.2.2`.

All of the above pass (verified on an Android 16 / API 36 emulator).
Build outputs are written to `app/build.nosync/` (kept out of iCloud sync).

## Project layout

```
app/src/main/java/com/chiforest/forestflow/
  MainActivity.kt, AppRoot.kt        shell, effects (toast, haptics, clipboard, share), sheets
  core/       AppState, Action, Reducer (pure), AppViewModel, ForestDatabase (SQLite), MapData, seed
  auth/       AuthRepository, LocalAuthRepository (SQLite + PBKDF2), FirebaseAuthRepository
  ui/theme/   colour tokens (light + dark), type ramp, motion springs
  ui/components/  glass nav pill, glass header, toasts, sheets, segmented control, switch, QR, charts, icons
  feature/login, feature/customer (Inventory, Map, Notifications, VIP), feature/supplier (Telemetry, Dispatch, Alerts)
docs/DECISIONS.md   every judgment call
docs/PARITY.md      action → screen → test map
```

Fonts: Plus Jakarta Sans, Inter, JetBrains Mono (SIL OFL). Libraries: Compose, Haze (glass blur), ZXing (QR),
kotlinx.serialization, optional Firebase. All free and open source.
