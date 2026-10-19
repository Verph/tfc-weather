package tfcweather.mixin.weather2;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

			if (player == null || biome == null) return null;
			Level level = player.level();
			BlockPos pos = player.blockPosition();
			float temperature = Climate.getTemperature(level, pos);
			float rainfall = Climate.getRainfall(level, pos);
			if (rainfall <= TFCWeatherConfig.COMMON.sandstormMaxRainfall.get()) return null;
			return temperature <= TFCWeatherConfig.COMMON.snowstormMaxTemp.get() ? PrecipitationType.SNOW : PrecipitationType.NORMAL;
		}
	}

	@Inject(method = "isSnowstorm", at = @At("RETURN"), cancellable = true, remap = false)
	private void restrictSnowstormToColdClimate(CallbackInfoReturnable<Boolean> cir)
	{
		if (cir.getReturnValueZ() && !Weather.isLoveTropicsInstalled())
		{
			Player player = Minecraft.getInstance().player;
			if (player == null || ModList.get().isLoaded("tfcbarrens") || Climate.getTemperature(player.level(), player.blockPosition()) > TFCWeatherConfig.COMMON.snowstormMaxTemp.get())
			{
				cir.setReturnValue(false);
			}
		}
	}
}
