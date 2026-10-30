package com.nitro.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * World-to-screen projection done by hand, so ESP/tracers/outlines are drawn on the HUD layer
 * (always visible through walls, no dependence on the world-render API).
 */
public final class Proj {
    public static boolean ready;
    public static double camX, camY, camZ;
    private static double fx, fy, fz, rx, rz, ux, uy, uz, tanHalf, aspect;
    private static int sw, sh;
    public static float yaw;

    private Proj() {}

    public static void setup(MinecraftClient mc, DrawContext c, float td) {
        ready = false;
        Entity e = mc.getCameraEntity();
        if (e == null) return;
        Vec3d p = e.getCameraPosVec(td);
        camX = p.x; camY = p.y; camZ = p.z;
        yaw = e.getYaw(td);
        double y = Math.toRadians(yaw), pt = Math.toRadians(e.getPitch(td));
        double cp = Math.cos(pt);
        fx = -Math.sin(y) * cp; fy = -Math.sin(pt); fz = Math.cos(y) * cp;
        rx = -Math.cos(y); rz = -Math.sin(y);
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
        sw = c.getScaledWindowWidth(); sh = c.getScaledWindowHeight();
        double fov = mc.options.getFov().getValue();
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
        aspect = (double) sw / sh;
        ready = true;
    }

    private static double[] cam(double x, double y, double z) {
        double dx = x - camX, dy = y - camY, dz = z - camZ;
        return new double[]{dx * rx + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
    }

    private static double[] scr(double[] c) {
        return new double[]{sw / 2.0 + c[0] / c[2] / (tanHalf * aspect) * sw / 2.0,
                            sh / 2.0 - c[1] / c[2] / tanHalf * sh / 2.0};
    }

    /** {screenX, screenY, depth} or null if behind the camera. */
    public static double[] project(double x, double y, double z) {
        double[] c = cam(x, y, z);
        if (c[2] < 0.05) return null;
        double[] s = scr(c);
        return new double[]{s[0], s[1], c[2]};
    }

    public static void line3(DrawContext c, double[] a, double[] b, int col, int w) {
        double[] ca = cam(a[0], a[1], a[2]), cb = cam(b[0], b[1], b[2]);
        double n = 0.05;
        if (ca[2] < n && cb[2] < n) return;
        if (ca[2] < n) ca = lerp(ca, cb, (n - ca[2]) / (cb[2] - ca[2]));
        else if (cb[2] < n) cb = lerp(cb, ca, (n - cb[2]) / (ca[2] - cb[2]));
        double[] sa = scr(ca), sb = scr(cb);
        Gfx.line(c, sa[0], sa[1], sb[0], sb[1], w, col);
    }

    private static double[] lerp(double[] p, double[] q, double t) {
        return new double[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t};
    }

    public static Box box(Entity e, float td) {
        Vec3d l = e.getLerpedPos(td);
        return e.getBoundingBox().offset(l.x - e.getX(), l.y - e.getY(), l.z - e.getZ());
    }

    /** Screen rectangle {minX,minY,maxX,maxY} around a box, using only corners in front of the camera. */
    public static double[] rect(Box b) {
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9; int n = 0;
        for (int i = 0; i < 8; i++) {
            double[] p = project((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            if (p == null) continue;
            n++;
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        return n == 0 ? null : new double[]{minX, minY, maxX, maxY};
    }
}
