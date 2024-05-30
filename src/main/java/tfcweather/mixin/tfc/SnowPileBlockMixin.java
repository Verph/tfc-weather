package tfcweather.mixin.tfc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.SnowPileBlock;

@Mixin(SnowPileBlock.class)
public abstract class SnowPileBlockMixin
{
    @Inject(method = "removePileOrSnow", at = @At("TAIL"), remap = false)
    private static void inject$removePileOrSnow(LevelAccessor level, BlockPos pos, BlockState state, CallbackInfo ci)
    {
        if (state.getValue(SnowPileBlock.LAYERS) <= 1)
        {
            level.getBlockEntity(pos, TFCBlockEntities.PILE.get()).ifPresent(pile -> {
                if (!level.isClientSide())
                {
                    if (pile.getInternalState().getBlock() instanceof SnowPileBlock)
                    {
                        level.destroyBlock(pos, false);
                    }
                }
            });
        }
    }
}
