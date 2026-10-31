# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.1.0] — 2026-11-01

### Added

- `noir scan` — inventory every app with INTERNET + sensitive permissions
- endpoint extraction from APKs on-device (`unzip | strings`), with a pull-based fallback
- classifier (telemetry / ads / security / AI / location / IME / carrier …) and vendor attribution
- risk scoring based on permissions, endpoints and system/privileged status
- `noir ui` — adaptive monospace dashboard (CJK-aware width, ANSI gradient, `--ascii` fallback)
- `noir auto` — plan + `--apply` for reversible app-ops, plus `plan.txt` / `rollback.txt`
- `noir deep <pkg>` — pull one APK, run jadx, grep for red-flag calls
- `noir block <pkg…>` — print or apply neutralisation commands with restore hints
- `noir setup` — download `adb` / `jadx` into `~/.noir/tools` when missing
- `noir doctor` — environment check
- guided first run when invoked without arguments
- device onboarding: developer-mode / USB-debugging instructions, 180 s wait loop,
  dedicated messaging for `unauthorized`
- `noir --about`, `noir --version`
- shared-UID aware `dumpsys package` parser (system-uid permissions live in a separate section)
- endpoint cache keyed by package + APK path
- cross-platform CI on Ubuntu, macOS and Windows
- release workflow producing signed-checksum zip archives per OS

### Known limits

- static analysis only: it proves capability and endpoints, not actual traffic
- app-ops cannot revoke `QUERY_ALL_PACKAGES` or device identifiers
- DNS-level blocking is exported as `domains.txt`, not applied
