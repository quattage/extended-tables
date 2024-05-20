package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;

import net.minecraft.world.level.block.state.BlockState;

public interface BatteryBankUpdatable {

    void createBatteryBankDefinition(BatteryBankBuilder<? extends BatteryBankUpdatable> builder);

    /***
     * Called whenever the Energy stored within this ElectricBlockEntity is
     * changed in any way. <p> Sending block updates and packets is handled by
     * the BatteryBank object, so you won't have to do that here.
     */
    default void onEnergyUpdated() {

    }

    abstract void reOrient(BlockState state);

}
