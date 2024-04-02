package com.quattage.mechano.content.block.power.alternator.stator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.SmallRotorBlockEntity;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.Hitbox;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class StatorBlock extends SimpleOrientedBlock implements IBE<StatorBlockEntity> {

    public static final EnumProperty<StatorBlockModelType> MODEL_TYPE = EnumProperty.create("model", StatorBlockModelType.class);  // BASE or CORNER
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());
    private static Hitbox<SimpleOrientation> hitbox;

    public StatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(MODEL_TYPE, StatorBlockModelType.BASE_MIDDLE)
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(hitbox == null) hitbox = Mechano.HITBOXES.get(ORIENTATION, state.getValue(MODEL_TYPE), this);
        return hitbox.getRotated(state.getValue(ORIENTATION));
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        updateRotorsAround(pLevel, pPos, pState);
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        updateRotorsAround(pLevel, pPos,pState);
    }

    private void updateRotorsAround(Level pLevel, BlockPos pPos, BlockState pState) {
        if(pLevel == null) return;
        Axis axis = pState.getValue(ORIENTATION).getOrient();
        BlockPos corner1 = pPos.offset(axis == Axis.Z ? 1 : 0,1, axis == Axis.X ? 1 : 0);
        BlockPos corner2 =  pPos.offset(axis == Axis.Z ? -1 : 0,-1, axis == Axis.X ? -1 : 0);
        BlockPos.betweenClosed(corner1, corner2).forEach(pos -> {
            if (pLevel.getBlockEntity(pos) instanceof SmallRotorBlockEntity rotor)
                rotor.updateStatorCount();
        });
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean pIsMoving) {

        Direction cardinal = state.getValue(StatorBlock.ORIENTATION).getCardinal();
        Axis orientation = state.getValue(StatorBlock.ORIENTATION).getOrient();

        BlockState rear = world.getBlockState(pos.relative(cardinal));
        BlockState front = world.getBlockState(pos.relative(cardinal.getOpposite()));

        boolean hasRear = this.isStator(rear.getBlock()) && rear.getValue(StatorBlock.ORIENTATION).getOrient() == orientation;
        boolean hasFront = this.isStator(front.getBlock()) && front.getValue(StatorBlock.ORIENTATION).getOrient() == orientation;

        StatorBlockModelType type = state.getValue(StatorBlock.MODEL_TYPE);
        StatorBlockModelType typeNew = type.copy();

        if(hasFront && hasRear) typeNew.toMiddle();
        else if(hasFront) typeNew.toEndB();
        else if(hasRear) typeNew.toEndA();
        else typeNew.toSingle();

        if(typeNew != type) setModel(world, pos, state, typeNew);

        super.neighborChanged(state, world, pos, block, fromPos, pIsMoving);
    }

    public boolean isStator(Block block) {
        return block == MechanoBlocks.STATOR.get();
    }

    private void setModel(Level world, BlockPos pos, BlockState state, StatorBlockModelType bType) {
        world.setBlock(pos, state.setValue(MODEL_TYPE, bType), Block.UPDATE_ALL);
    }

    @Override
    public Class<StatorBlockEntity> getBlockEntityClass() {
        return StatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StatorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.STATOR.get();
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODEL_TYPE);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult ray) {
        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);

        ItemStack heldItem = player.getItemInHand(hand);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		return InteractionResult.PASS;
	}

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if(state.getValue(MODEL_TYPE).isCorner())
            return InteractionResult.PASS;
        return super.onWrenched(state, context);
    }

    public boolean hasSegmentTowards(BlockPos pos, Direction dir, Level world) {
        return world.getBlockState(pos.relative(dir)).getBlock() == MechanoBlocks.SMALL_ROTOR.get();
	}

    public static BlockState getStatorOrientation(BlockState originalState, BlockState stateForPlacement, Level level, BlockPos pos) {
		return stateForPlacement.setValue(ORIENTATION, originalState.getValue(ORIENTATION));
	}

    @MethodsReturnNonnullByDefault
	private static class PlacementHelper extends StatorDirectionalHelper<SimpleOrientation> {
		// co-opted from Create's shaft placement helper, but this uses SimpelOrientation instead of DirectionalAxis

		private PlacementHelper() {
			super(state -> state.getBlock() instanceof StatorBlock, state -> state.getValue(ORIENTATION), ORIENTATION);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof StatorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.STATOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful()) {
				offset.withTransform(offset.getTransform());
            }
			return offset;
		}
	}
}
