package com.nitro.gui;

import com.nitro.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Drag HUD elements around and resize them. Positions and sizes are saved to the config.
 *  - drag an element to move it (snaps to edges / centre)
 *  - drag the little square on its bottom-right corner, or scroll over it, to make it smaller / bigger
 *  - right-click an element to reset its size
 */
public final class HudEditor extends Screen {
    public static boolean editing;
    private static final float MIN = 0.5f, MAX = 3.0f;

    private HudMod drag, resize, hover;
    private int ox, oy;
    private boolean prevDown = true, prevR = true;

    public HudEditor() { super(Text.literal("HUD Editor")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() { editing = true; }

    private static float snap(float s) { return Math.max(MIN, Math.min(MAX, Math.round(s * 20f) / 20f)); }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        long win = this.client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rdown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean pressed = down && !prevDown, rpressed = rdown && !prevR;
        prevDown = down; prevR = rdown;

        g.fill(0, 0, width, height, 0x30000000);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x18FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x18FFFFFF);

        // what is under the cursor (topmost wins); the corner handle counts as part of the element
        HudMod hovered = null; boolean onHandle = false;
        if (drag == null && resize == null) {
            for (Mod m : ModuleManager.ALL) {
                if (!(m instanceof HudMod h) || !h.enabled) continue;
                int x2 = h.x + h.sw(), y2 = h.y + h.sh();
                if (mx >= h.x - 3 && mx < x2 + 4 && my >= h.y - 3 && my < y2 + 4) {
                    hovered = h;
                    onHandle = mx >= x2 - 5 && my >= y2 - 5;
                }
            }
            hover = hovered;
        } else {
            hovered = drag != null ? drag : resize;
            hover = hovered;
        }

        if (pressed && hovered != null && drag == null && resize == null) {
            if (onHandle) resize = hovered;
            else { drag = hovered; ox = mx - hovered.x; oy = my - hovered.y; }
        }
        if (rpressed && hovered != null) hovered.scale = 1f;
        if (!down) { drag = null; resize = null; }

        if (resize != null) {
            resize.scale = snap((mx - resize.x) / (float) Math.max(1, resize.w));
            resize.x = Math.max(0, Math.min(width - resize.sw(), resize.x));
            resize.y = Math.max(0, Math.min(height - resize.sh(), resize.y));
        }
        if (drag != null) {
            int dw = drag.sw(), dh = drag.sh();
            int nx = mx - ox, ny = my - oy;
            if (Math.abs(nx) < 6) nx = 0;
            if (Math.abs(nx + dw - width) < 6) nx = width - dw;
            if (Math.abs(nx + dw / 2 - width / 2) < 6) nx = width / 2 - dw / 2;
            if (Math.abs(ny) < 6) ny = 0;
            if (Math.abs(ny + dh - height) < 6) ny = height - dh;
            if (Math.abs(ny + dh / 2 - height / 2) < 6) ny = height / 2 - dh / 2;
            drag.x = Math.max(0, Math.min(width - dw, nx));
            drag.y = Math.max(0, Math.min(height - dh, ny));
        }

        for (Mod m : ModuleManager.ALL) {
            if (!(m instanceof HudMod h) || !h.enabled) continue;
            boolean act = h == drag || h == resize || h == hovered;
            int x1 = h.x - 2, y1 = h.y - 2, x2 = h.x + h.sw() + 2, y2 = h.y + h.sh() + 2;
            int col = act ? Theme.accent() : Theme.accentA(0x80);
            Gfx.outline(g, x1, y1, x2, y2, col);
            // resize handle (bottom-right)
            g.fill(x2 - 5, y2 - 5, x2 + 1, y2 + 1, act ? 0xFFFFFFFF : col);
            String label = h.name + (Math.abs(h.scale - 1f) > 0.01f || act ? "  " + Math.round(h.scale * 100) + "%" : "");
            Gfx.text(g, label, h.x, h.y - 12 < 0 ? y2 + 4 : h.y - 12, act ? Theme.TEXT : Theme.DIM);
        }
        String t = Lang.t("Drag to move  \u00B7  drag the corner square or scroll to resize  \u00B7  right-click to reset  \u00B7  Esc to go back");
        Gfx.text(g, t, (width - Gfx.tw(t)) / 2, height - 14, Theme.TEXT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hover != null && verticalAmount != 0) {
            hover.scale = snap(hover.scale + (verticalAmount > 0 ? 0.05f : -0.05f));
            hover.x = Math.max(0, Math.min(width - hover.sw(), hover.x));
            hover.y = Math.max(0, Math.min(height - hover.sh(), hover.y));
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        ModuleManager.save();
        this.client.setScreen(new ClickGui());
    }

    @Override
    public void removed() { editing = false; ModuleManager.save(); }
}
