#!/usr/bin/env python3
"""
A local stand-in for a Supabase project (GoTrue auth + the PostgREST tables of supabase/schema.sql), in-memory,
for testing the app without an account:

    python3 tools/fake_supabase.py            # listens on 0.0.0.0:54321, API key "test-anon-key"
    SUPABASE_URL=http://10.0.2.2:54321 SUPABASE_ANON_KEY=test-anon-key ./gradlew connectedDebugAndroidTest

10.0.2.2 is the host as seen from the Android emulator. It speaks the same JSON shapes and error codes as the real
service for the calls the app makes; Row Level Security is emulated per access token. State is lost on exit.
"""
import json, sys, threading, itertools, re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs, unquote

API_KEY = "test-anon-key"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 54321
LOCK = threading.Lock()
SEQ = itertools.count(1)
USERS, ACCESS, REFRESH, STATES, ACTIVITY = {}, {}, {}, {}, []   # users by id
CONFIRM_EMAIL = False


def user_json(u):
    return {"id": u["id"], "email": u["email"], "user_metadata": u["meta"]}


def tokens(u):
    n = next(SEQ)
    at, rt = f"at-{n}", f"rt-{n}"
    ACCESS[at] = u["id"]; REFRESH[rt] = u["id"]
    return {"access_token": at, "token_type": "bearer", "expires_in": 3600, "refresh_token": rt, "user": user_json(u)}


class H(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *a):  # quiet
        pass

    def send(self, status, body=None):
        raw = b"" if body is None else json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def err(self, status, code, msg):
        self.send(status, {"code": status, "error_code": code, "msg": msg})

    def do_GET(self): self.route()
    def do_POST(self): self.route()

    def route(self):
        u = urlparse(self.path); q = parse_qs(u.query)
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        body = json.loads(raw) if raw.strip() else {}
        bearer = (self.headers.get("Authorization") or "").replace("Bearer ", "", 1)
        with LOCK:
            self.handle_locked(u.path, q, body, bearer)

    def handle_locked(self, path, q, body, bearer):
        if self.headers.get("apikey") != API_KEY:
            return self.send(401, {"message": "Invalid API key"})
        uid = ACCESS.get(bearer)
        if path == "/auth/v1/signup":
            email, pw = body["email"].lower(), body["password"]
            if len(pw) < 6 or pw in ("123456", "password"):
                return self.err(422, "weak_password", "Password should be at least 6 characters.")
            if any(x["email"] == email for x in USERS.values()):
                return self.err(422, "user_already_exists", "User already registered")
            nu = {"id": f"uid-{next(SEQ)}", "email": email, "pw": pw, "meta": body.get("data") or {}, "ok": not CONFIRM_EMAIL}
            USERS[nu["id"]] = nu
            return self.send(200, user_json(nu) if CONFIRM_EMAIL else tokens(nu))
        if path == "/auth/v1/token" and q.get("grant_type") == ["password"]:
            nu = next((x for x in USERS.values() if x["email"] == body["email"].lower() and x["pw"] == body["password"]), None)
            if not nu: return self.err(400, "invalid_credentials", "Invalid login credentials")
            if not nu["ok"]: return self.err(400, "email_not_confirmed", "Email not confirmed")
            return self.send(200, tokens(nu))
        if path == "/auth/v1/token" and q.get("grant_type") == ["refresh_token"]:
            rid = REFRESH.pop(body.get("refresh_token"), None)
            if not rid: return self.err(400, "refresh_token_not_found", "Invalid Refresh Token: Refresh Token Not Found")
            return self.send(200, tokens(USERS[rid]))
        if path == "/auth/v1/logout":
            ACCESS.pop(bearer, None); return self.send(204)
        if path.startswith("/rest/v1/"):
            if not uid: return self.send(401, {"code": "PGRST301", "message": "JWT expired or invalid"})
            if path == "/rest/v1/user_state" and self.command == "POST":
                if body["user_id"] != uid:
                    return self.send(403, {"code": "42501", "message": "new row violates row-level security policy"})
                STATES[uid] = (body["state"], body.get("client_ts", 0)); return self.send(201)
            if path == "/rest/v1/user_state" and self.command == "GET":
                m = re.search(r"user_id=eq\.([^&]+)", urlparse(self.path).query)
                want = unquote(m.group(1)) if m else None
                rows = [{"state": STATES[uid][0], "client_ts": STATES[uid][1]}] if uid in STATES and want == uid else []
                return self.send(200, rows)
            if path == "/rest/v1/activity_log" and self.command == "POST":
                ACTIVITY.append({"user_id": uid, **body}); return self.send(201)
            if path == "/rest/v1/rpc/delete_my_account":
                USERS.pop(uid, None); STATES.pop(uid, None)
                ACTIVITY[:] = [a for a in ACTIVITY if a["user_id"] != uid]
                for d in (ACCESS, REFRESH):
                    for k in [k for k, v in d.items() if v == uid]: d.pop(k)
                return self.send(204)
        if path == "/__debug":   # handy while testing: GET /__debug
            return self.send(200, {"users": [x["email"] for x in USERS.values()], "states": len(STATES), "activity": len(ACTIVITY)})
        self.send(404, {"message": f"no such route {path}"})


if __name__ == "__main__":
    print(f"fake Supabase on 0.0.0.0:{PORT}  (anon key: {API_KEY})", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), H).serve_forever()
