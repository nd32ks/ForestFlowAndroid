# Parity: action → trigger → test

Unit tests: `app/src/test/.../ReducerTest.kt` (R), `MapTest.kt` (M). Instrumented: `BackendTest.kt` (B), `AppFlowTest.kt` (UI).

| Brief action | `Action` | Triggered from | Test |
|---|---|---|---|
| role | `SetRole` | LoginScreen segmented control | R `role_switches_login_role`, UI `supplier_full_flow` |
| agree | `Agree` | LoginScreen agreement row | R `agree_toggles`, UI `agreeAndLogin` |
| getCode | `GetCode`, `CodeAutofill` | LoginScreen 获取验证码 | R `getCode_starts_countdown_and_autofill`, UI `sms_login_validation_and_code_flow` |
| login(wechat\|sms\|cas) | `Login` → `LoginSucceeded` / `CasFailed` | WeChat button, Sign In, CAS tile + CasSheet | R `sms_validates_phone_then_code`, `cas_opens_sheet`, `loginSucceeded_enters_role_and_logs`; B `cas_role_mismatch_is_rejected`; UI `cas_login_with_database_account` |
| visitor | `Visitor` | Visitor tile | R `visitor_requires_agreement`, UI `student_secondary_actions` |
| tab | `Tab` | GlassNavBar | R `tab_changes_and_alerts_tab_clears_badge`, UI flows |
| profile | `Profile` | GlassHeader avatar | R `profile_opens_sheet`, UI flows |
| logout | `Logout` | Profile sheet | R `logout_clears_session`, UI `student_full_flow` |
| reset | `Reset` | Profile sheet | R `reset_restores_seed_but_keeps_session`, UI `supplier_full_flow` |
| quick(Route\|I'm Here\|Lock\|Share) | `Quick` | Inventory quick actions | R `route_opens_map_with_directions`, `imHere_sets_you_are_here`, `lock_creates_escrow…`, `share_emits_share_effect`; UI student |
| coilNav | `CoilNav` | Inventory coil "Navigate" | R `coilNav_opens_map` |
| notify | `Notify` | Inventory "Notify Me" | R `notify_toggles_osmanthus_watch`, UI student |
| release | `Release` | Inventory escrow bar | R `release_clears_escrow`, UI student |
| pin | `Pin` | Map pins, search results, Stations sheet | R `pin_on_other_campus_switches_campus`, UI student |
| campus | `Campus` | Map segmented control | R `campus_switch_selects_a_node_on_that_campus`, UI student |
| mapFilter | `MapFilter`, `BuildingFilter` | Map chips, Buildings sheet | R `filters_and_search_combine` |
| search | `Search`, `SearchBuilding` | Map search field + dropdown | R `filters_and_search_combine`, `searchBuilding_pulses_and_focuses`; M `search_matches_en_zh_codes_and_departments`; UI student |
| locate | `Locate` | Map locate button | R `locate_and_compass_emit_focus`, UI student |
| compass | `Compass` | Map compass button | R `locate_and_compass_emit_focus`, UI student |
| layers(toggles) | `OpenSheet(Layers)`, `LayerToggle` | Map layers button / sheet | R `layers_toggle`, UI student |
| save | `Save` | Node card bookmark | R `save_toggles_bookmark`, UI student |
| directions | `Directions` | Node card | R `directions_toggle_and_replace`, `directions_refused_across_campuses`, UI student |
| viewStock | `ViewStock` | Node card | R `viewStock_sets_active_node_and_inventory_tab`, UI student |
| (camera) | `SaveCamera`, `FocusConsumed` | Map gestures settle / focus handled | R `saveCamera_persists_per_campus`; M geometry tests |
| nf | `Nf` | Notifications chips | R `nf_readAll_dismiss_unread`, UI student |
| readAll | `ReadAll` | Notifications "Mark All Read" | R `nf_readAll_dismiss_unread`, UI student |
| liveStatus | `LiveStatus` | Osmanthus watch card | R `liveStatus_toasts`, UI student |
| unsub | `Unsub` | Watch cards | R `unsub_and_alert_switches`, UI `student_secondary_actions` |
| pushAlert | `PushAlert` | White Peach card switch | R `unsub_and_alert_switches` |
| smsAlert | `SmsAlert` | White Peach card switch | R `unsub_and_alert_switches` |
| holdNav | `HoldNav` | Urgent drop "Navigate & Hold" | R `holdNav_opens_map_on_node_02`, UI `student_secondary_actions` |
| dismiss | `Dismiss` | Feed cards | R `nf_readAll_dismiss_unread`, UI student |
| reorder | `Reorder` | Escrow timeout "Re-order Now" | R `reorder_creates_escrow_and_opens_vip`, UI `student_secondary_actions` |
| pref(push\|sound\|quiet) | `Pref` | Alert preferences | R `prefs_toggle` |
| copyCode | `CopyCode` | VIP "COPY" | R `copyCode_and_qr`, UI student |
| qr | `Qr` | Tap the QR | R `copyCode_and_qr`, UI `student_secondary_actions` |
| redeem / doRedeem | `Redeem`, `DoRedeem` | VIP button / Redeem sheet | R `redeem_deducts_points_once`, UI student |
| coil | `SelectCoil` | Telemetry matrix | R `coil_select_and_restock_c3_clears_queue`, UI supplier |
| restockCoil | `RestockCoil` | Telemetry selected strip | same, UI supplier |
| velocity | `Velocity` | Telemetry bars | R `velocity_select`, UI supplier |
| aiDispatch | `AiDispatch` | Telemetry AI card | R `aiDispatch_advances_and_opens_dispatch`, UI supplier |
| pingRunner / callRunner | `PingRunner`, `CallRunner` | Dispatch runner card | R `ping_and_call_runner`, UI supplier |
| approve / decline | `Approve`, `Decline` | Dispatch clearance card | R `approve_and_decline_clearance`, UI supplier |
| surge | `Surge` | Dispatch simulation card | R `surge_updates_metrics`, UI supplier |
| confirmRestock | `ConfirmRestock` | Dispatch primary CTA | R `confirmRestock_fills_everything_once`, UI supplier |
| ping | `Ping` | Alerts urgent card | R `alerts_ping_filter_mute`, UI `supplier_secondary_actions` |
| alertFilter | `AlertFilter` | Alerts chips | R `alerts_ping_filter_mute`, UI supplier |
| mute | `Mute` | Alerts gateway switch | R `alerts_ping_filter_mute`, UI supplier |
| (timer) | `Tick` | ViewModel 1 s ticker | R `escrow_expires_on_tick`, B `state_persists_across_restart_and_escrow_expires_after_reopen` |
| (back) | `Back` | System back | R `back_closes_sheet_then_returns_to_first_tab` |
