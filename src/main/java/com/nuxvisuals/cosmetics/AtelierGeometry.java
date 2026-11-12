package com.nuxvisuals.cosmetics;
/** Faceted volumes and tapered prisms, all routed through the existing single consumer. */
final class AtelierGeometry {
 private AtelierGeometry(){}
 static void triangle(Draw r,float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,int c){r.face(ax,ay,az,bx,by,bz,cx,cy,cz,cx,cy,cz,c);}
 static void gem(Draw r,float x,float y,float z,float radius,float length,int color){
  for(int i=0;i<6;i++){
   double a=i*Math.PI/3,b=(i+1)*Math.PI/3;
   float ax=x+(float)Math.cos(a)*radius,az=z+(float)Math.sin(a)*radius,bx=x+(float)Math.cos(b)*radius,bz=z+(float)Math.sin(b)*radius;
   int c=Draw.mix(color,0xFFFFFFFF,(i%3)*.1f);
   triangle(r,x,y-length*.65f,z,ax,y,az,bx,y,bz,c);
   triangle(r,x,y+length*.35f,z,bx,y,bz,ax,y,az,Draw.mix(color,0xFF1B2546,.18f));
  }
 }
 static void ellipsoid(Draw r,float x,float y,float z,float rx,float ry,float rz,int color){
  for(int row=0;row<5;row++)for(int col=0;col<10;col++){
   double p=-Math.PI/2+row*Math.PI/5,q=p+Math.PI/5,a=col*Math.PI/5,b=a+Math.PI/5;
   r.face(x+(float)(Math.cos(p)*Math.cos(a))*rx,y+(float)Math.sin(p)*ry,z+(float)(Math.cos(p)*Math.sin(a))*rz,
    x+(float)(Math.cos(q)*Math.cos(a))*rx,y+(float)Math.sin(q)*ry,z+(float)(Math.cos(q)*Math.sin(a))*rz,
    x+(float)(Math.cos(q)*Math.cos(b))*rx,y+(float)Math.sin(q)*ry,z+(float)(Math.cos(q)*Math.sin(b))*rz,
    x+(float)(Math.cos(p)*Math.cos(b))*rx,y+(float)Math.sin(p)*ry,z+(float)(Math.cos(p)*Math.sin(b))*rz,color);
  }
 }
 static void ring(Draw r,float x,float y,float z,float rx,float ry,float thickness,int color){
  for(int i=0;i<20;i++){
   double a=i*Math.PI/10,b=(i+1)*Math.PI/10;
   float c=(float)Math.cos(a),s=(float)Math.sin(a),cc=(float)Math.cos(b),ss=(float)Math.sin(b);
   r.face(x+c*rx,y+s*ry,z,x+cc*rx,y+ss*ry,z,x+cc*(rx-thickness),y+ss*(ry-thickness),z,x+c*(rx-thickness),y+s*(ry-thickness),z,color);
   r.face(x+c*(rx-thickness),y+s*(ry-thickness),z+.22f,x+cc*(rx-thickness),y+ss*(ry-thickness),z+.22f,x+cc*rx,y+ss*ry,z+.22f,x+c*rx,y+s*ry,z+.22f,color);
   r.face(x+c*rx,y+s*ry,z,x+c*rx,y+s*ry,z+.22f,x+cc*rx,y+ss*ry,z+.22f,x+cc*rx,y+ss*ry,z,color);
  }
 }
 static void arc(Draw r,float rx,float ry,float cy,float from,float to,float thickness,int color){
  for(int i=0;i<24;i++){
   double a=Math.toRadians(from+(to-from)*i/24),b=Math.toRadians(from+(to-from)*(i+1)/24);
   float x=(float)Math.cos(a),y=(float)Math.sin(a),xx=(float)Math.cos(b),yy=(float)Math.sin(b);
   r.face(x*rx,cy+y*ry,-.25f,xx*rx,cy+yy*ry,-.25f,xx*(rx-thickness),cy+yy*(ry-thickness),-.25f,x*(rx-thickness),cy+y*(ry-thickness),-.25f,color);
   r.face(x*(rx-thickness),cy+y*(ry-thickness),.25f,xx*(rx-thickness),cy+yy*(ry-thickness),.25f,xx*rx,cy+yy*ry,.25f,x*rx,cy+y*ry,.25f,color);
   r.face(x*rx,cy+y*ry,-.25f,x*rx,cy+y*ry,.25f,xx*rx,cy+yy*ry,.25f,xx*rx,cy+yy*ry,-.25f,color);
  }
 }
 static void feather(Draw r,float x,float y,float length,float width,float angle,int c){
  r.push();try{r.move(x,y,0);r.rotZ(angle);
   r.face(0,0,-.15f,-width,-length*.45f,0,0,-length,.12f,0,-length*.45f,.5f,c);
   r.face(0,0,-.15f,width,-length*.45f,0,0,-length,.12f,0,-length*.45f,.5f,Draw.mix(c,0xFFFFFFFF,.16f));
   r.face(0,0,-.15f,0,-length,.12f,-width,-length*.45f,0,-width,-length*.45f,0,Draw.mix(c,0xFF35405C,.16f));
   r.face(0,0,-.15f,width,-length*.45f,0,0,-length,.12f,0,-length,.12f,c);
  }finally{r.pop();}
 }
 static void eye(Draw r,float x,float y,float z,int color){r.cube(x,y,z,.58f,.8f,.18f,color);r.cube(x-.12f,y-.17f,z-.12f,.18f,.22f,.12f,0xFFFFFFFF);}
}
