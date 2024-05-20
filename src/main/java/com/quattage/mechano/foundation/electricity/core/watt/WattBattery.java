package com.quattage.mechano.foundation.electricity.core.watt;

import java.util.function.Consumer;

import com.quattage.mechano.foundation.electricity.core.DirectionalWattStorable;
import com.quattage.mechano.foundation.electricity.core.watt.unit.Voltage;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;

/**
 * A WattBattery is an EnergyStorage-like object
 * that stores Watt-Ticks, a unit of power storage
 * akin to ForgeEnergy but made of both Voltage and Current.
 * 
 * @see {@link WattUnit <code>WattUnit</code>} for conveying Watts between objects.
 * @see {@link Voltage <code>Voltage</code>} for representing Voltage
 */
public class WattBattery<T extends DirectionalWattStorable> implements WattStorable {

    private final T parent;

    private final int maxStoredWattTicks;
    
    private final Voltage maxTolerance; // volts in
    private final Voltage maxFlux; // volts out
    
    private final int maxCharge; // watts in
    private final int maxDischarge; // watts out
    
    private final Consumer<Integer> overvoltEvent;
    
    private float storedWattTicks;
    private WattStorable.OvervoltBehavior overvoltBehavior;

    protected WattBattery(T parent, int maxStoredWattTicks, int maxCharge, int maxDischarge, Voltage maxTolerance, Voltage maxFlux, OvervoltBehavior overvoltBehavior, Consumer<Integer> overvoltEvent) {

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
     * @param <T> {@link DirectionalWattStorable DirectionalWattStorable} Handler for what happens
     * during energy updates and capability provisions. 
     * 
     * @param parent Should handle reading, writing, sending packets, etc.
     * @return A new fluent builder for defining the resulting WattBattery object.
     */
    public static <T extends DirectionalWattStorable> WattBatteryBuilder<T> newBatteryAt(T parent) {
        return new WattBatteryBuilder<T>(parent);
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
    public WattUnit extractWatts(float maxWattsToExtract, boolean simulate) {
        
        if(maxWattsToExtract == 0) return WattUnit.EMPTY;
        if(!canExtract()) return WattUnit.EMPTY;

        float wattsExtracted  = Math.min(storedWattTicks, Math.min(maxWattsToExtract, maxDischarge));
        if(!simulate) storedWattTicks -= wattsExtracted;
        return WattUnit.of(maxFlux, wattsExtracted);
    }

    @Override
    public WattUnit recieveWatts(WattUnit maxWattsToRecieve, boolean simulate) {
        
        if(maxWattsToRecieve.getCurrent() < 0.001) return WattUnit.EMPTY;
        if(!canReceive()) return WattUnit.EMPTY;

        float wattsReceived = 0;
        int volts = maxWattsToRecieve.getVoltage().get();

        if(volts < maxTolerance.get()) {
            wattsReceived = Math.min((float)maxStoredWattTicks - storedWattTicks, Math.min(maxWattsToRecieve.getWatts(), maxCharge));
        }


        //if(overvoltBehavior == OvervoltBehavior.HARD_LIMIT)

        return WattUnit.EMPTY;
    }

    @Override
    public float getStoredWatts() {
        return storedWattTicks;
    }

    @Override
    public int getCapacity() {
        return maxStoredWattTicks;
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
}
