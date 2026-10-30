package com.nitro.gui;

import com.nitro.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Nitro menu: icon tab bar, card grid, search, settings popup.
 * Immediate-mode; mouse and keys are polled through GLFW so it doesn't depend on Screen event signatures.
 */
public final class ClickGui extends Screen {
    public static long lastClosedMs;
    private static final int CH = 64, GAP = 12;
    /** Only real GLFW key codes (polling invalid ones makes GLFW log errors). */
    private static final int[] VALID;
    static {
        java.util.List<Integer> l = new java.util.ArrayList<>();
        int[][] ranges = {{32,32},{39,39},{44,57},{59,59},{61,61},{65,93},{96,96},{161,162},{256,269},{280,284},{290,314},{320,336},{340,348}};
        for (int[] r : ranges) for (int k = r[0]; k <= r[1]; k++) l.add(k);
        VALID = l.stream().mapToInt(Integer::intValue).toArray();
    }

    private DrawContext g;
    private int mx, my, px, py, pw, ph, hx0, hy0, hx1, hy1, cur, rx, rw;
    private int scroll, maxScroll, mscroll, mmax;
    private boolean down, pressed, rpressed, prevDown, prevR, prevMenu = true, dirty, focus;
    private Category cat = Category.COMBAT;
    private Mod binding, open;
    private String active, query = "";
    private final boolean[] kprev = new boolean[350];

    public ClickGui() { super(Text.literal("Nitro Client")); }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return binding == null && open == null && !focus; }

    private static String sub(Category c) {
        return switch (c) {
            case COMBAT -> "Precision and awareness in every fight";
            case VISUALS -> "See the world in a different light";
            case UNSEEN -> "To everyone else, you're just playing";
            case HUD -> "Everything you need, right where you look";
            default -> "Make Nitro yours";
        };
    }

    // ================================================================== frame
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // open animation: backdrop fades in, panel pops up from 92% and slides ~10px
        float t = Math.min(1f, (System.currentTimeMillis() - openedMs) / 300f);
        float e = 1f - (float) Math.pow(1f - t, 3);
        float sc = 0.92f + 0.08f * e, dy = (1f - e) * 10f;
        animating = t < 1f;
        ctx.fill(0, 0, width, height, (((int) (0xA0 * e)) << 24) | 0x050310);
        float cx = width / 2f, cy = height / 2f;
        var mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(cx, cy + dy);
        mat.scale(sc, sc);
        mat.translate(-cx, -cy);
        try {
            frame(ctx, (int) ((mouseX - cx) / sc + cx), (int) ((mouseY - cy - dy) / sc + cy));
        } finally {
            mat.popMatrix();
        }
    }

    private void frame(DrawContext ctx, int mouseX, int mouseY) {
        g = ctx; mx = mouseX; my = mouseY;
        hovNow = null;
        long win = this.client.getWindow().getHandle();
        down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        pressed = down && !prevDown; rpressed = r && !prevR; prevDown = down; prevR = r;
        if (animating) { pressed = false; rpressed = false; }

        boolean menu = GLFW.glfwGetKey(win, ModuleManager.menuKey) == GLFW.GLFW_PRESS;
        if (menu && !prevMenu && binding == null && !focus) { close(); return; }
        prevMenu = menu;
        keys(win);

        pw = Math.min(width - 40, 1060); ph = Math.min(height - 40, 600);
        px = (width - pw) / 2; py = (height - ph) / 2;

        // panel with purple glow
        Gfx.glow(g, px, py, pw, ph, 14, Theme.accent(), 0x30);
        Gfx.rr(g, px, py, pw, ph, 14, 0xF2090611);
        Gfx.rrGrad(g, px + 1, py + ph - 150, pw - 2, 149, 13, Theme.accentA(0x00), Theme.accentA(0x34));
        Gfx.rrGrad(g, px + 1, py + 1, pw - 2, 90, 13, Theme.accentA(0x1C), Theme.accentA(0x00));
        Gfx.rrBorder(g, px, py, pw, ph, 14, 0x3CFFFFFF);

        boolean modal = open != null;
        hx0 = 0; hy0 = 0; hx1 = width; hy1 = height;

        header(modal);

        // ---- title block
        String title = query.isEmpty() ? Lang.t(cat.label) : "Search";
        Compat.scaledText(g, title, px + 28, py + 58, 2.0f, 0xFFFFFFFF, true);
        Gfx.text(g, query.isEmpty() ? sub(cat) : "Results for \"" + query + "\"", px + 28, py + 82, Theme.DIM);

        List<Mod> list = filtered();
        int act = 0, tot = 0; for (Mod m : list) { if (m.isAction()) continue; tot++; if (m.enabled) act++; }
        String ac = act + " of " + tot + " active";
        Gfx.text(g, ac, px + pw - 28 - Gfx.tw(ac), py + 76, Theme.DIM);
        Gfx.circle(g, px + pw - 28 - Gfx.tw(ac) - 8, py + 80, 2, Theme.accent());

        // ---- card grid
        int gx0 = px + 28, gx1 = px + pw - 28, gy0 = py + 104, gy1 = py + ph - 34;
        hx0 = gx0 - 2; hy0 = gy0; hx1 = gx1 + 2; hy1 = gy1;
        int cw = gx1 - gx0;
        int cols = Math.max(1, Math.min(4, (cw + GAP) / (170 + GAP)));
        int cardW = (cw - GAP * (cols - 1)) / cols;
        int rows = (list.size() + cols - 1) / cols;
        int contentH = rows * (CH + GAP);
        maxScroll = Math.max(0, contentH - (gy1 - gy0));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        g.enableScissor(gx0 - 2, gy0, gx1 + 2, gy1);
        for (int i = 0; i < list.size(); i++) {
            int x = gx0 + (i % cols) * (cardW + GAP), y = gy0 - scroll + (i / cols) * (CH + GAP);
            if (y + CH < gy0 || y > gy1) continue;
            cardIdx = i;
            card(list.get(i), x, y, cardW, modal);
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(18, (gy1 - gy0) * (gy1 - gy0) / contentH);
            int barY = gy0 + (int) (((gy1 - gy0) - barH) * (scroll / (double) maxScroll));
            g.fill(px + pw - 14, gy0, px + pw - 12, gy1, 0x20FFFFFF);
            g.fill(px + pw - 14, barY, px + pw - 12, barY + barH, Theme.accent());
        }
        if (list.isEmpty()) Gfx.ctext(g, "Nothing here", px + pw / 2, py + ph / 2, Theme.DIM);

        Gfx.ctext(g, "Click to toggle  \u00B7  " + Lang.t("Right-click a module for settings") + "  \u00B7  Esc to close", px + pw / 2, py + ph - 22, 0xFF6F6890);

        if (modal) modal();
        lastHover = hovNow;
        if (dirty && !down) { ModuleManager.save(); dirty = false; }
    }

    // ---- menu sounds (soft amethyst chime, pentatonic by card index so sweeping across cards sounds musical)
    private static final float[] PENTA = {1.0f, 1.1225f, 1.26f, 1.4983f, 1.6818f};
    private void chime(float pitch, float vol) {
        if (!Theme.SOUNDS.v) return;
        try { Compat.chime(pitch, (float) Theme.SOUND_VOL.v * vol); } catch (Throwable ignored) {}
    }
    private void hoverSound() { chime(PENTA[cardIdx % 5] * (cardIdx % 10 >= 5 ? 1.18f : 1.0f), 0.55f); }

    // ================================================================== header
    private void header(boolean modal) {
        // wordmark
        Gfx.circle(g, px + 36, py + 30, 7, Theme.accentA(0x40));
        Gfx.ring(g, px + 36, py + 30, 7, 1, Theme.accent());
        Gfx.circle(g, px + 36, py + 30, 2, Theme.accent());
        int wx = px + 52; String w = "NITRO";
        for (int i = 0; i < w.length(); i++) { Gfx.bold(g, "" + w.charAt(i), wx, py + 26, 0xFFFFFF); wx += 10; }

        // tab bar (centered)
        Category[] cs = Category.values();
        int[] tw = new int[cs.length]; int total = 0;
        for (int i = 0; i < cs.length; i++) { tw[i] = cs[i] == cat ? 46 + Gfx.tw(Lang.t(cs[i].label)) : 30; total += tw[i] + 8; }
        int tx = px + (pw - total + 8) / 2, ty = py + 15;
        for (int i = 0; i < cs.length; i++) {
            int w2 = tw[i]; boolean sel = cs[i] == cat, h = !modal && inside(tx, ty, tx + w2, ty + 30);
            if (sel) { Gfx.glow(g, tx, ty, w2, 30, 15, Theme.accent(), 0x30); Gfx.rrGrad(g, tx, ty, w2, 30, 15, Gfx.bright(), Gfx.deep()); Gfx.rrBorder(g, tx, ty, w2, 30, 15, 0x50FFFFFF);
                       Icons.cat(g, cs[i], tx + 17, ty + 15, 0xFFFFFFFF); Gfx.bold(g, Lang.t(cs[i].label), tx + 32, ty + 11, 0xFFFFFF); }
            else { Gfx.rr(g, tx, ty, w2, 30, 15, h ? 0x44FFFFFF : 0x28FFFFFF); Gfx.rrBorder(g, tx, ty, w2, 30, 15, 0x20FFFFFF);
                   Icons.cat(g, cs[i], tx + 15, ty + 15, h ? 0xFFFFFFFF : 0xFFB9B2D6); }
            if (h && pressed && !sel) { cat = cs[i]; scroll = 0; query = ""; focus = false; active = null; chime(1.45f, 0.8f); }
            tx += w2 + 8;
        }

        // search
        int sw = 130, sx = px + pw - 28 - sw, sy = py + 18;
        boolean hs = !modal && inside(sx, sy, sx + sw, sy + 24);
        Gfx.rr(g, sx, sy, sw, 24, 12, 0x30FFFFFF); Gfx.rrBorder(g, sx, sy, sw, 24, 12, focus ? Theme.accent() : (hs ? 0x60FFFFFF : 0x28FFFFFF));
        Icons.search(g, sx + 14, sy + 12, 0xFFB9B2D6);
        String st = focus || !query.isEmpty() ? Gfx.fit(query + (focus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""), sw - 52) : "Search";
        Gfx.text(g, st, sx + 26, sy + 8, query.isEmpty() && !focus ? Theme.DIM : Theme.TEXT);
        Gfx.rrect(g, sx + sw - 22, sy + 5, 14, 14, 0x30FFFFFF); Gfx.text(g, "/", sx + sw - 18, sy + 8, Theme.DIM);
        if (hs && pressed) focus = true;
        else if (pressed && !hs && !modal) { if (focus && !inside(sx, sy, sx + sw, sy + 24)) focus = false; }
    }

    private List<Mod> filtered() {
        List<Mod> out = new ArrayList<>();
        String q = query.toLowerCase();
        for (Mod m : ModuleManager.ALL) {
            if (q.isEmpty()) { if (m.cat == cat) out.add(m); }
            else if (m.name.toLowerCase().contains(q) || m.desc.toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    // ================================================================== card
    private final long openedMs = System.currentTimeMillis();
    private boolean animating;
    private Mod lastHover, hovNow;
    private int cardIdx;

    private void tile(Mod m, int x, int y, int sz, boolean on) {
        if (on) { Gfx.glow(g, x, y, sz, sz, 8, Theme.accent(), 0x30); Gfx.rrGrad(g, x, y, sz, sz, 8, Gfx.bright(), Gfx.deep()); Gfx.rrBorder(g, x, y, sz, sz, 8, 0x40FFFFFF); }
        else { Gfx.rr(g, x, y, sz, sz, 8, 0x26FFFFFF); Gfx.rrBorder(g, x, y, sz, sz, 8, 0x28FFFFFF); }
        int col = on ? 0xFFFFFFFF : 0xFFB9B2D6;
        if (!Icons.mod(g, m, x + sz / 2, y + sz / 2, col)) Gfx.ctext(g, m.name.substring(0, 1), x + sz / 2, y + sz / 2 - 4, col);
    }

    /** Fully rounded switch with a round knob. */
    private void toggle(int x, int y, boolean on) {
        if (on) { Gfx.rrGrad(g, x, y, 30, 14, 7, Gfx.bright(), Gfx.deep()); Gfx.circle(g, x + 22, y + 7, 5, 0xFFFFFFFF); }
        else { Gfx.rr(g, x, y, 30, 14, 7, 0xFF2A2538); Gfx.rrBorder(g, x, y, 30, 14, 7, 0x30FFFFFF); Gfx.circle(g, x + 8, y + 7, 5, 0xFFB9B2D6); }
    }

    private void card(Mod m, int x, int y, int w, boolean modal) {
        boolean act = m.isAction();
        boolean h = !modal && hov(x, y, x + w, y + CH);
        if (h) { hovNow = m; if (m != lastHover && !animating) hoverSound(); }
        boolean on = m.enabled && !act;
        if (on) Gfx.rr(g, x, y, w, CH, 9, Theme.accentA(h ? 0x30 : 0x1C));
        Gfx.rr(g, x, y, w, CH, 9, on ? (h ? 0xB01C132E : 0x900F0818) : (h ? 0xD01C132E : 0xB0120B1F));
        Gfx.rrBorder(g, x, y, w, CH, 9, on ? Theme.accentA(h ? 0xC0 : 0x80) : (h ? Theme.accentA(0x90) : 0x30FFFFFF));

        tile(m, x + 10, y + 9, 26, on || (act && h));

        boolean hs = false;
        if (act) {
            // button card: a round "open" pill with an arrow instead of switch + sliders
            Gfx.rr(g, x + w - 40, y + 11, 30, 14, 7, h ? Theme.accentA(0xA0) : Theme.accentA(0x50));
            Gfx.rrBorder(g, x + w - 40, y + 11, 30, 14, 7, Theme.accentA(0x90));
            Gfx.line(g, x + w - 29, y + 14, x + w - 22, y + 18, 1, 0xFFFFFFFF);
            Gfx.line(g, x + w - 22, y + 18, x + w - 29, y + 22, 1, 0xFFFFFFFF);
        } else {
            int sx = x + w - 64, sy = y + 9;
            hs = !modal && hov(sx, sy, sx + 18, sy + 18);
            Gfx.rr(g, sx, sy, 18, 18, 6, hs ? 0x44FFFFFF : 0x20FFFFFF);
            Icons.sliders(g, sx + 9, sy + 9, hs ? 0xFFFFFFFF : 0xFFB9B2D6);
            toggle(x + w - 40, y + 11, m.enabled);
        }

        Gfx.bold(g, Gfx.fit(m.name, w - 24), x + 12, y + 38, 0xFFFFFF);
        Gfx.text(g, Gfx.fit(m.desc, w - 24), x + 12, y + 50, 0xFF8C85AA);

        if (m.key > 0 && !act) {
            String kn = Keys.name(m.key); int kw = Math.max(16, Gfx.tw(kn) + 8);
            int kx = x + w - 10 - kw, ky = y + CH - 24;
            Gfx.rr(g, kx, ky, kw, 14, 5, 0x30FFFFFF); Gfx.rrBorder(g, kx, ky, kw, 14, 5, 0x30FFFFFF);
            Gfx.text(g, kn, kx + 4, ky + 3, Theme.DIM);
        }

        if (h) {
            if (act) { if (pressed) { chime(1.7f, 1f); m.run(); } }
            else if (pressed) {
                if (hs) { open = m; mscroll = 0; chime(1.35f, 0.8f); }
                else { m.toggle(); dirty = true; chime(m.enabled ? 1.9f : 1.15f, 1f); }
            }
            else if (rpressed) { open = m; mscroll = 0; chime(1.35f, 0.8f); }
        }
    }

    // ================================================================== settings popup
    private void modal() {
        g.fill(px, py, px + pw, py + ph, 0xB8050310);
        int mw = Math.min(400, pw - 40), mh = Math.min(ph - 60, 470);
        int x = px + (pw - mw) / 2, y = py + (ph - mh) / 2;
        Gfx.glow(g, x, y, mw, mh, 12, Theme.accent(), 0x40);
        Gfx.rr(g, x, y, mw, mh, 12, 0xFA0E0919); Gfx.rrBorder(g, x, y, mw, mh, 12, Theme.accentA(0x80));
        if (pressed && !inside(x, y, x + mw, y + mh)) { open = null; return; }

        tile(open, x + 16, y + 14, 30, true);
        Gfx.bold(g, open.name, x + 56, y + 17, 0xFFFFFF);
        Gfx.text(g, Gfx.fit(open.desc, mw - 110), x + 56, y + 30, Theme.DIM);
        boolean hx = inside(x + mw - 32, y + 12, x + mw - 12, y + 32);
        Gfx.rr(g, x + mw - 32, y + 12, 20, 20, 7, hx ? 0x50FFFFFF : 0x28FFFFFF);
        Gfx.line(g, x + mw - 26, y + 18, x + mw - 18, y + 26, 1, 0xFFFFFFFF); Gfx.line(g, x + mw - 18, y + 18, x + mw - 26, y + 26, 1, 0xFFFFFFFF);
        if (hx && pressed) { open = null; return; }

        int by0 = y + 56, by1 = y + mh - 12;
        hx0 = x; hy0 = by0; hx1 = x + mw; hy1 = by1;
        rx = x + 16; rw = mw - 32;
        g.enableScissor(x + 4, by0, x + mw - 4, by1);
        cur = by0 - mscroll;

        // enabled + keybind
        int yy = cur; cur += 24;
        boolean he = hov(rx, yy, rx + rw, yy + 22);
        Gfx.rrect(g, rx, yy, rw, 22, he ? 0x30FFFFFF : 0x1CFFFFFF);
        Gfx.text(g, "Enabled", rx + 8, yy + 7, Theme.TEXT);
        pill(rx + rw - 38, yy + 4, open.enabled);
        if (he && pressed) { open.toggle(); dirty = true; }

        yy = cur; cur += 24;
        String bn = binding == open ? "press a key..." : Keys.name(open.key);
        boolean hk = hov(rx, yy, rx + rw, yy + 22);
        Gfx.rrect(g, rx, yy, rw, 22, hk || binding == open ? 0x30FFFFFF : 0x1CFFFFFF);
        Gfx.text(g, "Keybind", rx + 8, yy + 7, Theme.TEXT);
        Gfx.text(g, bn, rx + rw - 8 - Gfx.tw(bn), yy + 7, Theme.accent());
        if (hk && pressed) binding = open;

        for (Setting s : open.settings) setting(s);
        int content = cur - (by0 - mscroll);
        g.disableScissor();
        mmax = Math.max(0, content - (by1 - by0));
        mscroll = Math.max(0, Math.min(mmax, mscroll));
    }

    private void pill(int x, int y, boolean on) { toggle(x, y, on); }

    private void setting(Setting s) {
        int x = rx, w = rw, y = cur;
        if (s instanceof Setting.Bool b) {
            cur += 24;
            boolean h = hov(x, y, x + w, y + 22);
            Gfx.rrect(g, x, y, w, 22, h ? 0x30FFFFFF : 0x1CFFFFFF);
            Gfx.text(g, b.name, x + 8, y + 7, Theme.TEXT);
            pill(x + w - 38, y + 4, b.v);
            if (h && pressed) { b.v = !b.v; dirty = true; }
        } else if (s instanceof Setting.Num n) {
            cur += 24;
            n.v = slider("n" + System.identityHashCode(n), n.name, n.v, n.min, n.max, n.step, n.dec, y, x, w);
        } else if (s instanceof Setting.Mode m) {
            cur += 24;
            boolean h = hov(x, y, x + w, y + 22);
            Gfx.rrect(g, x, y, w, 22, h ? 0x30FFFFFF : 0x1CFFFFFF);
            Gfx.text(g, m.name, x + 8, y + 7, Theme.TEXT);
            String v = "< " + m.get() + " >";
            Gfx.text(g, v, x + w - 8 - Gfx.tw(v), y + 7, Theme.accent());
            if (h && pressed) { m.next(1); dirty = true; } else if (h && rpressed) { m.next(-1); dirty = true; }
        } else if (s instanceof Setting.Col c) {
            cur += 24;
            Gfx.rrect(g, x, y, w, 22, 0x1CFFFFFF);
            Gfx.text(g, c.name, x + 8, y + 7, Theme.TEXT);
            g.fill(x + w - 40, y + 5, x + w - 8, y + 17, 0xFF000000 | c.rgb()); Gfx.border(g, x + w - 41, y + 4, 34, 14, 0x60FFFFFF);
            int id = System.identityHashCode(c);
            c.r = (int) slider("r" + id, "R", c.r, 0, 255, 1, 0, cur, x + 10, w - 10); cur += 24;
            c.g = (int) slider("g" + id, "G", c.g, 0, 255, 1, 0, cur, x + 10, w - 10); cur += 24;
            c.b = (int) slider("b" + id, "B", c.b, 0, 255, 1, 0, cur, x + 10, w - 10); cur += 24;
        }
    }

    private double slider(String id, String label, double v, double min, double max, double step, int dec, int y, int x, int w) {
        boolean h = hov(x, y, x + w, y + 22);
        Gfx.rrect(g, x, y, w, 22, h ? 0x30FFFFFF : 0x1CFFFFFF);
        double t = Math.max(0, Math.min(1, (v - min) / (max - min)));
        int fx = x + (int) (w * t);
        Gfx.rgrad(g, x, y, Math.max(3, fx - x), 22, Theme.accentA(0x70), Theme.accentA(0x30));
        g.fill(Math.max(x, fx - 1), y, Math.min(x + w, fx + 1), y + 22, Theme.accent());
        Gfx.text(g, label, x + 8, y + 7, Theme.TEXT);
        String val = String.format("%." + dec + "f", v);
        Gfx.text(g, val, x + w - 8 - Gfx.tw(val), y + 7, Theme.TEXT);
        if (h && pressed) active = id;
        if (id.equals(active)) {
            if (down) {
                double nt = Math.max(0, Math.min(1, (mx - x) / (double) w));
                double nv = Math.round((min + nt * (max - min)) / step) * step;
                nv = Math.max(min, Math.min(max, Math.round(nv * 10000.0) / 10000.0));
                if (nv != v) { v = nv; dirty = true; }
            } else active = null;
        }
        return v;
    }

    // ================================================================== input
    private boolean inside(int x1, int y1, int x2, int y2) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    private boolean hov(int x1, int y1, int x2, int y2) { return inside(x1, y1, x2, y2) && mx >= hx0 && mx < hx1 && my >= hy0 && my < hy1; }

    private void keys(long win) {
        boolean[] now = new boolean[350];
        for (int k : VALID) now[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
        if (binding != null) {
            for (int k = 32; k < 349; k++) if (now[k] && !kprev[k]) {
                if (k == GLFW.GLFW_KEY_ESCAPE) binding = null;
                else { binding.key = (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) ? 0 : k; binding.keyWasDown = true; binding = null; dirty = true; }
                break;
            }
        } else {
            boolean esc = now[GLFW.GLFW_KEY_ESCAPE] && !kprev[GLFW.GLFW_KEY_ESCAPE];
            if (esc) { if (open != null) open = null; else if (focus) { focus = false; query = ""; } }
            else if (focus) {
                for (int k = 65; k <= 90; k++) if (now[k] && !kprev[k] && query.length() < 18) query += (char) (k + 32);
                for (int k = 48; k <= 57; k++) if (now[k] && !kprev[k] && query.length() < 18) query += (char) k;
                if (now[32] && !kprev[32] && query.length() < 18) query += " ";
                if (now[GLFW.GLFW_KEY_BACKSPACE] && !kprev[GLFW.GLFW_KEY_BACKSPACE] && !query.isEmpty()) query = query.substring(0, query.length() - 1);
                if (now[GLFW.GLFW_KEY_ENTER] && !kprev[GLFW.GLFW_KEY_ENTER]) focus = false;
            } else if (open == null && now[GLFW.GLFW_KEY_SLASH] && !kprev[GLFW.GLFW_KEY_SLASH]) focus = true;
        }
        System.arraycopy(now, 0, kprev, 0, 350);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (open != null) mscroll = Math.max(0, Math.min(mmax, mscroll - (int) (verticalAmount * 24)));
        else scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 30)));
        return true;
    }

    @Override
    public void removed() {
        ModuleManager.save();
        lastClosedMs = System.currentTimeMillis();
    }
}
