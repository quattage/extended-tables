package com.quattage.mechano.foundation.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A very basic callback framework for hooking into level changes
 * both <strong>before and after</strong> they happen.
 * 
 * This interface is intended to be implemented by a Block class.
 * You can approximate this behavior with methods in the block class, 
 * but I really, <strong>really</strong> do not like the way that this
 * has to be done. <p>
 * 
 * The methods in this interface are invoked by
 * {@link com.quattage.mechano.foundation.mixin.BlockUpdateMixin BlockUpdateMixin}.
 * Provided this mixin is maintained, this approach is far more reliable for 
 * accurrately detecting block changes regardless of source. For my purposes,
 * the traditional approach leaves all kinds of edge-cases that will result
 * in broken states and bad BlockEntity data. This is because methods in the
 * Block class (as well as Forge's own BlockEvents) are only called after 
 * blocks are placed/broken specifically and only by a <strong>player</strong>. <p>
 * 
 * This approach does not suffer the same pitfalls. Since the mixin hooks directly
 * into <code>Level.setBlock()</code>, any change, regardless of its cause,
 * will always be detected.
 */
public interface BlockChangeListenable {

    @Nullable
    public static BlockChangeListenable get(BlockState state) {
        if(state.getBlock() instanceof BlockChangeListenable bcl)
            return bcl;
        return null;
    }

    /**
     * Called by both the logical client and server before this block is broken
     * <p>
     * Called <strong>BEFORE</strong> the modification is pushed to the world.
     * Does not get called by chunk load/unload events.
     * @param world World to operate within
     * @param pos BlockPos of block broken
     * @param currentState BlockState of what is present at the time of the invocation.
     * * @param futureState What currentState will be after this call, assuming the event isn't cancellled elsewhere
     */
    default void onBeforeBlockBroken(Level world, BlockPos pos, BlockState currentState, BlockState futureState) {};

    /**
     * Called by both the logical client and server before this block is placed.
     * <p>
     * Called <strong>BEFORE</strong> the modification is pushed to the world.
     * Does not get called by chunk load/unload events.
     * @param world World to operate within
     * @param pos BlockPos of block placed
     * @param currentState BlockState of what is present at the time of the invocation
     * @param futureState What currentState will be after this call, assuming the event isn't cancellled elsewhere
     */
    default void onBeforeBlockPlaced(Level world, BlockPos pos, BlockState currentState, BlockState futureState) {};
    
    
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
    default void onAfterBlockBroken(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {};

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
    default void onAfterBlockPlaced(Level world, BlockPos pos, BlockState pastState, BlockState currentState) {};

    public interface BlockPreUpdatable {
        abstract BlockState mechano_getPreState();
        abstract BlockChangeListenable mechano_getPreBCL();
        abstract void mechano_resetPreUpdate();
    }
}