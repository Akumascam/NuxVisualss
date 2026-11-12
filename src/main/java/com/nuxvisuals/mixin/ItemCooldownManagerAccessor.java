package com.nuxvisuals.mixin;
import java.util.Map;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.ItemCooldownManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
    @Accessor("tick") int nux$getTick();
    @Accessor("entries") Map<Identifier, ?> nux$getEntries();
}
