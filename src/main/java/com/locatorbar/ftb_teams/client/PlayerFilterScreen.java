package com.locatorbar.ftb_teams.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 游戏内玩家过滤界面。
 * <p>
 * 列出当前可能出现在定位条上的玩家，逐个切换隐藏/显示；
 * 隐藏名单保存到客户端配置，登出或重启游戏后仍然有效。
 */
public final class PlayerFilterScreen extends Screen {
    private static final Component TITLE = Component.translatable("locatorbar_ftb_chunks_teams.filter.title");
    private static final Component HINT = Component.translatable("locatorbar_ftb_chunks_teams.filter.hint");

    @Nullable
    private final Screen parent;
    private PlayerFilterList list;

    public PlayerFilterScreen(@Nullable Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listTop = 44;
        int listHeight = Math.max(40, this.height - listTop - 44);
        this.list = new PlayerFilterList(this.minecraft, this.width, listHeight, listTop, 30);
        this.addRenderableWidget(this.list);

        int buttonY = this.height - 30;
        int buttonWidth = Math.min(150, Math.max(60, (this.width - 30) / 2));
        this.addRenderableWidget(Button.builder(
                Component.translatable("locatorbar_ftb_chunks_teams.filter.show_all"),
                button -> {
                    PlayerFilter.clear();
                    this.list.reload();
                }
        ).bounds(this.width / 2 - buttonWidth - 5, buttonY, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 + 5, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(this.font, HINT, this.width / 2, 28, 0xAAAAAA);

        if (this.list.children().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("locatorbar_ftb_chunks_teams.filter.empty"),
                    this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
