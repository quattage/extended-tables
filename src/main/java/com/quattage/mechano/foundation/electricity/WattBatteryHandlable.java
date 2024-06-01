package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.foundation.electricity.builder.WattBatteryHandlerBuilder;

import net.minecraft.world.level.block.state.BlockState;

/**
 * It is required that any class which instantiates and uses a WattBatteryHandler 
 * implement the WattBatteryHandlable interface
 */
public interface WattBatteryHandlable {

    /**
     * Typically called in an implementing class's constructor to initiate a fluent chain in a subclass. 
     * Used for building {@link com.quattage.mechano.foundation.electricity.}
     * @param builder
     */
    void createWattHandlerDefinition(WattBatteryHandlerBuilder<? extends WattBatteryHandlable> builder);

    /**
     * This method is optionally overridable and provided to expose additional functionality where needed, the WattHandler already
     * sends packets and syncs BEs, so that does not need to be done here.
     */
    default void onWattsUpdated() {}

    /**
     * Called by the Block to reflect a BlockState change, whether that be by a moving contraption or with a wrench. Used to update 
     * the sided WattStorable capabilities of the WattHandler. 
     * @param state
     */
    abstract void reOrient(BlockState state);

    abstract WattBatteryHandler<? extends WattBatteryHandlable> getWattBatteryHandler();
}
