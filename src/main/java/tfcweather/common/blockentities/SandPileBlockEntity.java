package tfcweather.common.blockentities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.common.blocks.GroundcoverBlockType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.crop.WildCropBlock;
import net.dries007.tfc.util.Helpers;

public class SandPileBlockEntity extends TFCBlockEntity
{
    public BlockState internalState;
    @Nullable public BlockState aboveState;

    public SandPileBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCWeatherBlockEntities.SAND_PILE.get(), pos, state);

        internalState = Blocks.AIR.defaultBlockState();
        aboveState = null;
    }

    @Override
    public BlockEntityType<?> getType()
    {
        return TFCWeatherBlockEntities.SAND_PILE.get();
    }

    public void setHiddenStates(BlockState internalState, @Nullable BlockState aboveState, boolean byPlayer)
    {
        if (Helpers.isBlock(internalState, TFCTags.Blocks.CONVERTS_TO_HUMUS) && !byPlayer)
        {
            this.internalState = TFCBlocks.GROUNDCOVER.get(GroundcoverBlockType.HUMUS).get().defaultBlockState();
        }
        else if (internalState.getBlock() instanceof WildCropBlock && internalState.hasProperty(WildCropBlock.MATURE))
        {
            this.internalState = internalState.setValue(WildCropBlock.MATURE, false);
        }
        else
        {
            this.internalState = internalState;
        }
        this.aboveState = aboveState;
    }

    public BlockState getInternalState()
    {
        return internalState;
    }

    @Nullable
    public BlockState getAboveState()
    {
        return aboveState;
    }

    @Override
    protected void loadAdditional(CompoundTag tag)
    {
        HolderGetter<Block> getter = getBlockGetter();
        internalState = NbtUtils.readBlockState(getter, tag.getCompound("internalState"));
        aboveState = tag.contains("aboveState", Tag.TAG_COMPOUND) ? NbtUtils.readBlockState(getter, tag.getCompound("aboveState")) : null;
        super.loadAdditional(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        tag.put("internalState", NbtUtils.writeBlockState(internalState));
        if (aboveState != null)
        {
            tag.put("aboveState", NbtUtils.writeBlockState(aboveState));
        }
        super.saveAdditional(tag);
    }
}
