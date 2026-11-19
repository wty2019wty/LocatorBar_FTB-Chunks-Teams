package com.locatorbar.ftb_teams.network;

import com.locatorbar.ftb_teams.LocatorBarMod;
import com.locatorbar.ftb_teams.client.ClientLocatorData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包注册与客户端处理入口。
 * <p>
 * 这里不直接引用任何客户端专属类型的方法签名，因此专用服务器加载本类时不会出问题；
 * 真正的 {@link ClientLocatorData} 只会在客户端收到数据包时被类加载器解析。
 */
@EventBusSubscriber(modid = LocatorBarMod.MODID)
public final class LocatorBarNetwork {
    private LocatorBarNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TeamPositionsPayload.TYPE, TeamPositionsPayload.STREAM_CODEC, LocatorBarNetwork::handlePositions);
    }

    private static void handlePositions(TeamPositionsPayload payload, IPayloadContext context) {
        // 在客户端主线程更新缓存
        context.enqueueWork(() -> ClientLocatorData.accept(payload));
    }
}
