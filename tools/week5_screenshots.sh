#!/bin/bash
# Week 5 evidence: (1) working login, (2) Feature 1 = AI stock demand prediction & dispatch (manager side).
# usage: tools/week5_screenshots.sh      output: docs/screenshots/week5/NN_name.png   (emulator, clean status bar)
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/screenshots/week5"; rm -rf "$OUT"; mkdir -p "$OUT"
A=~/Library/Android/sdk/platform-tools/adb
ACT=forest.flow/com.chiforest.forestflow.MainActivity
ui() { python3 -I "$ROOT/tools/ui.py" "$@"; }

tapt()  { back_to_app; for _ in 1 2 3; do ui tap "$@" >/dev/null && { sleep "${DELAY:-1.5}"; return; }; sleep 1; done; echo "  !! could not find: $1"; }
# like tapt, but scrolls down (up to 6 times) until the element is on screen AND above the floating nav pill
tapscroll() { back_to_app; for _ in 1 2 3 4 5 6; do ui tap "$@" visible >/dev/null && { sleep "${DELAY:-1.5}"; return; }; $A shell input swipe 540 1900 540 1000 350; sleep 1.0; done; echo "  !! could not reach: $1"; }
swipe() { $A shell input swipe 540 "$1" 540 "$2" 380; sleep 1.4; }
shot()  { sleep 0.9; $A exec-out screencap -p > "$OUT/$1.png"; echo "  $1"; }
text()  { $A shell input text "$1"; sleep 0.8; }
ime_open() { $A shell dumpsys input_method | grep -q "mInputShown=true"; }
close_ime() { ime_open && { $A shell input keyevent KEYCODE_BACK; sleep 1; }; }
waitfor() { for _ in $(seq 1 ${2:-20}); do ui pos "$1" >/dev/null 2>&1 && return; sleep 1; done; echo "  !! never appeared: $1"; }
back_to_app() { $A shell dumpsys window | grep -E "mCurrentFocus" | grep -q "forest.flow" && return; $A shell am start -n $ACT >/dev/null; sleep 2; }

# clean status bar, light theme
$A shell settings put global sysui_demo_allowed 1 >/dev/null
for c in "enter" "clock -e hhmm 1005" "battery -e level 100 -e plugged false" "network -e wifi show -e level 4" "network -e mobile show -e datatype none -e level 4" "notifications -e visible false"; do
  $A shell am broadcast -a com.android.systemui.demo -e command $c >/dev/null 2>&1
done
$A shell cmd uimode night no >/dev/null
$A shell input keyevent KEYCODE_HOME
$A shell pm clear forest.flow >/dev/null
$A shell am start -W -n $ACT >/dev/null; sleep 3.5

echo "1. Login"
shot 01_login_screen
tapt "补货供货商 Supplier" ; shot 02_login_role_supplier
tapt "学生 / 用户 Student"
DELAY=0.5 tapt "微信一键登录" ; shot 03_login_validation_agreement         # blocked until the agreement is ticked
tapt "我已阅读"
tapt "输入大陆手机号" ; text 13800138000 ; close_ime
tapt "获取验证码" ; sleep 2.4 ; close_ime ; shot 04_login_sms_code_sent       # 60 s countdown + code auto-filled
tapt "登录并解锁冷柜" ; waitfor "TRAY COIL INVENTORY" 15 ; sleep 1.2 ; shot 05_login_student_home

echo "1b. Real account sign-in (SQLite / Firebase)"
tapt "Profile" exact ; sleep 1 ; tapt "Sign out" exact ; sleep 1.5
tapt "我已阅读" ; tapt "XJTLU CAS" ; sleep 1.8 ; shot 06_login_cas_sheet
tapt "Demo account" ; sleep 1 ; shot 07_login_demo_account_filled
tapt "Sign in" exact ; waitfor "TRAY COIL INVENTORY" 25 ; sleep 1.5 ; shot 08_login_cas_signed_in
tapt "Profile" exact ; sleep 1.8 ; shot 09_login_profile_session
tapt "Sign out" exact ; sleep 1.5

echo "2. Feature 1 (supplier): AI stock demand prediction & dispatch"
tapt "补货供货商 Supplier" ; tapt "我已阅读"
tapt "微信登录 · Supplier WeChat" ; waitfor "Coil Tray Matrix" 15 ; sleep 1.2 ; shot 10_feature1_telemetry_dashboard
swipe 1800 1000 ; shot 11_feature1_coil_matrix
tapscroll "C3 Osmanthus" ; shot 12_feature1_empty_coil_selected
swipe 1800 900 ; shot 13_feature1_ai_prediction
tapscroll "Dispatch Runner" ; sleep 1.8 ; shot 14_feature1_dispatch_pipeline
swipe 1700 900 ; shot 15_feature1_runner_and_manifest
tapscroll "Simulate Foot-Traffic Surge" ; sleep 1.6 ; shot 16_feature1_surge_simulation
tapscroll "Confirm Restock Loaded" ; sleep 1.6 ; shot 17_feature1_restock_confirmed
tapt "Alerts" exact ; sleep 2 ; shot 18_feature1_alert_feed
tapt "Telemetry" exact ; sleep 2
swipe 1000 2000 ; shot 19_feature1_after_restock

$A shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1
echo "done: $OUT"
