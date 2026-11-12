package com.nuxvisuals.cosmetics;

// Разделы косметики. В каждом разделе можно надеть один предмет.
public enum Category {
    MODEL("Модели", "Модель", "shirt"),
    CAPE("Плащи", "Плащ", "cape"),
    WINGS("Крылья", "Крылья", "wings"),
    BACK("На спину", "На спину", "backpack"),
    PET("Питомцы", "Питомец", "paw"),
    HAT("Голова", "Аксессуар", "hat"),
    SWORD("Мечи", "Меч", "sword");

    public final String label;   // название вкладки
    public final String single;  // подпись на карточке
    public final String icon;    // textures/icons/<icon>.png

    Category(String label, String single, String icon) {
        this.label = label;
        this.single = single;
        this.icon = icon;
    }
}
