package tfcweather.client.model.render.blockentity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import weather2.ClientTickHandler;
import weather2.client.entity.model.AnemometerModel;
import weather2.weathersystem.WeatherManagerClient;
import weather2.weathersystem.wind.WindManager;

import tfcweather.common.blockentities.TFCAnemometerBlockEntity;
import tfcweather.common.blocks.TFCAnemometerBlock;

public class TFCAnemometerEntityRenderer implements BlockEntityRenderer<TFCAnemometerBlockEntity>
{
    public final AnemometerModel<?> model;
    public final Map<ResourceLocation, Material> materials = new HashMap<>();

    public TFCAnemometerEntityRenderer(final BlockEntityRendererProvider.Context context, ModelLayerLocation model)
    {
        super();
        this.model = new AnemometerModel<>(context.bakeLayer(model));
    }

    @Override
    public void render(TFCAnemometerBlockEntity entity, float partialTicks, PoseStack stack, MultiBufferSource buffers, int combinedLightIn, int combinedOverlayIn)
    {
        if (entity.getBlockState().getBlock() instanceof TFCAnemometerBlock anemometerBlock)
        {
            this.model.root().getAllParts().forEach(ModelPart::resetPose);

            ModelPart root = this.model.root();
            root.x += 8;
            root.y += 8;
            root.z += 8;
            root.xRot += Math.toRadians(180);
            root.yRot += Math.toRadians(180);
            root.y -= 32;

            ModelPart top = this.model.root().getChild("base").getChild("top");
            if (top != null)
            {
                WeatherManagerClient weatherMan = ClientTickHandler.weatherManager;
                if (weatherMan == null) return;
                WindManager windMan = weatherMan.getWindManager();
                if (windMan == null) return;

                float lerpAngle = (float) Mth.lerp((double)partialTicks, entity.smoothAnglePrev, entity.smoothAngle);
                float renderAngle = lerpAngle;

                top.yRot = (float) Math.toRadians(renderAngle);
                boolean shaking = entity.smoothAngleRotationalVel > 45;
                Level level = entity.getLevel();
                if (shaking && level != null)
                {
                    Random rand = new Random(level.getGameTime());
                    top.xRot = (float) ((rand.nextFloat() - rand.nextFloat()) * Math.toRadians(7));
                    top.zRot = (float) ((rand.nextFloat() - rand.nextFloat()) * Math.toRadians(7));
                }
            }
            model.renderToBuffer(stack, buffers.getBuffer(RenderType.entityCutout(anemometerBlock.getTextureLocation())), combinedLightIn, combinedOverlayIn, 1, 1, 1, 1);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TFCAnemometerBlockEntity entity)
    {
        return true;
    }
}