#!/usr/bin/env bash
# Zyphuel iOS bootstrap — run this on macOS (Xcode 16 or newer).
#
#   bash ios/bootstrap.sh          generate the project and build for the simulator
#   bash ios/bootstrap.sh --test   generate, build and run the unit test suite
#
set -euo pipefail

cd "$(dirname "$0")"
IOS_DIR="$(pwd)"
RUN_TESTS=0
[[ "${1:-}" == "--test" ]] && RUN_TESTS=1

echo "==> Checking toolchain"
if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "ERROR: xcodebuild not found. Install Xcode 16+ from the App Store."
  exit 1
fi
xcodebuild -version

if ! command -v xcodegen >/dev/null 2>&1; then
  echo "==> Installing XcodeGen"
  if command -v brew >/dev/null 2>&1; then
    brew install xcodegen
  else
    echo "ERROR: Install XcodeGen manually (https://github.com/yonaskolb/XcodeGen)."
    exit 1
  fi
fi

if [[ ! -f Config/GoogleService-Info.plist ]]; then
  echo "WARNING: ios/Config/GoogleService-Info.plist is missing."
  echo "         The app will launch in setup mode; sign-in and Firestore stay disabled."
fi

echo "==> Generating Zyphuel.xcodeproj"
xcodegen generate

echo "==> Resolving Swift packages"
xcodebuild -project Zyphuel.xcodeproj -scheme Zyphuel \
  -resolvePackageDependencies -clonedSourcePackagesDirPath .packages

DESTINATION="platform=iOS Simulator,name=iPhone 16"
if ! xcrun simctl list devices available | grep -q "iPhone 16"; then
  DEVICE_ID="$(xcrun simctl list devices available | grep -m1 -E "iPhone" | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/')"
  if [[ -z "${DEVICE_ID:-}" ]]; then
    echo "ERROR: No iPhone simulator available. Install an iOS runtime in Xcode."
    exit 1
  fi
  DESTINATION="id=${DEVICE_ID}"
fi
echo "==> Destination: ${DESTINATION}"

if [[ "$RUN_TESTS" -eq 1 ]]; then
  xcodebuild test -project Zyphuel.xcodeproj -scheme Zyphuel \
    -destination "$DESTINATION" CODE_SIGNING_ALLOWED=NO
else
  xcodebuild build -project Zyphuel.xcodeproj -scheme Zyphuel \
    -destination "$DESTINATION" CODE_SIGNING_ALLOWED=NO
fi

echo "==> Done. Open ios/Zyphuel.xcodeproj in Xcode to run on a device."
