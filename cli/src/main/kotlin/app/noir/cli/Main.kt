package app.noir.cli

import app.noir.core.ActionType
import app.noir.core.Advisor
import app.noir.core.AppEntry
import app.noir.core.Cache
import app.noir.core.Category
import app.noir.core.Classifier
import app.noir.core.Device
import app.noir.core.Recommendation
import app.noir.core.Report
import app.noir.core.Rules
import app.noir.core.Tui
import app.noir.core.adb.Adb
import app.noir.core.adb.DeviceScan
import app.noir.core.adb.Dumpsys
import app.noir.core.adb.PackagePath
import app.noir.core.apk.ApkScan
import app.noir.core.jadx.Jadx
import app.noir.core.platform.Platform
import app.noir.core.platform.Setup
import java.io.File
import kotlin.system.exitProcess

private const val VERSION = "0.1.0"

private val BOOLEAN_FLAGS = setOf(
    "apply", "yes", "verbose", "help", "ascii", "color", "no-color", "quiet", "no-clear"
)

private var QUIET = false

private fun say(message: String) {
    if (!QUIET) println(message)
}

private class NoirException(message: String, val hint: String? = null) : Exception(message)

fun main(args: Array<String>) {
    QUIET = args.contains("--quiet")
    try {
        runMain(args)
    } catch (e: NoirException) {
        System.err.println()
        System.err.println(Guidance.error(e.message ?: "出错了", e.hint))
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println()
        System.err.println(
            Guidance.error(
                e.message ?: e::class.simpleName ?: "未知错误",
                "加 --verbose 可以看完整堆栈；也可以先运行 noir doctor 检查环境。"
            )
        )
        if (args.contains("--verbose")) e.printStackTrace()
        exitProcess(1)
    }
}

private fun runMain(args: Array<String>) {
    if (args.isEmpty()) {
        if (isInteractive()) guided() else usage()
        return
    }
    when (val command = args[0]) {
        "devices" -> cmdDevices(args.drop(1))
        "scan" -> cmdScan(args.drop(1))
        "auto" -> cmdAuto(args.drop(1))
        "ui" -> cmdUi(args.drop(1))
        "deep" -> cmdDeep(args.drop(1))
        "block" -> cmdBlock(args.drop(1))
        "setup" -> cmdSetup(args.drop(1))
        "doctor" -> cmdDoctor(args.drop(1))
        "-h", "--help", "help" -> usage()
        "-v", "--version", "version" -> println("NOIR $VERSION")
        "--about", "about" -> println(Guidance.about(VERSION))
        else -> {
            System.err.println("不认识的命令：$command")
            usage()
            exitProcess(2)
        }
    }
}

private fun usage() {
    println(
        """
        NOIR — audit what your phone can leak, from the desktop.
        runs on Windows 10/11, macOS and Debian/Ubuntu.

        第一次用？直接运行 `noir`（不带参数），会进入向导。
        想先了解它是什么：noir --about

        usage:
          noir                                guided setup + scan (interactive)
          noir --about                        what this is, what it writes, license
          noir --version                      print the version
          noir doctor                         check adb / jadx / device
          noir setup                          download missing tools into ~/.noir/tools
          noir devices                        list connected devices
          noir scan [options]                 inventory apps, permissions and endpoints
          noir auto [options]                 scan + classify + plan
          noir ui [options]                   same, rendered as a dashboard
          noir deep <package> [options]       pull one APK, run jadx, grep for red flags
          noir block <package...> [options]   print (or apply) neutralisation commands

        options:
          --serial <id>      target a specific device
          --out <dir>        output directory (default: noir-out)
          --min <score>      only keep apps at or above this score
          --limit <n>        cap reported apps (default 500)
          --only <regex>     restrict scanning to matching package names
          --adb <path>       override adb binary
          --jadx <path>      override jadx binary

        auto only:
          --apply            execute the safe, reversible app-ops actions
          --yes              skip confirmation prompts (automation)

        display:
          --ascii            plain ASCII borders (legacy Windows console)
          --color            force ANSI colour (auto-detected on a TTY)
          --no-color         disable ANSI colour
          --quiet            suppress scan progress
          --no-clear         do not clear the screen before the dashboard
        """.trimIndent()
    )
}

// ── guided first run ───────────────────────────────────────────────────────

private fun bannerSmall() {
    println(
        """
        |  ███╗   ██╗ ██████╗ ██╗██████╗
        |  ████╗  ██║██╔═══██╗██║██╔══██╗
        |  ██╔██╗ ██║██║   ██║██║██████╔╝
        |  ██║╚██╗██║██║   ██║██║██╔══██╗
        |  ██║ ╚████║╚██████╔╝██║██║  ██║
        |  ╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═╝
        """.trimMargin()
    )
}

private fun guided() {
    bannerSmall()
    println("  首次运行向导 · Ctrl+C 随时退出")
    println()

    val adb = ensureAdbInteractive()
    val device = awaitDevice(adb, null, interactive = true)

    println()
    println("  设备：${device.model.ifBlank { device.serial }}  (${device.serial})")
    print("  开始扫描并生成报告? [Y/n] ")
    val proceed = readLine()?.trim()?.lowercase()
    if (proceed == "n" || proceed == "no") {
        println("  已取消。")
        return
    }

    val args = Args(emptyList())
    val result = performScan(args)
    val recommendations = Advisor.advise(result.apps)
    writeReports(result, recommendations)

    println()
    print(renderDashboard(args, result, recommendations))

    val planned = plannedCommands(recommendations)
    println()
    println("  可以安全执行 ${planned.size} 条（appops，可回滚）")
    if (planned.isEmpty()) {
        println("  没有需要自动处理的项。")
        return
    }
    print("  现在执行? [y/N] ")
    val apply = readLine()?.trim()?.lowercase()
    if (apply == "y" || apply == "yes") {
        applyCommands(adb, result, planned)
    } else {
        println("  已跳过。之后可以运行：noir auto --apply")
    }
}

private fun ensureAdbInteractive(): Adb {
    Platform.findAdb()?.let { return Adb(it) }

    println("  没有找到 adb。")
    println(Guidance.adbMissing())
    print("  要现在下载 Android platform-tools（约 15 MB）吗? [Y/n] ")
    val answer = readLine()?.trim()?.lowercase()
    if (answer == "n" || answer == "no") {
        throw NoirException("需要 adb 才能继续。", Guidance.adbMissing())
    }

    print("  下载中 ")
    val path = Setup.installAdb { read, total -> renderDownload(read, total) }
    println()
    if (path == null) throw NoirException("adb 下载失败。", "检查网络后重试，或手动安装 adb。")
    println("  已安装：$path")
    return Adb(path)
}

private fun renderDownload(read: Long, total: Long) {
    if (QUIET) return
    if (total > 0) {
        print("\r  ${read * 100 / total}%  ${read / 1_048_576}/${total / 1_048_576} MB   ")
    } else {
        print("\r  ${read / 1_048_576} MB   ")
    }
    System.out.flush()
}

// ── commands ───────────────────────────────────────────────────────────────

private fun cmdDevices(raw: List<String>) {
    val args = Args(raw)
    val adb = connectAdb(args)
    val filter = args.value("serial")
    val devices = adb.devices().let { all -> if (filter == null) all else all.filter { it.serial == filter } }
    if (devices.isEmpty()) {
        println(Guidance.deviceGuide())
        return
    }
    println("%-22s %-14s %-22s %s".format("SERIAL", "STATE", "MODEL", "PRODUCT"))
    devices.forEach {
        println("%-22s %-14s %-22s %s".format(it.serial, it.state, it.model, it.product))
    }
    if (devices.any { it.state == "unauthorized" }) {
        println()
        println(Guidance.unauthorizedGuide())
    }
}

private data class ScanResult(
    val device: Device,
    val apps: List<AppEntry>,
    val outDir: File,
    val android: String,
    val installed: Int
)

private fun cmdScan(raw: List<String>) {
    val args = Args(raw)
    val result = performScan(args)
    writeReports(result, emptyList())

    println()
    println("%5s  %-12s %-10s %-44s %s".format("SCORE", "CATEGORY", "VENDOR", "PACKAGE", "PERM/UPLOAD/CMD"))
    result.apps.take(40).forEach { a ->
        println(
            "%5d  %-12s %-10s %-44s %d/%d/%d".format(
                a.score, a.category.label, a.vendor ?: "—", a.pkg,
                a.permissions.size, a.uploadEndpoints.size, a.commandEndpoints.size
            )
        )
    }
    println()
    println(Tui.clip("[+] ${result.outDir.absolutePath}"))
}

private fun cmdAuto(raw: List<String>) {
    val args = Args(raw)
    val result = performScan(args)
    val recommendations = Advisor.advise(result.apps)
    writeReports(result, recommendations)

    println()
    print(renderDashboard(args, result, recommendations))
    println()
    println("== 建议动作 (前 15) ==")
    recommendations.take(15).forEach { r ->
        println("  %-5s %-16s %-46s %s".format(r.risk, r.action.label, r.pkg, r.reason))
    }

    val planned = plannedCommands(recommendations)
    println()
    println("== 将执行（${planned.size} 条，全部可回滚）==")
    planned.forEach { println("  $it") }
    println()
    println(Tui.clip("[+] ${result.outDir.absolutePath}"))

    if (!args.flag("apply")) {
        println()
        println("[*] dry-run — 加 --apply 执行这 ${planned.size} 条")
        return
    }

    if (!args.flag("yes")) {
        print("执行 ${planned.size} 条 appops 动作? [y/N] ")
        val answer = readLine()?.trim()?.lowercase()
        if (answer != "y" && answer != "yes") {
            println("aborted")
            return
        }
    }
    applyCommands(connectAdb(args), result, planned)
}

private fun cmdUi(raw: List<String>) {
    val args = Args(raw)
    val result = performScan(args)
    val recommendations = Advisor.advise(result.apps)
    writeReports(result, recommendations)

    if (!args.flag("no-clear") && System.console() != null) {
        print("\u001b[2J\u001b[H")
    }
    println()
    print(renderDashboard(args, result, recommendations))
    println()
    println(Tui.clip("  report  ${result.outDir.absolutePath}"))
    println()
}

private fun cmdDeep(raw: List<String>) {
    val args = Args(raw)
    val pkg = args.positional.firstOrNull()
        ?: throw NoirException("缺少包名。", "用法：noir deep <package> [--serial S] [--out DIR]")

    val adb = connectAdb(args)
    val device = awaitDevice(adb, args.value("serial"), interactive = isInteractive())
    val outDir = File(args.value("out") ?: "noir-out/deep-${pkg.replace('.', '_')}").apply { mkdirs() }

    val remote = adb.shell(device.serial, "pm path $pkg").output
        .lineSequence()
        .firstOrNull { it.startsWith("package:") }
        ?.removePrefix("package:")
        ?.trim()
        ?: throw NoirException("这台设备上没有安装 $pkg。", "先运行 noir scan 看看实际包名。")

    val apk = File(outDir, "$pkg.apk")
    println("[*] pulling $remote")
    adb.pull(device.serial, remote, apk)
    println("[*] apk: ${apk.length() / 1024} KiB")

    val urls = ApkScan.urls(apk)
    println("[*] endpoints: ${urls.size}")
    urls.filter { Rules.isUpload(it) || Rules.isCommand(it) }.forEach { println("    ! $it") }

    val jadxBin = Platform.findJadx(args.value("jadx"))
        ?: throw NoirException("没有找到 jadx。", Guidance.jadxMissing())

    println("[*] decompiling with $jadxBin ...")
    val src = File(outDir, "src")
    val code = Jadx(jadxBin).decompile(apk, src)
    println("[*] jadx exit=$code")

    val hits = Jadx(jadxBin).grep(
        src,
        listOf(
            "Runtime\\.exec", "ProcessBuilder", "setApplicationEnabledSetting",
            "enabled_accessibility_services", "DexClassLoader", "loadLibrary",
            "https?://", "verify\\(", "Cipher", "getInstalledPackages",
            "getRunningTasks", "MediaProjection"
        )
    )
    println("[*] hits: ${hits.size}")
    hits.take(40).forEach { println("    ${it.file}:${it.line}  ${it.text}") }
    println("[+] sources: ${src.absolutePath}")
}

private fun cmdBlock(raw: List<String>) {
    val args = Args(raw)
    val pkgs = args.positional
    if (pkgs.isEmpty()) throw NoirException("缺少包名。", "用法：noir block <package...> [--apply]")

    val adb = connectAdb(args)
    val device = awaitDevice(adb, args.value("serial"), interactive = isInteractive())

    val actions = ArrayList<String>()
    pkgs.forEach { pkg ->
        actions += "pm uninstall --user 0 $pkg"
        actions += "cmd appops set $pkg PROJECT_MEDIA ignore"
        actions += "cmd appops set $pkg GET_USAGE_STATS ignore"
    }

    println("# rollback")
    pkgs.forEach { println("#   adb -s ${device.serial} shell pm install-existing --user 0 $it") }
    println()
    actions.forEach { println("adb -s ${device.serial} shell $it") }

    if (!args.flag("apply")) {
        println()
        println("# dry-run — 加 --apply 执行")
        return
    }

    println()
    actions.forEach { action ->
        print("> $action ... ")
        val result = adb.shell(device.serial, action)
        println(if (result.ok) "ok" else "failed (${result.code}) ${result.output.trim().take(120)}")
    }
}

private fun cmdSetup(raw: List<String>) {
    val args = Args(raw)
    val autoYes = args.flag("yes")

    println("NOIR setup")
    println("  platform  ${Platform.describe()}")

    val adb = Platform.findAdb(args.value("adb"))
    if (adb != null) {
        println("  adb       $adb")
    } else {
        println("  adb       未找到")
        if (!autoYes && !confirm("  下载 Android platform-tools（约 15 MB）? [Y/n] ")) {
            println(Guidance.adbMissing())
        } else {
            print("  下载中 ")
            val path = Setup.installAdb { read, total -> renderDownload(read, total) }
            println()
            if (path == null) throw NoirException("adb 下载失败。", "检查网络后重试。")
            println("  adb       $path")
        }
    }

    val jadx = Platform.findJadx(args.value("jadx"))
    if (jadx != null) {
        println("  jadx      $jadx")
    } else {
        println("  jadx      未找到（只有 deep 命令需要）")
        if (!autoYes && !confirm("  下载 jadx（约 73 MB）? [Y/n] ")) {
            println(Guidance.jadxMissing())
        } else {
            print("  下载中 ")
            val path = Setup.installJadx { read, total -> renderDownload(read, total) }
            println()
            if (path == null) throw NoirException("jadx 下载失败。", "检查网络后重试。")
            println("  jadx      $path")
        }
    }

    println()
    println("完成。下一步：noir doctor")
}

private fun cmdDoctor(raw: List<String>) {
    val args = Args(raw)
    println("NOIR doctor")
    println("  platform  ${Platform.describe()}")
    println("  java      ${System.getProperty("java.version")}")

    val adbPath = Platform.findAdb(args.value("adb"))
    if (adbPath == null) {
        println("  adb       未找到")
        println(Guidance.adbMissing())
        return
    }
    val adb = Adb(adbPath)
    println("  adb       $adbPath")
    adb.exec(listOf("version"), timeoutSec = 15).output.lineSequence()
        .firstOrNull { it.startsWith("Version") }
        ?.let { println("            $it") }

    val devices = adb.devices()
    if (devices.isEmpty()) {
        println("  device    未连接")
        println()
        println(Guidance.deviceGuide())
    } else {
        devices.forEach { println("  device    ${it.serial}  ${it.state}  ${it.model}") }
        if (devices.any { it.state == "unauthorized" }) {
            println()
            println(Guidance.unauthorizedGuide())
        }
    }

    val jadx = Platform.findJadx(args.value("jadx"))
    if (jadx == null) println("  jadx      未找到（只有 deep 需要）") else println("  jadx      $jadx")
}

private fun confirm(prompt: String): Boolean {
    print(prompt)
    val answer = readLine()?.trim()?.lowercase()
    return answer != "n" && answer != "no"
}

// ── scan pipeline ──────────────────────────────────────────────────────────

private fun performScan(args: Args): ScanResult {
    val adb = connectAdb(args)
    val device = awaitDevice(adb, args.value("serial"), interactive = isInteractive())
    val outDir = File(args.value("out") ?: "noir-out").apply { mkdirs() }
    val minScore = args.value("min")?.toIntOrNull() ?: 0
    val limit = args.value("limit")?.toIntOrNull() ?: 500

    say("[*] host: ${Platform.describe()}")
    say("[*] device: ${device.serial} (${device.model})")

    val paths = DeviceScan.packagePaths(adb, device.serial)
    say("[*] packages: ${paths.size}")

    if (!QUIET) print("[*] reading dumpsys package ... ")
    val dump = adb.shell(device.serial, "dumpsys package", timeoutSec = 600)
    say("${dump.output.length / 1024} KiB")

    val meta = Dumpsys.parse(dump.output)
    say("[*] parsed: ${meta.size}")

    val only = args.value("only")?.let { runCatching { Regex(it) }.getOrNull() }
    val candidates = meta.entries.filter { (pkg, m) ->
        m.perms.contains("INTERNET") &&
            m.perms.count { it in Rules.SENSITIVE } > 0 &&
            paths.containsKey(pkg) &&
            (only == null || only.containsMatchIn(pkg))
    }
    say("[*] candidates (internet + sensitive): ${candidates.size}")

    val android = adb.shell(device.serial, "getprop ro.build.version.release").output.trim()

    val targets = candidates.map { PackagePath(it.key, paths.getValue(it.key)) }
    val endpoints = HashMap<String, List<String>>()
    val missing = ArrayList<PackagePath>()

    targets.forEach { target ->
        val hit = Cache.get(target.pkg, target.path)
        if (hit != null) endpoints[target.pkg] = hit else missing += target
    }

    if (missing.isNotEmpty()) {
        say("[*] analysing ${missing.size} new/changed APKs (cache hits: ${targets.size - missing.size})")
        val started = System.currentTimeMillis()
        val fresh = DeviceScan.urls(adb, device.serial, missing, File(outDir, "work")) { done, total ->
            if (!QUIET) {
                print(progressLine(started, done, total))
                System.out.flush()
            }
        }
        if (!QUIET) println()
        missing.forEach { target -> Cache.put(target.pkg, target.path, fresh[target.pkg].orEmpty()) }
        Cache.flush()
        endpoints.putAll(fresh)
    } else {
        say("[*] all endpoints served from cache")
    }

    val apps = candidates.map { (pkg, m) ->
        val perms = m.perms.filter { it in Rules.SENSITIVE }
        val urls = endpoints[pkg].orEmpty()
        AppEntry(
            pkg = pkg,
            uid = m.appId,
            isSystem = m.isSystem,
            debuggable = m.debuggable,
            installer = m.installer,
            permissions = perms,
            endpoints = urls,
            score = Rules.score(perms, urls, m.isSystem, m.debuggable),
            category = Classifier.category(pkg, urls, perms, m.isSystem),
            vendor = Classifier.vendor(pkg, urls)
        )
    }
        .filter { it.score >= minScore }
        .sortedByDescending { it.score }
        .take(limit)

    return ScanResult(device, apps, outDir, android, paths.size)
}

private fun writeReports(result: ScanResult, recommendations: List<Recommendation>) {
    File(result.outDir, "report.json").writeText(Report.json(result.device, result.apps, recommendations))
    File(result.outDir, "report.md").writeText(Report.markdown(result.device, result.apps, recommendations))
    File(result.outDir, "domains.txt").writeText(Report.domains(result.apps))
    File(result.outDir, "plan.txt").writeText(plannedCommands(recommendations).joinToString("\n", postfix = "\n"))
    File(result.outDir, "rollback.txt").writeText(rollbackLines(recommendations).joinToString("\n", postfix = "\n"))
}

private fun plannedCommands(recommendations: List<Recommendation>): List<String> =
    recommendations.filter { it.autoSafe }.mapNotNull { r ->
        when (r.action) {
            ActionType.APPOP_PROJECT_MEDIA -> "cmd appops set ${r.pkg} PROJECT_MEDIA ignore"
            ActionType.APPOP_USAGE_STATS -> "cmd appops set ${r.pkg} GET_USAGE_STATS ignore"
            else -> null
        }
    }.distinct()

private fun rollbackLines(recommendations: List<Recommendation>): List<String> =
    recommendations.filter { it.autoSafe }.mapNotNull { r ->
        when (r.action) {
            ActionType.APPOP_PROJECT_MEDIA -> "adb shell cmd appops set ${r.pkg} PROJECT_MEDIA default"
            ActionType.APPOP_USAGE_STATS -> "adb shell cmd appops set ${r.pkg} GET_USAGE_STATS default"
            else -> null
        }
    }.distinct()

private fun applyCommands(adb: Adb, result: ScanResult, planned: List<String>) {
    println()
    val applied = ArrayList<String>()
    planned.forEach { command ->
        val res = adb.shell(result.device.serial, command)
        val status = if (res.ok) "ok" else "failed (${res.code})"
        applied.add("$command\t$status")
        println("  %-72s %s".format(command, status))
    }
    File(result.outDir, "applied.txt").writeText(applied.joinToString("\n", postfix = "\n"))
    println()
    println("[*] rollback: ${File(result.outDir, "rollback.txt").absolutePath}")
}

private fun renderDashboard(args: Args, result: ScanResult, recommendations: List<Recommendation>): String {
    val color = (args.flag("color") || System.console() != null) &&
        !args.flag("no-color") &&
        !args.flag("ascii")
    return Tui.dashboard(
        device = result.device,
        apps = result.apps,
        recommendations = recommendations,
        android = result.android,
        host = Platform.describe(),
        installed = result.installed,
        ascii = args.flag("ascii"),
        color = color
    )
}

private fun progressLine(startedMs: Long, done: Int, total: Int): String {
    val elapsed = (System.currentTimeMillis() - startedMs).coerceAtLeast(1)
    val perItem = elapsed.toDouble() / done.coerceAtLeast(1)
    val eta = ((total - done) * perItem / 1000).toLong().coerceAtLeast(0)
    val width = 26
    val pct = done * 100 / total
    val filled = (done.toLong() * width / total).toInt().coerceIn(0, width)
    val bar = "█".repeat(filled) + "░".repeat(width - filled)
    val spin = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"[(done / 8) % 10]
    return "\r  " + bar + " " + pct + "%  " + done + "/" + total +
        "  ·  ETA " + (eta / 60) + ":" + (eta % 60).toString().padStart(2, '0') + "  " + spin
}

// ── plumbing ───────────────────────────────────────────────────────────────

private fun isInteractive(): Boolean =
    System.console() != null || System.getenv("NOIR_INTERACTIVE") == "1"

private fun connectAdb(args: Args): Adb {
    val explicit = args.value("adb")
    if (explicit != null && !File(explicit).exists()) {
        throw NoirException("指定的 adb 不存在：$explicit", "检查路径，或运行 noir setup 自动安装。")
    }
    val bin = Platform.findAdb(explicit)
        ?: throw NoirException("没有找到 adb。", Guidance.adbMissing())
    return Adb(bin)
}

private fun awaitDevice(adb: Adb, requested: String?, interactive: Boolean): Device {
    var waited = 0
    var printed = false

    while (true) {
        val devices = adb.devices()
        val online = devices.filter { it.state == "device" }

        online.firstOrNull { requested == null || it.serial == requested }?.let { return it }

        if (requested != null && online.isNotEmpty()) {
            throw NoirException(
                "没有找到设备 $requested。",
                "当前已连接：" + online.joinToString { "${it.serial} (${it.model})" }
            )
        }

        val unauthorized = devices.any { it.state == "unauthorized" }
        if (!printed) {
            println(if (unauthorized) Guidance.unauthorizedGuide() else Guidance.deviceGuide())
            printed = true
        }

        if (!interactive) {
            throw NoirException(
                "没有可用设备。",
                if (unauthorized) Guidance.unauthorizedGuide() else "插上手机后重新运行，或先运行 noir doctor。"
            )
        }
        if (waited >= 180) throw NoirException("等了 180 秒仍然没有设备。", Guidance.deviceGuide())

        print("\r  等待设备接入 ... ${180 - waited}s   ")
        System.out.flush()
        Thread.sleep(2000)
        waited += 2
    }
}

private class Args(raw: List<String>) {
    val positional = ArrayList<String>()
    val flags = HashSet<String>()
    val values = HashMap<String, String>()

    init {
        var i = 0
        while (i < raw.size) {
            val arg = raw[i]
            if (arg.startsWith("--")) {
                val name = arg.substring(2)
                val takesValue = name !in BOOLEAN_FLAGS && i + 1 < raw.size && !raw[i + 1].startsWith("--")
                if (takesValue) {
                    values[name] = raw[i + 1]
                    i++
                } else {
                    flags.add(name)
                }
            } else {
                positional.add(arg)
            }
            i++
        }
    }

    fun flag(name: String): Boolean = name in flags
    fun value(name: String): String? = values[name]
}
