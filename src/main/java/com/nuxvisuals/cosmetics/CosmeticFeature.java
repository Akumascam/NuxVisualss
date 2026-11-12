package com.nuxvisuals.cosmetics;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

// Слой рендера игрока: рисует надетую 3D-косметику, привязанную к голове/телу/рукам
public class CosmeticFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CosmeticFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        CosmeticData d = ((CosmeticHolder) (Object) state).nuxvisuals$data();
        if (d.invisible) {
            return;
        }
        boolean mine = d.me && Cosmetics.count() > 0;
        if (!mine && !d.friend) {
            return;
        }
        PlayerEntityModel model = this.getContextModel();
        Rig rig = (m, anchor) -> {
            switch (anchor) {
                case HEAD -> model.head.rotate(m);
                case BODY -> model.body.rotate(m);
                case RIGHT_ARM -> model.rightArm.rotate(m);
                case LEFT_ARM -> model.leftArm.rotate(m);
                case RIGHT_LEG -> model.rightLeg.rotate(m);
                case LEFT_LEG -> model.leftLeg.rotate(m);
            }
            m.scale(1f / 16f, 1f / 16f, 1f / 16f);
        };
        Draw r = new Draw(matrices, vertexConsumers.getBuffer(RenderLayer.getDebugQuads()),
                Cosmetics.brightness(light), d);
        if (mine) {
            Cosmetics.renderAll(r, rig);
        } else {
            Cosmetics.renderFriend(r, rig);
        }
    }
}
