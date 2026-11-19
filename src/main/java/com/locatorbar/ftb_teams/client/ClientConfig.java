package com.locatorbar.ftb_teams.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 纯客户端配置。
 */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("是否渲染定位条。", "Whether to render the locator bar.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue MAX_RANGE = BUILDER
            .comment("定位条显示的最大距离（单位：方块）。0 表示不限制。",
                    "Maximum distance in blocks at which players are shown.",
                    "0 means no client-side distance limit. The actual range is still",
                    "limited by what the server/FTB Chunks has synchronized to the client.")
            .defineInRange("maxRange", 0, 0, 1000000);

    public static final ModConfigSpec.BooleanValue ONLY_SAME_TEAM = BUILDER
            .comment("是否只显示同一 FTB Teams 队伍的玩家。",
                    "If true, only players in the same FTB Teams team are shown.",
                    "Set to false to show every player known to the client.")
            .define("onlySameTeam", true);

    public static final ModConfigSpec.BooleanValue USE_FTB_CHUNKS = BUILDER
            .comment("如果安装了 FTB Chunks，是否使用它的长距离玩家追踪位置。",
                    "When FTB Chunks is installed, use its long-range tracked player positions.",
                    "This allows showing teammates outside vanilla entity tracking range.",
                    "Disable to use only loaded vanilla player entities.")
            .define("useFtbChunks", true);

    public static final ModConfigSpec.BooleanValue SHOW_NAMES = BUILDER
            .comment("是否在定位点上方显示玩家名字。", "Whether to show player names above the markers.")
            .define("showNames", false);

    public static final ModConfigSpec.IntValue BAR_WIDTH = BUILDER
            .comment("定位条宽度（单位：GUI 像素）。", "Width of the locator bar in GUI pixels.")
            .defineInRange("barWidth", 182, 80, 400);

    public static final ModConfigSpec.IntValue Y_OFFSET = BUILDER
            .comment("定位条的垂直偏移（正数向下，负数向上）。",
                    "Vertical offset of the locator bar. Positive values move it down.")
            .defineInRange("yOffset", 0, -100, 100);

    public static final ModConfigSpec.DoubleValue SCALE = BUILDER
            .comment("定位条整体缩放比例。", "Scale of the locator bar.")
            .defineInRange("scale", 1.0D, 0.5D, 2.0D);

    public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
            .comment("在日志中输出定位条调试信息（队伍成员数、可见玩家数、FTB Chunks 位置数）。",
                    "Log locator bar diagnostics: team member count, visible players, FTB Chunks positions.")
            .define("debug", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
