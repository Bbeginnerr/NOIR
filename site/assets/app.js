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

  var CATEGORIES = [
    ["系统", 119], ["遥测/统计", 32], ["广告", 21], ["工具", 11],
    ["安全/杀毒", 8], ["社交", 8], ["游戏", 7], ["购物", 6], ["金融", 5]
  ];
  var FINDINGS = [
    [36, "安全/杀毒", "com.miui.securitycenter", 12, 3],
    [34, "安全/杀毒", "com.bsp.catchlog", 1, 0],
    [34, "遥测/统计", "com.xiaomi.aiasst.vision", 7, 0],
    [34, "遥测/统计", "com.xiaomi.location.fused", 1, 0],
    [34, "遥测/统计", "com.xiaomi.aiasst.service", 6, 0],
    [32, "遥测/统计", "com.google.android.gms", 4, 0],
    [31, "系统", "com.android.dynsystem", 0, 0],
    [31, "系统", "com.miui.powerkeeper", 0, 0]
  ];

  function line(html, className) {
    var div = document.createElement("div");
    div.className = "l" + (className ? " " + className : "");
    div.innerHTML = html;
    demoBody.appendChild(div);
    return div;
  }

  function spacer() {
    var div = document.createElement("div");
    div.className = "spacer";
    demoBody.appendChild(div);
  }

  function progressBar() {
    var div = document.createElement("div");
    div.className = "l";
    div.innerHTML = '<span class="dim">scan</span>     ' +
      '<span class="track"><i></i></span> <span class="dim">0/501</span>';
    demoBody.appendChild(div);
    return {
      fill: div.querySelector("i"),
      label: div.querySelector(".dim:last-child")
    };
  }

  function exposureRow(label, value, max) {
    var width = Math.max(2, Math.round(value * 200 / max));
    return '<span class="lbl dim">' + label + "</span>" +
      '<span class="mini" style="width:' + width + 'px"></span> ' +
      '<span class="dim">' + String(value).padStart(3, " ") + "</span>";
  }

  var running = false;
  var generation = 0;

  async function runDemo() {
    if (running) return;
    running = true;
    var mine = ++generation;
    demoRun.disabled = true;
    demoBody.innerHTML = "";

    line('<span class="ok">$</span> noir ui');
    await sleep(220);

    line('<span class="dim">device</span>   <span class="v">2509FPN0BC</span> · android 16');
    await sleep(160);

    var bar = progressBar();
    var frames = reduced ? 1 : 55;
    for (var i = 0; i <= frames; i++) {
      if (mine !== generation) return;
      var progress = i / frames;
      bar.fill.style.width = (progress * 100).toFixed(1) + "%";
      bar.label.textContent = String(Math.round(progress * 501)).padStart(3, " ") + "/501";
      await sleep(36);
    }
    await sleep(180);
    if (mine !== generation) return;

    line('<span class="dim">flagged</span>  <span class="v">256</span> apps · 31 tracker domains');
    spacer();
    await sleep(180);

    line("EXPOSURE", "head");
    var max = 119;
    for (var c = 0; c < CATEGORIES.length; c++) {
      if (mine !== generation) return;
      line(exposureRow(CATEGORIES[c][0], CATEGORIES[c][1], max));
      await sleep(45);
    }
    spacer();
    await sleep(160);
    if (mine !== generation) return;

    line("TOP FINDINGS", "head");
    var header = '<span class="dim">  #  SCORE  CATEGORY    PACKAGE</span>';
    line(header);
    for (var f = 0; f < FINDINGS.length; f++) {
      if (mine !== generation) return;
      var row = FINDINGS[f];
      line(
        '<span class="dim">' + String(f + 1).padStart(3, " ") + "</span>  " +
        '<span class="warn">' + String(row[0]).padStart(5, " ") + "</span>  " +
        '<span class="dim">' + row[1].padEnd(10, " ") + "</span>  " +
        '<span class="pkg">' + row[2] + "</span>"
      );
      await sleep(38);
    }
    spacer();
    await sleep(160);
    if (mine !== generation) return;

    line("PLAN", "head");
    line('<span class="dim">risk</span>      <span class="bad">● HIGH 20</span>   ' +
      '<span class="warn">● MED 51</span>   <span class="ok">● LOW 31</span>');
    line('<span class="dim">actions</span>   DNS 拦截 31   禁止录屏 12   人工复核 52   <span class="dim">+1</span>');
    line('<span class="dim">auto-safe</span> <span class="ok">19</span> reversible app-ops · rollback.txt');
    spacer();
    await sleep(160);
    line('<span class="dim">done · 4.2s · simulated output, no device involved</span>');

    running = false;
    demoRun.disabled = false;
  }

  function resetDemo() {
    generation++;
    running = false;
    demoRun.disabled = false;
    demoBody.innerHTML = '<div class="l dim">press “run scan” to replay · this is a simulation, not a real device</div>';
  }

  demoRun.addEventListener("click", runDemo);
  if (demoReset) demoReset.addEventListener("click", resetDemo);
  resetDemo();

  /* auto-run once when the demo scrolls into view */
  var autoStarted = false;
  if ("IntersectionObserver" in window) {
    var demoObserver = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting && !autoStarted) {
          autoStarted = true;
          demoObserver.disconnect();
          setTimeout(runDemo, reduced ? 0 : 350);
        }
      });
    }, { threshold: 0.35 });
    demoObserver.observe(demoBody);
  }
})();
