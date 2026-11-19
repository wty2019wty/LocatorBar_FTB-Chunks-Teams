package com.locatorbar.ftb_teams;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 主模组类。
 * <p>
 * 这是一个纯客户端模组：
 * <ul>
 *     <li>不注册任何网络包；</li>
 *     <li>不向服务端请求额外数据；</li>
 *     <li>只读取客户端已经同步的 FTB Teams 队伍数据、FTB Chunks 长距离玩家位置，
 *         以及原版客户端已经加载的玩家实体位置。</li>
 * </ul>
 * 服务端不需要安装本模组。
 */
@Mod(LocatorBarMod.MODID)
public class LocatorBarMod {
    public static final String MODID = "locatorbar_ftb_chunks_teams";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LocatorBarMod() {
        LOGGER.info("LocatorBar FTB Teams 客户端定位条已加载。");
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
