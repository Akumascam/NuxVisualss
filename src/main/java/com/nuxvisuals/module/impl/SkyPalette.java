package com.nuxvisuals.module.impl;
/** Shared fog/sky palette math; components match GLSL base horizon colors. */
public final class SkyPalette {
 private SkyPalette(){}
 private static final int[] HORIZONS={0x4DAFA4,0x657CAD,0x33374F,0xEAA07D,0xA4B9D3};
 public static int horizon(int mode,Integer tint,float brightness){
  int c=HORIZONS[Math.floorMod(mode,HORIZONS.length)];
  float b=Float.isFinite(brightness)?Math.max(.2f,Math.min(1.8f,brightness)):1;
  int out=0;for(int shift:new int[]{16,8,0}){
   float value=(c>>shift)&255;
   if(tint!=null)value*=.45f+.55f*((tint>>shift)&255)/255f;
   out|=Math.min(255,Math.max(0,Math.round(value*b)))<<shift;
  }return out;
 }
}
