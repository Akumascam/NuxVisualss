package com.nuxvisuals.cosmetics;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/** Safe declarative models: no scripts, shaders, remote downloads or executable plugins. */
public final class CustomCosmetics {
    private record Piece(float[] from,float[] to,float[] pivot,float[] rotation,int color,Rig.Anchor part) {}
    private CustomCosmetics() {}
    public static Path directory() {return FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals/cosmetics");}
    public static String lastReport="";
    public static List<Cosmetic> readAll() {
        List<Cosmetic> result=new ArrayList<>();int errors=0;
        try {
            Files.createDirectories(directory());
            try(var files=Files.list(directory())) {
                for(Path p:files.filter(f->f.getFileName().toString().endsWith(".json")).sorted().limit(64).toList()) {
                    if(Files.isSymbolicLink(p))continue;
                    try {result.add(read(p));}catch(Exception e){errors++;}
                }
            }
        }catch(Exception e){errors++;}
        lastReport="Загружено: "+result.size()+(errors>0?"; ошибок: "+errors:"");return result;
    }
    private static float[] vector(JsonObject object,String name,float[] fallback) {
        if(!object.has(name))return fallback.clone();
        JsonArray a=object.getAsJsonArray(name);if(a.size()!=3)throw new IllegalArgumentException("Vector size");
        float[] v=new float[3];for(int i=0;i<3;i++) {
            v[i]=a.get(i).getAsFloat();if(!Float.isFinite(v[i]) || Math.abs(v[i])>128)throw new IllegalArgumentException("Vector bounds");
        }return v;
    }
    private static int color(String raw) {
        String value=raw.replace("#","");if(value.length()!=6)throw new IllegalArgumentException("RGB format");
        return 0xFF000000|Integer.parseInt(value,16);
    }
    private static Cosmetic read(Path file) throws Exception {
        if(!Files.isRegularFile(file) || Files.size(file)>131072)throw new IllegalArgumentException("File size");
        JsonObject root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        String name=root.has("name")?root.get("name").getAsString():file.getFileName().toString();
        if(name.isBlank() || name.length()>48)throw new IllegalArgumentException("Name");
        Category category=Category.valueOf(root.get("category").getAsString().toUpperCase(Locale.ROOT));
        JsonArray array=root.getAsJsonArray("boxes");if(array.size()<1 || array.size()>128)throw new IllegalArgumentException("Box limit");
        List<Piece> pieces=new ArrayList<>();
        for(JsonElement entry:array) {
            JsonObject b=entry.getAsJsonObject();float[] from=vector(b,"from",new float[]{-1,-1,-1});
            float[] to=vector(b,"to",new float[]{1,1,1});
            for(int i=0;i<3;i++)if(to[i]<=from[i])throw new IllegalArgumentException("Box dimensions");
            int c=color(b.has("color")?b.get("color").getAsString():"8187FF");
            if(b.has("glow") && b.get("glow").getAsBoolean())c=Draw.glow(c);
            Rig.Anchor part=b.has("part")?Rig.Anchor.valueOf(b.get("part").getAsString().toUpperCase(Locale.ROOT)):Rig.Anchor.BODY;
            pieces.add(new Piece(from,to,vector(b,"pivot",new float[3]),vector(b,"rotation",new float[3]),c,part));
        }
        String animation=root.has("animation")?root.get("animation").getAsString():"none";
        float scale=root.has("scale")?root.get("scale").getAsFloat():1;
        if(!Float.isFinite(scale) || scale<0.1 || scale>3)throw new IllegalArgumentException("Scale");
        String id="custom:"+file.getFileName().toString().replaceAll("[^a-zA-Z0-9_.-]","_");
        return new Cosmetic(id,name,category,r->{
            r.push();try {
                r.scale(scale);
                if(animation.equals("breathe"))r.move(0,(float)Math.sin(r.t*0.06)*0.15f,0);
                else if(animation.equals("spin"))r.rotY(r.t*1.5f);
                for(Piece p:pieces) {
                    if(category==Category.MODEL && r.part!=p.part)continue;
                    r.push();try {
                        r.move(p.pivot[0],p.pivot[1],p.pivot[2]);
                        r.rotX(p.rotation[0]);r.rotY(p.rotation[1]);r.rotZ(p.rotation[2]);
                        r.move(-p.pivot[0],-p.pivot[1],-p.pivot[2]);
                        r.box(p.from[0],p.from[1],p.from[2],p.to[0],p.to[1],p.to[2],p.color);
                    }finally{r.pop();}
                }
            }finally{r.pop();}
        },"Своя");
    }
    private static JsonArray xyz(float... values) {JsonArray a=new JsonArray();for(float v:values)a.add(v);return a;}
    public static Path create(String title,Category category,int rgb) throws Exception {
        String stem=title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");
        if(stem.isBlank() || stem.replace("_","").isBlank())stem="my_cosmetic";
        stem=stem.substring(0,Math.min(40,stem.length()));Files.createDirectories(directory());
        Path out=directory().resolve(stem+".json");int n=1;while(Files.exists(out))out=directory().resolve(stem+"_"+(n++)+".json");
        JsonObject root=new JsonObject();root.addProperty("name",title.isBlank()?"Моя косметика":title.substring(0,Math.min(48,title.length())));
        root.addProperty("category",category.name());root.addProperty("animation",category==Category.PET?"breathe":"none");root.addProperty("scale",1);
        JsonArray boxes=new JsonArray();
        float[][] bounds=switch(category) {
            case CAPE -> new float[][]{{-5,0,0,5,16,0.45f}};
            case WINGS -> new float[][]{{0,0,0,3,3,0.5f},{2,-1,0,6,2,0.5f},{5,-3,0,9,1,0.5f}};
            case BACK -> new float[][]{{-3,1,0,3,9,3},{-2,3,3,2,6,4}};
            case PET -> new float[][]{{-2,-2,-2,2,2,2},{-1.4f,-0.5f,-2.2f,-0.7f,0.2f,-2}};
            case SWORD -> new float[][]{{-0.7f,-13,-0.4f,0.7f,0,0.4f},{-3,0,-0.7f,3,1,0.7f},{-0.6f,1,-0.6f,0.6f,5,0.6f}};
            default -> new float[][]{{-4.3f,-1,-4.3f,4.3f,0,4.3f},{-4.3f,-2,-4.3f,-3.3f,-1,4.3f},{3.3f,-2,-4.3f,4.3f,-1,4.3f}};
        };
        for(float[] b:bounds) {JsonObject box=new JsonObject();box.add("from",xyz(b[0],b[1],b[2]));box.add("to",xyz(b[3],b[4],b[5]));box.addProperty("color",String.format(Locale.ROOT,"%06X",rgb&0xFFFFFF));boxes.add(box);}
        root.add("boxes",boxes);Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);return out;
    }
}
