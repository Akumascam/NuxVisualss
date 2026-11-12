package com.nuxvisuals.cosmetics;

import net.minecraft.client.util.math.MatrixStack;

// «Скелет», к которому крепится косметика: в игре — части модели игрока, в меню — манекен
public interface Rig {
    enum Anchor { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    // переводит матрицу в систему координат части тела (в пикселях модели)
    void apply(MatrixStack m, Anchor anchor);

    // манекен для меню: всё в пикселях, руки висят вдоль тела
    Rig MANNEQUIN = (m, anchor) -> {
        if (anchor == Anchor.RIGHT_ARM) {
            m.translate(-5, 2, 0);
        } else if (anchor == Anchor.LEFT_ARM) {
            m.translate(5, 2, 0);
        } else if (anchor == Anchor.RIGHT_LEG) {
            m.translate(-1.9f, 12, 0);
        } else if (anchor == Anchor.LEFT_LEG) {
            m.translate(1.9f, 12, 0);
        }
    };
}
