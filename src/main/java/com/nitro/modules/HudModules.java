package com.nitro.modules;

import com.nitro.core.*;
import com.nitro.gui.HudEditor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class FpsHud extends HudMod {
    private final int[] hist = new int[36]; private long lastSample;
    FpsHud() { super("FPS", "Frame rate with a live graph", Category.HUD, 10, 10); }
    public void draw(DrawContext c, float td) {
        long now = System.currentTimeMillis();
        int fps = mc.getCurrentFps();
        if (now - lastSample > 120) { lastSample = now; System.arraycopy(hist, 1, hist, 0, hist.length - 1); hist[hist.length - 1] = fps; }
        w = 212; h = 28;
        Gfx.card(c, x, y, w, h);
        Gfx.circle(c, x + 15, y + 14, 6, Theme.accentA(0x50)); Gfx.ring(c, x + 15, y + 14, 6, 1, Theme.accent());
        int lx = x + 28;
        for (char ch : "NITRO".toCharArray()) { Gfx.text(c, "" + ch, lx, y + 10, 0xFFFFFFFF); lx += 7; }
        c.fill(lx + 2, y + 7, lx + 3, y + 21, 0x30FFFFFF);
        String f = "" + fps;
        Gfx.bold(c, f, lx + 10, y + 10, Theme.accent());
        int fx = lx + 10 + Gfx.tw(f) + 4;
        Gfx.text(c, "FPS", fx, y + 10, Theme.DIM);
        int gx = fx + Gfx.tw("FPS") + 8, gw = x + w - 8 - gx;
        int mn = Integer.MAX_VALUE, mx = 1; for (int v : hist) { if (v > 0) mn = Math.min(mn, v); mx = Math.max(mx, v); }
        if (mn == Integer.MAX_VALUE) mn = 0;
        int span = Math.max(1, mx - mn);
        for (int i = 1; i < hist.length; i++) {
            if (hist[i - 1] == 0) continue;
            int x1 = gx + (i - 1) * gw / (hist.length - 1), x2 = gx + i * gw / (hist.length - 1);
            int y1 = y + 22 - (hist[i - 1] - mn) * 14 / span, y2 = y + 22 - (hist[i] - mn) * 14 / span;
            Gfx.line(c, x1, y1, x2, y2, 1, Theme.accent());
        }
    }
}

final class CoordsHud extends HudMod {
    CoordsHud() { super("Coordinates", "Your XYZ position", Category.HUD, 6, 26); }
    public void draw(DrawContext c, float td) {
        chip(c, "XYZ  " + mc.player.getBlockX() + "  " + mc.player.getBlockY() + "  " + mc.player.getBlockZ());
    }
}

final class DirectionHud extends HudMod {
    DirectionHud() { super("Direction", "Facing direction and yaw", Category.HUD, 6, 46); }
    public void draw(DrawContext c, float td) {
        String f = mc.player.getHorizontalFacing().asString();
        f = Character.toUpperCase(f.charAt(0)) + f.substring(1);
        float yaw = ((mc.player.getYaw() % 360) + 540) % 360 - 180;
        chip(c, f + "  " + Math.round(yaw) + "°");
    }
}

final class PingHud extends HudMod {
    PingHud() { super("Ping", "Latency to the server", Category.HUD, 6, 66); }
    public void draw(DrawContext c, float td) {
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        chip(c, "PING  " + ping + " ms");
    }
}

final class TimeHud extends HudMod {
    final Setting.Bool seconds = add(new Setting.Bool("Show seconds", false));
    TimeHud() { super("Time", "Current real-world time", Category.HUD, 6, 86); }
    public void draw(DrawContext c, float td) {
        chip(c, LocalTime.now().format(DateTimeFormatter.ofPattern(seconds.v ? "HH:mm:ss" : "HH:mm")));
    }
}

final class ProfileHud extends HudMod {
    ProfileHud() { super("Profile", "Name, server and session time", Category.HUD, 6, 106); }
    public void draw(DrawContext c, float td) {
        long s = (System.currentTimeMillis() - State.SESSION) / 1000;
        String srv = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";
        String name = NickHider.apply(mc.player.getName().getString());
        chip(c, name + "  |  " + srv + "  |  " + (s / 60) + ":" + String.format("%02d", s % 60));
    }
}

final class CpsHud extends HudMod {
    CpsHud() { super("CPS", "Clicks per second", Category.HUD, 6, 126); }
    public void draw(DrawContext c, float td) { chip(c, "CPS  " + State.cps(true) + " | " + State.cps(false)); }
}

final class Keystrokes extends HudMod {
    Keystrokes() { super("Keystrokes", "WASD, mouse buttons and CPS", Category.HUD, 10, 200); }
    private void key(DrawContext c, int x, int y, int kw, int kh, String s, String s2, boolean d) {
        if (d) { Gfx.rgrad(c, x, y, kw, kh, Gfx.bright(), Gfx.deep()); Gfx.border(c, x, y, kw, kh, 0x60FFFFFF); }
        else { Gfx.rrect(c, x, y, kw, kh, 0x40201A30); Gfx.border(c, x, y, kw, kh, 0x30FFFFFF); }
        int col = d ? 0xFFFFFFFF : 0xFFC9C3E0;
        if (s2 == null) Gfx.ctext(c, s, x + kw / 2, y + (kh - 8) / 2, col);
        else { Gfx.ctext(c, s, x + kw / 2, y + 7, col); Gfx.ctext(c, s2, x + kw / 2, y + 19, d ? 0xFFE9DFFF : Theme.DIM); }
    }
    public void draw(DrawContext c, float td) {
        int k = 32, kh = 28, g = 4, pad = 8;
        w = 3 * k + 2 * g + pad * 2; h = pad * 2 + kh * 2 + 34 + g * 2;
        Gfx.card(c, x, y, w, h);
        int bx = x + pad, by = y + pad;
        key(c, bx + k + g, by, k, kh, "W", null, Keys.isDown(mc.options.forwardKey));
        key(c, bx, by + kh + g, k, kh, "A", null, Keys.isDown(mc.options.leftKey));
        key(c, bx + k + g, by + kh + g, k, kh, "S", null, Keys.isDown(mc.options.backKey));
        key(c, bx + 2 * (k + g), by + kh + g, k, kh, "D", null, Keys.isDown(mc.options.rightKey));
        int bw = (3 * k + 2 * g - g) / 2;
        key(c, bx, by + 2 * (kh + g), bw, 34, "LMB", State.cps(true) + " CPS", Keys.mouse(0));
        key(c, bx + bw + g, by + 2 * (kh + g), bw, 34, "RMB", State.cps(false) + " CPS", Keys.mouse(1));
    }
}

final class ArmorHud extends HudMod {
    ArmorHud() { super("Armor Status", "Armor and held item durability", Category.HUD, 6, 270); }
    public void draw(DrawContext c, float td) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        int count = 0;
        for (EquipmentSlot sl : slots) if (!mc.player.getEquippedStack(sl).isEmpty()) count++;
        if (count == 0 && !HudEditor.editing) { w = 0; h = 0; return; }
        w = 58; h = Math.max(24, count * 18 + 8);
        Gfx.glass(c, x, y, w, h);
        int yy = y + 4;
        for (EquipmentSlot sl : slots) {
            ItemStack s = mc.player.getEquippedStack(sl);
            if (s.isEmpty()) continue;
            c.drawItem(s, x + 4, yy);
            String t = s.isDamageable() ? (100 * (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage()) + "%" : (s.getCount() > 1 ? "x" + s.getCount() : "");
            Gfx.text(c, t, x + 24, yy + 4, Theme.TEXT);
            yy += 18;
        }
    }
}

final class PotionHud extends HudMod {
    PotionHud() { super("Potion Effects", "Active status effects", Category.HUD, 6, 360); }
    public void draw(DrawContext c, float td) {
        List<String> lines = new ArrayList<>();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String n = e.getEffectType().value().getName().getString();
            if (e.getAmplifier() > 0) n += " " + (e.getAmplifier() + 1);
            String t = e.isInfinite() ? "inf" : (e.getDuration() / 20 / 60) + ":" + String.format("%02d", (e.getDuration() / 20) % 60);
            lines.add(n + "  " + t);
        }
        if (lines.isEmpty()) { if (HudEditor.editing) lines.add("Speed II  1:30"); else { w = 0; h = 0; return; } }
        int mw = 0; for (String s : lines) mw = Math.max(mw, Gfx.tw(s));
        w = mw + 12; h = lines.size() * 11 + 6;
        Gfx.glass(c, x, y, w, h);
        int yy = y + 4; for (String s : lines) { Gfx.text(c, s, x + 6, yy, Theme.TEXT); yy += 11; }
    }
}

final class ActiveModulesHud extends HudMod {
    ActiveModulesHud() { super("Active Modules", "List of enabled modules", Category.HUD, 9999, 6); }
    public void draw(DrawContext c, float td) {
        List<String> names = new ArrayList<>();
        for (Mod m : ModuleManager.ALL) if (m.enabled && !m.isHud() && m.cat != Category.CLIENT) names.add(m.name);
        names.sort(Comparator.comparingInt((String s) -> -Gfx.tw(s)));
        if (names.isEmpty()) { if (HudEditor.editing) names.add("Example"); else { w = 0; h = 0; return; } }
        w = Gfx.tw(names.get(0)) + 14; h = names.size() * 12 + 4;
        int yy = y + 2;
        for (String s : names) {
            int tw = Gfx.tw(s);
            int xx = x + w - tw - 10;
            Gfx.rrect(c, xx - 2, yy, tw + 10, 12, Theme.bg());
            c.fill(xx + tw + 6, yy + 1, xx + tw + 8, yy + 11, Theme.accent());
            Gfx.text(c, s, xx + 2, yy + 2, Theme.TEXT);
            yy += 12;
        }
    }
}

final class Notifications extends HudMod {
    private record N(String title, String msg, String sub, long t, UUID id) {}
    static Notifications I;
    private final List<N> list = new ArrayList<>();

    Notifications() {
        super("Notifications", "Soft pop-up alerts, like Anti Vanish", Category.HUD, -1, 10);
        I = this;
        ModuleManager.toaster = (t, m) -> push(t, m);
    }

    static void push(String title, String msg) { push(title, msg, null, null); }
    static void push(String title, String msg, String sub) { push(title, msg, sub, null); }
    static void push(String title, String msg, String sub, UUID id) {
        if (I == null || !I.enabled) return;
        I.list.add(new N(title, msg, sub, System.currentTimeMillis(), id));
        if (I.list.size() > 5) I.list.remove(0);
    }

    public void draw(DrawContext c, float td) {
        long now = System.currentTimeMillis();
        list.removeIf(n -> now - n.t() > 4500);
        List<N> show = new ArrayList<>(list);
        if (show.isEmpty()) { if (HudEditor.editing) show.add(new N("VANISH DETECTED", "Notch", "Switched to spectator", now, null)); else { w = 0; h = 0; return; } }
        w = 190;
        if (x < 0) x = (int) ((c.getScaledWindowWidth() - w * scale) / 2);
        int yy = y;
        for (N n : show) {
            long age = now - n.t();
            int off = age < 250 ? -(int) ((1 - age / 250.0) * 24) : 0;
            int hh = n.sub() != null ? 40 : 32, yy2 = yy + off;
            int rr = hh / 2;
            Gfx.glow(c, x, yy2, w, hh, rr, Theme.accent(), 0x30);
            Gfx.rr(c, x, yy2, w, hh, rr, 0xEE1A1535);
            Gfx.rrGrad(c, x + 1, yy2 + 1, w - 2, hh - 2, rr - 1, Theme.accentA(0x30), Theme.accentA(0x08));
            Gfx.rrBorder(c, x, yy2, w, hh, rr, Theme.accentA(0x90));
            int ts = hh - 12;
            Gfx.rrGrad(c, x + 6, yy2 + 6, ts, ts, ts / 2, Gfx.bright(), Gfx.deep());
            if (n.id() != null) Compat.face(c, n.id(), x + 7, yy2 + 7, ts - 2);
            else Gfx.ctext(c, n.msg().substring(0, 1).toUpperCase(), x + 6 + ts / 2, yy2 + 6 + ts / 2 - 4, 0xFFFFFF);
            Icons.eye(c, x + 56, yy2 + 10, 0xFFCFC6F0);
            Gfx.text(c, n.title().toUpperCase(), x + 66, yy2 + 7, 0xFFCFC6F0);
            Gfx.bold(c, Gfx.fit(n.msg(), w - 66), x + 48, yy2 + 18, 0xFFFFFF);
            if (n.sub() != null) Gfx.text(c, Gfx.fit(n.sub(), w - 54), x + 48, yy2 + 30, Theme.DIM);
            yy += hh + 6;
        }
        h = Math.max(32, yy - y);
    }
}
