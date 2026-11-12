package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.Fx;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Примерная траектория полёта: стрелы (лук/арбалет), трезубец, эндер-жемчуг,
// снежки/яйца, взрывные и оседающие зелья, пузырёк опыта. Только отрисовка линии.
public class Trajectories extends Module {
    public static Trajectories INSTANCE;

    private final ColorSetting color = add(new ColorSetting("Color", 0.55f, 0.7f));
    private final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
    private final SliderSetting width = add(new SliderSetting("Width", 0.05, 0.02, 0.15, 0.01));
    private final BoolSetting landing = add(new BoolSetting("Landing Mark", true));
    private final BoolSetting hitColor = add(new BoolSetting("Highlight Hit", true));

    public Trajectories() {
        super("trajectories", "Trajectories", Category.Visuals, false);
        INSTANCE = this;
    }

    // параметры броска: скорость, гравитация, сопротивление, наклон (для зелий)
    public record Throw(float speed, float gravity, float drag, float waterDrag, float roll) {
    }

    public record Path(List<Vec3d> points, Vec3d end, Direction side, boolean hitEntity) {
    }

    public static Throw throwFor(ClientPlayerEntity player, ItemStack stack, Hand hand) {
        if (stack.isEmpty()) {
            return null;
        }
        var item = stack.getItem();
        if (item instanceof BowItem) {
            if (!player.isUsingItem() || player.getActiveHand() != hand) {
                return null;
            }
            float pull = BowItem.getPullProgress(player.getItemUseTime());
            if (pull < 0.1f) {
                return null;
            }
            return new Throw(pull * 3f, 0.05f, 0.99f, 0.6f, 0f);
        }
        if (item instanceof CrossbowItem) {
            ChargedProjectilesComponent loaded = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (!CrossbowItem.isCharged(stack) || loaded == null || loaded.getProjectiles().isEmpty()) return null;
            boolean rocket = loaded.getProjectiles().stream().anyMatch(p -> p.isOf(net.minecraft.item.Items.FIREWORK_ROCKET));
            return rocket ? new Throw(1.6f, 0f, 1f, 1f, 0f) : new Throw(3.15f, 0.05f, 0.99f, 0.6f, 0f);
        }
        if (item instanceof TridentItem) {
            return player.isUsingItem() && player.getActiveHand() == hand
                    ? new Throw(2.5f, 0.05f, 0.99f, 0.99f, 0f) : null;
        }
        if (item instanceof EnderPearlItem || item instanceof SnowballItem || item instanceof EggItem) {
            return new Throw(1.5f, 0.03f, 0.99f, 0.8f, 0f);
        }
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            return new Throw(0.5f, 0.05f, 0.99f, 0.8f, -20f);
        }
        if (item instanceof ExperienceBottleItem) {
            return new Throw(0.7f, 0.07f, 0.99f, 0.8f, -20f);
        }
        return null;
    }

    // симуляция полёта (как у ванильных снарядов, примерно)
    public static Path simulate(ClientPlayerEntity player, Throw t, float tickDelta) {
        return simulate(player, t, tickDelta, 0f);
    }

    public static Path simulate(ClientPlayerEntity player, Throw t, float tickDelta, float spreadDegrees) {
        World world = player.getWorld();
        float yaw = player.getYaw(tickDelta);
        float pitch = player.getPitch(tickDelta);
        double rad = Math.PI / 180.0;
        double vx = -Math.sin(yaw * rad) * Math.cos(pitch * rad);
        double vy = -Math.sin((pitch + t.roll()) * rad);
        double vz = Math.cos(yaw * rad) * Math.cos(pitch * rad);
        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        Vec3d vel = new Vec3d(vx / len * t.speed(), vy / len * t.speed(), vz / len * t.speed());
        // Rotate around the shooter's UP axis, as vanilla crossbows do. A yaw offset alone
        // is wrong when looking steeply upward/downward.
        if (spreadDegrees != 0f) {
            double pitchRad = pitch * rad, yawRad = yaw * rad;
            Vec3d axis = new Vec3d(-Math.sin(yawRad) * Math.sin(pitchRad), Math.cos(pitchRad),
                    Math.cos(yawRad) * Math.sin(pitchRad));
            double a = spreadDegrees * rad;
            vel = vel.multiply(Math.cos(a)).add(axis.crossProduct(vel).multiply(Math.sin(a)))
                    .add(axis.multiply(axis.dotProduct(vel) * (1 - Math.cos(a))));
        }
        Vec3d move = player.getVelocity();
        vel = vel.add(move.x, player.isOnGround() ? 0 : move.y, move.z);

        Vec3d pos = player.getCameraPosVec(tickDelta).subtract(0, 0.1, 0);
        List<Vec3d> points = new ArrayList<>();
        points.add(pos);
        Box search = player.getBoundingBox().expand(64);
        List<Entity> entities = world.getOtherEntities(player, search,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
        for (int i = 0; i < 300; i++) {
            Vec3d next = pos.add(vel);
            HitResult hit = world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, player));
            Vec3d stop = hit.getType() != HitResult.Type.MISS ? hit.getPos() : null;
            Entity hitEntity = null;
            if (i > 0) {
                for (Entity e : entities) {
                    Optional<Vec3d> r = e.getBoundingBox().expand(0.3).raycast(pos, stop != null ? stop : next);
                    if (r.isPresent()) {
                        stop = r.get();
                        hitEntity = e;
                    }
                }
            }
            if (stop != null) {
                points.add(stop);
                Direction side = hitEntity == null && hit instanceof BlockHitResult b ? b.getSide() : Direction.UP;
                return new Path(points, stop, side, hitEntity != null);
            }
            pos = next;
            points.add(pos);
            boolean water = !world.getFluidState(BlockPos.ofFloored(pos)).isEmpty();
            vel = vel.multiply(water ? t.waterDrag() : t.drag()).subtract(0, t.gravity(), 0);
            if (pos.y < world.getBottomY() - 16) {
                break;
            }
        }
        return new Path(points, null, Direction.UP, false);
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        ItemStack used = player.getMainHandStack();
        Hand usedHand = Hand.MAIN_HAND;
        Throw t = throwFor(player, used, usedHand);
        if (t == null) {
            used = player.getOffHandStack();
            usedHand = Hand.OFF_HAND;
            t = throwFor(player, used, usedHand);
        }
        if (t == null) {
            return;
        }
        int projectiles = 1;
        if (used.getItem() instanceof CrossbowItem) {
            ChargedProjectilesComponent loaded = used.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (loaded != null) projectiles = Math.max(1, Math.min(16, loaded.getProjectiles().size()));
        }
        for (int arrow = 0; arrow < projectiles; arrow++) {
            float angle = ProjectileSpread.angle(arrow, projectiles);
            renderPath(matrices, consumers, camera, simulate(player, t, tickDelta, angle), usedHand);
        }
    }

    private void renderPath(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, Path path, Hand hand) {
        Vec3d cam = camera.getPos();
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        Quaternionf q = camera.getRotation();
        Vector3f right = q.transform(new Vector3f(1, 0, 0));
        Vector3f up = q.transform(new Vector3f(0, 1, 0));
        int base = color(color, rainbow, 0f);
        if (hitColor.get() && path.hitEntity()) {
            base = 0xFFFF5A5A;
        }
        float w = width.getFloat();
        float handSide = hand == Hand.MAIN_HAND ? 1f : -1f;
        if (mc().player.getMainArm() == net.minecraft.util.Arm.LEFT) handSide = -handSide;
        List<Vec3d> pts = path.points();
        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3d a = pts.get(i);
            Vec3d b = pts.get(i + 1);
            // начало линии чуть смещено к руке, чтобы она не шла прямо из глаз
            float ka = Math.max(0f, 1f - i / 6f);
            float kb = Math.max(0f, 1f - (i + 1) / 6f);
            float ax = (float) (a.x - cam.x) + right.x * 0.25f * handSide * ka - up.x * 0.15f * ka;
            float ay = (float) (a.y - cam.y) + right.y * 0.25f * handSide * ka - up.y * 0.15f * ka;
            float az = (float) (a.z - cam.z) + right.z * 0.25f * handSide * ka - up.z * 0.15f * ka;
            float bx = (float) (b.x - cam.x) + right.x * 0.25f * handSide * kb - up.x * 0.15f * kb;
            float by = (float) (b.y - cam.y) + right.y * 0.25f * handSide * kb - up.y * 0.15f * kb;
            float bz = (float) (b.z - cam.z) + right.z * 0.25f * handSide * kb - up.z * 0.15f * kb;
            float fa = Math.min(1f, i / 3f);
            float fb = Math.min(1f, (i + 1) / 3f);
            Fx.ribbon(vc, m, ax, ay, az, bx, by, bz, w * 3f,
                    RenderUtil.withAlpha(base, 0.18f * fa), RenderUtil.withAlpha(base, 0.18f * fb), 0, 0, 0);
            Fx.ribbon(vc, m, ax, ay, az, bx, by, bz, w,
                    RenderUtil.withAlpha(base, 0.95f * fa), RenderUtil.withAlpha(base, 0.95f * fb), 0, 0, 0);
        }
        if (landing.get() && path.end() != null) {
            drawMark(vc, m, path.end().subtract(cam), path.side(), base, 0.45f);
        }
    }

    // круг-отметка на поверхности, куда прилетит снаряд
    public static void drawMark(VertexConsumer vc, Matrix4f m, Vec3d p, Direction side, int base, float radius) {
        Vector3f n = new Vector3f(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Vector3f a = Math.abs(n.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(n).cross(a).normalize();
        Vector3f v = new Vector3f(n).cross(u).normalize();
        float x = (float) p.x + n.x * 0.02f, y = (float) p.y + n.y * 0.02f, z = (float) p.z + n.z * 0.02f;
        Fx.glow(vc, m, x, y, z, radius, RenderUtil.withAlpha(base, 0.45f), u.x, u.y, u.z, v.x, v.y, v.z);
        int seg = 24;
        int ring = RenderUtil.withAlpha(base, 0.95f);
        for (int i = 0; i < seg; i++) {
            double a1 = i * Math.PI * 2 / seg;
            double a2 = (i + 1) * Math.PI * 2 / seg;
            float c1 = (float) Math.cos(a1) * radius, s1 = (float) Math.sin(a1) * radius;
            float c2 = (float) Math.cos(a2) * radius, s2 = (float) Math.sin(a2) * radius;
            float r2 = radius * 0.86f / radius;
            Fx.quad(vc, m,
                    x + u.x * c1 + v.x * s1, y + u.y * c1 + v.y * s1, z + u.z * c1 + v.z * s1, ring,
                    x + u.x * c2 + v.x * s2, y + u.y * c2 + v.y * s2, z + u.z * c2 + v.z * s2, ring,
                    x + (u.x * c2 + v.x * s2) * r2, y + (u.y * c2 + v.y * s2) * r2, z + (u.z * c2 + v.z * s2) * r2, ring,
                    x + (u.x * c1 + v.x * s1) * r2, y + (u.y * c1 + v.y * s1) * r2, z + (u.z * c1 + v.z * s1) * r2, ring);
        }
    }
}
