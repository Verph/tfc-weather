package tfcweather.mixin.weather2;

import java.util.Random;

import org.spongepowered.asm.mixin.*;

import com.corosus.coroutil.util.CoroUtilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import weather2.config.ConfigSand;
import weather2.util.WeatherUtilBlock;
import weather2.weathersystem.WeatherManager;
import weather2.weathersystem.storm.EnumWeatherObjectType;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.storm.WeatherObjectParticleStorm.StormType;
import weather2.weathersystem.storm.WeatherObjectSandstormOld;
import weather2.weathersystem.wind.WindManager;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;

import tfcweather.common.TFCWeatherTags;
import tfcweather.common.blocks.TFCWeatherBlocks;
import tfcweather.config.TFCWeatherConfig;
import tfcweather.util.TFCWeatherHelpers;

@Mixin(WeatherObjectSandstormOld.class)
public class WeatherObjectSandstormOldMixin extends WeatherObject
{
	@Shadow public Random rand = new Random();
	@Shadow public int age = 0;
	@Shadow public int maxAge = 20*20;

	public WeatherObjectSandstormOldMixin(WeatherManager parManager)
    {
		super(parManager);
		this.weatherObjectType = EnumWeatherObjectType.SAND;
	}

    @Unique
    private static boolean isOcean(BiomeExtension biome)
    {
        return biome == TFCBiomes.OCEAN || biome == TFCBiomes.DEEP_OCEAN || biome == TFCBiomes.DEEP_OCEAN_TRENCH || biome == TFCBiomes.OCEAN_REEF;
    }

	/**
	 * 0-1F for first half of age, 1-0F for second half of age
	 * @return
	 */
	@Shadow
	public float getIntensity()
    {
		float age = this.age;
		float maxAge = this.maxAge;
		if (age / maxAge <= 0.5F)
        {
			return age / (maxAge/2);
		}
        else
        {
			return 1F - (age / (maxAge/2) - 1F);
		}
	}

    @Unique
	private static boolean canSpawnHere(Level level, BlockPos pos, StormType type)
	{
		if (ModList.get().isLoaded("tfcbarrens") && type == StormType.SANDSTORM)
		{
			return true;
		}

        float rainfall = Climate.getRainfall(level, pos);
		Biome biomeIn = level.getBiome(pos).value();
		return (rainfall >= TFCWeatherConfig.COMMON.sandstormMinRainfall.get() && rainfall <= TFCWeatherConfig.COMMON.sandstormMaxRainfall.get()) && !TFCWeatherHelpers.isBiome(biomeIn, TFCWeatherTags.Biomes.IS_OCEANIC);
	}

    @Unique
	private Block getBlockForBuildup(Level level, BlockPos pos)
	{
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
		loop += rand.nextInt(50);

		if (!world.isClientSide)
		{
			if (world.getGameTime() % delay == 0)
			{
				for (int i = 0; i < loop; i++)
				{
					Vec3 vecPos = getRandomPosInSandstorm();
					BlockPos blockPos = WeatherUtilBlock.getPrecipitationHeightSafe(world, CoroUtilBlock.blockPos(vecPos.x, 0, vecPos.z));
					if (buildupOutsideArea || canSpawnHere(world, blockPos, StormType.SANDSTORM))
					{
						Block blockForBuildup = getBlockForBuildup(world, new BlockPos(blockPos.getX(), world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos.getX(), blockPos.getZ()), blockPos.getZ()));
						if (blockForBuildup != null)
						{
							if (rand.nextDouble() >= getIntensity()) continue;
							if (!world.hasChunkAt(blockPos)) continue;

							if (rand.nextInt(10) == 0)
							{
								TFCWeatherHelpers.doSand(world, blockPos, Climate.getRainfall(world, blockPos), TFCWeatherHelpers.getSandColor(world, blockPos));
							}
							WeatherUtilBlock.fillAgainstWallSmoothly(world, new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), angle, 15, 2, blockForBuildup, maxBlockStackingAllowed);
						}
					}
				}
			}
		}
	}

	@Shadow
	public Vec3 getRandomPosInSandstorm()
    {
		return null;
	}
}
