package com.quattage.mechano.foundation.electricity.core.watt;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.core.watt.unit.Voltage;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Provides the basic framework for expressing points of "energy" as a product of both potential (volts) and
 * current (amps) - shares the same purpose as Forge's own <code>IEnergyStorage</code> interface.
 */
public interface WattStorable {

    
    /**
    * Adds watts to the energy store and returns a WattUnit representing the energy recieved.
    * @param maxReceive Maximum amount of watts to be inserted.
    * @param voltage Voltage of the incoming watt-ticks to be inserted
    * @param simulate If TRUE, the insertion will only be simulated.
    * @return Amount of watt-ticks that were (or would have been, if simulated) accepted by the storage.
    */
    WattUnit receiveWatts(WattUnit maxWattsToRecieve, boolean simulate);

    /**
    * Removes watt-ticks from the energy store and returns a WattUnit representing the energy removed.
    * @param maxWattsExtract Maximum amount of watt-ticks to be extracted.
    * @param simulate If TRUE, the extraction will only be simulated.
    * @return Amount of watt-ticks that were (or would have been, if simulated) extracted from the storage.
    */
    WattUnit extractWatts(WattUnit maxWattsToExtract, boolean simulate);

    /**
     * Replaces the current amount of energy in the energy store with the WattUnit provided.
     * Used to override the amount of energy in this store without needing an explicit source.
     * @param watts Watts to set this energy store to.
     * @param update <code>TRUE</code> if the energy store should update as a result of this call, as if it received energy normally.
     */
    void setStoredWatts(float watts, boolean update);

    /**
     * Returns the amount of watt-ticks currently in the energy store.
     */
    float getStoredWatts();

    /**
     * Returns the maximum amount of watts the energy store can receive in one tick
     */
    int getMaxCharge();

    /**
     * Returns the maximum amount of watts the energy store can provide in one tick
     */
    int getMaxDischarge();

    /**
     * @return the maximum amount of watt-hours that can be stored.
     */
    int getCapacity();

    /**
     * If <code>FALSE</code>, calls to {@link WattStorable#extractWatts(float, short, boolean) extractWatts()} will return zero.
     * @return <code>TRUE</code> if this energy store can have watt-ticks extracted from it
     */
    boolean canExtract();

    /**
     * If <code>FALSE</code>, calls to {@link WattStorable#receiveWatts(float, short, boolean) receiveWatts()} will return zero.
     * @return <code>TRUE</code> if this energy store can receive watt-ticks.
     */
    boolean canReceive();

    /**
     * The flux of this energy store. Flux describes the potential of this energy store.
     * An energy store of 120 volts of flux will always tend towards an output of 120 volts if possible.
     * @return Voltage value representing this energy store's flux.
     */
    Voltage getFlux();

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

    CompoundTag writeTo(CompoundTag in);
    void readFrom(CompoundTag in);

    /**
     * Converts this WattStorable into an {@link com.quattage.mechano.foundation.electricity.core.watt.ExplicitFeConverter <code>ExplicitEnergyConverter</code>}
     * describing this energy store's Watt value as FE.
     * @param <T> Inferred generic
     * @return An inferred generic LazyOptional containing an <code>ExplicitFEConverter</code> object - This object should reflect up-to-date information
     * about this energy store, converted (as best you can) into FE.
     */
    <T> LazyOptional<T> getFeConverterLazy();
    ExplicitFeConverter<WattStorable> newFeConverter();

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
