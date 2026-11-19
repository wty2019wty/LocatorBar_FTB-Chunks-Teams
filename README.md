# LocatorBar FTB Teams

面向 **Minecraft 1.21.1 / NeoForge 21.1.265** 的**纯客户端**定位条模组。

它读取客户端已经同步的 **FTB Teams** 队伍数据判断队友，并在 HUD 底部绘制类似原版 1.21.2+ 玩家定位条（Locator Bar）的界面。

**作者：wty2019wty**

## 纯客户端说明

本模组：

- 不注册自定义网络包；
- 不要求服务端安装；
- 不向服务端请求额外位置数据；
- 只在客户端读取已有数据并绘制 HUD。

## 位置数据来源

FTB Teams 只同步队伍关系，不同步玩家坐标。因此本模组使用以下客户端数据源：

1. **原版客户端已加载的玩家实体**
   - 位置实时、精确；
   - 但只包含服务端实体追踪范围内的玩家（通常几十到一百多格，取决于服务器视距/实体追踪配置）。

2. **FTB Chunks 客户端长距离玩家追踪（可选）**
   - 安装 FTB Chunks 后，其服务端会把同队/盟友玩家的方块坐标同步到客户端；
   - 可以显示超出原版实体追踪范围的队友；
   - 坐标是方块级精度，且受 FTB Chunks 的 `location_mode` 等配置影响；
   - 通过反射读取 `FTBChunksClient.INSTANCE.longRangePlayerTracker`，FTB Chunks 不存在或字段变化时自动降级。

3. **FTB Teams 客户端队伍数据**
   - `ClientTeamManager` 会在登录时同步所有队伍和玩家的队伍 ID；
   - 用于判断目标玩家是否和自己是同一队伍；
   - 同时读取队伍颜色作为定位条点缀色。

## 功能

- 在快捷栏上方显示横向定位条。
- 中央代表正前方，左右代表左右方向。
- 每个队友显示为彩色圆点：
  - 颜色根据玩家 UUID 稳定分配；
  - 距离越远越透明；
  - 身后玩家贴在边缘并变暗；
  - 相对高低会让圆点轻微上下移动；
  - 目标明显高于/低于你时，圆点上方/下方会显示上下箭头；
  - 名字上方显示直线相对距离（例如 `128 格`）；
  - 方向相近时自动水平避让。
- 定位条边缘和中央准线使用 FTB Teams 队伍颜色。
- 可配置最大距离、是否仅显示同队、是否使用 FTB Chunks 位置、是否显示名字等。

## 依赖

| 依赖 | 是否必需 | 版本 |
| --- | --- | --- |
| Minecraft | 必需 | 1.21.1 |
| NeoForge | 必需 | 21.1.265 |
| FTB Teams | 客户端必需 | 2101.1.0+ |
| FTB Library | 客户端必需 | 2101.1.0+ |
| Architectury API | 客户端的 FTB Teams 前置 | 13.0.8+ |
| FTB Chunks | 可选 | 2101.1.0+ |

> 仅需在**客户端**安装。不要把本模组安装到专用服务器上。
> 如果服务器装了 FTB Chunks，客户端也装 FTB Chunks，才能获得长距离位置数据。

## 配置

配置文件：`config/locatorbar_ftb_teams-client.toml`

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否渲染定位条。 |
| `maxRange` | `0` | 最大显示距离，单位方块。`0` 表示客户端不限制距离；实际范围仍受服务端实体追踪和 FTB Chunks 同步范围限制。 |
| `onlySameTeam` | `true` | 是否只显示同一 FTB Teams 队伍的玩家。改为 `false` 可显示客户端已知的所有玩家。 |
| `useFtbChunks` | `true` | 安装 FTB Chunks 时是否使用其长距离玩家位置。 |
| `showNames` | `true` | 是否在圆点上方显示玩家名字。 |
| `showDistance` | `true` | 是否在名字上方显示直线相对距离。 |
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

构建产物：

```text
build/libs/locatorbar_ftb_teams-0.1.0.jar
```

开发环境客户端：

```powershell
.\gradlew.bat runClient
```

## 工作原理

1. 每个客户端 tick：
   - 从 `ClientTeamManager` 获取自己的队伍成员、队伍 ID 和队伍颜色；
   - 遍历 `Minecraft.getInstance().level.players()` 获取已加载玩家实体位置；
   - 如果启用 FTB Chunks 集成，读取其客户端长距离追踪位置作为补充；
   - 默认不再按距离过滤，按队伍和当前维度过滤后全部显示。
2. 在 `RegisterGuiLayersEvent` 注册的 HUD 层中绘制定位条。
3. 不做任何服务端同步，因此不需要服务端安装，也不会增加服务端网络负担。

## 限制

- **没有 FTB Chunks 时**，只能看到原版客户端已经加载的玩家实体，距离受服务器实体追踪范围限制。
- **有 FTB Chunks 时**，位置更新是方块级且仅在目标移动一定距离后刷新，因此圆点可能有轻微跳动（已做角度平滑）。
- FTB Chunks 的长距离追踪是否包含某名玩家，取决于其 `location_mode`、队伍/盟友关系和服务器配置。
- 跨维度玩家不会显示。

## 许可

本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。

Copyright (c) 2026 wty2019wty
