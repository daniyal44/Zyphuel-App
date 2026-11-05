#!/usr/bin/env python3
"""Synchronise the Zyphuel app version across Android, iOS and documentation.

`version.properties` is the single source of truth. Android Gradle reads it directly;
this script keeps the iOS project, the scripts and the docs identical, and can fail CI
on drift.

Usage
    python scripts/sync_versions.py            # apply version.properties to every target
    python scripts/sync_versions.py --bump     # increment once (code +1, name +0.01) then apply
    python scripts/sync_versions.py --check    # exit 1 if any target has drifted (CI)

The versionName format is the project sub-version format: 2.6.4.0.0.38
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Callable, NamedTuple

ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "version.properties"
NAME_PATTERN = re.compile(r"^\d+\.\d+\.\d+\.\d+\.\d+\.\d+$")
GRADLE_FILE = ROOT / "app" / "build.gradle.kts"

# Documents whose *header banner* advertises the current release. Only the first few
# lines are touched so historical changelog entries stay accurate.
DOC_HEADER_GLOBS = [
    "ARCHITECTURE.md",
    "DATABASE.md",
    "MIGRATIONS.md",
    "SECURITY.md",
    "TESTING.md",
    "PRIVACY_POLICY.md",
    "TERMS_AND_CONDITIONS.md",
    "PLAY_STORE_DATA_SAFETY_AND_COMPLIANCE.md",
    "GLOBAL_AUDIT_AND_FEATURE_INTEGRITY.md",
    "WEBSITE_INTEGRATION_AND_PLAY_STORE_GUIDE.md",
    "PLAY_STORE_ASO_BLUEPRINT.md",
    "MASTER_GLOBAL_SEO_AEO_GEO_STRATEGY.md",
    "rule.md",
    "docs/*.md",
    "docs_dev/*.md",
    "vibe-coding-context-files/*.md",
]
HEADER_LINES = 12

VERSION_TOKEN = re.compile(r"(?<![\d.])(v?)(\d+\.\d+\.\d+\.\d+\.\d+\.\d+)(?![\d.])")
BUILD_PATTERNS = [
    re.compile(r"(Build Code:\*\*\s*<font[^>]*><b>)(\d+)"),
    re.compile(r"(Production Build `)(\d+)"),
    re.compile(r"(Build Code:\s*)(\d+)"),
    re.compile(r"(Build )(\d+)"),
]

# Files that describe the current UI/build in their body, not only in a banner.
BODY_PATTERNS: dict[str, list[tuple[re.Pattern[str], Callable[[re.Match[str], int, str], str]]]] = {
    "docs/Side_bar.md": [
        (VERSION_TOKEN, lambda m, _c, n: f"{m.group(1)}{n}"),
        (re.compile(r"(Build )(\d+)"), lambda m, c, _n: f"{m.group(1)}{c}"),
    ],
    "docs/structure.md": [
        (
            re.compile(r"(?:shared version\.properties: )?versionCode \d+, versionName [\d.]+"),
            lambda _m, c, n: f"shared version.properties: versionCode {c}, versionName {n}",
        ),
    ],
}


class Target(NamedTuple):
    path: Path
    pattern: str
    replace: Callable[[re.Match[str]], str]
    label: str


def read_version() -> tuple[int, str]:
    if not VERSION_FILE.exists():
        sys.exit("version.properties is missing; restore it from git.")
    values: dict[str, str] = {}
    for raw in VERSION_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        if separator:
            values[key.strip()] = value.strip()

    try:
        code = int(values["versionCode"])
    except (KeyError, ValueError):
        sys.exit("version.properties needs an integer versionCode")

    name = values.get("versionName", "")
    if not NAME_PATTERN.match(name):
        sys.exit(f"versionName '{name}' must use the 2.6.4.0.0.XX sub-version format")

    return code, name


def write_version(code: int, name: str) -> None:
    VERSION_FILE.write_text(
        "# Zyphuel single source of truth for the app version.\n"
        "# Android Gradle reads this file directly; iOS and the docs are synchronised by:\n"
        "#   python scripts/sync_versions.py --bump\n"
        "# Both platforms always ship the same versionCode / versionName.\n"
        f"versionCode={code}\n"
        f"versionName={name}\n",
        encoding="utf-8",
        newline="\n",
    )


def targets(code: int, name: str) -> list[Target]:
    return [
        Target(
            ROOT / "ios" / "project.yml",
            r'MARKETING_VERSION: "[^"]+"',
            lambda _m: f'MARKETING_VERSION: "{name}"',
            "ios/project.yml MARKETING_VERSION",
        ),
        Target(
            ROOT / "ios" / "project.yml",
            r'CURRENT_PROJECT_VERSION: "\d+"',
            lambda _m: f'CURRENT_PROJECT_VERSION: "{code}"',
            "ios/project.yml CURRENT_PROJECT_VERSION",
        ),
        Target(
            ROOT / "ios" / "Zyphuel" / "Core" / "Config" / "ZyphuelConfig.swift",
            r'static let marketingVersion = "[^"]+"',
            lambda _m: f'static let marketingVersion = "{name}"',
            "ZyphuelConfig.marketingVersion",
        ),
        Target(
            ROOT / "ios" / "Zyphuel" / "Core" / "Config" / "ZyphuelConfig.swift",
            r"static let buildNumber = \d+",
            lambda _m: f"static let buildNumber = {code}",
            "ZyphuelConfig.buildNumber",
        ),
        Target(
            ROOT / "README.md",
            r"(<p><b>Version )\d+(?:\.\d+)+ \(Build \d+\)",
            lambda m: f"{m.group(1)}{name} (Build {code})",
            "README version line",
        ),
        Target(
            ROOT / "README.md",
            r"App%20Version-v[\d.]+%20\(Build%20\d+\)",
            lambda _m: f"App%20Version-v{name}%20(Build%20{code})",
            "README badge URL",
        ),
        Target(
            ROOT / "README.md",
            r'alt="App Version v[\d.]+"',
            lambda _m: f'alt="App Version v{name}"',
            "README badge alt text",
        ),
        Target(
            ROOT / "README.md",
            r"Key Features \(v[\d.]+ Latest\)",
            lambda _m: f"Key Features (v{name} Latest)",
            "README key-features heading",
        ),
        Target(
            ROOT / "README.md",
            r"component specifications \(v[\d.]+\)",
            lambda _m: f"component specifications (v{name})",
            "README feature-doc reference",
        ),
        Target(
            ROOT / "FEATURES_DOCUMENTATION.md",
            r"\*\*App Version:\*\* `v?[\d.]+ \(Build \d+\)`",
            lambda _m: f"**App Version:** `v{name} (Build {code})`",
            "FEATURES_DOCUMENTATION version",
        ),
        Target(
            ROOT / "FEATURES_DOCUMENTATION.md",
            r"## 0\. iOS Application — Native SwiftUI Port \(v[\d.]+\)",
            lambda _m: f"## 0. iOS Application — Native SwiftUI Port (v{name})",
            "FEATURES_DOCUMENTATION iOS heading",
        ),
    ]


def check_gradle_wiring(problems: list[str]) -> None:
    if not GRADLE_FILE.exists():
        problems.append("app/build.gradle.kts is missing")
        return
    text = GRADLE_FILE.read_text(encoding="utf-8")
    for needle in ("version.properties", "versionCode = appVersionCode", "versionName = appVersionName"):
        if needle not in text:
            problems.append(f"app/build.gradle.kts no longer reads {needle}")


def header_targets(code: int, name: str) -> list[Path]:
    paths: list[Path] = []
    for pattern in DOC_HEADER_GLOBS:
        paths.extend(sorted(ROOT.glob(pattern)))
    return [p for p in paths if p.is_file()]


def sync_header(path: Path, code: int, name: str, check_only: bool, problems: list[str], updated: list[str]) -> None:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    head, tail = lines[:HEADER_LINES], lines[HEADER_LINES:]

    def version_repl(match: re.Match[str]) -> str:
        return f"{match.group(1)}{name}"

    def build_repl(match: re.Match[str]) -> str:
        return f"{match.group(1)}{code}"

    new_head = []
    for line in head:
        new_line = VERSION_TOKEN.sub(version_repl, line)
        if "Build" in new_line:
            for pattern in BUILD_PATTERNS:
                new_line = pattern.sub(build_repl, new_line)
        new_head.append(new_line)

    special = BODY_PATTERNS.get(path.relative_to(ROOT).as_posix())
    changed_body = False
    if special:
        joined_tail = "".join(tail)
        for pattern, replacement in special:
            joined_tail = pattern.sub(lambda m, p=pattern, r=replacement: r(m, code, name), joined_tail)
        changed_body = joined_tail != "".join(tail)
        tail = [joined_tail]

    if new_head == head and not changed_body:
        return
    if check_only:
        reason = "header version out of date" if new_head != head else "body version out of date"
        problems.append(f"{path.relative_to(ROOT)}: {reason}")
        return
    path.write_text("".join(new_head + tail), encoding="utf-8", newline="\n")
    updated.append(str(path.relative_to(ROOT)))


def sync(code: int, name: str, check_only: bool) -> int:
    problems: list[str] = []
    updated: list[str] = []

    check_gradle_wiring(problems)

    for target in targets(code, name):
        if not target.path.exists():
            problems.append(f"{target.label}: {target.path.relative_to(ROOT)} is missing")
            continue
        text = target.path.read_text(encoding="utf-8")
        if not re.findall(target.pattern, text):
            problems.append(f"{target.label}: pattern not found")
            continue
        new_text = re.sub(target.pattern, target.replace, text)
        if new_text == text:
            continue
        if check_only:
            problems.append(f"{target.label}: out of date")
        else:
            target.path.write_text(new_text, encoding="utf-8", newline="\n")
            updated.append(target.label)

    for path in header_targets(code, name):
        sync_header(path, code, name, check_only, problems, updated)

    if check_only:
        if problems:
            print("Version sync check FAILED:")
            for problem in problems:
                print(f"  - {problem}")
            print(f"\nRun: python scripts/sync_versions.py --bump   (or without --bump to apply {name})")
            return 1
        print(f"Version sync check OK: v{name} (Build {code}) everywhere.")
        return 0

    for label in updated:
        print(f"  updated {label}")
    if problems:
        print("\nVersion sync finished with problems:")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print(f"Version sync complete: v{name} (Build {code}).")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Synchronise Zyphuel versions across Android, iOS and docs.")
    parser.add_argument("--bump", action="store_true", help="increment versionCode and the trailing versionName segment")
    parser.add_argument("--check", action="store_true", help="verify every target matches version.properties")
    args = parser.parse_args()

    if args.bump and args.check:
        parser.error("--bump and --check cannot be combined")

    code, name = read_version()

    if args.bump:
        code += 1
        parts = name.split(".")
        parts[-1] = f"{int(parts[-1]) + 1:02d}"
        name = ".".join(parts)
        write_version(code, name)
        print(f"Bumped to v{name} (Build {code})")

    return sync(code, name, args.check)


if __name__ == "__main__":
    raise SystemExit(main())
