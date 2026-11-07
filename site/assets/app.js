/* NOIR site — progressive enhancement only. No tracking, no cookies. */
(function () {
  "use strict";

  document.documentElement.classList.add("js");

  var zh = /^zh/i.test(document.documentElement.lang || "");
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
    if (platform) download.textContent = zh ? "下载 " + platform + " 版" : "Download for " + platform;
  }

  fetch("https://api.github.com/repos/Verlintas/NOIR/releases/latest", {
    headers: { Accept: "application/vnd.github+json" }
  })
    .then(function (response) { return response.ok ? response.json() : null; })
    .then(function (release) {
      if (!release) return;

      if (download) {
        download.href = release.html_url;
        download.textContent = zh
          ? "下载 " + release.tag_name + (platform ? " · " + platform : "")
          : "Download " + release.tag_name + (platform ? " for " + platform : "");
      }
      if (releaseMeta) {
        var published = release.published_at ? release.published_at.slice(0, 10) : "";
        releaseMeta.textContent = release.tag_name + (published ? " · " + published : "") +
          (zh ? " · macOS / Windows / Linux · MIT" : " · macOS / Windows / Linux · MIT");
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
  var demoReset = document.getElementById("demo-reset");
  var demoSize = document.getElementById("demo-size");
  if (!demoBody) return;

  var CATEGORY = {
    system: ["system", "系统"],
    telemetry: ["telemetry", "遥测/统计"],
    ads: ["ads", "广告"],
    tools: ["tools", "工具"],
    security: ["security", "安全/杀毒"],
    social: ["social", "社交"],
    games: ["games", "游戏"],
    shopping: ["shopping", "购物"],
    finance: ["finance", "金融"]
  };

  function categoryLabel(key) {
    var pair = CATEGORY[key] || [key, key];
    return zh ? pair[1] : pair[0];
  }

  var TEXT = {
    firstRun: zh ? "首次运行向导 · Ctrl+C 随时退出" : "first run · Ctrl+C to quit",
    scanAsk: zh ? "开始扫描并生成报告?" : "Start scan and write report?",
    applyNote: zh ? "可以安全执行 16 条（appops，可回滚）" : "16 app-ops can be applied safely (reversible)",
    applyAsk: zh ? "现在执行?" : "Apply now?",
    skipped: zh ? "已跳过。之后可以运行：noir auto --apply" : "skipped. run later: noir auto --apply",
    cancelled: zh ? "已取消。" : "cancelled."
  };

  var LOGO = [
    "███╗   ██╗ ██████╗ ██╗██████╗ ",
    "████╗  ██║██╔═══██╗██║██╔══██╗",
    "██╔██╗ ██║██║   ██║██║██████╔╝",
    "██║╚██╗██║██║   ██║██║██╔══██╗",
    "██║ ╚████║╚██████╔╝██║██║  ██║",
    "╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═╝"
  ];
  var BANNER = LOGO.map(function (line) {
    return "  " + line.replace(/\s+$/, "");
  }).join("\n");

  var EXPOSURE = [
    ["system", 119, 46],
    ["telemetry", 32, 12],
    ["ads", 21, 8],
    ["tools", 11, 4],
    ["security", 8, 3],
    ["social", 8, 3],
    ["games", 7, 2],
    ["shopping", 6, 2],
    ["finance", 5, 1]
  ];
  var UNCATEGORIZED = 39; // 217 classified + 39 unclassified = 256 flagged

  var FINDINGS = [
    [36, "security", "com.miui.securitycenter", 12, 3],
    [34, "security", "com.bsp.catchlog", 1, 0],
    [34, "telemetry", "com.xiaomi.aiasst.vision", 7, 0],
    [34, "telemetry", "com.xiaomi.location.fused", 1, 0],
    [34, "telemetry", "com.xiaomi.aiasst.service", 6, 0],
    [32, "telemetry", "com.google.android.gms", 4, 0],
    [31, "system", "com.android.dynsystem", 0, 0],
    [31, "system", "com.miui.powerkeeper", 0, 0],
    [31, "system", "com.miui.qr", 0, 0],
    [31, "system", "com.miui.contentcatcher", 0, 0]
  ];

  var APPLY_COMMANDS = [
    "cmd appops set com.miui.powerkeeper GET_USAGE_STATS ignore",
    "cmd appops set com.bsp.catchlog GET_USAGE_STATS ignore",
    "cmd appops set com.xiaomi.location.fused GET_USAGE_STATS ignore",
    "cmd appops set com.google.android.gms GET_USAGE_STATS ignore",
    "cmd appops set com.android.dynsystem GET_USAGE_STATS ignore",
    "cmd appops set com.miui.qr GET_USAGE_STATS ignore",
    "cmd appops set com.miui.contentcatcher GET_USAGE_STATS ignore",
    "cmd appops set com.miui.analytics GET_USAGE_STATS ignore",
    "cmd appops set com.miui.systemAdSolution GET_USAGE_STATS ignore",
    "cmd appops set com.xiaomi.mipicks GET_USAGE_STATS ignore",
    "cmd appops set com.miui.msa.global GET_USAGE_STATS ignore",
    "cmd appops set com.miui.video GET_USAGE_STATS ignore",
    "cmd appops set com.miui.player GET_USAGE_STATS ignore",
    "cmd appops set com.xiaomi.market GET_USAGE_STATS ignore",
    "cmd appops set com.miui.hybrid GET_USAGE_STATS ignore",
    "cmd appops set com.miui.securityadd GET_USAGE_STATS ignore"
  ];

  var SPINNER = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏";
  var scanSteps = 256;

  var running = false;
  var generation = 0;
  var activePrompt = null;

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
      return findingRow(index + 1, row[0], categoryLabel(row[1]), row[2], row[3], row[4]);
    }).join("");

    var exposure = EXPOSURE.map(function (row) {
      return exposureRow(categoryLabel(row[0]), row[1], row[2]);
    }).join("");

    var uncategorised = '<div class="tui-exp uncat"><span class="cat">' +
      (zh ? "未分类" : "unclassified") + "</span>" +
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
      '<span>' + (zh ? "人工复核" : "manual review") + ' 52</span>' +
      '<span>' + (zh ? "DNS 拦截" : "DNS block") + ' 31</span>' +
      '<span>' + (zh ? "阻止使用统计" : "block usage") + ' 12</span>' +
      '<span class="more">+1 more</span></span></div>' +
      '<div class="tui-planrow"><span class="k">auto-safe</span><span>' +
      '<span class="safe-n">16</span> <span class="dim">reversible appops, rollback written next to the report</span>' +
      "</span></div></div>" +
      "</div>";
  }

  function reportHTML() {
    return '<div class="tui-after"><div><span class="report-k">report</span>&nbsp;&nbsp;' +
      "/Users/verlintas/NOIR/noir-out</div></div>";
  }

  function scrollBottom() {
    demoBody.scrollTop = demoBody.scrollHeight;
  }

  function keepTerminalBottom() {
    var rect = demoBody.getBoundingClientRect();
    var overflow = rect.bottom - (window.innerHeight - 16);
    if (overflow > 0) {
      window.scrollBy({
        top: overflow,
        behavior: reduced ? "auto" : "smooth"
      });
    }
  }

  function finishFlow(message) {
    demoBody.scrollLeft = 0;
    if (message) {
      var line = document.createElement("div");
      line.className = "tui-rollback";
      line.textContent = message;
      demoBody.appendChild(line);
    }
    var prompt = document.createElement("div");
    prompt.className = "tui-prompt";
    prompt.innerHTML = '<span class="ok">$</span> <span class="tui-caret"></span>';
    demoBody.appendChild(prompt);
    scrollBottom();
  }

  function ask(question, yes, no, callback) {
    var wrap = document.createElement("div");
    wrap.className = "tui-ask";
    wrap.innerHTML = '<span class="ask-q">' + question + " [</span>" +
      '<button type="button" class="tui-choice" data-choice="y">' + yes + "</button>" +
      '<span class="dim">/</span>' +
      '<button type="button" class="tui-choice" data-choice="n">' + no + "</button>" +
      '<span>]</span><span class="tui-caret"></span>';
    demoBody.appendChild(wrap);
    demoBody.scrollLeft = 0;
    scrollBottom();

    function resolve(choice) {
      if (prompt.done) return;
      prompt.done = true;
      if (activePrompt === prompt) activePrompt = null;
      wrap.innerHTML = '<span class="ask-q">' + question + " [" + yes + "/" + no + "] </span>" +
        '<span class="ok">' + (choice === "y" ? yes : no) + "</span>";
      try { demoBody.focus({ preventScroll: true }); } catch (e) { demoBody.focus(); }
      callback(choice === "y" ? "y" : "n");
    }

    var prompt = {
      done: false,
      resolve: resolve
    };
    activePrompt = prompt;

    wrap.addEventListener("click", function (event) {
      var target = event.target;
      while (target && target !== wrap) {
        if (target.tagName === "BUTTON" && target.getAttribute("data-choice")) {
          prompt.resolve(target.getAttribute("data-choice"));
          return;
        }
        target = target.parentNode;
      }
    });

    return prompt;
  }

  demoBody.addEventListener("click", function (event) {
    if (!event.target.closest || !event.target.closest("button")) {
      demoBody.focus({ preventScroll: true });
    }
  });

  demoBody.addEventListener("keydown", function (event) {
    if (!activePrompt) return;
    if (event.metaKey || event.ctrlKey || event.altKey) return;
    var key = (event.key || "").toLowerCase();
    if (key !== "y" && key !== "n") return;
    event.preventDefault();
    activePrompt.resolve(key);
  });

  function renderWizard() {
    generation++;
    running = false;
    activePrompt = null;
    demoBody.classList.remove("expanded");
    if (demoSize) demoSize.textContent = "100×18 · " + (zh ? "模拟" : "simulated");
    demoBody.scrollTop = 0;
    demoBody.scrollLeft = 0;
    demoBody.innerHTML = '<div class="tui-wizard">' +
      '<div class="tui-command"><span class="ok">$</span> noir</div>' +
      '<pre class="tui-banner">' + BANNER + "</pre>" +
      '<div class="tui-first">' + TEXT.firstRun + "</div>" +
      '<div class="tui-wizard-gap"></div>' +
      '<div class="tui-kv"><span class="k">device</span>' +
      '<span class="v">2509FPN0BC  (178b0d2f)</span></div>' +
      "</div>";

    ask(TEXT.scanAsk, "Y", "n", function (choice) {
      if (choice === "y") {
        var token = generation;
        setTimeout(function () {
          if (token === generation) runScan();
        }, reduced ? 0 : 220);
      } else {
        finishFlow(TEXT.cancelled);
      }
    });
  }

  async function runScan() {
    if (running) return;
    running = true;
    var mine = ++generation;
    activePrompt = null;
    demoBody.scrollLeft = 0;
    demoBody.scrollTop = 0;
    demoBody.innerHTML = "";

    var log = document.createElement("div");
    log.className = "tui-run";
    demoBody.appendChild(log);

    function runLine(html) {
      var div = document.createElement("div");
      div.innerHTML = html;
      log.appendChild(div);
      return div;
    }

    runLine('<span class="prompt">$</span> noir');
    await sleep(reduced ? 0 : 240);
    if (mine !== generation) return;

    runLine('[*] host: Mac OS X aarch64');
    runLine('[*] device: 178b0d2f (2509FPN0BC)');
    await sleep(reduced ? 0 : 100);
    runLine('[*] packages: 501');
    await sleep(reduced ? 0 : 100);
    runLine('[*] reading dumpsys package ... 312 KiB');
    await sleep(reduced ? 0 : 130);
    runLine('[*] parsed: 498');
    runLine('[*] candidates (internet + sensitive): 256');
    await sleep(reduced ? 0 : 120);
    runLine('[*] analysing 256 new/changed APKs (cache hits: 0)');
    await sleep(reduced ? 0 : 160);
    if (mine !== generation) return;

    var progress = runLine("");
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

    demoBody.innerHTML = dashboardHTML() + reportHTML();
    demoBody.classList.add("expanded");
    if (demoSize) demoSize.textContent = "100×48 · " + (zh ? "模拟" : "simulated");
    demoBody.scrollLeft = 0;
    demoBody.scrollTop = 0;
    await sleep(reduced ? 0 : 900);
    if (mine !== generation) return;

    var note = document.createElement("div");
    note.className = "tui-apply-note";
    note.textContent = TEXT.applyNote;
    demoBody.appendChild(note);

    ask(TEXT.applyAsk, "y", "N", function (choice) {
      if (choice === "y") applyFlow(mine);
      else finishFlow(TEXT.skipped);
    });
    setTimeout(keepTerminalBottom, reduced ? 0 : 80);
    running = false;
  }

  async function applyFlow(token) {
    if (token !== generation) return;
    demoBody.scrollLeft = 0;
    var log = document.createElement("div");
    log.className = "tui-apply-log";
    demoBody.appendChild(log);

    for (var i = 0; i < APPLY_COMMANDS.length; i++) {
      if (token !== generation) return;
      var row = document.createElement("div");
      row.className = "tui-apply";
      row.innerHTML = '<span class="cmd">' + APPLY_COMMANDS[i] + '</span><span class="status">ok</span>';
      log.appendChild(row);
      scrollBottom();
      await sleep(reduced ? 0 : 55);
    }
    if (token !== generation) return;

    var rollback = document.createElement("div");
    rollback.className = "tui-rollback";
    rollback.innerHTML = '[*] rollback: <span class="path">' +
      "/Users/verlintas/NOIR/noir-out/rollback.txt</span>";
    demoBody.appendChild(rollback);
    finishFlow();
    keepTerminalBottom();
  }

  function resetDemo() {
    renderWizard();
  }

  if (demoReset) demoReset.addEventListener("click", resetDemo);
  renderWizard();
})();
