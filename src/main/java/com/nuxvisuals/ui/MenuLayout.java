package com.nuxvisuals.ui;
/** Pure sizing/UV math, independent of Minecraft and screen scale. */
public final class MenuLayout {
 private MenuLayout(){}
 public record Box(int x,int y,int width,int height){}
 public static Box cover(int width,int height,int imageWidth,int imageHeight){
  double scale=Math.max(width/(double)imageWidth,height/(double)imageHeight);
  int w=(int)Math.ceil(imageWidth*scale),h=(int)Math.ceil(imageHeight*scale);
  return new Box((width-w)/2,(height-h)/2,w,h);
 }
 public static int frame(long elapsedNanos,int frames,int fpsNumerator,int fpsDenominator){
  if(frames<=1||fpsNumerator<=0||fpsDenominator<=0)return 0;
  // Native source cadence, including 24000/1001. Split seconds to avoid overflow.
  long elapsed=Math.max(0,elapsedNanos),seconds=elapsed/1_000_000_000L,remainder=elapsed%1_000_000_000L;
  long whole=seconds*fpsNumerator;
  long index=whole/fpsDenominator+((whole%fpsDenominator)*1_000_000_000L+remainder*fpsNumerator)/(fpsDenominator*1_000_000_000L);
  return (int)(index%frames);
 }
 public static int nextFrame(int frame,int frames){return frames<=1?0:(frame+1)%frames;}
 public static float blend(long nanos,int numerator,int denominator){
  if(numerator<=0||denominator<=0)return 0;
  double pos=(Math.max(0,nanos)%60_000_000_000L)*(double)numerator/(denominator*1_000_000_000.0);
  // Whole minutes need to remain part of the fractional phase for NTSC sources.
  pos+=(Math.max(0,nanos)/60_000_000_000L)*(60.0*numerator/denominator);
  return (float)Math.max(0,Math.min(.999999,pos-Math.floor(pos)));
 }
 public static int page(int frame){return frame/16;}
 public static int column(int frame){return frame%4;}
 public static int row(int frame){return (frame%16)/4;}
}
