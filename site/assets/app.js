/* NOIR site — progressive enhancement only. No tracking, no cookies. */
(function () {
  "use strict";

  document.documentElement.classList.add("js");

  var reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var sleep = function (ms) {
    return new Promise(function (resolve) { setTimeout(resolve, reduced ? 0 : ms); });
  };

  /* ── reveal on scroll ─────────────────────────────────────────────────── */
  var revealables = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window) {
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("in");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08 });
    revealables.forEach(function (el) { observer.observe(el); });
  } else {
    revealables.forEach(function (el) { el.classList.add("in"); });
  }

  /* ── copy buttons ─────────────────────────────────────────────────────── */
  function legacyCopy(text) {
    var area = document.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.style.position = "fixed";
    area.style.left = "-9999px";
    document.body.appendChild(area);
    area.select();
    var ok = false;
    try { ok = document.execCommand("copy"); } catch (e) { /* ignore */ }
    area.remove();
    return ok;
  }

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text)
        .then(function () { return true; })
        .catch(function () { return legacyCopy(text); });
    }
    return Promise.resolve(legacyCopy(text));
  }

  document.querySelectorAll(".code-wrap").forEach(function (wrap) {
    var code = wrap.querySelector(".code");
    var button = wrap.querySelector(".copy");
    if (!code || !button) return;
    button.addEventListener("click", function () {
      copyText(code.innerText.trim()).then(function (ok) {
        var previous = button.textContent;
        button.textContent = ok ? "copied" : "failed";
        setTimeout(function () { button.textContent = previous; }, 1200);
      });
    });
  });

  /* ── email obfuscation ───────────────────────────────────────────────── */
  var emailLink = document.getElementById("email-link");
  var emailText = document.getElementById("email-text");
  if (emailLink && emailText) {
    var address = "ulv777777" + "@" + "gmail.com";
    emailText.textContent = address;
    emailLink.href = "mailto:" + address;
  }

  /* ── latest release + platform download ──────────────────────────────── */
  function detectPlatform() {
    var ua = navigator.userAgent;
    if (/Android|iPhone|iPad|iPod/i.test(ua)) return null;
    if (/Windows/i.test(ua)) return "Windows";
    if (/Macintosh|Mac OS X/i.test(ua)) return "macOS";
    if (/Linux|X11/i.test(ua)) return "Linux";
    return null;
  }

  function formatCount(value) {
    if (value >= 1000) return (value / 1000).toFixed(1).replace(/\.0$/, "") + "k";
    return String(value);
  }

  var download = document.getElementById("download-primary");
  var releaseMeta = document.getElementById("release-meta");
  var platform = detectPlatform();

  if (download) {
    if (platform) download.textContent = "Download for " + platform;
  }

  fetch("https://api.github.com/repos/Verlintas/NOIR/releases/latest", {
    headers: { Accept: "application/vnd.github+json" }
  })
    .then(function (response) { return response.ok ? response.json() : null; })
    .then(function (release) {
      if (!release) return;

      if (download) {
        download.href = release.html_url;
        download.textContent = "Download " + release.tag_name + (platform ? " for " + platform : "");
      }
      if (releaseMeta) {
        var published = release.published_at ? release.published_at.slice(0, 10) : "";
        releaseMeta.textContent = release.tag_name + (published ? " · " + published : "") +
          " · macOS / Windows / Linux · MIT";
      }
    })
    .catch(function () { /* offline: keep the static links */ });

  /* ── stars ───────────────────────────────────────────────────────────── */
  var stars = document.getElementById("stars");
  if (stars) {
    fetch("https://api.github.com/repos/Verlintas/NOIR", {
      headers: { Accept: "application/vnd.github+json" }
    })
      .then(function (response) { return response.ok ? response.json() : null; })
      .then(function (repo) {
        if (!repo) return;
        var count = stars.querySelector("b");
        if (count) count.textContent = formatCount(repo.stargazers_count);
        stars.hidden = false;
      })
      .catch(function () { /* keep hidden */ });
  }

  /* ── interactive terminal demo ───────────────────────────────────────── */
  var demoBody = document.getElementById("demo-body");
  var demoRun = document.getElementById("demo-run");
  var demoReset = document.getElementById("demo-reset");
  if (!demoBody || !demoRun) return;

  var LOGO = [
    "███╗   ██╗ ██████╗ ██╗██████╗ ",
    "████╗  ██║██╔═══██╗██║██╔══██╗",
    "██╔██╗ ██║██║   ██║██║██████╔╝",
    "██║╚██╗██║██║   ██║██║██╔══██╗",
    "██║ ╚████║╚██████╔╝██║██║  ██║",
    "╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═╝"
  ];

  var EXPOSURE = [
    ["系统", 119, 46],
    ["遥测/统计", 32, 12],
    ["广告", 21, 8],
    ["工具", 11, 4],
    ["安全/杀毒", 8, 3],
    ["社交", 8, 3],
    ["游戏", 7, 2],
    ["购物", 6, 2],
    ["金融", 5, 1]
  ];
  var UNCATEGORIZED = 25;

  var FINDINGS = [
    [36, "安全/杀毒", "com.miui.securitycenter", 12, 3],
    [34, "安全/杀毒", "com.bsp.catchlog", 1, 0],
    [34, "遥测/统计", "com.xiaomi.aiasst.vision", 7, 0],
    [34, "遥测/统计", "com.xiaomi.location.fused", 1, 0],
    [34, "遥测/统计", "com.xiaomi.aiasst.service", 6, 0],
    [32, "遥测/统计", "com.google.android.gms", 4, 0],
    [31, "系统", "com.android.dynsystem", 0, 0],
    [31, "系统", "com.miui.powerkeeper", 0, 0],
    [31, "系统", "com.miui.qr", 0, 0],
    [31, "系统", "com.miui.contentcatcher", 0, 0]
  ];

  var SPINNER = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏";
  var scanSteps = 256;

  function repeat(text, count) {
    var out = "";
    for (var i = 0; i < count; i++) out += text;
    return out;
  }

  function section(label) {
    return '<div class="tui-sec"><span class="tui-sec-label">' + label + "</span></div>";
  }

  function keyValue(key, value) {
    return '<div class="tui-kv"><span class="k">' + key + '</span><span class="v">' + value + "</span></div>";
  }

  function exposureRow(label, count, pct) {
    var filled = Math.max(1, Math.round(count * 30 / EXPOSURE[0][1]));
    var bar = '<span class="tui-bar-on">' + repeat("█", filled) + "</span>" +
      '<span class="tui-bar-off">' + repeat("░", 30 - filled) + "</span>";
    return '<div class="tui-exp"><span class="cat">' + label + "</span>" +
      '<span class="tui-bar">' + bar + "</span>" +
      '<span class="count">' + count + "</span>" +
      '<span class="pct">' + pct + "%</span></div>";
  }

  function findingRow(index, score, category, pkg, up, cmd) {
    var level = score >= 40 ? "bad" : score >= 25 ? "warn" : "good";
    return '<div class="tui-find">' +
      '<span class="num">' + index + "</span>" +
      '<span class="score ' + level + '">' + score + "</span>" +
      '<span class="cat">' + category + "</span>" +
      '<span class="pkg">' + pkg + "</span>" +
      '<span class="up">' + up + "</span>" +
      '<span class="cmd">' + cmd + "</span></div>";
  }

  function dashboardHTML() {
    var logo = LOGO.map(function (row) {
      return '<span class="tui-logo-line">' + row + "</span>";
    }).join("");

    var findings = FINDINGS.map(function (row, index) {
      return findingRow(index + 1, row[0], row[1], row[2], row[3], row[4]);
    }).join("");

    var exposure = EXPOSURE.map(function (row) {
      return exposureRow(row[0], row[1], row[2]);
    }).join("");

    var uncategorised = '<div class="tui-exp uncat"><span class="cat">未分类</span>' +
      '<span class="tui-dots">' + repeat("· ", 13) + "</span>" +
      '<span class="count">' + UNCATEGORIZED + "</span><span class=\"pct\"></span></div>";

    return '<div class="tui">' +
      '<div class="tui-brand"><div class="tui-logo" aria-hidden="true">' + logo + "</div>" +
      '<div class="tui-tags"><b>android leak audit</b><span>monospace edition</span>' +
      '<span class="blank"></span><span>Mac OS X aarch64</span></div></div>' +

      section("DEVICE") +
      keyValue("model", "2509FPN0BC") +
      keyValue("device", "178b0d2f  ·  popsicle") +
      keyValue("android", "16  ·  501 installed  ·  <b>256</b> flagged") +

      section("EXPOSURE") + exposure + uncategorised +

      section("TOP FINDINGS") +
      '<div class="tui-find head"><span class="num">#</span><span class="score">SCORE</span>' +
      '<span class="cat">CATEGORY</span><span class="pkg">PACKAGE</span>' +
      '<span class="up">UP</span><span class="cmd">CMD</span></div>' + findings +

      section("PLAN") +
      '<div class="tui-plan">' +
      '<div class="tui-planrow"><span class="k">risk</span><span class="tui-risk">' +
      '<span><span class="red">●</span> HIGH 20</span>' +
      '<span><span class="amber">●</span> MED 51</span>' +
      '<span><span class="green">●</span> LOW 31</span></span></div>' +
      '<div class="tui-planrow"><span class="k">actions</span><span class="tui-actions">' +
      '<span>人工复核 52</span><span>DNS 拦截 31</span><span>禁止录屏 12</span>' +
      '<span class="more">+1 more</span></span></div>' +
      '<div class="tui-planrow"><span class="k">auto-safe</span><span>' +
      '<span class="safe-n">19</span> <span class="dim">reversible appops, rollback written next to the report</span>' +
      "</span></div></div>" +
      "</div>" +

      '<div class="tui-after">' +
      '<div><span class="report-k">report</span>&nbsp;&nbsp;/Users/verlintas/NOIR/noir-out</div>' +
      '<div class="tui-prompt"><span class="ok">$</span> <span class="tui-caret"></span></div>' +
      "</div>";
  }

  var running = false;
  var generation = 0;

  function runLine(parent, html) {
    var div = document.createElement("div");
    div.innerHTML = html;
    parent.appendChild(div);
    return div;
  }

  async function runDemo() {
    if (running) return;
    running = true;
    var mine = ++generation;
    demoRun.disabled = true;
    demoBody.scrollLeft = 0;
    demoBody.scrollTop = 0;
    demoBody.innerHTML = "";

    var log = document.createElement("div");
    log.className = "tui-run";
    demoBody.appendChild(log);

    runLine(log, '<span class="prompt">$</span> noir ui');
    await sleep(reduced ? 0 : 240);
    if (mine !== generation) return;

    runLine(log, '[*] host: Mac OS X aarch64');
    runLine(log, '[*] device: 178b0d2f (2509FPN0BC)');
    await sleep(reduced ? 0 : 100);
    runLine(log, '[*] packages: 501');
    await sleep(reduced ? 0 : 100);
    runLine(log, '[*] reading dumpsys package ... 312 KiB');
    await sleep(reduced ? 0 : 130);
    runLine(log, '[*] parsed: 498');
    runLine(log, '[*] candidates (internet + sensitive): 256');
    await sleep(reduced ? 0 : 120);
    runLine(log, '[*] analysing 256 new/changed APKs (cache hits: 0)');
    await sleep(reduced ? 0 : 160);
    if (mine !== generation) return;

    var progress = runLine(log, "");
    var frames = reduced ? 1 : 56;
    for (var i = 0; i <= frames; i++) {
      if (mine !== generation) return;
      var ratio = i / frames;
      var done = Math.round(ratio * scanSteps);
      var pct = Math.floor(done * 100 / scanSteps);
      var filled = Math.round(ratio * 26);
      var eta = Math.max(0, Math.ceil((1 - ratio) * 4));
      progress.innerHTML = "  " +
        '<span class="pbar-on">' + repeat("█", filled) + "</span>" +
        '<span class="pbar-off">' + repeat("░", 26 - filled) + "</span> " +
        pct + "%  " + done + "/" + scanSteps +
        "  ·  ETA " + Math.floor(eta / 60) + ":" + String(eta % 60).padStart(2, "0") +
        '  <span class="spin">' + SPINNER[i % 10] + "</span>";
      await sleep(reduced ? 0 : 34);
    }

    await sleep(reduced ? 0 : 260);
    if (mine !== generation) return;

    demoBody.innerHTML = dashboardHTML();
    demoBody.scrollLeft = 0;
    demoBody.scrollTop = 0;
    running = false;
    demoRun.disabled = false;
  }

  function resetDemo() {
    generation++;
    running = false;
    demoRun.disabled = false;
    demoBody.scrollLeft = 0;
    demoBody.scrollTop = 0;
    demoBody.innerHTML = dashboardHTML();
  }

  demoRun.addEventListener("click", runDemo);
  if (demoReset) demoReset.addEventListener("click", resetDemo);
  resetDemo();
})();
