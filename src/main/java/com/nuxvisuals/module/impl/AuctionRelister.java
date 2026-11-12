package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.BoolSetting;
import com.nuxvisuals.module.setting.ModeSetting;
import com.nuxvisuals.module.setting.SliderSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// AH Relister (FunTime): просто перевыставляет предметы на аукционе и выходит из /ah.
// Раз в N минут (или когда в чате пишут, что лот истёк): /ah -> «Хранилище» ->
// один клик по каждому предмету (тот клик, что написан в описании, иначе из настройки) -> закрыть /ah.
// Ничего не забирает в инвентарь и ничего не кликает повторно.
public class AuctionRelister extends Module {
    private static final String[] RELIST_WORDS = {"перевыстав", "выставить снова", "выставить заново", "повторно выстав", "relist"};
    private static final String[] STORAGE_WORDS = {"хранилищ", "истекш", "истёкш", "истек", "истёк", "просроч",
            "непродан", "мои товар", "мои лот", "ваши товар", "ваши лот", "склад", "storage", "expired"};
    private static final String[] CONFIRM_WORDS = {"подтверд", "подтвержд", "confirm"};
    private static final String[] SKIP_WORDS = {"назад", "страниц", "обнов", "закрыть", "выход", "вперёд", "вперед",
            "далее", "сортир", "категор", "поиск", "помощь", "информац"};

    private final SliderSetting interval = add(new SliderSetting("Interval", 10, 1, 120, 1, " min"));
    private final BoolSetting onChat = add(new BoolSetting("Trigger On Chat", true));
    private final ModeSetting fallbackClick = add(new ModeSetting("Relist Click", "Auto",
            "Auto", "Left", "Right", "Shift Left", "Shift Right"));
    private final SliderSetting actionDelay = add(new SliderSetting("Action Delay", 10, 4, 40, 1, " tick"));
    private final BoolSetting notify = add(new BoolSetting("Notify", true));

    private static AuctionRelister instance;

    private long nextRun;
    private int stage = -1;
    private int timer;
    private int waitTicks;
    private int relisted;
    private final Map<Integer, Integer> attempts = new HashMap<>();

    public AuctionRelister() {
        super("ah-relister", "AH Relister", Category.Utilities, false);
        instance = this;
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (instance != null && instance.isEnabled() && !overlay) {
                instance.onChat(message);
            }
        });
    }

    private void onChat(Text message) {
        if (!onChat.get() || stage >= 0) {
            return;
        }
        String s = message.getString().toLowerCase(Locale.ROOT);
        if ((s.contains("истек") || s.contains("истёк") || s.contains("не продан") || s.contains("просроч"))
                && (s.contains("аукцион") || s.contains("лот") || s.contains("предмет") || s.contains("товар"))) {
            nextRun = System.currentTimeMillis() + 3000;
        }
    }

    @Override
    public void onEnable() {
        nextRun = System.currentTimeMillis() + 3000;
        stage = -1;
    }

    @Override
    public void onDisable() {
        stage = -1;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || client.getNetworkHandler() == null || client.interactionManager == null) {
            stage = -1;
            return;
        }
        if (stage < 0) {
            if (System.currentTimeMillis() >= nextRun && client.currentScreen == null) {
                stage = 0;
                timer = 0;
            }
            return;
        }
        if (timer-- > 0) {
            return;
        }
        int delay = actionDelay.getInt();
        switch (stage) {
            case 0 -> { // открыть аукцион
                client.getNetworkHandler().sendChatCommand("ah");
                stage = 1;
                waitTicks = 0;
                relisted = 0;
                attempts.clear();
                timer = delay;
            }
            case 1 -> { // на главной: «перевыставить всё» или вход в хранилище
                ScreenHandler h = container(client);
                if (h == null) {
                    if (++waitTicks > 6) finish(client, "аукцион не открылся");
                    timer = delay;
                    return;
                }
                int all = find(h, RELIST_WORDS, false);
                if (all >= 0) {
                    click(client, h, all, 0, SlotActionType.PICKUP);
                    stage = 3;
                    timer = delay;
                    return;
                }
                int storage = find(h, STORAGE_WORDS, true);
                if (storage < 0) {
                    finish(client, "не нашёл кнопку хранилища/истёкших лотов");
                    return;
                }
                click(client, h, storage, 0, SlotActionType.PICKUP);
                stage = 2;
                timer = delay;
            }
            case 2 -> { // в хранилище: кнопка «перевыставить всё», подтверждение или лоты по одному
                ScreenHandler h = container(client);
                if (h == null) {
                    finish(client, null);
                    return;
                }
                int confirm = find(h, CONFIRM_WORDS, false);
                if (confirm >= 0) {
                    click(client, h, confirm, 0, SlotActionType.PICKUP);
                    timer = delay;
                    return;
                }
                int all = find(h, RELIST_WORDS, false);
                if (all >= 0 && !isLot(h.getSlot(all).getStack())) {
                    click(client, h, all, 0, SlotActionType.PICKUP);
                    stage = 3;
                    timer = delay;
                    return;
                }
                int lot = nextLot(h);
                if (lot < 0 || relisted > 54) {
                    stage = 3;
                    timer = 2;
                    return;
                }
                int[] how = relistClick(h.getSlot(lot).getStack());
                click(client, h, lot, how[0], how[1] == 1 ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
                attempts.merge(lot, 1, Integer::sum);
                relisted++;
                timer = delay;
            }
            case 3 -> { // подтверждение после «перевыставить всё», затем закрыть
                ScreenHandler h = container(client);
                if (h != null) {
                    int confirm = find(h, CONFIRM_WORDS, false);
                    if (confirm >= 0) {
                        click(client, h, confirm, 0, SlotActionType.PICKUP);
                        timer = delay;
                        stage = 4;
                        return;
                    }
                }
                stage = 4;
            }
            default -> {
                if (notify.get()) {
                    InvUtil.actionBar(relisted > 0 || stage == 4
                            ? "§aAH Relister: §fлоты перевыставлены" : "§7AH Relister: истёкших лотов нет");
                }
                finish(client, null);
            }
        }
    }

    private static void click(MinecraftClient client, ScreenHandler h, int slot, int button, SlotActionType type) {
        client.interactionManager.clickSlot(h.syncId, slot, button, type, client.player);
    }

    private void finish(MinecraftClient client, String error) {
        if (error != null && notify.get()) {
            InvUtil.actionBar("§cAH Relister: " + error);
        }
        if (client.player != null && client.currentScreen instanceof HandledScreen<?>) {
            client.player.closeHandledScreen();
        }
        stage = -1;
        nextRun = System.currentTimeMillis() + (long) (interval.get() * 60_000);
    }

    // открытое меню-сундук (не инвентарь игрока)
    private static ScreenHandler container(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
            return null;
        }
        ScreenHandler h = screen.getScreenHandler();
        return h == client.player.playerScreenHandler ? null : h;
    }

    private static int size(ScreenHandler h) {
        return Math.max(0, h.slots.size() - 36);
    }

    private static String fullText(ItemStack s) {
        StringBuilder sb = new StringBuilder(s.getName().getString());
        LoreComponent lore = s.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                sb.append('\n').append(line.getString());
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    // слот, у которого в названии (и, если lore = true, в описании) есть одно из слов
    private static int find(ScreenHandler h, String[] words, boolean lore) {
        int n = size(h);
        for (int pass = 0; pass < (lore ? 2 : 1); pass++) {
            for (int i = 0; i < n; i++) {
                ItemStack s = h.getSlot(i).getStack();
                if (s.isEmpty()) continue;
                String t = pass == 0 ? s.getName().getString().toLowerCase(Locale.ROOT) : fullText(s);
                for (String w : words) {
                    if (t.contains(w)) return i;
                }
            }
        }
        return -1;
    }

    // лот = предмет с ценой/продавцом в описании, не кнопка меню
    private static boolean isLot(ItemStack s) {
        if (s.isEmpty() || s.isOf(Items.GRAY_STAINED_GLASS_PANE) || s.isOf(Items.BLACK_STAINED_GLASS_PANE)
                || s.isOf(Items.LIGHT_GRAY_STAINED_GLASS_PANE) || s.isOf(Items.WHITE_STAINED_GLASS_PANE)
                || s.isOf(Items.ARROW) || s.isOf(Items.BARRIER)) {
            return false;
        }
        String name = s.getName().getString().toLowerCase(Locale.ROOT);
        for (String w : SKIP_WORDS) {
            if (name.contains(w)) return false;
        }
        for (String w : STORAGE_WORDS) {
            if (name.contains(w)) return false;
        }
        return true;
    }

    private int nextLot(ScreenHandler h) {
        int n = size(h);
        for (int i = 0; i < n; i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (isLot(s) && !attempts.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    // какой клик перевыставляет лот: читаем из описания («ПКМ — перевыставить», «Shift+ЛКМ — ...»)
    private int[] relistClick(ItemStack s) {
        LoreComponent lore = s.get(DataComponentTypes.LORE);
        if (fallbackClick.is("Auto") && lore != null) {
            for (Text line : lore.lines()) {
                String l = line.getString().toLowerCase(Locale.ROOT);
                boolean relist = false;
                for (String w : RELIST_WORDS) {
                    if (l.contains(w)) relist = true;
                }
                if (!relist) continue;
                boolean shift = l.contains("shift") || l.contains("шифт");
                boolean right = l.contains("пкм") || l.contains("правой") || l.contains("правый") || l.contains("rmb")
                        || l.contains("right");
                return new int[]{right ? 1 : 0, shift ? 1 : 0};
            }
        }
        return switch (fallbackClick.get()) {
            case "Right" -> new int[]{1, 0};
            case "Shift Left" -> new int[]{0, 1};
            case "Shift Right" -> new int[]{1, 1};
            case "Left" -> new int[]{0, 0};
            default -> new int[]{1, 0}; // Auto без подсказки: на FunTime перевыставление обычно на ПКМ
        };
    }
}
