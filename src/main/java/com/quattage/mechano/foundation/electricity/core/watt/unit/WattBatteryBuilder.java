package com.quattage.mechano.foundation.electricity.core.watt.unit;

import java.util.function.Consumer;

import com.quattage.mechano.foundation.electricity.core.DirectionalWattStorable;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable.OvervoltBehavior;

/**
 * Fluent builder for {@link com.quattage.mechano.foundation.electricity.core.watt.unit.WattBattery <code>WattBattery</code>} objects.
 */
public class WattBatteryBuilder<T extends DirectionalWattStorable> {
    
    private final T parent;
    public WattBatteryBuilder(T parent) {
        this.parent = parent;
    }

    /**
     * A battery's flux (EMF, Electromagnetic Flux) describes the voltage of electricity
     * that it can supply when current is pulled.
     * <p>
     * A flux value of 120 volts means that energy coming out of this battery
     * will always be 120 volts.
     */
    public BB2 withFlux(int volts) {
        return new BB2(parent, volts);
    }

    public class BB2 {
        private final T parentBE;
        private final Voltage volts;
        private BB2(T parentBE, int volts) {
            this.parentBE = parentBE;
            this.volts = new Voltage(volts);
        }

        /**
         * Sets the maximum incoming voltage that this battery can handle
         */
        public BB3 withVoltageTolerance(int voltsIn) {
            return new BB3(parentBE, volts, voltsIn);
        }
    }

    public class BB3 {
        private final T parentBE;
        private final Voltage voltsIn;
        private final Voltage voltsOut;
        private int maxCharge = Integer.MAX_VALUE;

        private BB3(T parentBE, Voltage voltsIn, int voltsOut) {
            this.parentBE = parentBE;
            this.voltsIn = voltsIn;
            this.voltsOut = new Voltage(voltsOut);
        }

        /**
         * Sets the maximum incoming watt-ticks this battery can consume.
         * Determines the amount of nominal amps this battery will pull at 
         * maximum working capacity. As in real life, a battery should only
         * pull as many amps as it demands to do work.
         * @param maxCharge Watt-ticks of charge. 
         * @return
         */
        public BB3 withMaxCharge(int maxCharge) {
            this.maxCharge = maxCharge;
            return this;
        }

        /**
         * Sets the maximum outgoing watt-ticks that this battery can provide.
         * For those familiar, this can be thought of as an approximation of a battery's C rating.
         * @param maxDischarge Watt-ticks of discharge, or -1 for Integer.MAX_VALUE (unlimited discharge)
         */
        public BB4 withMaxDischarge(int maxDischarge) {
            if(maxDischarge < 0)
                return new BB4(parentBE, voltsIn, voltsOut, maxCharge, Integer.MAX_VALUE);
            return new BB4(parentBE, voltsIn, voltsOut, maxCharge, maxDischarge);
        }
    }

    public class BB4 {
        private final T parentBE;
        private final Voltage voltsIn;
        private final Voltage voltsOut;
        private final int maxCharge;
        private final int maxDischarge;
        
        private BB4(T parentBE, Voltage voltsIn, Voltage voltsOut, int maxCharge, int maxDischarge) {
            this.parentBE = parentBE;
            this.voltsIn = voltsIn;
            this.voltsOut = voltsOut;
            this.maxCharge = maxCharge;
            this.maxDischarge = maxDischarge;
        }

        /**
         * Sets the maximum total watt-ticks that this battery can hold.
         * @param cap Capacity of this battery, or -1 for Integer.MAX_VALUE (unlimited capacity)
         */
        public BB5 withCapacity(int cap) {
            if(cap < 0)
                return new BB5(parentBE, voltsOut, voltsIn, Integer.MAX_VALUE, maxCharge, maxDischarge);
            return new BB5(parentBE, voltsOut, voltsIn, cap, maxCharge, maxDischarge);
        }
    }

    public class BB5 {
        private final T parentBE;
        private final Voltage voltsEMF;
        private final Voltage voltsIn;
        private final int maxCharge;
        private final int maxDischarge;
        private final int cap;
        
        private BB5(T parentBE, Voltage voltsEMF, Voltage voltsIn, int cap, int maxCharge, int maxDischarge) {
            this.parentBE = parentBE;
            this.voltsEMF = voltsEMF;
            this.voltsIn = voltsIn;
            this.cap = cap;
            this.maxCharge = maxCharge;
            this.maxDischarge = maxDischarge;
        }

        /**
         * <li><code>SOFT_DENY:</code> The incoming watt-tick is simply denied, nothing happens, this energy store receives nothing.
         * <li><code>HARD_LIMIT:</code> The incoming watt-tick is chopped off at the maximum voltage tolerance value, energy is lost.
         * <li><code>TRANSFORM_IMPLICIT:</code> The incoming watt-tick is automatically converted and recieved in full by this energy store.
         * @param behavior {@link com.quattage.mechano.foundation.electricity.core.watt.WattStorable.OvervoltBehavior <code>See here</code>}
         */
        public BB6 withOvervoltBehavior(OvervoltBehavior behavior) {
            return new BB6(parentBE, voltsEMF, voltsIn, cap, behavior, maxCharge, maxDischarge);
        }
    }

    public class BB6 {
        private final T parentBE;
        private final Voltage voltsEMF;
        private final Voltage voltsIn;
        private final int maxStored;
        private final int maxCharge;
        private final int maxDischarge;
        private final OvervoltBehavior behavior;

        private BB6(T parentBE, Voltage voltsOut, Voltage voltsIn, int cap, OvervoltBehavior behavior, int maxCharge, int maxDischarge) {
            this.parentBE = parentBE;
            this.voltsEMF = voltsOut;
            this.voltsIn = voltsIn;
            this.maxStored = cap;
            this.behavior = behavior;
            this.maxCharge = maxCharge;
            this.maxDischarge = maxDischarge;
        }

        /**
         * Make a new WattBattery Object with no additional functionality
         * @return A new WattBattery object
         */
        public WattBattery<T> make() {
            return new WattBattery<T>(parentBE, maxStored, maxCharge, maxDischarge, voltsIn, voltsEMF, behavior, null);
        }

        /**
         * Make a new WattBattery object and attach a consumer. The consumer will be invoked
         * whenever the WattBattery recieves too much voltage.
         * @param consumer A functional interface 
         * @return A new WattBattery object
         */
        public WattBattery<T> makeWithOvervoltEvent(Consumer<Integer> consumer) {
            return new WattBattery<T>(parentBE, maxStored, maxCharge, maxDischarge, voltsIn, voltsEMF, behavior, consumer);
        }
    }
}
