package com.quattage.mechano.content.block.power.alternator.rotor.dummy;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.content.block.power.alternator.rotor.BlockRotorable;
import com.quattage.mechano.foundation.helper.CreativeTabExcludable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BigRotorDummyBlock extends RotatedPillarBlock implements BlockRotorable, IBE<BigRotorDummyBlockEntity>, CreativeTabExcludable {
    public BigRotorDummyBlock(Properties p_52591_) {
        super(p_52591_);
    }

    @Override
    public Class<BigRotorDummyBlockEntity> getBlockEntityClass() {
        return BigRotorDummyBlockEntity.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean pIsMoving) {        

        if(state.getBlock() == newState.getBlock()) {
            super.onRemove(state, world, pos, newState, pIsMoving);
            return;
        }

        BlockEntity dummy = world.getBlockEntity(pos);
        if(dummy instanceof BigRotorDummyBlockEntity dbe)
            dbe.obliterate();

        super.onRemove(state, world, pos, newState, pIsMoving);
    }

    @Override
    public Axis getRotorAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public BlockEntityType<? extends BigRotorDummyBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.BIG_ROTOR_DUMMY.get();
    }
}
