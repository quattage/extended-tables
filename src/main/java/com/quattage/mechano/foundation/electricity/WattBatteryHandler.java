package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable;
import com.quattage.mechano.foundation.electricity.core.InteractionJunction;
import com.quattage.mechano.foundation.electricity.core.InteractionJunction.ExternalInteractStatus;
import com.quattage.mechano.foundation.electricity.core.watt.WattBatteryBuilder.WattBatteryUnbuilt;
import com.quattage.mechano.foundation.electricity.core.watt.WattSendSummary;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnitConversions;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.helper.NullSortedArray;
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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/***
 * The <code>WattBatteryHandler</code> is an encapsulting object which provides <code>WattStorable</code> functionality to handling classes.
 * Can be thought of as a "transfer layer" between the energy capability provided by a {@link com.quattage.mechano.foundation.electricity.core.watt.WattStorable <code>WattStorable</code>} object, and the BlockEntity itself.
 */
public class WattBatteryHandler<T extends SmartBlockEntity & WattBatteryHandlable> implements DirectionalWattProvidable {

    @Nullable

    private ExternalInteractMode mode = ExternalInteractMode.BOTH;
    private boolean canChangeMode = true;

    private final InteractionJunction[] interactions;

    private final WattStorable battery;
    private final T target;
    private LazyOptional<WattStorable> energyHandler = LazyOptional.empty();

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
     * If <code>TRUE</code>, this WattBatteryHandler can receive watts. Offers more specificity than {@link WattStorable#canReceive()},
     * as this method will return false if this WattBatteryHandler's {@link ExternalInteractMode} disables energy insertion.
     * @return <code>TRUE</code> if this WattBatteryHandler is in the position to receive watts from external sources
     */
    public boolean shouldReceiveExternally() {
        if(!isInReceiveMode()) return false;
        return battery.canReceive();
    }

    /**
     * If <code>TRUE</code>, this WattBatteryHandler can have watts extracted. Offers more specificity than {@link WattStorable#canReceive()},
     * as this method will return false if this WattBatteryHandler's {@link ExternalInteractMode} disables energy extraction.
     * @return <code>TRUE</code> if this WattBatteryHandler is in the position to have watts extracted by external sourrces
     */
    public boolean shouldExtractExternally() {
        if(!isInExtractMode()) return false;
        return battery.canExtract();
    }

    /**
     * Evenly distributes internal stored Watts to adjacently connected blocks
     */
    public void tickWatts() {

        if(!shouldExtractExternally()) return;

        final List<OptionalWattOrFE> adjacents = new ArrayList<>();
        for(InteractionJunction inter : interactions) {
            OptionalWattOrFE opt = DirectionalWattProvidable.getFEOrWattsAt(target, inter.getDirection());
            if(opt.isPresent()) adjacents.add(opt);
        }

        distributeEnergyTo(adjacents);
    }
    
    /**
     * Emits energy to the destination handlers contained within an input set.
     * Prioritizes even distribution of energy to all destinations whenever possible.
     * @param batteries Set of <code>OptionalWattOrFe</code> objects to distribute between
     */
    public void distributeEnergyTo(final List<OptionalWattOrFE> batteries) {

        if(batteries.isEmpty()) return;

        float totalDemand = 0;
        final float[] demands = new float[batteries.size()];

        int x = 0;
        for(OptionalWattOrFE acceptorOpt : batteries) {
            if(acceptorOpt.getFECap() != null) {
                demands[x] = WattUnitConversions.toWattsSimple(acceptorOpt.getFECap().receiveEnergy(Integer.MAX_VALUE, true));
                totalDemand += demands[x];
            } else if(acceptorOpt.getWattCap() != null) {
                demands[x] = acceptorOpt.getWattCap().receiveWatts(WattUnit.INFINITY, true).getWatts();
                totalDemand += demands[x];
            } x++;
        }

        if(totalDemand == 0) return;
        // Gets the total power to distribute by pretending that all batteries are one big battery 
        final float wattsToDistribute = Math.min(Math.min(battery.getMaxDischarge(), battery.getStoredWatts()), totalDemand);

        x = 0;
        for(OptionalWattOrFE acceptorOpt : batteries) {
            if(demands[x] <= 0) continue;
            
            // Watts added to this iteration's energy store are multiplied by the distribution ratio
            float wattsToAccept = wattsToDistribute * ((float)demands[x] / totalDemand);
            wattsToAccept = Math.min(wattsToAccept, demands[x]);

            if(acceptorOpt.getFECap() != null) {
                acceptorOpt.getFECap().receiveEnergy(WattUnitConversions.toFE(battery.extractWatts(WattUnit.of(battery.getFlux(), wattsToAccept), false)), false);
            } else if(acceptorOpt.getWattCap() != null) {
                acceptorOpt.getWattCap().receiveWatts(battery.extractWatts(WattUnit.of(battery.getFlux(), wattsToAccept), false), false);
            }
            
            if(battery.getStoredWatts() <= 0) break;
            x++;
        }
    }

    /**
     * Emits energy (specifically in this case, only Watts) to the destination handlers contained within a list
     * of {@link WattSendSummary} objects. Used to send energy across traversed paths.
     * Prioritizes even distribution of energy to all destinations evenly whenever possible
     * @param sends List of <code>WattSendSummary</code> objects containing paths to send across.
     */
    public synchronized void distributeWattsTo(final List<WattSendSummary> sends) {

        float totalDemand = 0;
        final float[] demands = new float[sends.size()];

        int x = 0;
        for(WattSendSummary acceptor : sends) {
            float maxPathRate = acceptor.getAddressedPath().getMaxTransferRate();
            if(WattUnit.hasNoPotential(maxPathRate)) 
                demands[x] = 0;
            else {
                demands[x] = acceptor.getDestination().getEnergyHolder().receiveWatts(WattUnit.of(battery.getFlux(), maxPathRate), true).getWatts();
                totalDemand += demands[x];
            }
            x++;
        }

        if(totalDemand == 0) return;
        // Gets the total power to distribute by pretending that all batteries are one big battery 
        final float wattsToDistribute = Math.min(Math.min(battery.getMaxDischarge(), battery.getStoredWatts()), totalDemand);

        x = 0;
        for(WattSendSummary acceptor : sends) {
            if(WattUnit.hasNoPotential(demands[x])) continue;
        
            // Watts added to this iteration's energy store are multiplied by the distribution ratio for even splitting
            float wattsToAccept = Math.min(wattsToDistribute * (demands[x] / totalDemand), demands[x]);

            WattUnit receieved = acceptor.getDestination().getEnergyHolder().receiveWatts(
                battery.extractWatts(
                        WattUnit.of(battery.getFlux(), acceptor.getAddressedPath().getMaxTransferRate())
                            .getLowerStats(
                                WattUnit.of(battery.getFlux().copy(), wattsToAccept)), 
                    false
                ), false
            );

            acceptor.getAddressedPath().addLoad(receieved.copy());
            if(battery.getStoredWatts() <= 0) break;
            x++;
        }
    }

    /**
     * Awards "free" Watts to the specified destination handlers. <p>
     * In this case, by "free" we're simply refering to an arbitrary amount of watts.
     * These watts do not have to be extracted from any pre-existing energy store in order
     * to be properly sent.
     * <p>
     * Prioritizes even distribution of energy to all destinations evenly whenever possible
     * @param sends List of <code>WattSendSummary</code> objects containing paths to send across.
     */
    public static synchronized void awardWattsTo(final WattUnit wattsToAward, final NullSortedArray<WattSendSummary> sends) {

        if(wattsToAward.hasNoPotential()) return;

        float totalDemand = 0;
        final float[] demands = new float[sends.size()];

        for(int x = 0; x < sends.size(); x++) {
            WattSendSummary acceptor = sends.get(x);
            if(acceptor == null)
                break;
            float maxPathRate = acceptor.hasPath() ? acceptor.getAddressedPath().getMaxTransferRate() : Float.MAX_VALUE;
            if(WattUnit.hasNoPotential(maxPathRate)) demands[x] = 0;
            else {
                demands[x] = acceptor.getDestination().getEnergyHolder().receiveWatts(WattUnit.of(wattsToAward.getVoltage(), maxPathRate), true).getWatts();
                totalDemand += demands[x];
            }
        }

        if(totalDemand == 0) return;
        final float wattsToDistribute = Math.min(wattsToAward.getWatts(), totalDemand);

        for(int x = 0; x < sends.size(); x++) {
            WattSendSummary acceptor = sends.get(x);
            if(acceptor == null) break;

            if(WattUnit.hasNoPotential(demands[x])) continue;
        
            // Watts added to this iteration's energy store are multiplied by the distribution ratio for even splitting
            float wattsToAccept = Math.min(wattsToDistribute * (demands[x] / totalDemand), demands[x]);

            WattUnit receieved;
            if(acceptor.hasPath()) {
                receieved = acceptor.getDestination().getEnergyHolder().receiveWatts(WattUnit.of(wattsToAward.getVoltage(), Math.min(wattsToAccept, acceptor.getAddressedPath().getMaxTransferRate())), false);
                acceptor.getAddressedPath().addLoad(receieved.copy());
            } else
                receieved = acceptor.getDestination().getEnergyHolder().receiveWatts(WattUnit.of(wattsToAward.getVoltage(), wattsToAccept), false);
        }
    }

    /**
     * Called every time energy is added or removed from this WattBatteryHandler.
     */
    @Override
    public void onWattsUpdated(float oldStoredWatts, float newStoredWatts) {
        target.onWattsUpdated();
        target.setChanged();
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
    public void loadAndUpdate(BlockState state, boolean updateMode) {
        reflectStateChange(state);
        energyHandler = LazyOptional.of(() -> battery);
        if(updateMode) this.mode = getInteractionStatus().toCoorespondingMode();
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
    public ExternalInteractStatus getInteractionStatus() {
        if(!canInteractDirectly()) return ExternalInteractStatus.NONE;
        for(InteractionJunction pol : interactions) {
            ExternalInteractStatus status = pol.getInteractionStatusTowards(target);
            if(status.isInteracting()) return status;
        }
        return ExternalInteractStatus.NONE;
    }

    /**
     * Gets the raw WattStorable capability backing this WattBatteryHandler
     */
    @Override
    public WattStorable getEnergyHolder() {
        return this.battery;
    }

    public void cycleMode() {
        setMode(mode.next());
    }

    public void forceMode(ExternalInteractMode mode) {
        canChangeMode = true;
        setMode(mode);
        canChangeMode = false;
    }

    public void setMode(ExternalInteractMode mode) {
        if(canChangeMode && this.mode != mode) {
            this.mode = mode;
            if(!(target instanceof WireAnchorBlockEntity wbe)) return;
            MechanoPackets.sendToAllClients(new WattModeSyncS2CPacket(target.getBlockPos(), mode));
            wbe.getAnchorBank().sync(target.getLevel());
            wbe.setChanged();
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
