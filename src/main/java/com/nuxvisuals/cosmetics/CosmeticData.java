package com.nuxvisuals.cosmetics;

// Данные игрока для 3D-косметики на один кадр (заполняются в PlayerEntityRendererMixin)
public final class CosmeticData {
    public boolean me;          // это мы (косметику видишь только ты, на себе)
    public boolean friend;      // игрок из списка друзей — на нём рисуются крылья
    public boolean invisible;
    public boolean emptyHand;   // в главной руке пусто — меч берётся в руку
    public boolean leftHanded;
    public boolean swordHeld;   // в руке меч, на который надет скин
    public boolean elytra;      // надеты элитры — плащ и крылья прячутся
    public boolean sneaking;
    public boolean flying;
    public float time;          // тики (с дробной частью) — для анимаций
    public float speed;         // горизонтальная скорость, блоков/тик

    public void copy(CosmeticData o) {
        me = o.me;
        friend = o.friend;
        invisible = o.invisible;
        emptyHand = o.emptyHand;
        leftHanded = o.leftHanded;
        swordHeld = o.swordHeld;
        elytra = o.elytra;
        sneaking = o.sneaking;
        flying = o.flying;
        time = o.time;
        speed = o.speed;
    }
}
