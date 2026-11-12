package com.nuxvisuals.ui;
import java.util.*;import java.util.regex.*;
/** Display-only replacement. Rendering indices are mapped back to the unchanged original input. */
public final class NicknameMask{
 private static String cachedName="";private static Pattern cachedPattern;
 private static synchronized Pattern pattern(String name){if(cachedPattern==null||!name.equals(cachedName)){cachedName=name;cachedPattern=Pattern.compile(Pattern.quote(name),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);}return cachedPattern;}
 private record Span(int sourceStart,int sourceEnd,int displayStart,int displayEnd){}
 public static final class Mapped{
  private final String source,text;private final List<Span> spans;
  private Mapped(String source,String text,List<Span> spans){this.source=source;this.text=text;this.spans=spans;}
  public String text(){return text;}
  public boolean changed(){return !source.equals(text);}
  /** Original UTF-16 index of a rendered codepoint, bounded for trimToWidth / cursor users. */
  public int sourceIndex(int index,int codepointLength){
   index=Math.max(0,Math.min(text.length(),index));int delta=0,value=index;
   for(Span s:spans){
    if(index<s.displayStart())break;
    if(index<s.displayEnd()){
     value=s.sourceStart()+(int)((long)(index-s.displayStart())*(s.sourceEnd()-s.sourceStart())/Math.max(1,s.displayEnd()-s.displayStart()));
     return sourceBoundary(Math.min(value,Math.max(0,source.length()-codepointLength)));
    }
    delta+=(s.sourceEnd()-s.sourceStart())-(s.displayEnd()-s.displayStart());value=index+delta;
   }
   return sourceBoundary(Math.max(0,Math.min(Math.max(0,source.length()-codepointLength),index+delta)));
  }
  private int sourceBoundary(int value){value=Math.max(0,Math.min(source.length(),value));if(value>0&&value<source.length()&&Character.isLowSurrogate(source.charAt(value))&&Character.isHighSurrogate(source.charAt(value-1)))value--;return value;}
  public int displayIndex(int index){
   index=Math.max(0,Math.min(source.length(),index));int delta=0,value=index;
   for(Span s:spans){
    if(index<s.sourceStart())break;
    if(index<s.sourceEnd()){value=s.displayStart()+(int)((long)(index-s.sourceStart())*(s.displayEnd()-s.displayStart())/Math.max(1,s.sourceEnd()-s.sourceStart()));return displayBoundary(value);}
    delta+=(s.displayEnd()-s.displayStart())-(s.sourceEnd()-s.sourceStart());
   }
   return displayBoundary(index+delta);
  }
  private int displayBoundary(int value){value=Math.max(0,Math.min(text.length(),value));if(value>0&&value<text.length()&&Character.isLowSurrogate(text.charAt(value))&&Character.isHighSurrogate(text.charAt(value-1)))value--;return value;}
  public String slice(int start,int length){int from=Math.max(0,Math.min(source.length(),start));int end=(int)Math.min(source.length(),(long)from+Math.max(0,length));return text.substring(displayIndex(from),displayIndex(end));}
 }
 public static Mapped map(String text,String name,String replacement){
  if(text==null)text="";if(name==null||name.isEmpty())return new Mapped(text,text,List.of());
  String alias=replacement==null||replacement.isBlank()?"NuxVisual":replacement;
  alias=alias.replaceAll("§.","").replaceAll("[\\r\\n\\p{Cc}]","");if(alias.isBlank())alias="NuxVisual";
  if(alias.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))){alias="NuxVisual";if(alias.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))alias="Protected";if(alias.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))alias="Hidden";if(alias.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))alias="•";}
  Matcher m=pattern(name).matcher(text);if(!m.find())return new Mapped(text,text,List.of());
  StringBuilder out=new StringBuilder();List<Span> spans=new ArrayList<>();int end=0;
  do{out.append(text,end,m.start());int begin=out.length();out.append(alias);spans.add(new Span(m.start(),m.end(),begin,out.length()));end=m.end();}while(m.find());out.append(text,end,text.length());
  return new Mapped(text,out.toString(),List.copyOf(spans));
 }
 /** Legacy flag is ignored intentionally: nicknames no longer acquire padding or clipping. */
 public static String apply(String text,String name,String replacement,boolean fixedLength){return text==null?null:map(text,name,replacement).text();}
 private NicknameMask(){}
}
