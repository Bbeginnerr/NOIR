# NOIR website

The landing page is plain static HTML/CSS/JS. No build step, no framework, no
external fonts, no analytics. Publish the **contents** of `site/` under
`/noir/`.

```
site/
├── index.html                  EN — /noir/
├── zh/index.html               ZH — /noir/zh/
├── 404.html                    self-contained error page
├── robots.txt
├── sitemap.xml
├── _headers                    CSP / caching for Netlify or Cloudflare Pages
└── assets/
    ├── style.css
    ├── app.js                  reveal, copy, email, release, stars, demo
    ├── og.png                  1200×630 social card (X / Open Graph)
    ├── icon.svg
    ├── icon-192.png
    ├── icon-512.png
    ├── apple-touch-icon.png
    └── manifest.webmanifest
```

`docs/og-card.html` is the source for `site/assets/og.png`. `docs/site.md` is
this document; neither is part of the published site.

## Preview

GitHub API calls and clipboard access need `http(s)`, so do not preview with
`file://` if you want to test the live version / star count:

```bash
python3 -m http.server -d site 8080
# http://localhost:8080/       EN
# http://localhost:8080/zh/    ZH
```

Without JavaScript the page still renders; the version line stays static, the
star badge stays hidden, the email stays hidden, and the terminal demo shows a
short fallback notice.

## Deploy

### GitHub Pages (verlintas.dev)

The domain is served from the `Verlintas/Verlintas.github.io` repository under
the `noir/` directory:

```bash
rsync -a --delete site/ ../verlintas-site/noir/
cd ../verlintas-site
git add noir
git commit -m "site: publish NOIR landing page"
git push
```

GitHub Pages ignores `_headers`; that file is for Netlify / Cloudflare Pages.

### Any static host / nginx

```bash
rsync -a --delete site/ user@host:/var/www/verlintas.dev/noir/
```

If the host supports SPA-style 404s, point `/noir/*` errors at
`/noir/404.html`. The page is self-contained and its links use `/noir/`
absolutely.

### Netlify / Cloudflare Pages

Set the publish directory to `site`. `_headers` already contains:

- `Content-Security-Policy` limited to self, `api.github.com`, inline styles and
  the inline JSON-LD block
- `nosniff`, `Referrer-Policy`, `X-Frame-Options`, `Permissions-Policy`
- one-week cache for `/assets/*`

## Regenerate the social card

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=2 --window-size=1200,630 \
  --screenshot=/tmp/og2x.png \
  "file://$PWD/docs/og-card.html"
sips -z 630 1200 /tmp/og2x.png --out site/assets/og.png
```

Keep it at 1200×630; X, Slack, Discord and GitHub all crop around that ratio.

## Per-release checklist

- Nothing to edit for the download buttons: they read the latest release from
  the GitHub API and link to the GitHub release page, never directly to an
  asset. Keep it that way so the download flow, checksums and release notes stay
  together.
- `sitemap.xml` only changes if you add routes.
- `robots.txt`, canonical and `og:url` assume `https://verlintas.dev/noir/`.
- If the X handle changes, update `08 / Source` and the contact list in both
  `index.html` and `zh/index.html`.
- The demo numbers (`501` apps, `256` flagged, `19` app-ops) are illustrative,
  not a real report. Keep the "simulated" label visible.
- The demo runs the guided `noir` path end to end: first-run prompt → scan
  progress → dashboard → apply or skip. It mirrors
  `core/src/commonMain/kotlin/app/noir/core/Tui.kt` (logo, DEVICE, EXPOSURE
  percentages, TOP FINDINGS UP/CMD columns, PLAN). Update `app.js` when the TUI
  or guided flow changes.

## Content decisions

- Minimal monospace UI, black background, cyan → violet gradient only as accent.
- No screenshot or screen recording; `03 / Demo` is a replayed terminal.
- Live star count from the unauthenticated GitHub API. No trendshift badge.
- No roadmap, no sample report, no DeepScan sample section.
- Email is assembled in JS at runtime; it is not in the HTML source.
- Chinese page is a full translation, not a machine-translated fragment.
- Animations respect `prefers-reduced-motion`; all content is visible without JS.
