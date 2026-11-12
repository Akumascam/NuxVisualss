package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ColorSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

// Время, погода, снег, туман, небо и облака — только у тебя на клиенте. Применяется через миксины.
public class WorldCustomizer extends Module {
    public static WorldCustomizer INSTANCE;
    public static final int SNOW_TINT = 0xF2F6FA;

    private static final String[] TIME_NAMES = {"По серверу", "Рассвет", "День", "Закат", "Ночь", "Своё"};
    private static final int[] TIME_VALUES = {-1, 23300, 6000, 12500, 18000, -1};

    private final ModeSetting timeMode = add(new ModeSetting("Время", "Рассвет", TIME_NAMES));
    private final SliderSetting time = add(new SliderSetting("Своё время", 6000, 0, 23999, 100)
            .visibleIf(() -> timeMode.is("Своё")));
    private final ModeSetting weather = add(new ModeSetting("Погода", "Зима", "По серверу", "Ясно", "Дождь", "Гроза", "Зима"));
    private final SliderSetting snowAmount = add(new SliderSetting("Снегопад", 850, 100, 1600, 50)
            .visibleIf(() -> weather.is("Зима")));
    private final SliderSetting coverRadius = add(new SliderSetting("Радиус снега", 56, 16, 96, 4, " блоков")
            .visibleIf(() -> weather.is("Зима")));
    private final ModeSetting fog = add(new ModeSetting("Туман", "Обычный", "Обычный", "Морозный", "Выключен"));
    private final BoolSetting winterAtmosphere=add(new BoolSetting("Зимняя атмосфера",true).visibleIf(()->weather.is("Зима")));
    private final BoolSetting frozenWater=add(new BoolSetting("Визуальный лёд",true).visibleIf(()->weather.is("Зима")));
    private final SliderSetting flakeSize=add(new SliderSetting("Размер снежинок",.75,.35,1.6,.05).visibleIf(()->weather.is("Зима")));
    private final SliderSetting wind=add(new SliderSetting("Ветер",.6,0,2,.1).visibleIf(()->weather.is("Зима")));
    private final WinterCover cover = new WinterCover();
    private ClientWorld lastWorld;
    private boolean lastWinter;

    public WorldCustomizer() {
        super("world", "World Customizer", Category.Visuals, false);
        INSTANCE = this;
    }

    @Override public void onEnable() {
        cover.clear();
        reloadChunks();
    }

    // ---------- снегопад «Winter» ----------
    private static final class Flake {
        float x, y, z, px, py, pz, size, phase, fall, ground;
    }

    private final Random random = new Random();
    private Flake[] flakes = new Flake[0];
    private static final float RADIUS = 22f;
    private static final float HEIGHT = 14f;

    private void spawnFlake(Flake f, ClientWorld world, double ox, double oy, double oz, boolean anywhere) {
        double a = random.nextDouble() * Math.PI * 2;
        double d = Math.sqrt(random.nextDouble()) * RADIUS;
        f.x = (float) (ox + Math.cos(a) * d);
        f.z = (float) (oz + Math.sin(a) * d);
        f.y = (float) (oy + (anywhere ? (random.nextDouble() * 2 - 1) * HEIGHT : HEIGHT - random.nextDouble() * 2));
        f.px = f.x;
        f.py = f.y;
        f.pz = f.z;
        f.size = 0.6f + random.nextFloat() * 0.8f;
        f.phase = random.nextFloat() * 100f;
        f.fall = 0.6f + random.nextFloat() * 0.5f;
        f.ground = groundAt(world, f.x, f.z, oy);
    }

    private float groundAt(ClientWorld world, float x, float z, double camY) {
        // Snow in every biome, but not falling through solid ceilings.
        return world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
    }

    @Override
    public void onTick(MinecraftClient client) {
        boolean winter = weather.is("Зима");
        if (lastWorld != client.world || lastWinter != winter) {
            cover.clear();
            flakes = new Flake[0];
            lastWorld = client.world;
            lastWinter = winter;
            reloadChunks();
        }
        if (winter && client.world != null && client.player != null) {
            cover.tick(client.world, client.gameRenderer.getCamera().getPos(), coverRadius.getInt());
        }
        if (!weather.is("Зима") || client.world == null || client.player == null) {
            flakes = new Flake[0];
            return;
        }
        ClientWorld world = client.world;
        Vec3d c = client.gameRenderer.getCamera().getPos();
        int target = snowAmount.getInt();
        if (flakes.length != target) {
            Flake[] next = new Flake[target];
            for (int i = 0; i < target; i++) {
                if (i < flakes.length) {
                    next[i] = flakes[i];
                } else {
                    next[i] = new Flake();
                    spawnFlake(next[i], world, c.x, c.y, c.z, true);
                }
            }
            flakes = next;
        }
        float speed = 1f;
        float windX = 0.012f*wind.getFloat();
        long age = world.getTime();
        for (Flake f : flakes) {
            if((age+(int)f.phase)%10==0)f.ground=groundAt(world,f.x,f.z,c.y);
            f.px = f.x;
            f.py = f.y;
            f.pz = f.z;
            f.y -= 0.06f * f.fall * speed;
            f.x += windX + (float) Math.sin(age * 0.05 + f.phase) * 0.02f;
            f.z += (float) Math.cos(age * 0.043 + f.phase) * 0.02f;
            float dx = (float) (f.x - c.x), dz = (float) (f.z - c.z);
            if (f.y < f.ground || f.y < c.y - HEIGHT) {
                spawnFlake(f, world, c.x, c.y, c.z, false);
            } else if (dx * dx + dz * dz > RADIUS * RADIUS) {
                // вышла за радиус — переносим на противоположную сторону (снег «идёт» вместе с игроком)
                f.x = (float) (c.x - dx * 0.95f);
                f.z = (float) (c.z - dz * 0.95f);
                f.px = f.x;
                f.pz = f.z;
                f.ground = groundAt(world, f.x, f.z, c.y);
            }
        }
    }

    @Override
    public void onRender(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        if (weather.is("Зима") && mc().world != null) cover.render(matrices, consumers, camera, mc().world);
        if (flakes.length == 0 || !weather.is("Зима")) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        Vec3d cam = camera.getPos();
        Quaternionf q = camera.getRotation();
        Vector3f r = q.transform(new Vector3f(1, 0, 0));
        Vector3f u = q.transform(new Vector3f(0, 1, 0));
        Vector3f look = q.transform(new Vector3f(0, 0, -1));
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugQuads());
        // ночью снег не должен светиться
        float light = 1f - client.world.getAmbientDarkness() / 15f * 0.75f;
        int v = Math.round(255 * light);
        int rgb = (v << 16) | (Math.min(255, v + 4) << 8) | Math.min(255, v + 10);
        float baseSize = 0.042f*flakeSize.getFloat();

        for (Flake f : flakes) {
            float x = (float) (f.px + (f.x - f.px) * tickDelta - cam.x);
            float y = (float) (f.py + (f.y - f.py) * tickDelta - cam.y);
            float z = (float) (f.pz + (f.z - f.pz) * tickDelta - cam.z);
            float dist2 = x * x + y * y + z * z;
            // за спиной не рисуем — экономим FPS
            if (x * look.x + y * look.y + z * look.z < -1f || dist2 < 0.04f) {
                continue;
            }
            float dist = (float) Math.sqrt(dist2);
            float fade = Math.min(1f, (RADIUS - dist) / 6f) * Math.min(1f, dist / 1.5f);
            if (fade <= 0.02f) {
                continue;
            }
            float s = baseSize * f.size;
            int core = RenderUtil.withAlpha(rgb, 0.9f * fade);
            int halo = RenderUtil.withAlpha(rgb, 0.22f * fade);
            int clear = rgb;
            // мягкое сияние: ромб с затуханием к краям (4 треугольника)
            float g = s * 2.6f;
            float rx = r.x * g, ry = r.y * g, rz = r.z * g, ux = u.x * g, uy = u.y * g, uz = u.z * g;
            com.nuxvisuals.render.Fx.quad(vc, m, x, y, z, halo, x + rx, y + ry, z + rz, clear, x + ux, y + uy, z + uz, clear, x, y, z, halo);
            com.nuxvisuals.render.Fx.quad(vc, m, x, y, z, halo, x + ux, y + uy, z + uz, clear, x - rx, y - ry, z - rz, clear, x, y, z, halo);
            com.nuxvisuals.render.Fx.quad(vc, m, x, y, z, halo, x - rx, y - ry, z - rz, clear, x - ux, y - uy, z - uz, clear, x, y, z, halo);
            com.nuxvisuals.render.Fx.quad(vc, m, x, y, z, halo, x - ux, y - uy, z - uz, clear, x + rx, y + ry, z + rz, clear, x, y, z, halo);
            // ядро: два наложенных квадрата (восьмиугольник) — выглядит как круглая снежинка
            float ax = r.x * s, ay = r.y * s, az = r.z * s, bx = u.x * s, by = u.y * s, bz = u.z * s;
            com.nuxvisuals.render.Fx.quad(vc, m, x - ax - bx, y - ay - by, z - az - bz, core, x + ax - bx, y + ay - by, z + az - bz, core,
                    x + ax + bx, y + ay + by, z + az + bz, core, x - ax + bx, y - ay + by, z - az + bz, core);
            // Occasional close six-armed crystals, among the smaller soft flakes.
            if(((int)f.phase)%29==0&&dist<13){
                float arm=s*3.1f,stroke=s*.17f;
                for(int k=0;k<6;k++){
                    double angle=k*Math.PI/3+ageAngle(client.world.getTime(),f.phase);
                    float ca=(float)Math.cos(angle),sa=(float)Math.sin(angle);
                    float vx=(r.x*ca+u.x*sa)*arm,vy=(r.y*ca+u.y*sa)*arm,vz=(r.z*ca+u.z*sa)*arm;
                    float tx=(-r.x*sa+u.x*ca)*stroke,ty=(-r.y*sa+u.y*ca)*stroke,tz=(-r.z*sa+u.z*ca)*stroke;
                    com.nuxvisuals.render.Fx.quad(vc,m,x-tx,y-ty,z-tz,core,x+vx-tx,y+vy-ty,z+vz-tz,core,x+vx+tx,y+vy+ty,z+vz+tz,core,x+tx,y+ty,z+tz,core);
                }
            }
            float d = 1.414f;
            com.nuxvisuals.render.Fx.quad(vc, m, x - ax * d, y - ay * d, z - az * d, core, x - bx * d, y - by * d, z - bz * d, core,
                    x + ax * d, y + ay * d, z + az * d, core, x + bx * d, y + by * d, z + bz * d, core);
        }
    }

    private static double ageAngle(long age,float phase){return age*.005+phase;}

    @Override public void onDisable() {
        flakes = new Flake[0];
        cover.clear();
        lastWinter = false;
        lastWorld = null;
        reloadChunks();
    }

    // цвет травы/листвы запекается в чанки — их нужно перестроить
    private static void reloadChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.worldRenderer != null) {
            client.world.reloadColor();
            client.worldRenderer.reload();
        }
    }

    private static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static Long timeOverride() {
        if (!active()) {
            return null;
        }
        int index = INSTANCE.timeMode.index();
        if (INSTANCE.timeMode.is("Своё")) {
            return (long) INSTANCE.time.get();
        }
        return TIME_VALUES[index] >= 0 ? (long) TIME_VALUES[index] : null;
    }

    public static Float rainOverride() {
        if (!active()) {
            return null;
        }
        return switch (INSTANCE.weather.get()) {
            case "Ясно", "Зима" -> 0f;
            case "Дождь", "Гроза" -> 1f;
            default -> null;
        };
    }

    public static Float thunderOverride() {
        if (!active()) {
            return null;
        }
        return switch (INSTANCE.weather.get()) {
            case "Ясно", "Дождь", "Зима" -> 0f;
            case "Гроза" -> 1f;
            default -> null;
        };
    }

    public static boolean winter() { return active() && INSTANCE.weather.is("Зима"); }
    public static boolean forceSnow() { return winter(); }
    public static boolean snowyGround() { return winter(); }
    public static boolean icyWater() { return winter()&&INSTANCE.frozenWater.get(); }
    public static final int ICE_WATER = 0x9ABFE1;

    // WinterCover uses the real client world, never fake block states/collisions.
    public static boolean snowCover() { return false; }
    public static boolean snowEverywhere() { return false; }

    public static float[] fogDistance(float start, float end) {
        if (!active()) return null;
        if (INSTANCE.fog.is("Выключен")) return new float[]{100_000f, 100_001f};
        if (INSTANCE.fog.is("Морозный") || winterAtmosphere()) return new float[]{start * 0.5f, end * 0.75f};
        return null;
    }

    public static boolean winterAtmosphere(){return winter()&&INSTANCE.winterAtmosphere.get();}
    public static float[] fogColor() {
        Integer shader=ShaderSky.fogColor();
        if(shader!=null)return new float[]{((shader>>16)&255)/255f,((shader>>8)&255)/255f,(shader&255)/255f};
        if (!active() || (!INSTANCE.fog.is("Морозный")&&!winterAtmosphere()) || mc().world == null) return null;
        float light = 1f - mc().world.getAmbientDarkness() / 15f * 0.65f;
        return new float[]{0.48f * light, 0.55f * light, 0.68f * light};
    }
    public static Integer skyColor() {
        Integer shader=ShaderSky.fogColor();if(shader!=null)return shader;
        return winterAtmosphere()?0x65758F:null;
    }
    public static Integer cloudColor() {return winterAtmosphere()?0xA6B7D0:null;}
}
