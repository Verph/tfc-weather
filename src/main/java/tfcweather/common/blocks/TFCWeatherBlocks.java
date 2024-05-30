package tfcweather.common.blocks;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GravelBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.registry.RegistrationHelpers;

import tfcweather.common.blockentities.TFCWeatherBlockEntities;
import tfcweather.common.items.TFCWeatherItems;
import tfcweather.interfaces.RegistrySand;
import tfcweather.util.TFCWeatherHelpers;

import static tfcweather.TFCWeather.MOD_ID;

@SuppressWarnings("unused")
public class TFCWeatherBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);

    // Sand

    public static final Map<SandBlockType, RegistryObject<Block>> SAND_LAYERS = Helpers.mapOfKeys(SandBlockType.class, type ->
        register(("sand/sand_layer/" + type.name()), () -> new SandLayerBlock(type.getDustColor(), ExtendedProperties.of(Blocks.SAND).mapColor(type.getMaterialColor()).strength(0.1F).sound(SoundType.SAND).randomTicks().blockEntity(TFCWeatherBlockEntities.SAND_PILE), TFCBlocks.SAND.get(type), ((RegistrySand)(Object)type)))
    );

    // Metals

    public static final Map<Metal.Default, RegistryObject<Block>> ANEMOMETER = Helpers.mapOfKeys(Metal.Default.class, Metal.Default::hasParts, metal ->
        register("metal/anemometer/" + metal.getSerializedName(), () -> new TFCAnemometerBlock(ExtendedProperties.of().mapColor(metal.mapColor()).requiresCorrectToolForDrops().strength(5F, 6F).sound(SoundType.CHAIN).instrument(NoteBlockInstrument.IRON_XYLOPHONE), TFCWeatherHelpers.identifier("textures/block/metal/anemometer/" + metal.getSerializedName() + ".png")))
    );

    public static final Map<Metal.Default, RegistryObject<Block>> WIND_VANE = Helpers.mapOfKeys(Metal.Default.class, Metal.Default::hasParts, metal ->
        register("metal/wind_vane/" + metal.getSerializedName(), () -> new TFCWindVaneBlock(ExtendedProperties.of().mapColor(metal.mapColor()).requiresCorrectToolForDrops().strength(5F, 6F).sound(SoundType.CHAIN).instrument(NoteBlockInstrument.IRON_XYLOPHONE), TFCWeatherHelpers.identifier("textures/block/metal/wind_vane/" + metal.getSerializedName() + ".png")))
    );

    private static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> blockSupplier)
    {
        return register(name, blockSupplier, (Function<T, ? extends BlockItem>) null);
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier)
    {
        return register(name, blockSupplier, block -> new BlockItem(block, new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier, Item.Properties blockItemProperties)
    {
        return register(name, blockSupplier, block -> new BlockItem(block, blockItemProperties));
    }

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> blockSupplier, @Nullable Function<T, ? extends BlockItem> blockItemFactory)
    {
        return RegistrationHelpers.registerBlock(TFCWeatherBlocks.BLOCKS, TFCWeatherItems.ITEMS, name, blockSupplier, blockItemFactory);
    }
}
