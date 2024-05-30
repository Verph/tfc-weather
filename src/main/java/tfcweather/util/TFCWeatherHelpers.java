package tfcweather.util;

import java.util.Arrays;
import java.util.Random;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.SnowPileBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.plant.KrummholzBlock;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.noise.OpenSimplex2D;

import tfcweather.TFCWeather;
import tfcweather.common.blocks.SandLayerBlock;
import tfcweather.common.blocks.TFCWeatherBlocks;
import tfcweather.config.TFCWeatherConfig;
import tfcweather.interfaces.ISandColor;
import tfcweather.interfaces.RegistrySand;

public class TFCWeatherHelpers
{
    public static final long[] UNLOADED = new long[] {0};
    public static final Random RANDOM = new Random();
    public static Direction[] NOT_DOWN = new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP};
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction[] DIRECTIONS_HORIZONTAL_FIRST = new Direction[] {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
    public static final Direction[] DIRECTIONS_HORIZONTAL = Arrays.stream(DIRECTIONS).filter(d -> d != Direction.DOWN && d != Direction.UP).toArray(Direction[]::new);
    public static final Direction[] DIRECTIONS_VERTICAL = Arrays.stream(DIRECTIONS).filter(d -> d == Direction.DOWN || d == Direction.UP).toArray(Direction[]::new);

    public static ResourceLocation identifier(String name)
    {
        return new ResourceLocation(TFCWeather.MOD_ID, name);
    }

    public static ModelLayerLocation modelIdentifier(String name, String part)
    {
        return new ModelLayerLocation(identifier(name), part);
    }

    public static ModelLayerLocation modelIdentifier(String name)
    {
        return modelIdentifier(name, "main");
    }

    public static ModelPart bakeSimple(Context ctx, String layerName)
    {
        return ctx.bakeLayer(modelIdentifier(layerName));
    }

    public static boolean isBiome(Biome biome, TagKey<Biome> tag)
    {
        return Helpers.checkTag(ForgeRegistries.BIOMES, biome, tag);
    }

    public static double getWorldTPS(Level level, ResourceKey<Level> dimension)
    {
        long[] times = level.getServer().getTickTime(dimension);

        if (times == null)
            times = UNLOADED;

        double worldTickTime = mean(times) * 1.0E-6D;

        return Math.min(1000.0 / worldTickTime, 20);
    }

    public static double getTPS(Level level)
    {
        long[] times = level.getServer().getTickTime(level.dimension());

        if (times == null)
            times = UNLOADED;

        double worldTickTime = mean(times) * 1.0E-6D;

        return Math.min(1000.0 / worldTickTime, 20);
    }

    public static long mean(long[] values)
    {
        long sum = 0L;
        for (long v : values)
            sum += v;
        return sum / values.length;
    }

    public static Block getSandLayerBlock(Level level, BlockPos pos)
    {
        if (pos != null && level != null)
        {
            return TFCWeatherBlocks.SAND_LAYERS.get(getSandColor(level, pos)).get();
        }
        return TFCWeatherBlocks.SAND_LAYERS.get(SandBlockType.YELLOW).get();
    }

    public static Block getSandBlock(Level level, BlockPos pos)
    {
        if (pos != null && level != null)
        {
            return TFCBlocks.SAND.get(getSandColor(level, pos)).get();
        }
        return TFCBlocks.SAND.get(SandBlockType.YELLOW).get();
    }

	public static RegistrySand getSandColor(Level level, BlockPos pos)
	{
        if (level.getBlockState(pos.below()).getBlock() instanceof ISandColor sand)
        {
            return sand.getSandColor();
        }
        else if (level instanceof ServerLevel server && server.getChunkSource().getGenerator() instanceof TFCChunkGenerator chunkGen)
        {
            return getRare(server, chunkGen.chunkDataProvider().get(server, pos), pos.below());
        }
		return ((RegistrySand)(Object)SandBlockType.YELLOW);
	}

	public static RegistrySand getRare(ServerLevel level, ChunkData data, BlockPos pos)
    {
        BiomeExtension biome = TFCBiomes.getExtension(level, level.getBiome(pos).value());
        int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ());
        if (biome == TFCBiomes.SHORE || biome == TFCBiomes.TIDAL_FLATS)
        {
            if (new OpenSimplex2D(level.getSeed()).octaves(2).spread(0.003f).abs().noise(pos.getX(), pos.getZ()) > 0.6f)
            {
                if (data.getRainfall(pos) > 300f && data.getAverageTemp(pos) > 15f)
                {
                    return ((RegistrySand)(Object)SandBlockType.PINK);
                }
                else if (data.getRainfall(pos) > 300f)
                {
                    return ((RegistrySand)(Object)SandBlockType.BLACK);
                }
            }
            height = -64;
        }
        return data.getRockData().getRock(pos.getX(), height, pos.getZ()).sand() instanceof ISandColor sand ? sand.getSandColor() : ((RegistrySand)(Object)SandBlockType.YELLOW);
    }

    public static Block getSandLayerBlockWG(WorldGenLevel level, BlockPos pos)
    {
        if (pos != null && level != null)
        {
            return TFCWeatherBlocks.SAND_LAYERS.get(getSandColorWG(level, pos)).get();
        }
        return TFCWeatherBlocks.SAND_LAYERS.get(SandBlockType.YELLOW).get();
    }

    public static Block getSandBlockWG(WorldGenLevel level, BlockPos pos)
    {
        if (pos != null && level != null)
        {
            return TFCBlocks.SAND.get(getSandColorWG(level, pos)).get();
        }
        return TFCBlocks.SAND.get(SandBlockType.YELLOW).get();
    }

	public static RegistrySand getSandColorWG(WorldGenLevel level, BlockPos pos)
	{
        if (level.getBlockState(pos.below()).getBlock() instanceof ISandColor sand)
        {
            return sand.getSandColor();
        }
        else if (level instanceof ServerLevel server && server.getChunkSource().getGenerator() instanceof TFCChunkGenerator chunkGen)
        {
            return getRareWG(server, chunkGen.chunkDataProvider().get(server, pos), pos.below());
        }
		return ((RegistrySand)(Object)SandBlockType.YELLOW);
	}

	public static RegistrySand getRareWG(WorldGenLevel level, ChunkData data, BlockPos pos)
    {
        BiomeExtension biome = TFCBiomes.getExtension(level, level.getBiome(pos).value());
        int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ());
        if (biome == TFCBiomes.SHORE || biome == TFCBiomes.TIDAL_FLATS)
        {
            if (new OpenSimplex2D(level.getSeed()).octaves(2).spread(0.003f).abs().noise(pos.getX(), pos.getZ()) > 0.6f)
            {
                if (data.getRainfall(pos) > 300f && data.getAverageTemp(pos) > 15f)
                {
                    return ((RegistrySand)(Object)SandBlockType.PINK);
                }
                else if (data.getRainfall(pos) > 300f)
                {
                    return ((RegistrySand)(Object)SandBlockType.BLACK);
                }
            }
            height = -64;
        }
        return data.getRockData().getRock(pos.getX(), height, pos.getZ()).sand() instanceof ISandColor sand ? sand.getSandColor() : ((RegistrySand)(Object)SandBlockType.YELLOW);
    }

    public static void placeSnowPile(LevelAccessor level, BlockPos pos, BlockState state, int height)
    {
        // Create a snow pile block, accounting for double piles.
        final BlockPos posAbove = pos.above();
        final BlockState aboveState = level.getBlockState(posAbove);
        final BlockState savedAboveState = Helpers.isBlock(aboveState.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED) ? aboveState : null;
        final BlockState snowPile = TFCBlocks.SNOW_PILE.get().defaultBlockState().setValue(SandLayerBlock.LAYERS, height);

        level.setBlock(pos, snowPile, Block.UPDATE_ALL_IMMEDIATE);
        level.getBlockEntity(pos, TFCBlockEntities.PILE.get()).ifPresent(entity -> entity.setHiddenStates(state, savedAboveState, false));

        if (savedAboveState != null)
        {
            Helpers.removeBlock(level, posAbove, Block.UPDATE_ALL_IMMEDIATE);
        }

        // Then cause block updates
        level.blockUpdated(pos, TFCBlocks.SNOW_PILE.get());
        if (savedAboveState != null)
        {
            level.blockUpdated(posAbove, Blocks.AIR);
        }

        // And update grass with the snowy property
        final BlockPos posBelow = pos.below();
        level.setBlock(posBelow, Helpers.setProperty(level.getBlockState(posBelow), SnowyDirtBlock.SNOWY, true), 2);
    }

    public static void doSnow(Level level, BlockPos surfacePos, float temperature)
    {
        final RandomSource random = level.random;
        final int expectedLayers = (int) EnvironmentHelpers.getExpectedSnowLayerHeight(temperature);
        if (!placeSnowOrSnowPile(level, surfacePos, random, expectedLayers))
        {
            if (!placeSnowOrSnowPile(level, surfacePos.below(), random, expectedLayers))
            {
                placeSnowOrSnowPile(level, surfacePos.below(2), random, expectedLayers);
            }
        }
    }

    public static boolean placeSnowOrSnowPile(Level level, BlockPos initialPos, RandomSource random, int expectedLayers)
    {
        if (expectedLayers < 1)
        {
            return false;
        }

        final BlockPos pos = findOptimalSnowLocation(level, initialPos, level.getBlockState(initialPos), random);
        final BlockState state = level.getBlockState(pos);

        if (initialPos.equals(pos))
        {
            return false;
        }
        return placeSnowOrSnowPileAt(level, pos, state, random, expectedLayers);
    }

    public static BlockPos findOptimalSnowLocation(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random)
    {
        BlockPos targetPos = null;
        int found = 0;
        if (EnvironmentHelpers.isSnow(state))
        {
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final BlockPos adjPos = pos.relative(direction);
                final BlockState adjState = level.getBlockState(adjPos);
                if ((EnvironmentHelpers.isSnow(adjState) && adjState.getValue(SnowLayerBlock.LAYERS) < state.getValue(SnowLayerBlock.LAYERS)) // Adjacent snow that's lower than this one
                    || ((adjState.isAir() || Helpers.isBlock(adjState.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED)) && Blocks.SNOW.defaultBlockState().canSurvive(level, adjPos))) // Or, empty space that could support snow
                {
                    found++;
                    if (targetPos == null || random.nextInt(found) == 0)
                    {
                        targetPos = adjPos;
                    }
                }
            }
            if (targetPos != null)
            {
                return targetPos;
            }
        }
        return pos;
    }

    public static boolean placeSnowOrSnowPileAt(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random, int expectedLayers)
    {
        // Then, handle possibilities
        if (EnvironmentHelpers.isSnow(state) && state.getValue(SnowLayerBlock.LAYERS) < 7)
        {
            // Snow and snow layers can accumulate snow
            // The chance that this works is reduced the higher the pile is
            final int currentLayers = state.getValue(SnowLayerBlock.LAYERS);
            final BlockState newState = state.setValue(SnowLayerBlock.LAYERS, currentLayers + 1);
            if (newState.canSurvive(level, pos) && random.nextInt(1 + 3 * currentLayers) == 0 && expectedLayers > currentLayers)
            {
                level.setBlock(pos, newState, Block.UPDATE_ALL_IMMEDIATE);
            }
            return true;
        }
        else if (SnowPileBlock.canPlaceSnowPile(level, pos, state))
        {
            SnowPileBlock.placeSnowPile(level, pos, state, false);
            return true;
        }
        else if (state.getBlock() instanceof KrummholzBlock)
        {
            KrummholzBlock.updateFreezingInColumn(level, pos, true);
        }
        else if (state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos))
        {
            // Vanilla snow placement (single layers)
            level.setBlock(pos, Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            return true;
        }
        else if (level instanceof Level fullLevel)
        {
            // Fills cauldrons with snow
            state.getBlock().handlePrecipitation(state, fullLevel, pos, Biome.Precipitation.SNOW);
        }
        return false;
    }

    public static boolean isSand(BlockState state)
    {
        return state.getBlock() instanceof SandLayerBlock;
    }

    public static float getExpectedSandLayerHeight(float rainfall)
    {
        return Mth.clampedMap(rainfall, TFCWeatherConfig.COMMON.sandstormMaxRainfall.get().floatValue(), TFCWeatherConfig.COMMON.sandstormMinRainfall.get().floatValue(), 0f, 7f);
    }

    public static void doSand(Level level, BlockPos surfacePos, float rainfall, RegistrySand sand)
    {
        final RandomSource random = level.random;
        final int expectedLayers = (int) getExpectedSandLayerHeight(rainfall);
        if (!placeSandPile(level, surfacePos, random, expectedLayers, sand))
        {
            if (!placeSandPile(level, surfacePos.below(), random, expectedLayers, sand))
            {
                placeSandPile(level, surfacePos.below(2), random, expectedLayers, sand);
            }
        }
    }

    public static boolean placeSandPile(Level level, BlockPos initialPos, RandomSource random, int expectedLayers, RegistrySand sand)
    {
        if (expectedLayers < 1)
        {
            return false;
        }

        final BlockPos pos = findOptimalSandLocation(level, initialPos, level.getBlockState(initialPos), random, sand);
        final BlockState state = level.getBlockState(pos);

        if (initialPos.equals(pos))
        {
            return false;
        }
        return placeSandPileAt(level, pos, state, random, expectedLayers, sand);
    }

    public static BlockPos findOptimalSandLocation(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random, RegistrySand sand)
    {
        BlockPos targetPos = null;
        int found = 0;
        if (isSand(state))
        {
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final BlockPos adjPos = pos.relative(direction);
                final BlockState adjState = level.getBlockState(adjPos);
                if ((isSand(adjState) && adjState.getValue(SandLayerBlock.LAYERS) < state.getValue(SandLayerBlock.LAYERS))
                    || ((adjState.isAir() || Helpers.isBlock(adjState.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED)) && TFCWeatherBlocks.SAND_LAYERS.get(sand).get().defaultBlockState().canSurvive(level, adjPos)))
                {
                    found++;
                    if (targetPos == null || random.nextInt(found) == 0)
                    {
                        targetPos = adjPos;
                    }
                }
            }
            if (targetPos != null)
            {
                return targetPos;
            }
        }
        return pos;
    }

    public static boolean placeSandPileAt(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random, int expectedLayers, RegistrySand sand)
    {
        if (isSand(state) && state.getValue(SandLayerBlock.LAYERS) < 7)
        {
            final int currentLayers = state.getValue(SandLayerBlock.LAYERS);
            final BlockState newState = state.setValue(SandLayerBlock.LAYERS, currentLayers + 1);
            if (newState.canSurvive(level, pos) && random.nextInt(1 + 3 * currentLayers) == 0 && expectedLayers > currentLayers)
            {
                level.setBlock(pos, newState, Block.UPDATE_ALL_IMMEDIATE);
            }
            return true;
        }
        else if (SandLayerBlock.canPlaceSandPile(level, pos, state))
        {
            SandLayerBlock.placeSandPile(level, pos, state, sand, 1);
            return true;
        }
        return false;
    }

    public static boolean placeSand(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random, int expectedLayers, RegistrySand sand)
    {
        if (isSand(state) && state.getValue(SandLayerBlock.LAYERS) < 7)
        {
            final int currentLayers = state.getValue(SandLayerBlock.LAYERS);
            final BlockState newState = state.setValue(SandLayerBlock.LAYERS, currentLayers + 1);
            if (newState.canSurvive(level, pos) && expectedLayers > currentLayers)
            {
                level.setBlock(pos, newState, Block.UPDATE_ALL_IMMEDIATE);
            }
            return true;
        }
        else if (SandLayerBlock.canPlaceSandPile(level, pos, state))
        {
            SandLayerBlock.placeSandPile(level, pos, state, sand, 1);
            return true;
        }
        return false;
    }
}
