package com.quattage.mechano.content.block.power.alternator.rotor.dummy;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.BlockRotorable;
import com.quattage.mechano.foundation.block.BlockChangeListenable;
import com.quattage.mechano.foundation.helper.CreativeTabExcludable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class BigRotorDummyBlock extends RotatedPillarBlock implements IBE<BigRotorDummyBlockEntity>, BlockRotorable, CreativeTabExcludable, BlockChangeListenable {

    private BigRotorDummyBlockEntity cached = null;

    public BigRotorDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    @Override
    public Axis getRotorAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
	public BlockPos getParentPos(Level world, BlockPos pos, BlockState state) {
		BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof BigRotorDummyBlockEntity bde) return bde.getParentPos();
        return null;
	}

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if(world.getBlockEntity(pos) instanceof BigRotorDummyBlockEntity brdbe) {
            brdbe.forceRemoveDummies(world, fromPos, state.getValue(AXIS));
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
            Player player) {
        return new ItemStack(MechanoBlocks.BIG_ROTOR);
    }

    @Override
    public Class<BigRotorDummyBlockEntity> getBlockEntityClass() {
        return BigRotorDummyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BigRotorDummyBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.BIG_ROTOR_DUMMY.get();
    }

    @Override
    public void onAfterBlockBroken(Level world, BlockPos pos, BlockState currentState, BlockState futureState) {
        if(cached != null)
            cached.obliterate();
        cached = null;
    }

    @Override
    public void onBeforeBlockBroken(Level world, BlockPos pos, BlockState currentState, BlockState futureState) {
        if(world.getBlockEntity(pos) instanceof BigRotorDummyBlockEntity brdbe)
            cached = brdbe;
    }
}