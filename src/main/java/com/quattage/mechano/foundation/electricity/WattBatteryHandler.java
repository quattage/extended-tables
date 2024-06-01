package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable;
import com.quattage.mechano.foundation.electricity.core.InteractionJunction;
import com.quattage.mechano.foundation.electricity.core.watt.WattBatteryBuilder.WattBatteryUnbuilt;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnitConversions;
import com.quattage.mechano.foundation.network.WattModeSyncS2CPacket;
import com.quattage.mechano.foundation.network.WattSyncS2CPacket;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

/***
 * The <code>WattBatteryHandler</code> is an encapsulting object which provides <code>WattStorable</code> functionality to handling classes.
 * Can be thought of as a "transfer layer" between the energy capability provided by a {@link com.quattage.mechano.foundation.electricity.core.watt.WattStorable <code>WattStorable</code>} object, and the BlockEntity itself.
 */
public class WattBatteryHandler<T extends SmartBlockEntity & WattBatteryHandlable> implements DirectionalWattProvidable {

    @Nullable

    private ExternalInteractMode mode = ExternalInteractMode.BOTH;
    private final InteractionJunction[] interactions;

    public final WattStorable battery;
    private final T target;
    public LazyOptional<WattStorable> energyHandler = LazyOptional.empty();

    public WattBatteryHandler(T target, InteractionJunction[] interactions, WattBatteryUnbuilt unbuilt) {
        this.target = target;
        this.interactions = interactions;
        this.battery = unbuilt.buildAndAttach(this);
    }

    /***
     * @return The world that this WattBatteryHandler's parent 
     * BlockEntity belongs to
     */
    public Level getWorld() {
        return target.getLevel();
    }

    /**
     * @return True if this WattBatteryHandler can send/recieve
     * directly to adjacent blocks
     */
    public boolean canInteractDirectly() {
        return  mode.canInteract() && interactions != null;
    }
    
    /**
     * @return True if this WattBatteryHandler has no stored energy
     */
    public boolean isEmpty() {
        return WattUnit.hasNoPotential(battery.getStoredWatts());
    }

    /**
     * @return True if this WattBatteryHandler is at max energy
     */
    public boolean isFull() {
        return battery.getCapacity() <= battery.getStoredWatts();
    }

    /**
     * Evenly distributes internal stored Watts to adjacently connected blocks
     */
    public void tickWatts() {

        if(!battery.canExtract()) return;

        final Set<OptionalWattOrFE> adjacents = new HashSet<>();

        for(InteractionJunction inter : interactions) {
            OptionalWattOrFE opt = DirectionalWattProvidable.getFEOrWattsAt(target, inter.getDirection());
            if(opt.isPresent()) adjacents.add(opt);
        }

        distributeEnergy(adjacents);
    }
    
    /**
     * Emits energy the destination handlers contained within an nput set.
     * Prioritizes even distribution of energy to all destinations whenever possible
     * @param batteries Set of <code>OptionalWattOrFe</code> objects to distribute to.
     */
    public void distributeEnergy(Set<OptionalWattOrFE> batteries) {

        float totalDemand = 0;
        float[] demands = new float[batteries.size()];

        int x = 0;
        for(OptionalWattOrFE acceptorOpt : batteries) {
            if(acceptorOpt.getFECap() instanceof IEnergyStorage acceptor) {
                demands[x] = WattUnitConversions.toWattsSimple(acceptor.receiveEnergy(Integer.MAX_VALUE, true));
                totalDemand += demands[x];
            } else if(acceptorOpt.getWattCap() instanceof WattStorable acceptor) {
                demands[x] = acceptor.receiveWatts(WattUnit.INFINITY, true).getWatts();
                totalDemand += demands[x];
            } x++;
        }

        if(totalDemand == 0) return;
        // Gets the total power to distribute by sort of pretending that all batteries are one big battery 
        float wattsToDistribute = Math.min(Math.min(battery.getMaxDischarge(), battery.getStoredWatts()), totalDemand);

        x = 0;
        for(OptionalWattOrFE acceptorOpt : batteries) {
            if(demands[x] <= 0) continue;
            
            // Watts added to this iteration's energy store are multiplied by the distribution ratio
            float wattsToAccept = wattsToDistribute * ((float)demands[x] / totalDemand);
            wattsToAccept = Math.min(wattsToAccept, demands[x]);

            if(acceptorOpt.getFECap() instanceof IEnergyStorage acceptor) {
                acceptor.receiveEnergy(WattUnitConversions.toFE(battery.extractWatts(WattUnit.of(battery.getFlux(), wattsToAccept), false)), false);
            } else if(acceptorOpt.getWattCap() instanceof WattStorable acceptor) {
                acceptor.receiveWatts(battery.extractWatts(WattUnit.of(battery.getFlux(), wattsToAccept), false), false);
            }
            
            if(battery.getStoredWatts() <= 0) break;
            x++;
        }
    }

    /***
     * Called every time energy is added or removed from this WattBatteryHandler.
     */
    @Override
    public void onWattsUpdated(float oldStoredWatts, float newStoredWatts) {
        target.setChanged();
        target.onWattsUpdated();
        MechanoPackets.sendToAllClients(WattSyncS2CPacket.ofSource(battery, target.getBlockPos()));
    }

    @Override
    public <R> @NotNull LazyOptional<R> getEnergyHandler() {
        return energyHandler.cast();
    }

    public <R> @NotNull LazyOptional<R> provideEnergyCapabilities(@NotNull Capability<R> cap, @Nullable Direction side) {
        return getCapabilityForSide(cap, side, getInteractionDirections());
    }

    /***
     * Initializes the energy capabilities of this WattBatteryHandler and 
     * reflects a state change if needed
     */
    public void loadAndUpdate(BlockState state) {
        reflectStateChange(state);
        energyHandler = LazyOptional.of(() -> battery);
    }

    /***
     * Invalidates the energy capabilities of this WattBatteryHandler
     */
    public void invalidate() {
        energyHandler.invalidate();
    }

    public WattBatteryHandler<T> reflectStateChange(BlockState state) {
        if(state == null) return this;
        CombinedOrientation target = DirectionTransformer.extract(state);
        for(InteractionJunction interaction : interactions)
            interaction.rotateToFace(target);
        return this;
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putByte("mode", (byte)mode.ordinal());
        return battery.writeTo(in);
    }

    public void readFrom(CompoundTag in) {
        battery.readFrom(in);
        mode = ExternalInteractMode.values()[in.getByte("mode")];
    }

    /***
     * Gets every direction that this WattBatteryHandler can interact with.
     * Directions are relative to the world, and will change
     * depending on the orientation of this WattBatteryHandler's parent block.
     * @return A list of Directions; empty if this WattBatteryHandler
     * has no interaction directions.
     */
    public Direction[] getInteractionDirections() {
        if(interactions == null || interactions.length == 0) return new Direction[0];

        Direction[] out = new Direction[interactions.length];
        for(int x = 0; x < interactions.length; x++) {
            InteractionJunction p = interactions[x];
            if(p.isInput || p.isOutput)
                out[x] = interactions[x].getDirection();
        }

        return out;
    }

    /***
     * Checks whether this WattBatteryHandler is directly connected to any BlockEntities that have ForgeEnergy
     * capabilites.
     * @return <code>TRUE</code> if this WattBatteryHandler is interacting with a WattStorable or IEnergyStorage capability
     */
    public boolean isInteractingExternally() {
        if(!canInteractDirectly()) return false;
        for(InteractionJunction pol : interactions) 
            if(pol.canSendOrReceive(target)) return true;
        return false;
    }

    @Override
    public WattStorable getBattery() {
        return this.battery;
    }

    public void cycleMode() {
        setMode(mode.next());
    }

    public void setMode(ExternalInteractMode mode) {
        if(this.mode != mode) {
            this.mode = mode;
            MechanoPackets.sendToAllClients(new WattModeSyncS2CPacket(target.getBlockPos(), mode));
            if(!(target instanceof WireAnchorBlockEntity wbe)) return;
            wbe.getAnchorBank().sync(target.getLevel());
        }
    }

    public void setModeAsClient(ExternalInteractMode mode) {
        this.mode = mode;
    }

    @Override
    public ExternalInteractMode getMode() {
        return mode;
    }
}
