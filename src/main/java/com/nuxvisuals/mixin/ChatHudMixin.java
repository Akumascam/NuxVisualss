package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.ChatHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// ChatHelper: длинная история, счётчик/скрытие одинаковых сообщений, время сообщений
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow @Final private List<ChatHudLine> messages;

    @Shadow public abstract void addMessage(Text message, MessageSignatureData signature, MessageIndicator indicator);

    @Shadow private void refresh() {
        throw new AssertionError();
    }

    @Unique private static final DateTimeFormatter NUX_TIME = DateTimeFormatter.ofPattern("HH:mm");
    @Unique private final Map<ChatHudLine, String> nux$keys = new WeakHashMap<>();
    @Unique private final Map<ChatHudLine, Integer> nux$counts = new WeakHashMap<>();
    @Unique private boolean nux$inner;
    @Unique private String nux$pendingKey;
    @Unique private int nux$pendingCount;

    @ModifyConstant(method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", "addVisibleMessage",
            "addToMessageHistory"}, constant = @Constant(intValue = 100), require = 0)
    private int nux$historySize(int vanilla) {
        return ChatHelper.historySize(vanilla);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void nux$onMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (this.nux$inner || message == null) {
            return;
        }
        boolean dedupe = ChatHelper.antiDuplicate();
        boolean stamps = ChatHelper.timestamps();
        if (!dedupe && !stamps) {
            return;
        }
        String key = message.getString();
        int count = 1;
        if (dedupe && !key.isBlank()) {
            int depth = Math.min(this.messages.size(), ChatHelper.searchDepth());
            for (int i = 0; i < depth; i++) {
                ChatHudLine line = this.messages.get(i);
                if (key.equals(this.nux$keys.get(line))) {
                    if (ChatHelper.hideDuplicates()) {
                        ci.cancel();
                        return;
                    }
                    count = this.nux$counts.getOrDefault(line, 1) + 1;
                    this.messages.remove(i);
                    this.refresh();
                    break;
                }
            }
        }
        Text out = message;
        if (count > 1) {
            out = message.copy().append(Text.literal(" [x" + count + "]").formatted(Formatting.GRAY));
        }
        if (stamps) {
            out = Text.literal("[" + LocalTime.now().format(NUX_TIME) + "] ").formatted(Formatting.DARK_GRAY).append(out);
        }
        ci.cancel();
        this.nux$inner = true;
        this.nux$pendingKey = key;
        this.nux$pendingCount = count;
        try {
            this.addMessage(out, signature, indicator);
        } finally {
            this.nux$inner = false;
            this.nux$pendingKey = null;
        }
    }

    // запоминаем «чистый» текст новой строки, чтобы потом находить её дубли
    @Inject(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", at = @At("HEAD"))
    private void nux$remember(ChatHudLine line, CallbackInfo ci) {
        if (this.nux$pendingKey != null) {
            this.nux$keys.put(line, this.nux$pendingKey);
            this.nux$counts.put(line, this.nux$pendingCount);
        }
    }
}
