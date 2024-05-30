package tfcweather.common.blocks;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;

import tfcweather.common.blockentities.TFCAnemometerBlockEntity;
import tfcweather.common.blockentities.TFCWeatherBlockEntities;

public class TFCAnemometerBlock extends BaseEntityBlock implements IForgeBlockExtension, EntityBlockExtension
{
	public static final VoxelShape SHAPE = box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    public static final void register() {}
    public final ExtendedProperties properties;
    public final ResourceLocation textureLocation;

	public TFCAnemometerBlock(ExtendedProperties properties, ResourceLocation textureLocation)
    {
        super(properties.properties());
        this.properties = properties;
        this.textureLocation = textureLocation;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
    {
		return SHAPE;
	}

	@Override
	public RenderShape getRenderShape(BlockState p_49232_)
    {
		return RenderShape.INVISIBLE;
	}

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TFCAnemometerBlockEntity(pos, state);
    }

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState p_153213_, BlockEntityType<T> entityType)
    {
		return createTickerHelper(entityType, TFCWeatherBlockEntities.ANEMOMETER.get(), TFCAnemometerBlockEntity::tick);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(final BlockEntityType<A> type, final BlockEntityType<E> tickerType, final BlockEntityTicker<? super E> ticker)
    {
		return tickerType == type ? (BlockEntityTicker<A>) ticker : null;
	}

    @Override
    public ExtendedProperties getExtendedProperties()
    {
        return properties;
    }

    public ResourceLocation getTextureLocation()
    {
        return textureLocation;
    }
}
