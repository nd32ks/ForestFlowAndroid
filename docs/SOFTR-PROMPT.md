# Softr prompt — ForestFlow Week 5 (working login + Feature 1)

Paste the box into the Softr AI Co-Builder. (Long version: `SOFTR-PROMPT-LONG.md` in git history.)

```text
Build "ForestFlow": a role-based web app for the Chi Forest smart vending machine at XJTLU Taicang.
Free plan only: Softr Databases, users + user groups, pages/blocks, Workflows. No custom code.
Limits: 5 app users, 1,000 records/database, 500 workflow actions/month (no loops over many records).
Scope = Week 5: (1) working login with roles, (2) Feature 1 = AI stock demand prediction and dispatch (supplier side).
The real AI call comes in Week 6: use formulas now and add a placeholder card "AI forecast (Week 6)".

DATA
- Products: SKU, Name, Name ZH, Price CNY. Seed: White Peach 白桃, Lychee Fizzy 荔枝, Calamansi Lime 青桔, Osmanthus Plum 桂花乌梅 (all 5.50).
- Coils (16 for Node 01: A1-D4): Coil ID, Product (link), Level (0-15), Capacity (15), Cans per hour.
  Formulas: Fill % = Level/Capacity*100; State = Empty (0) / Low (1-6) / Optimal (7+);
  Minutes to empty = Level / (Cans per hour * Surge factor / 60); Risk = High (<=30 min) / Medium (<=90) / Low.
  Seed levels: A1 2, A2 9, A3 11, A4 8, B1 3, B2 2, B3 12, B4 10, C1 13, C2 9, C3 0 (Osmanthus), C4 5, D1 14, D2 12, D3 11, D4 11.
- Settings (1 row): Surge factor (1.0), Queue size (18), Dispensed today (318).
- Restock Tickets: Status (Predicted / Dispatched / En route / Restocked), Runner ("Zhang"), Items (text), ETA minutes, created time.
- Alerts: Type (Stockout / Restock / Dispatch / Surge), Title, Body, created time.

LOGIN AND ROLES
- Email + password sign-up/sign-in. Groups: Student (default for sign-ups) and Supplier (set by admin).
- Student sees only "Station" (read-only coil cards: product, "X left", status pill; empty = "Out of stock - restocking").
- Supplier sees "Telemetry", "Dispatch", "Alerts". Wrong role = "You do not have access", no data shown.
- Redirect after sign-in: Student to Station, Supplier to Telemetry. Sign-out button in the top bar.
- Demo users only: student@example.com and supplier@example.com.

FEATURE 1 (Supplier)
- Telemetry: KPI tiles (Dispensed today, Network fill % = average Fill %, Next surge 13:50); 4x4 coil matrix coloured by State
  (Empty red, Low amber, Optimal green); click a coil for detail + button "Restock this coil".
- "AI Predictive Restock Engine" card list: coils with Risk High/Medium, soonest first, "Predicted stock-out in X min";
  a "Recommended dispatch" text (e.g. "3 cases White Peach, 2 cases Lychee Fizzy"); button "Dispatch Runner".
- Dispatch: 4-step pipeline of the latest ticket, runner card with "Ping runner", restock manifest of High/Medium coils,
  "Simulate foot-traffic surge" and "Confirm restock loaded" buttons.
- Alerts: newest-first feed with a Type filter.

WORKFLOWS
1. Coil updated and Risk = High and no open ticket: create ticket (Predicted) + Alert.
2. Dispatch Runner: ticket = Dispatched, ETA 6; Alert.   3. Ping runner: ticket = En route; Alert.
4. Restock this coil: Level = Capacity; Alert.
5. Simulate surge: Dispensed +64, Queue +13, Surge factor 2.5; Alert.
6. Confirm restock: refill only High/Medium coils, ticket = Restocked, Queue 0, Surge factor 1.0; Alert.

DESIGN: mobile first, Apple-like. Red #B7131A, background #F2F4F6, white cards with rounded corners, dark text #191C1E,
green #006C49, amber #B86A00. Round red logo with the character 气.

DONE WHEN: a new sign-up lands on Station and cannot open supplier pages; the supplier sees the matrix and AI cards;
C3 is Empty with High risk and an auto ticket; Dispatch > Ping > Confirm moves the ticket to Restocked, refills the coils,
clears the queue and writes an Alert each step; Simulate surge raises the numbers; data persists after sign-out.
```
