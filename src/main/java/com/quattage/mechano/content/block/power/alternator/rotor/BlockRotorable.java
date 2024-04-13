package com.quattage.mechano.content.block.power.alternator.rotor;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockRotorable {


    public abstract Axis getRotorAxis(BlockState state);
}
