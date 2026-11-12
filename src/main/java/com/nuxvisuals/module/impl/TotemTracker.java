package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.ModuleManager;
import com.nuxvisuals.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

// Totem Tracker: пишет в чат, когда у игрока слетает тотем
// и какой именно — зачарованный или не зачарованный
public class TotemTracker extends Module {
    private final BoolSetting self = add(new BoolSetting("Self", true));

    public TotemTracker() {
        super("totem-tracker", "Totem Tracker", Category.Utilities, false);
    }

    private static TotemTracker instance() {
        Module m = ModuleManager.get("totem-tracker");
        return m instanceof TotemTracker t ? t : null;
    }

    // вызывается из ClientPlayNetworkHandlerMixin при статусе 35 (тотем сработал)
    public static void onTotemPop(Entity entity) {
        TotemTracker module = instance();
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) {
            return;
        }
        if (player == client.player && !module.self.get()) {
            return;
        }

        // Пакет статуса приходит раньше, чем обновление экипировки,
        // поэтому тотем ещё виден в руке игрока. Порядок как у сервера: основная рука, потом вторая.
        ItemStack totem = ItemStack.EMPTY;
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                totem = stack;
                break;
            }
        }

        MutableText msg = infoPrefix()
                .append(Text.literal("Игрок ").formatted(Formatting.GRAY))
                .append(Text.literal(player.getName().getString()).formatted(Formatting.WHITE))
                .append(Text.literal(" потерял ").formatted(Formatting.GRAY));

        if (totem.isEmpty()) {
            msg.append(Text.literal("тотем").formatted(Formatting.YELLOW));
        } else if (totem.hasGlint()) {
            // зачарованный тотем (зачарования или эффект свечения)
            msg.append(Text.literal("зачарованный").formatted(Formatting.GREEN))
                    .append(Text.literal(" тотем").formatted(Formatting.GRAY));
        } else {
            // обычный тотем
            msg.append(Text.literal("не зачарованный").formatted(Formatting.RED))
                    .append(Text.literal(" тотем").formatted(Formatting.GRAY));
        }

        client.inGameHud.getChatHud().addMessage(msg);
    }

    // гладкий жирный шрифт для префикса: assets/nuxvisuals/font/chat.json
    private static final Identifier CHAT_FONT = Identifier.of("nuxvisuals", "chat");
    private static final int INFO_FROM = 0x3B82F6;
    private static final int INFO_TO = 0x7DD3FC;

    // «info» синим градиентом красивым шрифтом + разделитель
    private static MutableText infoPrefix() {
        String word = "info";
        MutableText prefix = Text.empty();
        for (int i = 0; i < word.length(); i++) {
            float k = word.length() == 1 ? 0f : i / (float) (word.length() - 1);
            int color = lerp(INFO_FROM, INFO_TO, k);
            prefix.append(Text.literal(String.valueOf(word.charAt(i)))
                    .setStyle(Style.EMPTY.withFont(CHAT_FONT).withColor(TextColor.fromRgb(color))));
        }
        prefix.append(Text.literal(" » ").formatted(Formatting.DARK_GRAY));
        return prefix;
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
