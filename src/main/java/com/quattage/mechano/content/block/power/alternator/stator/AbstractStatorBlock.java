package com.quattage.mechano.content.block.power.alternator.stator;

import javax.annotation.Nullable;

import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.content.block.power.alternator.rotor.BlockRotorable;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.Hitbox;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractStatorBlock<T extends Enum<T> & StringRepresentable & HitboxNameable & StatorTypeTransformable<T>> extends SimpleOrientedBlock {

    private static Hitbox<SimpleOrientation> hitbox = new Hitbox<SimpleOrientation>();

    public AbstractStatorBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(getTypeProperty(), getDefaultModelType()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(hitbox.needsBuilt()) hitbox = MechanoClient.HITBOXES.collectAllOfType(this);
        return hitbox.get(state.getValue(getTypeProperty())).getRotated(state.getValue(ORIENTATION));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return getInitialState(context.getLevel(), context.getClickedPos(), state, state.getValue(ORIENTATION).getOrient());
    }

    public BlockState getInitialState(Level world, BlockPos pos, @Nullable BlockState state, Axis rotorAxis) {
        
        if(state == null) 
            state = this.defaultBlockState();

        BlockPos[] planePositions = getAlignmentType().toPositions(pos, rotorAxis);
        if(getAlignmentType().needsForcedUpdates()) {
            for(BlockPos visitedPos : planePositions) {
                BlockState visitedState = world.getBlockState(visitedPos);
                if(visitedState.getBlock() instanceof AbstractStatorBlock asb)
                    asb.forcedNeighborChange(world, rotorAxis, visitedPos, state, visitedState, planePositions);
            }
        }

        if(getAlignmentType() == UpdateAlignment.CORNERS) {

        } else {

            for(BlockPos visitedPos : planePositions) {
                BlockState visitedState = world.getBlockState(visitedPos);
                if(visitedState.getBlock() instanceof BlockRotorable br) {

                    if(br.getRotorAxis(visitedState) != rotorAxis) continue;        
                    state = 
                        state.setValue(ORIENTATION, SimpleOrientation.combine(Direction.fromDelta(
                            pos.getX() - visitedPos.getX(), 
                            pos.getY() - visitedPos.getY(), 
                            pos.getZ() - visitedPos.getZ()
                        ).getOpposite(), rotorAxis)
                    );

                    break;
                }
            }
        }

        return getAlignedModelState(world, rotorAxis, pos, state, state, planePositions);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean pIsMoving) {

        SimpleOrientation thisOrient = state.getValue(SmallStatorBlock.ORIENTATION);
        Direction orientation = DirectionTransformer.toDirection(thisOrient.getOrient());

        BlockState rear = world.getBlockState(pos.relative(orientation));
        BlockState front = world.getBlockState(pos.relative(orientation.getOpposite()));

        boolean hasRear = this.isStator(rear.getBlock()) && rear.getValue(SmallStatorBlock.ORIENTATION) == thisOrient;
        boolean hasFront = this.isStator(front.getBlock()) && front.getValue(SmallStatorBlock.ORIENTATION) == thisOrient;

        T type = state.getValue(getTypeProperty());
        T typeNew = type.copy();

        if(hasFront && hasRear) typeNew = typeNew.toMiddle();
        else if(hasFront) typeNew = typeNew.toEndA();
        else if(hasRear) typeNew =  typeNew.toEndB();
        else typeNew = typeNew.toSingle();

        if(typeNew != type) setModel(world, pos, state, typeNew, true);

        super.neighborChanged(state, world, pos, block, fromPos, pIsMoving);
    }

    public void forcedNeighborChange(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane) {

        Axis thisAxis = thisState.getValue(ORIENTATION).getOrient();
        if(fromAxis != thisAxis) return;

        BlockState newState = getAlignedModelState(world, fromAxis, centerPos, fromState, thisState, plane);
        if(thisState != newState)
            setModel(world, centerPos, thisState);
    }

    
    protected void setModel(Level world, BlockPos pos, BlockState state, T bType, boolean update) {
        world.setBlock(pos, state.setValue(getTypeProperty(), bType), update ? 3 : 16);
    }

    protected void setModel(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, 16);
    }

    protected abstract int getPlacementHelperId();
    protected abstract BlockEntry<? extends AbstractStatorBlock<?>> getEntry();
    protected abstract EnumProperty<T> getTypeProperty();
    protected abstract T getDefaultModelType();
    protected abstract UpdateAlignment getAlignmentType();
    protected abstract BlockState getAlignedModelState(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane);

    public abstract boolean isStator(Block block);
    public abstract int getRadius();

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(getTypeProperty());
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult ray) {
        IPlacementHelper helper = PlacementHelpers.get(getPlacementHelperId());

        ItemStack heldItem = player.getItemInHand(hand);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		return InteractionResult.PASS;
	}

    protected enum UpdateAlignment {
        ADJACENT(false),
        CORNERS(true);

        private final boolean needsForcedUpdates;

        private UpdateAlignment(boolean needsForcedUpdates) {
            this.needsForcedUpdates = needsForcedUpdates;
        }

        protected boolean needsForcedUpdates() {
            return this.needsForcedUpdates;
        }

        protected BlockPos[] toPositions(BlockPos center, Axis axis) {
            if(this == CORNERS)
                return DirectionTransformer.getAllCorners(center, axis);
            return DirectionTransformer.getAllAdjacent(center, axis);
        }
    }
}
