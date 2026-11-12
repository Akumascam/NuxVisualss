package com.nuxvisuals.mixin;
import com.nuxvisuals.module.impl.Protected;import com.nuxvisuals.ui.NicknameMask;
import net.minecraft.text.CharacterVisitor;import net.minecraft.text.Style;import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.Unique;import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TextVisitFactory.class)
public abstract class TextVisitFactoryMixin{
 @Unique private static final ThreadLocal<Boolean> NUX_REPLACING=ThreadLocal.withInitial(()->false);
 @Unique private static CharacterVisitor nux$indices(NicknameMask.Mapped mapped,int sourceLength,CharacterVisitor visitor){
  return (index,style,cp)->{int visible=Character.charCount(cp)>sourceLength?0xFFFD:cp;return visitor.accept(mapped.sourceIndex(index,Character.charCount(visible)),style,visible);};
 }
 @Inject(method="visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",at=@At("HEAD"),cancellable=true)
 private static void nux$protectName(String text,int startIndex,Style startingStyle,Style resetStyle,CharacterVisitor visitor,CallbackInfoReturnable<Boolean> cir){
  if(text==null||startIndex<0||startIndex>text.length()||NUX_REPLACING.get())return;
  var mapped=NicknameMask.map(text,Protected.hiddenName(),Protected.replacement());if(!mapped.changed())return;
  NUX_REPLACING.set(true);try{cir.setReturnValue(TextVisitFactory.visitFormatted(mapped.text(),mapped.displayIndex(startIndex),startingStyle,resetStyle,nux$indices(mapped,text.length(),visitor)));}finally{NUX_REPLACING.set(false);}
 }
 @Inject(method="visitForwards(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",at=@At("HEAD"),cancellable=true)
 private static void nux$protectForward(String text,Style style,CharacterVisitor visitor,CallbackInfoReturnable<Boolean> cir){
  if(text==null||NUX_REPLACING.get())return;var mapped=NicknameMask.map(text,Protected.hiddenName(),Protected.replacement());if(!mapped.changed())return;
  NUX_REPLACING.set(true);try{cir.setReturnValue(TextVisitFactory.visitForwards(mapped.text(),style,nux$indices(mapped,text.length(),visitor)));}finally{NUX_REPLACING.set(false);}
 }
 @Inject(method="visitBackwards(Ljava/lang/String;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",at=@At("HEAD"),cancellable=true)
 private static void nux$protectBackward(String text,Style style,CharacterVisitor visitor,CallbackInfoReturnable<Boolean> cir){
  if(text==null||NUX_REPLACING.get())return;var mapped=NicknameMask.map(text,Protected.hiddenName(),Protected.replacement());if(!mapped.changed())return;
  NUX_REPLACING.set(true);try{cir.setReturnValue(TextVisitFactory.visitBackwards(mapped.text(),style,nux$indices(mapped,text.length(),visitor)));}finally{NUX_REPLACING.set(false);}
 }
}
