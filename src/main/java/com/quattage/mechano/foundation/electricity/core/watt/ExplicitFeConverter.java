package com.quattage.mechano.foundation.electricity.core.watt;

import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnitConversions;

import net.minecraftforge.energy.IEnergyStorage;

public class ExplicitFeConverter<T extends WattStorable> implements IEnergyStorage {

    private final T parent;

    public ExplicitFeConverter(T wattBattery) {
        this.parent = wattBattery;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        WattUnit parentOperation = parent.receiveWatts(WattUnitConversions.toWatts(maxReceive), simulate);
        return WattUnitConversions.toFE(parentOperation.getWatts());
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        WattUnit parentOperation = parent.extractWatts(WattUnitConversions.toWatts(maxExtract), simulate);
        return WattUnitConversions.toFE(parentOperation);
    }

    @Override
    public int getEnergyStored() {
        return WattUnitConversions.toFE(parent.getStoredWatts());
    }

    @Override
    public int getMaxEnergyStored() {
        return WattUnitConversions.toFE(parent.getCapacity());
    }

    @Override
    public boolean canExtract() {
        return parent.canExtract();
    }

    @Override
    public boolean canReceive() {
        return parent.canReceive();
    }
    
}
