package com.yessmartypie.simplecraftingguide;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class CraftingGuideScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 190;

    public CraftingGuideScreen() {
        super(Text.translatable("screen.simple_crafting_guide.title"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        context.fill(left, top, right, bottom, 0xF0181B22);
        context.fill(left, top, right, top + 2, 0xFF55C2FF);
        context.fill(left + 1, top + 1, right - 1, bottom - 1, 0xE6222630);

        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.simple_crafting_guide.title"),
                left + 12, top + 12, 0xFFFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Recipe browser scaffold"),
                left + 12, top + 40, 0xFFB9C5D5
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Next: recipe search and ingredient grid"),
                left + 12, top + 60, 0xFF7F91A8
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Press ESC to close"),
                left + 12, bottom - 20, 0xFFB9C5D5
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
