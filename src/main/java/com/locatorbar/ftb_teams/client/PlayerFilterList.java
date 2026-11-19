package com.locatorbar.ftb_teams.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 玩家过滤界面里的可滚动列表。
 */
public final class PlayerFilterList extends ContainerObjectSelectionList<PlayerFilterList.PlayerEntry> {
    private PlayerFilter.Filter filter = PlayerFilter.load();

    public PlayerFilterList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        reload();
    }

    /**
     * 重新读取过滤名单和候选玩家，用于“全部显示”或界面重建。
     */
    public void reload() {
        this.filter = PlayerFilter.load();
        this.clearEntries();
        for (PlayerFilter.Candidate candidate : PlayerFilter.candidates()) {
            this.addEntry(new PlayerEntry(candidate));
        }
        this.clampScrollAmount();
    }

    /**
     * 名单发生变化后刷新过滤缓存和所有按钮文字，不重建列表以保留滚动位置。
     */
    public void updateFilter() {
        this.filter = PlayerFilter.load();
        for (PlayerEntry entry : this.children()) {
            entry.refreshButton();
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(420, this.width - 20);
    }

    public final class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
        private final PlayerFilter.Candidate candidate;
        private final Button toggleButton;
        private final Button soloButton;

        private PlayerEntry(PlayerFilter.Candidate candidate) {
            this.candidate = candidate;
            this.toggleButton = Button.builder(label(), button -> {
                boolean hidden = PlayerFilterList.this.filter.isHidden(
                        this.candidate.uuid(), this.candidate.name());
                PlayerFilter.setHidden(this.candidate.uuid(), this.candidate.name(), !hidden);
                PlayerFilterList.this.updateFilter();
            }).bounds(0, 0, 60, 20).build();

            this.soloButton = Button.builder(
                    Component.translatable("locatorbar_ftb_chunks_teams.filter.solo"),
                    button -> {
                        PlayerFilter.showOnly(this.candidate);
                        PlayerFilterList.this.updateFilter();
                    })
                    .bounds(0, 0, 60, 20)
                    .tooltip(Tooltip.create(Component.translatable(
                            "locatorbar_ftb_chunks_teams.filter.solo.tooltip")))
                    .build();
            // 只对当前能被定位条追踪的玩家开放“独显”
            this.soloButton.active = candidate.tracked();
        }

        private Component label() {
            boolean hidden = PlayerFilterList.this.filter.isHidden(
                    this.candidate.uuid(), this.candidate.name());
            return Component.translatable(hidden
                    ? "locatorbar_ftb_chunks_teams.filter.show"
                    : "locatorbar_ftb_chunks_teams.filter.hide");
        }

        private void refreshButton() {
            this.toggleButton.setMessage(label());
            this.soloButton.active = this.candidate.tracked();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean hidden = PlayerFilterList.this.filter.isHidden(
                    this.candidate.uuid(), this.candidate.name());
            int textColor = hidden ? 0x888888 : 0xFFFFFF;

            String name = this.candidate.displayName();
            String trimmedName = PlayerFilterList.this.minecraft.font.plainSubstrByWidth(
                    name, Math.max(20, width - 136));
            graphics.drawString(PlayerFilterList.this.minecraft.font, trimmedName,
                    left + 4, top + 5, textColor, false);

            String source = PlayerFilterList.this.minecraft.font.plainSubstrByWidth(
                    sourceText(this.candidate), Math.max(20, width - 136));
            graphics.drawString(PlayerFilterList.this.minecraft.font, source,
                    left + 4, top + 17, 0x777777, false);

            int buttonY = top + (height - 20) / 2;
            this.soloButton.setPosition(left + width - 128, buttonY);
            this.soloButton.render(graphics, mouseX, mouseY, partialTick);

            this.toggleButton.setPosition(left + width - 64, buttonY);
            this.toggleButton.render(graphics, mouseX, mouseY, partialTick);
        }

        private String sourceText(PlayerFilter.Candidate candidate) {
            String key;
            if (!candidate.tracked()) {
                key = "locatorbar_ftb_chunks_teams.filter.source.config";
            } else if (candidate.longRange()) {
                key = "locatorbar_ftb_chunks_teams.filter.source.long_range";
            } else {
                key = "locatorbar_ftb_chunks_teams.filter.source.vanilla";
            }
            return Component.translatable(key).getString();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.toggleButton, this.soloButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.toggleButton, this.soloButton);
        }
    }
}
