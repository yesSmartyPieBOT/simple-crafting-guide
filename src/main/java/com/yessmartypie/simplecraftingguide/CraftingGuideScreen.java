package com.yessmartypie.simplecraftingguide;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.context.ContextParameterMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CraftingGuideScreen extends Screen {
    private static final int VIEW_W = 960;
    private static final int VIEW_H = 540;

    private static final int BG = 0xEE05070B;
    private static final int PANEL = 0xE9121720;
    private static final int PANEL_ALT = 0xF018202B;
    private static final int CYAN = 0xFF00D2FF;
    private static final int CYAN_DIM = 0x8840BFD8;
    private static final int TEXT = 0xFFF0F7FF;
    private static final int MUTED = 0xFF8294A8;
    private static final int SLOT = 0xFF0A0E14;
    private static final int SLOT_BORDER = 0xFF263744;

    private final List<RecipeCard> recipes = new ArrayList<>();
    private final StringBuilder search = new StringBuilder();
    private List<RecipeCard> filtered = List.of();
    private int selectedIndex;
    private int listScroll;
    private int activeTab;
    private double scale = 1.0;
    private double offsetX;
    private double offsetY;
    private boolean searchFocused;

    public CraftingGuideScreen() {
        super(Text.translatable("screen.simple_crafting_guide.title"));
    }

    @Override
    protected void init() {
        super.init();
        refreshRecipes();
        calculateViewport();
    }

    private void calculateViewport() {
        double raw = Math.min(this.width / (double) VIEW_W, this.height / (double) VIEW_H);
        if (raw >= 1.0) {
            scale = Math.max(1.0, Math.floor(raw));
        } else {
            scale = Math.max(0.125, Math.floor(raw * 8.0) / 8.0);
        }
        offsetX = Math.floor((this.width - VIEW_W * scale) / 2.0);
        offsetY = Math.floor((this.height - VIEW_H * scale) / 2.0);
    }

    private double toVirtualX(double screenX) {
        return (screenX - offsetX) / scale;
    }

    private double toVirtualY(double screenY) {
        return (screenY - offsetY) / scale;
    }

    private void refreshRecipes() {
        recipes.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            filtered = List.of();
            return;
        }

        ContextParameterMap parameters = SlotDisplayContexts.createParameters(client.world);
        for (var collection : client.player.getRecipeBook().getOrderedResults()) {
            for (RecipeDisplayEntry entry : collection.getAllRecipes()) {
                var display = entry.display();
                List<SlotDisplay> ingredientDisplays;
                SlotDisplay resultDisplay;

                if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                    ingredientDisplays = shaped.ingredients();
                    resultDisplay = shaped.result();
                } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
                    ingredientDisplays = shapeless.ingredients();
                    resultDisplay = shapeless.result();
                } else {
                    continue;
                }

                ItemStack result = resultDisplay.getFirst(parameters);
                if (result.isEmpty()) {
                    continue;
                }

                List<ItemStack> ingredients = new ArrayList<>(9);
                for (SlotDisplay slot : ingredientDisplays) {
                    ingredients.add(slot.getFirst(parameters));
                }
                while (ingredients.size() < 9) {
                    ingredients.add(ItemStack.EMPTY);
                }

                String name = result.getName().getString();
                String id = entry.id().toString();
                recipes.add(new RecipeCard(entry, result.copy(), List.copyOf(ingredients), name, id));
            }
        }
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.toString().trim().toLowerCase(Locale.ROOT);
        filtered = recipes.stream()
                .filter(recipe -> needle.isEmpty()
                        || recipe.name().toLowerCase(Locale.ROOT).contains(needle)
                        || recipe.id().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        if (filtered.isEmpty()) {
            selectedIndex = 0;
            listScroll = 0;
        } else {
            selectedIndex = Math.max(0, Math.min(selectedIndex, filtered.size() - 1));
            listScroll = Math.max(0, Math.min(listScroll, Math.max(0, filtered.size() - 8)));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        calculateViewport();
        context.fill(0, 0, this.width, this.height, 0xB8000205);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float) offsetX, (float) offsetY);
        matrices.scale((float) scale, (float) scale);

        int mx = (int) Math.floor(toVirtualX(mouseX));
        int my = (int) Math.floor(toVirtualY(mouseY));
        drawDashboard(context, mx, my, delta);

        matrices.popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawDashboard(DrawContext ctx, int mx, int my, float delta) {
        // Outer frame and top chrome.
        panel(ctx, 12, 12, 936, 516, BG, CYAN);
        ctx.fill(24, 24, 936, 25, CYAN);
        ctx.fill(24, 25, 936, 26, 0x5533DFFF);

        ctx.drawTextWithShadow(textRenderer, "SIMPLE CRAFTING GUIDE", 30, 34, TEXT);
        ctx.drawTextWithShadow(textRenderer, "RECIPE INTELLIGENCE  /  CLIENT DATABASE", 30, 49, MUTED);
        ctx.drawTextWithShadow(textRenderer, "FABRIC 1.21.11", 822, 36, CYAN);
        ctx.drawTextWithShadow(textRenderer, "ESC  CLOSE", 852, 49, MUTED);

        // Column panels.
        panel(ctx, 24, 70, 238, 444, PANEL, 0xFF25404C);
        panel(ctx, 272, 70, 394, 444, PANEL, 0xFF25404C);
        panel(ctx, 676, 70, 260, 444, PANEL, 0xFF25404C);

        drawLibrary(ctx, mx, my);
        drawCraftingMatrix(ctx, mx, my, delta);
        drawDetails(ctx, mx, my);
    }

    private void drawLibrary(DrawContext ctx, int mx, int my) {
        header(ctx, "01  RECIPE LIBRARY", 38, 84);
        ctx.drawTextWithShadow(textRenderer, recipes.size() + " RECIPES SYNCED", 38, 101, MUTED);

        boolean searchHover = inside(mx, my, 36, 121, 214, 28);
        panel(ctx, 36, 121, 214, 28, searchFocused ? 0xFF111E29 : 0xFF090D13,
                searchFocused ? CYAN : (searchHover ? 0xFF4A7A8C : SLOT_BORDER));
        ctx.drawTextWithShadow(textRenderer, "⌕", 44, 130, CYAN);
        String shown = search.isEmpty() ? "Search recipes..." : search.toString();
        int shownColor = search.isEmpty() ? MUTED : TEXT;
        ctx.drawTextWithShadow(textRenderer, shown, 62, 131, shownColor);
        if (!search.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "x", 232, 131, CYAN);
        }

        drawTab(ctx, 36, 158, 102, "ALL RECIPES", activeTab == 0, mx, my);
        drawTab(ctx, 144, 158, 106, "CRAFTING", activeTab == 1, mx, my);

        ctx.fill(36, 190, 250, 191, CYAN_DIM);
        ctx.enableScissor(36, 195, 250, 491);
        int visible = 8;
        for (int row = 0; row < visible; row++) {
            int index = listScroll + row;
            if (index >= filtered.size()) break;
            RecipeCard card = filtered.get(index);
            int y = 198 + row * 36;
            boolean selected = index == selectedIndex;
            boolean hover = inside(mx, my, 36, y, 214, 32);
            if (selected) {
                ctx.fill(36, y, 250, y + 32, 0xCC103140);
                ctx.fill(36, y, 38, y + 32, CYAN);
            } else if (hover) {
                ctx.fill(36, y, 250, y + 32, 0xAA182632);
            }
            slot(ctx, 42, y + 5, 22, hover || selected);
            ctx.drawItem(card.result(), 45, y + 8);
            ctx.drawTextWithShadow(textRenderer, trim(card.name(), 22), 72, y + 7,
                    selected ? TEXT : 0xFFD5E1ED);
            ctx.drawTextWithShadow(textRenderer, "CRAFTING RECIPE", 72, y + 19, MUTED);
        }
        ctx.disableScissor();

        ctx.drawTextWithShadow(textRenderer,
                filtered.isEmpty() ? "No matching recipes" : (filtered.size() + " MATCHES"),
                38, 496, filtered.isEmpty() ? 0xFFFFA6A6 : MUTED);
    }

    private void drawCraftingMatrix(DrawContext ctx, int mx, int my, float delta) {
        header(ctx, "02  CRAFTING MATRIX", 286, 84);
        ctx.drawTextWithShadow(textRenderer, "3 x 3  /  RECIPE PREVIEW", 286, 101, MUTED);

        RecipeCard selected = getSelected();
        int gx = 306;
        int gy = 157;
        int size = 42;
        int gap = 5;
        for (int i = 0; i < 9; i++) {
            int x = gx + (i % 3) * (size + gap);
            int y = gy + (i / 3) * (size + gap);
            boolean hover = inside(mx, my, x, y, size, size);
            slot(ctx, x, y, size, hover);
            if (selected != null && i < selected.ingredients().size()) {
                ItemStack stack = selected.ingredients().get(i);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, x + 12, y + 12);
                }
            }
        }

        // Animated direction marker.
        int pulse = (int) ((System.currentTimeMillis() / 110L) % 3L);
        ctx.fill(452, 220, 476, 222, CYAN_DIM);
        ctx.fill(460, 213 + pulse, 468, 215 + pulse, CYAN);
        ctx.fill(468, 215 + pulse, 472, 217 + pulse, CYAN);
        ctx.fill(472, 217 + pulse, 476, 219 + pulse, CYAN);

        ctx.drawTextWithShadow(textRenderer, "OUTPUT", 498, 151, MUTED);
        slot(ctx, 506, 174, 54, true);
        if (selected != null) {
            ctx.drawItem(selected.result(), 525, 193);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, "—", 533, 194, MUTED);
        }

        ctx.fill(294, 326, 644, 327, CYAN_DIM);
        header(ctx, "RECIPE STATUS", 294, 341);
        if (selected == null) {
            ctx.drawTextWithShadow(textRenderer, "Select a recipe from the library.", 294, 365, MUTED);
        } else {
            ctx.drawTextWithShadow(textRenderer, trim(selected.name(), 38), 294, 365, TEXT);
            ctx.drawTextWithShadow(textRenderer, "Preview only — no items are consumed.", 294, 384, MUTED);
            ctx.drawTextWithShadow(textRenderer, "Select another output to inspect its recipe.", 294, 402, MUTED);
        }

        ctx.fill(294, 438, 644, 439, CYAN_DIM);
        ctx.drawTextWithShadow(textRenderer, "INPUT SLOTS", 294, 451, MUTED);
        int occupied = selected == null ? 0 : (int) selected.ingredients().stream().filter(s -> !s.isEmpty()).count();
        ctx.drawTextWithShadow(textRenderer, occupied + " / 9 ingredient positions", 294, 469, TEXT);
        ctx.drawTextWithShadow(textRenderer, "Scroll the library to browse more recipes.", 294, 491, MUTED);
    }

    private void drawDetails(DrawContext ctx, int mx, int my) {
        header(ctx, "03  RECIPE INTEL", 690, 84);
        RecipeCard selected = getSelected();
        if (selected == null) {
            ctx.drawTextWithShadow(textRenderer, "NO RECIPE SELECTED", 692, 113, MUTED);
            ctx.drawTextWithShadow(textRenderer, "Recipes sync from your", 692, 136, MUTED);
            ctx.drawTextWithShadow(textRenderer, "server recipe book.", 692, 150, MUTED);
            return;
        }

        slot(ctx, 692, 112, 44, true);
        ctx.drawItem(selected.result(), 706, 126);
        ctx.drawTextWithShadow(textRenderer, trim(selected.name(), 24), 744, 118, TEXT);
        ctx.drawTextWithShadow(textRenderer, "OUTPUT ITEM", 744, 134, CYAN);
        ctx.drawTextWithShadow(textRenderer, trim(selected.id(), 32), 692, 168, MUTED);

        ctx.fill(692, 190, 918, 191, CYAN_DIM);
        header(ctx, "INGREDIENTS", 692, 202);
        int row = 0;
        for (ItemStack ingredient : selected.ingredients()) {
            if (ingredient.isEmpty()) continue;
            int y = 226 + row * 24;
            if (y > 365) break;
            slot(ctx, 692, y, 22, inside(mx, my, 692, y, 22, 22));
            ctx.drawItem(ingredient, 695, y + 3);
            ctx.drawTextWithShadow(textRenderer, trim(ingredient.getName().getString(), 25), 721, y + 7, 0xFFD5E1ED);
            row++;
        }
        if (row == 0) {
            ctx.drawTextWithShadow(textRenderer, "Ingredient data unavailable", 692, 229, MUTED);
        }

        ctx.fill(692, 390, 918, 391, CYAN_DIM);
        header(ctx, "CRAFTING STATION", 692, 403);
        ctx.drawTextWithShadow(textRenderer, "Crafting Table / 3x3 grid", 692, 426, TEXT);
        ctx.drawTextWithShadow(textRenderer, "Recipe data is read-only.", 692, 449, MUTED);
        ctx.drawTextWithShadow(textRenderer, "Nothing is auto-crafted.", 692, 463, MUTED);
        ctx.drawTextWithShadow(textRenderer, "Use this as a quick reference.", 692, 477, MUTED);
    }

    private void drawTab(DrawContext ctx, int x, int y, int w, String label, boolean active, int mx, int my) {
        boolean hover = inside(mx, my, x, y, w, 24);
        panel(ctx, x, y, w, 24, active ? 0xCC103140 : (hover ? PANEL_ALT : 0xFF0A0E14),
                active ? CYAN : SLOT_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 8,
                active ? TEXT : MUTED);
    }

    private void header(DrawContext ctx, String label, int x, int y) {
        ctx.drawTextWithShadow(textRenderer, label, x, y, CYAN);
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h, int fill, int border) {
        ctx.fill(x, y, x + w, y + h, border);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void slot(DrawContext ctx, int x, int y, int size, boolean highlight) {
        int border = highlight ? CYAN : SLOT_BORDER;
        ctx.fill(x, y, x + size, y + size, border);
        ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, highlight ? 0xFF122733 : SLOT);
        if (highlight) {
            ctx.fill(x + 2, y + 2, x + size - 2, y + 3, 0x6640E7FF);
        }
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private RecipeCard getSelected() {
        return selectedIndex >= 0 && selectedIndex < filtered.size() ? filtered.get(selectedIndex) : null;
    }

    private void playClick() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double vx = toVirtualX(click.x());
        double vy = toVirtualY(click.y());
        int button = click.buttonInfo().button();

        if (button == 0) {
            if (inside((int) vx, (int) vy, 36, 121, 214, 28)) {
                searchFocused = true;
                playClick();
                return true;
            }
            if (searchFocused && inside((int) vx, (int) vy, 224, 121, 26, 28)) {
                search.setLength(0);
                applyFilter();
                playClick();
                return true;
            }
            searchFocused = false;
            if (inside((int) vx, (int) vy, 36, 158, 102, 24)) {
                activeTab = 0;
                playClick();
                return true;
            }
            if (inside((int) vx, (int) vy, 144, 158, 106, 24)) {
                activeTab = 1;
                playClick();
                return true;
            }
            if (inside((int) vx, (int) vy, 36, 198, 214, 288)) {
                int row = ((int) vy - 198) / 36;
                int index = listScroll + row;
                if (row >= 0 && row < 8 && index >= 0 && index < filtered.size()) {
                    selectedIndex = index;
                    playClick();
                    return true;
                }
            }
            if (inside((int) vx, (int) vy, 306, 157, 139, 136)) {
                playClick();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double vx = toVirtualX(mouseX);
        double vy = toVirtualY(mouseY);
        if (inside((int) vx, (int) vy, 36, 195, 214, 296)) {
            listScroll -= (int) Math.signum(verticalAmount) * 3;
            listScroll = Math.max(0, Math.min(listScroll, Math.max(0, filtered.size() - 8)));
            playClick();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEscape()) {
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
            close();
            return true;
        }
        if (searchFocused) {
            if (input.key() == 259) { // Backspace
                if (!search.isEmpty()) search.deleteCharAt(search.length() - 1);
                applyFilter();
                return true;
            }
            if (input.isSelectAll()) {
                // Keep this tiny search field dependency-free; Ctrl+A selects by clearing then typing.
                search.setLength(0);
                applyFilter();
                return true;
            }
            return true;
        }
        if (input.key() == 264 || input.key() == 265) { // Down / Up
            if (!filtered.isEmpty()) {
                selectedIndex = Math.max(0, Math.min(filtered.size() - 1, selectedIndex + (input.key() == 264 ? 1 : -1)));
                if (selectedIndex < listScroll) listScroll = selectedIndex;
                if (selectedIndex >= listScroll + 8) listScroll = selectedIndex - 7;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchFocused && !Character.isISOControl(input.codepoint()) && search.length() < 64) {
            search.appendCodePoint(input.codepoint());
            applyFilter();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record RecipeCard(RecipeDisplayEntry entry, ItemStack result, List<ItemStack> ingredients, String name, String id) {
    }
}
