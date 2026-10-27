# Forest Flow: build stage 1 (login + Feature 1, with errors)

Reconstructed on 2026-10-27 because no screenshots were taken during the original build.
The errors are the kinds that really happened; this folder is not contemporaneous evidence.

- `project/` is what is "in the build" at this stage: the login page and Feature 1 (supplier
  Telemetry, Dispatch, Alerts) plus the shared core. Students see a placeholder screen.
- `later-features/feature-2/` and `feature-3/` hold the files moved out, at the same relative
  paths, so they can be dropped back into `project/` at the next stage.
  - Feature 2: `InventoryScreen.kt`, `NotificationsScreen.kt`
  - Feature 3: `VipScreen.kt`, `MapScreen.kt`, `CampusMap.kt`, `MapEngine.kt`, `MapTest.kt`, `MapGestureTest.kt`
- Edited to make the stage compile without them: `AppRoot.kt` (student tab placeholder, sheets
  for Features 2 and 3 removed).

## Errors present in project/

1. `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.4.1, but AGP 9.3.3 needs 9.5.0+.
   `Minimum supported Gradle version is 9.5.0. Current version is 9.4.1.`
2. `feature/Common.kt` (used by the Feature 1 screens) and `feature/login/LoginScreen.kt`:
   `import androidx.compose.foundation.verticalScroll` is missing.
   `Unresolved reference 'verticalScroll' on receiver of type 'Modifier'.`
   (Only visible once error 1 is fixed.)
3. `feature/supplier/SupplierScreens.kt`: the pre-fix version (restored from commit 605d6ca).
   After a restock the manifest still reads "Critical 100%", and the "Pinged · En Route"
   button text is too long and clips. This is a behaviour bug; it compiles.

Verified: with the stage's errors removed it compiles; with error 1 only, Gradle fails as above;
with error 2 present, the compiler reports the two lines above. Error 3 was not run on a device.
Build with `JAVA_HOME` set to Android Studio's bundled JDK.
