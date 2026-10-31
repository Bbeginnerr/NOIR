```
  ███╗   ██╗ ██████╗ ██╗██████╗
  ████╗  ██║██╔═══██╗██║██╔══██╗
  ██╔██╗ ██║██║   ██║██║██████╔╝
  ██║╚██╗██║██║   ██║██║██╔══██╗
  ██║ ╚████║╚██████╔╝██║██║  ██║
  ╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═╝
```

# NOIR

[![build](https://github.com/USER/NOIR/actions/workflows/build.yml/badge.svg)](https://github.com/USER/NOIR/actions/workflows/build.yml)

**把「这台手机到底能泄露什么」变成一份可复现的报告。**

NOIR 是一个跑在电脑上的审计工具，通过 `adb` 读取已连接 Android 设备的应用清单、权限和 APK 里的网络端点，给出分类、风险评分和可执行的处置建议。它也可以在需要时调用 `jadx` 对单个应用做反编译和关键字检索。

> 定位：**审计与自我防护**。面向自己的设备、自己的应用、已授权的安全研究。

---

## 它做什么

1. **盘点**：列出所有「有网络权限 + 敏感权限」的应用，以及它们各自拿到了什么。
2. **抽取端点**：在设备端用 `unzip + strings` 从每个 APK 的 dex 里提取 URL，只回传几 KB，不把整包拉回电脑。
3. **分类与打分**：按包名和域名识别厂商（Xiaomi / Google / 腾讯 / 高德 …）与类别（遥测、广告、定位、输入法 …），结合权限和上报端点计算风险分。
4. **给建议**：区分「可安全自动执行」「需人工确认」「不建议动」，并为每条动作生成回滚命令。
5. **必要时深挖**：`deep` 会把单个 APK 拉到本地，用 `jadx` 反编译后检索 `Runtime.exec`、`setApplicationEnabledSetting`、`DexClassLoader`、`MediaProjection` 等高风险调用。

## 它不做什么

- 不联网、不上传任何数据、不需要账号。
- 不修改系统分区，不需要 root。
- 不会自动卸载任何东西，除非你显式加 `--aggressive`。

---

## 第一次运行

```bash
noir
```

不带参数会进入向导：

1. 检查 `adb`，没有就**问你要不要自动下载**（`~/.noir/tools`）
2. 检查设备，没有就打印**开发者模式 / USB 调试的开通步骤**，然后**等你插线**（最多 180 秒）
3. 问一句「开始扫描?」，扫完直接出仪表盘
4. 问一句「执行这 19 条安全处置?」，`y` 就执行，并写好回滚文件

全程只会问两三个 y/n，其余都是自动的。

## 环境自检

```bash
noir doctor
```

```
NOIR doctor
  platform  Mac OS X aarch64
  java      17.0.19
  adb       /opt/homebrew/.../platform-tools/adb
            Version 37.0.1-15733141
  device    178b0d2f  device  2509FPN0BC
  jadx      /opt/homebrew/bin/jadx
```

```bash
noir setup          # 缺什么下什么
noir setup --yes    # 不询问，适合脚本
```

## 出现问题时

所有错误都会带「怎么办」，而不是抛异常栈：

```
╭─ ERROR ──────────────────────────────────────────────────────────╮
│ 指定的 adb 不存在：/nonexistent/adb                              │
├──────────────────────────────────────────────────────────────────┤
│ 检查路径，或运行 noir setup 自动安装。                           │
╰──────────────────────────────────────────────────────────────────╯
```

没有设备时给出的是可执行步骤：

```
╭─ 没有检测到设备 ──────────────────────────────────────────────╮
│  1. 打开开发者模式                                           │
│     小米 / HyperOS：设置 → 我的设备 → 全部参数 → 连点「MIUI 版本」7 次
│  2. 开启 USB 调试：设置 → 系统 → 开发者选项 → USB 调试        │
│  3. 插线后手机弹「允许 USB 调试吗？」→ 勾选「一律允许」        │
│  4. 还不行就换一根数据线（很多线只能充电）                    │
╰──────────────────────────────────────────────────────────────╯
```

已连接但没授权时，会单独提示「撤销 USB 调试授权后重插」。

## 支持平台

| 系统 | 状态 |
|---|---|
| macOS (Intel / Apple Silicon) | ✅ |
| Windows 10 / 11 | ✅ |
| Debian / Ubuntu | ✅ |

依赖：

- **JDK 17+**
- **Android platform-tools**（`adb`）
- **jadx**（仅 `deep` 命令需要）

程序会自动在常见安装位置查找 `adb` 和 `jadx`；找不到时可以用 `--adb` / `--jadx` 指定。

```bash
# macOS
brew install --cask android-platform-tools jadx

# Debian / Ubuntu
sudo apt install adb
# jadx: https://github.com/skylot/jadx/releases

# Windows (scoop)
scoop install adb jadx
```

---

## 构建

```bash
git clone <repo> && cd NOIR
./gradlew :cli:installDist
```

产物：

```
cli/build/install/noir/bin/noir        # macOS / Linux
cli/build/install/noir/bin/noir.bat    # Windows
```

也可以直接：

```bash
./gradlew :cli:run --args="devices"
```

---

## 界面

`noir ui` 会把结果渲染成一块等宽仪表盘（中文按 2 列宽对齐，ANSI 渐变自动降级）：

```
╭────────────────────────────────────────────────────────────────────────────╮
│  ███╗   ██╗ ██████╗ ██╗██████╗    android leak audit                       │
│  ████╗  ██║██╔═══██╗██║██╔══██╗   v0.1 · monospace edition                 │
│  ██╔██╗ ██║██║   ██║██║██████╔╝                                            │
│  ██║╚██╗██║██║   ██║██║██╔══██╗   Mac OS X aarch64                         │
│  ██║ ╚████║╚██████╔╝██║██║  ██║                                            │
│  ╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═╝                                            │
├─ DEVICE ───────────────────────────────────────────────────────────────────┤
│  model     Xiaomi 17 Pro Max                                               │
│  device    178b0d2f  ·  popsicle                                           │
│  android   16  ·  502 installed  ·  234 flagged                            │
├─ EXPOSURE ─────────────────────────────────────────────────────────────────┤
│  遥测/统计     ██████████████████████████   12   5%                        │
│  定位/地图     ████░░░░░░░░░░░░░░░░░░░░░░    2   1%                        │
├─ PLAN ─────────────────────────────────────────────────────────────────────┤
│  risk      ● HIGH 2   ● MED 34   ● LOW 0                                   │
╰────────────────────────────────────────────────────────────────────────────╯
```

兼容开关：

- `--ascii`：纯 ASCII 边框，给旧版 Windows 控制台
- `--no-color` / `--color`：关闭或强制 ANSI 颜色（默认按是否为 TTY 自动判断）

## 用法

```bash
noir --about              # 它是什么、会写什么、许可
noir --version
noir devices
noir scan                 # 全量扫描，输出 JSON / Markdown / 域名清单
noir ui                   # 扫描并渲染仪表盘
noir auto                 # 扫描 + 分类 + 生成处置计划（dry-run）
noir auto --apply         # 执行安全的 appops 动作（可回滚）
noir auto --apply --aggressive   # 额外卸载被标记的用户应用
noir deep com.example.app # 拉包 + jadx + 关键字检索
noir block com.example.app        # 打印处置命令
noir block com.example.app --apply
```

常用参数：

| 参数 | 说明 |
|---|---|
| `--serial <id>` | 指定设备 |
| `--out <dir>` | 输出目录（默认 `noir-out`） |
| `--min <score>` | 只保留分数 ≥ N 的应用 |
| `--limit <n>` | 最多输出 N 条 |
| `--only <regex>` | 只扫描匹配的包名，例如 `--only 'com\.miui\.'` |
| `--adb <path>` / `--jadx <path>` | 手动指定工具路径 |
| `--yes` | 自动化场景下跳过确认 |

## 输出

```
noir-out/
├── report.json      # 结构化结果（应用、权限、端点、建议）
├── report.md        # 人类可读报告
├── domains.txt      # 去重后的上报域名，可直接导入 Pi-hole / AdGuard
└── rollback.txt     # 本次动作的回滚命令
```

## 缓存

扫描结果按 `包名 + APK 路径` 缓存在 `~/.noir/endpoints.tsv`。应用更新后路径变化会自动重新分析，重复扫描通常只需几秒。

---

## 设计取舍

- **端点抽取在设备端完成**：`adb shell` 内跑 `unzip | strings | grep`，避免把几百个 APK 拉回电脑。设备缺少 `unzip`/`strings` 时自动退化为本地解析。
- **共享 UID 特殊处理**：Android 上 `android.uid.system` 下的应用（常见 50+ 个）权限记录在 `dumpsys package` 尾部的 `Shared users` 段。解析器会把这些权限合并回每个包，否则最该被看见的应用反而会消失。
- **建议分级**：只有 appops 级别的动作标记为可自动执行；卸载必须显式开启。
- **无遥测**：程序不包含任何上报逻辑。

## 路线图

- [x] 等宽仪表盘（`noir ui`）
- [ ] 图形界面（Compose Multiplatform）
- [ ] 规则文件可配置（YAML），支持社区规则包
- [ ] 与 `apktool` / `jadx` 的批量流水线
- [ ] 设备端 DNS/VPN 拦截方案导出
- [ ] 扫描结果签名与版本对比

## 许可

MIT
