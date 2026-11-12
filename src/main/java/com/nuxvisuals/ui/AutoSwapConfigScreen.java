package com.nuxvisuals.ui;
import com.nuxvisuals.module.impl.AutoSwap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
/** Assign by inventory click, reorder by dragging, clear with right-click. */
public final class AutoSwapConfigScreen extends Screen {
 private final Screen parent;
 private final AutoSwap owner;
 private int dragging=-1;
 private static final int BG=0xFF111827;
 public AutoSwapConfigScreen(Screen parent,AutoSwap owner){super(Text.literal("Предметы круга AutoSwap"));this.parent=parent;this.owner=owner;}
 private int panelWidth(){return Math.min(450,width-20);}
 private int rowHeight(){int rows=(owner.wheelSlots()+1)/2;return Math.max(25,Math.min(48,(height-92)/rows));}
 private int top(){return Math.max(40,(height-((owner.wheelSlots()+1)/2)*rowHeight())/2);}
 private int at(double mx,double my){int w=panelWidth(),x=width/2-w/2,cw=(w-8)/2,ry=top(),ch=rowHeight();
  for(int i=0;i<owner.wheelSlots();i++){int xx=x+(i%2)*(cw+8),yy=ry+(i/2)*ch;
   if(mx>=xx&&mx<xx+cw&&my>=yy&&my<yy+ch-5)return i;}return -1;}
 private void label(DrawContext c,String s,int x,int y,int color){NuxFont.drawOn(c,textRenderer,s,x,y,color,BG);}
 @Override public boolean shouldPause(){return false;}
 @Override public void renderBackground(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0xBF090E18);}
 @Override public void render(DrawContext c,int mx,int my,float delta){
  renderBackground(c,mx,my,delta);int w=panelWidth(),x=width/2-w/2,cw=(w-8)/2,ch=rowHeight(),y=top();
  label(c,"Предметы круга AutoSwap",x,y-25,0xFFF0F4FF);
  for(int i=0;i<owner.wheelSlots();i++){
   int xx=x+(i%2)*(cw+8),yy=y+(i/2)*ch;boolean hovered=at(mx,my)==i;
   NuxShapes.roundedRect(c,xx,yy,cw,ch-5,7,hovered||i==dragging?0xFF354469:0xFF1C263C);
   ItemStack item=owner.wheelItem(i);if(!item.isEmpty())c.drawItem(item,xx+8,yy+(ch-21)/2);
   String s=item.isEmpty()?"Выбрать из инвентаря":item.getName().getString();
   while(s.length()>1&&NuxFont.width(textRenderer,s)>cw-45)s=s.substring(0,s.length()-1);
   label(c,(i+1)+". "+s,xx+30,yy+(ch-13)/2,0xFFDFE8FC);
  }
  int footer=y+((owner.wheelSlots()+1)/2)*ch+6;
  label(c,"ЛКМ — выбрать · перетаскивание — порядок · ПКМ — очистить",x,footer,0xFFB9C8E7);
  label(c,"Esc — назад. Число секторов меняется в настройках AutoSwap.",x,footer+14,0xFFAABBD9);
 }
 @Override public boolean mouseClicked(double x,double y,int button){int i=at(x,y);if(i<0)return super.mouseClicked(x,y,button);
  if(button==1){owner.setWheelItem(i,ItemStack.EMPTY);return true;}if(button==0){dragging=i;return true;}return true;}
 @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&dragging>=0){int from=dragging,to=at(x,y);dragging=-1;
  if(to>=0&&to!=from)owner.swapWheelItems(from,to);else if(to==from)client.setScreen(new AutoSwapItemPickerScreen(this,owner,from));return true;}
  return super.mouseReleased(x,y,button);}
 @Override public void close(){client.setScreen(parent);}
}
