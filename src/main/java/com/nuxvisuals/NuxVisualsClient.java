package com.nuxvisuals;

import com.nuxvisuals.config.Config;
import com.nuxvisuals.hud.HudManager;
import com.nuxvisuals.module.impl.HitSound;
import com.nuxvisuals.module.impl.KillEffect;
import com.nuxvisuals.module.impl.Zoom;
import com.nuxvisuals.waypoint.WaypointManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.ui.NuxClickGui;
import com.nuxvisuals.ui.NuxTitleScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.nuxvisuals.cosmetics.CosmeticFeature;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

public class NuxVisualsClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Открыть меню NuxVisuals",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "NuxVisuals"
        ));

        KeyBinding waypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Поставить точку (NuxVisuals)",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "NuxVisuals"
        ));

        KeyBinding cosmeticsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Открыть косметику (NuxVisuals)",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "NuxVisuals"
        ));

        // удары по сущностям — для Hit Sound и Kill Effect
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) {
                HitSound.onHit();
                KillEffect.onHit(entity);
                com.nuxvisuals.module.impl.Particles.onHit(entity);
                com.nuxvisuals.module.impl.TargetHud.onHit(entity);
                com.nuxvisuals.module.impl.HitColor.onHit(player, entity);
                com.nuxvisuals.module.impl.ShiftTap.onAttack(player, entity);
                com.nuxvisuals.module.impl.TargetESP.onAttack(player, entity);
                // удар по манекену обрабатываем сами и не отправляем на сервер
                if (com.nuxvisuals.module.impl.FakePlayer.onAttack(player, entity)) {
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        Config.load();
        HudManager.init();
        com.nuxvisuals.module.impl.ItemScroller.init();
        com.nuxvisuals.module.impl.AuctionRelister.init();
        com.nuxvisuals.module.impl.AutoAccept.init();
        com.nuxvisuals.ui.MultiplayerTheme.init();
        com.nuxvisuals.perf.PerfMods.checkAsync();

        // пинг: сетевой поток Netty — с наивысшим приоритетом, чтобы ответы серверу не ждали рендер и чанки
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> boostNetty());

        // 3D-косметика: отдельный слой рендера у модели игрока
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new CosmeticFeature(playerRenderer));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (waypointKey.wasPressed()) {
                WaypointManager.addHere();
            }
            while (openMenuKey.wasPressed()) {
                client.setScreen(new NuxClickGui());
            }
            while (cosmeticsKey.wasPressed()) {
                client.setScreen(new com.nuxvisuals.ui.NuxCosmeticsScreen(null));
            }

            if (client.currentScreen instanceof TitleScreen) {
                client.setScreen(new NuxTitleScreen());
            }

            ModuleManager.tick(client);
        });

        // 3D-эффекты модулей (круги, след, частицы)
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.consumers() == null) {
                return;
            }
            MatrixStack matrices = context.matrixStack() != null ? context.matrixStack() : new MatrixStack();
            float tickDelta = context.tickCounter().getTickDelta(false);
            ModuleManager.render(matrices, context.consumers(), context.camera(), tickDelta);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Config.save();
            com.nuxvisuals.render.MotionBlurRenderer.close();
            com.nuxvisuals.music.MediaBridge.stop();
            com.nuxvisuals.music.LyricsService.close();
        });
    }

    private static void boostNetty() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            String n = t.getName();
            if (n.startsWith("Netty") && (n.contains("Client") || n.contains("IO"))) {
                t.setPriority(Thread.MAX_PRIORITY);
            } else if (n.startsWith("Worker-Main") || n.startsWith("Chunk") || n.startsWith("Sodium") || n.startsWith("NuxVisuals-")) {
                t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            }
        }
    }
}
