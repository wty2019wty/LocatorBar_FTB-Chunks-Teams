package com.locatorbar.ftb_teams.network;

import com.locatorbar.ftb_teams.LocatorBarMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务端 -> 客户端：同步当前维度中队友（或附近玩家，取决于配置）的位置。
 *
 * @param dimension 玩家所在维度，客户端只接受与当前维度一致的数据
 * @param range     服务端使用的最大显示距离，用于客户端做距离淡出
 * @param teamColor 队伍颜色（0xRRGGBB），用于定位条的点缀色
 * @param entries   其他玩家的位置列表
 */
public record TeamPositionsPayload(ResourceLocation dimension, float range, int teamColor, List<Entry> entries)
        implements CustomPacketPayload {
    public static final Type<TeamPositionsPayload> TYPE =
            new Type<>(LocatorBarMod.rl("team_positions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeamPositionsPayload> STREAM_CODEC =
            StreamCodec.of(TeamPositionsPayload::encode, TeamPositionsPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, TeamPositionsPayload payload) {
        buf.writeResourceLocation(payload.dimension);
        buf.writeFloat(payload.range);
        buf.writeInt(payload.teamColor);
        buf.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buf.writeUUID(entry.playerId);
            buf.writeUtf(entry.name, 32);
            buf.writeDouble(entry.x);
            buf.writeDouble(entry.y);
            buf.writeDouble(entry.z);
        }
    }

    private static TeamPositionsPayload decode(RegistryFriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        float range = buf.readFloat();
        int teamColor = buf.readInt();
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf.readUUID(), buf.readUtf(32), buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new TeamPositionsPayload(dimension, range, teamColor, List.copyOf(entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 单个玩家位置。y 使用眼睛高度，方便客户端计算相对高低。
     */
    public record Entry(UUID playerId, String name, double x, double y, double z) {
    }
}
