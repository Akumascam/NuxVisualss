package com.nuxvisuals.module.impl;

import com.nuxvisuals.mixin.HandledScreenAccessor;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BundleItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

// Item Scroller: быстрое перекладывание в инвентаре
//  • колесо вниз над предметом — переложить стак в другой инвентарь
//  • Shift + провести зажатой ЛКМ по слотам — переложить все стаки по пути
//  • Ctrl + провести зажатой ЛКМ — выбросить все стаки по пути
public class ItemScroller extends Module {
    public static ItemScroller INSTANCE;

    private final BoolSetting scroll = add(new BoolSetting("Scroll Move", true));
    private final BoolSetting shiftDrag = add(new BoolSetting("Shift Drag", true));
    private final BoolSetting ctrlDrop = add(new BoolSetting("Ctrl Drag Drop", false));

    private static final Set<Integer> DONE = new HashSet<>();
    private static boolean dragging;

    public ItemScroller() {
        super("item-scroller", "Item Scroller", Category.Utilities, false);
        INSTANCE = this;
    }

    private static boolean on() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    // подключается один раз при запуске (NuxVisualsClient)
    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?> handled)) {
                return;
            }
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, horizontal, vertical) ->
                    !onScroll(handled, vertical));
            ScreenEvents.afterRender(screen).register((s, context, mx, my, delta) -> onFrame(handled));
            ScreenEvents.remove(screen).register(s -> {
                DONE.clear();
                dragging = false;
            });
        });
    }

    private static Slot hovered(HandledScreen<?> screen) {
        return ((HandledScreenAccessor) screen).nuxvisuals$getFocusedSlot();
    }

    private static void click(HandledScreen<?> screen, Slot slot, int button, SlotActionType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) {
            return;
        }
        client.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, button, type, client.player);
    }

    // true — колесо «съели» мы
    private static boolean onScroll(HandledScreen<?> screen, double vertical) {
        if (!on() || !INSTANCE.scroll.get() || vertical >= 0) {
            return false;
        }
        Slot slot = hovered(screen);
        if (slot == null || !slot.hasStack() || slot.getStack().getItem() instanceof BundleItem) {
            return false;
        }
        if (!screen.getScreenHandler().getCursorStack().isEmpty()) {
            return false;
        }
        click(screen, slot, 0, SlotActionType.QUICK_MOVE);
        return true;
    }

    private static void onFrame(HandledScreen<?> screen) {
        if (!on()) {
            return;
        }
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean shift = Screen.hasShiftDown();
        boolean ctrl = Screen.hasControlDown();
        boolean active = lmb && ((shift && INSTANCE.shiftDrag.get()) || (ctrl && INSTANCE.ctrlDrop.get()));
        if (!active) {
            dragging = false;
            DONE.clear();
            return;
        }
        Slot slot = hovered(screen);
        if (!dragging) {
            // первый слот обработала сама игра (Shift+клик)
            dragging = true;
            if (slot != null && shift) {
                DONE.add(slot.id);
            }
        }
        if (slot == null || !slot.hasStack() || DONE.contains(slot.id)) {
            return;
        }
        if (!screen.getScreenHandler().getCursorStack().isEmpty()) {
            return;
        }
        DONE.add(slot.id);
        if (shift) {
            click(screen, slot, 0, SlotActionType.QUICK_MOVE);
        } else {
            click(screen, slot, 1, SlotActionType.THROW);
        }
    }
}
