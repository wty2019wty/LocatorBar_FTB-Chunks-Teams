package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.network.TeamPositionsPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端保存的最新一次定位数据。
 */
public final class ClientLocatorData {
    /** 超过这个时间没有收到新数据，就认为数据已过期（避免玩家退出/断线后残留定位点）。 */
    private static final long EXPIRE_MILLIS = 2500L;

    private static ResourceLocation dimension;
    private static float range = 256.0F;
    private static int teamColor = 0xFFFFFF;
    private static List<TeamPositionsPayload.Entry> entries = List.of();
    private static long lastUpdateMillis;

    /** 每个玩家的显示角度，用于平滑旋转。 */
    private static final Map<UUID, Float> SMOOTH_ANGLES = new HashMap<>();

    private ClientLocatorData() {
    }

    public static void accept(TeamPositionsPayload payload) {
        dimension = payload.dimension();
        range = Math.max(1.0F, payload.range());
        teamColor = payload.teamColor() & 0xFFFFFF;
        entries = List.copyOf(payload.entries());
        lastUpdateMillis = System.currentTimeMillis();

        // 清理已经离开队伍/离开范围的玩家，防止平滑数据无限增长
        Set<UUID> ids = new HashSet<>();
        for (TeamPositionsPayload.Entry entry : entries) {
            ids.add(entry.playerId());
        }
        SMOOTH_ANGLES.keySet().retainAll(ids);
    }

    /**
     * 断开连接时清空缓存，避免在下一个服务器中短暂显示上一个服务器的数据。
     */
    public static void clear() {
        dimension = null;
        entries = List.of();
        lastUpdateMillis = 0L;
        SMOOTH_ANGLES.clear();
    }

    /**
     * 获取当前可渲染的快照。维度不一致或数据过期时返回空快照。
     */
    public static Snapshot snapshot(ResourceLocation currentDimension) {
        if (entries.isEmpty()) {
            return Snapshot.EMPTY;
        }
        if (System.currentTimeMillis() - lastUpdateMillis > EXPIRE_MILLIS) {
            return Snapshot.EMPTY;
        }
        if (dimension == null || !dimension.equals(currentDimension)) {
            return Snapshot.EMPTY;
        }
        return new Snapshot(entries, range, teamColor);
    }

    /**
     * 将目标相对角度平滑过渡到当前显示角度，避免标记点在网络更新时突然跳动。
     */
    public static float smoothAngle(UUID playerId, float targetAngle) {
        Float previous = SMOOTH_ANGLES.get(playerId);
        float result;
        if (previous == null) {
            result = targetAngle;
        } else {
            float difference = Mth.wrapDegrees(targetAngle - previous);
            result = Mth.wrapDegrees(previous + difference * 0.35F);
        }
        SMOOTH_ANGLES.put(playerId, result);
        return result;
    }

    public record Snapshot(List<TeamPositionsPayload.Entry> entries, float range, int teamColor) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), 256.0F, 0xFFFFFF);

        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }
}
