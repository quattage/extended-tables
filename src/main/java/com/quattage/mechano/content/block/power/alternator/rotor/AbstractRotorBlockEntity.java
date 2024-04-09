package com.quattage.mechano.content.block.power.alternator.rotor;

import com.quattage.mechano.content.block.power.alternator.stator.AbstractStatorBlock;
import com.quattage.mechano.foundation.helper.shape.CircleGetter;
import com.quattage.mechano.foundation.helper.shape.ShapeGetter;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.placement.PlacementOffset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractRotorBlockEntity extends KineticBlockEntity {

    private final ShapeGetter circle = ShapeGetter.ofShape(CircleGetter.class).withRadius(getStatorRadius()).build();

    public AbstractRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    protected abstract int getStatorCircumference();
    protected abstract int getStatorRadius();

    public void updateStators(Level world, BlockPos rotorPos, BlockState state) {
        Axis revolvingAxis = state.getValue(AbstractRotorBlock.AXIS);
        circle.moveTo(rotorPos).setAxis(revolvingAxis).evaluatePlacement(perimeterPos -> {

            BlockState perimeterState = world.getBlockState(perimeterPos);
            if(perimeterState.getBlock() instanceof AbstractStatorBlock block)
                addConnectedStator(block);

            return PlacementOffset.success();
        });
    }

    public boolean addConnectedStator(AbstractStatorBlock block) {
        // if(stators.size() >= getStatorCircumference()) return false;
        // return stators.add(block);
        return false;
    }

    public int getStatorCount() {
        // return stators.size();
        return 0;
    }

    public int getMultiplier() {
        return 1;
    }
}
