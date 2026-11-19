package com.locatorbar.ftb_teams.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
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

    @Override
    public int getRowWidth() {
        return Math.min(420, this.width - 20);
    }

    public final class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
        private final PlayerFilter.Candidate candidate;
        private final Button toggleButton;

        private PlayerEntry(PlayerFilter.Candidate candidate) {
            this.candidate = candidate;
            this.toggleButton = Button.builder(label(), button -> {
                boolean hidden = PlayerFilterList.this.filter.isHidden(
                        this.candidate.uuid(), this.candidate.name());
                PlayerFilter.setHidden(this.candidate.uuid(), this.candidate.name(), !hidden);
                PlayerFilterList.this.filter = PlayerFilter.load();
                button.setMessage(label());
            }).bounds(0, 0, 70, 20).build();
        }

        private Component label() {
            boolean hidden = PlayerFilterList.this.filter.isHidden(
                    this.candidate.uuid(), this.candidate.name());
            return Component.translatable(hidden
                    ? "locatorbar_ftb_chunks_teams.filter.show"
                    : "locatorbar_ftb_chunks_teams.filter.hide");
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean hidden = PlayerFilterList.this.filter.isHidden(
                    this.candidate.uuid(), this.candidate.name());
            int textColor = hidden ? 0x888888 : 0xFFFFFF;

            String name = this.candidate.displayName();
            String trimmedName = PlayerFilterList.this.minecraft.font.plainSubstrByWidth(
                    name, Math.max(20, width - 92));
            graphics.drawString(PlayerFilterList.this.minecraft.font, trimmedName,
                    left + 4, top + 5, textColor, false);

            graphics.drawString(PlayerFilterList.this.minecraft.font, sourceText(this.candidate),
                    left + 4, top + 17, 0x777777, false);

            int buttonX = left + width - 74;
            int buttonY = top + (height - 20) / 2;
            this.toggleButton.setPosition(buttonX, buttonY);
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
            return List.of(this.toggleButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.toggleButton);
        }
    }
}
