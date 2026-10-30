package com.nitro.mixin;

import com.nitro.core.State;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.21.11 draws the sky dome in SkyRendering.renderTopSky(int colour) (the old ClientWorld.getSkyColor is gone).
 * We swap the colour for the Skies palette while keeping vanilla's day/night brightness.
 */
@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @ModifyVariable(method = "renderTopSky", at = @At("HEAD"), argsOnly = true, require = 0)
    private int nitro$topSky(int color) {
        if (State.skyRgb < 0) return color;
        return (color & 0xFF000000) | State.blendSky(color & 0xFFFFFF);
    }
}
