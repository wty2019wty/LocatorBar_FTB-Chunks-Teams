package com.locatorbar.ftb_teams.client;

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
 * 中央代表玩家正前方，左右代表左右方向；超出 ±90° 的玩家会贴在边缘并降低透明度。
 */
public final class LocatorBarRenderer implements LayeredDraw.Layer {
    private static final int BAR_HEIGHT = 9;
    private static final int EDGE_PADDING = 6;
    private static final int MARKER_SIZE = 5;

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

        ClientTeamPositions.Snapshot snapshot = ClientTeamPositions.snapshot();
        if (snapshot.isEmpty()) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
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
        double configuredRange = ClientConfig.MAX_RANGE.get();
        // 0 = 不限制客户端距离，也不做距离淡出
        boolean unlimitedRange = configuredRange <= 0.0D;
        float range = unlimitedRange ? Float.MAX_VALUE : (float) configuredRange;

        List<Marker> markers = new ArrayList<>();
        for (ClientTeamPositions.TrackedPlayer tracked : snapshot.players()) {
            double dx = tracked.x() - playerX;
            double dy = tracked.y() - playerEyeY;
            double dz = tracked.z() - playerZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > range) {
                continue;
            }

            // Minecraft 的 yaw：0 = +Z，90 = -X。目标方位角使用同样的约定。
            float targetYaw = (float) (Math.atan2(-dx, dz) * (180.0D / Math.PI));
            float relativeAngle = Mth.wrapDegrees(targetYaw - viewYaw);
            float angle = smoothAngle(tracked.id(), relativeAngle);

            boolean behind = Math.abs(angle) > 90.0F;
            float clampedAngle = Mth.clamp(angle, -90.0F, 90.0F);
            int markerCenterX = centerX + Math.round(clampedAngle / 90.0F * maxOffset);

            // 相对高度：目标在上方时标记点上移，反之向下，最多偏移 2 像素
            int yShift = Mth.clamp((int) Math.round(-dy / 24.0D), -2, 2);
            int markerCenterY = top + BAR_HEIGHT / 2 + yShift;

            float fade = unlimitedRange ? 0.0F : Mth.clamp((float) (distance / range), 0.0F, 1.0F);
            int alpha = (int) (255.0F - fade * 150.0F);
            if (behind) {
                alpha = (int) (alpha * 0.55F);
            }
            if (tracked.longRange()) {
                // FTB Chunks 长距离位置是方块级精度，稍微降低不透明度作为提示
                alpha = (int) (alpha * 0.9F);
            }
            alpha = Mth.clamp(alpha, 55, 255);

            markers.add(new Marker(
                    tracked,
                    markerCenterX - MARKER_SIZE / 2,
                    markerCenterY - MARKER_SIZE / 2,
                    playerColor(tracked.id()),
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
        graphics.fill(left - 1, top - 1, left + barWidth + 1, bottom + 1, 0x44000000);
        graphics.fill(left, top, left + barWidth, bottom, 0x88000000);

        graphics.fill(left, top, left + barWidth, top + 1, withAlpha(accent, 115));
        graphics.fill(left, bottom - 1, left + barWidth, bottom, withAlpha(accent, 80));

        for (int i = 1; i <= 3; i++) {
            if (i == 2) {
                continue;
            }
            int tickX = left + barWidth * i / 4;
            graphics.fill(tickX, top + 2, tickX + 1, bottom - 2, withAlpha(accent, 55));
        }

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
     * 方向相近的玩家不会完全重叠在一起。
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

    private static float smoothAngle(UUID playerId, float targetAngle) {
        return SmoothAngles.update(playerId, targetAngle);
    }

    private static int playerColor(UUID playerId) {
        return PLAYER_COLORS[Math.floorMod(playerId.hashCode(), PLAYER_COLORS.length)];
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static final class Marker {
        private final ClientTeamPositions.TrackedPlayer entry;
        private int x;
        private final int y;
        private final int color;
        private final int alpha;

        private Marker(ClientTeamPositions.TrackedPlayer entry, int x, int y, int color, int alpha) {
            this.entry = entry;
            this.x = x;
            this.y = y;
            this.color = color;
            this.alpha = alpha;
        }
    }
}
