package com.nitro.core;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.render.RenderTickCounter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.UUID;

/**
 * Every Minecraft/authlib call that changed name between 1.21.x releases lives here.
 * If the build fails, the error will most likely point at one of these lines - just rename the call.
 */
public final class Compat {
    private Compat() {}
    public static float tickProgress(RenderTickCounter c) { return c.getTickProgress(false); }
    public static String name(GameProfile p) { return p.name(); }
    public static UUID id(GameProfile p) { return p.id(); }

    /** Soft glassy menu chime (amethyst), played as a UI sound so it follows the Master volume slider. */
    public static void chime(float pitch, float volume) {
        if (volume <= 0.02f) return;
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.ui(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, Math.max(0.5f, Math.min(2f, pitch)), volume));
    }

    /**
     * Draws a player's skin face (with hat layer) as a size x size square.
     * If this does not compile on your mappings, replace the body with a no-op; callers fall back to a letter tile.
     */
    public static void face(DrawContext c, UUID id, int x, int y, int size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerListEntry e = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(id) : null;
        SkinTextures t = e != null ? e.getSkinTextures() : DefaultSkinHelper.getSkinTextures(id);
        PlayerSkinDrawer.draw(c, t, x, y, size);
    }

    /** Text drawn at a scale (1.21.6+ uses a 2D matrix stack). If this fails to compile, replace body with c.drawText(...) unscaled. */
    public static void scaledText(DrawContext c, String s, float x, float y, float sc, int col, boolean shadow) {
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(sc, sc);
        c.drawText(MinecraftClient.getInstance().textRenderer, s, 0, 0, col | 0xFF000000, shadow);
        m.popMatrix();
    }
}
