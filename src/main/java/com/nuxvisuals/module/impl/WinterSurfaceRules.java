package com.nuxvisuals.module.impl;
/** Render-only surface eligibility and periodic UVs, with no world mutation. */
public final class WinterSurfaceRules {
 private WinterSurfaceRules(){}
 public static boolean snow(int y,int exposed,boolean leaves,boolean opaqueAbove,boolean fluidAbove){return (y>=exposed||leaves)&&!opaqueAbove&&!fluidAbove;}
 public static boolean ice(int y,int exposed,boolean water,boolean still,boolean fluidAbove){return y>=exposed&&water&&still&&!fluidAbove;}
 public static float uv(double coordinate,double scale){double p=coordinate*scale;return (float)(p-Math.floor(p));}
 public static float edgeAlpha(double distance,double radius){return (float)Math.max(0,Math.min(1,(radius-distance)/6));}
}
