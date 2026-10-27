# Week 5 errors: login + Feature 1 (supplier)

Reconstructed on 2026-10-27, because no screenshots were taken during the original build.
The errors are the kinds that happened (or, where marked, close variants); this is not
contemporaneous evidence. Each error is its own commit on the `week-5` branch.

| # | Error | File | Message / symptom | Origin | Status |
|---|-------|------|-------------------|--------|--------|
| 1 | Gradle too old for the Android Gradle Plugin | `gradle/wrapper/gradle-wrapper.properties` | `Minimum supported Gradle version is 9.5.0. Current version is 9.4.1.` | Real | Fixed (Gradle 9.5.1) |
| 2 | Missing `verticalScroll` import | `feature/Common.kt`, `feature/login/LoginScreen.kt` | `Unresolved reference 'verticalScroll' on receiver of type 'Modifier'` | Real kind | Fixed (import added) |
| E | Launcher icon points at a missing drawable | `res/mipmap-anydpi-v26/ic_launcher.xml` | `resource drawable/ic_launcher_foreground_v2 not found` (AAPT) | Variant of a real AAPT problem | Fixed (reference corrected) |
| F | Haze 2.0.0 API change | `gradle/libs.versions.toml`, `ui/components/Chrome.kt` | `Unresolved reference 'HazeStyle'`, `'HazeTint'`, `hazeEffect` argument errors | Real | Fixed (back to 1.7.3) |
| G | Crash opening Dispatch | `feature/supplier/SupplierScreens.kt` | `NoSuchElementException: Collection contains no element matching the predicate` (`first` instead of `firstOrNull`) | New | Open |
| B | Missing `clip` import | `feature/supplier/SupplierScreens.kt` | `Unresolved reference 'clip' on receiver of type 'Modifier'` | Real kind | Open |
| C | Type mismatch in `pct` | `feature/supplier/SupplierScreens.kt` line 439 | `Return type mismatch: expected 'Int', actual 'String'` | New | Open |
| 3 | Manifest shows stale status after restock | `feature/supplier/SupplierScreens.kt` | After a restock the manifest still reads "Critical 100%"; "Pinged · En Route" clips | Real (fixed in commit 605d6ca originally) | Open |

## Order in which errors appear

1. Gradle fails before any code compiles (error 1).
2. Resource linking fails next (error E).
3. The Kotlin compiler then reports F, 2, B and C together.
4. G and error 3 only appear when the app runs.

## Not caught by the compiler

G crashes the app on opening Dispatch (checked on the emulator, read from logcat).
Error 3 compiles and shows only on screen; it has not been re-run on a device.
