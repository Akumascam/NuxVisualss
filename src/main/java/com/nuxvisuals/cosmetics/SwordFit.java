package com.nuxvisuals.cosmetics;
/** Normalize actual mesh bounds, not a fixed guessed pixel size. */
public final class SwordFit {
 private SwordFit() {}
 public static float scale(float[] b,float target) {
  if(b==null||b.length!=6||!Float.isFinite(target)||target<=0)return 0;
  float extent=0;
  for(int i=0;i<3;i++){float d=b[i+3]-b[i];if(!Float.isFinite(d)||d<0)return target/24;extent=Math.max(extent,d);}
  return target/Math.max(1,extent);
 }
}
