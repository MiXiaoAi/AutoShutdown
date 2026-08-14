# Changelog / 更新日志

## v1.2.0

### English
- ✨ Added NeoForge 1.21.1 support (multi-loader: Forge 1.20.1 + NeoForge 1.21.1)
- ✨ Refactored into a shared common codebase with per-loader modules
- 🔧 Renamed the mod to "Auto Shutdown" (`auto_shutdown`) under `com.mixiaoai.autoshutdown`
- 🔧 Renamed the config file to `world/serverconfig/auto_shutdown-server.toml`
- 🔧 JARs now carry the loader suffix, e.g. `auto_shutdown-1.20.1-1.2.0-forge.jar`

### 中文
- ✨ 新增 NeoForge 1.21.1 支持（多加载器：Forge 1.20.1 + NeoForge 1.21.1）
- ✨ 重构为共享公共代码 + 分加载器模块
- 🔧 模组更名为 "Auto Shutdown"（`auto_shutdown`），包名 `com.mixiaoai.autoshutdown`
- 🔧 配置文件更名为 `world/serverconfig/auto_shutdown-server.toml`
- 🔧 JAR 文件名带加载器后缀，如 `auto_shutdown-1.20.1-1.2.0-forge.jar`

---

## v1.20.1-1.1.0

### English
- ✨ Adapted to Minecraft 1.20.1
- ✨ Added idle auto-shutdown feature
- ✨ Added hot-reload configuration feature
- ✨ Added configuration status view command
- 🐛 Fixed server shutdown thread blocking issue
- 🔧 Upgraded Gradle to 8.11.1
- 🔧 Upgraded ForgeGradle to 6.0.x

### 中文
- ✨ 适配 Minecraft 1.20.1
- ✨ 新增空闲自动关服功能
- ✨ 新增热重载配置功能
- ✨ 新增配置状态查看命令
- 🐛 修复服务器关闭时线程阻塞问题
- 🔧 升级 Gradle 到 8.11.1
- 🔧 升级 ForgeGradle 到 6.0.x
