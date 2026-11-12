package com.nuxvisuals.module.impl;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
/** Pure profile and name matching logic: no guessed HolyWorld material IDs. */
public final class ServerItemProfiles {
 public enum Profile { FUNTIME, HOLYWORLD }
 private static final Pattern FORMAT=Pattern.compile("§.");
 private static final Pattern SEPARATORS=Pattern.compile("[^\\p{L}\\p{N}]+");
 private ServerItemProfiles(){}
 public static String normalize(String text){
  if(text==null)return "";
  String s=Normalizer.normalize(FORMAT.matcher(text).replaceAll(""),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ё','е');
  return SEPARATORS.matcher(s).replaceAll(" ").strip();
 }
 public static boolean knownAddress(String address){
  String a=address==null?"":address.toLowerCase(Locale.ROOT);
  return a.contains("funtime")||a.contains("holyworld");
 }
 public static Profile resolve(String choice,String address){
  if("HolyWorld".equals(choice))return Profile.HOLYWORLD;
  if("Авто".equals(choice)&&address!=null&&address.toLowerCase(Locale.ROOT).contains("holyworld"))return Profile.HOLYWORLD;
  // Legacy «Любой» remains the FunTime item set; it never merges the two sets.
  return Profile.FUNTIME;
 }
 public static boolean matches(Profile profile,String title,String aliases,String displayName){
  String name=normalize(displayName);
  if(name.isEmpty()||aliases==null)return false;
  if(profile==Profile.HOLYWORLD&&"Трапка".equals(title)){
   for(String word:name.split(" "))if(word.startsWith("взрыв"))return false;
  }
  for(String word:aliases.split("[|,;]")){
   String candidate=normalize(word);if(candidate.isEmpty())continue;
   if(profile==Profile.FUNTIME?name.contains(candidate):(" "+name+" ").contains(" "+candidate+" "))return true;
  }
  return false;
 }
}
