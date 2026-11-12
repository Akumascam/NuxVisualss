package com.nuxvisuals.ui;
import net.minecraft.client.MinecraftClient;import net.minecraft.client.gui.DrawContext;import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;import net.minecraft.client.texture.NativeImageBackedTexture;import net.minecraft.util.Identifier;
import java.io.InputStream;import java.util.*;import java.util.concurrent.*;
/** CPU-only atlas cache + two reusable single-frame GPU textures. No external video/native runtime. */
public final class NuxBackground{
 private static final org.slf4j.Logger LOG=com.nuxvisuals.NuxVisuals.LOGGER;
 private static final Object LOCK=new Object();private static volatile long generation;
 private record Decoded(long generation,int page,NativeImage image){}
 private static final Queue<Decoded> READY=new ConcurrentLinkedQueue<>();
 private static final Set<Integer> REQUESTED=new HashSet<>();private static final Map<Integer,Long> RETRY=new HashMap<>();
 private static final Map<Integer,NativeImage> CACHE=new HashMap<>();
 private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(4),r->{Thread t=new Thread(r,"NuxVisuals-MenuDecode");t.setDaemon(true);return t;});
 private static final class Slot{Identifier id;NativeImageBackedTexture texture;int frame=-1;}
 private static final Slot[] GPU={new Slot(),new Slot()};
 private static MenuTheme active;private static long started;private static int lastSlot=-1;
 public static void render(DrawContext c,int w,int h){
  MenuTheme theme=MenuTheme.current();if(theme!=active){release();active=theme;started=System.nanoTime();}
  drain();if(!theme.animated()||!MenuTheme.motion()){if(!CACHE.isEmpty()||!REQUESTED.isEmpty()||GPU[0].texture!=null||GPU[1].texture!=null)clearFrames();poster(c,theme,0,0,w,h);return;}
  long elapsed=System.nanoTime()-started;int frame=MenuLayout.frame(elapsed,theme.frames,theme.fpsNumerator,theme.fpsDenominator),next=MenuLayout.nextFrame(frame,theme.frames);
  int pages=(theme.frames+15)/16,page=MenuLayout.page(frame);for(int i=0;i<3;i++)request(theme,(page+i)%pages);
  int current=find(frame),following=find(next);
  boolean update=current<0&&CACHE.containsKey(page)||following<0&&CACHE.containsKey(MenuLayout.page(next));
  if(update)c.draw(); // Finish any queued GUI draw before reusing a mutable texture.
  if(current<0&&CACHE.containsKey(page)){current=following==0?1:following==1?0:lastSlot>=0?1-lastSlot:0;upload(current,frame,theme);}
  following=find(next);if(following<0&&current>=0&&CACHE.containsKey(MenuLayout.page(next))){following=1-current;upload(following,next,theme);}
  if(current>=0){paint(c,GPU[current].id,theme,w,h,255);lastSlot=current;
   if(following>=0&&following!=current){int alpha=Math.round(255*MenuLayout.blend(elapsed,theme.fpsNumerator,theme.fpsDenominator));if(alpha>0)paint(c,GPU[following].id,theme,w,h,alpha);}
  }else if(lastSlot>=0&&GPU[lastSlot].texture!=null)paint(c,GPU[lastSlot].id,theme,w,h,255);else poster(c,theme,0,0,w,h);
  {
   Set<Integer> keep=Set.of(page,(page+1)%pages,(page+2)%pages);
   var it=CACHE.entrySet().iterator();while(it.hasNext()){var e=it.next();if(!keep.contains(e.getKey())){e.getValue().close();it.remove();}}
  }
 }
 private static int find(int frame){for(int i=0;i<GPU.length;i++)if(GPU[i].texture!=null&&GPU[i].frame==frame)return i;return -1;}
 private static void upload(int index,int frame,MenuTheme theme){
  Slot slot=GPU[index];NativeImage atlas=CACHE.get(MenuLayout.page(frame));if(atlas==null)return;
  if(slot.texture==null){slot.id=Identifier.of("nuxvisuals","menu_stream/"+index);slot.texture=new NativeImageBackedTexture(theme.imageWidth,theme.imageHeight,false);MinecraftClient.getInstance().getTextureManager().registerTexture(slot.id,slot.texture);}
  slot.texture.bindTexture();atlas.upload(0,0,0,MenuLayout.column(frame)*theme.imageWidth,MenuLayout.row(frame)*theme.imageHeight,theme.imageWidth,theme.imageHeight,false);
  slot.texture.setFilter(true,false);slot.texture.setClamp(true);slot.frame=frame;
 }
 private static void paint(DrawContext c,Identifier id,MenuTheme theme,int w,int h,int alpha){
  var b=MenuLayout.cover(w,h,theme.imageWidth,theme.imageHeight);
  c.drawTexture(RenderLayer::getGuiTextured,id,b.x(),b.y(),0f,0f,b.width(),b.height(),theme.imageWidth,theme.imageHeight,theme.imageWidth,theme.imageHeight,(alpha<<24)|0xFFFFFF);
 }
 public static void poster(DrawContext c,MenuTheme theme,int x,int y,int w,int h){
  var b=MenuLayout.cover(w,h,theme.imageWidth,theme.imageHeight);c.enableScissor(x,y,x+w,y+h);
  try{c.drawTexture(RenderLayer::getGuiTextured,Identifier.of("nuxvisuals","textures/menu/"+theme.asset+".png"),x+b.x(),y+b.y(),0f,0f,b.width(),b.height(),theme.imageWidth,theme.imageHeight,theme.imageWidth,theme.imageHeight);}finally{c.disableScissor();}
 }
 private static void request(MenuTheme theme,int page){
  if(CACHE.containsKey(page)||REQUESTED.contains(page)||System.nanoTime()<RETRY.getOrDefault(page,0L))return;
  REQUESTED.add(page);long ticket=generation;
  try{IO.execute(()->{
   if(ticket!=generation)return;NativeImage image=null;
   try(InputStream in=NuxBackground.class.getResourceAsStream("/assets/nuxvisuals/textures/menu/"+theme.asset+"/"+String.format(Locale.ROOT,"%03d",page)+".png")){
    if(in==null)throw new IllegalStateException("Missing animation page");image=NativeImage.read(in);
    if(image.getWidth()!=theme.imageWidth*4||image.getHeight()!=theme.imageHeight*4)throw new IllegalStateException("Invalid animation page size");
    synchronized(LOCK){if(ticket==generation){READY.add(new Decoded(ticket,page,image));image=null;}}
   }catch(Exception error){LOG.warn("Menu page decode failed: "+theme.asset+"/"+page,error);}finally{
    if(image!=null)image.close();synchronized(LOCK){if(ticket==generation)READY.add(new Decoded(ticket,page,null));}
   }
  });}catch(RejectedExecutionException ignored){REQUESTED.remove(page);}
 }
 private static void drain(){Decoded d;while((d=READY.poll())!=null){
  if(d.generation()!=generation){if(d.image()!=null)d.image().close();continue;}
  REQUESTED.remove(d.page());if(d.image()==null){if(!CACHE.containsKey(d.page()))RETRY.put(d.page(),System.nanoTime()+3_000_000_000L);continue;}
  RETRY.remove(d.page());NativeImage old=CACHE.put(d.page(),d.image());if(old!=null)old.close();
 }}
 private static void clearFrames(){
  synchronized(LOCK){generation++;Decoded d;while((d=READY.poll())!=null)if(d.image()!=null)d.image().close();}
  IO.getQueue().clear();REQUESTED.clear();RETRY.clear();for(NativeImage image:CACHE.values())image.close();CACHE.clear();
  for(Slot slot:GPU){if(slot.texture!=null)MinecraftClient.getInstance().getTextureManager().destroyTexture(slot.id);slot.texture=null;slot.id=null;slot.frame=-1;}lastSlot=-1;
 }
 public static void release(){clearFrames();active=null;}
 private NuxBackground(){}
}
