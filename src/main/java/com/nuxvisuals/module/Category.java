package com.nuxvisuals.module;

public enum Category {
    Visuals("Visuals"),
    Hud("Display"),
    Utilities("Utils"),
    Waypoint("Waypoint"),
    Friends("Friends"),
    Configs("Configs");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
