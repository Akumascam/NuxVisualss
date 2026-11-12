package com.nuxvisuals.mixin;

import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

// Для смены аккаунта прямо в игре
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Mutable
    @Accessor("session")
    void nuxvisuals$setSession(Session session);

    @Mutable
    @Accessor("profileKeys")
    void nuxvisuals$setProfileKeys(ProfileKeys profileKeys);

    // профиль, который берёт одиночная игра (ник игрока в мире)
    @Accessor("gameProfileFuture")
    CompletableFuture<ProfileResult> nuxvisuals$getGameProfileFuture();

    @Mutable
    @Accessor("gameProfileFuture")
    void nuxvisuals$setGameProfileFuture(CompletableFuture<ProfileResult> future);
}
