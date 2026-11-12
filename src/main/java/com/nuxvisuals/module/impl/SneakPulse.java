package com.nuxvisuals.module.impl;

/** Tick-based pulse. Never edits KeyBinding, so held/toggle-sneak is preserved. */
public final class SneakPulse {
    private int remaining;
    public void start(int ticks) { remaining = Math.max(1, ticks); }
    public boolean sample(boolean manual) {
        if (remaining <= 0) return manual;
        remaining--;
        return true;
    }
    public void reset() { remaining = 0; }
}
