
package com.quattage.mechano.foundation.electricity;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSounds;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.helper.builder.WattBatteryHandlerBuilder;
import com.simibubi.create.AllSoundEvents.SoundEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * It is required that any class which instantiates and uses a WattBatteryHandler 
 * implement the WattBatteryHandlable interface
 */
public interface WattBatteryHandlable {

    /**
     * Typically called in an implementing class's constructor to initiate a fluent chain in a subclass. 
     * Used for building {@link com.quattage.mechano.foundation.electricity.}
     * @param builder
     */
    void createWattHandlerDefinition(WattBatteryHandlerBuilder<? extends WattBatteryHandlable> builder);

    /**
     * This method is optionally overridable and provided to expose additional functionality where needed, the WattHandler already
     * sends packets and syncs BEs, so that does not need to be done here.
     */
    default void onWattsUpdated() {}

    /**
     * Called by the Block to reflect a BlockState change, whether that be by a moving contraption or with a wrench. Used to update 
     * the sided WattStorable capabilities of the WattHandler. 
     * @param state
     */
    abstract void reOrient(BlockState state);


    abstract WattBatteryHandler<? extends WattBatteryHandlable> getWattBatteryHandler();

    /**
     * Sets the default mode of an implementing energy store.
     * To be called by implementing Block classes to
     * automatically set the mode depending on surrounding conditions.
     */
    public static void setDefaultMode(Level world, BlockPos pos) {
        if(!(world.getBlockEntity(pos) instanceof WattBatteryHandlable wbh)) return;
        wbh.getWattBatteryHandler().setMode(wbh.getWattBatteryHandler().getInteractionStatus());
    }

    public static InteractionResult cycleModeOnRightClick(Level world, Player player, BlockPos pos) {
        
        if(!player.isCrouching()) return InteractionResult.PASS;
        if(world.getBlockEntity(pos) instanceof WireAnchorBlockEntity wabe) {
            wabe.resetChevronRotation();
            wabe.battery.cycleMode();
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Called BEFORE the mode is changed, only by the logical client.
     * Useful for setting things up to be passed to the renderer for 
     * implementing BlockEntities
     */
    abstract void onAwaitingModeChange();

    public enum ExternalInteractMode {
        NONE(false, false, MechanoSounds.CONNECTOR_CLICK_DENY),
        PUSH_OUT(true, false, MechanoSounds.CONNECTOR_CLICK_UP), // SENDS ENERGY (pushes energy out to external blocks)
        PULL_IN(false, true, MechanoSounds.CONNECTOR_CLICK_DOWN), // RECEIVES ENERGY (has energy pushed in from external blocks)
        BOTH(true, true, null);

        private final boolean canExtract;
        private final boolean canReceive;

        @Nullable
        private final SoundEntry transitionSound;

        private ExternalInteractMode(boolean canExtract, boolean canReceive, SoundEntry transitionSound) {
            this.canExtract = canExtract;
            this.canReceive = canReceive;
            this.transitionSound = transitionSound;
        }

        public boolean canInteract() {
            return canExtract || canReceive;
        }

        public boolean canExtract() {
            return canExtract;
        }

        public boolean canReceive() {
            return canReceive;
        }

        public ExternalInteractMode next() {
            return values()[(this.ordinal() + 1) % 4];
        }

        public void playSound(Level world, BlockPos pos) {
            if(transitionSound != null) transitionSound.playOnServer(world, pos, 0.4f, 1);
        }

        public boolean isCompatableWith(ExternalInteractMode other) {
            if(this == NONE || other == NONE) return false;
            if(this == BOTH || other == BOTH) return true;
            return this == PULL_IN && other == PUSH_OUT;
        }
    }
}
