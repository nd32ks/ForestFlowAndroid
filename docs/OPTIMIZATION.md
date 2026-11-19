# Optimization (branch `optimize`)

Goal: smoother on weak devices. What changed, what was measured, and what was not.

## Changes

| # | Change | Where | Why |
|---|--------|-------|-----|
| 1 | The 1 s clock is held as `State` and read only inside tiny countdown composables | `AppRoot.kt`, `InventoryScreen.kt` (`HoldLeft`), `VipScreen.kt` (`ClaimCountdown`), `LoginScreen.kt` | The root used to read the clock, so the app shell and the Inventory/VIP screens recomposed every second |
| 2 | Full-state JSON snapshot encoded with `flowOn(Dispatchers.Default)` | `AppViewModel.persist()` | The encode used to run on the main thread on every state change |
| 3 | `@Immutable` on the 12 state classes | `core/Models.kt` | Lets Compose skip parts of the UI whose state did not change |
| 4 | Campus maps converted to WebP, long edge 1600 px; only one decoded map is kept | `assets/maps/*.webp`, `map_data.json`, `MapBitmaps` | Two PNGs of 2.2 MB and 1.3 MB were decoded at full size and both kept |
| 5 | Live blur skipped on low-RAM devices (`isLowRamDevice` or heap of 128 MB or less); blur radius 24 dp to 18 dp; glass style remembered | `ui/components/Chrome.kt`, `AppRoot.kt` | The blur re-renders everything behind the header and nav bar every frame |
| 6 | `profileinstaller` and a hand-written baseline profile for the app package | `app/src/main/baseline-prof.txt`, `build.gradle.kts` | Precompiles the app's code at install so the first launches are not interpreted |

Checked and left alone: the infinite animations (map, VIP card, pipeline step, live dot) already read their
values inside draw or `graphicsLayer` blocks, so they do not recompose the UI.

## Measured (deterministic)

- Release APK: 5,695,436 bytes to 2,467,150 bytes (57% smaller).
- Map image files: 3.4 MB to 208 KB (94% smaller). Compared by eye on both maps; no visible difference.
- Native heap with the Taicang map open: 33,049 KB to 31,624 KB (about 4% lower). The image is 1600x1208 instead
  of 1696x1280.
- Decoded map memory when both campuses have been opened: about 24 MB before (both kept) to at most about 9 MB
  after (one kept). This figure is computed from the image sizes, not measured on a device.
- Unit tests pass. The release build runs through login, Inventory, Campus Map and VIP Pass with no crash, and the
  VIP countdown still ticks.

## Not reliably measured

Cold start time and frame times could not be compared. The emulator runs without hardware graphics on a Mac
with about 90 MB free memory. The same unchanged "before" build took a median of 545 ms to cold start in one run
and about 3,000 ms in the next, and its median frame time went from 97 ms to 600 ms. That drift is larger than any
effect being measured. The baseline profile was also installed through a broadcast whose result was not
confirmed.

The estimates made before the work (cold start 20 to 30% faster, 40 to 60% fewer janky frames on weak devices) are
therefore still estimates. To measure them: connect the phone (Nothing A059), run the same cold start and
`dumpsys gfxinfo` steps on the `week-7` and `optimize` builds alternately, several times each, and compare medians.

## Safe to revert

Each change is its own commit on `optimize`. `main` and `week-7` are untouched.
