package com.nuxvisuals.cosmetics;
import java.util.List;
/** Complete articulated outfits with six body attachments. */
final class NewCharacterModels {
 private NewCharacterModels(){}
 static void addAll(List<Cosmetic> list){
  list.add(new Cosmetic("model:astral_guard","Астральный страж",Category.MODEL,r->outfit(r,0),"Новое"));
  list.add(new Cosmetic("model:tech_ronin","Техно-ронин",Category.MODEL,r->outfit(r,1),"Новое"));
  list.add(new Cosmetic("model:frost_wanderer","Ледяной странник",Category.MODEL,r->outfit(r,2),"Новое"));
  list.add(new Cosmetic("pet:orbit_drone","Орбитальный дрон",Category.PET,NewCharacterModels::drone,"Новое"));
 }
 private static void outfit(Draw r,int style){
  int plate=style==0?0xFF596D91:style==1?0xFF283044:0xFFD5E3EA;
  int dark=style==0?0xFF25334F:style==1?0xFF111724:0xFF506B83;
  int trim=style==0?0xFFB7C9EC:style==1?0xFFB95069:0xFF8BD4E2;
  int light=Draw.glow(style==0?0xFF8FE3EE:style==1?0xFFFF9F91:0xFFD2FFFF);
  if(r.part==null)return;
  switch(r.part){
   case HEAD->head(r,style,plate,dark,trim,light);
   case BODY->body(r,style,plate,dark,trim,light);
   case RIGHT_ARM,LEFT_ARM->{
    float x=r.part==Rig.Anchor.RIGHT_ARM?-3:-1;
    r.box(x,-2,-2,x+4,10,2,dark);
    r.box(x-.2f,-2.4f,-2.4f,x+4.2f,1.7f,2.4f,plate);
    r.box(x+.2f,1.8f,-2.25f,x+3.8f,6,2.25f,plate);
    r.box(x-.12f,6.4f,-2.32f,x+4.12f,8.4f,2.32f,trim);
    r.box(x+.6f,-1.3f,-2.5f,x+3.4f,-.9f,-2.4f,light);
    if(style==1&&r.part==Rig.Anchor.RIGHT_ARM)r.box(x-.4f,-2.8f,-2.6f,x+4.3f,-1.8f,2.6f,trim);
   }
   case RIGHT_LEG,LEFT_LEG->{
    r.box(-1.9f,0,-1.9f,1.9f,12,1.9f,dark);r.box(-1.75f,.6f,-2.12f,1.75f,5.5f,2,plate);
    r.box(-1.5f,5.5f,-2.45f,1.5f,7.4f,-1.9f,trim);r.box(-2,8,-2.2f,2,12,2.1f,plate);
    r.box(-2.02f,11.4f,-2.7f,2.02f,12.2f,2.12f,dark);
    if(style==2)r.box(-1.4f,8.5f,-2.35f,1.4f,8.9f,-2.2f,light);
   }
  }
 }
 private static void head(Draw r,int style,int plate,int dark,int trim,int light){
  r.box(-4,-8,-4,4,0,4,dark);r.box(-4.3f,-8.25f,-3.8f,4.3f,-6.5f,4.25f,plate);
  r.box(-4.25f,-6.5f,-3.9f,-3.35f,0,4.15f,plate);r.box(3.35f,-6.5f,-3.9f,4.25f,0,4.15f,plate);
  r.box(-3.4f,-6.5f,-4.2f,3.4f,-2.8f,-3.95f,0xFF111E30);
  r.box(-3,-5.4f,-4.3f,3,-4.85f,-4.18f,light);r.box(-3.4f,-2.9f,-4.3f,3.4f,-.5f,-3.95f,plate);
  if(style==0){
   r.box(-.45f,-8.9f,-3.3f,.45f,-6.6f,3.5f,trim);
   r.face(-3.4f,-2.8f,-4.25f,0,-1,-4.9f,3.4f,-2.8f,-4.25f,0,0,-4.5f,trim);
  }else if(style==1){
   r.box(-4.5f,-7.6f,-4.35f,4.5f,-6.7f,3.8f,trim);r.box(4,-7.4f,3,5,-5.8f,5,trim);
   r.box(4.5f,-6.8f,4.5f,5.5f,-3,5.3f,dark);r.box(-3,-2.7f,-4.4f,3,-2.1f,-4.25f,trim);
  }else{
   r.box(-4.4f,-8.6f,-4.1f,4.4f,-7.7f,4.4f,0xFFF0F7F8);
   r.box(-4.45f,-7.7f,-4.05f,-3.5f,.2f,4.4f,plate);r.box(3.5f,-7.7f,-4.05f,4.45f,.2f,4.4f,plate);
   r.box(-3.5f,-.7f,-4.1f,3.5f,.7f,3.8f,trim);
  }
 }
 private static void body(Draw r,int style,int plate,int dark,int trim,int light){
  r.box(-4,0,-2,4,12,2,dark);r.box(-4.25f,.7f,-2.5f,4.25f,8.6f,2.5f,plate);
  r.box(-3.8f,8.6f,-2.6f,3.8f,10,2.6f,dark);r.box(-4.1f,10,-2.65f,4.1f,11.2f,2.65f,trim);
  r.box(-.9f,10.1f,-2.85f,.9f,11.1f,-2.6f,0xFFDEE5EF);
  if(style==0){
   r.face(-3.6f,1.5f,-2.55f,0,2.8f,-3.2f,0,7.5f,-3,-3.2f,5.8f,-2.55f,trim);
   r.face(3.6f,1.5f,-2.55f,3.2f,5.8f,-2.55f,0,7.5f,-3,0,2.8f,-3.2f,plate);
   r.face(0,2.7f,-3.3f,.7f,4.2f,-3.3f,0,5.7f,-3.3f,-.7f,4.2f,-3.3f,light);
  }else if(style==1){
   r.box(-4.1f,0,-2.8f,4.1f,1.6f,2.7f,trim);r.push();
   try{r.move(0,4,-2.65f);r.rotZ(-28);r.box(-.6f,-5,-.1f,.6f,5,.1f,trim);}finally{r.pop();}
   for(int i=0;i<3;i++)r.box(-3.7f,3+i*2,-2.72f,3.7f,3.6f+i*2,-2.5f,dark);
   r.box(2.5f,1.8f,2.5f,3.3f,11.5f,3.3f,dark);
  }else{
   r.box(-3.9f,1,-2.75f,-.5f,8,-2.5f,0xFFEAF4F6);r.box(.5f,1,-2.75f,3.9f,8,-2.5f,0xFFEAF4F6);
   r.box(-.25f,2,-2.8f,.25f,8.7f,-2.6f,light);r.box(-4.2f,10.7f,-2.55f,-.5f,14,2.55f,plate);
   r.box(.5f,10.7f,-2.55f,4.2f,14,2.55f,plate);
  }
 }
 private static void drone(Draw r){
  r.rotY(r.t*.5f);r.box(-2,-1.3f,-1.5f,2,1.3f,1.5f,0xFF39465F);
  r.box(-1.6f,-1.4f,-1.55f,1.6f,-.8f,1.55f,0xFFB8C9E5);r.box(-1,-.5f,-1.8f,1,.3f,-1.5f,Draw.glow(0xFF8CE6ED));
  for(int side=-1;side<=1;side+=2){
   r.box(side*3.8f-.9f,-.8f,-.9f,side*3.8f+.9f,-.4f,.9f,0xFF8395B4);
   r.box(Math.min(0,side*3),-.5f,-.25f,Math.max(0,side*3),.2f,.25f,0xFF39465F);
   r.push();try{r.move(side*3.8f,-1,0);r.rotY(r.t*25);r.box(-1.8f,-.1f,-.2f,1.8f,.1f,.2f,0xFFB8C9E5);}finally{r.pop();}
  }
 }
}
