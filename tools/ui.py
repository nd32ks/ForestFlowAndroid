#!/usr/bin/env python3
"""Tiny uiautomator helper: `ui.py tap "text"` taps the centre of the first node whose text/content-desc contains it."""
import re, subprocess, sys, time, os
A = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
def sh(*a): return subprocess.run([A, *a], capture_output=True, text=True).stdout
def dump():
    for _ in range(4):
        sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        x = sh("shell", "cat", "/sdcard/ui.xml")
        if "<hierarchy" in x and "forest.flow" in x: return x
        time.sleep(0.6)
    return sh("shell", "cat", "/sdcard/ui.xml")
def find(label, exact=False, visible=False):
    x = dump()
    for m in re.finditer(r"<node [^>]*>", x):
        n = m.group(0)
        t = re.search(r' text="([^"]*)"', n); d = re.search(r' content-desc="([^"]*)"', n)
        for v in ((t.group(1) if t else ""), (d.group(1) if d else "")):
            if (v == label) if exact else (label in v):
                b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
                cy = (b[1] + b[3]) // 2
                if visible and not (330 < cy < 2050): continue
                return (b[0] + b[2]) // 2, cy
    return None
if __name__ == "__main__":
    cmd, label = sys.argv[1], sys.argv[2]; flags = sys.argv[3:]
    pos = find(label, "exact" in flags, "visible" in flags)
    if not pos: print("MISSING", label); sys.exit(1)
    if cmd == "tap": sh("shell", "input", "tap", str(pos[0]), str(pos[1])); print("tapped", label, pos)
    else: print(pos)
