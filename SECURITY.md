# Security policy

## What NOIR is

NOIR is a **local, read-only-by-default** audit tool. It talks to a device through
`adb`, reads package metadata and APK strings, and writes reports to your disk.

- no telemetry, no analytics, no network calls of its own
- no root, no bootloader unlock, no system partition changes
- `--apply` only sets Android **app-ops**, which are reversible with `default`

## Reporting a vulnerability in NOIR

Please use GitHub's private vulnerability reporting:

<https://github.com/Verlintas/NOIR/security/advisories/new>

Include:

- affected version (`noir --version`) and OS
- a minimal reproduction
- impact (what an attacker could do)

We aim to acknowledge within 72 hours. Please do not open a public issue for
anything that could put users at risk before a fix is available.

## Out of scope

- Vulnerabilities in third-party apps that NOIR merely *reports* on. Report those
  to the app vendor.
- Anything that requires an already-compromised machine or device.
- "This tool can uninstall a system app for the current user" — that is documented
  behaviour, not a vulnerability.

## Using NOIR safely

- Only run it against devices you own or are explicitly authorised to test.
- `noir auto` is a dry run; nothing is executed without `--apply`.
- Review `plan.txt` and `rollback.txt` before applying anything.
- Uninstalling a package for user 0 can break device features. `noir block`
  prints the exact restore command (`pm install-existing`) before doing anything.
