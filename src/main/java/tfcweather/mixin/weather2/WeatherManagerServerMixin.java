package tfcweather.mixin.weather2;

import com.corosus.coroutil.util.CoroUtilBlock;
import com.corosus.coroutil.util.CoroUtilMisc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import weather2.*;
import weather2.config.ConfigMisc;
import weather2.config.ConfigStorm;
import weather2.config.WeatherUtilConfig;
import weather2.datatypes.StormState;
import weather2.weathersystem.WeatherManager;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.storm.WeatherObjectParticleStorm;
import weather2.weathersystem.wind.WindManager;

import org.spongepowered.asm.mixin.*;

import java.util.*;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.climate.Climate;

import tfcweather.config.TFCWeatherConfig;
import tfcweather.util.TFCWeatherHelpers;

@Mixin(WeatherManagerServer.class)
public class WeatherManagerServerMixin extends WeatherManager
{
	@Shadow private final ServerLevel world;

	public WeatherManagerServerMixin(ServerLevel world)
    {
		super(world.dimension());
		this.world = world;
	}

    @Shadow
	public Level getWorld()
    {
		return world;
	}

    @Overwrite(remap = false)
	public void tick()
	{
		super.tick();

		StormState snowstorm = ServerWeatherProxy.getSnowstormForEverywhere(world);
		if (snowstorm != null)
        {
			tickStormBlockBuildup(snowstorm, TFCBlocks.SNOW_PILE.get());
		}

		StormState sandstorm = ServerWeatherProxy.getSandstormForEverywhere(world);
		if (sandstorm != null)
		{
			for (int i = 0; i < getStormObjects().size(); i++)
			{
				WeatherObject so = getStormObjects().get(i);
				BlockPos blockPos = CoroUtilBlock.blockPos(so.posGround.x, so.posGround.y, so.posGround.z);

				tickStormBlockBuildup(sandstorm, TFCWeatherHelpers.getSandLayerBlock(world, blockPos));
			}
		}

		tickWeatherCoverage();

		if (world != null)
		{
			WindManager windMan = getWindManager();

			getStormObjects().stream()
					.filter(wo -> world.getGameTime() % wo.getUpdateRateForNetwork() == 0)
					.forEach(this::syncStormUpdate);

			if (world.getGameTime() % 60 == 0)
			{
				syncWindUpdate(windMan);
			}

			int rate = 20;
			if (world.getGameTime() % rate == 0)
			{
				for (int i = 0; i < getStormObjects().size(); i++)
				{
					WeatherObject so = getStormObjects().get(i);
					Player closestPlayer = world.getNearestPlayer(so.posGround.x, so.posGround.y, so.posGround.z, ConfigMisc.Misc_simBoxRadiusCutoff, EntitySelector.ENTITY_STILL_ALIVE);

					if (so instanceof StormObject && ((StormObject) so).isPet()) continue;

					if (ConfigMisc.Winter_Wonderland && so instanceof WeatherObjectParticleStorm && ((WeatherObjectParticleStorm) so).getType() == WeatherObjectParticleStorm.StormType.SNOWSTORM)
						continue;

					if (closestPlayer == null || ConfigMisc.Aesthetic_Only_Mode)
					{
						so.ticksSinceNoNearPlayer += rate;
						if (so.ticksSinceNoNearPlayer > 20 * 30 || ConfigMisc.Aesthetic_Only_Mode)
						{
							removeStormObject(so.ID);
							syncStormRemove(so);
						}
					}
					else
					{
						so.ticksSinceNoNearPlayer = 0;
					}
				}

				Random rand = new Random();
				boolean spawnClouds = true;
				if (spawnClouds && !Weather.isLoveTropicsInstalled() && !ConfigMisc.Aesthetic_Only_Mode && WeatherUtilConfig.shouldTickClouds(world.dimension().location().toString()))
				{
					for (int i = 0; i < world.players().size(); i++)
					{
						Player entP = world.players().get(i);
						if (getStormObjects().size() < ConfigStorm.Storm_MaxPerPlayerPerLayer * world.players().size())
						{
							if (rand.nextInt(5) == 0)
							{
								trySpawnStormCloudNearPlayerForLayer(entP, 0);
							}
						}
					}
				}
			}
			if (!Weather.isLoveTropicsInstalled() && WeatherUtilConfig.listDimensionsStorms.contains(world.dimension().location().toString()) && world.getGameTime() % 200 == 0 && windMan.isHighWindEventActive())
			{
				if (!world.players().isEmpty())
				{
					Player entP = world.players().get(CoroUtilMisc.random().nextInt(world.players().size()));

					float temperature = Climate.getTemperature(world, entP.blockPosition());
					float rainfall = Climate.getRainfall(world, entP.blockPosition());
					boolean suitableForSandstorm = rainfall >= TFCWeatherConfig.COMMON.sandstormMinRainfall.get() && rainfall <= TFCWeatherConfig.COMMON.sandstormMaxRainfall.get();
					boolean suitableForSnowstorm = temperature <= TFCWeatherConfig.COMMON.snowstormMaxTemp.get();

					if (ModList.get().isLoaded("tfcbarrens"))
					{
						tryParticleStorm(world, WeatherObjectParticleStorm.StormType.SANDSTORM);
					}
					else if (suitableForSandstorm || suitableForSnowstorm)
					{
						tryParticleStorm(world, suitableForSandstorm ? WeatherObjectParticleStorm.StormType.SANDSTORM : suitableForSnowstorm ? WeatherObjectParticleStorm.StormType.SNOWSTORM : null);
					}
				}
			}
		}
	}

    @Shadow
	public void tickStormBlockBuildup(StormState stormState, Block block) {}

    @Shadow
	public void syncStormRemove(WeatherObject parStorm) {}

    @Shadow
	public void syncWindUpdate(WindManager parManager) {}

    @Shadow
	public void tickWeatherCoverage() {}

    @Shadow
	public void tryParticleStorm(Level level, WeatherObjectParticleStorm.StormType type) {}

    @Shadow
	public boolean trySpawnParticleStormNearPos(Level world, Vec3 posIn, WeatherObjectParticleStorm.StormType type)
	{
		return true;
	}

    @Shadow
	public void syncStormNew(WeatherObject parStorm) {}

    @Shadow
	public void trySpawnStormCloudNearPlayerForLayer(Player entP, int layer) {}

    @Shadow
	public void syncStormUpdate(WeatherObject parStorm) {}
}
