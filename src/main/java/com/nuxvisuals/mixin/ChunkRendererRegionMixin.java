package com.nuxvisuals.mixin;

import com.nuxvisuals.module.impl.WorldCustomizer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// World Customizer «Snow Cover»: копия мира, из которой строятся меши чанков, «видит» слой снега.
// «Snow Everywhere» — на каждой открытой сверху грани (в домах, под деревьями, на дорожках),
// иначе — только на самом верхнем блоке под открытым небом. Трава/подзол/мицелий под снегом — заснеженные.
// Меняется только картинка: физика, сервер и коллизии используют настоящий мир.
@Mixin(ChunkRendererRegion.class)
public abstract class ChunkRendererRegionMixin {
    @Unique
    private static final ThreadLocal<Boolean> nuxvisuals$nested = ThreadLocal.withInitial(() -> false);

    @Unique
    private static boolean nuxvisuals$holdsSnow(BlockState below) {
        if (!below.getFluidState().isEmpty() || below.isOf(Blocks.ICE) || below.isOf(Blocks.PACKED_ICE)
                || below.isOf(Blocks.BLUE_ICE) || below.isOf(Blocks.SNOW)) {
            return false;
        }
        if (below.isOpaqueFullCube() || below.getBlock() instanceof LeavesBlock) {
            return true;
        }
        // дорожки, грядки, верхние/двойные полублоки, стекло и прочие полные блоки
        if (below.isOf(Blocks.DIRT_PATH) || below.isOf(Blocks.FARMLAND) || below.isOf(Blocks.SNOW_BLOCK)) {
            return true;
        }
        if (below.getBlock() instanceof SlabBlock && below.contains(SlabBlock.TYPE)) {
            return below.get(SlabBlock.TYPE) != SlabType.BOTTOM;
        }
        return below.isFullCube(net.minecraft.world.EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true, require = 0)
    private void nuxvisuals$snow(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!WorldCustomizer.snowCover() || nuxvisuals$nested.get()) {
            return;
        }
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }
        boolean everywhere = WorldCustomizer.snowEverywhere();
        BlockState state = cir.getReturnValue();
        boolean air = state.isAir();
        boolean snowable = !air && state.contains(Properties.SNOWY) && !state.get(Properties.SNOWY);
        if (!air && !snowable) {
            return;
        }
        int top = Integer.MIN_VALUE;
        if (!everywhere) {
            try {
                top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
            } catch (Exception e) {
                return;
            }
        }
        ChunkRendererRegion self = (ChunkRendererRegion) (Object) this;
        nuxvisuals$nested.set(true);
        try {
            if (air) {
                if (!everywhere && pos.getY() != top) {
                    return;
                }
                if (nuxvisuals$holdsSnow(self.getBlockState(pos.down()))) {
                    cir.setReturnValue(Blocks.SNOW.getDefaultState());
                }
            } else {
                boolean covered = everywhere ? self.getBlockState(pos.up()).isAir() : pos.getY() + 1 == top;
                if (covered) {
                    cir.setReturnValue(state.with(Properties.SNOWY, true));
                }
            }
        } finally {
            nuxvisuals$nested.set(false);
        }
    }
}
