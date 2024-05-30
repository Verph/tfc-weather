package tfcweather.mixin.weather2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviorSandstorm;
import extendedrenderer.particle.entity.*;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import weather2.*;
import weather2.config.ConfigMisc;
import weather2.datatypes.PrecipitationType;
import weather2.datatypes.WeatherEventType;
import weather2.client.SceneEnhancer;
import weather2.client.entity.particle.ParticleHail;
import weather2.client.entity.particle.ParticleSandstorm;
import weather2.config.ConfigParticle;
import weather2.config.ConfigSand;
import weather2.util.*;
import weather2.weathersystem.WeatherManagerClient;
import weather2.weathersystem.fog.FogAdjuster;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObjectParticleStorm;
import weather2.weathersystem.wind.WindManager;

import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.dries007.tfc.util.climate.Climate;

import com.corosus.coroutil.util.CULog;
import com.corosus.coroutil.util.CoroUtilBlock;
import com.corosus.coroutil.util.CoroUtilCompatibility;
import com.corosus.coroutil.util.CoroUtilMisc;

import tfcweather.common.TFCWeatherTags;
import tfcweather.config.TFCWeatherConfig;

@Mixin(SceneEnhancer.class)
@OnlyIn(Dist.CLIENT)
public abstract class SceneEnhancerMixin
{
	@Shadow private static final double PRECIPITATION_PARTICLE_EFFECT_RATE = 0.7;

    @Shadow public static List<Block> LEAVES_BLOCKS = new ArrayList<>();

	@Shadow private static final List<BlockPos> listPosRandom = new ArrayList<>();

	@Shadow public static boolean FORCE_ON_DEBUG_TESTING = false;

	@Shadow public static ParticleBehaviorSandstorm particleBehavior;

	@Shadow private static FogAdjuster fogAdjuster;

	@Shadow public static boolean isPlayerOutside = true;
	@Shadow public static boolean isPlayerNearTornadoCached = false;

	@Shadow private static Biome lastBiomeIn = null;
	@Shadow public static float downfallSheetThreshold = 0.32F;

    @Overwrite(remap = false)
	public void tickEnvironmentalParticleSpawning()
    {
		if (!FMLEnvironment.dist.isClient()) return;

		FORCE_ON_DEBUG_TESTING = false;

		Player entP = Minecraft.getInstance().player;
		WeatherManagerClient weatherMan = ClientTickHandler.weatherManager;
		if (weatherMan == null) return;
		WindManager windMan = weatherMan.getWindManager();
		if (windMan == null) return;
		if (particleBehavior == null) return;

		ClientWeatherProxy weather = ClientWeatherProxy.get();

		float curPrecipVal = weather.getRainAmount();

		if (FORCE_ON_DEBUG_TESTING)
		{
			curPrecipVal = 0.3F;
		}

		if (Weather.isLoveTropicsInstalled())
		{
			if (curPrecipVal < 0.0001F)
			{
				curPrecipVal = 0;
			}
		}
		float maxPrecip = 1F;

        BlockPos posPlayer = CoroUtilBlock.blockPos(Mth.floor(entP.getX()), Mth.floor(entP.getY()), Mth.floor(entP.getZ()));
		Biome biome = entP.level().getBiome(posPlayer).get();
		lastBiomeIn = biome;

		Level world = entP.level();
		Random rand = CoroUtilMisc.random();
		float windSpeed = windMan.getWindSpeed(posPlayer);

		if (Weather.isLoveTropicsInstalled())
		{
			curPrecipVal = Math.min(maxPrecip, Math.max(0, curPrecipVal));
		}
		else
		{
			curPrecipVal = Math.min(maxPrecip, Math.abs(curPrecipVal));
		}

		float particleSettingsAmplifier = 1F;
		if (Minecraft.getInstance().options.particles.get() == ParticleStatus.DECREASED)
		{
			particleSettingsAmplifier = 0.5F;
		}
		else if (Minecraft.getInstance().options.particles.get() == ParticleStatus.MINIMAL)
		{
			particleSettingsAmplifier = 0.2F;
		}

		particleSettingsAmplifier *= ConfigParticle.Particle_effect_rate;
		float curPrecipValMaxNoExtraRender = 0.3F;
		int extraRenderCountMax = 10;
		int extraRenderCount = 0;

		if (curPrecipVal > curPrecipValMaxNoExtraRender)
		{
			float precipValForExtraRenders = curPrecipVal - curPrecipValMaxNoExtraRender;
			float precipValExtraRenderRange = 1F - curPrecipValMaxNoExtraRender;
			extraRenderCount = Math.min((int) (extraRenderCountMax * (precipValForExtraRenders / precipValExtraRenderRange)), extraRenderCountMax);
		}

		float curPrecipCappedForSpawnNeed = Math.min(curPrecipVal, curPrecipValMaxNoExtraRender);

		int spawnCount;
		double spawnNeedBase = curPrecipCappedForSpawnNeed * ConfigParticle.Precipitation_Particle_effect_rate * particleSettingsAmplifier;
		int safetyCutout = 100;

		if (world.getGameTime() % 20 == 0)
		{
			StormObject stormObject = ClientTickHandler.weatherManager.getClosestStorm(entP.position(), ConfigMisc.sirenActivateDistance, StormObject.STATE_FORMING);
			if (stormObject != null && entP.position().distanceTo(stormObject.pos) < stormObject.getSize())
			{
				isPlayerNearTornadoCached = true;
			}
			else
			{
				isPlayerNearTornadoCached = false;
			}
		}

        final float temperaturePlayer = Climate.getTemperature(world, posPlayer);
        final float rainfallPlayer = Climate.getRainfall(world, posPlayer);
		final double snowstormMaxTemp = TFCWeatherConfig.COMMON.snowstormMaxTemp.get();
		final double sandstormMaxRainfall = TFCWeatherConfig.COMMON.sandstormMaxRainfall.get();

        boolean hasBarrensMod = ModList.get().isLoaded("tfcbarrens");
		boolean canPrecip = !hasBarrensMod && (weather.getPrecipitationType(biome) == PrecipitationType.NORMAL || weather.getPrecipitationType(biome) == PrecipitationType.SNOW);
		boolean canSnow = temperaturePlayer <= snowstormMaxTemp && rainfallPlayer > sandstormMaxRainfall;
		boolean canRain = !canSnow;

		boolean isRain = canPrecip && canRain;
		boolean isHail = weather.isHail();
		boolean isSnowstorm = weather.isSnowstorm();
		boolean isSandstorm = weather.isSandstorm();
		boolean isSnow = (canPrecip && canSnow) || (canPrecip && canSnow && shouldSnowHere(world, biome, posPlayer));
		boolean isRain_WaterParticle = (temperaturePlayer > snowstormMaxTemp && rainfallPlayer > sandstormMaxRainfall);
		boolean isRain_GroundSplash = (temperaturePlayer > snowstormMaxTemp && rainfallPlayer > sandstormMaxRainfall);
		boolean isRain_DownfallSheet = (temperaturePlayer > snowstormMaxTemp && rainfallPlayer > sandstormMaxRainfall);

		if (isHail && temperaturePlayer <= snowstormMaxTemp)
		{
			isRain_WaterParticle = false;
			isRain_GroundSplash = false;
			isRain_DownfallSheet = false;
		}

		if (isPlayerNearTornadoCached)
		{
			isRain_DownfallSheet = false;
		}

		boolean farSpawn = Minecraft.getInstance().player.isSpectator() || !isPlayerOutside;
		float particleStormIntensity = 0;
		if (isSandstorm)
		{
			WeatherObjectParticleStorm storm = ClientTickHandler.weatherManager.getClosestParticleStormByIntensity(entP.position(), WeatherObjectParticleStorm.StormType.SANDSTORM);
			if (storm != null)
			{
				particleStormIntensity = storm.getIntensity();
			}
		}
		if (isSnowstorm)
		{
			WeatherObjectParticleStorm storm = ClientTickHandler.weatherManager.getClosestParticleStormByIntensity(entP.position(), WeatherObjectParticleStorm.StormType.SNOWSTORM);
			if (storm != null)
			{
				particleStormIntensity = storm.getIntensity();
			}
		}

		if (biome != null && (biome.getPrecipitationAt(posPlayer) != Biome.Precipitation.NONE))
		{
			if (curPrecipVal > 0)
			{
				if (isRain && Climate.getRainfall(world, entP.blockPosition()) > sandstormMaxRainfall)
				{
					spawnCount = 0;
					int spawnAreaSize = 30;

					int spawnNeed = (int) (spawnNeedBase * 300);
					if (isRain_WaterParticle && spawnNeed > 0)
					{
						for (int i = 0; i < safetyCutout; i++)
						{
							BlockPos pos = CoroUtilBlock.blockPos(
									entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
									entP.getY() - 5 + rand.nextInt(25),
									entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

							if (canPrecipitateAt(world, pos))
							{
								ParticleTexExtraRender rain = new ParticleTexExtraRender((ClientLevel) entP.level(),
										pos.getX(),
										pos.getY(),
										pos.getZ(),
										0D, 0D, 0D, ParticleRegistry.rain_white);
								particleBehavior.initParticleRain(rain, extraRenderCount);

								spawnCount++;
								if (spawnCount >= spawnNeed)
								{
									break;
								}
							}
						}
					}
					spawnAreaSize = 40;
					if (isRain_GroundSplash && curPrecipVal > 0.15)
					{
						for (int i = 0; i < 30F * curPrecipVal * PRECIPITATION_PARTICLE_EFFECT_RATE * 8F * particleSettingsAmplifier; i++)
						{
							BlockPos pos = CoroUtilBlock.blockPos(
									entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
									entP.getY() - 5 + rand.nextInt(15),
									entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

							pos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).below();

							BlockState state = world.getBlockState(pos);
							double maxY = 0;
							double minY = 0;
							VoxelShape shape = state.getShape(world, pos);
							if (!shape.isEmpty())
							{
								minY = shape.bounds().minY;
								maxY = shape.bounds().maxY;
							}

							if (pos.distSqr(entP.blockPosition()) > (spawnAreaSize / 2) * (spawnAreaSize / 2))
								continue;

							if (canPrecipitateAt(world, pos.above()))
							{
								if (world.getBlockState(pos).getBlock().defaultMapColor() == MapColor.WATER)
								{
									pos = pos.offset(0,1,0);
								}

								ParticleTexFX rain = new ParticleTexFX((ClientLevel) entP.level(),
										pos.getX() + rand.nextFloat(),
										pos.getY() + 0.01D + maxY,
										pos.getZ() + rand.nextFloat(),
										0D, 0D, 0D, ParticleRegistry.groundSplash);
								particleBehavior.initParticleGroundSplash(rain);

								rain.spawnAsWeatherEffect();
							}
						}
					}
					spawnAreaSize = 30;
					if (isRain_DownfallSheet && curPrecipVal > downfallSheetThreshold)
					{
						int scanAheadRange = 0;

						if (WeatherUtilDim.canBlockSeeSky(world, entP.blockPosition()))
						{
							scanAheadRange = 3;
						}
						else
						{
							scanAheadRange = 10;
						}

						double closeDistCutoff = 10D;

						for (int i = 0; i < 2F * curPrecipVal * PRECIPITATION_PARTICLE_EFFECT_RATE * particleSettingsAmplifier * 0.5F; i++)
						{
							BlockPos pos = CoroUtilBlock.blockPos(
									entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
									entP.getY() + 5 + rand.nextInt(15),
									entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

							if (WeatherUtilEntity.getDistanceSqEntToPos(entP, pos) < closeDistCutoff * closeDistCutoff) continue;

							if (canPrecipitateAt(world, pos.above(-scanAheadRange)))
							{
								ParticleTexFX rain = new ParticleTexFX((ClientLevel) entP.level(),
										pos.getX() + rand.nextFloat(),
										pos.getY() - 1 + 0.01D,
										pos.getZ() + rand.nextFloat(),
										0D, 0D, 0D, ParticleRegistry.downfall3);
								particleBehavior.initParticleRainDownfall(rain);

								rain.spawnAsWeatherEffect();
							}
						}
					}
				}
				else if (isSnow)
				{
					spawnCount = 0;
					int spawnAreaSize = 50;
					int spawnNeed = (int) (spawnNeedBase * 80);
					if (entP.level().getGameTime() % 40 == 0)
					{
						CULog.dbg("rain spawnNeed: " + spawnNeed);
					}
					if (spawnNeed > 0)
					{
						for (int i = 0; i < safetyCutout; i++)
						{
							BlockPos pos = CoroUtilBlock.blockPos(
									entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
									entP.getY() - 5 + rand.nextInt(25),
									entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

							if (canPrecipitateAt(world, pos))
							{
								ParticleTexExtraRender snow = new ParticleTexExtraRender((ClientLevel) entP.level(), pos.getX(), pos.getY(), pos.getZ(),
										0D, 0D, 0D, ParticleRegistry.snow);

										particleBehavior.initParticleSnow(snow, extraRenderCount, windSpeed);
								snow.spawnAsWeatherEffect();

								spawnCount++;
								if (spawnCount >= spawnNeed)
								{
									break;
								}
							}
						}
					}
				}

				int spawnAreaSize = 30;
				spawnCount = 0;
				int spawnNeed = (int) (spawnNeedBase * 80);

				if ((getWeatherState() == WeatherEventType.HAIL || isHail) && spawnNeed > 0)
				{
					for (int i = 0; i < safetyCutout / 4; i++)
					{
						BlockPos pos = CoroUtilBlock.blockPos(
								entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
								entP.getY() - 5 + rand.nextInt(25),
								entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

						if (canPrecipitateAt(world, pos))
						{
							ParticleHail hail = new ParticleHail((ClientLevel) entP.level(),
									pos.getX(),
									pos.getY(),
									pos.getZ(),
									0D, 0D, 0D, ParticleRegistry.hail);

							particleBehavior.initParticleHail(hail);

							hail.spawnAsWeatherEffect();

							spawnCount++;
							if (spawnCount >= spawnNeed)
							{
								break;
							}
						}
					}
				}
			}

			boolean groundFire = ClientWeatherProxy.get().isHeatwave();
			int spawnAreaSize = 40;

			if (groundFire)
			{
				for (int i = 0; i < 10F * PRECIPITATION_PARTICLE_EFFECT_RATE * 1F * particleSettingsAmplifier; i++)
				{
					BlockPos pos = CoroUtilBlock.blockPos(
							entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
							entP.getY() - 5 + rand.nextInt(15),
							entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

					pos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).below();

					BlockState state = world.getBlockState(pos);
					double maxY = 0;
					double minY = 0;
					VoxelShape shape = state.getShape(world, pos);
					if (!shape.isEmpty())
					{
						minY = shape.bounds().minY;
						maxY = shape.bounds().maxY;
					}

					if (pos.distSqr(entP.blockPosition()) > (spawnAreaSize / 2) * (spawnAreaSize / 2))
						continue;

					if (canPrecipitateAt(world, pos.above()) && world.getBlockState(pos).getBlock().defaultMapColor() != MapColor.WATER)
					{
						world.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 0.01D + maxY, pos.getZ() + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
						world.addParticle(ParticleTypes.FLAME, pos.getX() + rand.nextFloat(), pos.getY() + 0.01D + maxY, pos.getZ() + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
					}
				}
			}
		}

		{
			int spawnAreaSize = 25;
			int spawnNeed = (int) (particleSettingsAmplifier * 5 * windSpeed);
			spawnCount = 0;

			for (int i = 0; i < safetyCutout; i++)
			{
				if (spawnCount >= spawnNeed)
				{
					break;
				}
				if (windSpeed >= 0.1F)
				{
					BlockPos pos = CoroUtilBlock.blockPos(
							entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
							entP.getY() - 5 + rand.nextInt(25),
							entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

					if (canPrecipitateAt(world, pos))
					{
						ParticleTexExtraRender dust = new ParticleTexExtraRender((ClientLevel) entP.level(),
								pos.getX(),
								pos.getY(),
								pos.getZ(),
								0D, 0D, 0D, ParticleRegistry.squareGrey);
						particleBehavior.initParticleDustAir(dust);

						dust.spawnAsWeatherEffect();

						spawnCount++;
					}
				}
			}
		}

		if (isSnowstorm)
		{
			spawnCount = 0;
			float particleSettingsAmplifierExtra = particleSettingsAmplifier;
			spawnNeedBase = ConfigParticle.Precipitation_Particle_effect_rate * particleSettingsAmplifierExtra;
			int spawnNeed = (int) Math.max(0, spawnNeedBase * 5);
			safetyCutout = 60;
			int spawnAreaSize = 20;
			double closeDistCutoff = 7D;
			float yetAnotherRateNumber = 120 * getParticleFadeInLerpForNewWeatherState();
			Minecraft client = Minecraft.getInstance();
			Player player = client.player;

			if (farSpawn)
			{
				safetyCutout = 20;
				spawnAreaSize = 100;
				yetAnotherRateNumber = 40 * getParticleFadeInLerpForNewWeatherState();
			}

			if (particleStormIntensity >= 0.01F)
			{
				if (spawnNeed > 0)
				{
					if (getParticleFadeInLerpForNewWeatherState() > 0.5F)
					{
						particleSettingsAmplifierExtra *= (getParticleFadeInLerpForNewWeatherState() - 0.5F) * 2F;
					}
					else
					{
						particleSettingsAmplifierExtra = 0;
					}

					for (int i = 0; i < Math.max(1, safetyCutout * particleSettingsAmplifierExtra)/*curPrecipVal * 20F * PRECIPITATION_PARTICLE_EFFECT_RATE*/; i++)
					{
						BlockPos pos = CoroUtilBlock.blockPos(
								entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
								entP.getY() - 5 + rand.nextInt(20),
								entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

						Vec3 windForce = ClientTickHandler.getClientWeather().getWindManager().getWindForce(null);
						double upwindDistAdjust = -10D;
						windForce = windForce.multiply(upwindDistAdjust, upwindDistAdjust, upwindDistAdjust);
						pos = pos.offset(Mth.floor(windForce.x), Mth.floor(windForce.y), Mth.floor(windForce.z));

						if (WeatherUtilEntity.getDistanceSqEntToPos(entP, pos) < closeDistCutoff * closeDistCutoff)
							continue;

						if (canPrecipitateAt(world, pos))
						{
							ParticleTexExtraRender snow = new ParticleTexExtraRender((ClientLevel) entP.level(), pos.getX(), pos.getY(), pos.getZ(),
									0D, 0D, 0D, ParticleRegistry.snow);

							particleBehavior.initParticleSnowstorm(snow, (int)(10 * particleStormIntensity));
							snow.spawnAsWeatherEffect();
						}
					}

					double sandstormParticleRateDust = ConfigSand.Sandstorm_Particle_Dust_effect_rate;

					//float adjustAmountSmooth75 = (particleStormIntensity * 8F) - 7F;
					float adjustAmountSmooth75 = particleStormIntensity;

					//extra snow cloud dust
					for (int i = 0; i < (particleSettingsAmplifier * yetAnotherRateNumber * adjustAmountSmooth75 * sandstormParticleRateDust); i++)
					{
						BlockPos pos = CoroUtilBlock.blockPos(
								player.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
								player.getY() - 2 + rand.nextInt(10),
								player.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

						if (WeatherUtilEntity.getDistanceSqEntToPos(entP, pos) < closeDistCutoff * closeDistCutoff)
							continue;

						if (canPrecipitateAt(world, pos))
						{
							TextureAtlasSprite sprite = ParticleRegistry.cloud256;

							ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
									pos.getY(),
									pos.getZ(),
									0, 0, 0, sprite);
							particleBehavior.initParticle(part);
							particleBehavior.initParticleSnowstormCloudDust(part);
							particleBehavior.particles.add(part);
							part.spawnAsWeatherEffect();
						}
					}
				}
			}
			tickSandstormSound();
		}

		if (isSandstorm)
		{
			Minecraft client = Minecraft.getInstance();
			Player player = client.player;
			ClientTickHandler.getClientWeather();

			if (particleStormIntensity >= 0.1F)
			{
				rand = CoroUtilMisc.random();
				int spawnAreaSize = 60;

				double sandstormParticleRateDebris = ConfigSand.Sandstorm_Particle_Debris_effect_rate;
				double sandstormParticleRateDust = ConfigSand.Sandstorm_Particle_Dust_effect_rate;

				float adjustAmountSmooth75 = particleStormIntensity;

				if (farSpawn)
				{
					adjustAmountSmooth75 *= 0.3F;
				}

				adjustAmountSmooth75 *= particleSettingsAmplifier;

				adjustAmountSmooth75 *= getParticleFadeInLerpForNewWeatherState();

				for (int i = 0; i < ((float) 60 * adjustAmountSmooth75 * sandstormParticleRateDust); i++)
				{
					BlockPos pos = CoroUtilBlock.blockPos(
							player.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
							player.getY() - 2 + rand.nextInt(10),
							player.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

					if (canPrecipitateAt(world, pos))
					{
						TextureAtlasSprite sprite = ParticleRegistry.cloud256;

						ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
								pos.getY(),
								pos.getZ(),
								0, 0, 0, sprite);
						particleBehavior.initParticle(part);
						particleBehavior.initParticleSandstormDust(part);
						particleBehavior.particles.add(part);
						part.spawnAsWeatherEffect();
					}
				}

				for (int i = 0; i < ((float) 1 * adjustAmountSmooth75 * sandstormParticleRateDebris); i++)
				{
					BlockPos pos = CoroUtilBlock.blockPos(
							player.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
							player.getY() - 2 + rand.nextInt(10),
							player.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

					if (canPrecipitateAt(world, pos))
					{
						TextureAtlasSprite sprite = ParticleRegistry.tumbleweed;

						ParticleCrossSection part = new ParticleCrossSection(world, pos.getX(),
								pos.getY(),
								pos.getZ(),
								0, 0, 0, sprite);
						particleBehavior.initParticle(part);
						particleBehavior.initParticleSandstormTumbleweed(part);
						particleBehavior.particles.add(part);
						part.spawnAsWeatherEffect();
					}
				}

				for (int i = 0; i < ((float) 8 * adjustAmountSmooth75 * sandstormParticleRateDebris); i++)
				{
					BlockPos pos = CoroUtilBlock.blockPos(
							player.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
							player.getY() - 2 + rand.nextInt(10),
							player.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

					if (canPrecipitateAt(world, pos))
					{
						TextureAtlasSprite sprite = null;
						int tex = rand.nextInt(3);
						if (tex == 0)
						{
							sprite = ParticleRegistry.debris_1;
						}
						else if (tex == 1)
						{
							sprite = ParticleRegistry.debris_2;
						}
						else if (tex == 2)
						{
							sprite = ParticleRegistry.debris_3;
						}

						ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
								pos.getY(),
								pos.getZ(),
								0, 0, 0, sprite);
						particleBehavior.initParticle(part);
						particleBehavior.initParticleSandstormDebris(part);
						particleBehavior.particles.add(part);
						part.spawnAsWeatherEffect();
					}
				}
			}
			tickSandstormSound();
		}
    }

	@Shadow private int rainSoundTime;

    @Overwrite(remap = false)
	public void tickRainSound()
	{
		if (!FMLEnvironment.dist.isClient()) return;

		Minecraft minecraft = Minecraft.getInstance();
		Player player = Minecraft.getInstance().player;
		final double snowstormMaxTemp = TFCWeatherConfig.COMMON.snowstormMaxTemp.get();
		final double sandstormMaxRainfall = TFCWeatherConfig.COMMON.sandstormMaxRainfall.get();

		float precipitationStrength = ClientWeatherHelper.get().getPrecipitationStrength(player);
		float temperature = Climate.getTemperature(player.level(), player.blockPosition());
		float rainfall = Climate.getRainfall(player.level(), player.blockPosition());

		if (!(precipitationStrength <= 0.0F) && !shouldSnowHere(player.level(), player.level().getBiome(player.blockPosition()).value(), player.blockPosition()) && temperature > snowstormMaxTemp && rainfall > sandstormMaxRainfall)
		{
			Random random = new Random(player.level().getGameTime() * 312987231L);
			LevelReader levelreader = minecraft.level;
			Level level = player.level();
			BlockPos blockpos = player.blockPosition();
			BlockPos blockpos1 = null;
			int i = (int)(100.0F * precipitationStrength * precipitationStrength) / (minecraft.options.particles.get() == ParticleStatus.DECREASED ? 2 : 1);

			for(int j = 0; j < i; ++j)
			{
				int k = random.nextInt(21) - 10;
				int l = random.nextInt(21) - 10;
				BlockPos blockpos2 = levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));
				Biome biome = levelreader.getBiome(blockpos2).value();
				if (blockpos2.getY() > levelreader.getMinBuildHeight() && blockpos2.getY() <= blockpos.getY() + 10 && blockpos2.getY() >= blockpos.getY() - 10 && biome.getPrecipitationAt(blockpos2) == Biome.Precipitation.RAIN && biome.warmEnoughToRain(blockpos2))
				{
					blockpos1 = blockpos2.below();
					if (minecraft.options.particles.get() == ParticleStatus.MINIMAL)
					{
						break;
					}

					double d0 = random.nextDouble();
					double d1 = random.nextDouble();
					BlockState blockstate = levelreader.getBlockState(blockpos1);
					FluidState fluidstate = levelreader.getFluidState(blockpos1);
					VoxelShape voxelshape = blockstate.getCollisionShape(levelreader, blockpos1);
					double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
					double d3 = (double)fluidstate.getHeight(levelreader, blockpos1);
					double d4 = Math.max(d2, d3);
					final boolean isLitCampfire = blockstate.hasProperty(FirepitBlock.LIT) && blockstate.getValue(FirepitBlock.LIT);
					ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA) && !blockstate.is(TFCWeatherTags.Blocks.MAGMA_BLOCKS) && !isLitCampfire ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
					minecraft.level.addParticle(particleoptions, (double)blockpos1.getX() + d0, (double)blockpos1.getY() + d4, (double)blockpos1.getZ() + d1, 0.0D, 0.0D, 0.0D);
				}
			}

			if (blockpos1 != null && random.nextInt(3) < this.rainSoundTime++)
			{
				this.rainSoundTime = 0;
				if (blockpos1.getY() > blockpos.getY() + 1 && levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY()))
				{
					minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F + (0.4F * precipitationStrength), 0.5F, false);
				}
				else
				{
					minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F + (0.6F * precipitationStrength), 1.0F, false);
				}
			}
		}
	}

    @Overwrite(remap = false)
	public static boolean canPrecipitateAt(Level world, BlockPos strikePosition)
	{
		return world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, strikePosition).getY() <= strikePosition.getY();
	}

    @Shadow
	private static Block getBlock(Level parWorld, BlockPos pos)
	{
		return getBlock(parWorld, pos.getX(), pos.getY(), pos.getZ());
	}

    @Shadow
    private static Block getBlock(Level parWorld, int x, int y, int z)
    {
        return null;
    }

    @Shadow
	public static void tickSandstormSound() {}

    @Shadow
	public static WeatherEventType getWeatherState()
	{
		return null;
	}

    @Shadow
	public static float getParticleFadeInLerpForNewWeatherState()
	{
		return 0;
	}

    @Overwrite(remap = false)
	public static boolean shouldRainHere(Level level, Biome biome, BlockPos pos)
	{
		return CoroUtilCompatibility.warmEnoughToRain(biome, pos, level) && Climate.getRainfall(level, pos) > TFCWeatherConfig.COMMON.snowstormMaxTemp.get();
	}

    @Overwrite(remap = false)
	public static boolean shouldSnowHere(Level level, Biome biome, BlockPos pos)
	{
		return CoroUtilCompatibility.coldEnoughToSnow(biome, pos, level) && Climate.getTemperature(level, pos) < TFCWeatherConfig.COMMON.sandstormMaxRainfall.get();
	}

    @Shadow
	public static FogAdjuster getFogAdjuster()
	{
		return fogAdjuster;
	}
}
