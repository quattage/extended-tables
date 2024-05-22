package com.quattage.mechano.foundation.electricity.core.watt;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable;
import com.quattage.mechano.foundation.electricity.core.watt.unit.Voltage;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * A WattBattery is an EnergyStorage-like object
 * that stores Watt-Ticks, a unit of power storage
 * akin to ForgeEnergy but made of both Voltage and Current.
 * 
 * @see {@link WattUnit <code>WattUnit</code>} for conveying Watts between objects.
 * @see {@link Voltage <code>Voltage</code>} for representing Voltage
 */
public class WattBattery<T extends DirectionalWattProvidable> implements WattStorable {

    private final T parent;

    private final int maxStoredWattTicks;
    
    private final Voltage maxTolerance; // volts in
    private final Voltage maxFlux; // volts out
    
    private final int maxCharge; // watts in
    private final int maxDischarge; // watts out
    
    private final Consumer<Integer> overvoltEvent;
    
    private float storedWattTicks;
    private WattStorable.OvervoltBehavior overvoltBehavior;

    protected WattBattery(T parent, int maxStoredWattTicks, int maxCharge, int maxDischarge, Voltage maxTolerance, Voltage maxFlux, OvervoltBehavior overvoltBehavior, @Nullable Consumer<Integer> overvoltEvent) {

        this.parent = parent;

        this.storedWattTicks = 0;
        this.maxStoredWattTicks = maxStoredWattTicks;
        this.maxFlux = maxFlux;
        this.maxCharge = maxCharge;
        this.maxDischarge = maxDischarge;
        this.maxTolerance = maxTolerance;
        this.overvoltBehavior = overvoltBehavior;
        this.overvoltEvent = overvoltEvent;
        
    }

    /**
     * Create a new WattBatteryBuilder bound to the given Object.
     * <p>
     * <pre>
     * WattBattery<T> batt = WattBattery.newBatteryAt(parent)
            .withFlux(120)                 // output voltage (battery's EMF)
            .withVoltageTolerance(240)     // max input voltage
            .withMaxCharge(2048)           //  maximum watt-ticks in
            .withMaxDischarge(2048)        // maximum watt-ticks out (battery's C-rating)
            .withCapacity(2048)            // max watts battery can hold 
            .withOvervoltBehavior(OvervoltBehavior.LIMIT_LOSSY) 
        .makeWithOvervoltEvent(this::onOvervolt); // event is called when battery is overvolted
        </pre>
     * 
     * @param <T> {@link DirectionalWattProvidable DirectionalWattStorable} Handler for what happens
     * during energy updates and capability provisions. 
     * 
     * @param parent Should handle reading, writing, sending packets, etc.
     * @return A new fluent builder for defining the resulting WattBattery object.
     */
    public static <T extends DirectionalWattProvidable> WattBatteryBuilder newBattery() {
        return new WattBatteryBuilder();
    }

    @Override
    public boolean canExtract() {
        return maxFlux.getRaw() < -32767 ? false : storedWattTicks > 0;
    }

    @Override
    public boolean canReceive() {
        return maxTolerance.getRaw() < -32767 ? false : storedWattTicks < maxStoredWattTicks;
    }

    @Override
    public WattUnit extractWatts(WattUnit maxWattsToExtract, boolean simulate) {
        
        if(maxWattsToExtract.hasNoPotential()) return WattUnit.EMPTY;
        if(!canExtract()) return WattUnit.EMPTY;

        float wattsExtracted  = Math.min(storedWattTicks, Math.min(maxWattsToExtract.getWatts(), maxDischarge));
        if(!simulate) {
            float oldWatts = storedWattTicks;
            storedWattTicks -= wattsExtracted;
            if(oldWatts != storedWattTicks) 
                parent.onWattsUpdated(oldWatts, storedWattTicks);
        }
        return WattUnit.of(maxFlux, wattsExtracted);
    }

    @Override
    public WattUnit receiveWatts(WattUnit maxWattsToRecieve, boolean simulate) {
        
        if(maxWattsToRecieve.hasNoPotential()) return WattUnit.EMPTY;
        if(!canReceive()) return WattUnit.EMPTY;

        float wattsReceived = 0;
        int volts = maxWattsToRecieve.getVoltage().get();

        if(volts <= maxTolerance.get()) {
            wattsReceived = Math.min((float)maxStoredWattTicks - storedWattTicks, Math.min(maxWattsToRecieve.getWatts(), maxCharge));
        } else {
            if(overvoltBehavior == OvervoltBehavior.SOFT_DENY) {
                onOvervolt(maxFlux.get() - volts);
                return WattUnit.EMPTY;
            }
            else if(overvoltBehavior == OvervoltBehavior.LIMIT_LOSSY) {
                onOvervolt(maxFlux.get() - volts);
                return receiveWatts(maxWattsToRecieve.copy().setVoltageLossy(maxFlux.get()), simulate);      
            }
            else if(overvoltBehavior == OvervoltBehavior.TRANSFORM_LOSSLESS) {
                onOvervolt(maxFlux.get() - volts);
                return receiveWatts(maxWattsToRecieve.copy().adjustVoltage(maxFlux.get()), simulate);            
            }

            else throw new UnsupportedOperationException("OvervoltBehavior type " + overvoltBehavior + " has no implementation!");
        }

        if(!simulate) {
            float oldWatts = storedWattTicks;
            storedWattTicks += wattsReceived;
            if(oldWatts != storedWattTicks)
                parent.onWattsUpdated(oldWatts, storedWattTicks);
        }
        return WattUnit.of(volts, wattsReceived);
    }

    @Override
    public float getStoredWatts() {
        return storedWattTicks;
    }

    @Override
    public void setStoredWatts(float watts, boolean update) {
        float oldWatts = getStoredWatts();
        this.storedWattTicks = Math.min(getCapacity(), watts);
        if(update && (oldWatts != storedWattTicks)) parent.onWattsUpdated(oldWatts, oldWatts);
    }

    @Override
    public int getCapacity() {
        return maxStoredWattTicks;
    }

    @Override
    public int getMaxCharge() {
        return maxCharge;
    }

    @Override
    public int getMaxDischarge() {
        return maxDischarge;
    }

    @Override
    public void onOvervolt(int excessVolts) {
        if(overvoltEvent != null) overvoltEvent.accept(excessVolts);
    }

	@Override
	public OvervoltBehavior getOvervoltBehavior() {
		return this.overvoltBehavior;
	}

    @Override
	public void setOvervoltBehavior(OvervoltBehavior overvoltBehavior) {
		this.overvoltBehavior = overvoltBehavior;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LazyOptional<IEnergyStorage> toFeEquivalent() {
        return LazyOptional.of(() -> new ExplicitFeConverter<WattBattery<?>>(this));
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        in.putFloat("storedWatts", storedWattTicks);
        in.putByte("mode", (byte)overvoltBehavior.ordinal());
        return in;
    }

    @Override
    public void readFrom(CompoundTag in) {
        this.storedWattTicks = in.getFloat("storedWatts");
        this.overvoltBehavior = OvervoltBehavior.values()[in.getByte("mode")];
    }
}
