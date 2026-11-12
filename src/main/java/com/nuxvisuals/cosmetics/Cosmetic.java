package com.nuxvisuals.cosmetics;

// Один предмет косметики: id, название, раздел, 3D-модель и метка события («Хеллоуин», «Зима»)
public record Cosmetic(String id, String name, Category category, Shape shape, String tag) {
    public Cosmetic(String id, String name, Category category, Shape shape) {
        this(id, name, category, shape, "");
    }

    public interface Shape {
        void draw(Draw r);
    }
}
