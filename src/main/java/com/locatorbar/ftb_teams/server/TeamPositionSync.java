package com.locatorbar.ftb_teams.server;

import com.locatorbar.ftb_teams.Config;
import com.locatorbar.ftb_teams.LocatorBarMod;
import com.locatorbar.ftb_teams.network.TeamPositionsPayload;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 服务端位置同步。
 * <p>
 * 每个同步周期内，对每名在线玩家调用 FTB Teams API 获取其“有效队伍”
 * （个人队伍或队伍频道），再把同队在线成员的位置发送给该玩家。
 */
@EventBusSubscriber(modid = LocatorBarMod.MODID)
public final class TeamPositionSync {
    private TeamPositionSync() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int interval = Config.SYNC_INTERVAL_TICKS.get();
        if (interval > 1 && server.getTickCount() % interval != 0) {
            return;
        }

        FTBTeamsAPI.API api = FTBTeamsAPI.api();
        TeamManager manager = null;
        if (api != null && api.isManagerLoaded()) {
            manager = api.getManager();
        }

        boolean onlySameTeam = Config.ONLY_SAME_TEAM.get();
        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();

        if (onlySameTeam && manager == null) {
            // FTB Teams 尚未完成加载：清空客户端旧数据，避免显示过期标记
            for (ServerPlayer viewer : onlinePlayers) {
                sendEmpty(viewer);
            }
            return;
        }

        double maxRange = Config.MAX_RANGE.get();
        double maxRangeSquared = maxRange * maxRange;

        for (ServerPlayer viewer : onlinePlayers) {
            Team viewerTeam = manager == null ? null : manager.getTeamForPlayer(viewer).orElse(null);

            if (onlySameTeam && viewerTeam == null) {
                sendEmpty(viewer);
                continue;
            }

            Collection<ServerPlayer> candidates = onlySameTeam
                    ? viewerTeam.getOnlineMembers()
                    : onlinePlayers;

            ResourceLocation dimension = viewer.serverLevel().dimension().location();
            List<TeamPositionsPayload.Entry> entries = new ArrayList<>();

            for (ServerPlayer member : candidates) {
                if (member == viewer) {
                    continue;
                }
                if (!member.serverLevel().dimension().equals(viewer.serverLevel().dimension())) {
                    continue;
                }
                if (member.distanceToSqr(viewer) > maxRangeSquared) {
                    continue;
                }
                entries.add(new TeamPositionsPayload.Entry(
                        member.getUUID(),
                        member.getGameProfile().getName(),
                        member.getX(),
                        member.getEyeY(),
                        member.getZ()
                ));
            }

            int teamColor = teamColor(viewerTeam);
            PacketDistributor.sendToPlayer(viewer,
                    new TeamPositionsPayload(dimension, (float) maxRange, teamColor, List.copyOf(entries)));
        }
    }

    private static void sendEmpty(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TeamPositionsPayload(
                player.serverLevel().dimension().location(),
                (float) Config.MAX_RANGE.get(),
                0xFFFFFF,
                List.of()
        ));
    }

    private static int teamColor(Team team) {
        if (team == null) {
            return 0xFFFFFF;
        }
        Color4I color = team.getProperty(TeamProperties.COLOR);
        return color == null ? 0xFFFFFF : color.rgb() & 0xFFFFFF;
    }
}
