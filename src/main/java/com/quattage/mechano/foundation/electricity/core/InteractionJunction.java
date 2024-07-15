package com.quattage.mechano.foundation.electricity.core;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.OptionalWattOrFE;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;

/***
 * An InteractionJunction is a "rule" describing
 * what blocks, and in what direction, Watts can be
 * transmitted to/from a parent BlockEntity.
 */
public class InteractionJunction {
    
    private final RelativeDirection dir;
    public boolean isInput;
    public boolean isOutput;

    // when false, the interactions list is a blacklist. 
    // when true, the interactions list is a whitelist
    private final boolean denyOrAllow;
    private final Block[] interactions;

    /***
     * Create a new InteractionJunction at the given RelativeDirection.
     * @param dir RelativeDirection 
     * @param isInput True if this InteractionJunction can accept energy from external sources
     * @param isOutput True if this InteractionJunction can send energy to external sources
     * @param interactions An array representing a list of blocks to consider
     * @param denyOrAllow True if the interactions list is a whitelist, or false if the interactions list is a blacklist.
     */
    public InteractionJunction(RelativeDirection dir, boolean isInput, boolean isOutput, Block[] interactions, boolean denyOrAllow) {
        this.dir = dir;
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.interactions = interactions;
        this.denyOrAllow = denyOrAllow;
    }

    /***
     * Create a new InteractionJunction at the given RelativeDirection.
     * @param dir RelativeDirection
     * @param isInput True if this InteractionJunction can accept energy from external sources
     * @param isOutput True if this InteractionJunction can send energy to external sources
     */
    public InteractionJunction(RelativeDirection dir, boolean isInput, boolean isOutput) {
        this.dir = dir;
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.interactions = null;
        this.denyOrAllow = false;
    }

    /***
     * Create a new InteractionJunction at the given RelativeDirection. <p>
     * This policy has no exceptions, and will always interact.
     * @param dir
     */
    public InteractionJunction(RelativeDirection dir) {
        this.dir = dir;
        this.isInput = true;
        this.isOutput = true;
        this.interactions = null;
        this.denyOrAllow = false;
    }

    /***
     * Create a new InteractionJunction at the given RelativeDirection. <p>
     * This policy has no exceptions, and will always interact.
     * @param dir
     */
    public InteractionJunction(Relative rel) {
        this.dir = new RelativeDirection(rel);
        this.isInput = true;
        this.isOutput = true;
        this.interactions = null;
        this.denyOrAllow = false;
    }

    public Direction getDirection() {
        return dir.get();
    }

    public InteractionJunction rotateToFace(CombinedOrientation orient) {
        dir.rotate(orient);
        return this;
    }

    public boolean canRecieveFrom(Block block) {
        if(!isInput) return false;
        return isBlockAllowed(block);
    }

    public boolean canSendTo(Block block) {
        if(!isOutput) return false;
        return isBlockAllowed(block);
    }

    public boolean isBlockAllowed(Block block) {

        if(interactions == null) return true;
        boolean hasBlock = false;
        for(Block b : interactions)
            if(b.equals(block)) hasBlock = true;

        return denyOrAllow ? hasBlock : !hasBlock;
    }

    /***
     * Determines whether or not this InteractionJunction
     * is interacting with any external energy capabilities in the world.
     * @param parent BlockEntity to use as a reference for getting real-world positions
     * @return True if this InteractionJunction is facing toward
     * a block which provides ForgeEnergy or Watt capabilities in the opposing direction
     */
    public ExternalInteractStatus getInteractionStatusTowards(BlockEntity parent) {

        if((!isInput && !isOutput) || parent.getLevel() == null) 
            return ExternalInteractStatus.NONE;
        OptionalWattOrFE battery = getConnectedCapability(parent);

        if(battery.getFECap() != null)
            return battery.getFECap().getEnergyStored() > 0 ? ExternalInteractStatus.HAS_POWER : ExternalInteractStatus.HAS_EMPTY;
        if(battery.getWattCap() != null) //TODO figure out if this is wrong or not
            return battery.getWattCap().getStoredWatts() > 0 ? ExternalInteractStatus.HAS_POWER : ExternalInteractStatus.HAS_EMPTY;;

        return ExternalInteractStatus.NONE;
    }

    /**
     * Determines whether or not this InteractionJunction
     * can send power to any external energy capabilities in the world.
     * @param parent BlockEntity to use as a reference for getting real-world positions
     * @return True if this InteractionJunction is facing towards
     * a block which provides ForgeEnergy or Watt capabilities in the opposing direction
     */
    public boolean canSend(BlockEntity parent) {
        
        if((!isInput && !isOutput) || parent.getLevel() == null) 
            return false;
        OptionalWattOrFE battery = getConnectedCapability(parent);

        if(battery.isFE())
            return battery.getFECap().canReceive();

        if(battery.isWatt())
            return battery.getWattCap().canReceive();

        return false;
    }

    // public float getSendRate(BlockEntity parent, WattStorable source) {
    //     if((!isInput && !isOutput) || parent.getLevel() == null) 
    //         return 0;

    //     OptionalWattOrFE destination = getConnectedCapability(parent);
    //     if(destination.isFE()) {
    //         return destination.getFECap().receiveEnergy(source.toRealFeEquivalent().extractEnergy(Integer.MAX_VALUE, true), true);
    //     }

    //     return destination.getWattCap().receiveWatts(source.extractWatts(WattUnit.INFINITY, true), true).getWatts();
    // }

    public OptionalWattOrFE getConnectedCapability(BlockEntity parent) {
        return DirectionalWattProvidable.getFEOrWattsAt(
                parent.getLevel().getBlockEntity(
                parent.getBlockPos().relative(getDirection())), 
                getDirection().getOpposite()
            );
    }

    public boolean equals(Object other) {
        if(other instanceof InteractionJunction ip) 
            return dir.equals(ip.dir) && 
                this.isInput == ip.isInput && 
                this.isOutput == ip.isOutput;
        return false;
    }

    public int hashCode() {
        return dir.getRaw().ordinal() + (isInput ? 10 : 11) + (isOutput ? 10 : 11) * 31;
    }

    public enum ExternalInteractStatus {
        NONE(ExternalInteractMode.NONE),
        HAS_EMPTY(ExternalInteractMode.PUSH_OUT),
        HAS_POWER(ExternalInteractMode.PULL_IN);

        private final ExternalInteractMode cooresponding;

        private ExternalInteractStatus(ExternalInteractMode cooresponding) {
            this.cooresponding = cooresponding;
        }

        public boolean isInteracting() {
            return this != NONE;
        }

        public ExternalInteractMode toCoorespondingMode() {
            return cooresponding;
        }
    }
}
