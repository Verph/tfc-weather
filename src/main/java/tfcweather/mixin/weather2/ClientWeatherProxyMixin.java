package tfcweather.mixin.weather2;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.corosus.coroutil.util.CoroUtilBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.ModList;
import weather2.ClientWeatherProxy;
import weather2.Weather;
import weather2.datatypes.PrecipitationType;
import weather2.ltcompat.ClientWeatherIntegration;

import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.OverworldClimateModel;

import tfcweather.config.TFCWeatherConfig;

@Mixin(ClientWeatherProxy.class)
public class ClientWeatherProxyMixin
{
    @Overwrite(remap = false)
	@Nullable
	public PrecipitationType getPrecipitationType(Biome biome)
    {
		if (ModList.get().isLoaded("tfcbarrens"))
		{
			return null;
		}
		else if (Weather.isLoveTropicsInstalled())
        {
			return ClientWeatherIntegration.get().getPrecipitationType();
		}
        else
        {
			Minecraft client = Minecraft.getInstance();
			Player player = client.player;

            float temperature = 0F;
            float rainfall = 100F;
			if (player != null)
			{
            	Level level = player.level();
                BlockPos pos = CoroUtilBlock.blockPos(player.getX(), 0, player.getZ());
                temperature = Climate.getTemperature(level, pos);
                rainfall = Climate.getRainfall(level, pos);
            }

			if (biome == null) return null;
			if (temperature > OverworldClimateModel.SNOW_FREEZE_TEMPERATURE) return PrecipitationType.NORMAL;
			if (temperature <= OverworldClimateModel.SNOW_FREEZE_TEMPERATURE) return PrecipitationType.SNOW;
			if (rainfall <= TFCWeatherConfig.COMMON.sandstormMaxRainfall.get()) return null;
		}
		return null;
	}
}
