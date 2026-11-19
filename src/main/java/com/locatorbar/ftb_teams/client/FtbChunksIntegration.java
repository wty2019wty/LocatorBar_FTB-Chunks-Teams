package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.LocatorBarMod;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 可选集成：读取 FTB Chunks 客户端已经同步的长距离玩家位置。
 * <p>
 * FTB Chunks 服务端会通过 {@code SendPlayerPositionPacket} 把被追踪玩家的方块坐标发送到客户端，
 * 客户端保存在 {@code FTBChunksClient.INSTANCE.longRangePlayerTracker} 中。
 * 该字段不是公开 API，因此这里使用反射读取；如果 FTB Chunks 不存在或字段发生变化，
 * 会自动降级为只使用原版客户端已加载的玩家实体位置。
 */
public final class FtbChunksIntegration {
    private static boolean initialized;
    private static boolean available;
    private static Object clientInstance;
    private static Field trackerField;
    private static Method getPosMethod;

    private FtbChunksIntegration() {
    }

    /**
     * @return 玩家 UUID -> 客户端已知的方块中心位置
     */
    public static Map<UUID, Vec3> trackedPositions() {
        init();
        if (!available) {
            return Map.of();
        }

        try {
            Object mapObject = trackerField.get(clientInstance);
            if (!(mapObject instanceof Map<?, ?> tracked) || tracked.isEmpty()) {
                return Map.of();
            }

            Map<UUID, Vec3> result = new HashMap<>(tracked.size());
            for (Map.Entry<?, ?> entry : tracked.entrySet()) {
                if (!(entry.getKey() instanceof UUID playerId) || entry.getValue() == null) {
                    continue;
                }
                Object posObject = getPosMethod.invoke(entry.getValue(), 0.0F);
                if (posObject instanceof Vec3 pos) {
                    result.put(playerId, pos);
                }
            }
            return result;
        } catch (Throwable throwable) {
            available = false;
            LocatorBarMod.LOGGER.warn("读取 FTB Chunks 长距离玩家位置失败，将只使用原版实体位置：{}", throwable.toString());
            return Map.of();
        }
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!ModList.get().isLoaded("ftbchunks")) {
            return;
        }

        try {
            Class<?> clientClass = Class.forName("dev.ftb.mods.ftbchunks.client.FTBChunksClient");
            clientInstance = clientClass.getField("INSTANCE").get(null);

            trackerField = clientClass.getDeclaredField("longRangePlayerTracker");
            trackerField.setAccessible(true);

            Class<?> iconClass = Class.forName("dev.ftb.mods.ftbchunks.client.mapicon.TrackedPlayerMapIcon");
            getPosMethod = iconClass.getMethod("getPos", float.class);

            available = true;
            LocatorBarMod.LOGGER.info("已启用 FTB Chunks 长距离玩家位置集成。");
        } catch (Throwable throwable) {
            LocatorBarMod.LOGGER.warn("无法启用 FTB Chunks 长距离玩家位置集成，将只使用原版实体位置：{}", throwable.toString());
        }
    }
}
