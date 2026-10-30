package com.nitro.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Base class for every module. (Named Mod to avoid clashing with java.lang.Module.) */
public abstract class Mod {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name, desc; public final Category cat;
    public boolean enabled, expanded, keyWasDown; public int key, errors;
    public final List<Setting> settings = new ArrayList<>();

    protected Mod(String name, String desc, Category cat) { this.name = name; this.desc = desc; this.cat = cat; }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    public void setEnabled(boolean b) {
        if (enabled == b) return;
        enabled = b;
        if (b) onEnable(); else onDisable();
    }
    public void toggle() { setEnabled(!enabled); }

    public boolean isHud() { return false; }
    /** Action modules are buttons (e.g. HUD Editor): clicking runs them instead of toggling. */
    public boolean isAction() { return false; }
    public void run() {}
    public boolean holdToUse() { return false; }
    public boolean persistEnabled() { return true; }
    public boolean tickAlways() { return false; }

    public void onEnable() {}
    public void onDisable() {}
    public void onStartTick(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
    public void onHud(DrawContext c, float td) {}
    public void onAttack(Entity e, Vec3d hit) {}
    public void onMessage(Text t) {}
    public Text modifyMessage(Text t) { return t; }
    public boolean replaceCrosshair(DrawContext c, float td) { return false; }
    public void onWorldChange() {}
    /** Called once when the game is closing (restore anything you changed in the player's options). */
    public void onShutdown() {}
}
