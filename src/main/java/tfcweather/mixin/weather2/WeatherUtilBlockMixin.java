package tfcweather.mixin.weather2;

import org.spongepowered.asm.mixin.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;

import com.corosus.coroutil.util.CoroUtilBlock;
import weather2.util.WeatherUtilBlock;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.PileBlockEntity;
import net.dries007.tfc.common.blocks.SnowPileBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.climate.Climate;
import tfcweather.common.TFCWeatherTags;
import tfcweather.common.blocks.SandLayerBlock;
import tfcweather.util.TFCWeatherHelpers;

@Mixin(WeatherUtilBlock.class)
public class WeatherUtilBlockMixin
{
	@Shadow public static int layerableHeightPropMax = 8;

    @Overwrite(remap = false)
	public static void fillAgainstWallSmoothly(Level world, Vec3 posSource, float directionYaw, float scanDistance, float fillRadius, Block blockLayerable, int heightDiff, int maxBlockStackingAllowed)
	{
		BlockPos posSourcei = CoroUtilBlock.blockPos(posSource);
		int y = posSourcei.getY();
		float tickStep = 0.75F;

		Vec3 posLastNonWall = new Vec3(posSource.x, posSource.y, posSource.z);
		Vec3 posWall = null;

		BlockPos lastScannedPosXZ = null;

		int previousBlockHeight = 0;
		for (float i = 0; i < scanDistance; i += tickStep)
		{
			double vecX = (-Math.sin(Math.toRadians(directionYaw)) * (i));
			double vecZ = (Math.cos(Math.toRadians(directionYaw)) * (i));

			int x = Mth.floor(posSource.x + vecX);
			int z = Mth.floor(posSource.z + vecZ);

			BlockPos pos = new BlockPos(x, y, z);
			BlockPos posXZ = new BlockPos(x, 0, z);
			BlockState state = world.getBlockState(pos);

			if (lastScannedPosXZ == null || !posXZ.equals(lastScannedPosXZ))
			{
				lastScannedPosXZ = new BlockPos(posXZ);

				AABB aabbCompare = new AABB(pos);
				VoxelShape voxelshape = Shapes.create(aabbCompare);
				boolean collided = Shapes.joinIsNotEmpty(state.getCollisionShape(world, pos).move(pos.getX(), pos.getY(), pos.getZ()), voxelshape, BooleanOp.AND);

				if (!state.isAir() && collided)
				{
					BlockPos posUp = new BlockPos(x, y + 1, z);
					BlockState stateUp = world.getBlockState(posUp);
					if (stateUp.isAir())
					{
						int height = getHeightForAnyBlock(world, posUp, state);
						if (height - previousBlockHeight <= heightDiff)
						{
							if (height == 8)
							{
								previousBlockHeight = 0;
								y++;
							}
							else
							{
								previousBlockHeight = height;
							}

							posLastNonWall = new Vec3(posSource.x + vecX, y, posSource.z + vecZ);
							continue;
						}
						else
						{
							posWall = new Vec3(posSource.x + vecX, y, posSource.z + vecZ);
							break;
						}
					}
					else
					{
						posWall = new Vec3(posSource.x + vecX, y, posSource.z + vecZ);
						break;
					}
				}
				else
				{
					posLastNonWall = new Vec3(posSource.x + vecX, y, posSource.z + vecZ);
				}
			}
			else
			{
				continue;
			}
		}

		if (posWall != null)
		{
			int amountWeHave = 1;
			int amountToAddPerXZ = 1;

			BlockState state = world.getBlockState(CoroUtilBlock.blockPos(posWall));
			BlockState state1 = world.getBlockState(CoroUtilBlock.blockPos(posLastNonWall).offset(1, 0, 0));
			BlockState state22 = world.getBlockState(CoroUtilBlock.blockPos(posLastNonWall).offset(-1, 0, 0));
			BlockState state3 = world.getBlockState(CoroUtilBlock.blockPos(posLastNonWall).offset(0, 0, 1));
			BlockState state4 = world.getBlockState(CoroUtilBlock.blockPos(posLastNonWall).offset(0, 0, -1));

			if (!(state.is(TFCTags.Blocks.CAN_BE_SNOW_PILED) || state1.is(TFCTags.Blocks.CAN_BE_SNOW_PILED) || state22.is(TFCTags.Blocks.CAN_BE_SNOW_PILED) || state3.is(TFCTags.Blocks.CAN_BE_SNOW_PILED) || state4.is(TFCTags.Blocks.CAN_BE_SNOW_PILED)))
			{
				return;
			}

			BlockPos pos2 = CoroUtilBlock.blockPos(posLastNonWall.x, posLastNonWall.y, posLastNonWall.z);
			BlockState state2 = world.getBlockState(pos2);
			if (state2.getBlock().defaultMapColor() == MapColor.WATER || state2.getBlock().defaultMapColor() == MapColor.FIRE)
			{
				return;
			}
			amountWeHave = trySpreadOnPos2(world, CoroUtilBlock.blockPos(posLastNonWall.x, posLastNonWall.y, posLastNonWall.z), amountWeHave, amountToAddPerXZ, 10, blockLayerable, maxBlockStackingAllowed);
		}
	}

    @Overwrite(remap = false)
	public static int trySpreadOnPos2(Level world, BlockPos posSpreadTo, int amount, int amountAllowedToAdd, int maxDropAllowed, Block blockLayerable, int maxBlockStackingAllowed)
	{
		if (amount <= 0) return amount;

		if (!world.getBlockState(posSpreadTo.offset(0, 1, 0)).isAir())
		{
			return amount;
		}

		BlockPos posCheckNonAir = new BlockPos(posSpreadTo);
		BlockState stateCheckNonAir = world.getBlockState(posCheckNonAir);

		int depth = 0;

		while (stateCheckNonAir.isAir())
		{
			posCheckNonAir = posCheckNonAir.offset(0, -1, 0);
			stateCheckNonAir = world.getBlockState(posCheckNonAir);
			depth++;
			if (depth > maxDropAllowed)
			{
				return amount;
			}
		}

		BlockPos posCheckPlaceable = new BlockPos(posCheckNonAir);
		BlockState stateCheckPlaceable = world.getBlockState(posCheckPlaceable);

		if (maxBlockStackingAllowed > 0)
		{
			boolean sandMode = false;
			if (blockLayerable.equals(TFCBlocks.SNOW_PILE.get()) || blockLayerable.equals(Blocks.SNOW) || blockLayerable instanceof SnowLayerBlock)
			{
				sandMode = false;
			}
			else if (blockLayerable.builtInRegistryHolder().is(TFCWeatherTags.Blocks.SAND_LAYER_BLOCKS) || blockLayerable instanceof SandLayerBlock)
			{
				sandMode = true;
			}
			int foundBlocks = 0;
			BlockPos posCheckDownForStacks = new BlockPos(posCheckPlaceable);
			BlockState stateCheckDownForStacks = world.getBlockState(posCheckPlaceable);
			if ((!sandMode && (stateCheckPlaceable.getBlock() == Blocks.SNOW_BLOCK || stateCheckDownForStacks.is(BlockTags.SNOW))) || (sandMode && stateCheckPlaceable.is(Tags.Blocks.SAND)))
			{
				while ((!sandMode && (stateCheckDownForStacks.getBlock() == Blocks.SNOW_BLOCK || stateCheckDownForStacks.is(BlockTags.SNOW))) || (sandMode && stateCheckDownForStacks.is(Tags.Blocks.SAND)))
				{
					foundBlocks++;
					if (foundBlocks >= maxBlockStackingAllowed)
					{
						return amount;
					}
					posCheckDownForStacks = posCheckDownForStacks.offset(0, -1, 0);
					stateCheckDownForStacks = world.getBlockState(posCheckDownForStacks);
				}
			}
		}

		int distForPlaceableBlocks = 0;

		while (true && distForPlaceableBlocks < 10)
		{
			AABB aabbCompare = new AABB(posCheckPlaceable);
			VoxelShape voxelshape = Shapes.create(aabbCompare);
			boolean collided = Shapes.joinIsNotEmpty(stateCheckPlaceable.getCollisionShape(world, posCheckPlaceable).move(posCheckPlaceable.getX(), posCheckPlaceable.getY(), posCheckPlaceable.getZ()), voxelshape, BooleanOp.AND);

			if (!stateCheckPlaceable.hasProperty(BlockStateProperties.LAYERS) && !collided && !stateCheckPlaceable.liquid())
			{
				posCheckPlaceable = posCheckPlaceable.offset(0, -1, 0);
				stateCheckPlaceable = world.getBlockState(posCheckPlaceable);
				distForPlaceableBlocks++;
				continue;
			}
			else if (stateCheckPlaceable.isFaceSturdy(world, posCheckPlaceable, Direction.UP) || stateCheckPlaceable.is(TFCBlocks.SNOW_PILE.get()) || stateCheckPlaceable.is(Blocks.SNOW) || stateCheckPlaceable.getBlock() instanceof SnowLayerBlock || stateCheckPlaceable.is(TFCWeatherTags.Blocks.SAND_LAYER_BLOCKS) || stateCheckPlaceable.getBlock() instanceof SandLayerBlock)
			{
				break;
			}
			else
			{
				return amount;
			}
		}

		if (distForPlaceableBlocks >= 10)
		{
			return amount;
		}

		if (!stateCheckPlaceable.isFaceSturdy(world, posCheckPlaceable, Direction.UP) && !(stateCheckPlaceable.is(TFCBlocks.SNOW_PILE.get()) || stateCheckPlaceable.is(Blocks.SNOW) || stateCheckPlaceable.getBlock() instanceof SnowLayerBlock || stateCheckPlaceable.is(TFCWeatherTags.Blocks.SAND_LAYER_BLOCKS) || stateCheckPlaceable.getBlock() instanceof SandLayerBlock))
		{
			return amount;
		}

		for (int i = 0; i < distForPlaceableBlocks; i++)
		{
			world.setBlockAndUpdate(posCheckNonAir.offset(0, -i, 0), Blocks.AIR.defaultBlockState());
		}

		BlockPos posPlaceLayerable = new BlockPos(posCheckPlaceable);
		BlockState statePlaceLayerable = world.getBlockState(posPlaceLayerable);

		int amountToAdd = amountAllowedToAdd;

		while (amountAllowedToAdd > 0 && world.getBlockState(posPlaceLayerable.offset(0, 1, 0)).isAir())
		{
			if (amountAllowedToAdd <= 0)
			{
				break;
			}

			if (statePlaceLayerable.hasProperty(BlockStateProperties.LAYERS) && getHeightForLayeredBlock(world, posPlaceLayerable, statePlaceLayerable) < layerableHeightPropMax)
			{
				int height = getHeightForLayeredBlock(world, posPlaceLayerable, statePlaceLayerable);
				height += amountAllowedToAdd;
				if (height > layerableHeightPropMax)
				{
					amountAllowedToAdd = height - layerableHeightPropMax;
					height = layerableHeightPropMax;
				}
				else
				{
					amountAllowedToAdd = 0;
				}
				try
				{
					placeSandOrSnow(world, posPlaceLayerable, blockLayerable, height);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				if (height == layerableHeightPropMax)
				{
					posPlaceLayerable = posPlaceLayerable.offset(0, 1, 0);
					statePlaceLayerable = world.getBlockState(posPlaceLayerable);
				}
			}
			else if (statePlaceLayerable.isFaceSturdy(world, posPlaceLayerable, Direction.UP))
			{
				posPlaceLayerable = posPlaceLayerable.offset(0, 1, 0);
				statePlaceLayerable = world.getBlockState(posPlaceLayerable);
			}
			else if (statePlaceLayerable.isAir())
			{
				int height = amountAllowedToAdd;
				if (height > layerableHeightPropMax)
				{
					amountAllowedToAdd = height - layerableHeightPropMax;
					height = layerableHeightPropMax;
				}
				else
				{
					amountAllowedToAdd = 0;
				}
				try
				{
					placeSandOrSnow(world, posPlaceLayerable, blockLayerable, height);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				if (height == layerableHeightPropMax)
				{
					posPlaceLayerable = posPlaceLayerable.offset(0, 1, 0);
					statePlaceLayerable = world.getBlockState(posPlaceLayerable);
				}
			}
		}

		int amountAdded = amountToAdd - amountAllowedToAdd;
		amount -= amountAdded;
		return amount;
	}

	@Unique
	private static void placeSandOrSnow(Level world, BlockPos pos, Block block, int height)
	{
		//BlockState placeState = setBlockWithLayerState(world, pos, block, height);
		if (block instanceof SnowPileBlock || block instanceof SnowLayerBlock)
		{
			TFCWeatherHelpers.placeSnowPile(world, pos, world.getBlockState(pos), height);
		}
		else if (block instanceof SandLayerBlock)
		{
			SandLayerBlock.placeSandPile(world, pos, world.getBlockState(pos), TFCWeatherHelpers.getSandColor(world, pos), height);
		}
	}

	@Unique
	private static int getHeightForAnyBlock(Level world, BlockPos pos, BlockState state)
	{
		Block block = state.getBlock();
		if (block instanceof SnowLayerBlock)
		{
			return state.getValue(SnowLayerBlock.LAYERS);
		}
		else if (block instanceof SandLayerBlock)
		{
			return state.getValue(SandLayerBlock.LAYERS);
		}
		else if (state.is(BlockTags.SAND) || state.is(BlockTags.SNOW))
		{
			return 8;
		}
		else if (block instanceof SlabBlock)
		{
			return 4;
		}
		else if (block == Blocks.AIR)
		{
			return 0;
		}
		return 8;
	}

	@Unique
	private static int getHeightForLayeredBlock(Level world, BlockPos pos, BlockState state)
	{
		Block block = state.getBlock();
		if (block instanceof SnowLayerBlock)
		{
			return state.getValue(SnowLayerBlock.LAYERS);
		}
		else if (block instanceof SandLayerBlock)
		{
			return state.getValue(SandLayerBlock.LAYERS);
		}
		else if (state.is(BlockTags.SAND) || state.is(BlockTags.SNOW))
		{
			return 8;
		}
		return 0;
	}

	@Unique
	private static BlockState setBlockWithLayerState(Level world, BlockPos pos, Block block, int height)
	{
		if (block instanceof SnowLayerBlock)
		{
			if (height == layerableHeightPropMax)
			{
				return Blocks.SNOW_BLOCK.defaultBlockState();
			}
			return block.defaultBlockState().setValue(SnowLayerBlock.LAYERS, height);
		}
		else if (height == layerableHeightPropMax)
		{
			return TFCWeatherHelpers.getSandBlock(world, pos).defaultBlockState();
		}
		return TFCWeatherHelpers.getSandLayerBlock(world, pos).defaultBlockState().setValue(SandLayerBlock.LAYERS, height);
	}
}
