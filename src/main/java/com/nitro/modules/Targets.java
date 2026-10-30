package com.nitro.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Shared entity filtering for the overlay modules. */
final class Targets {
    private Targets() {}

    static double dist(Entity e) {
        Entity c = MinecraftClient.getInstance().getCameraEntity();
        return c == null ? 0 : c.distanceTo(e);
    }

    static boolean wanted(Entity e, boolean players, boolean mobs, boolean items) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e == mc.getCameraEntity()) return false;
        if (e instanceof PlayerEntity) {
            if (e == mc.player && !Freecam.on) return false;
            return players;
        }
        if (e instanceof MobEntity) return mobs;
        if (e instanceof ItemEntity) return items;
        return false;
    }
}
