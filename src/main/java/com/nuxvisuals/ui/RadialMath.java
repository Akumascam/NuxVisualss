package com.nuxvisuals.ui;
public final class RadialMath {
 private RadialMath(){}
 public static int index(double x,double y,double cx,double cy,double inner,double outer,int count){
  if(count<1||!Double.isFinite(x)||!Double.isFinite(y)||inner<0||outer<=inner)return -1;
  double dx=x-cx,dy=y-cy,d=dx*dx+dy*dy;
  if(d<inner*inner||d>outer*outer)return -1;
  double a=Math.atan2(dy,dx)+Math.PI/2;
  if(a<0)a+=Math.PI*2;
  return Math.min(count-1,(int)(a/(Math.PI*2)*count));
 }
 public static double midpoint(int index,int count){return -Math.PI/2+(index+.5)*Math.PI*2/count;}
}
