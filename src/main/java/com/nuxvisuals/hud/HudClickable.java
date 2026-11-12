package com.nuxvisuals.hud;

// HUD-элемент с кнопками: клик ЛКМ при открытом чате (координаты без масштаба, от левого верхнего угла)
public interface HudClickable {
    boolean click(double x, double y);
}
