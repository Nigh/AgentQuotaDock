# LLM Watcher

将闲置 Android 手机变成常亮横屏看板，集中查看多个 AI 平台的用量、额度与账户余额。

## 功能

- 自动刷新平台数据，左右滑动切换平台；显示用量进度、重置时间和倒计时（取决于平台提供的数据）。
- 切换已用量或剩余用量显示；用量变化时通过动画和背景闪烁提醒。
- 选择标准深色、纯黑 OLED 或浅色主题。
- 防烧屏默认开启，定期平滑移动看板内容；该功能降低烧屏风险，不能完全消除烧屏。
- 用量连续 30 分钟未变化时自动降低屏幕亮度；操作设备或离开应用时恢复亮度。最低和最高亮度可在设置中调整。
- 显示网络、认证、限流和数据不可用等错误，方便排查。

## 支持的平台

| 平台 | 登录或配置方式 | 显示内容 |
| --- | --- | --- |
| Codex | 设备码授权 | 额度 |
| Claude | OAuth 登录 | 5 小时窗口与周用量 |
| Antigravity | Google OAuth 登录，需提供官方客户端的 OAuth client secret | 模型配额 |
| DeepSeek | API key | 账户余额 |
| z.ai | API key，选择中国区或国际区 | 使用额度 |
| MiniMax | API key，选择中国区或国际区 | 使用额度 |
| Cursor | 浏览器登录 | 使用额度 |
| Kimi | 浏览器登录 | 使用额度 |
| Kimi Code API | API key | 使用额度 |

## 安装

需要 Android 7.0（API 24）或更高版本的设备，以及网络连接。

### 从源码构建并安装

构建使用 Docker 镜像 `xianii/android-dev:latest`。Linux/WSL 使用 `build.sh`，Windows 使用 PowerShell 和 WSLC 执行 `build.ps1`。将手机通过 USB 连接电脑，在手机上启用 USB 调试并允许连接。

```bash
git clone https://github.com/Nigh/AgentQuotaDock.git
cd AgentQuotaDock
./build.sh run
```

Windows：

```powershell
git clone https://github.com/Nigh/AgentQuotaDock.git
cd AgentQuotaDock
.\build.ps1 run
```

脚本会构建 Debug APK、安装并启动应用。也可以只执行 `./build.sh build` 或 `.\build.ps1 build`，生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

覆盖安装保留应用数据。如果已安装版本的签名不同，安装会失败；请先备份需要保留的数据，再决定是否卸载旧版本。

## 开始使用

1. 打开应用，点击看板右上角的设置按钮。
2. 在「账号登录」页登录平台或填写 API key。Codex 按设备码页面提示完成授权；Cursor 和 Kimi 登录后点击「完成登录」。
3. 可使用「测试」检查凭据，再打开平台旁的显示开关。
4. 在「系统设置」页选择主题、用量显示方式、亮度范围、防烧屏和刷新间隔。
5. 返回看板，左右滑动切换已启用的平台。看板右上角可手动刷新或打开设置。

凭据通过 Android Keystore 支持的 AES-GCM 加密保存在本机。登录失败时，检查网络、凭据和平台账号状态；限流时等待后再刷新。

## 测试

```bash
./build.sh test
```

Windows：

```powershell
.\build.ps1 test
```

## 第三方项目

平台登录和用量协议参考 [CodexMeter](https://github.com/KyoMio/CodexMeter)，其许可文本见 [third_party/CodexMeter-LICENSE](third_party/CodexMeter-LICENSE)。界面配色参考 [xianii-theme](https://github.com/Nigh/xianii-theme)。
