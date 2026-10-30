package com.nitro.modules;

import com.nitro.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

// ======================================================================= Weather (particle based)
final class Weather extends Mod {
    final Setting.Mode mode = add(new Setting.Mode("Type", new String[]{"Rain", "Snow", "Ember"}, 0));
    final Setting.Num density = add(new Setting.Num("Density", 8, 1, 30, 1, 0));
    final Setting.Num radius = add(new Setting.Num("Radius", 12, 4, 24, 1, 0));

    Weather() { super("Weather", "Glassy rain, settling snow or ember showers around you", Category.VISUALS); }

    @Override public void onTick(MinecraftClient m) {
        Entity cam = m.getCameraEntity();
        if (cam == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < density.i(); i++) {
            double x = cam.getX() + (r.nextDouble() * 2 - 1) * radius.v;
            double z = cam.getZ() + (r.nextDouble() * 2 - 1) * radius.v;
            double y = cam.getY() + 2 + r.nextDouble() * 8;
            switch (mode.get()) {
                case "Snow" -> m.particleManager.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (r.nextDouble() - 0.5) * 0.02, -0.04, (r.nextDouble() - 0.5) * 0.02);
                case "Ember" -> m.particleManager.addParticle(ParticleTypes.FLAME, x, y, z, (r.nextDouble() - 0.5) * 0.02, -0.05, (r.nextDouble() - 0.5) * 0.02);
                default -> m.particleManager.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, -0.3, 0);
            }
        }
    }
}

// ======================================================================= Ambience
final class Ambience extends Mod {
    final Setting.Mode mode = add(new Setting.Mode("Particles", new String[]{"End Rod", "Glow", "Enchant", "Soul"}, 0));
    final Setting.Num density = add(new Setting.Num("Density", 2, 1, 12, 1, 0));

    Ambience() { super("Ambience", "Drifting particles around you", Category.VISUALS); }

    @Override public void onTick(MinecraftClient m) {
        Entity cam = m.getCameraEntity();
        if (cam == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < density.i(); i++) {
            double x = cam.getX() + (r.nextDouble() * 2 - 1) * 8, z = cam.getZ() + (r.nextDouble() * 2 - 1) * 8;
            double y = cam.getY() + r.nextDouble() * 4 - 1;
            var p = switch (mode.get()) {
                case "Glow" -> ParticleTypes.GLOW;
                case "Enchant" -> ParticleTypes.ENCHANT;
                case "Soul" -> ParticleTypes.SOUL;
                default -> ParticleTypes.END_ROD;
            };
            m.particleManager.addParticle(p, x, y, z, (r.nextDouble() - 0.5) * 0.01, 0.01, (r.nextDouble() - 0.5) * 0.01);
        }
    }
}

// ======================================================================= Afterglow
final class Afterglow extends Mod {
    final Setting.Num amount = add(new Setting.Num("Amount", 36, 8, 120, 4, 0));
    private final Set<Integer> done = new HashSet<>();

    Afterglow() { super("Afterglow", "The fallen dissolve into drifting light", Category.VISUALS); }

    @Override public void onWorldChange() { done.clear(); }
    @Override public void onTick(MinecraftClient m) {
        if (m.world == null) return;
        for (Entity e : m.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || l == m.player || !l.isDead()) continue;
            if (done.add(l.getId())) burst(m, l);
        }
        if (done.size() > 1024) done.clear();
    }
    private void burst(MinecraftClient m, LivingEntity l) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Box b = l.getBoundingBox();
        for (int i = 0; i < amount.i(); i++) {
            double x = b.minX + r.nextDouble() * (b.maxX - b.minX), y = b.minY + r.nextDouble() * (b.maxY - b.minY), z = b.minZ + r.nextDouble() * (b.maxZ - b.minZ);
            m.particleManager.addParticle(i % 3 == 0 ? ParticleTypes.WITCH : ParticleTypes.END_ROD, x, y, z,
                (r.nextDouble() - 0.5) * 0.02, 0.02 + r.nextDouble() * 0.05, (r.nextDouble() - 0.5) * 0.02);
        }
    }
}

// ======================================================================= Block outline (spring glide)
final class BlockOutline extends Mod {
    final Setting.Num stiffness = add(new Setting.Num("Spring stiffness", 1.0, 0.3, 3, 0.1, 1));
    final Setting.Num width = add(new Setting.Num("Line width", 2, 1, 4, 1, 0));
    final Setting.Bool fill = add(new Setting.Bool("Soft fill", true));
    final Setting.Col color = add(new Setting.Col("Color", 0xB48CFF));
    private final double[] cur = new double[6], vel = new double[6];
    private boolean init; private long last = System.nanoTime();

    BlockOutline() { super("Block Outline", "Glides from block to block on a spring", Category.VISUALS); }

    @Override public void onHud(DrawContext c, float td) {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - last) / 1e9); last = now;
        if (!Proj.ready || mc.world == null) return;
        HitResult hr = mc.crosshairTarget;
        if (!(hr instanceof BlockHitResult bh) || hr.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bh.getBlockPos();
        VoxelShape s = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
        if (s.isEmpty()) return;
        Box bb = s.getBoundingBox().offset(pos);
        double[] t = {bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ};
        if (!init) { System.arraycopy(t, 0, cur, 0, 6); java.util.Arrays.fill(vel, 0); init = true; }
        double k = 220 * stiffness.v, d = 2 * Math.sqrt(k) * 0.9;
        for (int i = 0; i < 6; i++) { vel[i] += (k * (t[i] - cur[i]) - d * vel[i]) * dt; cur[i] += vel[i] * dt; }
        Box b = new Box(cur[0], cur[1], cur[2], cur[3], cur[4], cur[5]).expand(0.003);
        if (fill.v) {
            double[] r = Proj.rect(b);
            if (r != null) c.fill((int) r[0], (int) r[1], (int) r[2], (int) r[3], color.argb(0x18));
        }
        Gfx.wire(c, b, color.argb(255), width.i());
    }
}

// ======================================================================= Name tags
final class NameTags extends Mod {
    final Setting.Num range = add(new Setting.Num("Range", 96, 16, 256, 8, 0));
    final Setting.Num lift = add(new Setting.Num("Height offset", 14, 0, 40, 1, 0));
    final Setting.Bool mobs = add(new Setting.Bool("Include mobs", true));
    final Setting.Bool dist = add(new Setting.Bool("Show distance", true));

    NameTags() { super("Name Tags", "Clean tags for players, mobs and items", Category.VISUALS); }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || !Targets.wanted(e, true, mobs.v, false)) continue;
            double d = Targets.dist(e);
            if (d > range.v) continue;
            Box b = Proj.box(e, td);
            double[] s = Proj.project((b.minX + b.maxX) / 2, b.maxY + 0.45, (b.minZ + b.maxZ) / 2);
            if (s == null) continue;
            String t = NickHider.apply(e.getName().getString()) + "  " + String.format("%.1f", l.getHealth() + l.getAbsorptionAmount())
                     + (dist.v ? "  " + (int) d + "m" : "");
            int tw = Gfx.tw(t), x = (int) s[0] - tw / 2 - 5, y = (int) s[1] - lift.i() - 6;
            Gfx.card(c, x, y, tw + 10, 14);
            Gfx.text(c, t, x + 5, y + 3, Theme.TEXT);
        }
    }
}

// ======================================================================= Zoom
final class Zoom extends Mod {
    final Setting.Num fov = add(new Setting.Num("Zoom FOV", 30, 30, 90, 1, 0));
    final Setting.Num smooth = add(new Setting.Num("Smoothness", 0.35, 0.1, 1, 0.05, 2));
    private Integer saved;

    Zoom() { super("Zoom", "Hold the bound key to zoom (bind it in the menu)", Category.VISUALS); key = 67; }
    @Override public boolean holdToUse() { return true; }
    @Override public boolean tickAlways() { return true; }
    @Override public boolean persistEnabled() { return false; }

    @Override public void onTick(MinecraftClient m) {
        int cur = m.options.getFov().getValue();
        if (enabled) {
            if (saved == null) saved = cur;
            m.options.getFov().setValue(step(cur, (int) fov.v));
        } else if (saved != null) {
            int n = step(cur, saved);
            m.options.getFov().setValue(n);
            if (n == saved) saved = null;
        }
    }
    @Override public void onShutdown() { if (saved != null) { mc.options.getFov().setValue(saved); saved = null; } }
    private int step(int cur, int target) {
        int n = (int) Math.round(cur + (target - cur) * smooth.v);
        return n == cur ? target : n;
    }
}

// ======================================================================= Fullbright
/** Client-side night vision (no gamma hacks): you see clearly in caves and at night. */
final class Fullbright extends Mod {
    private boolean added;
    Fullbright() { super("Fullbright", "See clearly in the darkest caves", Category.VISUALS); }

    @Override public void onTick(MinecraftClient m) {
        if (m.player == null) return;
        StatusEffectInstance cur = m.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (cur == null || (added && cur.getDuration() < 300)) {
            m.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, false, false, false));
            added = true;
        }
    }
    @Override public void onDisable() {
        if (added && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        added = false;
    }
    @Override public void onWorldChange() { added = false; }
}

// ======================================================================= Low fire
final class LowFire extends Mod {
    final Setting.Num height = add(new Setting.Num("Flame height", 26, 8, 70, 2, 0));
    final Setting.Col low = add(new Setting.Col("Base color", 0xFF4FA8));
    final Setting.Col high = add(new Setting.Col("Tip color", 0x8A5CFF));

    LowFire() { super("Low Fire", "Low flames in your colours; your view stays clear", Category.VISUALS); }

    @Override public void onTick(MinecraftClient m) { State.hideFire = enabled; }
    @Override public void onDisable() { State.hideFire = false; }

    @Override public void onHud(DrawContext c, float td) {
        if (mc.player == null || !mc.player.isOnFire()) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        double t = System.currentTimeMillis() / 1000.0;
        for (int x = 0; x < sw; x += 4) {
            double hh = height.v * (0.55 + 0.45 * Math.sin(t * 6 + x * 0.09) * Math.sin(t * 3.7 + x * 0.031));
            for (int layer = 0; layer < 3; layer++) {
                double f = 1 - layer * 0.3;
                int top = sh - (int) (hh * f);
                int col = Gfx.lerpColor(0xFF000000 | low.rgb(), 0xFF000000 | high.rgb(), layer / 2f);
                c.fill(x, top, x + 4, sh, Gfx.alpha(col, 0x38 + layer * 0x10));
            }
        }
    }
}

// ======================================================================= Screen overlays
final class CuteWorld extends Mod {
    final Setting.Num intensity = add(new Setting.Num("Intensity", 40, 5, 120, 5, 0));
    CuteWorld() { super("Cute World", "A soft pink tint over the whole screen", Category.VISUALS); }
    @Override public void onHud(DrawContext c, float td) {
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        c.fill(0, 0, w, h, Gfx.alpha(0xFF9CCB, Math.min(255, intensity.i())));
        for (int i = 0; i < 12; i++) {
            int a = (int) (intensity.v * 0.9 * (12 - i) / 12);
            c.fill(0, i * 3, w, i * 3 + 3, Gfx.alpha(0xFFFFFF, a)); c.fill(0, h - i * 3 - 3, w, h - i * 3, Gfx.alpha(0xFFFFFF, a));
        }
    }
}

final class Cyberspace extends Mod {
    final Setting.Num intensity = add(new Setting.Num("Intensity", 40, 5, 120, 5, 0));
    Cyberspace() { super("Cyberspace", "Neon tint, scanlines and glitch bars", Category.VISUALS); }
    @Override public void onHud(DrawContext c, float td) {
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        long ms = System.currentTimeMillis();
        c.fill(0, 0, w / 2, h, Gfx.alpha(0x00E0FF, intensity.i() / 3));
        c.fill(w / 2, 0, w, h, Gfx.alpha(0xFF30D0, intensity.i() / 3));
        for (int y = 0; y < h; y += 3) c.fill(0, y, w, y + 1, 0x16000000);
        int band = (int) ((ms / 12) % (h + 60)) - 30;
        c.fill(0, band, w, band + 18, Gfx.alpha(0xFFFFFF, intensity.i() / 6));
        if ((ms / 700) % 4 == 0) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            int gy = r.nextInt(h), gx = r.nextInt(w / 2);
            c.fill(gx, gy, gx + w / 3, gy + 3 + r.nextInt(5), Gfx.alpha(0x00FFFF, 70));
        }
    }
}

final class Bokeh extends Mod {
    final Setting.Num count = add(new Setting.Num("Circles", 14, 4, 40, 1, 0));
    final Setting.Num vignette = add(new Setting.Num("Vignette", 90, 0, 200, 10, 0));
    Bokeh() { super("Bokeh", "Soft drifting light circles and a vignette", Category.VISUALS); }
    @Override public void onHud(DrawContext c, float td) {
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        double t = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < 14; i++) {
            double sx = Math.abs(Math.sin(i * 12.9898) * 43758.5453) % 1, sy = Math.abs(Math.sin(i * 78.233) * 12345.678) % 1;
            int cx = (int) (((sx * w + t * (6 + i % 5 * 3)) % (w + 80)) - 40), cy = (int) (((sy * h - t * (4 + i % 3 * 2)) % (h + 80) + h + 80) % (h + 80) - 40);
            int r = 10 + (i % 5) * 7;
            if (i >= count.i()) continue;
            Gfx.circle(c, cx, cy, r, Theme.accentA(0x10));
            Gfx.ring(c, cx, cy, r, 1, Theme.accentA(0x30));
        }
        int steps = 14;
        for (int i = 0; i < steps; i++) {
            int a = (int) (vignette.v * (steps - i) / steps / 2.5);
            int e = i * 6;
            c.fill(0, e, w, e + 6, a << 24); c.fill(0, h - e - 6, w, h - e, a << 24);
            c.fill(e, 0, e + 6, h, a << 24); c.fill(w - e - 6, 0, w - e, h, a << 24);
        }
    }
}

// ======================================================================= Nick hider
final class NickHider extends Mod {
    static NickHider I;
    final Setting.Mode fake = add(new Setting.Mode("Shown name", new String[]{"Steve", "Alex", "Player", "Hidden"}, 0));

    NickHider() { super("Nick Hider", "Replaces your name in chat and in Nitro's own HUD", Category.VISUALS); I = this; }

    static String apply(String s) {
        if (I == null || !I.enabled) return s;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return s;
        String me = mc.player.getName().getString();
        return s.replace(me, I.fake.get());
    }

    @Override public Text modifyMessage(Text t) {
        String s = t.getString(), r = apply(s);
        return r.equals(s) ? t : Text.literal(r);
    }
}

// ======================================================================= Skies
final class Skies extends Mod {
    private static final String[] NAMES = {"Galaxy", "Aether", "Nocturne", "Amethyst", "Opaline", "Rain", "Snow", "Fire"};
    private static final int[][] PAL = {
        {0x2A0F5C, 0xC15BFF}, {0x6E7380, 0xB9BEC9}, {0x120B26, 0x3B2A6B}, {0x3A1558, 0x8E3FD1},
        {0x23535A, 0x7FD6B8}, {0x4A4F58, 0x7A828E}, {0xB8C2CC, 0xE6EDF2}, {0x7A1D0B, 0xFF6A1F}};
    final Setting.Mode sky = add(new Setting.Mode("Sky", NAMES, 0));
    final Setting.Num speed = add(new Setting.Num("Shift speed", 0.4, 0.0, 2.0, 0.05, 2));
    final Setting.Num strength = add(new Setting.Num("Strength", 1.0, 0.1, 1.0, 0.05, 2));
    final Setting.Bool stars = add(new Setting.Bool("Shooting stars (Galaxy)", true));
    private int tk;

    Skies() { super("Skies", "Eight living skies, from cosmic light to snow (colour-based)", Category.VISUALS); }

    @Override public boolean tickAlways() { return true; }
    @Override public void onDisable() { State.skyRgb = -1; }

    @Override public void onTick(MinecraftClient m) {
        if (!enabled) { State.skyRgb = -1; return; }
        double t = (Math.sin(System.currentTimeMillis() / 1000.0 * speed.v) + 1) / 2;
        int[] p = PAL[sky.idx];
        State.skyRgb = Gfx.lerpColor(0xFF000000 | p[0], 0xFF000000 | p[1], (float) t) & 0xFFFFFF;
        State.skyStrength = strength.v;
        // Galaxy: occasional shooting stars across the night sky
        if (stars.v && sky.idx == 0 && m.player != null && State.skyLum < 0.35 && ++tk % 50 == 0
                && ThreadLocalRandom.current().nextInt(100) < 55) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            double a = r.nextDouble() * Math.PI * 2, d = 45 + r.nextDouble() * 30;
            double sx = m.player.getX() + Math.cos(a) * d, sy = m.player.getY() + 55 + r.nextDouble() * 30, sz = m.player.getZ() + Math.sin(a) * d;
            double b = r.nextDouble() * Math.PI * 2, dx = Math.cos(b) * 2.2, dz = Math.sin(b) * 2.2, dy = -0.9;
            for (int i = 0; i < 14; i++)
                m.particleManager.addParticle(ParticleTypes.END_ROD, sx + dx * i * 0.6, sy + dy * i * 0.6, sz + dz * i * 0.6, dx * 0.02, dy * 0.02, dz * 0.02);
        }
    }
}
