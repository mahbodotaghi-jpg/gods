package com.nitro.mixin;

import com.nitro.core.State;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Detects totem pops (entity status 35). */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"), require = 0)
    private void nitro$status(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread() || mc.world == null) return;
        if (packet.getStatus() == 35) {
            Entity e = packet.getEntity(mc.world);
            if (e instanceof LivingEntity le) State.onPop(le);
        }
    }
}
