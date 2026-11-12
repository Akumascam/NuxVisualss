package com.nuxvisuals.ui;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.impl.AutoSwap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;

/** Non-pausing wheel: cursor selection, physical movement only, no attack/use click-through. */
public final class AutoSwapWheelScreen extends Screen {
 private final AutoSwap owner;
 private final ClientWorld openedWorld;
 private static final int[] COLORS={0x609BA5B9,0x60D66077,0x60CDBB52,0x6075B5D3,0x60A591CF,0x6079BC9D,0x60D89D6A,0x609CA9D8};
 private final int[] available=new int[8];
 private int lastAge=Integer.MIN_VALUE;
 public AutoSwapWheelScreen(AutoSwap owner){super(Text.literal("AutoSwap"));this.owner=owner;openedWorld=MinecraftClient.getInstance().world;}
 public AutoSwap owner(){return owner;}
 private float outer(){return Math.max(36,Math.min(128,Math.min(width,height)*.37f));}
 private float inner(){return outer()*.55f;}
 private int selected(double mx,double my){return RadialMath.index(mx,my,width/2.0,height/2.0,inner(),outer(),owner.wheelSlots());}
 private boolean valid(){return owner.isEnabled()&&owner.isWheel()&&owner.wheelKeyHeld()&&client!=null&&client.player!=null
   &&client.world==openedWorld&&client.isWindowFocused();}
 @Override public boolean shouldPause(){return false;}
 @Override public void renderBackground(DrawContext context,int mx,int my,float delta){}
 @Override public void tick(){if(!valid())close();}
 @Override public void render(DrawContext context,int mx,int my,float delta){
  if(!valid()){close();return;}
  int count=owner.wheelSlots(),hover=selected(mx,my),cx=width/2,cy=height/2;
  float r=outer(),inside=inner();
  if(client.player.age!=lastAge){lastAge=client.player.age;for(int i=0;i<count;i++)available[i]=owner.available(i);}
  for(int i=0;i<count;i++){
   double start=-Math.PI/2+i*Math.PI*2/count+.008,end=-Math.PI/2+(i+1)*Math.PI*2/count-.008;
   int fill=COLORS[i];if(available[i]==0)fill=(fill&0xFFFFFF)|0x28000000;
   if(hover==i)fill=(fill&0xFFFFFF)|0xB0000000;
   sector(context,cx,cy,inside,r,start,end,fill);
   sector(context,cx,cy,r-1,r,start,end,hover==i?0xEEEBEDF5:0x55FFFFFF);
   sector(context,cx,cy,inside,inside+1,start,end,0x44FFFFFF);
   double mid=RadialMath.midpoint(i,count);int x=(int)(cx+Math.cos(mid)*r*.77)-8,y=(int)(cy+Math.sin(mid)*r*.77)-8;
   ItemStack item=owner.wheelItem(i);
   if(!item.isEmpty()){
    context.drawItem(item,x,y);String number=Integer.toString(available[i]);
    int nw=NuxFont.width(textRenderer,number);
    NuxShapes.roundedRect(context,x+8-nw/2-3,y+16,nw+6,13,3,0xD0181D29);
    NuxFont.drawOn(context,textRenderer,number,x+8-nw/2,y+18,0xFFFFFFFF,0xFF181D29);
   }else NuxFont.drawOn(context,textRenderer,"+",x+5,y+3,0xFFEAEDF5,0xFF202633);
  }
  String name=hover<0?"AutoSwap":owner.wheelItem(hover).isEmpty()?"Не назначено":owner.wheelItem(hover).getName().getString();
  while(name.length()>1&&NuxFont.width(textRenderer,name)>inside*2-18)name=name.substring(0,name.length()-1);
  int tw=NuxFont.width(textRenderer,name);
  NuxShapes.roundedRect(context,cx-tw/2-5,cy-12,tw+10,15,5,0xC0181D29);
  NuxFont.drawOn(context,textRenderer,name,cx-tw/2,cy-9,0xFFF0F3FA,0xFF181D29);
  String setup="Настроить";int sw=NuxFont.width(textRenderer,setup);
  NuxShapes.roundedRect(context,cx-sw/2-6,cy+8,sw+12,17,5,0xB0181D29);
  NuxFont.drawOn(context,textRenderer,setup,cx-sw/2,cy+12,0xFFCAD6EF,0xFF181D29);
  String hint="ЛКМ — выбрать · WASD — движение · отпустить клавишу — закрыть";
  while(hint.length()>1&&NuxFont.width(textRenderer,hint)>width-20)hint=hint.substring(0,hint.length()-1);
  int hw=NuxFont.width(textRenderer,hint);
  NuxShapes.roundedRect(context,cx-hw/2-5,cy+(int)r+9,hw+10,15,4,0xD0181D29);
  NuxFont.drawOn(context,textRenderer,hint,cx-hw/2,cy+(int)r+12,0xFFEBEFF8,0xFF181D29);
 }
 private static void sector(DrawContext context,float cx,float cy,float inner,float outer,double start,double end,int color){
  var matrix=context.getMatrices().peek().getPositionMatrix();int steps=Math.max(8,(int)Math.ceil((end-start)*14));
  context.draw(consumers->{var v=consumers.getBuffer(RenderLayer.getGui());
   for(int j=0;j<steps;j++){
    double a=start+(end-start)*j/steps,b=start+(end-start)*(j+1)/steps;
    float ca=(float)Math.cos(a),sa=(float)Math.sin(a),cb=(float)Math.cos(b),sb=(float)Math.sin(b);
    v.vertex(matrix,cx+ca*inner,cy+sa*inner,0).color(color);v.vertex(matrix,cx+ca*outer,cy+sa*outer,0).color(color);
    v.vertex(matrix,cx+cb*outer,cy+sb*outer,0).color(color);v.vertex(matrix,cx+cb*inner,cy+sb*inner,0).color(color);
   }
  });
 }
 @Override public boolean mouseClicked(double x,double y,int button){
  if(button==1){close();return true;}
  if(button!=0)return true;
  int index=selected(x,y);
  if(index>=0){close();owner.chooseWheelItem(index);return true;}
  if(Math.abs(x-width/2.0)<inner()*.8&&y>=height/2.0+5&&y<=height/2.0+29){
   client.setScreen(new AutoSwapConfigScreen(null,owner));return true;
  }
  return true;
 }
 @Override public void close(){
  if(client!=null&&client.currentScreen==this){client.setScreen(null);restoreMovement();}
 }
 private void restoreMovement(){
  if(client.player==null||client.world!=openedWorld||client.currentScreen!=null||!client.isWindowFocused())return;
  for(KeyBinding binding:new KeyBinding[]{client.options.forwardKey,client.options.backKey,client.options.leftKey,client.options.rightKey,
    client.options.jumpKey,client.options.sneakKey,client.options.sprintKey})binding.setPressed(Keys.isBindingDown(binding));
 }
 /** Called after vanilla KeyboardInput.tick, before ShiftTap's optional sneak pulse. */
 public static void applyMovement(Input input){
  var c=MinecraftClient.getInstance();
  if(!(c.currentScreen instanceof AutoSwapWheelScreen wheel)||!wheel.valid())return;
  var o=c.options;
  boolean f=Keys.isBindingDown(o.forwardKey),b=Keys.isBindingDown(o.backKey),l=Keys.isBindingDown(o.leftKey),r=Keys.isBindingDown(o.rightKey);
  input.playerInput=new PlayerInput(f,b,l,r,Keys.isBindingDown(o.jumpKey),Keys.isBindingDown(o.sneakKey),Keys.isBindingDown(o.sprintKey));
  input.movementForward=f==b?0:f?1:-1;input.movementSideways=l==r?0:l?1:-1;
 }
}
