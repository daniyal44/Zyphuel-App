"""
Zyphuel Fuel Price API Tester
=============================
Run this script on your PC to test which fuel price APIs actually work.
Usage: python test_fuel_apis.py

This tests all 7 sources used by the Zyphuel app and shows which ones
return valid fuel prices.
"""

import urllib.request
import urllib.error
import json
import re
import ssl
import sys

# Disable SSL verification for testing (some Pakistan sites have cert issues)
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/json,*/*',
    'Accept-Language': 'en-US,en;q=0.9',
}

def fetch(url, accept='text/html'):
    """Fetch a URL and return response text."""
    headers = dict(HEADERS)
    headers['Accept'] = accept
    req = urllib.request.Request(url, headers=headers)
    try:
        resp = urllib.request.urlopen(req, timeout=15, context=ctx)
        return resp.read().decode('utf-8', errors='replace')
    except Exception as e:
        return f"ERROR: {e}"

def extract_pkr_price(text, fuel_type=""):
    """Extract PKR price from text using regex."""
    patterns = [
        r'(?:Rs\.?|PKR|₨)\s*(\d{2,3}(?:\.\d{1,2})?)',
        r'(\d{2,3}\.\d{1,2})\s*(?:Rs\.?|PKR)',
        r'(\d{2,3}(?:\.\d{1,2})?)\s*(?:per|/)\s*(?:litre|liter|L)',
    ]
    for pattern in patterns:
        for match in re.finditer(pattern, text, re.IGNORECASE):
            price = float(match.group(1))
            if 150 <= price <= 600:
                return price
    return 0

def test_source_1_oilprices_pk():
    """Test OilPrices.pk API (JSON)"""
    print("\n" + "="*60)
    print("SOURCE 1: OilPrices.pk API")
    print("URL: https://oilprices.pk/api/latest")
    print("="*60)

    body = fetch("https://oilprices.pk/api/latest", accept="application/json")
    if body.startswith("ERROR"):
        print(f"  FAILED: {body}")
        return None

    print(f"  Response length: {len(body)} chars")
    print(f"  First 500 chars: {body[:500]}")

    try:
        data = json.loads(body)
        print(f"  Parsed JSON keys: {list(data.keys()) if isinstance(data, dict) else 'array'}")

        # Try to extract prices from various JSON structures
        petrol = diesel = octane = 0

        if isinstance(data, dict):
            # Direct keys
            for k in ['petrol', 'petrol_price', 'ms_petrol', 'motor_spirit', 'gasoline']:
                v = data.get(k, 0)
                if isinstance(v, (int, float)) and 150 <= v <= 600:
                    petrol = v; break
                elif isinstance(v, dict):
                    petrol = v.get('price', v.get('rate', 0))
                    if 150 <= petrol <= 600: break

            for k in ['diesel', 'diesel_price', 'hsd', 'high_speed_diesel']:
                v = data.get(k, 0)
                if isinstance(v, (int, float)) and 150 <= v <= 600:
                    diesel = v; break
                elif isinstance(v, dict):
                    diesel = v.get('price', v.get('rate', 0))
                    if 150 <= diesel <= 600: break

            for k in ['high_octane', 'hi_octane', 'hobc', 'octane']:
                v = data.get(k, 0)
                if isinstance(v, (int, float)) and 150 <= v <= 600:
                    octane = v; break
                elif isinstance(v, dict):
                    octane = v.get('price', v.get('rate', 0))
                    if 150 <= octane <= 600: break

            # Try data wrapper
            if petrol == 0 and 'data' in data:
                inner = data['data']
                if isinstance(inner, dict):
                    for k in ['petrol', 'petrol_price']:
                        v = inner.get(k, 0)
                        if isinstance(v, (int, float)) and 150 <= v <= 600:
                            petrol = v; break
                        elif isinstance(v, dict):
                            petrol = v.get('price', 0)
                            if 150 <= petrol <= 600: break
                elif isinstance(inner, list):
                    for item in inner:
                        name = str(item.get('name', item.get('product', item.get('fuel', '')))).lower()
                        price = item.get('price', item.get('rate', 0))
                        if isinstance(price, (int, float)) and 150 <= price <= 600:
                            if 'petrol' in name and petrol == 0: petrol = price
                            elif 'diesel' in name and diesel == 0: diesel = price
                            elif 'octane' in name and octane == 0: octane = price

        print(f"\n  EXTRACTED PRICES:")
        print(f"    Petrol:      Rs. {petrol}")
        print(f"    Diesel:      Rs. {diesel}")
        print(f"    High Octane: Rs. {octane}")
        print(f"  STATUS: {'WORKING' if petrol > 0 and diesel > 0 else 'FAILED TO EXTRACT PRICES'}")
        return {'petrol': petrol, 'diesel': diesel, 'octane': octane}
    except json.JSONDecodeError:
        print(f"  NOT VALID JSON - might be HTML or error page")
        return None

def test_source_2_trackmate():
    """Test TrackMate API (JSON)"""
    print("\n" + "="*60)
    print("SOURCE 2: TrackMate Fuel API")
    print("URL: https://fuel.trackmate.page/api/prices")
    print("="*60)

    body = fetch("https://fuel.trackmate.page/api/prices", accept="application/json")
    if body.startswith("ERROR"):
        print(f"  FAILED: {body}")
        return None

    print(f"  Response length: {len(body)} chars")
    print(f"  First 500 chars: {body[:500]}")

    try:
        data = json.loads(body)
        print(f"  Parsed JSON: {type(data).__name__}")
        if isinstance(data, dict):
            print(f"  Keys: {list(data.keys())}")
        elif isinstance(data, list):
            print(f"  Array length: {len(data)}")
            if data: print(f"  First item: {data[0]}")

        print(f"  STATUS: RETURNED DATA (check structure above)")
        return data
    except json.JSONDecodeError:
        print(f"  NOT VALID JSON")
        return None

def test_source_3_hamariweb():
    """Test HamariWeb (HTML scraping)"""
    print("\n" + "="*60)
    print("SOURCE 3: HamariWeb Pakistan")
    print("URL: https://hamariweb.com/finance/petrol-prices.aspx")
    print("="*60)

    html = fetch("https://hamariweb.com/finance/petrol-prices.aspx")
    if html.startswith("ERROR"):
        print(f"  FAILED: {html}")
        return None

    print(f"  Response length: {len(html)} chars")
    print(f"  Contains 'petrol': {'petrol' in html.lower()}")
    print(f"  Contains 'diesel': {'diesel' in html.lower()}")
    print(f"  Contains 'Rs.': {'Rs.' in html}")

    # Extract prices
    petrol = diesel = octane = 0
    lines = re.split(r'<tr|<td|<div|<span|\n', html)
    for i, line in enumerate(lines):
        lower = line.lower()
        if len(lower) < 3: continue
        for j in range(i, min(i+5, len(lines))):
            price = extract_pkr_price(lines[j])
            if price > 0:
                if petrol == 0 and ('petrol' in lower or 'motor spirit' in lower) and 'octane' not in lower:
                    petrol = price
                elif diesel == 0 and ('diesel' in lower or 'hsd' in lower):
                    diesel = price
                elif octane == 0 and ('octane' in lower or 'hobc' in lower):
                    octane = price
                break

    print(f"\n  EXTRACTED PRICES:")
    print(f"    Petrol:      Rs. {petrol}")
    print(f"    Diesel:      Rs. {diesel}")
    print(f"    High Octane: Rs. {octane}")
    print(f"  STATUS: {'WORKING' if petrol > 0 and diesel > 0 else 'FAILED TO EXTRACT PRICES'}")
    return {'petrol': petrol, 'diesel': diesel, 'octane': octane}

def test_source_4_globalpetrol():
    """Test GlobalPetrolPrices (HTML scraping)"""
    print("\n" + "="*60)
    print("SOURCE 4: GlobalPetrolPrices.com")
    print("URL: https://www.globalpetrolprices.com/Pakistan/gasoline_prices/")
    print("="*60)

    html = fetch("https://www.globalpetrolprices.com/Pakistan/gasoline_prices/")
    if html.startswith("ERROR"):
        print(f"  FAILED: {html}")
        return None

    print(f"  Response length: {len(html)} chars")
    petrol = extract_pkr_price(html)

    html2 = fetch("https://www.globalpetrolprices.com/Pakistan/diesel_prices/")
    diesel = extract_pkr_price(html2) if not html2.startswith("ERROR") else 0

    print(f"\n  EXTRACTED PRICES:")
    print(f"    Petrol: Rs. {petrol}")
    print(f"    Diesel: Rs. {diesel}")
    print(f"  STATUS: {'WORKING' if petrol > 0 else 'FAILED'}")
    return {'petrol': petrol, 'diesel': diesel}

def test_source_5_google():
    """Test Google Search (HTML)"""
    print("\n" + "="*60)
    print("SOURCE 5: Google Search")
    print("URL: https://www.google.com/search?q=current+petrol+price+pakistan+PKR")
    print("="*60)

    html = fetch("https://www.google.com/search?q=current+petrol+price+in+pakistan+per+litre+PKR+today")
    if html.startswith("ERROR"):
        print(f"  FAILED: {html}")
        return None

    print(f"  Response length: {len(html)} chars")

    # Extract all PKR prices found
    prices = []
    for match in re.finditer(r'(?:Rs\.?|PKR|₨)\s*(\d{2,3}(?:\.\d{1,2})?)', html, re.IGNORECASE):
        p = float(match.group(1))
        if 150 <= p <= 600:
            prices.append(p)

    print(f"  PKR prices found in results: {prices[:10]}")
    petrol = prices[0] if prices else 0

    print(f"\n  Most likely petrol price: Rs. {petrol}")
    print(f"  STATUS: {'WORKING' if petrol > 0 else 'FAILED (possibly CAPTCHA)'}")
    return {'petrol': petrol}

def test_source_6_pso():
    """Test PSO website"""
    print("\n" + "="*60)
    print("SOURCE 6: PSO Pakistan")
    print("URL: https://psopk.com/en/products-and-services/product-prices")
    print("="*60)

    html = fetch("https://psopk.com/en/products-and-services/product-prices")
    if html.startswith("ERROR"):
        print(f"  FAILED: {html}")
        return None

    print(f"  Response length: {len(html)} chars")
    has_price = bool(re.search(r'\d{2,3}\.\d{2}', html))
    has_fuel_word = 'petrol' in html.lower() or 'diesel' in html.lower()
    print(f"  Contains fuel keywords: {has_fuel_word}")
    print(f"  Contains price-like numbers: {has_price}")

    if not has_fuel_word:
        print(f"  NOTE: PSO is a JavaScript SPA - HTML might be empty shell")
        print(f"  First 300 chars of body: {html[:300]}")

    print(f"  STATUS: {'MIGHT WORK' if has_price and has_fuel_word else 'LIKELY EMPTY (JS SPA)'}")
    return None

def test_source_7_shell():
    """Test Shell website"""
    print("\n" + "="*60)
    print("SOURCE 7: Shell Pakistan")
    print("URL: https://www.shell.com.pk/motorists/shell-fuels/fuel-pricing.html")
    print("="*60)

    html = fetch("https://www.shell.com.pk/motorists/shell-fuels/fuel-pricing.html")
    if html.startswith("ERROR"):
        print(f"  FAILED: {html}")
        return None

    print(f"  Response length: {len(html)} chars")
    has_price = bool(re.search(r'\d{2,3}\.\d{2}', html))
    has_fuel_word = 'petrol' in html.lower() or 'diesel' in html.lower() or 'fuel' in html.lower()
    print(f"  Contains fuel keywords: {has_fuel_word}")
    print(f"  Contains price-like numbers: {has_price}")

    if not has_fuel_word:
        print(f"  NOTE: Shell is a JavaScript SPA - HTML might be empty shell")

    print(f"  STATUS: {'MIGHT WORK' if has_price and has_fuel_word else 'LIKELY EMPTY (JS SPA)'}")
    return None

if __name__ == '__main__':
    print("=" * 60)
    print("  ZYPHUEL FUEL PRICE API TESTER")
    print("  Testing all 7 sources used by the app...")
    print("=" * 60)

    results = {}

    results['oilprices_pk'] = test_source_1_oilprices_pk()
    results['trackmate'] = test_source_2_trackmate()
    results['hamariweb'] = test_source_3_hamariweb()
    results['globalpetrol'] = test_source_4_globalpetrol()
    results['google'] = test_source_5_google()
    results['pso'] = test_source_6_pso()
    results['shell'] = test_source_7_shell()

    print("\n\n" + "=" * 60)
    print("  SUMMARY")
    print("=" * 60)

    working = []
    failed = []
    for name, data in results.items():
        if data and isinstance(data, dict) and data.get('petrol', 0) > 0:
            working.append(f"  {name}: Petrol Rs.{data['petrol']}, Diesel Rs.{data.get('diesel', '?')}")
        else:
            failed.append(f"  {name}: FAILED or NO PRICES EXTRACTED")

    print(f"\nWORKING SOURCES ({len(working)}):")
    for w in working:
        print(w)

    print(f"\nFAILED SOURCES ({len(failed)}):")
    for f in failed:
        print(f)

    print("\n" + "=" * 60)
    print("COPY THE FULL OUTPUT ABOVE AND SHARE IT WITH CLAUDE")
    print("=" * 60)

    input("\nPress Enter to exit...")
