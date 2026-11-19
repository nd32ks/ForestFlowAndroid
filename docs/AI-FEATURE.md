# Consumer AI feature: "Ask for a drink" + live restock ETA

Student side (Inventory screen and Notifications). The supplier side already has AI-style restock prediction.

## 1. Ask for a drink (uses a chat model)
- **Input:** a free-text request ("something sweet", "no sugar", "青桔").
- **Logic:** `ai/DrinkAssistant.kt`. A clear request ("lychee") is answered offline by `QuickMatcher`. A vague one is sent
  to a chat model together with only the in-stock drinks. The reply must name an in-stock drink or it is discarded.
- **Output:** one pick with a short reason, the stock left, and a pill showing "AI pick" or "Quick match".
- **Falls back** to the offline matcher when there is no key, no network, a bad reply, or the call cap is reached.

### How few API calls it makes
- Clear requests never call the model.
- Prompt: a fixed 30-word instruction, one short line per in-stock drink, the request last. `max_tokens` is 60, temperature 0.2.
- A 30-entry cache keyed on the request and on which drinks are in stock (not on counts).
- One call per tap (no calls while typing), the button is disabled while waiting, 8 s timeout, no retries.
- At most 30 model calls per app run.

### Configuration (`local.properties`, git-ignored)
```
ai.key=<key>
ai.endpoint=https://api.deepseek.com/chat/completions   # default
ai.model=deepseek-chat                                    # default
```
Any OpenAI-style `/chat/completions` endpoint works. Without `ai.key` the app uses only the offline matcher.

**Security:** the key is compiled into the APK and can be extracted. Use it for demo builds only; for anything public, run
the model behind your own server and point `ai.endpoint` at it. Do not commit `local.properties`.

**Status:** the key supplied during development was rejected by DeepSeek ("api key is invalid"), so a live model reply
has not been seen. The fallback path was run on the emulator, and the transport and reply parsing are covered by unit
tests with a fake model. Set the right endpoint, model and key and the "AI pick" path switches on.

## 2. Live restock ETA (no model)
- `core/RestockEta.kt` reads the supplier's restock pipeline for Node 01: queued 40 min, runner assigned 25 min,
  on the way 10 min, delivered 0.
- Shown under an out-of-stock coil and on the Osmanthus watch card in Notifications, so it changes as the supplier
  dispatches, pings the runner and confirms the restock. Other nodes have no live supplier data and show no ETA.

## Tests
`app/src/test/.../DrinkAssistantTest.kt`: 13 tests (offline matching, no call on clear requests, cache, in-stock-only
prompt, bad or out-of-stock model picks, network failure, call cap, response parsing, ETA stages).
