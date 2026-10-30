package com.nitro;

import com.nitro.core.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public class NitroClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        ClientTickEvents.START_CLIENT_TICK.register(ModuleManager::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(ModuleManager::tick);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.shutdown());

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) ModuleManager.attack(entity, hit);
            return ActionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> ModuleManager.message(msg));
        ClientReceiveMessageEvents.MODIFY_GAME.register((msg, overlay) -> ModuleManager.modify(msg));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ModuleManager.disconnect());

        HudElementRegistry.addLast(Identifier.of("nitroclient", "main"),
            (ctx, counter) -> ModuleManager.hud(ctx, counter));
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR,
            original -> (ctx, counter) -> { if (!ModuleManager.crosshair(ctx, counter)) original.render(ctx, counter); });
    }
}
