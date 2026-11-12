package com.nuxvisuals.ui;
import net.minecraft.client.gui.DrawContext;import net.minecraft.client.gui.PlayerSkinDrawer;import net.minecraft.client.util.SkinTextures;
/** Clip only this portrait; never change the shared player's skin texture/filter. */
public final class NuxAvatar{
 public static void draw(DrawContext c,SkinTextures skin,int x,int y,int size){
  int r=Math.max(2,size/5);NuxShapes.roundedRect(c,x-1,y-1,size+2,size+2,r+1,0x35FFFFFF);
  for(int row=0;row<r;row++){
   double dy=r-row-.5;int inset=(int)Math.ceil(r-Math.sqrt(Math.max(0,r*r-dy*dy)));
   slice(c,skin,x,y,size,x+inset,y+row,x+size-inset,y+row+1);
   slice(c,skin,x,y,size,x+inset,y+size-row-1,x+size-inset,y+size-row);
  }
  slice(c,skin,x,y,size,x,y+r,x+size,y+size-r);
 }
 private static void slice(DrawContext c,SkinTextures skin,int x,int y,int size,int x1,int y1,int x2,int y2){
  c.enableScissor(x1,y1,x2,y2);try{PlayerSkinDrawer.draw(c,skin,x,y,size);}finally{c.disableScissor();}
 }
 private NuxAvatar(){}
}
