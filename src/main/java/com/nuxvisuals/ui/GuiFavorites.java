package com.nuxvisuals.ui;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;
/** Local UI-only preferences, independent of gameplay configs. Bounded reads and atomic writes. */
public final class GuiFavorites {
 private final Path file;private final Set<String> ids=new HashSet<>();
 public GuiFavorites(Path file){this.file=file;load();}
 private static boolean valid(String s){return s!=null&&s.matches("[a-zA-Z0-9_:. -]{1,128}");}
 private void load(){try{if(!Files.isRegularFile(file)||Files.size(file)>65536)return;Properties p=new Properties();try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){p.load(reader);}for(String key:p.stringPropertyNames())if(key.startsWith("favorite.")&&p.getProperty(key).equals("true")){String id=key.substring(9);if(valid(id)&&ids.size()<512)ids.add(id);}}catch(IOException|IllegalArgumentException|SecurityException ignored){}}
 public boolean contains(String id){return ids.contains(id);}
 public boolean toggle(String id){if(!valid(id))return false;if(!ids.remove(id)){if(ids.size()>=512)return false;ids.add(id);}return save();}
 public Set<String> ids(){return Set.copyOf(ids);}
 private boolean save(){Path temp=null;try{Path parent=file.toAbsolutePath().getParent();Files.createDirectories(parent);temp=Files.createTempFile(parent,"nux-gui-",".tmp");Properties p=new Properties();for(String id:ids)p.setProperty("favorite."+id,"true");try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){p.store(writer,"NuxVisuals GUI favorites");}try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}return true;}catch(IOException|SecurityException e){return false;}finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}}
}
