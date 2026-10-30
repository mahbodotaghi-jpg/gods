package com.nitro.core;

import net.minecraft.client.gui.DrawContext;

/** Tiny vector icons drawn from fills/lines (no textures needed). */
public final class Icons {
    private Icons() {}

    public static void cat(DrawContext c, Category cat, int cx, int cy, int col) {
        switch (cat) {
            case COMBAT -> { Gfx.line(c, cx - 5, cy - 5, cx + 5, cy + 5, 1, col); Gfx.line(c, cx + 5, cy - 5, cx - 5, cy + 5, 1, col);
                             Gfx.line(c, cx - 7, cy - 2, cx - 2, cy - 7, 1, col); Gfx.line(c, cx + 7, cy - 2, cx + 2, cy - 7, 1, col); }
            case VISUALS -> eye(c, cx, cy, col);
            case UNSEEN -> { Gfx.circle(c, cx, cy - 2, 4, col); c.fill(cx - 4, cy - 2, cx + 5, cy + 5, col); c.fill(cx - 2, cy + 3, cx - 1, cy + 6, 0xFF000000 | 0x0B0716); c.fill(cx + 1, cy + 3, cx + 2, cy + 6, 0xFF000000 | 0x0B0716); }
            case HUD -> { c.fill(cx - 5, cy - 5, cx - 1, cy - 1, col); c.fill(cx + 1, cy - 5, cx + 5, cy - 1, col);
                          c.fill(cx - 5, cy + 1, cx - 1, cy + 5, col); c.fill(cx + 1, cy + 1, cx + 5, cy + 5, col); }
            default -> { Gfx.ring(c, cx, cy, 5, 2, col); c.fill(cx - 1, cy - 8, cx + 1, cy - 5, col); c.fill(cx - 1, cy + 5, cx + 1, cy + 8, col);
                         c.fill(cx - 8, cy - 1, cx - 5, cy + 1, col); c.fill(cx + 5, cy - 1, cx + 8, cy + 1, col); }
        }
    }

    public static void eye(DrawContext c, int cx, int cy, int col) {
        Gfx.line(c, cx - 7, cy, cx - 3, cy - 3, 1, col); Gfx.line(c, cx - 3, cy - 3, cx + 3, cy - 3, 1, col); Gfx.line(c, cx + 3, cy - 3, cx + 7, cy, 1, col);
        Gfx.line(c, cx - 7, cy, cx - 3, cy + 3, 1, col); Gfx.line(c, cx - 3, cy + 3, cx + 3, cy + 3, 1, col); Gfx.line(c, cx + 3, cy + 3, cx + 7, cy, 1, col);
        Gfx.circle(c, cx, cy, 2, col);
    }

    public static void sliders(DrawContext c, int cx, int cy, int col) {
        c.fill(cx - 5, cy - 4, cx + 5, cy - 3, col); c.fill(cx - 5, cy, cx + 5, cy + 1, col); c.fill(cx - 5, cy + 4, cx + 5, cy + 5, col);
        c.fill(cx - 2, cy - 6, cx, cy - 1, col); c.fill(cx + 2, cy - 2, cx + 4, cy + 3, col); c.fill(cx - 3, cy + 2, cx - 1, cy + 7, col);
    }

    public static void search(DrawContext c, int cx, int cy, int col) {
        Gfx.ring(c, cx - 1, cy - 1, 4, 1, col); Gfx.line(c, cx + 2, cy + 2, cx + 5, cy + 5, 1, col);
    }

    public static void diamond(DrawContext c, int cx, int cy, int col) {
        Gfx.line(c, cx, cy - 4, cx + 4, cy, 1, col); Gfx.line(c, cx + 4, cy, cx, cy + 4, 1, col);
        Gfx.line(c, cx, cy + 4, cx - 4, cy, 1, col); Gfx.line(c, cx - 4, cy, cx, cy - 4, 1, col);
    }

    public static void shield(DrawContext c, int cx, int cy, int col) {
        c.fill(cx - 4, cy - 5, cx + 5, cy + 1, col);
        c.fill(cx - 3, cy + 1, cx + 4, cy + 3, col); c.fill(cx - 1, cy + 3, cx + 2, cy + 5, col);
    }

    // ------------------------------------------------------------------ per-module glyphs
    public static void target(DrawContext c, int cx, int cy, int col) {
        Gfx.ring(c, cx, cy, 5, 1, col); c.fill(cx, cy, cx + 1, cy + 1, col); c.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
        c.fill(cx, cy - 8, cx + 1, cy - 5, col); c.fill(cx, cy + 6, cx + 1, cy + 9, col);
        c.fill(cx - 8, cy, cx - 5, cy + 1, col); c.fill(cx + 6, cy, cx + 9, cy + 1, col);
    }
    public static void sparkle(DrawContext c, int cx, int cy, int col) {
        c.fill(cx, cy - 7, cx + 1, cy + 8, col); c.fill(cx - 7, cy, cx + 8, cy + 1, col);
        c.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
        c.fill(cx - 4, cy - 4, cx - 3, cy - 3, col); c.fill(cx + 4, cy - 4, cx + 5, cy - 3, col);
        c.fill(cx - 4, cy + 4, cx - 3, cy + 5, col); c.fill(cx + 4, cy + 4, cx + 5, cy + 5, col);
    }
    public static void heart(DrawContext c, int cx, int cy, int col) {
        Gfx.circle(c, cx - 3, cy - 2, 3, col); Gfx.circle(c, cx + 3, cy - 2, 3, col);
        for (int i = 0; i < 7; i++) c.fill(cx - 6 + i, cy + i - 1, cx + 7 - i, cy + i, col);
    }
    public static void cloud(DrawContext c, int cx, int cy, int col) {
        Gfx.circle(c, cx - 3, cy, 3, col); Gfx.circle(c, cx + 2, cy - 2, 4, col); c.fill(cx - 6, cy, cx + 7, cy + 4, col);
        c.fill(cx - 4, cy + 6, cx - 3, cy + 8, col); c.fill(cx, cy + 6, cx + 1, cy + 8, col); c.fill(cx + 4, cy + 6, cx + 5, cy + 8, col);
    }
    public static void clock(DrawContext c, int cx, int cy, int col) {
        Gfx.ring(c, cx, cy, 7, 1, col); c.fill(cx, cy - 4, cx + 1, cy + 1, col); c.fill(cx, cy, cx + 4, cy + 1, col);
    }
    public static void bolt(DrawContext c, int cx, int cy, int col) {
        Gfx.line(c, cx + 2, cy - 7, cx - 3, cy + 1, 1, col); Gfx.line(c, cx - 3, cy + 1, cx + 3, cy + 1, 1, col); Gfx.line(c, cx + 3, cy + 1, cx - 2, cy + 8, 1, col);
    }
    public static void waves(DrawContext c, int cx, int cy, int col) {
        Gfx.ring(c, cx, cy, 3, 1, col); Gfx.ring(c, cx, cy, 7, 1, col);
    }
    public static void drop(DrawContext c, int cx, int cy, int col) {
        Gfx.circle(c, cx, cy + 2, 4, col);
        for (int i = 0; i < 5; i++) c.fill(cx - i / 2, cy - 6 + i, cx + 1 + i / 2, cy - 5 + i, col);
    }
    public static void cube(DrawContext c, int cx, int cy, int col) {
        Gfx.outline(c, cx - 6, cy - 3, cx + 3, cy + 6, col); Gfx.outline(c, cx - 3, cy - 6, cx + 6, cy + 3, col);
        Gfx.line(c, cx - 6, cy - 3, cx - 3, cy - 6, 1, col); Gfx.line(c, cx + 3, cy - 3, cx + 6, cy - 6, 1, col); Gfx.line(c, cx + 3, cy + 6, cx + 6, cy + 3, 1, col);
    }
    public static void sun(DrawContext c, int cx, int cy, int col) {
        Gfx.circle(c, cx, cy, 3, col);
        c.fill(cx, cy - 8, cx + 1, cy - 5, col); c.fill(cx, cy + 6, cx + 1, cy + 9, col);
        c.fill(cx - 8, cy, cx - 5, cy + 1, col); c.fill(cx + 6, cy, cx + 9, cy + 1, col);
        c.fill(cx - 6, cy - 6, cx - 5, cy - 5, col); c.fill(cx + 6, cy - 6, cx + 7, cy - 5, col);
        c.fill(cx - 6, cy + 6, cx - 5, cy + 7, col); c.fill(cx + 6, cy + 6, cx + 7, cy + 7, col);
    }
    public static void bars(DrawContext c, int cx, int cy, int col) {
        c.fill(cx - 6, cy + 1, cx - 3, cy + 6, col); c.fill(cx - 2, cy - 3, cx + 1, cy + 6, col); c.fill(cx + 2, cy - 7, cx + 5, cy + 6, col);
    }
    public static void pin(DrawContext c, int cx, int cy, int col) {
        Gfx.ring(c, cx, cy - 2, 5, 2, col);
        for (int i = 0; i < 4; i++) c.fill(cx - 3 + i, cy + 2 + i, cx + 4 - i, cy + 3 + i, col);
    }
    public static void tag(DrawContext c, int cx, int cy, int col) {
        Gfx.outline(c, cx - 7, cy - 4, cx + 8, cy + 5, col); c.fill(cx - 4, cy - 1, cx + 5, cy, col); c.fill(cx - 4, cy + 2, cx + 1, cy + 3, col);
    }
    public static void grid(DrawContext c, int cx, int cy, int col) {
        c.fill(cx - 6, cy - 6, cx - 1, cy - 1, col); c.fill(cx + 1, cy - 6, cx + 6, cy - 1, col);
        c.fill(cx - 6, cy + 1, cx - 1, cy + 6, col); c.fill(cx + 1, cy + 1, cx + 6, cy + 6, col);
    }
    public static void radar(DrawContext c, int cx, int cy, int col) {
        Gfx.ring(c, cx, cy, 7, 1, col); Gfx.ring(c, cx, cy, 3, 1, col); Gfx.line(c, cx, cy, cx + 5, cy - 5, 1, col);
    }
    public static void tracer(DrawContext c, int cx, int cy, int col) {
        Gfx.line(c, cx - 6, cy + 6, cx + 4, cy - 4, 1, col); Gfx.circle(c, cx + 5, cy - 5, 2, col);
    }
    public static void cursor(DrawContext c, int cx, int cy, int col) {
        Gfx.line(c, cx - 4, cy - 6, cx - 4, cy + 4, 1, col); Gfx.line(c, cx - 4, cy - 6, cx + 4, cy + 1, 1, col);
        Gfx.line(c, cx - 4, cy + 4, cx - 1, cy + 1, 1, col); Gfx.line(c, cx - 1, cy + 1, cx + 4, cy + 1, 1, col);
    }

    /** Draws a glyph for the module; returns false when it has none (caller falls back to the first letter). */
    public static boolean mod(DrawContext c, Mod m, int cx, int cy, int col) {
        switch (m.name) {
            case "Target HUD", "Custom Crosshair" -> target(c, cx, cy, col);
            case "Radiance", "Ambience", "Skies" -> sparkle(c, cx, cy, col);
            case "Afterglow", "Staff List", "Totem Counter" -> shield(c, cx, cy, col);
            case "Cute World" -> heart(c, cx, cy, col);
            case "Weather" -> cloud(c, cx, cy, col);
            case "Time" -> clock(c, cx, cy, col);
            case "W-Tap", "Combo Counter" -> bolt(c, cx, cy, col);
            case "Ripple" -> waves(c, cx, cy, col);
            case "Hit Splash", "Low Fire" -> drop(c, cx, cy, col);
            case "Hitboxes", "Block Outline", "Storage ESP" -> cube(c, cx, cy, col);
            case "Fullbright" -> sun(c, cx, cy, col);
            case "FPS", "Active Modules" -> bars(c, cx, cy, col);
            case "Coordinates", "Direction", "Chunk Finder", "Base Finder" -> pin(c, cx, cy, col);
            case "Name Tags", "Nick Hider", "Profile" -> tag(c, cx, cy, col);
            case "Cyberspace", "HUD Editor", "Keystrokes", "Armor Status", "Potion Effects" -> grid(c, cx, cy, col);
            case "Radar", "Bokeh", "Ping" -> radar(c, cx, cy, col);
            case "Tracers" -> tracer(c, cx, cy, col);
            case "CPS" -> cursor(c, cx, cy, col);
            case "Zoom" -> search(c, cx, cy, col);
            case "Player ESP", "Anti Vanish", "Freecam" -> eye(c, cx, cy, col);
            case "Interface", "Notifications" -> sliders(c, cx, cy, col);
            default -> { return false; }
        }
        return true;
    }
}
