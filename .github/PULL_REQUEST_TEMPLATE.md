## What changed

<!-- one or two sentences -->

## Why

<!-- the problem this solves; link an issue if there is one -->

## How to test

```bash
./gradlew :core:jvmTest :cli:installDist
./cli/build/install/noir/bin/noir scan --only 'com\.example\.'
```

## Checklist

- [ ] `:core:jvmTest` passes
- [ ] `:cli:installDist` passes
- [ ] dashboard changes survive `COLUMNS=50`
- [ ] no new network calls
- [ ] `CHANGELOG.md` updated under `Unreleased`
- [ ] no new destructive action in `auto` without an explicit flag
