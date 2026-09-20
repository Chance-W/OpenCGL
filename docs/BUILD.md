# OpenCGL 构建与发布指南

本工程与公开插件仓库分开维护：

- 主程序：`OpenCGL_New`
- API、Base 与全部插件：`OpenCGL-Plugin-New`
- 唯一支持的发布工具链：Azul Zulu JDK 21

## 1. 本地目录和环境

主程序版本只在根 `pom.xml` 的 `<opencgl.version>` 中维护一次；`<software-version>` 仅作为构建器兼容入口引用它。修改该值后，jpackage 的应用版本、安装包/便携包文件名和发布校验和会统一使用同一版本号。GitHub Release 标签仍建议使用对应的 `v<版本>`，例如 `v2.2.4`。

建议两个仓库位于同一父目录。macOS 本机可执行：

```bash
# 文件/目录：终端环境；告诉 Maven、jdeps、jlink、jpackage 使用 Zulu 21。
export JAVA_HOME=/Users/chancew./Software/Java/zulu21.44.17_aarch64/zulu-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# 验证输出必须同时包含 21 和 Azul/Zulu。
java -XshowSettings:properties -version 2>&1 | grep -E 'java.version|java.vendor'
```

## 2. 只验证，不生成安装包

```bash
# 执行位置：OpenCGL_New 根目录。
# 内容：依次构建插件 API/Base/全部插件，校验并收集插件，再测试主程序。
python3 build/build.py verify \
  --plugins-dir ../OpenCGL-Plugin-New
```

如需把 其它 内部插件一并验证和集成，在同一命令追加：

```bash
# 文件：OpenCGL_New/build/build.py
# --plugins-dir 是公开插件仓库；--extra-plugins-dir 可重复指定私有/本地插件仓库。
# 构建器会分别编译、收集，再按 JAR 文件名、module 和 pluginId 检查冲突。
python3 build/build.py verify \
  --plugins-dir /xxx/xxx/code/self_code/OpenCGL-Plugin-New \
  --extra-plugins-dir /xxx/xxx/code/other/Other-FX-Plugin
```

Windows PowerShell：

```powershell
# 文件/环境：当前 PowerShell 会话；改成 Zulu 21 的实际安装目录。
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 执行位置：OpenCGL_New 根目录。
python build\build.py verify --plugins-dir ..\OpenCGL-Plugin-New
```

插件集合写入：

- `OpenCGL-Plugin-New/target/plugin-dist/`：85 个经过 ZIP 完整性校验的运行时插件。
- `OpenCGL-Plugin-New/target/plugin-manifest.json`：插件文件名、大小和 SHA-256。

## 3. 本机生成原生安装包和便携包

```bash
# 执行位置：OpenCGL_New 根目录。
# --output 指定最终发布文件目录；中间文件只写入 target/native-package。
python3 build/build.py package \
  --output target/release
```

插件验证与宿主打包是两个独立步骤。如需同时验证公开插件和 其它 插件，先执行：

```bash
# verify 生成独立插件 JAR/索引；随后 package 只构建宿主。
python3 build/build.py verify \
  --plugins-dir  /xxx/code/self_code/OpenCGL-Plugin-New \
  --extra-plugins-dir /xxx/code/other/Other-FX-Plugin
python3 build/build.py package --output target/release
```

所有平台安装包都只包含宿主程序、宿主运行库以及空的 `ext-plugin/` 目录。插件由应用市场
统一下载到用户目录 `~/.opencgl/ext-plugin/` 后动态加载。公开插件和 其它 插件的
`target/plugin-dist/`、`target/plugin-manifest.json` 是独立发布物，不属于客户端安装包。

平台产物：

- Windows x64：`OpenCGL-Tool-2.2.3-windows-x64.exe` 与 `.zip`
- macOS Intel：`OpenCGL-Tool-2.2.3-macos-x64.dmg` 与 `.tar.gz`
- macOS ARM：`OpenCGL-Tool-2.2.3-macos-arm64.dmg` 与 `.tar.gz`
- Ubuntu x64：`OpenCGL-Tool-2.2.3-linux-x64.deb` 与 `.tar.gz`
- 每个平台同时生成 `build-manifest.json` 和 `SHA256SUMS`

Linux 打 `.deb` 前需要安装 `fakeroot`：

```bash
# 系统依赖：仅 Ubuntu/Debian runner 需要。
sudo apt-get update
sudo apt-get install -y fakeroot
```

## 4. GitHub Actions 文件

### 文件：`.github/workflows/build.yml`

作用：PR、`main/develop` 推送或手动触发时，在四种原生 runner 上执行相同的 `build.py package`。每个平台单独上传 artifact，任一构建失败都不会得到完整发布集。

### 文件：`.github/workflows/release.yml`

作用：推送 `v*` 标签或手动指定已有标签时调用四平台构建；`build/verify_release.py` 确认平台齐全并逐个核验 SHA-256 后，才用 GitHub CLI 创建公开 Release。

### 文件：`.github/actions/setup-opencgl/action.yml`

作用：统一安装 Azul Zulu 21，并按 x64/aarch64 选择 JDK 架构，同时启用 Maven 缓存。

宿主工作流不会 checkout、构建或发布任何插件仓库。插件应在各自仓库的独立流水线中发布。

## 5. 发布前检查

```bash
# 文件：OpenCGL_New/src/test/python；验证构建器和发布保护逻辑。
python3 -m unittest discover -s src/test/python -p 'test_*.py' -v

# 文件：OpenCGL-Plugin-New/src/test/python；验证 reactor 与插件收集器。
python3 -m unittest discover -s ../OpenCGL-Plugin-New/src/test/python -p 'test_*.py' -v

# 对下载后的四平台 artifact 做完整性检查。
python3 build/verify_release.py path/to/downloaded-release
```

当前阶段未配置代码签名。Windows SmartScreen 与 macOS Gatekeeper 可能提示未知发布者；接入证书后应在 `jpackage` 完成后、生成校验和之前签名并公证。

GitHub 当前公布的 `macos-15-intel` runner 可用期到 2027 年 8 月；之后若 GitHub 不再提供 Intel runner，需要改用自托管 Intel Mac，`macos-x64` 的产物名称和构建入口无需改变。
