package com.nitro.core;

import com.google.gson.*;
import com.nitro.gui.ClickGui;
import com.nitro.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class ModuleManager {
    public static final List<Mod> ALL = new ArrayList<>();
    public static int menuKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public static final List<String> staffKeywords = new ArrayList<>(List.of(
        "owner", "co-owner", "admin", "administrator", "mod", "moderator", "helper", "staff", "support", "dev", "manager", "builder"));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nitroclient3.json");
    private static boolean menuPrev = true;
    public static BiConsumer<String, String> toaster;

    /** Run a module hook; if it throws, log it, count it and switch the module off after 3 failures instead of crashing the game. */
    private static void guard(Mod m, String where, Runnable r) {
        try { r.run(); }
        catch (Throwable t) {
            m.errors++;
            System.err.println("[Nitro] " + m.name + " failed in " + where + " (" + m.errors + "): " + t);
            if (m.errors == 1) t.printStackTrace();
            if (m.errors >= 3 && m.enabled) { m.enabled = false; if (toaster != null) toaster.accept("Module error", m.name + " disabled (see log)"); }
        }
    }

    private ModuleManager() {}

    public static void init() { ALL.addAll(Modules.create()); load(); }

    public static <T extends Mod> T get(Class<T> c) {
        for (Mod m : ALL) if (c.isInstance(m)) return c.cast(m);
        return null;
    }

    // ---------------- events
    public static void startTick(MinecraftClient mc) {
        for (Mod m : ALL) if (m.enabled) guard(m, "startTick", () -> m.onStartTick(mc));
    }

    public static void tick(MinecraftClient mc) {
        State.tick(mc);
        if (mc.getWindow() == null) return;
        long win = mc.getWindow().getHandle();

        boolean menu = GLFW.glfwGetKey(win, menuKey) == GLFW.GLFW_PRESS;
        if (menu && !menuPrev && mc.currentScreen == null && mc.world != null
                && System.currentTimeMillis() - ClickGui.lastClosedMs > 250) mc.setScreen(new ClickGui());
        menuPrev = menu;

        for (Mod m : ALL) {
            if (m.key > 0) {
                boolean d = GLFW.glfwGetKey(win, m.key) == GLFW.GLFW_PRESS;
                if (mc.currentScreen == null && mc.world != null) {
                    if (m.holdToUse()) m.setEnabled(d);
                    else if (d && !m.keyWasDown) m.toggle();
                }
                m.keyWasDown = d;
            }
            if (mc.world != null && (m.enabled || m.tickAlways())) guard(m, "tick", () -> m.onTick(mc));
        }
    }

    public static void hud(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        float td = Compat.tickProgress(counter);
        Proj.setup(mc, ctx, td);
        for (Mod m : ALL) if (m.enabled) guard(m, "hud", () -> m.onHud(ctx, td));
    }

    public static boolean crosshair(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        float td = Compat.tickProgress(counter);
        for (Mod m : ALL) {
            if (!m.enabled) continue;
            try { if (m.replaceCrosshair(ctx, td)) return true; } catch (Throwable t) { m.errors++; System.err.println("[Nitro] " + m.name + " crosshair failed: " + t); if (m.errors >= 3) m.enabled = false; }
        }
        return false;
    }

    public static void attack(Entity e, EntityHitResult hit) {
        State.onAttack(e);
        Vec3d pos = hit != null ? hit.getPos() : new Vec3d(e.getX(), e.getY() + e.getHeight() / 2, e.getZ());
        for (Mod m : ALL) if (m.enabled) guard(m, "attack", () -> m.onAttack(e, pos));
    }

    public static void message(Text t) { for (Mod m : ALL) if (m.enabled) guard(m, "message", () -> m.onMessage(t)); }

    public static Text modify(Text t) {
        for (Mod m : ALL) if (m.enabled) t = m.modifyMessage(t);
        return t;
    }

    public static void disconnect() {
        State.reset();
        for (Mod m : ALL) m.onWorldChange();
    }

    public static void shutdown() {
        for (Mod m : ALL) { try { m.onShutdown(); } catch (Throwable ignored) {} }
        save();
    }

    public static void resetAll() {
        for (Mod m : ALL) for (Setting s : m.settings) s.reset();
        save();
    }

    // ---------------- config
    private static void load() {
        try {
            if (!Files.exists(PATH)) { save(); return; }
            JsonObject root;
            try (Reader r = Files.newBufferedReader(PATH)) { root = JsonParser.parseReader(r).getAsJsonObject(); }
            if (root.has("menuKey")) menuKey = root.get("menuKey").getAsInt();
            if (root.has("staffKeywords")) {
                staffKeywords.clear();
                for (JsonElement e : root.getAsJsonArray("staffKeywords")) staffKeywords.add(e.getAsString());
            }
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Mod m : ALL) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                try {
                    if (o.has("enabled") && m.persistEnabled()) m.enabled = o.get("enabled").getAsBoolean();
                    if (o.has("key")) m.key = o.get("key").getAsInt();
                    if (m instanceof HudMod h) {
                        if (o.has("x")) h.x = o.get("x").getAsInt();
                        if (o.has("y")) h.y = o.get("y").getAsInt();
                        if (o.has("scale")) h.scale = Math.max(0.5f, Math.min(3f, o.get("scale").getAsFloat()));
                    }
                    if (o.has("settings")) {
                        JsonObject so = o.getAsJsonObject("settings");
                        for (Setting s : m.settings) if (so.has(s.name)) { try { s.load(so.get(s.name)); } catch (Exception ignored) {} }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[Nitro] config read failed, using defaults: " + e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("menuKey", menuKey);
        JsonArray ks = new JsonArray();
        for (String k : staffKeywords) ks.add(k);
        root.add("staffKeywords", ks);
        JsonObject mods = new JsonObject();
        for (Mod m : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.persistEnabled() && m.enabled);
            o.addProperty("key", m.key);
            if (m instanceof HudMod h) { o.addProperty("x", h.x); o.addProperty("y", h.y); o.addProperty("scale", h.scale); }
            JsonObject so = new JsonObject();
            for (Setting s : m.settings) so.add(s.name, s.save());
            o.add("settings", so);
            mods.add(m.name, o);
        }
        root.add("modules", mods);
        try (Writer w = Files.newBufferedWriter(PATH)) { GSON.toJson(root, w); }
        catch (Exception e) { System.err.println("[Nitro] config save failed: " + e); }
    }
}
