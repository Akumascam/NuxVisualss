package com.nuxvisuals.mixin;
import com.nuxvisuals.module.impl.Protected;import com.nuxvisuals.ui.NicknameMask;
import net.minecraft.client.gui.screen.ChatInputSuggestor;import net.minecraft.client.gui.widget.TextFieldWidget;import net.minecraft.text.OrderedText;import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;import org.spongepowered.asm.mixin.injection.Inject;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin{
 @Shadow @Final TextFieldWidget textField;
 @Inject(method="provideRenderText",at=@At("HEAD"),cancellable=true)
 private void nux$maskPassword(String original,int firstCharacterIndex,CallbackInfoReturnable<OrderedText> cir){
  String actual=textField.getText();String protectedText=Protected.hidePassword()?Protected.maskAuthCommand(actual):null;
  if(protectedText==null)protectedText=actual;
  var mapped=NicknameMask.map(protectedText,Protected.hiddenName(),Protected.replacement());
  if(protectedText.equals(actual)&&!mapped.changed())return;
  // The window is expressed in ORIGINAL text coordinates, not nickname-alias coordinates.
  String visible=mapped.slice(firstCharacterIndex,original.length());
  cir.setReturnValue(OrderedText.styledForwardsVisitedString(visible,Style.EMPTY));
 }
}
