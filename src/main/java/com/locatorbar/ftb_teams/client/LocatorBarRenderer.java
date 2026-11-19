package com.locatorbar.ftb_teams.client;

import com.locatorbar.ftb_teams.network.TeamPositionsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 定位条 HUD 渲染器。
 * <p>
 * 视觉风格参考原版 1.21.2+ 的玩家定位条：屏幕底部居中、横向排列、中央代表正前方，
 * 两侧代表左右方向；超出 ±90° 的队友会被贴在边缘并降低透明度。
 */
public final class LocatorBarRenderer implements LayeredDraw.Layer {
    private static final int BAR_HEIGHT = 9;
    private static final int EDGE_PADDING = 6;
    private static final int MARKER_SIZE = 5;

    /** 16 个区分度较高的玩家颜色，根据 UUID 稳定分配。 */
    private static final int[] PLAYER_COLORS = {
            0xE53935, 0xFB8C00, 0xFDD835, 0x7CB342,
            0x43A047, 0x00ACC1, 0x1E88E5, 0x3949AB,
            0x8E24AA, 0xD81B60, 0x6D4C41, 0x546E7A,
            0xEF5350, 0xFFA726, 0x9CCC65, 0x26C6DA
    };

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        ClientLocatorData.Snapshot snapshot = ClientLocatorData.snapshot(minecraft.level.dimension().location());
        if (snapshot.isEmpty()) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        // 防止配置的宽度超过当前 GUI 宽度
        int barWidth = Math.min(ClientConfig.BAR_WIDTH.get(), Math.max(80, screenWidth - 8));
        int left = (screenWidth - barWidth) / 2;
        int top = screenHeight - 44 + ClientConfig.Y_OFFSET.get();
        int bottom = top + BAR_HEIGHT;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float viewYaw = minecraft.player.getViewYRot(partialTick);
        double playerEyeY = minecraft.player.getEyeY();
        double playerX = minecraft.player.getX();
        double playerZ = minecraft.player.getZ();

        int centerX = left + barWidth / 2;
        int maxOffset = barWidth / 2 - EDGE_PADDING;
        float range = snapshot.range();

        List<Marker> markers = new ArrayList<>();
        for (TeamPositionsPayload.Entry entry : snapshot.entries()) {
            double dx = entry.x() - playerX;
            double dy = entry.y() - playerEyeY;
            double dz = entry.z() - playerZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > range) {
                continue;
            }

            // Minecraft 的 yaw：0 = +Z，90 = -X。目标方位角使用同样的约定。
            float targetYaw = (float) (Math.atan2(-dx, dz) * (180.0D / Math.PI));
            float relativeAngle = Mth.wrapDegrees(targetYaw - viewYaw);
            float angle = ClientLocatorData.smoothAngle(entry.playerId(), relativeAngle);

            boolean behind = Math.abs(angle) > 90.0F;
            float clampedAngle = Mth.clamp(angle, -90.0F, 90.0F);
            int markerCenterX = centerX + Math.round(clampedAngle / 90.0F * maxOffset);

            // 相对高度：目标在上方时标记点上移，反之向下，最多偏移 2 像素
            int yShift = Mth.clamp((int) Math.round(-(entry.y() - playerEyeY) / 24.0D), -2, 2);
            int markerCenterY = top + BAR_HEIGHT / 2 + yShift;

            float fade = Mth.clamp((float) (distance / range), 0.0F, 1.0F);
            int alpha = (int) (255.0F - fade * 150.0F);
            if (behind) {
                alpha = (int) (alpha * 0.55F);
            }
            alpha = Mth.clamp(alpha, 55, 255);

            markers.add(new Marker(
                    entry,
                    markerCenterX - MARKER_SIZE / 2,
                    markerCenterY - MARKER_SIZE / 2,
                    playerColor(entry.playerId()),
                    alpha
            ));
        }

        if (markers.isEmpty()) {
            return;
        }

        resolveOverlaps(markers, left + EDGE_PADDING, left + barWidth - EDGE_PADDING - MARKER_SIZE);

        float scale = ClientConfig.SCALE.get().floatValue();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (scale != 1.0F) {
            pose.translate(screenWidth / 2.0F, top + BAR_HEIGHT / 2.0F, 0.0F);
            pose.scale(scale, scale, 1.0F);
            pose.translate(-screenWidth / 2.0F, -(top + BAR_HEIGHT / 2.0F), 0.0F);
        }

        int accent = snapshot.teamColor() == 0 ? 0xFFFFFF : snapshot.teamColor();
        drawBar(graphics, left, top, bottom, barWidth, centerX, accent);

        for (Marker marker : markers) {
            drawMarker(graphics, marker);
        }

        if (ClientConfig.SHOW_NAMES.get()) {
            drawNames(graphics, minecraft, markers, top, screenWidth);
        }

        pose.popPose();
    }

    private static void drawBar(GuiGraphics graphics, int left, int top, int bottom, int barWidth, int centerX, int accent) {
        // 外阴影 + 半透明底板
        graphics.fill(left - 1, top - 1, left + barWidth + 1, bottom + 1, 0x44000000);
        graphics.fill(left, top, left + barWidth, bottom, 0x88000000);

        // 上下边缘使用队伍颜色点缀
        graphics.fill(left, top, left + barWidth, top + 1, withAlpha(accent, 115));
        graphics.fill(left, bottom - 1, left + barWidth, bottom, withAlpha(accent, 80));

        // 四分之一刻度
        for (int i = 1; i <= 3; i++) {
            if (i == 2) {
                continue;
            }
            int tickX = left + barWidth * i / 4;
            graphics.fill(tickX, top + 2, tickX + 1, bottom - 2, withAlpha(accent, 55));
        }

        // 中央准线
        graphics.fill(centerX, top + 2, centerX + 1, bottom - 2, withAlpha(accent, 140));
    }

    private static void drawMarker(GuiGraphics graphics, Marker marker) {
        int outlineAlpha = Math.max(80, marker.alpha);
        graphics.fill(marker.x - 1, marker.y - 1, marker.x + MARKER_SIZE + 1, marker.y + MARKER_SIZE + 1,
                withAlpha(0x000000, outlineAlpha));
        graphics.fill(marker.x, marker.y, marker.x + MARKER_SIZE, marker.y + MARKER_SIZE,
                withAlpha(marker.color, marker.alpha));
    }

    private static void drawNames(GuiGraphics graphics, Minecraft minecraft, List<Marker> markers, int top, int screenWidth) {
        for (Marker marker : markers) {
            String name = marker.entry.name();
            if (name.isBlank()) {
                continue;
            }
            int textWidth = minecraft.font.width(name);
            int textX = Mth.clamp(marker.x + MARKER_SIZE / 2 - textWidth / 2, 2, screenWidth - textWidth - 2);
            int textY = top - 11;
            graphics.drawString(minecraft.font, name, textX, textY,
                    withAlpha(0xFFFFFF, Math.min(255, marker.alpha + 25)), true);
        }
    }

    /**
     * 简单的水平避让：方向相近的队友不会完全重叠在一起。
     */
    private static void resolveOverlaps(List<Marker> markers, int minLeft, int maxLeft) {
        markers.sort(Comparator.comparingInt(marker -> marker.x));

        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);
            int lowerBound = i == 0 ? minLeft : markers.get(i - 1).x + MARKER_SIZE + 1;
            if (marker.x < lowerBound) {
                marker.x = lowerBound;
            }
        }

        int overflow = markers.get(markers.size() - 1).x - maxLeft;
        if (overflow > 0) {
            for (Marker marker : markers) {
                marker.x -= overflow;
            }
        }

        for (int i = markers.size() - 2; i >= 0; i--) {
            Marker marker = markers.get(i);
            Marker next = markers.get(i + 1);
            int upperBound = next.x - MARKER_SIZE - 1;
            if (marker.x > upperBound) {
                marker.x = upperBound;
            }
        }

        for (Marker marker : markers) {
            marker.x = Mth.clamp(marker.x, minLeft, maxLeft);
        }
    }

    private static int playerColor(UUID playerId) {
        return PLAYER_COLORS[Math.floorMod(playerId.hashCode(), PLAYER_COLORS.length)];
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static final class Marker {
        private final TeamPositionsPayload.Entry entry;
        private int x;
        private final int y;
        private final int color;
        private final int alpha;

        private Marker(TeamPositionsPayload.Entry entry, int x, int y, int color, int alpha) {
            this.entry = entry;
            this.x = x;
            this.y = y;
            this.color = color;
            this.alpha = alpha;
        }
    }
}
