# Vivo 健康音乐白名单注入（LSPosed / libxposed API 102）

让 vivo 健康 App 在连接 Android 手机时，能识别并控制任意音乐 App。

## 界面（Jetpack Compose + Material Design 3）

- **从应用列表勾选**：读取所有带桌面图标的应用（名称 + 图标 + 包名），点击整行或复选框即可切换，不再需要手动输入包名。
- **搜索**：顶部搜索框实时按应用名或包名过滤。
- **筛选**：`全部 / 已选 / 内置` 三个 FilterChip，分别查看已安装应用、自定义选择、vivo 健康内置白名单。
- **内置识别**：内置白名单中的应用在列表里带「内置」徽标并显示为已生效（只读、不可取消）；未安装的内置包名也会列在「内置」里并标「未安装」。
- **实时状态**：顶部卡片显示 LSPosed 框架的连接状态、版本，以及「已选 / 内置」数量；未连接时给出提示。
- **深浅色**：自动跟随系统；Android 12+ 支持 Material You 动态取色，其余设备使用内置青绿配色。
- **手动兜底**：右上角「更多」菜单保留「手动输入包名」，用于没有桌面图标的应用；若该包名已被 vivo 健康内置，会直接提示无需添加。
- **关于 / 使用说明**：右上角 **ⓘ 按钮**直接进入（不在二级菜单），说明模块功能、工作原理、使用方法、列表含义与常见问题，并显示版本与连接状态。
- **隐藏桌面图标**：在「关于」页一键隐藏 / 恢复桌面图标（见下文）。
- **Edge-to-edge**：透明系统栏，列表内容一直画到屏幕底部（滚动时会从系统手势条下方经过），保留系统手势条本身，只避让顶部 AppBar。

选择结果通过 `RemotePreferences` 实时同步，无需「保存」按钮。

## 构建

```bash
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

release 使用 debug 签名（`signingConfig = signingConfigs["debug"]`），方便直接 sideload；正式发布请替换成自己的签名。

## 安装与使用

1. 安装 APK，并在 LSPosed 中启用「Vivo 健康音乐白名单」模块。
   作用域已由 `META-INF/xposed/scope.list` 固定为 `com.vivo.health`（`staticScope=true`），无需手动勾选。
2. 强制停止并重新打开 **vivo 健康**，让模块注入。
3. 打开本模块 App：在搜索框输入应用名（如「网易云音乐」）或包名，勾选目标音乐 App。勾选结果会实时同步。
4. 回到手机播放音乐，手表音乐卡片即可显示并控制。

## 问题成因

反编译 vivo 健康 7.1.1.02 后可以看到，音乐控制白名单是硬编码的：

```smali
# MusicCtrlManager.smali <clinit> —— 约 100 个硬编码包名，存进静态字段 d
const-string v10, "com.netease.cloudmusic"
const-string v11, "com.kugou.android"
...
sput-object v0, Lcom/vivo/health/devices/watch/musiccontrol/MusicCtrlManager;->d:Ljava/util/List;
```

调用链：

| 位置 | 作用 |
| --- | --- |
| `MusicCtrlManager$Companion.b()` | `getAppList()`，返回字段 `d`（白名单） |
| `MusicCtrlManager$Companion.m(pkg, controllers)` | `updateMediaControl()`，先调用 `b()` |
| `MusicCtrlManager$Companion.k(pkg, controllers, whitelist)` | `selectController()`，只保留 `whitelist.contains(controller.getPackageName())` 的会话 |
| `MusicModule.onCallback` | 再判断一次 `appList.contains(musicExtra.a())`，`a()` 就是 controller 的包名 |

播放音乐的包名不在白名单里 → `selectController()` 返回 null → 手机不把播放状态推给手表 → 手表一直显示「请先在手机播放音乐」。普通应用通知走的是另一条链路，所以不受影响。

`access$getAppList$cp()` 只被 `b()` 调用，因此 **Hook `b()` 一个点即可覆盖全部白名单读取**。

## 方案

```
模块 App（Compose 界面，从已安装应用列表勾选）
   │  写入 RemotePreferences(group=music_whitelist, key=packages)
   ▼
LSPosed 框架数据库  ──(变更回调)──▶  com.vivo.health 进程
                                      │
                        Hook MusicCtrlManager$Companion.b()
                                      │
                        返回 原白名单 + 用户勾选的包名（去重）
```

- 采用 libxposed **现代 API 102**（`io.github.libxposed:api:102.0.0`）。
- 模块 App 侧用 `io.github.libxposed:service:102.0.0` 通过 `RemotePreferences` 与框架通信。
- Hook 侧 `getRemotePreferences()` 返回的是**可实时更新**的对象（LSPosed 通过 `IRemotePreferenceCallback` 推送），所以勾选后无需重启健康 App，下一次播放状态变化就会生效。

## 常见问题

- 列表里找不到某个音乐 App？

  该 App 可能没有桌面图标。用右上角菜单的「手动输入包名」补充，包名可用 `adb shell pm list packages | grep -i music` 查询。

- 改完列表要重启健康 App 吗？
正常情况下不需要，RemotePreferences 会实时推送。若长时间没反应，把音乐暂停/播放一次触发会话变化，或强制停止健康 App 重新打开。

- 换健康 App 版本后失效了？
Hook 是按「`MusicCtrlManager$Companion` 上无参且返回 `java.util.List` 的方法」定位的，并优先匹配已知混淆名 `b`；若 vivo 改了类名/包名结构，需要按新版本重新定位。当前适配 `7.1.1.02`。

- 为什么模块 App 自己不在作用域里？
libxposed 现代 API 下模块 App 不会被 Hook，框架通过模块自身的 `XposedService` ContentProvider 把服务 binder 发给模块 App。

## 兼容

仅使用 Vivo Watch GT2 实机测试通过，理论上所有使用 vivo 健康 App 的手表都能使用。

## 说明

这是原型实现：白名单在内存里按需合并（不修改 App 原始字段），只对用户显式勾选（或手动输入）的包名生效，列表为空时行为与未安装模块完全一致。

本项目与 vivo 官方无关，仅供个人学习与设备兼容性研究；仓库中不含 vivo 健康的 APK、反编译镜像或任何官方资源。
