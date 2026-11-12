package com.nuxvisuals.mixin;
import com.nuxvisuals.cosmetics.CosmeticSwordSkin;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemModelManager.class)
public class ItemModelManagerCosmeticMixin {
 @Inject(method="update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",at=@At("HEAD"),cancellable=true)
 private void nux$sword(ItemRenderState state,ItemStack stack,ModelTransformationMode mode,boolean leftHand,World world,LivingEntity entity,int seed,CallbackInfo ci){
  ItemStack rendered=CosmeticSwordSkin.apply(stack,mode,entity);
  // Fixed point: the nested update sees ITEM_MODEL already set and does not recurse again.
  if(rendered!=stack){((ItemModelManager)(Object)this).update(state,rendered,mode,leftHand,world,entity,seed);ci.cancel();}
 }
}
