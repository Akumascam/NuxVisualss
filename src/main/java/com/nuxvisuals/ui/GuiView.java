package com.nuxvisuals.ui;
import java.text.Normalizer;import java.util.Locale;import java.util.function.ToIntFunction;
/** Pure GUI layout, search and filtering. No module or rendering side effects. */
public final class GuiView {
 public record Layout(int x,int y,int width,int height,int columns,int cardWidth){}
 public static Layout layout(int sw,int sh){
  int w=Math.max(1,Math.min(560,sw-24)),h=Math.max(1,Math.min(330,sh-20));int content=w-148;
  int columns=content>=300?2:1;return new Layout((sw-w)/2,(sh-h)/2,w,h,columns,Math.max(1,(content-(columns-1)*8)/columns));
 }
 public static String normalize(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ё','е').trim().replaceAll("\\s+"," ");}
 public static boolean matches(String normalizedHaystack,String normalizedQuery){
  if(normalizedQuery.isEmpty())return true;for(String word:normalizedQuery.split(" "))if(!normalizedHaystack.contains(word))return false;return true;
 }
 public static boolean visible(int filter,boolean enabled,boolean favorite,boolean bound){return switch(filter){case 1->enabled;case 2->favorite;case 3->bound;default->true;};}
 public static String fit(String value,int maxWidth,ToIntFunction<String> width){
  if(maxWidth<=0)return "";if(width.applyAsInt(value)<=maxWidth)return value;
  String dot="…";if(width.applyAsInt(dot)>maxWidth)return "";
  int n=value.length();while(n>0&&width.applyAsInt(value.substring(0,n)+dot)>maxWidth)n=value.offsetByCodePoints(n,-1);
  return value.substring(0,n)+dot;
 }
 private GuiView(){}
}
