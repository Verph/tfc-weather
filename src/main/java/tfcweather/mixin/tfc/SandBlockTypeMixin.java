package tfcweather.mixin.tfc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.dries007.tfc.common.blocks.soil.SandBlockType;

import tfcweather.common.blocks.NewSandBlock;
import tfcweather.interfaces.RegistrySand;

@Mixin(SandBlockType.class)
public abstract class SandBlockTypeMixin implements RegistrySand
{
    @Shadow private int dustColor;
    @Shadow private MapColor mapColor;

    @Override
    public int getDustColor()
    {
        return dustColor;
    }

    @Override
    public MapColor getMaterialColor()
    {
        return mapColor;
    }

    @Overwrite(remap = false)
    public Block create()
    {
        return new NewSandBlock(this.getDustColor(), BlockBehaviour.Properties.copy(Blocks.SAND).mapColor(this.getMaterialColor()).strength(0.5F).sound(SoundType.SAND), this);
    }
}
