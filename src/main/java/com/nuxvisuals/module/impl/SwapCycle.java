package com.nuxvisuals.module.impl;
public final class SwapCycle {
 private SwapCycle() {}
 public static int next(int current,int count,int direction) {
  if(count<1)throw new IllegalArgumentException("Empty cycle");
  int step=direction<0?-1:1;
  return current<0?(step>0?0:count-1):Math.floorMod(current+step,count);
 }
}
