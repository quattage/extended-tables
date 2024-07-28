package com.quattage.mechano.content.block.power.alternator.rotor;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

public interface RotorReferable {

    @Nullable
    public abstract AbstractRotorBlockEntity getRotorBE();
    public abstract BlockState getRotorState();
}
