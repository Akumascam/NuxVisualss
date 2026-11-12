package com.nuxvisuals.music;
import java.util.*;
public final class LyricPresentation {
 private LyricPresentation(){}
 public static List<String> wrap(String input,int limit,int maxLines){
  if(input==null||input.isBlank()||limit<1||maxLines<1)return List.of();
  List<String> result=new ArrayList<>();StringBuilder line=new StringBuilder();
  for(String word:input.strip().split("\\s+")){
   if(line.length()>0&&line.codePointCount(0,line.length())+1+word.codePointCount(0,word.length())>limit){
    result.add(line.toString());line.setLength(0);
    if(result.size()>=maxLines){int last=result.size()-1;result.set(last,result.get(last)+"…");return List.copyOf(result);}
   }
   if(line.length()>0)line.append(' ');line.append(word);
  }
  if(line.length()>0&&result.size()<maxLines)result.add(line.toString());
  return List.copyOf(result);
 }
}
