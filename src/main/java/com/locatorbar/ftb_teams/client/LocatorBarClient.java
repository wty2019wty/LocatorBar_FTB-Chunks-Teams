package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.LocatorBarMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 客户端入口。没有任何服务端逻辑或自定义网络包。
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
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientTeamPositions.update();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && minecraft.player != null && minecraft.level != null) {
            boolean openFilter = false;
            // 一次消费掉所有排队的点击，避免关闭界面后立刻又被旧点击重新打开
            while (ClientKeyMappings.OPEN_FILTER.consumeClick()) {
                openFilter = true;
            }
            if (openFilter) {
                minecraft.setScreen(new PlayerFilterScreen(null));
            }
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LOCATOR_BAR_LAYER, new LocatorBarRenderer());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTeamPositions.clear();
    }
}
