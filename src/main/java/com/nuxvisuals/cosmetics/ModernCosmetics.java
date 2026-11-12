package com.nuxvisuals.cosmetics;

import java.util.ArrayList;
import java.util.List;

/** Lower-poly silhouettes: continuous cloth, tapered feathers, bevelled blades, restrained light. */
public final class ModernCosmetics {
    private ModernCosmetics() {}
    public static List<Cosmetic> upgrade(List<Cosmetic> source) {
        List<Cosmetic> out=new ArrayList<>();
        for(Cosmetic c:source) {
            Cosmetic.Shape shape=c.shape();
            int accent=switch(c.id()) {
                case "cape:flame","wings:phoenix","sword:phoenix" -> 0xFFFF9B5A;
                case "cape:frost","wings:crystal","sword:frost" -> 0xFFA1E3FF;
                case "cape:sakura","wings:butterfly" -> 0xFFF4A8D9;
                case "cape:royal","sword:pharaoh" -> 0xFFE3C476;
                case "wings:demon","sword:partisan" -> 0xFFD66A7F;
                default -> 0xFFA3ACFF;
            };
            if(c.category()==Category.CAPE && c.tag().isEmpty())shape=r->cape(r,accent);
            else if(c.category()==Category.WINGS && c.tag().isEmpty())shape=r->wing(r,accent,c.id().equals("wings:crystal"));
            else if(c.id().equals("hat:crown"))shape=r->crown(r,accent);
            else if(c.id().equals("back:backpack"))shape=ModernCosmetics::backpack;
            else if(c.id().equals("pet:crystal"))shape=r->crystal(r,accent);
            out.add(new Cosmetic(c.id(),c.name(),c.category(),shape,c.tag()));
        }
        return out;
    }
    private static int shade(int c,float f){return com.nuxvisuals.render.RenderUtil.shade(c,f);}
    private static void cape(Draw r,int accent) {
        r.push();try {
            float speed=Math.min(1,r.d.speed*3);
            r.rotX(6+speed*25+(r.d.sneaking?12:0));
            for(int row=0;row<8;row++) {
                float y=row*2, next=y+2;
                float z=(float)Math.sin(row*0.32+r.t*0.035)*(0.12f+speed*0.35f)+row*0.04f;
                float zn=(float)Math.sin((row+1)*0.32+r.t*0.035)*(0.12f+speed*0.35f)+(row+1)*0.04f;
                int cloth=Draw.mix(0xFF20283F,accent,row/7f*0.32f);
                r.face(-5,y,z,5,y,z,5,next,zn,-5,next,zn,cloth);
                r.face(5,y,z+0.25f,-5,y,z+0.25f,-5,next,zn+0.25f,5,next,zn+0.25f,shade(cloth,0.9f));
                r.box(-5.1f,y,z-0.02f,-4.8f,next,Math.max(z,zn)+0.3f,accent);
                r.box(4.8f,y,z-0.02f,5.1f,next,Math.max(z,zn)+0.3f,accent);
            }
            // Small emblem rather than noisy 160-pixel extrusion.
            r.box(-1.6f,3,-0.08f,-0.9f,7,0.2f,accent);
            r.box(0.9f,3,-0.08f,1.6f,7,0.2f,accent);
            r.push();r.move(0,5,0);r.rotZ(-28);r.box(-0.4f,-2,-0.1f,0.4f,2,0.15f,accent);r.pop();
        }finally{r.pop();}
    }
    private static void wing(Draw r,int accent,boolean crystal) {
        int n=crystal?6:9;
        for(int i=0;i<n;i++) {
            float u=i/(float)(n-1), x=1.2f+u*10.5f;
            float top=-1.8f-(float)Math.sin(u*Math.PI)*5.2f;
            float end=3.6f+u*5.6f;
            float width=crystal?2.1f:1.45f;
            int c=Draw.mix(0xFFE7EBFA,accent,u*0.7f);
            r.face(x-width*0.5f,top,0,x+width*0.5f,top-0.5f,0.2f,x+width*0.15f,end,0.7f,x-width*0.2f,end+1,0.3f,c);
            r.face(x-width*0.5f,top,0,x-width*0.2f,end+1,0.3f,x,end-0.7f,1,x+width*0.5f,top-0.5f,0.2f,shade(c,0.78f));
        }
        r.box(0,-0.5f,-0.3f,2,2,0.5f,shade(accent,0.65f));
    }
    private static void crown(Draw r,int accent) {
        r.box(-4.2f,-1,-4.2f,4.2f,0,-3.5f,accent);r.box(-4.2f,-1,3.5f,4.2f,0,4.2f,accent);
        r.box(-4.2f,-1,-3.5f,-3.5f,0,3.5f,accent);r.box(3.5f,-1,-3.5f,4.2f,0,3.5f,accent);
        for(int i=-1;i<=1;i++) {
            float x=i*3;
            r.face(x-0.8f,-1,-4.2f,x,-3.2f,-4.2f,x+0.8f,-1,-4.2f,x+0.8f,-1,-3.5f,accent);
        }
        r.box(-0.45f,-1.3f,-4.35f,0.45f,-0.4f,-4.15f,0xFFB9E8FF);
    }
    private static void backpack(Draw r) {
        r.box(-3,1,0,3,9,2.6f,0xFF343D56);r.box(-2.8f,1.2f,2.6f,2.8f,8.7f,3.1f,0xFF47536D);
        r.box(-2.2f,4.7f,3.1f,2.2f,7.6f,3.6f,0xFF65769A);r.box(-0.6f,2.5f,3.1f,0.6f,4,3.3f,0xFFCCDAEB);
        for(int side:new int[]{-1,1})r.box(side*2-0.3f,0.5f,-0.2f,side*2+0.3f,9.2f,0.1f,0xFF222A3D);
    }
    private static void crystal(Draw r,int accent) {
        float bob=(float)Math.sin(r.t*0.06)*0.25f;r.push();r.move(0,bob,0);r.rotY(r.t*0.6f);
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2,b=(i+1)*Math.PI/2;
            float x=(float)Math.cos(a)*2,z=(float)Math.sin(a)*2,xx=(float)Math.cos(b)*2,zz=(float)Math.sin(b)*2;
            r.face(0,-3,0,x,0,z,xx,0,zz,0,-3,0,i%2==0?accent:shade(accent,0.7f));
            r.face(0,3,0,xx,0,zz,x,0,z,0,3,0,shade(accent,0.6f));
        }r.pop();
    }
}
