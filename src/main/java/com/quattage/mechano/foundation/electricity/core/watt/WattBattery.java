package com.quattage.mechano.foundation.electricity.core.watt;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
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
    
    private final Consumer<OvervoltEvent> overvoltEvent;
    
    private float storedWattTicks;
    private WattStorable.OvervoltBehavior overvoltBehavior;

    protected WattBattery(T parent, int maxStoredWattTicks, int maxCharge, int maxDischarge, Voltage maxTolerance, Voltage maxFlux, OvervoltBehavior overvoltBehavior, @Nullable Consumer<OvervoltEvent> overvoltEvent) {

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
     * Create a new {@link com.quattage.mechano.foundation.electricity.core.watt.WattBatteryBuilder <code>WattBatteryBuilder</code>} bound to the given Object.
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
     * @param <T> {@link DirectionalWattProvidable <code>DirectionalWattStorable</code>} Handler for what happens
     * during energy updates and capability provisions. 
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
    public WattUnit extractWatts(final WattUnit maxWattsToExtract, boolean simulate) {

        if(!canExtract() || maxWattsToExtract.hasNoPotential()) 
            return WattUnit.EMPTY;

        float wattsExtracted  = Math.min(storedWattTicks, Math.min(maxWattsToExtract.getWatts(), maxDischarge));
        if(!simulate && wattsExtracted > WattUnit.MIN_WATTS) {
            float oldWatts = storedWattTicks;
            storedWattTicks -= wattsExtracted;
            parent.onWattsUpdated(oldWatts, storedWattTicks);
        }
        return WattUnit.of(maxFlux.copy(), wattsExtracted);
    }

    @Override
    public WattUnit receiveWatts(final WattUnit maxWattsToRecieve, boolean simulate) {

        if(!canReceive() || maxWattsToRecieve.hasNoPotential()) 
            return WattUnit.EMPTY;

        float wattsReceived = 0;
        int volts = maxWattsToRecieve.getVoltage().get();

        boolean hasBeenOvervolted = false;
        WattUnit actualReceievedWatts = maxWattsToRecieve.copy();

        if(volts <= maxTolerance.get()) {
            wattsReceived = Math.min((float)maxStoredWattTicks - storedWattTicks, Math.min(maxWattsToRecieve.getWatts(), maxCharge));
        } else {
            if(overvoltBehavior == OvervoltBehavior.SOFT_DENY) {
                hasBeenOvervolted = true;
                return WattUnit.EMPTY;
            }
            else if(overvoltBehavior == OvervoltBehavior.LIMIT_LOSSY) {
                hasBeenOvervolted = true;
                actualReceievedWatts.setVoltageLossy(maxFlux.get());
                volts = actualReceievedWatts.getVoltage().get();
            }
            else if(overvoltBehavior == OvervoltBehavior.TRANSFORM_LOSSLESS) {
                hasBeenOvervolted = true;
                actualReceievedWatts.adjustVoltage(maxFlux.get());
                volts = actualReceievedWatts.getVoltage().get();
            }
            else throw new UnsupportedOperationException("OvervoltBehavior type " + overvoltBehavior + " has no implementation!");

            // TOOD some other stuff perhaps
            wattsReceived = Math.min(maxStoredWattTicks - storedWattTicks, Math.min(actualReceievedWatts.getWatts(), maxCharge));
        }

        if(!simulate && wattsReceived > WattUnit.MIN_WATTS) {
            float oldWatts = storedWattTicks;
            storedWattTicks += wattsReceived;
            parent.onWattsUpdated(oldWatts, storedWattTicks);
            if(hasBeenOvervolted) onOvervolt(new OvervoltEvent(maxFlux.get(), volts));
        }

        return WattUnit.of(volts, wattsReceived);
    }

    @Override
    public float getStoredWatts() {
        return storedWattTicks;
    }

    @Override
    public void setStoredWatts(float watts, boolean update) {
        if(!update) {
            this.storedWattTicks = Math.min(getCapacity(), watts);
            return;
        }

        float oldWatts = getStoredWatts();
        this.storedWattTicks = Math.min(getCapacity(), watts);
        if(oldWatts != storedWattTicks) 
            parent.onWattsUpdated(oldWatts, oldWatts);
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
    public void onOvervolt(OvervoltEvent event) {
        Mechano.log("BATTERY IS OVERVOLTED!");
        if(overvoltEvent != null) overvoltEvent.accept(event);
    }

	@Override
	public OvervoltBehavior getOvervoltBehavior() {
		return this.overvoltBehavior;
	}

    @Override
    public Voltage getFlux() {
        return maxFlux;
    }

    @Override
	public void setOvervoltBehavior(OvervoltBehavior overvoltBehavior) {
		this.overvoltBehavior = overvoltBehavior;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LazyOptional<IEnergyStorage> getFeConverterLazy() {
        return LazyOptional.of(() -> new ExplicitFeConverter<WattBattery<?>>(this));
    }

    @Override
    public ExplicitFeConverter<WattStorable> newFeConverter() {
        return new ExplicitFeConverter<WattStorable>(this);
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        in.putFloat("storedWatts", storedWattTicks);
        in.putByte("ovb", (byte)overvoltBehavior.ordinal());
        return in;
    }

    @Override
    public void readFrom(CompoundTag in) {  
        this.overvoltBehavior = OvervoltBehavior.values()[in.getByte("ovb")];
        this.storedWattTicks = in.getFloat("storedWatts");
    }
}
