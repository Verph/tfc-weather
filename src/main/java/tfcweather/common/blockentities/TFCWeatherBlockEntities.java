package tfcweather.common.blockentities;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.LogPileBlockEntity;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.dries007.tfc.common.blocks.wood.Wood;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.registry.RegistrationHelpers;

import tfcweather.common.blocks.TFCWeatherBlocks;
import tfcweather.common.items.TFCWeatherItems;

import static tfcweather.TFCWeather.MOD_ID;

@SuppressWarnings("unused")
public class TFCWeatherBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final RegistryObject<BlockEntityType<SandPileBlockEntity>> SAND_PILE = register("sand_pile", SandPileBlockEntity::new, Stream.of(
            TFCWeatherBlocks.SAND_LAYERS.values()
        ).<Supplier<? extends Block>>flatMap(Helpers::flatten)
    );
    public static final RegistryObject<BlockEntityType<TFCAnemometerBlockEntity>> ANEMOMETER = register("anemometer", TFCAnemometerBlockEntity::new, Stream.of(TFCWeatherBlocks.ANEMOMETER).flatMap(metalMap -> metalMap.values().stream()));
    public static final RegistryObject<BlockEntityType<TFCWindVaneBlockEntity>> WIND_VANE = register("wind_vane", TFCWindVaneBlockEntity::new, Stream.of(TFCWeatherBlocks.WIND_VANE).flatMap(metalMap -> metalMap.values().stream()));

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block)
    {
        return RegistrationHelpers.register(BLOCK_ENTITIES, name, factory, block);
    }

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Stream<? extends Supplier<? extends Block>> blocks)
    {
        return RegistrationHelpers.register(BLOCK_ENTITIES, name, factory, blocks);
    }
}
