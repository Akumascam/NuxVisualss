package com.nuxvisuals.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

// Клики «как мышкой» для AutoClicker / TapeMouse и задержка броска для Fast Exp
@Mixin(MinecraftClient.class)
public interface MinecraftClientInvoker {
    @Invoker("doAttack")
    boolean nuxvisuals$doAttack();

    @Invoker("doItemUse")
    void nuxvisuals$doItemUse();

    @Accessor("itemUseCooldown")
    int nuxvisuals$getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void nuxvisuals$setItemUseCooldown(int cooldown);
}
