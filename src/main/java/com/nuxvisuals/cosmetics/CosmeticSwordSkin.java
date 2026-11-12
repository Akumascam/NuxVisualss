package com.nuxvisuals.cosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
/** Render-only component on a copy: vanilla handles poses, UVs, lighting, glint and handedness. */
public final class CosmeticSwordSkin {
 private CosmeticSwordSkin(){}
 private static final java.util.Set<String> BUNDLED=java.util.Set.of("requiem","phoenix","pharaoh","frost","katana","partisan","reaper","bloodmoon","candycane","icefang","emerald","thunder","dragonslayer","celestial");
 public static ItemStack apply(ItemStack original,ModelTransformationMode mode,LivingEntity entity){
  var client=MinecraftClient.getInstance();
  if(client.player==null||entity!=client.player||original.isEmpty())return original;
  boolean hand=switch(mode){case FIRST_PERSON_LEFT_HAND,FIRST_PERSON_RIGHT_HAND,THIRD_PERSON_LEFT_HAND,THIRD_PERSON_RIGHT_HAND->true;default->false;};
  if(!hand||!Cosmetics.swordActive(original))return original;
  Cosmetic selected=Cosmetics.equipped(Category.SWORD);String id=selected.id();
  if(!id.startsWith("sword:")||!BUNDLED.contains(id.substring(6)))return original;
  Identifier model=Identifier.of("nuxvisuals","cosmetics/"+id.substring(6));
  if(model.equals(original.get(DataComponentTypes.ITEM_MODEL)))return original;
  ItemStack rendered=original.copy();rendered.set(DataComponentTypes.ITEM_MODEL,model);return rendered;
 }
}
