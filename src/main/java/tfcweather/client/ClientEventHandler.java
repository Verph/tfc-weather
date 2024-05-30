package tfcweather.client;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import weather2.client.entity.model.AnemometerModel;
import weather2.client.entity.model.WindVaneModel;

import net.dries007.tfc.util.Metal;

import tfcweather.client.model.render.blockentity.TFCAnemometerEntityRenderer;
import tfcweather.client.model.render.blockentity.TFCWindVaneEntityRenderer;
import tfcweather.common.TFCWeatherCreativeTabs;
import tfcweather.common.blockentities.TFCWeatherBlockEntities;
import tfcweather.util.TFCWeatherHelpers;

public class ClientEventHandler
{
    public static void init()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(TFCWeatherCreativeTabs::onBuildCreativeTab);
        bus.addListener(ClientEventHandler::registerEntityRenderers);
        bus.addListener(ClientEventHandler::registerLayerDefinitions);
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        for (Metal.Default metal : Metal.Default.values())
        {
            if (metal.hasParts())
            {
                event.registerBlockEntityRenderer(TFCWeatherBlockEntities.ANEMOMETER.get(), ctx -> new TFCAnemometerEntityRenderer(ctx, TFCWeatherHelpers.modelIdentifier("anemometer")));
                event.registerBlockEntityRenderer(TFCWeatherBlockEntities.WIND_VANE.get(), ctx -> new TFCWindVaneEntityRenderer(ctx, TFCWeatherHelpers.modelIdentifier("wind_vane")));
            }
        }
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        for (Metal.Default metal : Metal.Default.values())
        {
            if (metal.hasParts())
            {
                event.registerLayerDefinition(TFCWeatherHelpers.modelIdentifier("anemometer"), AnemometerModel::createBodyLayer);
                event.registerLayerDefinition(TFCWeatherHelpers.modelIdentifier("wind_vane"), WindVaneModel::createBodyLayer);
            }
        }
    }
}
