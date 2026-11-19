# AGENTS.md

本文件是本项目的 agent 协作准则。任何 agent 在开始修改本项目时都必须先阅读本文件；如果工作目录下还有更深层的 `AGENTS.md`，以更深层文件为优先准则。

## 项目现状

- 项目名：`LLM Watcher`
- 产品定位：利用废旧 Android 手机作为常亮横屏看板，集中展示多个 LLM 平台的用量状态
- 类型：新建 Android 应用，当前为单模块工程 `:app`
- 语言与 UI：Kotlin + Jetpack Compose + Material 3
- 构建系统：Gradle Kotlin DSL，使用 Version Catalog `gradle/libs.versions.toml`
- 当前包名与 namespace：`com.example.llmwatcher`
- 入口 Activity：`app/src/main/java/com/example/llmwatcher/MainActivity.kt`
- 主题入口：`app/src/main/java/com/example/llmwatcher/ui/theme/Theme.kt`
- SDK：`compileSdk = 36`，`minSdk = 24`，`targetSdk = 36`
- Java/Kotlin 目标：JVM 11
- 构建与执行环境：统一使用 Docker 镜像 `xianii/android-dev:latest` 配合 `./build.sh` / `build.ps1`，解耦宿主 Android SDK 与 Java 环境依赖

当前已从 Android Studio 默认 Compose 模板迁移到首版端到端 MVP 骨架。App 使用内存状态和 mock 数据仓库展示多个 LLM 平台用量，尚未接入真实平台 API，也未做持久化凭据存储。

## 构建与设备

- **只用 `./build.sh`（Linux/WSL + Docker）或 `.\build.ps1`（Windows + WSLC）** 编译、测试、安装、adb；勿直接使用本机 `adb`/`gradlew`。
- 镜像：`xianii/android-dev:latest`（`ANDROID_DEV_IMAGE` 可覆盖），见 `android-dev-docker`。
- 编译：容器 root + `GRADLE_USER_HOME=/workspace/.gradle-docker`（不挂载宿主 `~/.gradle`，避免 journal lock）；脚本通过 Gradle Wrapper launcher 启动，兼容 Windows checkout 的 CRLF。
- Linux adb：`build.sh` 使用 `--user` + USB + `~/.android`，安装前会 `adb kill-server` 释放宿主 adb；Windows adb：`build.ps1` 通过 WSLC 容器使用 `%USERPROFILE%\.android`。
- 安装使用 `adb install -r -t` 覆盖更新，保留应用数据；签名不匹配直接报错（不得自动卸载回退）。Linux/Windows 脚本行为一致。
- 子命令：`build` / `release` / `test` / `install` / `run` / `adb <args>`；无参 = `build` + `install`。
- Release 签名：`./build.sh release` / `.\build.ps1 release` 读取 `keystore/release.env`；`/keystore/` 不入库。
- `build.sh` 与 `build.ps1` 的功能或参数有改动时，必须同步修改另一个脚本及本文件。

## 产品需求

- 主界面优先为横屏常亮看板优化，适合旧手机长时间放置显示。
- 用户可以在设置中选择常用 LLM 平台，并为每个平台配置 API key、令牌或其他必要凭据。
- App 自动请求不同平台的用量数据，并在主界面以进度条为核心形式展示。
- 不同平台的用量统计规则可能不同，展示模型应按平台定制。例如 Claude Code 需要显示 5 小时用量和周用量两条进度条。
- App 需要请求并使用必要权限来保持屏幕常亮。
- 当数据一段时间未变化时调暗屏幕；当检测到数据变化时调亮屏幕。
- 屏幕最暗亮度与最亮亮度应可在设置中配置。
- 视觉主题以深色与纯黑为主，并在设置中提供主题切换选项（标准深色主题与专为 OLED 优化的纯黑主题）。
- 在无网络、认证失败、接口异常、限流或平台数据不可用时，主界面应显示合适且可理解的错误信息。

## 视觉与主题规范

- 视觉设计配色参考：统一参考 [xianii-theme](https://github.com/Nigh/xianii-theme)（仅包含 dark 配色体系）。
  - 主色与辅色：Primary `#FFA1AD`（OnPrimary `#242424`）、Secondary `#D8B0FF`（OnSecondary `#242424`）、Tertiary `#7FCFC4`（OnTertiary `#242424`）。
  - 语义反馈色：Error `#FA6863`（OnError `#1E1311`）、Success `#7FC08C`、Warning `#EEBC4A`、Info `#7CB3EB`。
  - 标准深色主题（Dark）：
    - 背景底色：`#161616`（base-200）
    - 表面与卡片色：`#242424`（base-100）
    - 次级表面：`#404040`（base-300）
    - 文字与前景色：`#F2F2F2`（base-content）
    - 边框与轮廓：`#404040`
    - 中性色：`#44403C`
- OLED 纯黑主题（OLED Black）：
  - 专为长时间常亮放置的旧 OLED 手机设计，彻底消除发光背景的电量消耗、降低发热并预防烧屏。
  - 大部分背景均设置为纯黑色（`#000000`），包括主看板背景、卡片底色、设置界面背景等。
  - 界面元素与卡片边界采用低对比度描边（如 Outline `#333333` / `#404040`）进行区隔，保持纯黑底色的同时维持清晰层次与远距离可读性。
- 主题切换：设置中提供明确选项供用户在“标准深色”（Dark）和“纯黑 OLED”（OLED Black）之间切换。

## 设计重点

- 横屏是主要体验，不要只按竖屏手机界面设计主界面；布局应优先保证远距离可读性。
- 主界面应突出看板信息密度、状态变化和异常提示，避免需要频繁触控操作。
- 设置页可以承载平台选择、凭据配置、主题模式切换、亮度范围、刷新频率等低频操作。
- API key、令牌等敏感信息必须使用 Android 合适的本地安全存储方案；不要写入日志、截图、测试快照或明文配置文件。
- 平台接入代码应隔离差异，避免把某个平台的用量规则硬编码进通用 UI 层。
- 网络错误、认证错误、解析错误和平台限流应尽量区分展示，便于用户自行修复配置或网络问题。

## 当前实现结构

- `model/`：平台、用量指标、错误状态、主题偏好、亮度配置和 UI 状态模型。
- `data/`：`UsageRepository` 接口和 `MockUsageRepository`，首版通过 mock 数据模拟 Claude Code、OpenAI、Gemini、Cursor 等平台。
- `brightness/`：纯 Kotlin 亮度策略，负责根据数据指纹变化决定是否调暗和目标亮度。
- `ui/app/`：应用根组件和内存状态管理；当前没有引入 Navigation、ViewModel、DataStore 或依赖注入框架。
- `ui/dashboard/`：强制横屏看板，主界面为固定全屏单页，不可上下滚动；通过左右滑动分页切换平台，每页只显示一个来源。每页卡片右上显示当前 `HH:mm` 时间和紧凑图标操作，错误信息以悬浮气泡展示；进度条上方末尾显示详细用量。
- `ui/settings/`：平台开关、凭据输入占位、凭据测试按钮、主题选择、亮度范围和刷新间隔。

首版凭据输入只保存在内存状态中，用于打通配置体验。接入真实 API 前必须先设计安全存储方案，并同步更新本文件。

## 必须保持同步

后续 agent 每次完成会改变项目理解的信息时，必须同步更新本文件。包括但不限于：

- 新增、删除或重命名模块、包名、主要目录或入口文件
- 引入新的架构分层、状态管理、导航、数据库、网络、后台任务或依赖注入方案
- 修改 Gradle 插件、SDK 版本、重要依赖或构建命令（包括 `build.sh`、`build.ps1` 变更）
- 视觉与设计系统规范、主题配色调整
- 新增必须运行的测试、格式化、lint 或发布检查
- 形成新的编码约定、资源命名约定或目录组织方式
- 从模板示例迁移到真实业务实现后，更新“项目现状”和相关约定

不要把本文件当作历史日志。更新时应让它描述当前真实状态，移除过期内容，而不是不断追加流水账。

## 开发约定

- 优先沿用 Kotlin、AndroidX、Jetpack Compose 和 Material 3 的官方惯用写法。
- UI 优先使用 Compose；不要新增 XML 布局，除非已有功能明确需要。
- 可组合函数保持小而清晰；可预览的 UI 尽量提供 `@Preview`。
- 主题、颜色、排版相关变更放在 `app/src/main/java/com/example/llmwatcher/ui/theme/` 下，除非后续建立新的设计系统目录。
- 依赖版本统一维护在 `gradle/libs.versions.toml`，不要在模块 `build.gradle.kts` 中硬编码新依赖版本。
- 修改包名、`applicationId` 或 `namespace` 前，先确认影响范围，包括 Manifest、测试、资源和可能的发布配置。
- 不要恢复 Android Studio 模板示例代码；默认 `Greeting` 示例已经被 MVP 看板替换。
- 当前首版没有真实网络层。新增真实平台接入时，应先扩展 `UsageRepository` 抽象，并为各平台用量规则补充单元测试。
- 主界面进度条按用量等级着色：低于 70% 为绿色（`#7FC08C`），70% 及以上为橙色（`#EEBC4A`），90% 及以上使用 Error 红色（`#FA6863`）。

## 验证命令

所有构建、测试与安装命令统一通过 Docker 脚本执行：

```bash
# 运行单元测试
./build.sh test

# 编译 Debug APK
./build.sh build

# 安装并运行到连接的设备
./build.sh run

# Windows 环境下对应命令（PowerShell + WSLC）
.\build.ps1 test
.\build.ps1 build
.\build.ps1 run
```

根据改动范围选择验证命令。只改文档通常不需要运行构建；修改 Kotlin、Compose、资源、编译脚本或 Gradle 配置后，至少运行与改动相关的测试或构建检查，并在最终回复中说明已运行或未运行的命令。

## 工作方式

- 开始任务前先查看相关文件，不要基于模板默认结构做过度假设。
- 保持改动范围聚焦；不要顺手重构无关模板代码或格式化大量无关文件。
- 如果工作树已有用户改动，必须保留并与之协作，不要回滚不属于自己的修改。
- 新增功能时，同步补充必要测试；如果暂时无法测试，在回复中明确说明原因和剩余风险。
- 当项目形成更具体的架构后，把关键规则沉淀到本文件，使后续 agent 能快速理解最新项目状态。
