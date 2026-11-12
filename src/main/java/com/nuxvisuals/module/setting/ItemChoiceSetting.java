package com.nuxvisuals.module.setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
/** Registry-aware item template, persisted as SNBT. Never inserted into the real inventory. */
public final class ItemChoiceSetting extends Setting {
 private String serialized="";
 private ItemStack template=ItemStack.EMPTY,normalized=ItemStack.EMPTY;
 private java.lang.ref.WeakReference<ClientWorld> resolvedWorld=new java.lang.ref.WeakReference<>(null);
 private boolean resolved;
 public ItemChoiceSetting(String name){super(name);}
 public void set(ItemStack stack){
  var world=MinecraftClient.getInstance().world;
  if(stack.isEmpty()){serialized="";template=normalized=ItemStack.EMPTY;resolved=true;resolvedWorld=new java.lang.ref.WeakReference<>(world);changed();return;}
  if(world==null)throw new IllegalStateException("No world registry");
  ItemStack copy=stack.copyWithCount(1);
  String encoded=copy.toNbt(world.getRegistryManager()).toString();
  if(encoded.length()>262144)throw new IllegalArgumentException("Item data too large");
  serialized=encoded;template=copy;resolvedWorld=new java.lang.ref.WeakReference<>(world);resolved=true;normalize();changed();
 }
 private void normalize(){if(template.isEmpty()){normalized=ItemStack.EMPTY;return;}normalized=template.copy();normalized.remove(DataComponentTypes.DAMAGE);normalized.remove(DataComponentTypes.REPAIR_COST);}
 public ItemStack get(){
  var world=MinecraftClient.getInstance().world;
  if(world==null){resolved=false;resolvedWorld.clear();template=normalized=ItemStack.EMPTY;return ItemStack.EMPTY;}
  if(!resolved||world!=resolvedWorld.get()){
   resolved=true;resolvedWorld=new java.lang.ref.WeakReference<>(world);template=normalized=ItemStack.EMPTY;
   if(!serialized.isEmpty())try{
    template=ItemStack.fromNbt(world.getRegistryManager(),StringNbtReader.parse(serialized)).orElse(ItemStack.EMPTY);
    normalize();
   }catch(Exception ignored){}
  }
  return template;
 }
 public boolean matches(ItemStack candidate){
  ItemStack value=get();
  if(candidate.isEmpty()||value.isEmpty()||!candidate.isOf(value.getItem()))return false;
  if(ItemStack.areItemsAndComponentsEqual(value,candidate))return true;
  ItemStack copy=candidate.copy();copy.remove(DataComponentTypes.DAMAGE);copy.remove(DataComponentTypes.REPAIR_COST);
  return ItemStack.areItemsAndComponentsEqual(normalized,copy);
 }
 @Override public JsonElement save(){return new JsonPrimitive(serialized);}
 @Override public void load(JsonElement element){String value=element.getAsString();serialized=value.length()<=262144?value:"";resolved=false;resolvedWorld=new java.lang.ref.WeakReference<>(null);template=normalized=ItemStack.EMPTY;}
}
