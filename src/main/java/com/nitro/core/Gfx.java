package com.nitro.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;

/** 2D drawing helpers (everything is drawn through DrawContext.fill so it works on any 1.21.x). */
public final class Gfx {
    private Gfx() {}

    public static int tw(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        if ((col >>> 24) == 0) col |= 0xFF000000;
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, col, true);
    }

    // ------------------------------------------------------------ rounded shapes (fill-only, no extra APIs)
    private static int autoR(int w, int h) { return Math.max(1, Math.min(6, Math.min(w, h) / 3)); }

    /** Horizontal inset of row i (0 = outermost) of a corner with radius r. */
    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        int in = r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
        if (i == 0 && in == 0) in = 1;
        return in;
    }

    public static void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w < 3 || h < 3) { c.fill(x, y, x + w, y + h, col); return; }
        r = Math.min(r, Math.min(w, h) / 2);
        if (r < 1) { c.fill(x, y, x + w, y + h, col); return; }
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            c.fill(x + in, y + i, x + w - in, y + i + 1, col);
            c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, col);
        }
        if (h - 2 * r > 0) c.fill(x, y + r, x + w, y + h - r, col);
    }

    public static void rrBorder(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w < 3 || h < 3) { outline(c, x, y, x + w, y + h, col); return; }
        r = Math.min(r, Math.min(w, h) / 2);
        if (r < 1) { outline(c, x, y, x + w, y + h, col); return; }
        int prev = 0;
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            if (i == 0) {
                c.fill(x + in, y, x + w - in, y + 1, col);
                c.fill(x + in, y + h - 1, x + w - in, y + h, col);
            } else {
                int seg = Math.max(1, prev - in);
                c.fill(x + in, y + i, x + in + seg, y + i + 1, col);
                c.fill(x + w - in - seg, y + i, x + w - in, y + i + 1, col);
                c.fill(x + in, y + h - 1 - i, x + in + seg, y + h - i, col);
                c.fill(x + w - in - seg, y + h - 1 - i, x + w - in, y + h - i, col);
            }
            prev = in;
        }
        if (h - 2 * r > 0) { c.fill(x, y + r, x + 1, y + h - r, col); c.fill(x + w - 1, y + r, x + w, y + h - r, col); }
    }

    /** Vertical gradient inside a rounded rectangle (one fill per row). */
    public static void rrGrad(DrawContext c, int x, int y, int w, int h, int r, int top, int bot) {
        if (w < 3 || h < 3) { c.fillGradient(x, y, x + w, y + h, top, bot); return; }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = 0;
            if (r >= 1) { if (row < r) in = inset(r, row); else if (row >= h - r) in = inset(r, h - 1 - row); }
            c.fill(x + in, y + row, x + w - in, y + row + 1, lerpArgb(top, bot, row / (float) (h - 1)));
        }
    }

    /** Soft halo around a rounded rectangle. */
    public static void glow(DrawContext c, int x, int y, int w, int h, int r, int rgb, int a) {
        for (int i = 1; i <= 4; i++) rrBorder(c, x - i, y - i, w + 2 * i, h + 2 * i, r + i, alpha(rgb, Math.max(1, a / (i + 1))));
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int col) { rr(c, x, y, w, h, autoR(w, h), col); }

    /** "Glass" panel: translucent dark body, accent top edge. */
    public static void glass(DrawContext c, int x, int y, int w, int h) {
        int r = Math.min(7, Math.min(w, h) / 2);
        rr(c, x, y, w, h, r, Theme.bg());
        rrBorder(c, x, y, w, h, r, 0x30FFFFFF);
        c.fill(x + r, y, x + w - r, y + 1, Theme.accentA(0xA0));
        c.fill(x + r, y + 1, x + w - r, y + 2, 0x14FFFFFF);
    }

    public static void outline(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1, y1, x2, y1 + 1, col); c.fill(x1, y2 - 1, x2, y2, col);
        c.fill(x1, y1, x1 + 1, y2, col); c.fill(x2 - 1, y1, x2, y2, col);
    }

    public static void circle(DrawContext c, int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt(r * r - dy * dy);
            c.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, col);
        }
    }

    public static void ring(DrawContext c, int cx, int cy, int r, int t, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int oh = (int) Math.sqrt(r * r - dy * dy);
            int ir = r - t;
            if (Math.abs(dy) >= ir) { c.fill(cx - oh, cy + dy, cx + oh + 1, cy + dy + 1, col); continue; }
            int ih = (int) Math.sqrt(ir * ir - dy * dy);
            c.fill(cx - oh, cy + dy, cx - ih, cy + dy + 1, col);
            c.fill(cx + ih + 1, cy + dy, cx + oh + 1, cy + dy + 1, col);
        }
    }

    /** Line of squares, clipped to the screen (Liang-Barsky). */
    public static void line(DrawContext c, double x1, double y1, double x2, double y2, int w, int col) {
        double xmin = -8, ymin = -8, xmax = c.getScaledWindowWidth() + 8, ymax = c.getScaledWindowHeight() + 8;
        double dx = x2 - x1, dy = y2 - y1, t0 = 0, t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x1 - xmin, xmax - x1, y1 - ymin, ymax - y1};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) { if (q[i] < 0) return; }
            else {
                double t = q[i] / p[i];
                if (p[i] < 0) { if (t > t1) return; if (t > t0) t0 = t; }
                else { if (t < t0) return; if (t < t1) t1 = t; }
            }
        }
        double ax = x1 + t0 * dx, ay = y1 + t0 * dy, bx = x1 + t1 * dx, by = y1 + t1 * dy;
        int wi = Math.max(1, w);
        double len = Math.hypot(bx - ax, by - ay);
        if (Double.isNaN(len)) return;
        int n = (int) Math.min(2500, Math.max(1, Math.ceil(len / wi)));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            int px = (int) Math.round(ax + (bx - ax) * t - wi / 2.0);
            int py = (int) Math.round(ay + (by - ay) * t - wi / 2.0);
            c.fill(px, py, px + wi, py + wi, col);
        }
    }

    /** 3D wireframe box projected onto the HUD (visible through walls). */
    public static void wire(DrawContext c, Box b, int col, int w) {
        double[][] v = {
            {b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ},
            {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ}};
        int[][] e = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] ed : e) Proj.line3(c, v[ed[0]], v[ed[1]], col, w);
    }

    public static int alpha(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }
    public static int lerpColor(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static void border(DrawContext c, int x, int y, int w, int h, int col) { rrBorder(c, x, y, w, h, autoR(w, h), col); }

    public static void rgrad(DrawContext c, int x, int y, int w, int h, int top, int bot) { rrGrad(c, x, y, w, h, autoR(w, h), top, bot); }

    public static int lerpArgb(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int bright() { return lerpColor(Theme.accent(), 0xFFFFFFFF, 0.28f); }
    public static int deep() { return lerpColor(Theme.accent(), 0xFF000000, 0.40f); }

    public static void bold(DrawContext c, String s, int x, int y, int col) {
        MinecraftClient mc = MinecraftClient.getInstance();
        c.drawText(mc.textRenderer, s, x, y, col | 0xFF000000, false);
        c.drawText(mc.textRenderer, s, x + 1, y, col | 0xFF000000, false);
    }

    public static void ctext(DrawContext c, String s, int cx, int y, int col) { text(c, s, cx - tw(s) / 2, y, col); }

    public static String fit(String s, int maxW) {
        if (tw(s) <= maxW) return s;
        while (s.length() > 2 && tw(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    /** Glass card used by HUD elements: dark body + soft accent border. */
    public static void card(DrawContext c, int x, int y, int w, int h) {
        int r = Math.min(9, Math.min(w, h) / 2);
        rr(c, x, y, w, h, r, 0xE00C0814);
        rrBorder(c, x, y, w, h, r, Theme.accentA(0x55));
        c.fill(x + r, y + 1, x + w - r, y + 2, 0x14FFFFFF);
    }
}
