# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.1.2] — 2026-11-01

### Fixed

- deepscan: skip decompiled library code (androidx / kotlin / okhttp / gson …) when
  counting signals — the filter used paths relative to the output root instead of
  `sources/`, so `Class.forName` in androidX was inflating `reflection`
- deepscan: surface a `jadx exit != 0` as an error instead of silently scoring an
  empty output directory

## [0.1.1] — 2026-11-01

### Added

- `noir deepscan` — pull + decompile the riskiest apps in parallel and score
  code-level signals (`exec`, dynamic loading, app disabling, accessibility
  rewriting, screen capture, reflection, crypto, networking)
- `--apply` / `--purge` cleanup flow after DeepScan, with per-app confirmation
- decompilation cache keyed by package + APK path
- portable, double-clickable builds with a bundled runtime (jpackage):
  `Start NOIR.command` / `Start NOIR.bat` / `Start-NOIR.sh`

### Changed

- multiple connected devices: a physical device is preferred over an emulator;
  interactive runs ask which one to use

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
