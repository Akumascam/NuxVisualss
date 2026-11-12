package com.nuxvisuals.music;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Optional local Windows TTS. Lyrics are data sent as Base64, never executable shell text. */
public final class LyricsSpeech {
 private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"NuxVisuals-LyricSpeech");t.setDaemon(true);return t;});
 private static final AtomicReference<String> LATEST=new AtomicReference<>();
 private static final AtomicBoolean DRAINING=new AtomicBoolean();
 private static volatile Process process;
 private static PrintWriter writer;
 private static long retryAt;
 public static volatile String error="";
 private LyricsSpeech(){}
 public static void speak(String line){if(MediaBridge.WINDOWS&&line!=null&&!line.isBlank())queue("SPEAK:"+Base64.getEncoder().encodeToString(line.substring(0,Math.min(1000,line.length())).getBytes(StandardCharsets.UTF_8)));}
 public static void stop(){if(MediaBridge.WINDOWS)queue("STOP");}
 private static void queue(String message){LATEST.set(message);drain();}
 private static void drain(){
  if(!DRAINING.compareAndSet(false,true))return;
  IO.execute(()->{
   try{
    String message;
    while((message=LATEST.getAndSet(null))!=null){
     if(message.equals("STOP")&&(process==null||!process.isAlive()))continue;
     if((process==null||!process.isAlive())&&!start())continue;
     writer.println(message);writer.flush();
     if(writer.checkError()){process.destroy();process=null;error="Озвучка недоступна";}
    }
   }finally{DRAINING.set(false);if(LATEST.get()!=null)drain();}
  });
 }
 private static boolean start(){
  if(System.currentTimeMillis()<retryAt)return false;
  retryAt=System.currentTimeMillis()+10_000;
  try{
   Path dir=Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"),"nuxvisuals-speech-"+ProcessHandle.current().pid()));
   Path script=dir.resolve("speech.ps1");
   try(InputStream in=LyricsSpeech.class.getResourceAsStream("/assets/nuxvisuals/scripts/speech.ps1")){
    if(in==null)throw new IOException("Missing speech script");Files.copy(in,script,StandardCopyOption.REPLACE_EXISTING);
   }
   process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",script.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
   writer=new PrintWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.US_ASCII));error="";return true;
  }catch(Exception e){error="Нет доступного голоса Windows";return false;}
 }
 private static synchronized void shutdown(){if(process!=null)process.destroy();}
 static{Runtime.getRuntime().addShutdownHook(new Thread(LyricsSpeech::shutdown));}
}
