package tfcweather.common.blocks;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.common.blocks.soil.TFCSandBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.IFluidLoggable;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.common.fluids.FluidProperty.FluidKey;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.TFCChunkGenerator;

import tfcweather.common.blockentities.TFCWeatherBlockEntities;
import tfcweather.config.TFCWeatherConfig;
import tfcweather.interfaces.ISandColor;
import tfcweather.interfaces.RegistrySand;
import tfcweather.util.TFCWeatherHelpers;

import weather2.util.WindReader;
import weather2.weathersystem.WeatherManager;
import weather2.weathersystem.wind.WindManager;

public class SandLayerBlock extends TFCSandBlock implements IFluidLoggable, IForgeBlockExtension, ISandColor, EntityBlockExtension
{
   public static final FluidProperty ALL_WATER_AND_LAVA = FluidProperty.create("fluid", Stream.of(Fluids.EMPTY, Fluids.WATER, TFCFluids.SALT_WATER, TFCFluids.SPRING_WATER, TFCFluids.RIVER_WATER, Fluids.LAVA));
   public static final FluidProperty FLUID = ALL_WATER_AND_LAVA;
   public static final Boolean SNOWY = false;

   public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
   public static final VoxelShape[] SHAPES = new VoxelShape[]{Shapes.empty(), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};

   public final Supplier<? extends Block> transformsInto;
   public final ExtendedProperties properties;
   public final RegistrySand sandColor;

   public SandLayerBlock(int dustColorIn, ExtendedProperties properties, Supplier<? extends Block> transformsInto, RegistrySand sandColor)
   {
      super(dustColorIn, properties.properties());
      this.properties = properties;
      this.transformsInto = transformsInto;
      this.sandColor = sandColor;
      this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, Integer.valueOf(1)).setValue(this.getFluidProperty(), this.getFluidProperty().keyFor(Fluids.EMPTY)));
   }

   @Override
   public ExtendedProperties getExtendedProperties()
   {
      return properties;
   }

   @Override
   public RegistrySand getSandColor()
   {
      return sandColor;
   }

   public BlockState transformsInto()
   {
      return transformsInto.get().defaultBlockState();
   }

   @Override
   public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid)
   {
      if (fluid instanceof FlowingFluid && !getFluidProperty().canContain(fluid))
      {
         return true;
      }
      return IFluidLoggable.super.canPlaceLiquid(level, pos, state, fluid);
   }

   @Override
   public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidStateIn)
   {
      if (fluidStateIn.getType() instanceof FlowingFluid && !getFluidProperty().canContain(fluidStateIn.getType()))
      {
         return true;
      }
      return IFluidLoggable.super.placeLiquid(level, pos, state, fluidStateIn);
   }

   @Override
   public boolean isPathfindable(BlockState state, BlockGetter worldIn, BlockPos pos, PathComputationType type)
   {
      return true;
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
   {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
   {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos)
   {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   public VoxelShape getVisualShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context)
   {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   public boolean useShapeForLightOcclusion(BlockState state)
   {
      return true;
   }

   @Override
   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
   {
      BlockState blockstate = level.getBlockState(pos.below());
      if (!blockstate.is(Blocks.ICE) && !blockstate.is(Blocks.PACKED_ICE) && !blockstate.is(Blocks.BARRIER))
      {
         if (!blockstate.is(Blocks.HONEY_BLOCK) && !blockstate.is(Blocks.SOUL_SAND))
         {
            return Block.isFaceFull(blockstate.getCollisionShape(level, pos.below()), Direction.UP) || (blockstate.getBlock() instanceof SandLayerBlock && blockstate.getValue(LAYERS) == 8);
         }
         else
         {
            return true;
         }
      }
      else
      {
         return false;
      }
   }

   public void addSandLayer(Level level, BlockState state, BlockPos pos, int extraLayer)
   {
      if (state.getValue(LAYERS) + extraLayer >= 8)
      {
         if (state.getValue(LAYERS) + extraLayer > 8)
         {
            level.setBlock(pos.above(), this.defaultBlockState().setValue(LAYERS, extraLayer), Block.UPDATE_ALL);
         }
         else
         {
            level.setBlock(pos, transformsInto.get().defaultBlockState(), Block.UPDATE_ALL);
         }
      }
      else
      {
         level.setBlock(pos, state.setValue(LAYERS, state.getValue(LAYERS) + extraLayer), Block.UPDATE_ALL);
      }
   }

   @Override
   public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
   {
      FluidHelpers.tickFluid(level, currentPos, state);
      if (state.getValue(LAYERS) > 7 && transformsInto != null)
      {
         level.setBlock(currentPos, transformsInto.get().defaultBlockState(), Block.UPDATE_ALL);
      }
      if (state.canSurvive(level, currentPos))
      {
         return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
      }
      else
      {
         ItemStack itemStack = new ItemStack(this.asItem(), state.getValue(LAYERS));
         ItemEntity itemEntity = new ItemEntity((Level) level, (double)currentPos.getX() + 0.5D, (double)currentPos.getY() + 0.2D + state.getValue(LAYERS) * 0.1D, (double)currentPos.getZ() + 0.5D, itemStack);
         itemEntity.setDefaultPickUpDelay();
         level.addFreshEntity(itemEntity);
         return state.getFluidState().createLegacyBlock();
      }
   }

   @Override
   public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
   {
      int i = state.getValue(LAYERS);
      if (useContext.getItemInHand().is(this.asItem()) && i < 8)
      {
         if (useContext.replacingClickedOnBlock())
         {
            return useContext.getClickedFace() == Direction.UP;
         }
         else
         {
            return true;
         }
      }
      else
      {
         return i == 1;
      }
   }

   @Override
   @SuppressWarnings("deprecation")
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
   {
      final ItemStack stack = player.getItemInHand(hand);
      if (Helpers.isItem(stack, this.asItem()) && transformsInto != null)
      {
         int sandLayers = state.getValue(LAYERS);
         if (sandLayers >= 8 && level.getBlockState(pos.above()).isAir())
            level.setBlockAndUpdate(pos.above(), this.defaultBlockState());
         else if (sandLayers == 7)
            level.setBlockAndUpdate(pos, transformsInto.get().defaultBlockState());
         else
            level.setBlockAndUpdate(pos, state.setValue(LAYERS,  Mth.clamp(sandLayers + 1, 1, 8)));

         if (!player.isCreative())
            stack.shrink(1);

         Helpers.playSound(level, pos, SoundType.SAND.getPlaceSound());
         level.scheduleTick(pos, this, 2);
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
   }

   @Override
   @SuppressWarnings("deprecation")
   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
   {
      if (pos.getY() <= TFCChunkGenerator.SEA_LEVEL_Y)
      {
         FluidHelpers.tickFluid(level, pos, state);
      }
      super.neighborChanged(state, level, fromPos, blockIn, fromPos, isMoving);
   }

   @Override
   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context)
   {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (state.is(this) || (state.getBlock() instanceof SandLayerBlock && state.getValue(LAYERS) >= 1))
      {
         int i = state.getValue(LAYERS);
         return state.setValue(LAYERS, Mth.clamp(i + 1, 1, 8));
      }
      else
      {
         BlockState defaultState = defaultBlockState();
         FluidState fluidState = level.getFluidState(pos);
         FluidKey fluidKey = getFluidProperty().canContain(fluidState.getType()) ? getFluidProperty().keyFor(fluidState.getType()) : getFluidProperty().keyFor(Fluids.EMPTY);
         return defaultState.setValue(getFluidProperty(), fluidKey);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
   {
      builder.add(getFluidProperty(), LAYERS);
   }

   @Override
   @SuppressWarnings("deprecation")
   public FluidState getFluidState(BlockState state)
   {
      return IFluidLoggable.super.getFluidState(state);
   }

   @Override
   public FluidProperty getFluidProperty()
   {
      return FLUID;
   }

   @Override
   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
   {
      if (random.nextInt(TFCWeatherConfig.COMMON.sandLayerErosionChance.get()) == 0)
      {
         WeatherManager weatherMan = WindReader.getWeatherManagerFor(level);
         WindManager windMan = weatherMan.getWindManager();
         Vec3 posSource = new Vec3(pos.getX(), pos.getY(), pos.getZ());
         float windSpeed = windMan.getWindSpeedForClouds();
         if (windSpeed >= random.nextDouble() && weatherMan.getSandstormsAround(posSource, 384).isEmpty())
         {
            float angle = windMan.getWindAngleForClouds();
            double windSpeedFactor = windSpeed * TFCWeatherConfig.COMMON.sandLayerErosionFactor.get();

            double vecX = -Math.sin(Math.toRadians(angle)) * windSpeedFactor;
            double vecZ = Math.cos(Math.toRadians(angle)) * windSpeedFactor;

            int x = Mth.ceil(posSource.x + vecX);
            int z = Mth.ceil(posSource.z + vecZ);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos posAdjacent = new BlockPos(x, y, z);
            BlockState stateAdjacent = level.getBlockState(posAdjacent);
            if (posAdjacent != pos)
            {
               if (posAdjacent.getY() <= pos.getY())
               {
                  if (canPlaceSand(level, posAdjacent, stateAdjacent))
                  {
                     TFCWeatherHelpers.placeSand(level, posAdjacent, stateAdjacent, random, 8, this.getSandColor());
                     removeLayer(level, pos, state);
                  }
               }
               else if (posAdjacent.getY() == pos.above().getY() && state.getValue(LAYERS) >= 7)
               {
                  if (canPlaceSand(level, posAdjacent, stateAdjacent))
                  {
                     TFCWeatherHelpers.placeSand(level, posAdjacent, stateAdjacent, random, 8, this.getSandColor());
                     removeLayer(level, pos, state);
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
   {
      playerWillDestroy(level, pos, state, player);
      removeLayer(level, pos, state);
      return true; // Cause drops and other stuff to occur
   }

   public static boolean canPlaceSandPile(LevelAccessor level, BlockPos pos, BlockState state)
   {
      return Helpers.isBlock(state.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED) && TFCWeatherBlocks.SAND_LAYERS.get(SandBlockType.YELLOW).get().defaultBlockState().canSurvive(level, pos);
   }

   public static void placeSandPile(LevelAccessor level, BlockPos pos, BlockState state, RegistrySand sand, int height)
   {
      final BlockPos posAbove = pos.above();
      final BlockState aboveState = level.getBlockState(posAbove);
      final BlockState savedAboveState = Helpers.isBlock(aboveState.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED) ? aboveState : null;

      BlockState sandPile = TFCWeatherBlocks.SAND_LAYERS.get(sand).get().defaultBlockState().setValue(SandLayerBlock.LAYERS, height);
      sandPile = FluidHelpers.fillWithFluid(sandPile, state.getFluidState().getType());

      level.setBlock(pos, sandPile, Block.UPDATE_ALL_IMMEDIATE);
      if (!(state.getBlock() instanceof SandLayerBlock))
      {
         level.getBlockEntity(pos, TFCWeatherBlockEntities.SAND_PILE.get()).ifPresent(entity -> entity.setHiddenStates(state, savedAboveState, false));
      }

      if (savedAboveState != null)
      {
         Helpers.removeBlock(level, posAbove, Block.UPDATE_ALL_IMMEDIATE);
      }

      level.blockUpdated(pos, TFCWeatherBlocks.SAND_LAYERS.get(sand).get());
      if (savedAboveState != null)
      {
         level.blockUpdated(posAbove, Blocks.AIR);
      }
   }

   public static void removePile(LevelAccessor level, BlockPos pos, BlockState state, int expectedLayers)
   {
      final int layers = state.getValue(LAYERS);
      if (expectedLayers >= layers)
      {
         return;
      }
      if (layers > 1 && expectedLayers != 0)
      {
         level.setBlock(pos, state.setValue(LAYERS, expectedLayers == -1 ? layers - 1 : expectedLayers), Block.UPDATE_ALL_IMMEDIATE);
      }
      else
      {
         level.getBlockEntity(pos, TFCWeatherBlockEntities.SAND_PILE.get()).ifPresent(pile -> {
            if (!level.isClientSide())
            {
               if (pile.getInternalState().getBlock() instanceof SandLayerBlock)
               {
                  level.destroyBlock(pos, false);
               }
               final BlockPos above = pos.above();
               level.setBlock(pos, pile.getInternalState(), Block.UPDATE_ALL_IMMEDIATE);
               if (pile.getAboveState() != null && level.isEmptyBlock(above))
               {
                  level.setBlock(above, pile.getAboveState(), Block.UPDATE_ALL_IMMEDIATE);
               }

               pile.getInternalState().updateNeighbourShapes(level, pos, Block.UPDATE_ALL_IMMEDIATE);
               level.getBlockState(above).updateNeighbourShapes(level, above, Block.UPDATE_ALL_IMMEDIATE);

               level.blockUpdated(pos, pile.getInternalState().getBlock());
               if (pile.getAboveState() != null)
               {
                  level.blockUpdated(above, pile.getAboveState().getBlock());
               }
            }
         });
      }
   }

   public static void removeLayer(LevelAccessor level, BlockPos pos, BlockState state)
   {
      removePile(level, pos, state, -1);
   }

   public static boolean canPlaceSand(LevelAccessor level, BlockPos pos, BlockState state)
   {
      if (TFCWeatherHelpers.isSand(state) && state.getValue(SandLayerBlock.LAYERS) < 7)
      {
         return true;
      }
      else if (SandLayerBlock.canPlaceSandPile(level, pos, state))
      {
         return true;
      }
      return false;
   }
}