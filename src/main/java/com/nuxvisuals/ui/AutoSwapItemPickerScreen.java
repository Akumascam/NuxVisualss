package com.nuxvisuals.ui;
import com.nuxvisuals.module.impl.AutoSwap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
/** Reads inventory stacks only. Selecting a template never sends inventory clicks to the server. */
public final class AutoSwapItemPickerScreen extends Screen {
 private final Screen parent;private final AutoSwap owner;private final int target;
 private String message="";
 public AutoSwapItemPickerScreen(Screen parent,AutoSwap owner,int target){super(Text.literal("Выбери предмет"));this.parent=parent;this.owner=owner;this.target=target;}
 private int unit(){return Math.max(18,Math.min(24,(width-24)/9));}
 private int x0(){return width/2-unit()*9/2;}
 private int y0(){return height/2-unit()*2;}
 private int slotAt(double mx,double my){int u=unit(),x=(int)Math.floor((mx-x0())/u),y=(int)Math.floor((my-y0())/u);
  if(x>=0&&x<9&&y>=0&&y<4)return y==3?x:9+y*9+x;
  if(mx>=x0()&&mx<x0()+u&&my>=y0()+u*4+13&&my<y0()+u*5+13)return 40;return -1;}
 private void label(DrawContext c,String s,int x,int y){NuxFont.drawOn(c,textRenderer,s,x,y,0xFFE2EAF9,0xFF172037);}
 @Override public boolean shouldPause(){return false;}
 @Override public void renderBackground(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0xCE090E18);}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  renderBackground(c,mx,my,delta);if(client.player==null){label(c,"Сначала зайди в мир",width/2-65,height/2);return;}
  int u=unit(),xx=x0(),yy=y0();label(c,"Сектор "+(target+1)+": выбери любой предмет",xx,yy-24);
  for(int row=0;row<4;row++)for(int col=0;col<9;col++){
   int inv=row==3?col:9+row*9+col;drawSlot(c,inv,xx+col*u,yy+row*u,mx,my);
  }
  drawSlot(c,40,xx,yy+u*4+13,mx,my);label(c,"Шаблон из второй руки",xx+u+6,yy+u*4+19);
  label(c,"Сохраняются название, чары и компоненты предмета.",xx,yy+u*5+30);
  label(c,message.isEmpty()?"Esc — назад; предметы не перемещаются при настройке.":message,xx,yy+u*5+44);
 }
 private void drawSlot(DrawContext c,int index,int x,int y,int mx,int my){int u=unit();boolean hover=slotAt(mx,my)==index;
  NuxShapes.roundedRect(c,x+1,y+1,u-2,u-2,3,hover?0xFF4E6797:0xFF26334B);
  ItemStack item=client.player.getInventory().getStack(index);
  if(!item.isEmpty()){c.drawItem(item,x+(u-16)/2,y+(u-16)/2);c.drawStackOverlay(textRenderer,item,x+(u-16)/2,y+(u-16)/2,null);}
 }
 @Override public boolean mouseClicked(double mx,double my,int button){
  int slot=slotAt(mx,my);if(button==0&&slot>=0&&client.player!=null){ItemStack item=client.player.getInventory().getStack(slot);
   if(!item.isEmpty())try{owner.setWheelItem(target,item);client.setScreen(parent);}catch(Exception e){message="Не удалось сохранить этот предмет";}return true;}
  return super.mouseClicked(mx,my,button);
 }
 @Override public void close(){client.setScreen(parent);}
}
