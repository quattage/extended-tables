package com.quattage.mechano.foundation.mixin;

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

@Mixin(Level.class)
public abstract class BlockUpdateMixin implements BlockPreUpdatable{

    @Unique private BlockState mechano_preUpdateBlockState = null;
    @Unique private BlockChangeListenable mechano_preUpdateBCL = null;

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "HEAD"), cancellable = false)
    private void mechano_preOnSetBlock(BlockPos pos, BlockState destinedState, int pFlags, int pRecursionLeft, CallbackInfoReturnable<Boolean> cir) {

        Level world = (Level)(Object)this;
        mechano_preUpdateBlockState = world.getBlockState(pos);

        if(mechano_preUpdateBlockState != null && destinedState.getBlock() != mechano_preUpdateBlockState.getBlock()) {

            mechano_preUpdateBCL = BlockChangeListenable.get(mechano_preUpdateBlockState);
            BlockChangeListenable destinedBCL = BlockChangeListenable.get(destinedState);

            if(mechano_preUpdateBCL != null)
                mechano_preUpdateBCL.onBeforeBlockBroken(world, pos, mechano_preUpdateBlockState, destinedState);
            if(destinedBCL != null)
                destinedBCL.onBeforeBlockPlaced(world, pos, mechano_preUpdateBlockState, destinedState);
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

        Level world = (Level)(Object)this;
        BlockState oldBlockState = ((BlockPreUpdatable)(world)).mechano_getPreState();

        if(oldBlockState != null && oldBlockState.getBlock() != newBlockState.getBlock()) {

            BlockChangeListenable thenBCL = ((BlockPreUpdatable)(world)).mechano_getPreBCL();
            if(thenBCL != null)
                thenBCL.onAfterBlockBroken(world, pos, oldBlockState, newBlockState);

            BlockChangeListenable nowBCL = BlockChangeListenable.get(newBlockState);
            if(nowBCL != null)
                nowBCL.onAfterBlockPlaced(world, pos, oldBlockState, newBlockState);
        }

        ((BlockPreUpdatable)(world)).mechano_resetPreUpdate();
    }
}