package com.quattage.mechano.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A very basic callback framework for hooking into level changes
 * <strong>after they happen.</strong> <p>
 * Intended to be implemented by a Block class.
 * <p>
 * You can approximate this behavior with methods in the block class, 
 * but I really, <strong>really</strong> do not like the way that this
 * has to be done, so I made my own. This code is invoked by {@link com.quattage.mechano.foundation.mixin.BlockUpdateMixin BlockUpdateMixin}
 */
public interface BlockChangeListenable {

    /**
     * Called by both the logical client and server before this block is broken, 
     * whether that be by a player, an explosion, or the level.
     * <p>
     * Called <strong>BEFORE</strong> the modification is pushed to the world.
     * Does not get called by chunk load/unload events.
     * @param world World to operate within
     * @param pos BlockPos of block broken
     * @param currentState BlockState of what is present at the time of the invocation.
     * * @param destinedState What currentState will be after this call, assuming the event isn't cancellled elsewhere
     */
    default void onBeforeBlockBroken(Level world, BlockPos pos, BlockState currentState, BlockState destinedState) {};

    /**
     * Called by both the logical client and server before this block is placed.
     * whether that be by a player or any other entity (like an Enderman)
     * <p>
     * Called <strong>BEFORE</strong> the modification is pushed to the world.
     * Does not get called by chunk load/unload events.
     * @param world World to operate within
     * @param pos BlockPos of block placed
     * @param currentState BlockState of what is present at the time of the invocation
     * @param destinedState What currentState will be after this call, assuming the event isn't cancellled elsewhere
     */
    default void onBeforeBlockPlaced(Level world, BlockPos pos, BlockState currentState, BlockState destinedState) {};
    
    
    /**
     * Called by both the logical client and server whenever this block is broken, 
     * whether that be by a player, an explosion, or the level.
     * <p>
     * Called <strong>AFTER</strong> the modification is pushed to the world.
     * Does not get called by chunk load/unload events.
     * @param world World to operate within
     * @param pos BlockPos of block placed
     * @param state BlockState of block broken
     * @param pastState BlockState containing what was present before this block was placed (9/10 times, this will contain the implementing block)
     * @param currentState BlockState of what is present at the time of the invocation. (What the block was replaced with - If the player breaks it, this will be air)
     */
    default void onBlockBroken(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {};

    /**
     * Called by both the logical client and server whenever this block is placed.
     * whether that be by a player or any other entity (like an Enderman)
     * <p>
     * Called <strong>AFTER</strong> the modification is pushed to the world.
     * Does not get called by chunk load/unload events.
     * @param world World to operate within
     * @param pos BlockPos of block placed
     * @param pastState BlockState containing what was present before this block was placed (Usually this would just contain air)
     * @param currentState BlockState of what is present at the time of the invocation. (What the block was replaced with - 9/10 times, this will contain the implementing block)
     */
    default void onBlockPlaced(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {};
}