package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.Locale;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.placement.PoleHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public abstract class AbstractRotorBlock extends RotatedPillarKineticBlock {

    public static final EnumProperty<RotorModelType> MODEL_TYPE = EnumProperty.create("model", RotorModelType.class);
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public AbstractRotorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, RotorModelType.SINGLE));
    }

    public enum RotorModelType implements StringRepresentable {
        SINGLE, MIDDLE, END_A, END_B;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    abstract boolean isRotor(Block block);

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);

        Direction facing = DirectionTransformer.getForward(state);

        boolean hasRear = this.isRotor(world.getBlockState(pos.relative(facing)).getBlock());
        boolean hasFront = this.isRotor(world.getBlockState(pos.relative(facing.getOpposite())).getBlock());

        if(hasFront && hasRear) setModel(world, pos, state, RotorModelType.MIDDLE);
        else if(hasFront) setModel(world, pos, state, RotorModelType.END_B);
        else if(hasRear)setModel(world, pos, state, RotorModelType.END_A);
        else setModel(world, pos, state, RotorModelType.SINGLE);
    }

    private void setModel(Level world, BlockPos pos, BlockState state, RotorModelType bType) {
        world.setBlock(pos, state.setValue(MODEL_TYPE, bType), Block.UPDATE_ALL);
    }

    @Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult ray) {
        if(player.isShiftKeyDown() || !player.mayBuild()) return InteractionResult.PASS;

        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        ItemStack heldItem = player.getItemInHand(hand);

        if (helper.matchesItem(heldItem))
            return helper.getOffset(player, world, state, pos, ray)
                .placeInWorld(world, (BlockItem)heldItem.getItem(), player, hand, ray);

        return InteractionResult.PASS;
    }

    @Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(AXIS);
	}

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    @MethodsReturnNonnullByDefault
	private static class PlacementHelper extends PoleHelper<Direction.Axis> {
		private PlacementHelper() {
			super(state -> state.getBlock() instanceof AbstractRotorBlock, state -> state.getValue(AXIS), AXIS);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof AbstractRotorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.SMALL_ROTOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful()) {
				offset.withTransform(offset.getTransform()
					.andThen(newState -> ShaftBlock.pickCorrectShaftType(newState, world, offset.getBlockPos())));
            }
			return offset;
		}
	}
}
