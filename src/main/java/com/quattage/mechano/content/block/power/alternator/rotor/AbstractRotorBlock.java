package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.Locale;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.quattage.mechano.foundation.block.BlockChangeListenable;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;

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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("deprecation")
public abstract class AbstractRotorBlock extends RotatedPillarKineticBlock implements BlockRotorable, BlockChangeListenable {

    public static final EnumProperty<RotorModelType> MODEL_TYPE = EnumProperty.create("model", RotorModelType.class);

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

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction out = context.getClickedFace();
        if(context.getPlayer().isShiftKeyDown()) {

        }
        return defaultBlockState().setValue(AXIS, out.getAxis());
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

        IPlacementHelper helper = PlacementHelpers.get(getPlacementHelperId());
        ItemStack heldItem = player.getItemInHand(hand);

        if(helper.matchesItem(heldItem))
            return helper.getOffset(player, world, state, pos, ray)
                .placeInWorld(world, (BlockItem)heldItem.getItem(), player, hand, ray);

        return InteractionResult.PASS;
    }

    @Override
    public void onBlockPlaced(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {
        if(world.getBlockEntity(pos) instanceof AbstractRotorBlockEntity thisArbe) {

            thisArbe.findConnectedStators(true);
            Direction dir = DirectionTransformer.toDirection(thisArbe.getBlockState().getValue(RotatedPillarKineticBlock.AXIS));

            if(world.getBlockEntity(pos.relative(dir)) instanceof AbstractRotorBlockEntity arbeNegative) {
                SlipRingShaftBlockEntity controller = arbeNegative.getController();
                if(controller != null) {
                    thisArbe.setControllerPos(controller.getBlockPos(), true);
                    return;
                }
            }
            
            if(world.getBlockEntity(pos.relative(dir.getOpposite())) instanceof AbstractRotorBlockEntity arbePositive) {
                SlipRingShaftBlockEntity controller = arbePositive.getController();
                if(controller != null) {
                    thisArbe.setControllerPos(controller.getBlockPos(), true);
                    return;
                }
            }

            if(!searchForSlipRing(world, pos, dir))
                searchForSlipRing(world, pos, dir.getOpposite());
        }
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {
        if(world.getBlockEntity(pos) instanceof AbstractRotorBlockEntity thisArbe) {
            SlipRingShaftBlockEntity controller = thisArbe.getController();
            //AA if(controller == null) return;
                //AA controller.evaluateAlternatorStructure();
        }
    }

    private boolean searchForSlipRing(Level world, BlockPos pos, Direction dir) {

        if(!(world.getBlockEntity(pos) instanceof AbstractRotorBlockEntity thisArbe)) return false;

        for(int x = 0; x < MechanoSettings.ALTERNATOR_MAX_LENGTH; x++) {

            BlockPos thisPos = pos.relative(dir, x + 1);
            if(world.getBlockEntity(thisPos) instanceof SlipRingShaftBlockEntity srbe && srbe.getBlockState().getValue(DirectionalKineticBlock.FACING) == dir.getOpposite()) {
                thisArbe.setControllerPos(thisPos, true);
                return true;
            } 
            
            if(!(world.getBlockEntity(thisPos) instanceof AbstractRotorBlockEntity))
                break;
        }

        return false;
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
    public Axis getRotorAxis(BlockState state) {
        return getRotationAxis(state);
    }

    @Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODEL_TYPE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    protected abstract int getPlacementHelperId();
}