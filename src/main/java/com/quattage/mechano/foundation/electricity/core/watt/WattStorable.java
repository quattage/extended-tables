package com.quattage.mechano.foundation.electricity.core.watt;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;

public interface WattStorable {

    
    /**
    * Adds watts to the energy store and returns a WattUnit representing the energy recieved.
    * @param maxReceive Maximum amount of watts to be inserted.
    * @param voltage Voltage of the incoming watt-ticks to be inserted
    * @param simulate If TRUE, the insertion will only be simulated.
    * @return Amount of watt-ticks that were (or would have been, if simulated) accepted by the storage.
    */
    WattUnit recieveWatts(WattUnit maxWattsToRecieve, boolean simulate);

    /**
    * Removes watt-ticks from the energy store and returns a WattUnit representing the energy removed.
    * @param maxWattsExtract Maximum amount of watt-ticks to be extracted.
    * @param simulate If TRUE, the extraction will only be simulated.
    * @return Amount of watt-ticks that were (or would have been, if simulated) extracted from the storage.
    */
    WattUnit extractWatts(float maxWattsToExtract, boolean simulate);

    /**
     * Returns the amount of watt-ticks currently in the energy store.
     */
    float getStoredWatts();

    /**
     * @return the maximum amount of watt-hours that can be stored.
     */
    int getCapacity();

    /**
     * If <code>FALSE</code>, calls to {@link WattStorable#recieveWatts(float, short, boolean) receiveWatts()} will return zero.
     * @return <code>TRUE</code> if this energy store can have watt-ticks extracted from it
     */
    boolean canExtract();

    /**
     * If <code>FALSE</code>, calls to {@link WattStorable#extractWatts(float, short, boolean) extractWatts()} will return zero.
     * @return <code>TRUE</code> if this energy store can receive watt-ticks.
     */
    boolean canReceive();

    /**
     * <li><code>SOFT_DENY:</code> The incoming watt-tick is simply denied, nothing happens, this energy store receives nothing.
     * <li><code>HARD_LIMIT:</code> The incoming watt-tick is chopped off at the maximum voltage tolerance value, energy is lost.
     * Implementations can make use of whatever sort of efficiency losses they'd like to here.
     * <li><code>TRANSFORM_IMPLICIT:</code> The incoming watt-tick is automatically converted (as if this energy store has a 
     * built-in voltage transformer) and recieved in full by this energy store.
     * <p>
     * A good rule of thumb, even if it isn't generally needed for your use case, is to implement all 3 modes. It is a
     * designed feature of this interface that the OvervoltBehavior is expected to change at runtime. 
     * 
     * @return an OvervoltBehavior describing how this energy store should react when 
     * it receives energy at a voltage greater than it is designed to handle.
     */
    @Nullable
    OvervoltBehavior getOvervoltBehavior();

    /**
     * Sets the OvervoltBehavior in this energy store. 
     */
    void setOvervoltBehavior(OvervoltBehavior overvoltBehavior);


    /**
     * An OvervoltBehavior describes how this energy store should react when 
     * it receives energy at a voltage greater than it is capable of handling.
     * 
     * <li><code>SOFT_DENY:</code> The incoming watt-tick is simply denied, nothing happens, this energy store receives nothing.
     * <li><code>LIMIT_LOSSY:</code> The incoming watt-tick is chopped off at the maximum voltage tolerance value, energy is lost.
     * Implementations can make use of whatever sort of efficiency losses they'd like to here.
     * <li><code>TRANSFORM_LOSSLESS:</code> The incoming watt-tick is automatically converted (as if this energy store has a 
     * built-in voltage transformer) and recieved in full by this energy store.
     * <p>
     * Most general implementations should expect to accommodate all three modes, assuming the mode can change at runtime.
     */
    public static enum OvervoltBehavior {
        SOFT_DENY,
        LIMIT_LOSSY,
        TRANSFORM_LOSSLESS,
    }

    void onOvervolt(int excessVolts);
}
