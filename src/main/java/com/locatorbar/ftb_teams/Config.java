package com.locatorbar.ftb_teams;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 服务端通用配置。
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SYNC_INTERVAL_TICKS = BUILDER
            .comment("队友位置同步间隔（单位：tick）。20 tick = 1 秒。",
                    "How often teammate positions are synchronized, in ticks. 20 ticks = 1 second.")
            .defineInRange("syncIntervalTicks", 10, 1, 200);

    public static final ModConfigSpec.IntValue MAX_RANGE = BUILDER
            .comment("定位条显示的最大距离（单位：方块）。超过此距离的队友不会被同步。",
                    "Maximum distance in blocks at which teammates are shown.")
            .defineInRange("maxRange", 256, 16, 4096);

    public static final ModConfigSpec.BooleanValue ONLY_SAME_TEAM = BUILDER
            .comment("是否只显示同一 FTB Teams 队伍的玩家。",
                    "If true, only players in the same FTB Teams team are shown.",
                    "Set to false to show every nearby player instead.")
            .define("onlySameTeam", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
