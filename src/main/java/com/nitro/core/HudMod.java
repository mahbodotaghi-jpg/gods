package com.nitro.core;

import net.minecraft.client.gui.DrawContext;

/** A module that draws a movable HUD element. */
public abstract class HudMod extends Mod {
    public int x, y, w = 60, h = 14;
    public float scale = 1f;
    public int sw() { return Math.max(1, Math.round(w * scale)); }
    public int sh() { return Math.max(1, Math.round(h * scale)); }

    protected HudMod(String name, String desc, Category cat, int x, int y) {
        super(name, desc, cat); this.x = x; this.y = y;
    }

    @Override public boolean isHud() { return true; }

    /** Draw at (x,y) and set w/h. */
    public abstract void draw(DrawContext c, float td);

    @Override
    public void onHud(DrawContext c, float td) {
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(scale, scale);
        m.translate((float) -x, (float) -y);
        try { draw(c, td); } finally { m.popMatrix(); }
        int scrW = c.getScaledWindowWidth(), scrH = c.getScaledWindowHeight();
        x = Math.max(0, Math.min(x, Math.max(0, scrW - sw())));
        y = Math.max(0, Math.min(y, Math.max(0, scrH - sh())));
    }

    protected void chip(DrawContext c, String s) {
        w = Gfx.tw(s) + 12; h = 16;
        Gfx.glass(c, x, y, w, h);
        Gfx.text(c, s, x + 6, y + 4, Theme.TEXT);
    }
}
