# Week 6: Feature 2 (student Inventory and Notifications)

Problems met and fixed, with the commit for each.

**How this was written.** No screenshots were taken during the original build. These
problems were reproduced afterwards on 2026-10-27 for documentation, so this is a
reconstruction, not a contemporaneous record. The error messages below are real compiler
and runtime output from those runs. The git history on `main` (Oct 9 to Oct 23) is the real
record of when the app was built.

## 1. What week 6 added

- Feature 2 screens restored to `feature/customer/`: `InventoryScreen.kt` (station card and
  product cards with Notify Me) and `NotificationsScreen.kt` (watched coils, push/SMS toggles).
- Student tabs: Inventory and Notifications. The Campus Map and VIP Pass tab shows
  "Coming next". Feature 3 files stay in `later-features/feature-3/`.
- `tools/run.sh`: one command that builds, installs and launches the app.
- Commit: `f776df5`.

## 2. Problems found in week 6

| # | Problem | File | Message / symptom | Introduced | Fixed |
|---|---------|------|-------------------|-----------|-------|
| 1 | Missing import | `InventoryScreen.kt` | `Unresolved reference 'height'` (7 lines) | `aa0e120` | `6a5ea57` |
| 2 | Nullable value used without a check | `InventoryScreen.kt` line 61 | `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'MapNode?'` (13 lines) | `94dfe50` | `78cb899` |
| 3 | Resource name does not exist | `ui/components/Visuals.kt` line 59 | `Unresolved reference 'can_lychee_v2'` | `d5d6c34` | `b5c3c40` |
| 4 | Flipped condition (runs, wrong behaviour) | `NotificationsScreen.kt` line 119 | "No restock alerts yet" shown together with the White Peach subscription card | `078bf9a` | `832c7c2` |

### Problem 1: missing import
- **Cause:** `import androidx.compose.foundation.layout.height` was missing, so every
  `Spacer(Modifier.height(...))` failed.
- **Fix:** add the import (after `layout.fillMaxWidth`).
- **Lesson:** in Compose, modifiers such as `height`, `clip` and `verticalScroll` are
  extension functions. They only resolve when imported.

### Problem 2: nullable value
- **Cause:** `map.node(s.activeNodeId)` returns `MapNode?`. The code used the result as if
  it could not be null.
- **Fix:** `val node = map.node(s.activeNodeId) ?: map.allNodes.first()`
- **Lesson:** a `?` type means "can be null". Handle it with `?.`, `?:` or a check.

### Problem 3: resource name does not exist
- **Cause:** code asked for `R.drawable.can_lychee_v2`. The image in `res/drawable-nodpi/`
  is `can_lychee.jpg`.
- **Fix:** `R.drawable.can_lychee`
- **Lesson:** `R.drawable.<name>` is built from the file name. Rename a file and every
  reference must change with it.

### Problem 4: flipped condition
- **Symptom:** the Notifications screen showed the empty-state message and a subscribed
  coil at the same time.
- **Cause:** the empty state was guarded by `if (active != 0)` instead of `if (active == 0)`.
- **Fix:** `if (active == 0)`
- **How it was found:** run on the emulator, sign in as a student, open Notifications.
  The compiler cannot catch this, because the code is valid.
- **Lesson:** a successful build does not mean the screen is right. Check it by running.

## 3. Week 5 problems carried into week 6 and fixed

| Problem | File | Message / symptom | Fix commit |
|---------|------|-------------------|------------|
| Crash opening Dispatch | `SupplierScreens.kt` | `NoSuchElementException: Collection contains no element matching the predicate` (`first` on a missing coil) | `af52cf2` (`sp.coil("A1")`) |
| Type mismatch | `SupplierScreens.kt` line 439 | `Return type mismatch: expected 'Int', actual 'String'` | `14d9c0a` |
| Missing `clip` import | `SupplierScreens.kt` | `Unresolved reference 'clip'` (24 lines) | `3f28a50` |
| Stale "Critical 100%" after restock; runner button clips | `SupplierScreens.kt` | wrong text on screen | `601cb85` (reapplies original fix `605d6ca`) |

## 4. Verification

- **Build:** `tools/run.sh` on a freshly restarted emulator (`ForestFlow_API36`):
  `BUILD SUCCESSFUL`, app installed and launched.
- **Student, Notifications:** the White Peach card shows and "No restock alerts yet" does
  not appear.
- **Supplier, Dispatch:** opened without a crash (no `FATAL EXCEPTION` in logcat, app still running).
- **Unit tests:** `./gradlew :app:testDebugUnitTest` passes, including the test added with
  the "Critical 100%" fix.
- **Not checked:** a full restock run on screen to see "Full 100%" in the manifest. Only
  the unit test covers that fix.

## 5. Status at the end of week 6

Login, Feature 1 (supplier Telemetry, Dispatch, Alerts) and Feature 2 (student Inventory,
Notifications) work. Feature 3 (VIP pass, escrow, campus map) is not in the build yet.
