package com.nuxvisuals.music;
import java.nio.file.*;
import java.util.concurrent.*;
public final class CoverLoader {
 private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"NuxVisuals-Cover");t.setDaemon(true);return t;});
 private Future<byte[]> pending;
 private String key="";
 private long nextTry;
 public byte[] poll(String wanted,Path file){
  if(!wanted.equals(key)){clear();key=wanted;}
  if(pending!=null&&pending.isDone()){
   try{byte[] bytes=pending.get();pending=null;nextTry=System.currentTimeMillis()+800;return bytes;}
   catch(Exception ignored){pending=null;nextTry=System.currentTimeMillis()+800;}
  }
  if(pending==null&&file!=null&&System.currentTimeMillis()>=nextTry){
   pending=IO.submit(()->{
    try(var in=Files.newInputStream(file)){byte[] bytes=in.readNBytes(1_048_577);return bytes.length<=1_048_576?bytes:null;}
   });
  }
  return null;
 }
 public void clear(){if(pending!=null)pending.cancel(true);pending=null;key="";nextTry=0;}
}
