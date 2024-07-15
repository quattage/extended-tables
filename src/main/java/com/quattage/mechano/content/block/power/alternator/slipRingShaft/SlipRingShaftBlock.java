package com.quattage.mechano.content.block.power.alternator.slipRingShaft;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.SmallRotorBlock;
import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.watt.WattSendSummary;
import com.quattage.mechano.foundation.block.BlockChangeListenable;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

public class SlipRingShaftBlock extends DirectionalKineticBlock implements IBE<SlipRingShaftBlockEntity>, BlockChangeListenable {

    private static RotatableHitboxShape<Direction> hitbox;

    public static final EnumProperty<CollectorBlockModelType> MODEL_TYPE = EnumProperty.create("model", CollectorBlockModelType.class);

    public SlipRingShaftBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState().setValue(MODEL_TYPE, CollectorBlockModelType.BASE));
    }

    public enum CollectorBlockModelType implements StringRepresentable, HitboxNameable {
        BASE(false), ROTORED(true), BIG_ROTORED(true);

        final boolean isRotored;

        private CollectorBlockModelType(boolean isRotored) {
            this.isRotored = isRotored;
        }

        public boolean isRotored() {
            return this.isRotored;
        }

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
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        BlockPos origin = context.getClickedPos();
        for(Direction dir : Direction.values()) {
            BlockState relativeState = context.getLevel().getBlockState(origin.relative(dir));
            if(relativeState.getBlock() instanceof AbstractRotorBlock) {
                if(relativeState.getValue(RotatedPillarKineticBlock.AXIS) == dir.getAxis())
                    return defaultBlockState().setValue(DirectionalKineticBlock.FACING, dir);
            }
        }

        return super.getStateForPlacement(context);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1f;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        hitbox = MechanoClient.HITBOXES.get(FACING, state.getValue(MODEL_TYPE), this);
        return hitbox.getRotated(state.getValue(FACING));
    }

    @Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
		return dir.getAxis() == state.getValue(FACING).getAxis();
	}

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        updateRotorType(world, pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(state, world, pos, pBlock, pFromPos, pIsMoving);
        if(state.getValue(MODEL_TYPE).isRotored()) {
            if(!getRotoredType(world, pos, state).isRotored()) 
                world.destroyBlock(pos, true);
        }
        else updateRotorType(world, pos, state);

        evaluateNeighbor(world, pos, pFromPos);
    }

    public void evaluateNeighbor(Level world, BlockPos sourcePos, BlockPos updatePos) {
        if(world.getBlockEntity(sourcePos) instanceof SlipRingShaftBlockEntity srbe) {
            if(world.getBlockEntity(updatePos) instanceof WireAnchorBlockEntity wabe) {

                if(!srbe.canControl() && srbe.opposingPos != null) {
                    if(world.getBlockEntity(srbe.opposingPos) instanceof SlipRingShaftBlockEntity srbe2)
                        srbe = srbe2;
                }

                srbe.sends.add(new WattSendSummary(wabe.battery, sourcePos, wabe.getBlockPos(), null));
            } else
                srbe.sends.removeBy(
                    updatePos, (lookup, compare) -> (
                        lookup.getDestinationPos().equals(compare)
                    ), true
                );
        }
    }

    private void updateRotorType(Level world, BlockPos pos, BlockState state) {
        CollectorBlockModelType result = getRotoredType(world, pos, state);
        if(result != state.getValue(MODEL_TYPE))
            world.setBlock(pos, state.setValue(MODEL_TYPE, result), 3);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack heldItem = player.getItemInHand(hand);
        if(heldItem == ItemStack.EMPTY && player.isShiftKeyDown()) {
            boolean result = true; //       <----   TODO CHANGE PLACEHOLDER
            if(result == true) {
                world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.3f, 1);
                world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.1f, 3);
                spawnParticles(world, ParticleTypes.END_ROD, pos, 10);
            } else {
                world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.BLOCKS, 0.2f, 8);
                world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 8);
                spawnParticles(world, ParticleTypes.SMOKE, pos, 10);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void spawnParticles(Level world, SimpleParticleType type, BlockPos pos, int count) {
        Random r = new Random();
        for(int x = 0; x < count; x++) {
            Supplier<Double> rS = () -> (r.nextDouble() - 0.5d) * .1f;
		    Supplier<Double> rO = () -> ((r.nextDouble() - 0.5d) * .2f) + 0.5f;
            world.addParticle(type, pos.getX() + rO.get(), pos.getY() + rO.get(), pos.getZ() + rO.get(), rS.get(), rS.get(), rS.get());
        }
    }

    private CollectorBlockModelType getRotoredType(Level world, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FACING);

        BlockState forwardState = world.getBlockState(pos.relative(dir));
        if(forwardState.getBlock() instanceof AbstractRotorBlock rb && forwardState.getValue(RotatedPillarKineticBlock.AXIS) == dir.getAxis())
            return rb instanceof SmallRotorBlock ? CollectorBlockModelType.ROTORED : CollectorBlockModelType.BIG_ROTORED;
        return CollectorBlockModelType.BASE;
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {
    }

    @Override
    public void onBeforeBlockBroken(Level world, BlockPos pos, BlockState currentState, BlockState destinedState) {
        if(world.getBlockEntity(pos) instanceof SlipRingShaftBlockEntity srbe)
            srbe.forgetAlternatorStructure(currentState.getValue(DirectionalKineticBlock.FACING));
    }

    @Override
    public Class<SlipRingShaftBlockEntity> getBlockEntityClass() {
        return SlipRingShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SlipRingShaftBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.SLIP_RING_SHAFT.get();
    }
}