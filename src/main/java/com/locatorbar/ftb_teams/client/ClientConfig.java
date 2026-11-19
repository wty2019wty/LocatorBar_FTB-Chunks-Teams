package com.locatorbar.ftb_teams.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 纯客户端渲染配置。
 */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("是否渲染定位条。", "Whether to render the locator bar.")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue SHOW_NAMES = BUILDER
            .comment("是否在定位点上方显示队友名字。", "Whether to show teammate names above the markers.")
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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
