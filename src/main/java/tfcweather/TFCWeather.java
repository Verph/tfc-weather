package tfcweather;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import tfcweather.client.ClientEventHandler;
import tfcweather.common.blockentities.TFCWeatherBlockEntities;
import tfcweather.common.blocks.TFCWeatherBlocks;
import tfcweather.common.items.TFCWeatherItems;
import tfcweather.config.TFCWeatherConfig;

@Mod(TFCWeather.MOD_ID)
public class TFCWeather
{
    public static final String MOD_ID = "tfcweather";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TFCWeather()
    {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        TFCWeatherConfig.init();
        TFCWeatherBlocks.BLOCKS.register(bus);
        TFCWeatherItems.ITEMS.register(bus);
        TFCWeatherBlockEntities.BLOCK_ENTITIES.register(bus);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ClientEventHandler.init();
        }
    }
}