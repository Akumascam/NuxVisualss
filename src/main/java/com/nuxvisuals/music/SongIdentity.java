package com.nuxvisuals.music;
import java.text.Normalizer;
import java.util.Locale;
public final class SongIdentity {
 private SongIdentity(){}
 public static String title(String raw){
  if(raw==null)return "";
  return raw.replaceAll("(?i)\\s*[\\[(](?:official(?: music)? (?:video|audio)|lyrics?|lyric video|текст песни|официальное видео)[\\])]", "").strip();
 }
 public static String[] split(String artist,String rawTitle){
  String a=artist==null?"":artist.strip(),t=title(rawTitle);
  if(a.isBlank()){
   String[] parts=t.split("\\s+[-–—]\\s+",2);
   if(parts.length==2&&!parts[0].isBlank()&&!parts[1].isBlank()){a=parts[0].strip();t=parts[1].strip();}
  }else{
   for(String sep:new String[]{" - "," – "," — "})if(t.regionMatches(true,0,a+sep,0,a.length()+sep.length())){t=t.substring(a.length()+sep.length()).strip();break;}
  }
  return new String[]{a,t};
 }
 public static String normalize(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("[^\\p{L}\\p{N}]+"," ").strip();}
}
