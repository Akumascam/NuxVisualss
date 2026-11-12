package com.nuxvisuals.cosmetics;
import java.util.ArrayList;
import java.util.List;
import static com.nuxvisuals.cosmetics.AtelierGeometry.*;
/** Original, reference-inspired collection. No downloaded models or remote code. */
public final class AtelierCosmetics {
 public static final String TAG="Atelier";
 private AtelierCosmetics(){}
 public static List<Cosmetic> collection(){
  List<Cosmetic> out=new ArrayList<>();
  String[] wings={"Иней • кристаллы","Феникс • перья","Аврора • бабочка","Серафим","Сакура • лепестки","Изумрудный дракон"};
  for(int i=0;i<wings.length;i++){final int style=i;out.add(new Cosmetic("atelier:wings_"+i,wings[i],Category.WINGS,r->wing(r,style),TAG));}
  String[] pets={"Медуза • жемчуг","Кицунэ • мороз","Искорка","Небесный кит","Сова • хранитель","Пчёлка • мёд","Луна • котик","Дракоша • аметист"};
  for(int i=0;i<pets.length;i++){final int style=i;out.add(new Cosmetic("atelier:pet_"+i,pets[i],Category.PET,r->pet(r,style),TAG));}
  String[] hats={"Корона • самоцветы","Неко • наушники","Венок сакуры","Нимб • сияние","Рога • обсидиан","Цилиндр • рубин","Ведьма • звёзды","Кицунэ • маска","Кибервизор","Очки • золото"};
  for(int i=0;i<hats.length;i++){final int style=i;out.add(new Cosmetic("atelier:hat_"+i,hats[i],Category.HAT,r->hat(r,style),TAG));}
  return List.copyOf(out);
 }
 public static List<Cosmetic> extend(List<Cosmetic> previous){List<Cosmetic> out=new ArrayList<>(collection());out.addAll(previous);return out;}
 private static final int WHITE=0xFFF3F0FF,GOLD=0xFFE9BA51,DARK=0xFF26243C;
 private static float sin(double t){return(float)Math.sin(t);}
 private static void wing(Draw r,int style){
  int accent=switch(style){case 0->0xFF9EE9FF;case 1->0xFFFF9A36;case 2->0xFFDDA0FF;case 3->0xFFF4E8CF;case 4->0xFFFF9CCC;default->0xFF53D9A3;};
  r.push();try{
   if(style==2){
    feather(r,1,0,6.2f,1.1f,55,0xFFBA83E7);
    feather(r,1,0,5.8f,1.0f,130,0xFF8FBCED);
    butterflyLobe(r,6.7f,-4,6.4f,4.7f,0xFFBA83E7);
    butterflyLobe(r,4.8f,3.3f,4.3f,3.7f,0xFF8FBCED);
   }else if(style==4){
    // A thick fan of shaped lobes, with veins and bright tips rather than a flat rectangle.
    for(int i=0;i<5;i++){
     r.push();r.rotZ(-47+i*24);r.move(2,0,.12f*i);
     feather(r,0,0,12-Math.abs(i-2)*1.7f,2.1f,0,Draw.mix(accent,WHITE,i*.11f));
     feather(r,0,-.3f,8-Math.abs(i-2),.20f,0,Draw.glow(Draw.mix(accent,WHITE,.55f)));
     gem(r,0,-(10-Math.abs(i-2)*1.6f),.2f,.22f,.7f,Draw.glow(WHITE));r.pop();
    }
   }else if(style==0){
    for(int i=0;i<7;i++){r.push();r.move(1+i*1.5f,1-i*.5f,0);r.rotZ(20+i*8);gem(r,0,-3,0,.8f,7+i*.5f,Draw.mix(accent,WHITE,i*.07f));r.pop();}
    for(int i=0;i<4;i++)gem(r,1+i*2,2+i*.4f,.3f,.5f,4,Draw.glow(accent));
   }else if(style==5){
    for(int i=0;i<5;i++){
     float x=2+i*2.6f,y=-5+Math.abs(i-2)*1.3f;
     triangle(r,0,0,0,x,y,0,x+1,5+i*.4f,.5f,Draw.mix(accent,0xFF123B3B,i*.12f));
     feather(r,x,2,8,.27f,-i*13,Draw.glow(accent));
    }
   }else{
    for(int row=0;row<2;row++)for(int i=0;i<8;i++){
     float u=i/7f;int c=style==1?Draw.mix(0xFFFFD970,0xFFC74630,u):Draw.mix(WHITE,0xFFB7AEC8,u*.6f);
     feather(r,1+i*1.4f,2+row*2,8+sin(u*Math.PI)*4-row*2,.94f,20+i*7,c);
    }
   }
   gem(r,1,.2f,.4f,.65f,2.8f,Draw.glow(accent));
  }finally{r.pop();}
 }
 private static void butterflyLobe(Draw r,float cx,float cy,float rx,float ry,int accent){
  for(int i=0;i<16;i++){
   double a=i*Math.PI/8,b=(i+1)*Math.PI/8;
   float x=(float)Math.cos(a),y=(float)Math.sin(a),xx=(float)Math.cos(b),yy=(float)Math.sin(b);
   int c=Draw.mix(accent,0xFFFFB9E7,.35f+.25f*sin(i*.7));
   triangle(r,cx,cy,.45f,cx+x*rx,cy+y*ry,0,cx+xx*rx,cy+yy*ry,0,c);
   triangle(r,cx,cy,-.25f,cx+xx*rx,cy+yy*ry,0,cx+x*rx,cy+y*ry,0,c);
   r.face(cx+x*rx,cy+y*ry,-.31f,cx+xx*rx,cy+yy*ry,-.31f,cx+xx*(rx-.2f),cy+yy*(ry-.2f),-.31f,cx+x*(rx-.2f),cy+y*(ry-.2f),-.31f,0xFF51486B);
   if(i%2==0){
    r.face(cx-.045f,cy,-.34f,cx+x*(rx-.3f),cy+y*(ry-.3f),-.34f,cx+x*(rx-.3f)+.09f,cy+y*(ry-.3f),-.34f,cx+.045f,cy,-.34f,Draw.glow(0xFFE4E0FF));
    gem(r,cx+x*(rx-.38f),cy+y*(ry-.38f),-.36f,.15f,.4f,Draw.glow(WHITE));
   }
  }
 }
 private static void pet(Draw r,int style){
  r.push();try{
   r.move(0,sin(r.t*.09)*.25f,0);
   switch(style){
    case 0->{ // translucent faceted jelly dome; independently waving tentacles
     ellipsoid(r,0,-1,0,2.8f,1.7f,2.6f,0xCBBF93ED);
     for(int i=0;i<8;i++){
      double a=i*Math.PI/4;float x=(float)Math.cos(a)*1.7f,z=(float)Math.sin(a)*1.7f;
      for(int s=0;s<5;s++){float sway=sin(r.t*.12+i+s*.45)*.4f;r.cube(x+sway,.5f+s*.7f,z,.25f,.72f,.25f,Draw.glow(Draw.mix(0xFFDEA0FA,0xFF8EEBEE,s/4f)));}
     }eye(r,-.8f,-.9f,-2.4f,DARK);eye(r,.8f,-.9f,-2.4f,DARK);
    }
    case 1->{
     ellipsoid(r,0,.2f,0,2,1.5f,2.3f,WHITE);ellipsoid(r,0,-1.8f,-1.2f,1.7f,1.4f,1.5f,WHITE);
     for(int side:new int[]{-1,1}){
      feather(r,side*1.2f,-2.5f,1.6f,.65f,side*12,0xFFA4DFFF);
      eye(r,side*.7f,-1.9f,-2.62f,0xFF399ED5);
     }
     ellipsoid(r,0,-.95f,-2.3f,.65f,.42f,.7f,WHITE);r.cube(0,-1,-3,.28f,.2f,.15f,DARK);
     for(int i=0;i<3;i++){r.push();r.move((i-1)*1.15f,.3f,1.6f);r.rotY((i-1)*28);r.rotX(24+sin(r.t*.08+i)*8);ellipsoid(r,0,-.4f,1.3f,.7f,.9f,1.8f,0xFFB5E9F7);r.pop();}
    }
    case 2->{
     ellipsoid(r,0,0,0,2,2.5f,1.7f,0xFFFFB449);gem(r,0,-2.4f,0,1,3,Draw.glow(0xFFFFD376));
     for(int side:new int[]{-1,1}){eye(r,side*.7f,-.1f,-1.6f,DARK);r.push();r.move(side*1.8f,.5f,0);r.rotZ(side*(35+sin(r.t*.2)*14));feather(r,0,0,2.9f,.8f,0,Draw.glow(0xFFFFE194));r.pop();}
    }
    case 3->{
     ellipsoid(r,0,0,0,3.3f,1.8f,2,0xFF526CAC);ellipsoid(r,-1.7f,.6f,-.35f,1.2f,.85f,1.6f,0xFFE0E5F6);
     eye(r,-1.65f,-.25f,-1.8f,DARK);
     for(int side:new int[]{-1,1}){r.push();r.move(2.5f,0,0);r.rotX(side*30);feather(r,0,0,2.6f,.85f,90,0xFF809FE7);r.pop();}
     for(int i=0;i<7;i++){float x=-1.5f+i*.55f;r.cube(x,-1.4f,-.7f+sin(i*2)*.4f,.16f,.13f,.15f,Draw.glow(WHITE));}
    }
    case 4->{
     ellipsoid(r,0,.3f,0,2,2.1f,1.7f,0xFF957654);
     for(int side:new int[]{-1,1}){ellipsoid(r,side*.85f,-.65f,-1.35f,.95f,1.05f,.5f,0xFFEEDCB3);eye(r,side*.85f,-.65f,-1.86f,DARK);feather(r,side*1.45f,-1.25f,2,.65f,side*25,0xFF795D45);}
     gem(r,0,.05f,-2,.35f,.8f,GOLD);r.box(-1.1f,.8f,-1.7f,1.1f,1.7f,-1.5f,0xFFEAD9BA);
    }
    case 5->{
     ellipsoid(r,0,0,0,2.5f,1.7f,1.7f,0xFFFFCC55);
     for(int i=0;i<3;i++)r.box(-1.2f+i*.9f,-1.55f,-1.4f,-.8f+i*.9f,1.55f,1.4f,0xFF68503B);
     eye(r,-1.5f,-.3f,-1.4f,DARK);eye(r,-.5f,-.3f,-1.7f,DARK);
     for(int side:new int[]{-1,1}){r.push();r.move(0,-1.3f,side*.7f);r.rotX(side*(25+sin(r.t*.7)*20));ellipsoid(r,0,-.7f,side*.9f,1.2f,.2f,1.4f,0xAED9F6FF);r.pop();}
    }
    case 6->{
     r.push();r.move(0,0,.8f);r.rotZ(-22);arc(r,4.1f,4.1f,0,-115,115,.75f,Draw.glow(0xFFFFD875));r.pop();
     ellipsoid(r,0,.6f,-.3f,1.5f,1.7f,1.1f,0xFF353047);ellipsoid(r,0,-1.4f,-.6f,1.6f,1.25f,1.2f,0xFF353047);
     for(int side:new int[]{-1,1}){feather(r,side*1.05f,-2,1.8f,.6f,side*12,0xFF6F5790);eye(r,side*.65f,-1.4f,-1.7f,0xFFB8EDA8);}
    }
    default->{
     ellipsoid(r,0,.1f,.4f,1.8f,1.5f,2.6f,0xFF8D7ED8);ellipsoid(r,0,-1.2f,-1.5f,1.6f,1.4f,1.6f,0xFFB2A3F2);
     for(int side:new int[]{-1,1}){eye(r,side*.7f,-1.25f,-2.9f,DARK);gem(r,side*1.1f,-2.4f,-1, .45f,1.8f,0xFFE9E0FF);r.push();r.move(side*1.1f,0,.8f);r.scale(side*.35f,.35f,.35f);r.rotY(sin(r.t*.18)*15);wing(r,5);r.pop();}
     for(int i=0;i<4;i++)gem(r,0,-.4f,1+i*.8f,.32f,1.6f,Draw.glow(0xFFC7C1FF));
    }
   }
  }finally{r.pop();}
 }
 private static void flower(Draw r,float x,float y,float z,int color){
  r.push();try{r.move(x,y,z);for(int i=0;i<5;i++){r.push();r.rotZ(i*72);feather(r,0,0,1.05f,.34f,0,color);r.pop();}r.cube(0,0,-.2f,.38f,.38f,.25f,GOLD);}finally{r.pop();}
 }
 private static void hat(Draw r,int style){
  switch(style){
   case 0->{
    r.push();r.rotX(90);ring(r,0,0,0,4.4f,4.4f,.7f,GOLD);r.pop();
    for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=(float)Math.cos(a)*4,z=(float)Math.sin(a)*4;gem(r,x,-1.1f,z,.65f,3,0xFFFFD681);gem(r,x,-.3f,z,.34f,.8f,Draw.glow(i%2==0?0xFF9EE6F0:0xFFEC84AC));}
   }
   case 1->{
    arc(r,4.5f,3.6f,1.7f,180,360,.5f,WHITE);
    for(int side:new int[]{-1,1}){
     r.box(side*4.5f-.55f,1,-1.5f,side*4.5f+.55f,4,1.5f,WHITE);
     r.push();r.move(side*5.1f,2.5f,0);r.rotY(side*90);ring(r,0,0,0,.9f,1.1f,.2f,Draw.glow(0xFFFFA7DF));r.pop();
     feather(r,side*2.7f,.1f,3.4f,1.1f,side*13,WHITE);feather(r,side*2.7f,-.4f,2.1f,.55f,side*13,0xFFF2A2D8);
    }
   }
   case 2->{r.push();r.rotX(90);ring(r,0,0,0,4.3f,4.3f,.3f,0xFF76A87D);r.pop();for(int i=0;i<8;i++){double a=i*Math.PI/4;flower(r,(float)Math.cos(a)*4.1f,-.3f,(float)Math.sin(a)*4.1f,0xFFFFB9DC);}}
   case 3->{r.push();r.move(0,-2.8f,0);r.rotX(90);r.rotY(sin(r.t*.04)*8);ring(r,0,0,0,4.1f,4.1f,.4f,Draw.glow(0xFFFFEBC3));r.pop();}
   case 4->{for(int side:new int[]{-1,1})for(int i=0;i<5;i++){r.push();r.move(side*(3.2f+i*.25f),-i*.85f,.5f+i*.25f);r.rotZ(side*(i*7));gem(r,0,-.2f,0,.85f-i*.12f,2,Draw.mix(DARK,0xFFAA536F,i*.15f));r.pop();}}
   case 5->{r.box(-5.2f,-.3f,-5.2f,5.2f,.1f,5.2f,DARK);r.box(-3.5f,-5.7f,-3.5f,3.5f,-.3f,3.5f,0xFF28263A);r.box(-3.6f,-1.5f,-3.6f,3.6f,-.55f,3.6f,0xFF943B59);gem(r,0,-1,-3.85f,.65f,1.4f,Draw.glow(0xFFFF8CA8));}
   case 6->{r.box(-5.2f,-.4f,-5.2f,5.2f,0,5.2f,0xFF4F3879);for(int i=0;i<6;i++){float half=3.6f-i*.47f;r.box(-half+i*.14f,-1.1f-i*.75f,-half,half+i*.14f,-.4f-i*.75f,half,Draw.mix(0xFF634594,DARK,i*.12f));}gem(r,-1.6f,-1,-4,.5f,1.5f,Draw.glow(GOLD));}
   case 7->{
    // Located in front of the face (head-local y=+4 after the HAT anchor's -8 offset).
    r.push();r.move(2.8f,3.2f,-4.65f);r.rotZ(-18);ellipsoid(r,0,0,0,1.45f,2.1f,.35f,WHITE);
    for(int side:new int[]{-1,1}){feather(r,side*.85f,-1.3f,1.65f,.6f,side*10,WHITE);r.box(side*.65f-.25f,-.2f,-.42f,side*.65f+.25f,.05f,-.3f,0xFFCC596A);}gem(r,0,.6f,-.45f,.28f,.65f,0xFFC85868);r.pop();
   }
   case 8->{r.box(-4.2f,2,-4.7f,4.2f,3.7f,-4.15f,DARK);r.box(-3.8f,2.35f,-4.85f,3.8f,3.3f,-4.72f,Draw.glow(0xFF68ECF5));for(int s:new int[]{-1,1})r.box(s*4.1f-.18f,2.3f,-4.3f,s*4.1f+.18f,2.9f,2.5f,DARK);}
   case 9->{for(int side:new int[]{-1,1}){ring(r,side*2.05f,2.9f,-4.62f,1.65f,1.65f,.19f,GOLD);r.box(side*4-.15f,2.5f,-4.4f,side*4+.15f,2.8f,2.4f,GOLD);}r.box(-.6f,2.6f,-4.62f,.6f,2.8f,-4.4f,GOLD);}
  }
 }
}
