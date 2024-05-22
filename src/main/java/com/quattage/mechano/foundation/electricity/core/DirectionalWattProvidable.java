package com.quattage.mechano.foundation.electricity.core;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/***
 * Provides implementing classes with the groundwork required to attach sided {@link com.quattage.mechano.foundation.electricity.core.watt.WattStorable <code>Watt</code>} capabilities to
 * a parent BlockEntity. 
 */
public interface DirectionalWattProvidable {

    /***
     * Called by implementations whenever a stored watt value is changed
     * Implementations should mark block entities changed and probably
     * handle S2C packets for syncing.
     */
    public abstract void onWattsUpdated(float oldStoredWatts, float newStoredWatts);

    /***
     * @return the WattStorable object backing this handler
     */
    public abstract WattStorable getBattery();

    /***
     * Expects a WattStorable object belonging to the implementing handler. 
     * Should be cast to an inferred generic.
     * @param <T> Inferred generic - can be acquired using <code>LazyOptional.cast()</code>
     * @return Non-Null LazyOptional (you can use <code>LazyOptional.empty()</code> if the capability is effectively null)
     */
    public abstract <T> @NotNull LazyOptional<T> getEnergyHandler();

    /***
     * Provides energy capabilities for the given side of the parent block.
     * More specifically, it compares the given side with a list of valid sides.
     * This list of valid sides should be stored by your BlockEntity or whatever
     * object is implementing this interface. <p>
     * 
     * The capability returned will always be pulled from the return value of {@link DirectionalWattProvidable#getEnergyHandler() <code>getEnergyHandler().</code>}
     * If the expected capability is of Forge's <code>IEnergyStorage</code> type, a LazyOptional contianing an {@link com.quattage.mechano.foundation.electricity.core.watt.ExplicitFeConverter <code>ExplicitFeConverter</code>}
     * will be returned instead, in order to allow automatic conversion between FE and Watts.
     * @param <T> A Generic LazyOptional 
     * @param cap The capability in question.
     * @param side Side to check, if <code>null</code> will always return <code>LazyOptional.empty()</code>
     * @param energyDirs A list of all interaction directions the implementing
     * object can accommodate, or <code>null</code> to ignore this step.
     * @return Infered generic LazyOptional containing a <code>WattStorable or IEnergyStorage</code> capability.
     */
    default <T> @NotNull LazyOptional<T> getCapabilityForSide(@NotNull Capability<T> cap, 
        @Nullable Direction side, @Nullable Direction[] energyDirs) {

        if(side == null) return LazyOptional.empty();
        if(energyDirs.length >= 6) {
            return getOrFake(cap);
        }
        for(int x = 0; x < energyDirs.length; x++) 
            if(side.equals(energyDirs[x])) return getOrFake(cap);

        return LazyOptional.empty();
    }

    private <T> @NotNull LazyOptional <T> getOrFake(Capability<T> cap) { 
        if(cap == ForgeCapabilities.ENERGY) {
            if(getEnergyHandler().orElse(null) instanceof WattStorable batt)
                return batt.toFeEquivalent();
            else return LazyOptional.empty();
        }
        if(cap == Mechano.CAPABILITIES.WATT_CAPABILITY) return getEnergyHandler();
        return LazyOptional.empty();
    }

    /**
     * Gets an OptionalWattOrFE object which stores both ForgeEnergy and Mechano Watt capabilities. 
     * Will return an OptionalWattOrFE object, regardless of whether or not the BlockEntity has any capabilities. 
     * See {@link DirectionalWattProvidable.OptionalWattOrFE <code>OptionalWattOrFE</code>} for more info.
     * @param targetBE BlockEntity to get the capabilities of
     * @param dir Direction the capability faces
     * @return OptionalWattOrFE object, regardless of targetBE's capability content. 
     */
    static OptionalWattOrFE getFEOrWattsAt(BlockEntity targetBE, @Nullable Direction dir) {
        if(targetBE == null) return new OptionalWattOrFE(null, null);
        return new OptionalWattOrFE(targetBE.getCapability(ForgeCapabilities.ENERGY, dir).orElse(null), targetBE.getCapability(Mechano.CAPABILITIES.WATT_CAPABILITY).orElse(null));
    }

    /**
     * An intermediary object that stores both FE and Watt capabilities
     * in order to retrieve them from the world in a sort of generic way.
     */
    public class OptionalWattOrFE {
        final IEnergyStorage feBattery;
        final WattStorable wattBattery;

        public OptionalWattOrFE(@Nullable IEnergyStorage feBattery, @Nullable WattStorable wattBattery) {
            this.feBattery = feBattery;
            this.wattBattery = wattBattery;
        }

        public boolean isPresent() {
            return isFE() || isWatt();
        }

        public boolean isFE() {
            return feBattery != null && wattBattery == null;
        }

        public boolean isWatt() {
            return feBattery == null && wattBattery != null;
        }

        public WattStorable getWattCap() {
            return wattBattery;
        }

        public IEnergyStorage getFECap() {
            return feBattery;
        }
    }
}
