package com.nuxvisuals.ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
/** A keyboard-accessible 3-by-2 gallery containing exactly the supplied six media. */
public final class NuxWallpaperScreen extends Screen {
 private final Screen parent;
 public NuxWallpaperScreen(Screen parent){super(Text.literal("Фоны главного меню"));this.parent=parent;}
 @Override protected void init(){
  int total=Math.min(660,width-28),gap=10,cw=(total-gap*2)/3;
  int ch=Math.min(cw*9/16+23,(height-88)/2),start=(width-total)/2;
  MenuTheme[] themes=MenuTheme.values();
  for(int i=0;i<themes.length;i++)addDrawableChild(new Tile(start+(i%3)*(cw+gap),44+(i/3)*(ch+gap),cw,ch,themes[i]));
  int w=Math.min(190,(width-36)/2),x=width/2-w-4;
  addDrawableChild(new NuxMenuButton(x,height-27,w,20,motionLabel(),textRenderer,true,false,b->{MenuTheme.setMotion(!MenuTheme.motion());b.setMessage(Text.literal(motionLabel()));}));
  addDrawableChild(new NuxMenuButton(width/2+4,height-27,w,20,"Готово",textRenderer,true,false,b->close()));
 }
 private String motionLabel(){return "Анимация: "+(MenuTheme.motion()?"включена":"выключена");}
 @Override public void renderBackground(DrawContext c,int mx,int my,float delta){
  NuxBackground.render(c,width,height);c.fill(0,0,width,height,0xB8090C15);
  String title="Выбери свой пейзаж";
  NuxFont.drawOn(c,textRenderer,title,(width-NuxFont.width(textRenderer,title))/2,15,0xFFFFF5FA,0xFF11151F);
  String caption="3 видео  /  3 фотографии";
  NuxFont.drawOn(c,textRenderer,caption,(width-NuxFont.width(textRenderer,caption))/2,29,0xFFC6BCCD,0xFF11151F);
 }
 @Override public void close(){client.setScreen(parent);}
 @Override public void removed(){NuxBackground.release();super.removed();}
 private final class Tile extends ButtonWidget {
  private final MenuTheme theme;
  Tile(int x,int y,int w,int h,MenuTheme theme){super(x,y,w,h,Text.literal(theme.title),b->MenuTheme.set(theme),DEFAULT_NARRATION_SUPPLIER);this.theme=theme;}
  @Override protected void renderWidget(DrawContext c,int mx,int my,float delta){
   int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean chosen=theme==MenuTheme.current();
   c.fill(x-1,y-1,x+w+1,y+h+1,chosen?theme.accentA:isHovered()||isFocused()?0xFFE7C7DF:0x706D687A);
   c.fill(x,y,x+w,y+h,0xFF121620);NuxBackground.poster(c,theme,x+2,y+2,w-4,h-21);
   String text=(chosen?"• ":"")+theme.title;
   while(NuxFont.width(textRenderer,text)>w-10&&text.length()>2)text=text.substring(0,text.length()-2)+"…";
   NuxFont.drawOn(c,textRenderer,text,x+5,y+h-14,chosen?0xFFFFE5F3:0xFFE3DAE7,0xFF121620);
  }
 }
}
