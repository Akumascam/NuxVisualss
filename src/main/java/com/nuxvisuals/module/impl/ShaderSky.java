package com.nuxvisuals.module.impl;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.block.enums.CameraSubmersionType;
/** A real GLSL sky pass; no fullscreen post-effect over the terrain. */
public final class ShaderSky extends Module {
 public static ShaderSky INSTANCE;
 private final ModeSetting mode=add(new ModeSetting("Режим","Аврора","Аврора","Туманность","Галактика","Закат","Облака"));
 private final ModeSetting palette=add(new ModeSetting("Палитра","Пресет","Пресет","Свой"));
 private final ColorSetting tint=add(new ColorSetting("Цвет",.56f,.55f).visibleIf(()->palette.is("Свой")));
 private final SliderSetting brightness=add(new SliderSetting("Яркость",100,20,180,5,"%"));
 private final SliderSetting speed=add(new SliderSetting("Скорость",1,.1,3,.1,"x"));
 private final SliderSetting stars=add(new SliderSetting("Звёзды",60,0,150,5,"%"));
 private final SliderSetting horizon=add(new SliderSetting("Слияние с горизонтом",100,0,100,5,"%"));
 private final BoolSetting clouds=add(new BoolSetting("Скрыть облака",true));
 private final ModeSetting quality=add(new ModeSetting("Качество","Высокое","Низкое","Высокое"));
 private long started=System.nanoTime();
 public ShaderSky(){super("shader-sky","ShaderSky",Category.Visuals,false);INSTANCE=this;}
 @Override public void onEnable(){started=System.nanoTime();com.nuxvisuals.render.ShaderSkyRenderer.retry();}
 public static boolean active(){
  var m=INSTANCE;var mc=MinecraftClient.getInstance();
  return m!=null&&m.isEnabled()&&mc.world!=null&&mc.gameRenderer!=null
   &&mc.world.getDimensionEffects().getSkyType()==DimensionEffects.SkyType.NORMAL
   &&mc.gameRenderer.getCamera().getSubmersionType()==CameraSubmersionType.NONE;
 }
 public static boolean hideClouds(){return active()&&INSTANCE.clouds.get();}
 public int effect(){return mode.index();}
 public float brightness(){return brightness.getFloat()/100;}
 public float stars(){return stars.getFloat()/100;}
 public float horizon(){return horizon.getFloat()/100;}
 public int octaves(){return quality.is("Высокое")?4:2;}
 public float seconds(){return (float)(((System.nanoTime()-started)/1e9*speed.get())%4096);}
 public int tint(){return palette.is("Свой")?tint.get():0xFFFFFFFF;}
 public int horizonColor(){
  int rgb=SkyPalette.horizon(mode.index(),palette.is("Свой")?tint.get():null,brightness());return rgb;
 }
 public static Integer fogColor(){return active()&&INSTANCE.horizon()>0?INSTANCE.horizonColor():null;}
}
