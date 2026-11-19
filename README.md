# LocatorBar FTB Teams

一个面向 **Minecraft 1.21.1 / NeoForge 21.1.265** 的定位条模组。

它通过 **FTB Teams** 的队伍数据判断哪些玩家是自己的队友，并在 HUD 底部绘制类似原版 1.21.2+ 玩家定位条（Locator Bar）的界面，用来指示队友的相对方向和大致距离。

## 功能

- 在快捷栏上方显示一条横向定位条。
- 中央代表玩家正前方，左右分别代表左右方向。
- 每个队友显示为一个彩色圆点：
  - 颜色由玩家 UUID 稳定分配，便于区分不同队友。
  - 距离越远，圆点越透明。
  - 目标位于身后时，圆点会贴在定位条边缘并进一步变暗。
  - 目标高于/低于玩家时，圆点会轻微上移/下移。
  - 方向相近的圆点会自动水平避让，避免完全重叠。
- 定位条的上下边缘和中央准线使用 FTB Teams 队伍颜色点缀。
- 可在客户端配置中开启队友名字显示。
- 默认只显示同一 FTB Teams 队伍的玩家，也可在服务端配置中改为显示所有附近玩家。
- 跨维度玩家不会显示（与原版定位条行为一致）。

## 依赖

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 |
| FTB Teams | 2101.1.0+（开发时使用 2101.1.12） |
| FTB Library | 2101.1.0+（开发时使用 2101.1.37） |
| Architectury API | 13.0.8（FTB Teams 的前置） |

FTB Teams、FTB Library 与 Architectury API 由 Gradle 从官方 Maven 仓库自动下载：

- FTB Maven：`https://maven.ftb.dev/releases`
- Architectury Maven：`https://maven.architectury.dev/`

发布整合包时请确保玩家安装 FTB Teams、FTB Library 和 Architectury API。

## 工作原理

FTB Teams 的公开 API 提供了队伍、成员和在线成员信息，但**并不直接同步玩家坐标**。因此本模组的实现方式如下：

1. 服务端每 `syncIntervalTicks` tick 调用一次 `FTBTeamsAPI`：
   - 使用 `TeamManager#getTeamForPlayer` 获取玩家的“有效队伍”（个人队伍或队伍频道）。
   - 使用 `Team#getOnlineMembers` 获取同队在线成员。
   - 过滤掉不同维度、超过最大距离的成员。
   - 读取 `TeamProperties.COLOR` 作为定位条点缀色。
2. 通过 NeoForge 自定义网络包 `locatorbar_ftb_teams:team_positions`，把队友位置单独发送给对应玩家。
3. 客户端缓存最新数据，并在 `RegisterGuiLayersEvent` 注册的 HUD 层中绘制定位条。
4. 数据超过 2.5 秒未更新会自动隐藏，避免玩家退出或断线后残留标记。

所有位置同步都只发送给同一队伍的玩家，服务端不会把队友坐标广播给无关玩家。

## 配置文件

### 服务端（`config/locatorbar_ftb_teams-common.toml`）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `syncIntervalTicks` | `10` | 位置同步间隔，单位 tick。20 tick = 1 秒。 |
| `maxRange` | `256` | 最大显示距离，单位方块。 |
| `onlySameTeam` | `true` | 是否只显示同一 FTB Teams 队伍的玩家。改为 `false` 可显示所有附近玩家。 |

### 客户端（`config/locatorbar_ftb_teams-client.toml`）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否渲染定位条。 |
| `showNames` | `false` | 是否在圆点上方显示队友名字。 |
| `barWidth` | `182` | 定位条宽度，单位 GUI 像素。 |
| `yOffset` | `0` | 垂直偏移，正数向下、负数向上。 |
| `scale` | `1.0` | 整体缩放比例。 |

也可以直接在游戏内通过 **模组列表 → LocatorBar FTB Teams → 配置** 修改。

## 构建

环境要求：

- JDK 21
- Gradle Wrapper（项目已自带）

```powershell
.\gradlew.bat build
```

构建产物位于：

```text
build/libs/locatorbar_ftb_teams-*.jar
```

开发环境客户端/服务端：

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
```

## 目录结构

```text
src/main/java/com/locatorbar/ftb_teams/
├── LocatorBarMod.java              # 主模组类，注册通用配置
├── Config.java                     # 服务端配置
├── client/
│   ├── ClientConfig.java           # 客户端渲染配置
│   ├── ClientLocatorData.java      # 客户端位置缓存与角度平滑
│   ├── LocatorBarClient.java       # 客户端入口 / GUI 层注册
│   └── LocatorBarRenderer.java     # 定位条 HUD 渲染
├── network/
│   ├── LocatorBarNetwork.java      # 网络包注册与客户端处理
│   └── TeamPositionsPayload.java   # 队友位置同步包
└── server/
    └── TeamPositionSync.java       # 服务端周期性收集并发送队友位置
```

## 与原版定位条的区别

- 原版定位条从 1.21.2 才开始提供；本模组为 1.21.1 实现了类似效果。
- 本模组默认只显示 FTB Teams 同队玩家，而不是服务器上的所有玩家。
- 圆点颜色根据玩家 UUID 分配，不使用原版的玩家颜色机制。

## 许可

本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。
