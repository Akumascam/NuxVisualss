package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

// Эффект, когда убитая вами сущность умирает (видно только вам).
// Смерть ловим тремя способами, потому что многие сервера (FunTime и т.п.) не присылают здоровье игроков:
//  1) пакет статуса смерти (EntityStatus 3) — миксин LivingEntityMixin;
//  2) здоровье <= 0 / isDead;
//  3) сущность пропала с клиента вскоре после вашего удара.
public class KillEffect extends Module {
    public static KillEffect INSTANCE;

    private final ModeSetting effect = add(new ModeSetting("Effect", "Lightning", "Lightning", "Totem", "Flame", "Soul", "Hearts"));
    private final SliderSetting amount = add(new SliderSetting("Amount", 40, 10, 120, 5));

    private static final long TRACK_MS = 5000;
    private static final long REMOVED_MS = 1500;

    private static LivingEntity target;
    private static long hitTime;
    private static double lastX, lastY, lastZ, lastH;

    public KillEffect() {
        super("kill-effect", "Kill Effect", Category.Utilities, false);
        INSTANCE = this;
    }

    private static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static void onHit(Entity entity) {
        if (entity instanceof LivingEntity living && living != MinecraftClient.getInstance().player) {
            target = living;
            hitTime = System.currentTimeMillis();
            remember(living);
        }
    }

    private static void remember(LivingEntity e) {
        lastX = e.getX();
        lastY = e.getY();
        lastZ = e.getZ();
        lastH = e.getHeight();
    }

    // вызывается из LivingEntityMixin, когда сервер прислал статус смерти
    public static void onDeathStatus(LivingEntity entity) {
        if (!active() || target == null || entity != target) {
            return;
        }
        if (System.currentTimeMillis() - hitTime > TRACK_MS) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            remember(entity);
            INSTANCE.play(client.world);
        }
        target = null;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (target == null || client.world == null) {
            return;
        }
        long since = System.currentTimeMillis() - hitTime;
        if (since > TRACK_MS || target.getWorld() != client.world) {
            target = null;
            return;
        }
        if (target.isRemoved()) {
            // пропала сразу после удара — почти всегда это смерть
            if (since <= REMOVED_MS) {
                play(client.world);
            }
            target = null;
            return;
        }
        remember(target);
        if (target.isDead() || target.getHealth() <= 0) {
            play(client.world);
            target = null;
        }
    }

    @Override
    public void onDisable() {
        target = null;
    }

    private void play(ClientWorld world) {
        double x = lastX;
        double y = lastY;
        double z = lastZ;
        if (effect.is("Lightning")) {
            LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            bolt.refreshPositionAfterTeleport(x, y, z);
            bolt.setCosmetic(true);
            world.addEntity(bolt);
            return;
        }
        ParticleEffect particle = switch (effect.get()) {
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "Flame" -> ParticleTypes.FLAME;
            case "Soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.HEART;
        };
        Random random = world.getRandom();
        double h = lastH > 0 ? lastH : 1.8;
        for (int i = 0; i < amount.getInt(); i++) {
            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = random.nextDouble() * 0.5;
            double vz = (random.nextDouble() - 0.5) * 0.5;
            world.addParticle(particle, x, y + random.nextDouble() * h, z, vx, vy, vz);
        }
    }
}
