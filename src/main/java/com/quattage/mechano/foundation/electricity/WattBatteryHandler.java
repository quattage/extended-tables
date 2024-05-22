package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable;
import com.quattage.mechano.foundation.electricity.core.InteractionJunction;
import com.quattage.mechano.foundation.electricity.core.watt.WattBatteryBuilder.WattBatteryUnbuilt;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.network.WattSyncS2CPacket;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/***
 * The WattBatteryHandler is an encapsulting object which provides WattStorable functionality to handling classes.
 * Can be thought of as a "transfer layer" between the energy capability of the WattStorable object, and the BlockEntity itself.
 */
public class WattBatteryHandler<T extends SmartBlockEntity & WattBatteryHandlable> implements DirectionalWattProvidable {

    @Nullable
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

    /***
     * Checks whether this WattBatteryHandler is allowed to 
     * send power to the given block.
     * @param block Block to check
     * @return True if this WattBatteryHandler can send power to
     * the given block.
     */
    public boolean canSendTo(Block block) {
        if(!canInteractDirectly()) return false;
        for(InteractionJunction p : interactions)
            if(p.canSendTo(block)) return true;
        return false;
    }

    /***
     * Checks whether this WattBatteryHandler is allowed to
     * recieve power from the given block.
     * @param block Block to check
     * @return True if this WattBatteryHandler can receive
     * power from the given block.
     */
    public boolean canRecieveFrom(Block block) {
        if(!canInteractDirectly()) return false;
        for(InteractionJunction p : interactions)
            if(p.canRecieveFrom(block)) return true;
        return false;
    }

    /***
     * @return True if this WattBatteryHandler can send/recieve
     * directly to adjacent blocks
     */
    public boolean canInteractDirectly() {
        return interactions != null;
    }
    
    /***
     * @return True if this WattBatteryHandler has no stored energy
     */
    public boolean isEmpty() {
        return WattUnit.hasNoPotential(battery.getStoredWatts());
    }

    /***
     * @return True if this WattBatteryHandler is at max energy
     */
    public boolean isFull() {
        return battery.getCapacity() <= battery.getStoredWatts();
    }

    /***
     * Compares the energy between this WattBatteryHandler and a given
     * WattBatteryHandler.
     * @param other WattBatteryHandler to compare with.
     * @return True if this WattBatteryHandler has more stored energy
     * than the given WattBatteryHandler.
     */
    public boolean hasMoreEnergyThan(WattBatteryHandler<?> other) {
        return this.battery.getStoredWatts() > other.battery.getStoredWatts();
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
        return battery.writeTo(in);
    }

    public void readFrom(CompoundTag in) {
        battery.readFrom(in);
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
     * @return True if this WattBatteryHandler is interacting with a ForgeEnergy BlockEntity
     */
    public boolean isConnectedExternally() {
        for(InteractionJunction pol : interactions) 
            if(pol.canSendOrReceive(target)) return true;
        return false;
    }

    /***
     * Differs from {@link #getInteractionDirections()} <p>
     * Returns InteractionPolicies directly, rather than 
     * summarizing the interactions as a list of Directions.
     * @return
     */
    public InteractionJunction[] getRawInteractions() {
        return interactions;
    }

    @Override
    public WattStorable getBattery() {
        return this.battery;
    }
}
