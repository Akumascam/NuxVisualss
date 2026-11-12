package com.nuxvisuals.module.impl;

import com.mojang.authlib.GameProfile;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

// Fake Player: манекен-копия тебя для тренировки (виден только тебе).
// Получает урон, криты, отбрасывание, показывает урон; на сервер удары не отправляются.
public class FakePlayer extends Module {
    private static final int ID = -133742;

    private final SliderSetting health = add(new SliderSetting("Health", 20, 1, 100, 1, " hp"));
    private final BoolSetting copyInventory = add(new BoolSetting("Copy Inventory", true));
    private final BoolSetting knockback = add(new BoolSetting("Knockback", true));
    private final BoolSetting strafe = add(new BoolSetting("Strafe", false));
    private final SliderSetting strafeSpeed = add(new SliderSetting("Strafe Speed", 0.15, 0.05, 0.4, 0.01))
            .visibleIf(this.strafe::get);
    private final BoolSetting lookAtYou = add(new BoolSetting("Look At You", true));
    private final BoolSetting regen = add(new BoolSetting("Regenerate", true));
    private final BoolSetting showDamage = add(new BoolSetting("Show Damage", true));

    private static FakePlayer instance;
    private OtherClientPlayerEntity fake;
    private ClientWorld world;
    private Vec3d velocity = Vec3d.ZERO;
    private long lastHit;
    private int strafeDir = 1;
    private int strafeTicks;

    public FakePlayer() {
        super("fake-player", "Fake Player", Category.Utilities, false);
        instance = this;
    }

    public static boolean isFake(Entity e) {
        return instance != null && instance.fake != null && e == instance.fake;
    }

    @Override
    public void onEnable() {
        spawn();
    }

    @Override
    public void onDisable() {
        remove();
    }

    private void spawn() {
        MinecraftClient mc = mc();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            return;
        }
        remove();
        world = mc.world;
        GameProfile profile = new GameProfile(UUID.randomUUID(), "NuxBot");
        fake = new OtherClientPlayerEntity(world, profile) {
            @Override
            public SkinTextures getSkinTextures() {
                ClientPlayerEntity me = MinecraftClient.getInstance().player;
                return me != null ? me.getSkinTextures() : super.getSkinTextures();
            }
        };
        fake.setId(ID);
        fake.copyPositionAndRotation(p);
        fake.setHeadYaw(p.getHeadYaw());
        fake.setBodyYaw(p.getBodyYaw());
        if (copyInventory.get()) {
            fake.getInventory().clone(p.getInventory());
        }
        fake.setHealth(health.getFloat());
        velocity = Vec3d.ZERO;
        world.addEntity(fake);
    }

    private void remove() {
        if (fake != null && world != null) {
            world.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        }
        fake = null;
        world = null;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            fake = null;
            world = null;
            return;
        }
        if (fake == null || world != client.world || fake.isRemoved()) {
            spawn(); // смена мира / измерения
            return;
        }
        // поворот к игроку
        if (lookAtYou.get()) {
            double dx = client.player.getX() - fake.getX();
            double dz = client.player.getZ() - fake.getZ();
            double dy = client.player.getEyeY() - fake.getEyeY();
            float yaw = (float) (MathHelper.atan2(dz, dx) * 180 / Math.PI) - 90f;
            float pitch = (float) -(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
            fake.setYaw(yaw);
            fake.setHeadYaw(yaw);
            fake.setBodyYaw(yaw);
            fake.setPitch(pitch);
        }
        // стрейф влево-вправо
        Vec3d move = velocity;
        if (strafe.get()) {
            if (++strafeTicks > 20 + (int) (Math.random() * 20)) {
                strafeTicks = 0;
                strafeDir = -strafeDir;
            }
            float yawRad = fake.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            double s = strafeSpeed.get() * strafeDir;
            move = move.add(Math.cos(yawRad) * s, 0, Math.sin(yawRad) * s);
        }
        // физика: гравитация, трение, столкновения
        velocity = velocity.add(0, -0.08, 0);
        fake.move(MovementType.SELF, move.add(0, velocity.y - move.y, 0));
        if (fake.isOnGround()) {
            velocity = new Vec3d(velocity.x * 0.6, 0, velocity.z * 0.6);
        } else {
            velocity = new Vec3d(velocity.x * 0.91, velocity.y * 0.98, velocity.z * 0.91);
        }
        // регенерация через 3 секунды без ударов
        if (regen.get() && System.currentTimeMillis() - lastHit > 3000 && fake.getHealth() < health.getFloat()) {
            fake.setHealth(Math.min(health.getFloat(), fake.getHealth() + 0.5f));
        }
    }

    // вызывается из AttackEntityCallback; true = удар обработан локально, пакет не отправляем
    public static boolean onAttack(PlayerEntity player, Entity target) {
        if (!isFake(target)) {
            return false;
        }
        instance.hit(player);
        return true;
    }

    private void hit(PlayerEntity player) {
        MinecraftClient mc = mc();
        boolean crit = HitColor.isCrit(player, fake);
        float cooldown = player.getAttackCooldownProgress(0.5f);
        float damage = (float) player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        damage *= 0.2f + cooldown * cooldown * 0.8f;
        if (crit) {
            damage *= 1.5f;
        }
        boolean sprintHit = player.isSprinting() && cooldown > 0.9f;

        // неуязвимость после удара, как у настоящих игроков
        if (fake.hurtTime > 0 && System.currentTimeMillis() - lastHit < 450) {
            player.resetLastAttackedTicks();
            return;
        }
        lastHit = System.currentTimeMillis();
        fake.animateDamage(player.getYaw());
        float hp = fake.getHealth() - damage;

        if (knockback.get()) {
            double strength = 0.4 + (sprintHit ? 0.5 : 0);
            float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            velocity = new Vec3d(-MathHelper.sin(yaw) * strength * 0.6, fake.isOnGround() ? 0.4 : velocity.y,
                    MathHelper.cos(yaw) * strength * 0.6);
        }
        if (sprintHit) {
            player.setSprinting(false);
        }
        mc.world.playSound(player, fake.getX(), fake.getY(), fake.getZ(),
                crit ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYERS, 1f, 1f);
        if (crit) {
            mc.particleManager.addEmitter(fake, ParticleTypes.CRIT);
        }
        if (hp <= 0) {
            mc.world.playSound(player, fake.getX(), fake.getY(), fake.getZ(), SoundEvents.ENTITY_PLAYER_DEATH,
                    SoundCategory.PLAYERS, 1f, 1f);
            mc.particleManager.addEmitter(fake, ParticleTypes.TOTEM_OF_UNDYING, 30);
            hp = health.getFloat();
            if (showDamage.get()) {
                InvUtil.actionBar("§cNuxBot убит! §7(возрождён)");
            }
        } else if (showDamage.get()) {
            InvUtil.actionBar((crit ? "§eКрит! " : "§f") + String.format("-%.1f", damage)
                    + " §7| §c" + String.format("%.1f", hp) + " ❤");
        }
        fake.setHealth(hp);
        player.resetLastAttackedTicks();
    }
}
