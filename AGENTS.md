# AGENTS.md

本文件是本项目的 agent 协作准则。任何 agent 在开始修改本项目时都必须先阅读本文件；如果工作目录下还有更深层的 `AGENTS.md`，以更深层文件为优先准则。

## 项目现状

- 项目名：`LLM Watcher`
- 类型：新建 Android 应用，当前为单模块工程 `:app`
- 语言与 UI：Kotlin + Jetpack Compose + Material 3
- 构建系统：Gradle Kotlin DSL，使用 Version Catalog `gradle/libs.versions.toml`
- 当前包名与 namespace：`com.example.llmwatcher`
- 入口 Activity：`app/src/main/java/com/example/llmwatcher/MainActivity.kt`
- 主题入口：`app/src/main/java/com/example/llmwatcher/ui/theme/Theme.kt`
- SDK：`compileSdk = 36`，`minSdk = 24`，`targetSdk = 36`
- Java/Kotlin 目标：JVM 11

当前代码基本保持 Android Studio 新建 Compose 模板状态，只有默认 `Greeting` 示例界面和默认主题文件。后续实现真实功能时，应同步更新本节，避免让后来接手的 agent 误判项目阶段。

## 必须保持同步

后续 agent 每次完成会改变项目理解的信息时，必须同步更新本文件。包括但不限于：

- 新增、删除或重命名模块、包名、主要目录或入口文件
- 引入新的架构分层、状态管理、导航、数据库、网络、后台任务或依赖注入方案
- 修改 Gradle 插件、SDK 版本、重要依赖或构建命令
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
- 避免保留 Android Studio 模板示例代码作为真实功能的一部分；一旦开始实现产品功能，应删除或替换默认 `Greeting` 示例。

## 验证命令

常用命令从项目根目录执行：

```bash
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
./gradlew assembleDebug
```

根据改动范围选择验证命令。只改文档通常不需要运行 Gradle；修改 Kotlin、Compose、资源或 Gradle 配置后，至少运行与改动相关的测试或构建检查，并在最终回复中说明已运行或未运行的命令。

## 工作方式

- 开始任务前先查看相关文件，不要基于模板默认结构做过度假设。
- 保持改动范围聚焦；不要顺手重构无关模板代码或格式化大量无关文件。
- 如果工作树已有用户改动，必须保留并与之协作，不要回滚不属于自己的修改。
- 新增功能时，同步补充必要测试；如果暂时无法测试，在回复中明确说明原因和剩余风险。
- 当项目形成更具体的架构后，把关键规则沉淀到本文件，使后续 agent 能快速理解最新项目状态。
