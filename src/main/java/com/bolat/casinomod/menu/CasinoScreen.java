package com.bolat.casinomod.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CasinoScreen extends AbstractContainerScreen<CasinoMenu> {
    private static final int COLS = 5, ROWS = 6, PER_PAGE = COLS * ROWS, CELL = 20;
    private static final int PICK_X = 188, PICK_Y = 28; // relative to the window

    private EditBox search;
    private String lastSearch = "";
    private final List<Integer> filtered = new ArrayList<>();
    private ItemStack[] cache;
    private int page = 0;

    public CasinoScreen(CasinoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 300;
        this.imageHeight = 202;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        cache = new ItemStack[CasinoMenu.pool().size()];

        addRenderableWidget(Button.builder(Component.literal("PLAY"),
                        b -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CasinoMenu.BTN_PLAY))
                .bounds(leftPos + 58, topPos + 68, 60, 20).build());

        search = new EditBox(this.font, leftPos + 186, topPos + 6, 106, 14, Component.literal("Search"));
        search.setMaxLength(32);
        search.setValue(lastSearch);
        search.setResponder(s -> { lastSearch = s; refilter(); });
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (page > 0) page--; })
                .bounds(leftPos + 186, topPos + 152, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { if (page < pages() - 1) page++; })
                .bounds(leftPos + 268, topPos + 152, 24, 16).build());
        refilter();
    }

    private void refilter() {
        filtered.clear();
        String q = lastSearch.toLowerCase(Locale.ROOT).trim();
        List<Item> pool = CasinoMenu.pool();
        for (int i = 0; i < pool.size(); i++) {
            Item it = pool.get(i);
            if (q.isEmpty()
                    || ForgeRegistries.ITEMS.getKey(it).getPath().contains(q.replace(' ', '_'))
                    || it.getDescription().getString().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(i);
            }
        }
        page = 0;
    }

    private int pages() {
        return Math.max(1, (filtered.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private ItemStack stackFor(int poolIndex) {
        if (cache[poolIndex] == null) cache[poolIndex] = new ItemStack(CasinoMenu.pool().get(poolIndex));
        return cache[poolIndex];
    }

    /** pool index under the mouse or -1 */
    private int cellAt(double mx, double my) {
        double x = mx - leftPos - PICK_X, y = my - topPos - PICK_Y;
        if (x < 0 || y < 0 || x >= COLS * CELL || y >= ROWS * CELL) return -1;
        int n = page * PER_PAGE + (int) (y / CELL) * COLS + (int) (x / CELL);
        return n < filtered.size() ? filtered.get(n) : -1;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2B2B);
        g.fill(leftPos + 2, topPos + 2, leftPos + 178, topPos + imageHeight - 2, 0xFFC6C6C6);
        g.fill(leftPos + 182, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);

        for (Slot s : this.menu.slots) {
            g.fill(leftPos + s.x - 1, topPos + s.y - 1, leftPos + s.x + 17, topPos + s.y + 17, 0xFF373737);
            g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xFF8B8B8B);
        }
        g.drawCenteredString(this.font, ">>", leftPos + 88, topPos + 48, 0xFF3A3A3A);

        // chance bar (green = win zone)
        int chance = this.menu.getChance();
        g.fill(leftPos + 16, topPos + 26, leftPos + 160, topPos + 30, 0xFF8E2A2A);
        g.fill(leftPos + 16, topPos + 26, leftPos + 16 + 144 * chance / 100, topPos + 30, 0xFF2E9E2E);

        // item picker
        ItemStack chosen = this.menu.getSlot(CasinoMenu.TARGET).getItem();
        int start = page * PER_PAGE;
        for (int n = start; n < Math.min(filtered.size(), start + PER_PAGE); n++) {
            int k = n - start;
            int cx = leftPos + PICK_X + (k % COLS) * CELL;
            int cy = topPos + PICK_Y + (k / COLS) * CELL;
            ItemStack st = stackFor(filtered.get(n));
            boolean sel = !chosen.isEmpty() && chosen.getItem() == st.getItem();
            g.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, sel ? 0xFF2E9E2E : 0xFF8B8B8B);
            g.fill(cx + 1, cy + 1, cx + CELL - 2, cy + CELL - 2, 0xFF565656);
            g.renderItem(st, cx + 1, cy + 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "UPGRADER", 8, 6, 0x404040, false);
        g.drawCenteredString(this.font, "Chance: " + this.menu.getChance() + "%", 88, 14, 0xFF1E6B1E);
        g.drawCenteredString(this.font, "STAKE", 52, 33, 0xFF404040);
        g.drawCenteredString(this.font, "TARGET", 124, 33, 0xFF404040);

        String text;
        int color = 0xFF404040;
        switch (this.menu.getStatus()) {
            case CasinoMenu.ST_WON -> { text = "YOU WON!"; color = 0xFF1E6B1E; }
            case CasinoMenu.ST_LOST -> { text = "You lost. Try again"; color = 0xFFA01E1E; }
            case CasinoMenu.ST_NEED_STAKE -> { text = "Put a stake first"; color = 0xFFA01E1E; }
            case CasinoMenu.ST_NEED_TARGET -> { text = "Pick a target item ->"; color = 0xFFA01E1E; }
            default -> text = "Stake + target, then PLAY";
        }
        g.drawCenteredString(this.font, text, 88, 93, color);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
        g.drawCenteredString(this.font, (page + 1) + " / " + pages(), 239, 156, 0xFF404040);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int idx = cellAt(mouseX, mouseY);
        if (idx >= 0) {
            g.renderTooltip(this.font, stackFor(idx), mouseX, mouseY);
        } else {
            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int idx = cellAt(mx, my);
        if (idx >= 0 && button == 0) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, CasinoMenu.BTN_PICK_BASE + idx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx > leftPos + 182) {
            if (delta < 0 && page < pages() - 1) page++;
            else if (delta > 0 && page > 0) page--;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // typing "e" in the search box must not close the screen
        if (search != null && search.isFocused() && key != 256) {
            search.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
