package com.quattage.mechano.foundation.mixin;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.BlockChangeListenable;
import com.quattage.mechano.foundation.block.BlockChangeListenable.BlockPreUpdatable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;


/**
 * This mixin bypasses the need to override Block#onPlace on relevent blocks,
 * replacing its functionality with the methods found in 
 * {@link com.quattage.mechano.foundation.block.BlockChangeListenable BlockChangeListenable}
 */
@Mixin(Level.class)
public abstract class BlockUpdateMixin implements BlockPreUpdatable {

    @Unique private BlockState mechano_preUpdateBlockState = null;
    @Unique private BlockChangeListenable mechano_preUpdateBCL = null;

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "HEAD"), cancellable = false)
    private void mechano_preOnSetBlock(BlockPos pos, BlockState destinedState, int pFlags, int pRecursionLeft, CallbackInfoReturnable<Boolean> cir) {

        if(pFlags == 2) return;

        Level world = (Level)(Object)this;
        mechano_preUpdateBlockState = world.getBlockState(pos);

        if(mechano_preUpdateBlockState != null && !destinedState.equals(mechano_preUpdateBlockState)) {

            mechano_preUpdateBCL = BlockChangeListenable.get(mechano_preUpdateBlockState);
            BlockChangeListenable destinedBCL = BlockChangeListenable.get(destinedState);

            if(mechano_preUpdateBCL != null) {
                try {
                    mechano_preUpdateBCL.onBeforeBlockBroken(world, pos, mechano_preUpdateBlockState, destinedState);
                } catch (Exception e) {
                    Mechano.LOGGER.error("Exception encountered processing pre-break block change: " + ExceptionUtils.getStackTrace(e));
                }
            }
            if(destinedBCL != null) {
                try {
                    destinedBCL.onBeforeBlockPlaced(world, pos, mechano_preUpdateBlockState, destinedState);
                } catch (Exception e) {
                    Mechano.LOGGER.error("Exception encountered processing pre-place block change: " + ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    @Override
    public BlockState mechano_getPreState() {
        return mechano_preUpdateBlockState;
    }

    @Override
    public BlockChangeListenable mechano_getPreBCL() {
        return mechano_preUpdateBCL;
    }

    @Override
    public void mechano_resetPreUpdate() {
        mechano_preUpdateBlockState = null;
        mechano_preUpdateBCL = null;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "TAIL"), cancellable = false)
    private void mechano_postOnSetBlock(BlockPos pos, BlockState newBlockState, int pFlags, int pRecursionLeft, CallbackInfoReturnable<Boolean> cir) {

        if(pFlags == 2) return;
        Level world = (Level)(Object)this;
        BlockState oldBlockState = ((BlockPreUpdatable)(world)).mechano_getPreState();

        if(oldBlockState != null && !oldBlockState.equals(newBlockState)) {

            BlockChangeListenable thenBCL = ((BlockPreUpdatable)(world)).mechano_getPreBCL();
            if(thenBCL != null) {
                try {
                    thenBCL.onAfterBlockBroken(world, pos, oldBlockState, newBlockState);
                } catch (Exception e) {
                    Mechano.LOGGER.error("Exception encountered processing post-break block change: " + ExceptionUtils.getStackTrace(e));
                }
            }

            BlockChangeListenable nowBCL = BlockChangeListenable.get(newBlockState);
            if(nowBCL != null) {
                try {
                    nowBCL.onAfterBlockPlaced(world, pos, oldBlockState, newBlockState);
                } catch (Exception e) {
                    Mechano.LOGGER.error("Exception encountered processing post-place block change: " + ExceptionUtils.getStackTrace(e));
                }
            }
        }

        ((BlockPreUpdatable)(world)).mechano_resetPreUpdate();
    }
}