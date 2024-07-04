package com.quattage.mechano.foundation.electricity.core.watt;

import com.quattage.mechano.foundation.electricity.WattBatteryHandler;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;

import net.minecraft.core.BlockPos;

public class WattSendSummary {
    final WattBatteryHandler<?> destination;
    final BlockPos sourcePos;
    final BlockPos destinationPos;
    final GridPath addressedPath;

    public WattSendSummary(WattBatteryHandler<?> destination, BlockPos sourcePos, BlockPos destinationPos, GridPath addressedPath) {
        this.destination = destination;
        this.sourcePos = sourcePos;
        this.destinationPos = destinationPos;
        this.addressedPath = addressedPath;
    }

    public WattBatteryHandler<?> getDestination() {
        return destination;
    }

    public BlockPos getBlockPos() {
        return destinationPos;
    }

    public GridPath getAddressedPath() {
        return addressedPath;
    }
}
