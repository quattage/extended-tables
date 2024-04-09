package com.quattage.mechano.content.block.power.alternator.rotor;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.dummy.BigRotorDummyBlock;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.placement.PoleHelper;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BigRotorBlock extends AbstractRotorBlock implements IBE<BigRotorBlockEntity>{

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public BigRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<BigRotorBlockEntity> getBlockEntityClass() {
        return BigRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BigRotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.BIG_ROTOR.get();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return canPlaceDummies(world, pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        placeDummies(world, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean pIsMoving) {

        if(state.getBlock() != newState.getBlock())
            removeDummies(world, pos, state);

        super.onRemove(state, world, pos, newState, pIsMoving);
    }

    protected void placeDummies(Level world, BlockPos pos, BlockState state) {

        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getCorners(pos, state.getValue(AXIS));

        for(BlockPos vPos : BlockPos.betweenClosed(corners.getFirst(), corners.getSecond())) {
            if(!(world.getBlockState(vPos).getBlock() instanceof BigRotorBlock)) {
                BlockState dummy = MechanoBlocks.BIG_ROTOR_DUMMY.get().defaultBlockState();
                dummy = dummy.setValue(AXIS, state.getValue(AXIS));
                world.setBlock(vPos, dummy, 3);
            }
        }
    }

    protected void removeDummies(Level world, BlockPos pos, BlockState state) {

        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getCorners(pos, state.getValue(AXIS));
        BlockPos.betweenClosed(corners.getFirst(), corners.getSecond()).forEach(vPos -> {

            if(world.getBlockState(vPos).getBlock() instanceof BigRotorDummyBlock)
                world.setBlock(vPos, Blocks.AIR.defaultBlockState(), 3);
        });
    }

    protected boolean canPlaceDummies(LevelReader world, BlockPos pos, BlockState state) {
        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getCorners(pos, state.getValue(AXIS));
        for(BlockPos vPos : BlockPos.betweenClosed(corners.getFirst(), corners.getSecond())) {
            if(!world.getBlockState(vPos).canBeReplaced())
                return false;
        }
        return true;
    }

    @Override
    boolean isRotor(Block block) {
        return block == MechanoBlocks.BIG_ROTOR.get();
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends PoleHelper<Direction.Axis> {

		protected PlacementHelper() {
			super(state -> state.getBlock() instanceof BigRotorBlock, state -> state.getValue(AXIS), AXIS);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof BigRotorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.BIG_ROTOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if(offset.isSuccessful()) {
				offset.withTransform(offset.getTransform()
					.andThen(newState -> ShaftBlock.pickCorrectShaftType(newState, world, offset.getBlockPos())));
            }
			return offset;
		}
	}
}
