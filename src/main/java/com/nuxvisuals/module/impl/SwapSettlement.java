package com.nuxvisuals.module.impl;
/** Fast client-side completion, bounded waiting on mismatch; never a server ACK. */
public final class SwapSettlement {
 public enum Result { WAITING, SETTLED, TIMED_OUT }
 private int elapsed;
 private Result result=Result.WAITING;
 public Result tick(boolean swapped){
  if(result!=Result.WAITING)return result;
  elapsed++;
  if(swapped)result=Result.SETTLED;
  else if(elapsed>=60)result=Result.TIMED_OUT;
  return result;
 }
}
