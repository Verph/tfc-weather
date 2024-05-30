package tfcweather.mixin.weather2;

import java.util.Random;

import org.spongepowered.asm.mixin.*;

import com.corosus.coroutil.util.CoroUtilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import weather2.config.ConfigSand;
import weather2.config.ConfigSnow;
import weather2.util.WeatherUtilBlock;
import weather2.weathersystem.WeatherManager;
import weather2.weathersystem.storm.EnumWeatherObjectType;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.storm.WeatherObjectParticleStorm;
import weather2.weathersystem.storm.WeatherObjectParticleStorm.StormType;
import weather2.weathersystem.wind.WindManager;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.util.climate.Climate;

import tfcweather.common.TFCWeatherTags;
import tfcweather.common.blocks.TFCWeatherBlocks;
import tfcweather.config.TFCWeatherConfig;
import tfcweather.interfaces.RegistrySand;
import tfcweather.util.TFCWeatherHelpers;

@Mixin(WeatherObjectParticleStorm.class)
public class WeatherObjectParticleStormMixin extends WeatherObject
{
	@Shadow public int age = 0;
	@Shadow public int maxAge = 20*20;
	@Shadow public Random rand = new Random();
	@Shadow public StormType type;

	public WeatherObjectParticleStormMixin(WeatherManager parManager)
	{
		super(parManager);
		this.weatherObjectType = EnumWeatherObjectType.SAND;
	}

    @Overwrite(remap = false)
	public static boolean canSpawnHere(Level level, BlockPos pos, StormType type, boolean forSpawn)
	{
        float temperature = Climate.getTemperature(level, pos);
        float rainfall = Climate.getRainfall(level, pos);

		Biome biomeIn = level.getBiome(pos).value();

		if (ModList.get().isLoaded("tfcbarrens") && type == StormType.SANDSTORM)
		{
			return true;
		}
		if (type == StormType.SANDSTORM)
		{
			return (rainfall >= TFCWeatherConfig.COMMON.sandstormMinRainfall.get() && rainfall <= TFCWeatherConfig.COMMON.sandstormMaxRainfall.get()) && !TFCWeatherHelpers.isBiome(biomeIn, TFCWeatherTags.Biomes.IS_OCEANIC);
		}
		else if (type == StormType.SNOWSTORM)
		{
			return (isColdForStorm(level, biomeIn, pos) || temperature <= TFCWeatherConfig.COMMON.snowstormMaxTemp.get()) && !TFCWeatherHelpers.isBiome(biomeIn, TFCWeatherTags.Biomes.IS_OCEANIC);
		}
		return false;
	}

    @Unique
	private static boolean isColdForStorm(Level world, Biome biome, BlockPos pos)
	{
		return biome.getPrecipitationAt(pos) == Biome.Precipitation.RAIN || biome.getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
	}

	@Shadow
	public float getIntensity()
	{
		return 0F;
	}

    @Unique
	private Block getBlockForBuildup(Level level, BlockPos pos)
	{
		if (this.type == StormType.SNOWSTORM)
		{
			return TFCBlocks.SNOW_PILE.get();
		}
		// Doesn't actually place anything yet
		return TFCWeatherBlocks.SAND_LAYERS.get(SandBlockType.YELLOW).get();
	}

    @Overwrite(remap = false)
	public void tickBlockSandBuildup()
	{
		Level world = manager.getWorld();
		WindManager windMan = manager.getWindManager();

		float angle = windMan.getWindAngleForClouds();

		int delay = ConfigSand.Sandstorm_Sand_Buildup_TickRate;
		int loop = (int)((float)ConfigSand.Sandstorm_Sand_Buildup_LoopAmountBase * getIntensity());
		boolean buildupOutsideArea = ConfigSand.Sandstorm_Sand_Buildup_AllowOutsideDesert;
		int maxBlockStackingAllowed = ConfigSand.Sandstorm_Sand_Block_Max_Height;

		if (getType() == StormType.SNOWSTORM)
		{
			delay = ConfigSnow.Snowstorm_Snow_Buildup_TickRate;
			loop = (int)((float)ConfigSnow.Snowstorm_Snow_Buildup_LoopAmountBase * getIntensity());
			buildupOutsideArea = ConfigSnow.Snowstorm_Snow_Buildup_AllowOutsideColdBiomes;
			maxBlockStackingAllowed = ConfigSnow.Snowstorm_Snow_Block_Max_Height;
		}
		loop += rand.nextInt(50);

		if (!world.isClientSide)
		{
			if (world.getGameTime() % delay == 0)
			{
				Vec3 sandVecPos = getRandomPosInStorm();
				BlockPos sandPos = CoroUtilBlock.blockPos(sandVecPos.x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) sandVecPos.x, (int) sandVecPos.z), sandVecPos.z);
				RegistrySand sandColor = TFCWeatherHelpers.getSandColor(world, sandPos);
				for (int i = 0; i < loop; i++)
				{
					Vec3 vecPos = getRandomPosInStorm();
					BlockPos blockPos = WeatherUtilBlock.getPrecipitationHeightSafe(world, CoroUtilBlock.blockPos(vecPos.x, 0, vecPos.z));
					Vec3 sandVecPosOther = getRandomPosInStorm();
					BlockPos sandPosOther = CoroUtilBlock.blockPos(sandVecPosOther.x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) sandVecPosOther.x, (int) sandVecPosOther.z), sandVecPosOther.z);
					if (buildupOutsideArea || canSpawnHere(world, blockPos, getType(), false))
					{
						Block blockForBuildup = getBlockForBuildup(world, blockPos);
						if (blockForBuildup != null)
						{
							if (rand.nextDouble() >= getIntensity()) continue;
							if (!world.hasChunkAt(blockPos)) continue;

							if (getType() == StormType.SNOWSTORM && rand.nextInt(10) == 0)
							{
								TFCWeatherHelpers.doSnow(world, sandPosOther, Climate.getTemperature(world, sandPosOther));
							}
							if (getType() == StormType.SANDSTORM && rand.nextInt(10) == 0)
							{
								TFCWeatherHelpers.doSand(world, sandPosOther, Climate.getRainfall(world, sandPosOther), sandColor);
							}
							WeatherUtilBlock.fillAgainstWallSmoothly(world, new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), angle, 15, 2, blockForBuildup, maxBlockStackingAllowed);
						}
					}
				}
			}
		}
	}

	@Shadow
	public Vec3 getRandomPosInStorm()
	{
		return null;
	}

	@Shadow
	public StormType getType()
	{
		return type;
	}
}
