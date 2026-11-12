package com.nuxvisuals.mixin;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.block.BlockState;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.tag.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockColors.class)
public class WinterLeafColorsMixin {
 @Inject(method="getColor(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;I)I",at=@At("HEAD"),cancellable=true)
 private void nux$leaves(BlockState state,BlockRenderView view,BlockPos pos,int tint,CallbackInfoReturnable<Integer> cir){
  if(com.nuxvisuals.module.impl.WorldCustomizer.winter()&&state.isIn(BlockTags.LEAVES))cir.setReturnValue(com.nuxvisuals.module.impl.WorldCustomizer.SNOW_TINT);
 }
}
