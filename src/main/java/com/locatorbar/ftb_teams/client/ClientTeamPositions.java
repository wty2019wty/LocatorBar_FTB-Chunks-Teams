package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.LocatorBarMod;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.client.ClientTeamManager;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 纯客户端的位置收集器。
 * <p>
 * 数据来源（按优先级）：
 * <ol>
 *     <li>原版客户端已经加载的玩家实体 —— 位置实时且精确，但只在服务端实体追踪范围内；</li>
 *     <li>FTB Chunks 客户端长距离追踪数据 —— 可以覆盖更远距离，但坐标是方块级精度。</li>
 * </ol>
 * 队伍过滤使用 FTB Teams 客户端已经同步的 {@link ClientTeamManager}。
 */
public final class ClientTeamPositions {
    private static List<TrackedPlayer> players = List.of();
    private static List<TrackedPlayer> candidates = List.of();
    private static int teamColor = 0xFFFFFF;
    private static long lastDebugLog;

    private ClientTeamPositions() {
    }

    /**
     * 每个客户端 tick 调用一次。
     */
    public static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return;
        }

        FTBTeamsAPI.API api = FTBTeamsAPI.api();
        if (api == null || !api.isClientManagerLoaded()) {
            debug("FTB Teams 客户端队伍管理器尚未加载");
            clear();
            return;
        }

        ClientTeamManager manager = api.getClientManager();
        Team selfTeam = manager.selfTeam();
        UUID selfTeamId = selfTeam == null ? null : selfTeam.getTeamId();
        if (selfTeamId == null) {
            KnownClientPlayer self = manager.getKnownPlayer(minecraft.player.getUUID()).orElse(manager.self());
            selfTeamId = self == null ? null : self.teamId();
        }
        Set<UUID> teamMembers = selfTeam == null ? Set.of() : selfTeam.getMembers();
        int color = teamColor(selfTeam);

        boolean onlySameTeam = ClientConfig.ONLY_SAME_TEAM.get();
        double configuredRange = ClientConfig.MAX_RANGE.get();
        // 0 或负数表示客户端不限制距离；实际可见范围仍由客户端已同步的数据决定
        double maxRangeSquared = configuredRange <= 0.0D
                ? Double.MAX_VALUE
                : configuredRange * configuredRange;

        Map<UUID, TrackedPlayer> found = new LinkedHashMap<>();
        // 本 tick 的隐藏名单只解析一次，避免每个玩家都重复读取配置
        PlayerFilter.Filter hiddenFilter = PlayerFilter.load();

        // 1) 原版客户端实体：精度最高，优先使用
        for (AbstractClientPlayer other : minecraft.level.players()) {
            if (other == minecraft.player) {
                continue;
            }
            if (onlySameTeam && !isSameTeam(manager, other.getUUID(), selfTeamId, teamMembers)) {
                continue;
            }

            double dx = other.getX() - minecraft.player.getX();
            double dy = other.getEyeY() - minecraft.player.getEyeY();
            double dz = other.getZ() - minecraft.player.getZ();
            if (dx * dx + dy * dy + dz * dz > maxRangeSquared) {
                continue;
            }

            found.put(other.getUUID(), new TrackedPlayer(
                    other.getUUID(),
                    other.getGameProfile().getName(),
                    other.getX(),
                    other.getEyeY(),
                    other.getZ(),
                    false
            ));
        }

        // 2) FTB Chunks 长距离追踪：仅补充原版实体范围外的玩家
        int ftbChunksPositions = 0;
        if (ClientConfig.USE_FTB_CHUNKS.get()) {
            Map<UUID, Vec3> trackedPositions = FtbChunksIntegration.trackedPositions();
            ftbChunksPositions = trackedPositions.size();
            for (Map.Entry<UUID, Vec3> entry : trackedPositions.entrySet()) {
                UUID playerId = entry.getKey();
                if (playerId.equals(minecraft.player.getUUID()) || found.containsKey(playerId)) {
                    continue;
                }
                if (onlySameTeam && !isSameTeam(manager, playerId, selfTeamId, teamMembers)) {
                    continue;
                }

                Vec3 pos = entry.getValue();
                double approximateEyeY = pos.y + 1.0D;
                double dx = pos.x - minecraft.player.getX();
                double dy = approximateEyeY - minecraft.player.getEyeY();
                double dz = pos.z - minecraft.player.getZ();
                if (dx * dx + dy * dy + dz * dz > maxRangeSquared) {
                    continue;
                }

                String name = manager.getKnownPlayer(playerId)
                        .map(KnownClientPlayer::name)
                        .filter(playerName -> !playerName.isBlank())
                        .orElse("?");

                found.put(playerId, new TrackedPlayer(
                        playerId,
                        name,
                        pos.x,
                        approximateEyeY,
                        pos.z,
                        true
                ));
            }
        }

        List<TrackedPlayer> allCandidates = List.copyOf(found.values());
        candidates = allCandidates;
        players = allCandidates.stream()
                .filter(player -> !hiddenFilter.isHidden(player.id(), player.name()))
                .toList();
        teamColor = color;
        SmoothAngles.retain(found.keySet());

        debug("selfTeam={}, selfTeamId={}, teamMembers={}, levelPlayers={}, ftbChunksPositions={}, maxRange={}, candidates={}, hidden={}, visible={}",
                selfTeam == null ? "none" : selfTeam.getId(),
                selfTeamId,
                teamMembers.size(),
                minecraft.level.players().size(),
                ftbChunksPositions,
                configuredRange <= 0.0D ? "unlimited" : configuredRange,
                allCandidates.size(),
                allCandidates.size() - players.size(),
                players.size());
    }

    private static void debug(String format, Object... args) {
        if (!ClientConfig.DEBUG.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDebugLog < 5000L) {
            return;
        }
        lastDebugLog = now;
        LocatorBarMod.LOGGER.info("[LocatorBar] " + format, args);
    }

    public static void clear() {
        players = List.of();
        candidates = List.of();
        teamColor = 0xFFFFFF;
        SmoothAngles.clear();
    }

    public static Snapshot snapshot() {
        return new Snapshot(players, teamColor);
    }

    /**
     * 当前所有候选玩家，包含被过滤名单隐藏的玩家。
     * 过滤界面需要它才能把隐藏项重新显示出来。
     */
    public static List<TrackedPlayer> candidates() {
        return candidates;
    }

    private static boolean isSameTeam(ClientTeamManager manager, UUID playerId, UUID selfTeamId, Set<UUID> teamMembers) {
        if (teamMembers.contains(playerId)) {
            return true;
        }
        if (selfTeamId == null) {
            return false;
        }
        return manager.getKnownPlayer(playerId)
                .map(known -> selfTeamId.equals(known.teamId()))
                .orElse(false);
    }

    private static int teamColor(Team team) {
        if (team == null) {
            return 0xFFFFFF;
        }
        Color4I color = team.getProperty(TeamProperties.COLOR);
        return color == null ? 0xFFFFFF : color.rgb() & 0xFFFFFF;
    }

    /**
     * @param longRange true 表示该位置来自 FTB Chunks 长距离追踪，精度为方块级
     */
    public record TrackedPlayer(UUID id, String name, double x, double y, double z, boolean longRange) {
    }

    public record Snapshot(List<TrackedPlayer> players, int teamColor) {
        public boolean isEmpty() {
            return players.isEmpty();
        }
    }
}
