package com.quattage.mechano.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.foundation.block.BlockChangeListenable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Level.class)

public abstract class BlockPreUpdateMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "HEAD"), cancellable = false, remap = false)
    private void onSetBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft, CallbackInfoReturnable<Boolean> cir) {
        BlockState currentState = ((Level)(Object)this).getBlockState(pPos);
        if(currentState != null && currentState.getBlock() != pState.getBlock()) {
            if(currentState.getBlock() instanceof BlockChangeListenable bcl)
                bcl.onBeforeBlockBroken((Level)(Object)this, pPos, currentState, pState); 
            if(pState.getBlock() instanceof BlockChangeListenable bcl2)
                bcl2.onBeforeBlockPlaced((Level)(Object)this, pPos, currentState, pState); 
        }
    }
}