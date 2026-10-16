# Week 5 evidence — working login + Feature 1

Module handbook, Week 5: **"Working login + Feature 1"**. Screenshots of the finished app (release build,
Android 16 emulator, light theme). Re-create them with `tools/week5_screenshots.sh`.

## 1. Working login

| # | Screen | What it shows |
|---|---|---|
| 01 | `01_login_screen.png` | Login page: Student / Supplier role switch, WeChat one-tap, SMS code, XJTLU CAS, Visitor |
| 02 | `02_login_role_supplier.png` | Role switch → Supplier (labels change to the supplier wording) |
| 03 | `03_login_validation_agreement.png` | Validation: sign-in is blocked until the user agreement is ticked |
| 04 | `04_login_sms_code_sent.png` | SMS flow: 60 s countdown, demo code auto-filled |
| 05 | `05_login_student_home.png` | Signed in as a **student** → Station Inventory |
| 06 | `06_login_cas_sheet.png` | Real-account sign-in sheet (XJTLU CAS) — email + password, create account |
| 07 | `07_login_demo_account_filled.png` | Demo account filled in |
| 08 | `08_login_cas_signed_in.png` | Signed in with a real account (Firebase Authentication) |
| 09 | `09_login_profile_session.png` | Profile: role, session (Firebase), local SQLite database, activity log |

Role-based access: the account's role must match the selected tab; a Supplier account cannot open the
Student app and vice versa. Passwords are never stored in plain text; accounts live in Firebase, with a local
PBKDF2-hashed SQLite fallback. Each account keeps its own data.

## 2. Feature 1 — AI stock demand prediction & dispatch (manager side)

| # | Screen | What it shows |
|---|---|---|
| 10 | `10_feature1_telemetry_dashboard.png` | Telemetry: weather context, dispensed count, network fill, next surge (13:50, Hall G end) |
| 11 | `11_feature1_coil_matrix.png` | 4×4 coil matrix with Empty / Low / Optimal levels |
| 12 | `12_feature1_empty_coil_selected.png` | Empty coil C3 selected: 0 cans, 18 students queued, one-tap Restock |
| 13 | `13_feature1_ai_prediction.png` | **AI Predictive Restock Engine**: predicted stock-out time and the recommended dispatch (3 cases White Peach, 2 Lychee) |
| 14 | `14_feature1_dispatch_pipeline.png` | Dispatch: restock pipeline (Predicted → Dispatched → En route → Restocked) |
| 15 | `15_feature1_runner_and_manifest.png` | Runner card and restock manifest with priorities |
| 16 | `16_feature1_surge_simulation.png` | Dynamic clearance price cut and the foot-traffic surge simulation |
| 17 | `17_feature1_restock_confirmed.png` | Restock confirmed: manifest turns LOADED, queue cleared |
| 18 | `18_feature1_alert_feed.png` | Live alert feed records every step (dispatch, surge, restock, clearance) |
| 19 | `19_feature1_after_restock.png` | Telemetry afterwards: network fill 76%, all coils 15/15 |
