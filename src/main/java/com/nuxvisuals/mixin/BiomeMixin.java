package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.WorldCustomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// World Customizer: снег вместо дождя и «заснеженная» трава/листва (только визуально)
@Mixin(Biome.class)
public class BiomeMixin {
    @Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true, require = 0)
    private void nuxvisuals$snow(CallbackInfoReturnable<Biome.Precipitation> cir) {
        // только на клиенте, чтобы встроенный сервер не начал засыпать мир снегом
        if (WorldCustomizer.forceSnow() && MinecraftClient.getInstance().isOnThread()) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }

    @Inject(method = "getGrassColorAt", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$grass(CallbackInfoReturnable<Integer> cir) {
        if (WorldCustomizer.snowyGround()) {
            cir.setReturnValue(WorldCustomizer.SNOW_TINT);
        }
    }

    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$foliage(CallbackInfoReturnable<Integer> cir) {
        if (WorldCustomizer.snowyGround()) {
            cir.setReturnValue(WorldCustomizer.SNOW_TINT);
        }
    }

    @Inject(method = "getWaterColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$water(CallbackInfoReturnable<Integer> cir) {
        if (WorldCustomizer.icyWater()) {
            cir.setReturnValue(WorldCustomizer.ICE_WATER);
        }
    }
}
