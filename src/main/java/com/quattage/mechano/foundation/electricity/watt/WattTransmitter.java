package com.quattage.mechano.foundation.electricity.watt;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.watt.unit.Voltage;
import com.quattage.mechano.foundation.electricity.watt.unit.WattUnit;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/**
 */
public class WattTransmitter<T extends DirectionalWattProvidable> implements WattStorable {


    private final float maxCurrentTolerance;
    private final Voltage maxVoltageTolerance;

    private final Consumer<OvervoltEvent> overvoltEvent;
    
    private WattUnit currentStorage;


    public WattTransmitter(float maxCurrentTolerance, Voltage maxVoltageTolerance, @Nullable Consumer<OvervoltEvent> overvoltEvent) {
        this.maxCurrentTolerance = maxCurrentTolerance;
        this.maxVoltageTolerance = maxVoltageTolerance;
        this.overvoltEvent = overvoltEvent;
    }


    @Override
    public WattUnit receiveWatts(WattUnit maxWattsToRecieve, boolean simulate) {
        return null;
    }

    @Override
    public WattUnit extractWatts(WattUnit maxWattsToExtract, boolean simulate) {
        return null;
    }

    @Override
    public void setStoredWatts(float watts, boolean update) {
        return;
    }

    @Override
    public float getStoredWatts() {
        return 0;
    }

    @Override
    public int getMaxCharge() {
        return 0;
    }

    @Override
    public int getMaxDischarge() {
        return 0;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public Voltage getFlux() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFlux'");
    }

    @Override
    public OvervoltBehavior getOvervoltBehavior() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOvervoltBehavior'");
    }

    @Override
    public void setOvervoltBehavior(OvervoltBehavior overvoltBehavior) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setOvervoltBehavior'");
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeTo'");
    }

    @Override
    public void readFrom(CompoundTag in) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readFrom'");
    }

    @Override
    @SuppressWarnings("unchecked")
    public LazyOptional<IEnergyStorage> getFeConverterLazy() {
        return LazyOptional.of(() -> new ExplicitFeConverter<WattTransmitter<?>>(this));
    }

    @Override
    public ExplicitFeConverter<WattStorable> newFeConverter() {
        return new ExplicitFeConverter<WattStorable>(this);
    }

    @Override
    public void onOvervolt(OvervoltEvent event) {
        if(overvoltEvent != null) overvoltEvent.accept(event);
    }
    
}
