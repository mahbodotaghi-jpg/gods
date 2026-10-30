package com.nitro.mixin;

import com.nitro.core.State;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tints the fog / horizon colour so it matches the Skies palette (skipped underwater and in lava). */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nitro$fog(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness,
                           CallbackInfoReturnable<Vector4f> cir) {
        if (State.skyRgb < 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && (mc.player.isSubmergedInWater() || mc.player.isInLava())) return;
        Vector4f v = cir.getReturnValue();
        if (v == null) return;
        int vr = Math.min(255, Math.max(0, (int) (v.x * 255))), vg = Math.min(255, Math.max(0, (int) (v.y * 255))), vb = Math.min(255, Math.max(0, (int) (v.z * 255)));
        int rgb = State.blendSky((vr << 16) | (vg << 8) | vb);
        cir.setReturnValue(new Vector4f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, v.w));
    }
}
