package com.quattage.mechano.foundation.electricity.watt;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.watt.WattStorable.OvervoltBehavior;
import com.quattage.mechano.foundation.electricity.watt.WattStorable.OvervoltEvent;
import com.quattage.mechano.foundation.electricity.watt.unit.Voltage;

/**
 * Fluent builder for {@link com.quattage.mechano.foundation.electricity.watt.WattBattery <code>WattBattery</code>} objects.
 */
public class WattBatteryBuilder {
    /**
     * A battery's flux (EMF, Electromagnetic Flux) describes the voltage of electricity
     * that it can supply when current is pulled.
     * <p>
     * A flux value of 120 volts means that energy coming out of this battery
     * will nominally be 120 volts.
     */
    public BB2 withFlux(int voltsOut) {
        return new BB2(voltsOut);
    }

    public class BB2 {

        private final Voltage voltsOut;
        private BB2(int voltsOut) {
            this.voltsOut = new Voltage(voltsOut);
        }

        /**
         * Sets the maximum incoming voltage that this battery can handle
         */
        public BB3 withVoltageTolerance(int voltsIn) {
            return new BB3(voltsOut, voltsIn);
        }
    }

    public class BB3 {

        private final Voltage voltsIn;
        private final Voltage voltsOut;
        private int maxCharge = Integer.MAX_VALUE;

        private BB3(Voltage voltsOut, int voltsIn) {
            this.voltsOut = voltsOut;
            this.voltsIn = new Voltage(voltsIn);
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
                return new BB4(voltsIn, voltsOut, maxCharge, Integer.MAX_VALUE);
            return new BB4(voltsIn, voltsOut, maxCharge, maxDischarge);
        }
    }

    public class BB4 {
        private final Voltage voltsIn;
        private final Voltage voltsOut;
        private final int maxCharge;
        private final int maxDischarge;
        
        private BB4(Voltage voltsIn, Voltage voltsOut, int maxCharge, int maxDischarge) {
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
                return new BB5(voltsOut, voltsIn, Integer.MAX_VALUE, maxCharge, maxDischarge);
            return new BB5(voltsOut, voltsIn, cap, maxCharge, maxDischarge);
        }
    }

    public class BB5 {
        private final Voltage voltsEMF;
        private final Voltage voltsIn;
        private final int maxCharge;
        private final int maxDischarge;
        private final int cap;
        
        private BB5(Voltage voltsEMF, Voltage voltsIn, int cap, int maxCharge, int maxDischarge) {
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
         * @param behavior {@link com.quattage.mechano.foundation.electricity.watt.WattStorable.OvervoltBehavior <code>See here</code>}
         */
        public BBNeedsEvent withIncomingPolicy(@Nullable OvervoltBehavior behavior) {
            return new BBNeedsEvent(voltsEMF, voltsIn, cap, behavior, maxCharge, maxDischarge);
        }
    }

    public class BBNeedsEvent {
        private final Voltage voltsEMF;
        private final Voltage voltsIn;
        private final int maxStored;
        private final int maxCharge;
        private final int maxDischarge;
        private final OvervoltBehavior behavior;

        private BBNeedsEvent(Voltage voltsOut, Voltage voltsIn, int cap, OvervoltBehavior behavior, int maxCharge, int maxDischarge) {
            this.voltsEMF = voltsOut;
            this.voltsIn = voltsIn;
            this.maxStored = cap;
            this.behavior = behavior;
            this.maxCharge = maxCharge;
            this.maxDischarge = maxDischarge;
        }

        /**
         * An OvervoltEvent is a consumer that gets invoked by the WattBattery object whenever it recieves 
         * voltage greater than its voltage tolerance. This consumer can do whatever you want on the BE side of things.
         * @return
         */
        public WattBatteryUnbuilt withNoEvent() {
            return new WattBatteryUnbuilt(voltsEMF, voltsIn, maxStored, behavior, maxCharge, maxDischarge, null);
        }

        /**
         * An OvervoltEvent is a consumer that gets invoked by the WattBattery object whenever it recieves 
         * voltage greater than its voltage tolerance. This consumer can do whatever you want on the BE side of things.
         * @return
         */
        public WattBatteryUnbuilt withOvervoltEvent(Consumer<OvervoltEvent> event) {
            return new WattBatteryUnbuilt(voltsEMF, voltsIn, maxStored, behavior, maxCharge, maxDischarge, event);
        }
    }

    public class WattBatteryUnbuilt {
        private final Voltage voltsEMF;
        private final Voltage voltsIn;
        private final int maxStored;
        private final int maxCharge;
        private final int maxDischarge;
        private final OvervoltBehavior behavior;
        private final Consumer<OvervoltEvent> ove;

        private WattBatteryUnbuilt(Voltage voltsOut, Voltage voltsIn, int cap, OvervoltBehavior behavior, int maxCharge, int maxDischarge, Consumer<OvervoltEvent> ove) {
            this.voltsEMF = voltsOut;
            this.voltsIn = voltsIn;
            this.maxStored = cap;
            this.behavior = behavior;
            this.maxCharge = maxCharge;
            this.maxDischarge = maxDischarge;
            this.ove = ove;
        }

        public <T extends DirectionalWattProvidable> WattStorable buildAndAttach(T parent) {
            return new WattBattery<T>(parent, maxStored, maxCharge, maxDischarge, voltsIn, voltsEMF, behavior, ove);
        }
    }
}
