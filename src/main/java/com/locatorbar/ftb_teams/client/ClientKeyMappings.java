package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.LocatorBarMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键绑定。
 * <p>
 * {@link RegisterKeyMappingsEvent} 是模组加载期事件，FML 会根据事件类型
 * 自动把该订阅者注册到 MOD 总线。
 */
@EventBusSubscriber(modid = LocatorBarMod.MODID, value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final KeyMapping OPEN_FILTER = new KeyMapping(
            "key.locatorbar_ftb_chunks_teams.open_filter",
            KeyConflictContext.IN_GAME,
            // 默认组合键：Ctrl + O
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.locatorbar_ftb_chunks_teams"
    );

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FILTER);
    }
}
