package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.LocatorBarMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 客户端入口：注册客户端配置、配置界面和定位条 GUI 层。
 */
@Mod(value = LocatorBarMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = LocatorBarMod.MODID, value = Dist.CLIENT)
public final class LocatorBarClient {
    private static final ResourceLocation LOCATOR_BAR_LAYER = LocatorBarMod.rl("locator_bar");

    public LocatorBarClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // 放在快捷栏之上，具体 Y 坐标由渲染器自行控制
        event.registerAbove(VanillaGuiLayers.HOTBAR, LOCATOR_BAR_LAYER, new LocatorBarRenderer());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLocatorData.clear();
    }
}
