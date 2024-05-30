package tfcweather.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import net.dries007.tfc.util.Helpers;

import tfcweather.util.TFCWeatherHelpers;

public class TFCWeatherTags
{
    public static class Blocks
    {
        public static final TagKey<Block> MAGMA_BLOCKS = create("magma_blocks");
        public static final TagKey<Block> SAND_LAYER_BLOCKS = create("sand_layer_blocks");

        private static TagKey<Block> create(String id)
        {
            return TagKey.create(Registries.BLOCK, TFCWeatherHelpers.identifier(id));
        }
    }

    public static class Biomes
    {
        public static final TagKey<Biome> IS_OCEANIC = create("is_oceanic");
        public static final TagKey<Biome> IS_SHORE = create("is_shore");

        private static TagKey<Biome> create(String id)
        {
            return TagKey.create(Registries.BIOME, TFCWeatherHelpers.identifier(id));
        }
    }

    public static class TFCBiomes
    {
        public static final TagKey<Biome> IS_RIVER = create("is_river");
        public static final TagKey<Biome> IS_LAKE = create("is_lake");
        public static final TagKey<Biome> IS_VOLCANIC = create("is_volcanic");

        private static TagKey<Biome> create(String id)
        {
            return TagKey.create(Registries.BIOME, Helpers.identifier(id));
        }
    }
}
