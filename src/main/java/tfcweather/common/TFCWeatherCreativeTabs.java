package tfcweather.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.dries007.tfc.common.TFCCreativeTabs;
import net.dries007.tfc.common.blocks.DecorationBlockRegistryObject;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.SelfTests;

import tfcweather.TFCWeather;
import tfcweather.common.blocks.TFCWeatherBlocks;
import tfcweather.util.TFCWeatherHelpers;

@SuppressWarnings("unused")
public class TFCWeatherCreativeTabs
{
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent out)
    {
        if (out.getTab() == TFCCreativeTabs.EARTH.tab().get())
        {
            for (SandBlockType type : SandBlockType.values())
            {
                accept(out, TFCWeatherBlocks.SAND_LAYERS.get(type));
            }
        }
        if (out.getTab() == TFCCreativeTabs.METAL.tab().get())
        {
            for (Metal.Default type : Metal.Default.values())
            {
                if (type.hasParts())
                {
                    accept(out, TFCWeatherBlocks.ANEMOMETER.get(type));
                    accept(out, TFCWeatherBlocks.WIND_VANE.get(type));
                }
            }
        }
    }

    public static <T extends ItemLike, R extends Supplier<T>> void accept(CreativeModeTab.Output out, R reg)
    {
        if (reg.get().asItem() == Items.AIR)
        {
            SelfTests.reportExternalError();
            return;
        }
        out.accept(reg.get());
    }
}
