package com.nuxvisuals.waypoint;

// Одна точка (вейпоинт)
public class Waypoint {
    public String name;
    public int x;
    public int y;
    public int z;
    public String dimension;
    public float hue;
    public boolean visible = true;
    public int color;

    public Waypoint(String name, int x, int y, int z, String dimension, float hue) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.hue = hue;
        this.color = com.nuxvisuals.render.RenderUtil.hsv(hue, 0.65f, 1f) & 0xFFFFFF;
    }
}
