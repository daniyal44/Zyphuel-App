"""
Zyphuel Authorized Fuel Price Verification
==========================================
Run: python test_fuel_apis.py

Fetches https://zyphuel.netlify.app/order, extracts the published petrol,
diesel and high-octane retail rates, and verifies they match the values
published on the authorized website (Petrol: 327.00 / Diesel: 360.00 / High-Octane: 350.00).

Only the authorized Zyphuel website is contacted. Fuel Track Mate, PSO,
Shell and third-party aggregators are prohibited and never used.
"""

import re
import ssl
import sys
import urllib.request

AUTHORIZED_URL = "https://zyphuel.netlify.app/order"
EXPECTED = {"petrol": 327.00, "diesel": 360.00, "high_octane": 350.00}

PATTERNS = {
    "petrol": r"Petrol.{0,200}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)(?:<[^>]+>)*\s*/\s*L",
    "diesel": r"Diesel.{0,200}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)(?:<[^>]+>)*\s*/\s*L",
    "high_octane": r"High-?Octane.{0,200}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)(?:<[^>]+>)*\s*/\s*L",
}

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Zyphuel Official App)",
    "Accept": "text/html,application/xhtml+xml,*/*",
    "Accept-Language": "en-US,en;q=0.9",
}


def fetch(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=20, context=ctx) as resp:
        return resp.read().decode("utf-8", errors="replace")


def main():
    print("=" * 60)
    print("  ZYPHUEL AUTHORIZED FUEL PRICE VERIFICATION")
    print("  Source: " + AUTHORIZED_URL)
    print("=" * 60)
    try:
        html = fetch(AUTHORIZED_URL)
    except Exception as exc:
        print("FAILED to fetch authorized website: %s" % exc)
        return 1
    print("Response length: %d chars" % len(html))
    failures = []
    for fuel, pattern in PATTERNS.items():
        match = re.search(pattern, html, re.I | re.S)
        if not match:
            # Fallback to secondary pattern
            fallback = rf"{fuel}.{{0,300}}?Rs\.?\s*(\d{{2,4}}(?:\.\d{{1,2}})?)\s*/\s*L"
            match = re.search(fallback, html, re.I | re.S)
        if not match:
            print("  %s: NO MATCH" % fuel)
            failures.append("%s not found on authorized website" % fuel)
            continue
        value = float(match.group(1))
        ok = abs(value - EXPECTED[fuel]) < 0.001
        print("  %s: Rs. %.2f/L (authorized website expects %.2f) -> %s"
              % (fuel, value, EXPECTED[fuel], "OK" if ok else "MISMATCH"))
        if not ok:
            failures.append("%s: website %.2f != expected %.2f" % (fuel, value, EXPECTED[fuel]))
    date_match = re.search(r"Last updated:?.{0,200}?(\d{1,2}\s+[A-Za-z]+\s+\d{4})", html, re.I | re.S)
    print("  Effective date: %s" % (date_match.group(1) if date_match else "not published"))
    print("=" * 60)
    if failures:
        for failure in failures:
            print("FAIL: " + failure)
        return 1
    print("PASS: authorized website prices match the verified values")
    print("      Petrol 327.00 / Diesel 360.00 / High-Octane 350.00")
    print("      Only the authorized Zyphuel website was contacted.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
