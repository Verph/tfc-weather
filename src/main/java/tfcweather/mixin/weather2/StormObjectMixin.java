package tfcweather.mixin.weather2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.corosus.coroutil.util.CoroUtilBlock;
import com.corosus.coroutil.util.CoroUtilCompatibility;
import com.corosus.coroutil.util.CoroUtilMisc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.config.ConfigStorm;
import weather2.config.ConfigTornado;
import weather2.config.WeatherUtilConfig;
import weather2.util.WeatherUtilBlock;
import weather2.weathersystem.WeatherManager;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.tornado.simple.TornadoFunnelSimple;

import net.dries007.tfc.common.blockentities.BarrelBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.BarrelBlock;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;

import tfcweather.common.TFCWeatherTags;
import tfcweather.config.TFCWeatherConfig;
import tfcweather.util.TFCWeatherHelpers;

@Mixin(StormObject.class)
public class StormObjectMixin extends WeatherObject
{
	@Shadow public String spawnerUUID = "";

	public StormObjectMixin(WeatherManager parManager)
    {
		super(parManager);
	}

	@Shadow public int sizeMaxFunnelParticles = 600;
	@Shadow public static int static_YPos_layer0 = ConfigMisc.Cloud_Layer0_Height;
	@Shadow public static int static_YPos_layer1 = ConfigMisc.Cloud_Layer1_Height;
	@Shadow public static int static_YPos_layer2 = ConfigMisc.Cloud_Layer2_Height;
	@Shadow public int layer = 0;
	@Shadow public boolean angleIsOverridden = false;
	@Shadow public float angleMovementTornadoOverride = 0;
	@Shadow public float tempAngleFormingTornado = 0;
	@Shadow public boolean isGrowing = true;
	@Shadow public int levelWater = 0; //builds over water and humid biomes, causes rainfall (not technically a storm)
	@Shadow public float levelWindMomentum = 0; //high elevation builds this, plains areas lowers it, 0 = no additional speed ontop of global speed
	@Shadow public float levelTemperature = 0; //negative for cold, positive for warm, we subtract 0.7 from vanilla values to make forest = 0, plains 0.1, ocean -0.5, etc
	@Shadow public int levelWaterStartRaining = 100;
	@Shadow public int levelStormIntensityMax = 0; //calculated from colliding warm and cold fronts, used to determine how crazy a storm _will_ get
	@Shadow public int levelCurIntensityStage = 0; //since we want storms to build up to a climax still, this will start from 0 and peak to levelStormIntensityMax
	@Shadow public float levelCurStagesIntensity = 0;
	@Shadow public boolean hasStormPeaked = false;
	@Shadow public int maxIntensityStage = STATE_STAGE5;
	@Shadow public int stormType = TYPE_LAND;
	@Shadow public static int TYPE_LAND = 0; //for tornados
	@Shadow public static int TYPE_WATER = 1; //for tropical cyclones / hurricanes
	@Shadow public static int STATE_NORMAL = 0;
	@Shadow public static int STATE_THUNDER = 1;
	@Shadow public static int STATE_HIGHWIND = 2;
	@Shadow public static int STATE_HAIL = 3;
	@Shadow public static int STATE_FORMING = 4; //forming tornado for land, for water... stage 0 or something?
	@Shadow public static int STATE_STAGE1 = 5; //these are for both tornados (land) and tropical cyclones (water)
	@Shadow public static int STATE_STAGE2 = 6;
	@Shadow public static int STATE_STAGE3 = 7;
	@Shadow public static int STATE_STAGE4 = 8;
	@Shadow public static int STATE_STAGE5 = 9; //counts as hurricane for water
	@Shadow public static float levelStormIntensityFormingStartVal = STATE_FORMING;
	@Shadow public double spinSpeed = 0.02D;
	@Shadow public boolean attrib_precipitation = false;
	@Shadow public boolean attrib_waterSpout = false;
	@Shadow public float scale = 1F;
	@Shadow public float strength = 100;
	@Shadow public int maxHeight = 60;
	@Shadow public int currentTopYBlock = -1;
	@Shadow private TornadoFunnelSimple tornadoFunnelSimple;
	@Shadow public int updateLCG = (new Random()).nextInt();
    @Shadow public float formingStrength = 0; //for transition from 0 (in clouds) to 1 (touch down)
    @Shadow public Vec3 posBaseFormationPos = new Vec3(pos.x, pos.y, pos.z); //for formation / touchdown progress, where all the ripping methods scan from
    @Shadow public boolean naturallySpawned = true;
	@Shadow public boolean weatherMachineControlled = false;
    @Shadow public boolean canSnowFromCloudTemperature = false;
    @Shadow public boolean alwaysProgresses = false;
    @Shadow public long ticksSinceLastPacketReceived = 0;
    @Shadow public boolean canBeDeadly = true;
	@Shadow public boolean cloudlessStorm = false;
	@Shadow public float cachedAngleAvoidance = 0;
	@Shadow public boolean isFirenado = false;
	@Shadow public List<LivingEntity> listEntitiesUnderClouds = new ArrayList<>();
	@Shadow private boolean playerControlled = false;
	@Shadow private int playerControlledTimeLeft = 20;
	@Shadow private boolean baby = false;
	@Shadow private boolean pet = false;
	@Shadow private boolean petGrabsItems = false;
	@Shadow private boolean sharknado = false;
	@Shadow private boolean configNeedsSync = true;
	@Shadow private int age;
	@Shadow private int ageSinceTornadoTouchdown;
	@Shadow private boolean isBeingDeflectedCached = true;
	@Shadow private boolean debugCloudTemperature = false;

    @Unique
	private int maxIntensityStage(Level level, BlockPos pos)
	{
        final float averageTemp = Climate.getAverageTemperature(level, pos);
        final float rainfall = Climate.getRainfall(level, pos);
        final KoppenClimateClassification koppen = KoppenClimateClassification.classify(averageTemp, rainfall);

        int STATE_HAIL = 3;
        int STATE_STAGE1 = 5; //these are for both tornados (land) and tropical cyclones (water)
        int STATE_STAGE2 = 6;
        int STATE_STAGE3 = 7;
        int STATE_STAGE4 = 8;
        int STATE_STAGE5 = 9; //counts as hurricane for water

        switch(koppen)
        {
            case ARCTIC:
            case TUNDRA:
            case SUBARCTIC:
            case COLD_DESERT:
            case HOT_DESERT:
            case HUMID_SUBARCTIC:
                return STATE_HAIL;
            case TEMPERATE:
                return STATE_STAGE1;
            case SUBTROPICAL:
                return STATE_STAGE2;
            case HUMID_OCEANIC:
            case TROPICAL_SAVANNA:
                return STATE_STAGE3;
            case HUMID_SUBTROPICAL:
                return STATE_STAGE4;
            case TROPICAL_RAINFOREST:
                return STATE_STAGE5;
            default:
                return STATE_STAGE1;
        }
	}

    @Unique
	private double seasonalIntensity()
	{
        final Month month = Calendars.SERVER.getCalendarMonthOfYear();

        switch(month)
        {
			case JANUARY:
				return TFCWeatherConfig.COMMON.valueJanuary.get();
			case FEBRUARY:
				return TFCWeatherConfig.COMMON.valueFebruary.get();
			case MARCH:
				return TFCWeatherConfig.COMMON.valueMarch.get();
			case APRIL:
				return TFCWeatherConfig.COMMON.valueApril.get();
			case MAY:
				return TFCWeatherConfig.COMMON.valueMay.get();
			case JUNE:
				return TFCWeatherConfig.COMMON.valueJune.get();
			case JULY:
				return TFCWeatherConfig.COMMON.valueJuly.get();
			case AUGUST:
				return TFCWeatherConfig.COMMON.valueAugust.get();
			case SEPTEMBER:
				return TFCWeatherConfig.COMMON.valueSeptember.get();
			case OCTOBER:
				return TFCWeatherConfig.COMMON.valueOctober.get();
			case NOVEMBER:
				return TFCWeatherConfig.COMMON.valueNovember.get();
			case DECEMBER:
				return TFCWeatherConfig.COMMON.valueDecember.get();
            default:
                return 1.0D;
        }
	}

    @Overwrite(remap = false)
	public void tickProgression()
    {
		Level world = manager.getWorld();

		if (world.getGameTime() % 3 == 0)
		{
			if (isGrowing)
			{
				if (size < maxSize)
				{
					size++;
				}
			}
		}

		float tempAdjustRate = (float)ConfigStorm.Storm_TemperatureAdjustRate;//0.1F;
		int levelWaterBuildRate = ConfigStorm.Storm_Rain_WaterBuildUpRate;
		int levelWaterSpendRate = ConfigStorm.Storm_Rain_WaterSpendRate;
		int randomChanceOfWaterBuildFromWater = ConfigStorm.Storm_Rain_WaterBuildUpOddsTo1FromSource;
		int randomChanceOfWaterBuildFromNothing = ConfigStorm.Storm_Rain_WaterBuildUpOddsTo1FromNothing;
		int randomChanceOfWaterBuildFromOvercastRaining = ConfigStorm.Storm_Rain_WaterBuildUpOddsTo1FromOvercastRaining;
		randomChanceOfWaterBuildFromOvercastRaining = 10;
		//int randomChanceOfRain = ConfigMisc.Player_Storm_Rain_OddsTo1;

		boolean isInOcean = false;
		boolean isOverWater = false;
		boolean tryFormStorm = false;
		float levelStormIntensityRate = 0.02F + (float) (seasonalIntensity() * 0.015D);
		float minIntensityToProgress = 0.6F / (float) (seasonalIntensity() * 0.9D);

		if (world.getGameTime() % ConfigStorm.Storm_AllTypes_TickRateDelay == 0)
		{
			Player player = getPlayer();
			CompoundTag playerNBT = null;
			if (player != null)
			{
				playerNBT = player.getPersistentData();
			}
			else
			{
				playerNBT = new CompoundTag();
			}

			long lastStormDeadlyTime = playerNBT.getLong("lastStormDeadlyTime");
			if (lastStormDeadlyTime == 0)
			{
				lastStormDeadlyTime = world.getGameTime();
			}
			WeatherManagerServer wm = ServerTickHandler.getWeatherManagerFor(world.dimension());
			if (wm.lastStormFormed == 0)
			{
				wm.lastStormFormed = world.getGameTime();
			}

			Holder<Biome> bgbHolder = world.getBiome(WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(Mth.floor(pos.x), 0, Mth.floor(pos.z))));
			Biome bgb = bgbHolder.get();

			if (bgb != null && TFCBiomes.hasExtension(world, bgb))
			{
				boolean hasBarrensMod = ModList.get().isLoaded("tfcbarrens");
				isInOcean = !hasBarrensMod && (bgbHolder.unwrap().left().toString().toLowerCase().contains("ocean") || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.Biomes.IS_OCEANIC) || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.Biomes.IS_SHORE));

				float biomeTempAdj = getTemperatureMCToWeatherSys(CoroUtilCompatibility.getAdjustedTemperature(manager.getWorld(), bgb, new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z))));
				if (levelTemperature > biomeTempAdj)
				{
					levelTemperature -= tempAdjustRate;
				}
				else
				{
					levelTemperature += tempAdjustRate;
				}
			}

			boolean performBuildup = false;

			Random rand = new Random();

			if (!isPrecipitating() && rand.nextInt(randomChanceOfWaterBuildFromNothing) == 0)
			{
				performBuildup = true;
			}

			if (!isPrecipitating() && ConfigMisc.overcastMode && manager.getWorld().isRaining() &&
					rand.nextInt(randomChanceOfWaterBuildFromOvercastRaining) == 0) {
				performBuildup = true;
			}

			BlockPos tryPos = new BlockPos(Mth.floor(pos.x), currentTopYBlock - 1, Mth.floor(pos.z));
			if (world.isLoaded(tryPos))
			{
				BlockState state = world.getBlockState(tryPos);
				if (!CoroUtilBlock.isAir(state.getBlock()))
				{
					if (world.getBlockState(tryPos).getBlock().defaultMapColor() == MapColor.WATER || EnvironmentHelpers.isWater(state) || state.is(Blocks.WATER) || state.is(TFCBlocks.SALT_WATER.get()))
					{
						isOverWater = true;
					}
				}
			}
			if (!performBuildup && !isPrecipitating() && rand.nextInt(randomChanceOfWaterBuildFromWater) == 0)
			{
				if (isOverWater)
				{
					performBuildup = true;
				}

				if (bgb != null && TFCBiomes.hasExtension(world, bgb))
				{
					String biomecat = bgbHolder.unwrap().left().toString().toLowerCase();

                    final float currentTemp = Climate.getTemperature(world, tryPos);
                    final float rainfall = Climate.getRainfall(world, tryPos);
					final boolean adequateBiome = isInOcean || biomecat.contains("swamp") || biomecat.contains("jungle") || biomecat.contains("river") || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.Biomes.IS_OCEANIC) || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.TFCBiomes.IS_RIVER) || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.TFCBiomes.IS_LAKE) || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.TFCBiomes.IS_VOLCANIC) || TFCWeatherHelpers.isBiome(bgb, TFCWeatherTags.Biomes.IS_SHORE);

					if (!performBuildup && adequateBiome && currentTemp >= TFCWeatherConfig.COMMON.minCurrentTempBuildup.get() && currentTemp <= TFCWeatherConfig.COMMON.maxCurrentTempBuildup.get() && rainfall >= TFCWeatherConfig.COMMON.minRainfallBuildup.get() && rainfall <= TFCWeatherConfig.COMMON.maxRainfallBuildup.get())
					{
						performBuildup = true;
					}
				}
			}
			
			if (performBuildup)
			{
				levelWater += levelWaterBuildRate;
				Weather.dbg(ID + ": building rain: " + levelWater);
			}

			if (isPrecipitating())
			{
				levelWater -= levelWaterSpendRate;
				if (levelWater < 0) levelWater = 0;

				if (levelWater <= 0)
				{
					setPrecipitating(false);
					Weather.dbg("ending raining for: " + ID);
				}
			}
			else
			{
				if (levelWater >= levelWaterStartRaining)
				{
					if (ConfigMisc.overcastMode)
					{
						if (manager.getWorld().isRaining())
						{
							if (ConfigStorm.Storm_Rain_Overcast_OddsTo1 != -1 && rand.nextInt(ConfigStorm.Storm_Rain_Overcast_OddsTo1) == 0)
							{
								setPrecipitating(true);
								Weather.dbg("starting raining for: " + ID);
							}
						}
					}
					else
					{
						if (ConfigStorm.Storm_Rain_OddsTo1 != -1 && rand.nextInt(ConfigStorm.Storm_Rain_OddsTo1) == 0)
						{
							setPrecipitating(true);
							Weather.dbg("starting raining for: " + ID);
						}
					}
				}
			}

			boolean tempAlwaysFormStorm = false;

			if (this.canBeDeadly && this.levelCurIntensityStage == STATE_NORMAL)
			{
				if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate)
				{
					if (ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks != -1)
					{
						if (wm.lastStormFormed == 0 || wm.lastStormFormed + ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks < world.getGameTime())
						{
							tryFormStorm = true;
						}
						else if (ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks_Land_Based != -1 && wm.lastStormFormed + ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks_Land_Based < world.getGameTime())
						{
							tryFormStorm = true;
						}
					}
				}
				else
				{
					if (tempAlwaysFormStorm || ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks != -1)
					{
						if (tempAlwaysFormStorm || lastStormDeadlyTime == 0 || lastStormDeadlyTime + ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks < world.getGameTime())
						{
							tryFormStorm = true;
						}
						else if (ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks_Land_Based != -1 && lastStormDeadlyTime + ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks_Land_Based < world.getGameTime())
						{
							tryFormStorm = true;
						}
					}
				}
			}

			if (weatherMachineControlled)
			{
				return;
            }

			if (((ConfigMisc.overcastMode && manager.getWorld().isRaining()) || !ConfigMisc.overcastMode)
					&& WeatherUtilConfig.listDimensionsStorms.contains(manager.getWorld().dimension().location().toString()) && tryFormStorm)
			{
				int stormFrontCollideDist = ConfigStorm.Storm_Deadly_CollideDistance;
				int randomChanceOfCollide = ConfigStorm.Player_Storm_Deadly_OddsTo1;
				int randomChanceOfCollideLand = ConfigStorm.Player_Storm_Deadly_OddsTo1_Land_Based;

				if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate)
				{
					randomChanceOfCollide = ConfigStorm.Server_Storm_Deadly_OddsTo1;
					randomChanceOfCollideLand = ConfigStorm.Server_Storm_Deadly_OddsTo1_Land_Based;
				}

				if (isInOcean && (ConfigStorm.Storm_OddsTo1OfOceanBasedStorm > 0 && rand.nextInt(ConfigStorm.Storm_OddsTo1OfOceanBasedStorm) == 0))
				{
					Player entP = getPlayer();

					if (entP != null)
					{
						initRealStorm(entP, null);
					}
					else
					{
						initRealStorm(null, null);
					}

					if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate)
					{
						wm.lastStormFormed = world.getGameTime();
					}
					else
					{
						playerNBT.putLong("lastStormDeadlyTime", world.getGameTime());
					}
				}
				else if ((!isInOcean && randomChanceOfCollideLand > 0 && rand.nextInt(randomChanceOfCollideLand) == 0))
				{
					Player entP = getPlayer();

					if (entP != null) {
						initRealStorm(entP, null);
					} else {
						initRealStorm(null, null);
					}

					if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate)
					{
						wm.lastStormFormed = world.getGameTime();
					}
					else
					{
						playerNBT.putLong("lastStormDeadlyTime", world.getGameTime());
					}
				}
				else if (rand.nextInt(randomChanceOfCollide) == 0)
				{
					for (int i = 0; i < manager.getStormObjects().size(); i++)
					{
						WeatherObject wo = manager.getStormObjects().get(i);

						if (wo instanceof StormObject)
						{
							StormObject so = (StormObject) wo;

							boolean startStorm = false;

							if (so.ID != this.ID && so.levelCurIntensityStage <= 0 && !so.isCloudlessStorm() && !so.weatherMachineControlled)
							{
								if (so.pos.distanceTo(pos) < stormFrontCollideDist)
								{
									if (this.levelTemperature < 0)
									{
										if (so.levelTemperature > 0)
										{
											startStorm = true;
										}
									}
									else if (this.levelTemperature > 0)
									{
										if (so.levelTemperature < 0)
										{
											startStorm = true;
										}
									}
								}
							}

							if (startStorm)
							{
								playerNBT.putLong("lastStormDeadlyTime", world.getGameTime());

								Player entP = getPlayer();

								if (entP != null)
								{
									initRealStorm(entP, so);
								}
								else
								{
									initRealStorm(null, so);
								}
								break;
							}
						}
					}
				}
			}

			if (isRealStorm())
			{
				if (ConfigMisc.overcastMode)
				{
					if (!manager.getWorld().isRaining())
					{
						hasStormPeaked = true;
					}
				}
				if (!hasStormPeaked)
				{
					levelWater = levelWaterStartRaining;
					setPrecipitating(true);
				}
				if ((levelCurIntensityStage == STATE_HIGHWIND || levelCurIntensityStage == STATE_HAIL) && isOverWater)
				{
					if (ConfigStorm.Storm_OddsTo1OfHighWindWaterSpout != 0 && rand.nextInt(ConfigStorm.Storm_OddsTo1OfHighWindWaterSpout) == 0)
					{
						attrib_waterSpout = true;
					}
				}
				else
				{
					attrib_waterSpout = false;
				}
				if (!hasStormPeaked)
				{
					
					if (levelCurIntensityStage < maxIntensityStage && (!ConfigTornado.Storm_NoTornadosOrCyclones || levelCurIntensityStage < STATE_FORMING-1))
					{
						if (levelCurStagesIntensity >= minIntensityToProgress)
						{
							if (alwaysProgresses || levelCurIntensityStage < levelStormIntensityMax)
							{
								stageNext();
								Weather.dbg("storm ID: " + this.ID + " - growing, stage: " + levelCurIntensityStage + " pos: " + pos);
								if (isInOcean && false)
								{
									if (levelCurIntensityStage == STATE_FORMING)
									{
										stormType = TYPE_WATER;
										levelStormIntensityMax = rollDiceOnMaxIntensity();
									}
								}
							}
						}
						if (levelCurIntensityStage == STATE_FORMING)
						{
							if (levelCurStagesIntensity >= 0.5 && levelCurStagesIntensity < 0.9)
							{
								levelCurStagesIntensity = 0.90F;
							}
						}
					}
					if (levelCurStagesIntensity >= 1F)
					{
						Weather.dbg("storm peaked at: " + levelCurIntensityStage);
						hasStormPeaked = true;
					}
				}
				else
				{
					if (ConfigMisc.overcastMode && manager.getWorld().isRaining())
					{
						levelCurStagesIntensity -= levelStormIntensityRate * 0.9F;
					}
					else
					{
						levelCurStagesIntensity -= levelStormIntensityRate * 0.3F;
						if (levelCurIntensityStage >= STATE_FORMING)
						{
							Weather.dbg("storm ID: " + this.ID + " - active info, stage: " + levelCurIntensityStage + " levelCurStagesIntensity: " + levelCurStagesIntensity + " pos: " + pos);
						}
					}
					if (levelCurIntensityStage == STATE_FORMING)
					{
						if (levelCurStagesIntensity > 0.5)
						{
							levelCurStagesIntensity = 0.5F;
						}
						levelCurStagesIntensity -= levelStormIntensityRate * 0.9F;
					}
					if (levelCurStagesIntensity <= 0)
					{
						stagePrev();
						Weather.dbg("storm ID: " + this.ID + " - dying, stage: " + levelCurIntensityStage + " pos: " + pos);
						if (levelCurIntensityStage <= 0)
						{
							setNoStorm();
						}
					}
					
					
				}
			}
			else
			{
				if (ConfigMisc.overcastMode)
				{
					if (!manager.getWorld().isRaining())
					{
						if (attrib_precipitation)
						{
							setPrecipitating(false);
						}
					}
				}
			}
		}
		if (world.getGameTime() % 2 == 0)
		{
			if (((ConfigMisc.overcastMode && manager.getWorld().isRaining()) || !ConfigMisc.overcastMode) && WeatherUtilConfig.listDimensionsStorms.contains(manager.getWorld().dimension().location().toString()))
			{
				if (isRealStorm() && !hasStormPeaked)
				{
					if (levelCurIntensityStage >= levelStormIntensityFormingStartVal)
					{
						levelStormIntensityRate *= 3;
					}
					levelCurStagesIntensity += levelStormIntensityRate / 30F;
				}
			}
		}
		if (playerControlled)
		{
			if (playerControlledTimeLeft > 0)
			{
				playerControlledTimeLeft--;
				if (playerControlledTimeLeft <= 0)
				{
					featherFallAllNearbyPlayers();
					remove();
				}
			}
		}
	}

    @Inject(method = "tickWeatherEvents", at = @At("HEAD"), remap = false)
	private void inject$tickWeatherEvents(CallbackInfo ci)
	{
		if (isPrecipitating() || (ConfigMisc.overcastMode && manager.getWorld().isRaining()))
		{
			Level level = manager.getWorld();
			if (level != null && level.getGameTime() % TFCWeatherConfig.COMMON.barrelFillRate.get() <= levelCurIntensityStage)
			{
				Random random = CoroUtilMisc.random();
				double gaussDistribution = random.nextGaussian() * (size * 0.5D);
				int x = (int) Math.floor(pos.x + gaussDistribution);
				int z = (int) Math.floor(pos.z + gaussDistribution);
				int y = (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1) + random.nextInt(3);
				BlockPos blockPos = new BlockPos(x, y, z);

				if (level.isLoaded(new BlockPos(blockPos)))
				{
					if ((level.canSeeSky(blockPos) || level.isRainingAt(blockPos.above())) && level.getBlockEntity(blockPos) instanceof BarrelBlockEntity barrel)
					{
						final boolean sealed = level.getBlockState(blockPos).getValue(BarrelBlock.SEALED);
						if (!sealed)
						{
							barrel.getCapability(Capabilities.FLUID, Direction.UP).ifPresent(cap -> cap.fill(new FluidStack(Fluids.WATER, (int) ((levelCurIntensityStage + 1)^2)), IFluidHandler.FluidAction.EXECUTE));
							barrel.markForSync();
						}
					}
				}
			}
		}
	}

    @Shadow
	public void tickClient() {}

    @Shadow
	public void tickWeatherEvents() {}

    @Shadow
	public void tickMovement() {}

    @Shadow
	public static float getTemperatureMCToWeatherSys(float parOrigVal)
    {
		return 1;
	}

    @Shadow
	public boolean isCloudlessStorm()
    {
		return false;
	}

    @Shadow
	public void setCloudlessStorm(boolean cloudlessStorm) {}

    @Shadow
	public boolean isTornadoFormingOrGreater()
    {
		return false;
	}

    @Shadow
	public boolean isCycloneFormingOrGreater()
    {
		return false;
	}

    @Shadow
	public boolean isSpinning()
    {
		return false;
	}

    @Shadow
	public boolean isTropicalCyclone()
    {
		return false;
	}

    @Shadow
	public boolean isHurricane()
    {
		return false;
	}

    @Shadow
	public boolean isPrecipitating()
    {
		return false;
	}

    @Shadow
	public void setPrecipitating(boolean parVal) {}

    @Shadow
	public void initRealStorm(Player entP, StormObject stormToAbsorb) {}

    @Shadow
	public boolean isRealStorm()
    {
		return false;
    }

    @Shadow
	public void stageNext() {}

    @Shadow
	public int rollDiceOnMaxIntensity()
    {
		return 0;
	}

    @Shadow
	public void stagePrev() {}

    @Shadow
	public void setNoStorm() {}

    @Shadow
	public void featherFallAllNearbyPlayers() {}

    @Shadow
	public Player getPlayer()
	{
		if (spawnerUUID.equals("")) return null;
		return manager.getWorld().getPlayerByUUID(UUID.fromString(spawnerUUID));
	}
}