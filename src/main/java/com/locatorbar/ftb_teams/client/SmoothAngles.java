package com.locatorbar.ftb_teams.client;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 每个玩家标记点的角度平滑缓存，避免 FTB Chunks 的方块级位置更新造成跳动。
 */
final class SmoothAngles {
    private static final Map<UUID, Float> ANGLES = new HashMap<>();

    private SmoothAngles() {
    }

    static float update(UUID playerId, float targetAngle) {
        Float previous = ANGLES.get(playerId);
        float result;
        if (previous == null) {
            result = targetAngle;
        } else {
            float difference = Mth.wrapDegrees(targetAngle - previous);
            result = Mth.wrapDegrees(previous + difference * 0.35F);
        }
        ANGLES.put(playerId, result);
        return result;
    }

    static void retain(Set<UUID> playerIds) {
        ANGLES.keySet().retainAll(playerIds);
    }

    static void clear() {
        ANGLES.clear();
    }
}
