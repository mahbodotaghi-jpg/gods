package com.nitro.modules;

import com.nitro.core.*;
import com.nitro.gui.HudEditor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// ======================================================================= Target HUD
final class TargetHud extends HudMod {
    final Setting.Bool auto = add(new Setting.Bool("Auto PvP mode", true));
    final Setting.Bool showDist = add(new Setting.Bool("Show distance", true));
    final Setting.Bool showStats = add(new Setting.Bool("Show pops / totem / armor", true));
    private float shown = -1, prevHp, lastDelta; private long deltaTime; private LivingEntity last;

    TargetHud() { super("Target HUD", "Health, armour, pops and combos of your target", Category.COMBAT, 300, 190); }

    private LivingEntity pick() {
        long now = System.currentTimeMillis();
        if (State.target != null && now - State.targetTime < 6000) return State.target;
        if (mc.targetedEntity instanceof LivingEntity l) return l;
        return null;
    }

    static String mode(PlayerEntity p, LivingEntity t) {
        ItemStack m = p.getMainHandStack();
        if (m.isOf(Items.MACE)) return "MACE";
        if (m.isIn(ItemTags.AXES)) return "AXE";
        if (m.isIn(ItemTags.SWORDS)) return "SWORD";
        if (m.isOf(Items.END_CRYSTAL)) return "CRYSTAL";
        if (m.isOf(Items.BOW)) return "BOW";
        if (m.isOf(Items.CROSSBOW)) return "CROSSBOW";
        if (m.isOf(Items.TRIDENT)) return "TRIDENT";
        if (m.isOf(Items.SPLASH_POTION) || m.isOf(Items.LINGERING_POTION)) return "POTION";
        if (p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) || t.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return "ELYTRA";
        return "FIST";
    }

    private static float dur(ItemStack s) { return s.isEmpty() ? 0 : (s.isDamageable() ? 1f - s.getDamage() / (float) s.getMaxDamage() : 1f); }

    private void chip(DrawContext c, int x, int y, int w, String txt, int col, boolean bad) {
        Gfx.rrect(c, x, y, w, 18, 0x30FFFFFF); Gfx.border(c, x, y, w, 18, 0x28FFFFFF);
        Gfx.ring(c, x + 9, y + 9, 4, 1, col);
        Gfx.text(c, Gfx.fit(txt, w - 22), x + 17, y + 5, col);
    }

    public void draw(DrawContext c, float td) {
        LivingEntity t = pick();
        String name; float hp, max, abs; boolean dead; double dist; String md; int pops; boolean totem; float[] segs = new float[4];
        long now = System.currentTimeMillis();
        if (t == null) {
            if (!HudEditor.editing) { w = 0; h = 0; return; }
            name = "kairo"; hp = 1.0f; max = 20; abs = 0.5f; dead = false; dist = 3.9; md = "CRYSTAL"; pops = 1; totem = false;
            segs = new float[]{0.6f, 0.3f, 0.45f, 0.7f}; lastDelta = 7.5f; deltaTime = now;
        } else {
            name = NickHider.apply(t.getName().getString()); hp = t.getHealth(); max = Math.max(1, t.getMaxHealth());
            abs = t.getAbsorptionAmount(); dead = t.isDead() || hp <= 0; dist = mc.player.distanceTo(t);
            md = auto.v ? mode(mc.player, t) : ""; pops = State.pops.getOrDefault(t.getUuid(), 0);
            totem = t.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) || t.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
            EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) segs[i] = dur(t.getEquippedStack(sl[i]));
            if (t != last) { last = t; prevHp = hp; lastDelta = 0; shown = hp; }
            else if (hp < prevHp - 0.01f) { lastDelta = prevHp - hp; deltaTime = now; }
            prevHp = hp;
        }
        if (shown < 0) shown = hp;
        shown += (hp - shown) * 0.18f;

        w = 250; h = 90;
        Gfx.card(c, x, y, w, h);
        // head tile with corner brackets
        int hx = x + 10, hy = y + 10, hs = 38;
        Gfx.rrect(c, hx, hy, hs, hs, 0xFF1D1530);
        if (t instanceof PlayerEntity) Compat.face(c, t.getUuid(), hx + 2, hy + 2, hs - 4);
        else Compat.scaledText(c, name.substring(0, 1).toUpperCase(), hx + hs / 2f - 6, hy + hs / 2f - 8, 2f, 0xFFD9C8FF, true);
        int a = Theme.accent(), L = 7;
        c.fill(hx - 2, hy - 2, hx + L, hy - 1, a); c.fill(hx - 2, hy - 2, hx - 1, hy + L, a);
        c.fill(hx + hs - L, hy - 2, hx + hs + 2, hy - 1, a); c.fill(hx + hs + 1, hy - 2, hx + hs + 2, hy + L, a);
        c.fill(hx - 2, hy + hs + 1, hx + L, hy + hs + 2, a); c.fill(hx - 2, hy + hs - L, hx - 1, hy + hs + 2, a);
        c.fill(hx + hs - L, hy + hs + 1, hx + hs + 2, hy + hs + 2, a); c.fill(hx + hs + 1, hy + hs - L, hx + hs + 2, hy + hs + 2, a);

        Gfx.bold(c, Gfx.fit(name, 96), x + 58, y + 11, dead ? 0x9A93B8 : 0xFFFFFF);
        // mode pill
        int pw = md.isEmpty() ? 0 : Gfx.tw(md) + 28;
        if (pw > 0) {
            int pxx = x + w - 10 - pw;
            Gfx.rgrad(c, pxx, y + 9, pw, 18, 0xFF3A1A66, 0xFF2A1250); Gfx.border(c, pxx, y + 9, pw, 18, Theme.accentA(0xB0));
            Icons.diamond(c, pxx + 10, y + 18, 0xFFFFFFFF);
            Gfx.bold(c, md, pxx + 20, y + 14, 0xFFFFFF);
        }
        // damage taken
        if (now - deltaTime < 2200 && lastDelta > 0) {
            String d = "-" + String.format("%.1f", lastDelta);
            Compat.scaledText(c, d, x + w - 16 - pw - Gfx.tw(d) * 1.3f, y + 12, 1.3f, 0xFF9B3A55, true);
        }
        // hp
        if (dead) Gfx.bold(c, "ELIMINATED", x + 58, y + 30, Theme.accent());
        else {
            String hs2 = String.format("%.1f", hp);
            Compat.scaledText(c, "\u2665", x + 58, y + 29, 1.4f, 0xFFFF5FA8, true);
            Compat.scaledText(c, hs2, x + 74, y + 28, 1.6f, 0xFFFF6FB0, true);
            if (abs > 0) Gfx.text(c, "+" + String.format("%.1f", abs), x + 76 + (int) (Gfx.tw(hs2) * 1.6f), y + 33, 0xFFFFD27A);
        }
        if (showDist.v) { String ds = String.format("%.1f m", dist); Compat.scaledText(c, ds, x + w - 12 - Gfx.tw(ds) * 1.2f, y + 31, 1.2f, 0xFFB9B2D6, true); }

        // health bar + armor segments
        int bx = x + 10, bw = w - 20;
        c.fill(bx, y + 52, bx + bw, y + 58, 0x40FFFFFF);
        float ratio = Math.max(0, Math.min(1, shown / max));
        c.fillGradient(bx, y + 52, bx + Math.max(2, (int) (bw * ratio)), y + 58, 0xFFFF8CC0, 0xFFFF4E94);
        if (abs > 0) c.fill(bx, y + 58, bx + (int) (bw * Math.min(1, abs / max)), y + 60, 0xFFFFD27A);
        int sg = (bw - 9) / 4;
        for (int i = 0; i < 4; i++) {
            int sx = bx + i * (sg + 3);
            c.fill(sx, y + 62, sx + sg, y + 64, 0x40FFFFFF);
            c.fill(sx, y + 62, sx + (int) (sg * segs[i]), y + 64, 0xFFFFD27A);
        }
        if (showStats.v) {
            int cw = (bw - 8) / 3, cy = y + 68;
            chip(c, bx, cy, cw, pops + (pops == 1 ? " pop" : " pops"), 0xFFFFD27A, false);
            chip(c, bx + cw + 4, cy, cw, totem ? "Totem" : "No totem", totem ? 0xFFFFD27A : 0xFFFF5FA8, !totem);
            float avg = (segs[0] + segs[1] + segs[2] + segs[3]) / 4f;
            chip(c, bx + 2 * (cw + 4), cy, cw, Math.round(avg * 100) + "%", 0xFFFFD27A, false);
        }
    }
}

// ======================================================================= W-Tap
final class WTap extends Mod {
    final Setting.Num chance = add(new Setting.Num("Chance %", 100, 0, 100, 5, 0));
    final Setting.Num delay = add(new Setting.Num("Release ticks", 1, 1, 3, 1, 0));
    private int ticks; private boolean forced;

    WTap() { super("W-Tap", "Resets your sprint after every hit for full knockback", Category.COMBAT); }

    @Override public void onAttack(Entity e, Vec3d hit) {
        if (mc.player != null && mc.player.isSprinting() && ThreadLocalRandom.current().nextDouble() * 100 < chance.v) ticks = delay.i();
    }
    @Override public void onStartTick(MinecraftClient m) {
        if (ticks > 0) { m.options.forwardKey.setPressed(false); ticks--; forced = true; }
        else if (forced) { forced = false; m.options.forwardKey.setPressed(Keys.isDown(m.options.forwardKey)); }
    }
    @Override public void onDisable() {
        if (forced) { forced = false; mc.options.forwardKey.setPressed(Keys.isDown(mc.options.forwardKey)); }
        ticks = 0;
    }
}

// ======================================================================= Ripple
final class Ripple extends Mod {
    final Setting.Num size = add(new Setting.Num("Max radius", 1.6, 0.5, 4, 0.1, 1));
    private final List<double[]> waves = new ArrayList<>(); // x,y,z,age

    Ripple() { super("Ripple", "A wave of light over whoever you hit", Category.COMBAT); }

    @Override public void onAttack(Entity e, Vec3d hit) { waves.add(new double[]{hit.x, hit.y, hit.z, 0}); }
    @Override public void onTick(MinecraftClient m) {
        for (int i = waves.size() - 1; i >= 0; i--) {
            double[] w = waves.get(i);
            w[3]++;
            double r = size.v * w[3] / 10.0;
            for (int k = 0; k < 18; k++) {
                double a = k * Math.PI * 2 / 18;
                m.particleManager.addParticle(ParticleTypes.END_ROD, w[0] + Math.cos(a) * r, w[1], w[2] + Math.sin(a) * r,
                    Math.cos(a) * 0.01, 0.0, Math.sin(a) * 0.01);
            }
            if (w[3] >= 10) waves.remove(i);
        }
    }
    @Override public void onWorldChange() { waves.clear(); }
}

// ======================================================================= Hit Splash
final class HitSplash extends Mod {
    final Setting.Num amount = add(new Setting.Num("Droplets", 18, 4, 60, 2, 0));
    HitSplash() { super("Hit Splash", "Every hit splashes glowing ink where it lands", Category.COMBAT); }
    @Override public void onAttack(Entity e, Vec3d hit) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < amount.i(); i++)
            mc.particleManager.addParticle(ParticleTypes.GLOW_SQUID_INK, hit.x, hit.y, hit.z,
                (r.nextDouble() - 0.5) * 0.25, r.nextDouble() * 0.18, (r.nextDouble() - 0.5) * 0.25);
    }
}

// ======================================================================= Hitboxes
final class Hitboxes extends Mod {
    final Setting.Bool players = add(new Setting.Bool("Players", true));
    final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
    final Setting.Bool items = add(new Setting.Bool("Items", false));
    final Setting.Num range = add(new Setting.Num("Range", 64, 8, 256, 8, 0));
    final Setting.Num width = add(new Setting.Num("Line width", 1, 1, 4, 1, 0));
    final Setting.Col color = add(new Setting.Col("Color", 0xFFFFFF));

    Hitboxes() { super("Hitboxes", "Clean outlines around entities", Category.COMBAT); }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.wanted(e, players.v, mobs.v, items.v) || Targets.dist(e) > range.v) continue;
            Gfx.wire(c, Proj.box(e, td), color.argb(230), width.i());
        }
    }
}

// ======================================================================= Radiance
final class Radiance extends Mod {
    final Setting.Bool players = add(new Setting.Bool("Players", true));
    final Setting.Bool mobs = add(new Setting.Bool("Mobs", true));
    final Setting.Bool items = add(new Setting.Bool("Items", true));
    final Setting.Num range = add(new Setting.Num("Range", 48, 8, 128, 8, 0));
    final Setting.Col color = add(new Setting.Col("Color", 0xC9A0FF));

    Radiance() { super("Radiance", "A glowing outline on players, mobs and items", Category.COMBAT); }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.wanted(e, players.v, mobs.v, items.v) || Targets.dist(e) > range.v) continue;
            Box b = Proj.box(e, td);
            Gfx.wire(c, b, color.argb(0x16), 7);
            Gfx.wire(c, b, color.argb(0x30), 4);
            Gfx.wire(c, b, color.argb(0xE0), 1);
        }
    }
}

// ======================================================================= Combo / Totem counters
final class ComboHud extends HudMod {
    ComboHud() { super("Combo Counter", "Your hit streak", Category.COMBAT, 300, 150); }
    public void draw(DrawContext c, float td) {
        if (State.combo <= 0 && !HudEditor.editing) { w = 0; h = 0; return; }
        chip(c, "COMBO  x" + Math.max(State.combo, HudEditor.editing ? 7 : 0));
    }
}

final class TotemHud extends HudMod {
    TotemHud() { super("Totem Counter", "Totems popped around you", Category.COMBAT, 300, 220); }
    public void draw(DrawContext c, float td) {
        List<Map.Entry<java.util.UUID, Integer>> top = new ArrayList<>(State.pops.entrySet());
        top.sort((a, b) -> b.getValue() - a.getValue());
        if (top.isEmpty() && !HudEditor.editing) { w = 0; h = 0; return; }
        List<String> lines = new ArrayList<>();
        lines.add("TOTEMS  " + State.totalPops);
        for (int i = 0; i < Math.min(4, top.size()); i++)
            lines.add(NickHider.apply(State.popNames.getOrDefault(top.get(i).getKey(), "?")) + "  " + top.get(i).getValue());
        int mw = 0; for (String s : lines) mw = Math.max(mw, Gfx.tw(s));
        w = mw + 14; h = lines.size() * 11 + 7;
        Gfx.glass(c, x, y, w, h);
        int yy = y + 4;
        for (int i = 0; i < lines.size(); i++) { Gfx.text(c, lines.get(i), x + 7, yy, i == 0 ? Theme.accent() : Theme.TEXT); yy += 11; }
    }
}

// ======================================================================= Custom crosshair
final class CustomCrosshair extends Mod {
    final Setting.Mode style = add(new Setting.Mode("Style", new String[]{"Dot", "Cross", "Plus", "Circle", "Square", "Diamond"}, 1));
    final Setting.Num size = add(new Setting.Num("Size", 6, 2, 20, 1, 0));
    final Setting.Num gap = add(new Setting.Num("Gap", 3, 0, 10, 1, 0));
    final Setting.Num thick = add(new Setting.Num("Thickness", 2, 1, 4, 1, 0));
    final Setting.Col color = add(new Setting.Col("Color", 0xFF8CC8));
    final Setting.Bool hit = add(new Setting.Bool("Hit confirmation", true));
    final Setting.Bool cooldown = add(new Setting.Bool("Attack cooldown", true));

    CustomCrosshair() { super("Custom Crosshair", "Six styles, hit confirmation and a clear cooldown", Category.COMBAT); }

    @Override public boolean replaceCrosshair(DrawContext c, float td) {
        if (!mc.options.getPerspective().isFirstPerson()) return true;
        int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
        int col = color.argb(255), s = size.i(), g = gap.i(), t = thick.i(), h = t / 2;
        switch (style.get()) {
            case "Dot" -> c.fill(cx - t, cy - t, cx + t, cy + t, col);
            case "Cross" -> { int gl = Gfx.alpha(color.rgb(), 0x38);
                              Gfx.line(c, cx - s, cy - s, cx - g, cy - g, t + 3, gl); Gfx.line(c, cx + s, cy - s, cx + g, cy - g, t + 3, gl);
                              Gfx.line(c, cx - s, cy + s, cx - g, cy + g, t + 3, gl); Gfx.line(c, cx + s, cy + s, cx + g, cy + g, t + 3, gl);
                              Gfx.line(c, cx - s, cy - s, cx - g, cy - g, t, col); Gfx.line(c, cx + s, cy - s, cx + g, cy - g, t, col);
                              Gfx.line(c, cx - s, cy + s, cx - g, cy + g, t, col); Gfx.line(c, cx + s, cy + s, cx + g, cy + g, t, col); }
            case "Circle" -> Gfx.ring(c, cx, cy, s, t, col);
            case "Square" -> Gfx.outline(c, cx - s, cy - s, cx + s, cy + s, col);
            case "Diamond" -> { Gfx.line(c, cx, cy - s, cx + s, cy, t, col); Gfx.line(c, cx + s, cy, cx, cy + s, t, col);
                                Gfx.line(c, cx, cy + s, cx - s, cy, t, col); Gfx.line(c, cx - s, cy, cx, cy - s, t, col); }
            default -> { c.fill(cx - s, cy - h, cx - g, cy - h + t, col); c.fill(cx + g, cy - h, cx + s, cy - h + t, col);
                         c.fill(cx - h, cy - s, cx - h + t, cy - g, col); c.fill(cx - h, cy + g, cx - h + t, cy + s, col); }
        }
        if (hit.v) {
            long age = System.currentTimeMillis() - State.hitMarkMs;
            if (age < 220) {
                int a = (int) (255 * (1 - age / 220.0)), rc = (a << 24) | 0xFF4060;
                Gfx.line(c, cx - 4, cy - 4, cx - 9, cy - 9, 1, rc); Gfx.line(c, cx + 4, cy - 4, cx + 9, cy - 9, 1, rc);
                Gfx.line(c, cx - 4, cy + 4, cx - 9, cy + 9, 1, rc); Gfx.line(c, cx + 4, cy + 4, cx + 9, cy + 9, 1, rc);
            }
        }
        if (cooldown.v && mc.player != null) {
            float cd = mc.player.getAttackCooldownProgress(0f);
            if (cd < 1f) {
                int bw = 20, by = cy + Math.max(s, 8) + 6;
                c.fill(cx - bw / 2, by, cx + bw / 2, by + 2, 0x60FFFFFF);
                c.fill(cx - bw / 2, by, cx - bw / 2 + (int) (bw * cd), by + 2, Theme.accent());
            }
        }
        return true;
    }
}
