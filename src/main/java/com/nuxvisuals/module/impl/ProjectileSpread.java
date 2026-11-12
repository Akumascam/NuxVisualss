package com.nuxvisuals.module.impl;

/** Standard crossbow cone: center, left, right. Loaded component determines actual shot count. */
public final class ProjectileSpread {
    private ProjectileSpread() {}
    public static float angle(int index, int count) {
        if (count <= 1) return 0f;
        if (count == 3) return index == 0 ? 0f : index == 1 ? -10f : 10f;
        return -10f + index * 20f / (count - 1);
    }
}
