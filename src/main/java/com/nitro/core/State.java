package com.nitro.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** Shared fight/session state used by several modules. */
public final class State {
    public static LivingEntity target; public static long targetTime;
    public static int combo; public static long lastHitMs, hitMarkMs;
    public static int totalPops;
    public static final Map<UUID, Integer> pops = new LinkedHashMap<>();
    public static final Map<UUID, String> popNames = new HashMap<>();
    public static volatile boolean hideFire;
    public static volatile int skyRgb = -1;       // -1 = vanilla sky
    public static volatile double skyStrength = 1.0;
    public static volatile double skyLum = 1.0;   // vanilla sky brightness 0..1 (written by the sky mixin)

    /** Keep vanilla's day/night brightness, swap in the Skies palette colour. Input and output are 0xRRGGBB. */
    public static int blendSky(int vanilla) {
        int custom = skyRgb;
        if (custom < 0) return vanilla;
        int vr = (vanilla >> 16) & 255, vg = (vanilla >> 8) & 255, vb = vanilla & 255;
        double lum = (0.3 * vr + 0.59 * vg + 0.11 * vb) / 255.0;
        skyLum = lum;
        double f = Math.max(0.04, Math.min(1.0, lum * 1.7));
        double s = skyStrength;
        int r = (int) (vr * (1 - s) + ((custom >> 16) & 255) * f * s);
        int g = (int) (vg * (1 - s) + ((custom >> 8) & 255) * f * s);
        int b = (int) (vb * (1 - s) + (custom & 255) * f * s);
        return (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }
    public static final long SESSION = System.currentTimeMillis();

    private static final ArrayDeque<Long> LC = new ArrayDeque<>(), RC = new ArrayDeque<>();
    private static boolean ld, rd; private static int prevHurt;

    private State() {}

    public static void onAttack(Entity e) {
        long now = System.currentTimeMillis();
        hitMarkMs = now;
        if (e instanceof LivingEntity l) {
            target = l; targetTime = now;
            combo = (now - lastHitMs < 3000) ? combo + 1 : 1;
            lastHitMs = now;
        }
    }

    public static void onPop(LivingEntity e) {
        totalPops++;
        pops.merge(e.getUuid(), 1, Integer::sum);
        popNames.put(e.getUuid(), e.getName().getString());
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (mc.player.hurtTime > 0 && prevHurt == 0) combo = 0;
        prevHurt = mc.player.hurtTime;
        if (now - lastHitMs > 3000) combo = 0;
        if (mc.getWindow() != null) {
            long w = mc.getWindow().getHandle();
            boolean l = GLFW.glfwGetMouseButton(w, 0) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(w, 1) == GLFW.GLFW_PRESS;
            if (mc.currentScreen == null) { if (l && !ld) LC.add(now); if (r && !rd) RC.add(now); }
            ld = l; rd = r;
        }
    }

    public static int cps(boolean left) {
        long now = System.currentTimeMillis();
        ArrayDeque<Long> q = left ? LC : RC;
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    public static void reset() {
        target = null; combo = 0; totalPops = 0; pops.clear(); popNames.clear(); LC.clear(); RC.clear();
    }
}
