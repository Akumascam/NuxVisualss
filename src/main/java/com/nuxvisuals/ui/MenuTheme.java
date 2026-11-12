package com.nuxvisuals.ui;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
/** Only the six user-supplied media; legacy procedural themes are removed. */
public enum MenuTheme {
 WINTER_FILM("Зимний лес · видео","winter_film",412,700,392,30,1,0xFFCBDDEF,0xFF9EA7D6),
 SUNSET_FILM("Золотой закат · видео","sunset_film",313,736,414,24000,1001,0xFFFFD8A0,0xFFDCAB79),
 CHERRY_FILM("Сакура у воды · видео","cherry_film",300,736,414,30,1,0xFFFFCDE3,0xFFD7A8EF),
 WINTER_PHOTO("Снежная сакура","winter_photo",1,735,422,0,1,0xFFD5E5F6,0xFFAEA2CD),
 SPRING_PHOTO("Весенняя поляна","spring_photo",1,736,414,0,1,0xFFF6D4E5,0xFFB9D693),
 SUNSET_PHOTO("Сакура на закате","sunset_photo",1,735,490,0,1,0xFFFFCDCF,0xFFE8A6C8);
 public final String title,asset;
 public final int frames,imageWidth,imageHeight,fpsNumerator,fpsDenominator,accentA,accentB;
 MenuTheme(String t,String a,int f,int w,int h,int fn,int fd,int ca,int cb){title=t;asset=a;frames=f;imageWidth=w;imageHeight=h;fpsNumerator=fn;fpsDenominator=fd;accentA=ca;accentB=cb;}
 public boolean animated(){return frames>1;}
 private static MenuTheme current;
 private static boolean motion=true;
 private static Path file(){return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals/menu.json");}
 public static MenuTheme current(){
  if(current==null){
   current=CHERRY_FILM;
   boolean migrated=false;
   try{JsonObject o=JsonParser.parseString(Files.readString(file())).getAsJsonObject();
    if(o.has("theme"))try{current=valueOf(o.get("theme").getAsString());}catch(IllegalArgumentException ignored){migrated=true;}
    if(o.has("motion"))motion=o.get("motion").getAsBoolean();
   }catch(Exception ignored){}
   if(migrated)save();
  }
  return current;
 }
 public static void set(MenuTheme theme){if(theme==null)return;current();current=theme;save();}
 public static boolean motion(){current();return motion;}
 public static void setMotion(boolean value){current();motion=value;save();}
 private static void save(){try{Path p=file();Files.createDirectories(p.getParent());JsonObject o=new JsonObject();o.addProperty("theme",current.name());o.addProperty("motion",motion);Files.writeString(p,o.toString());}catch(Exception ignored){}}
 public static void cycle(int direction){MenuTheme[] all=values();set(all[Math.floorMod(current().ordinal()+direction,all.length)]);}
}
