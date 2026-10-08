# Decisions

Judgment calls made where the brief was silent, or where it had to be overridden.

## Inputs that were missing
- **`reference/forestflow-prototype.html` was not in the folder.** The only HTML prototype present (`ForestFlow Prototype/`) is an older, different design (Radar / Flavor Lab). Copy, data and screen structure were therefore taken from the **Figma file `Figma/ForestFlow.fig`** (text extracted from its canvas data) and from the brief's section 6. Where neither specified something, the choice is listed below.
- **`assets/maps/*` was missing.** TC map = `JPEG image.jpeg` (the official illustrated Taicang map). SIP map = page 1 of `XJTLU-SIP-Campus-Map-March2026-v2.pdf`, rendered at 2400 px and cropped to the map area (legend removed). `map_data.json` was authored by hand: building anchors read off the two images; TC entrances placed at the four road openings (approximate); `tc_entrances_reference.png` did not exist, so entrance styling follows the brief's wording (red dots + EN/中文 labels).
- **Figma MCP** needed authorization and was not used. Product photos and the runner portrait come from the images embedded in the `.fig`.

## Platform / build
- **AGP 9.3.3, not the newest 9.4.1**: the installed Android Studio 2026.1 supports AGP up to 9.3. Using 9.4 would make Studio refuse to sync. Gradle 9.5.1 (AGP 9.3 minimum). Kotlin 2.4.21 (Compose compiler plugin), Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 26.
- AGP 9 has built-in Kotlin support, so `org.jetbrains.kotlin.android` is not applied to the module (only declared at the root to pin the Kotlin version).
- **Haze 1.7.3 instead of 2.0.1**: 2.0 moved effects into separate factory artifacts with a new API; 1.7.3 gives the same real backdrop blur with a stable `hazeSource` / `hazeEffect` API.
- Release build is signed with the debug key so a release APK can be side-loaded for demos. R8 + resource shrinking on: release APK ≈ 7 MB.

## Persistence — SQLite instead of DataStore
- The user asked for "a working database". The brief suggested DataStore; I used the **Android framework SQLite** (`SQLiteOpenHelper`, zero extra dependencies, no KSP) because it is a real, inspectable database: `users`, `app_state`, `activity_log`. Visible in Android Studio's Database Inspector. The app state is still a kotlinx.serialization JSON snapshot (as the brief wanted), stored in `app_state`.
- Session persistence lives inside the state snapshot; signing out clears it.
- `activity_log` is new (not in the brief): it gives the supervisor something concrete to look at and is shown in the profile sheet.

## Behaviour
- **Initial escrow**: `fresh()` starts with an active escrow on Node 01 coil #3 (Calamansi Lime) because both the Inventory and VIP Figma frames show one. Claim window 04:58; dispenser hold = claim + 5:00.
- **Escrow expiry** happens when the claim countdown reaches 0 (the escrow and its hold are released together).
- **Osmanthus watch starts off** (Figma's Inventory shows "Notify Me") so the demo story works: tap Notify Me → card appears in Notifications. White Peach watch starts on.
- **Unread** is derived (three feed cards not yet read or dismissed) rather than a separate counter, so it can never drift.
- **SMS**: only `482915` (the demo code) passes; any other 6 digits shake with "Incorrect code".
- **Navigate & Hold** also creates an escrow on Node 02 (White Peach) if none exists ("hold"), then opens the map with directions.
- **Re-order Now** creates an escrow on the active node's coil #3 and opens VIP Pass; if an escrow exists it just opens VIP Pass.
- **AI Dispatch** advances the pipeline one stage (max "Runner En Route") and opens Dispatch. **Surge** after a completed restock re-opens the pipeline at stage 2 and re-enables *Confirm Restock Loaded*.
- **Mute** in Alerts only changes the gateway state and toast; badges still count so nothing is lost.
- **Call runner** shows a toast (brief), no dialer intent.
- **Share** uses the Android share sheet (works on device, no permission needed).
- **Directions across campuses** are refused with a toast (You Are Here is on one campus).
- **Selecting a pin while directions are on** moves the route to the new node (brief: "moving to another node replaces it").
- Velocity bars: 11:00–16:00 with a 13:30 bar so the 14:00 bar (index 4) is the 120 peak.
- Coil matrix seed (A1…D4) sums to exactly 132 so Network Fill starts at 42% as in the brief formula.

## UI
- Theme follows the system (no manual toggle). Both themes tokenised (`ui/theme/Theme.kt`).
- The map uses **cover-fit** at zoom 1 so the card is always filled (no empty bands), and the image can never leave the viewport. While dragging, edges rubber-band (Apple's formula); on release, the flick velocity is projected (Apple's deceleration projection, rate 0.995) and the camera eases to the clamped landing point.
- Pins, labels, the route line and You Are Here are drawn in screen space; the bitmap is drawn with `drawImage` into a computed destination rect (no per-frame allocations of bitmaps).
- Press feedback is a scale on pointer-down with a springy release (no ripple), per the Apple design guidance.
- Copy is kept inline in the composables (English + Chinese exactly as in Figma) rather than `strings.xml`. This is a deliberate trade-off for a bilingual demo prototype; `app_name` is in `strings.xml`.
- Reduce motion: when the system animator scale is 0, pulses/sheen/dash animations are static and camera moves snap.

## Found and fixed during on-device testing (emulator, API 36)
- **Drags that began on a pin scrolled the page instead of the map.** Pan/zoom now lives on one container around the bitmap *and* the pins, reading events in the `Initial` pass, so the map claims the drag before the page's `verticalScroll`.
- **Campus switch could drop a focus request** (the outgoing map consumed it during the crossfade). Only the current campus's map consumes focus now.
- **Pins overlapped at the default zoom.** TC opens at 1.6× centred on the ring, SIP at 1.35×. Pins show the full "Node 01 · 82% · 2m" label from **2.2×** (brief said 1.4×; at 1.4× the six TC labels collide).
- The walking-time chip is drawn above the pins so it is never hidden.
- **SIP map showed the PDF's orange border** at the bottom/right edge: re-cropped to 1890×2072 and anchors rescaled.
- Non-Osmanthus out-of-stock tiles showed "Notify Me" (which subscribes to Osmanthus). They now show **Other Nodes** (opens the stations list). The temperature pill reads "Hyper-Cool" only below 4 °C.
- A database write failing (e.g. storage removed) can no longer crash the app: every SQLite call in the ViewModel is guarded.
- Build outputs go to `build.nosync/` because the project sits in iCloud Drive; iCloud does not sync `.nosync` folders, which stops it from moving files under Gradle mid-build.
- **Double-tap zoom did not fire** (the tap detector lost events to the pan handler). Double-tap is now recognised inside the map's own gesture handler: two short presses within 300 ms and 48 dp toggle 2.5× at the tap point ↔ fit. A double-tap that starts on a pin selects the pin instead of zooming.
- **State persistence had a 250 ms debounce**, so signing out and killing the app at once could reopen signed in. Every change is now written straight away (writes are coalesced, never dropped).
- **Firebase backend verified** against the Firebase Emulator Suite: cross-instance sign-in, roles from Firestore, the app's CAS path, demo-account auto-provisioning. Debug builds allow plain HTTP only to `10.0.2.2`/`localhost` (emulator); release builds do not.
- The two published demo accounts are auto-created in Firebase on first sign-in (only with the published password), so a fresh Firebase project works for the demo without manual setup.
