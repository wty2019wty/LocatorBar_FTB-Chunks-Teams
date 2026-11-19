package com.locatorbar.ftb_teams;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * 主模组类。
 * <p>
 * 这是一个纯功能性模组：服务端通过 FTB Teams API 获取队伍成员位置，
 * 再通过自定义网络包同步给客户端，由客户端绘制定位条 HUD。
 */
@Mod(LocatorBarMod.MODID)
public class LocatorBarMod {
    public static final String MODID = "locatorbar_ftb_teams";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LocatorBarMod(IEventBus modEventBus, ModContainer modContainer) {
        // 服务端同步参数（同步间隔、最大距离、是否仅同队）
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("LocatorBar FTB Teams 已加载，定位条将使用 FTB Teams 的队伍数据。");
    }

    /**
     * 便捷方法：生成本模组命名空间下的 ResourceLocation。
     */
    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
