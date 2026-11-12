package com.nuxvisuals.module.impl;

/** Alternating left/right scene anchors, kept in the forward hemisphere. */
public final class LyricPlacement {
    private LyricPlacement() {}
    public static float angle(int slot, int count) {
        if (count <= 1) return 0;
        float[] angles = {-38f, 38f, -18f, 18f, -58f, 58f};
        return angles[Math.floorMod(slot, angles.length)];
    }
    public static float height(int slot) {
        return switch (Math.floorMod(slot, 6)) {
            case 0, 1 -> 0.15f;
            case 2, 3 -> -0.25f;
            default -> 0.5f;
        };
    }
}
