package com.quattage.mechano.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.quattage.mechano.foundation.block.BlockChangeListenable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Level.class)

public abstract class BlockUpdateMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = {@At(value = "TAIL")}, cancellable = false, remap = false)
    private void onSetBlock(BlockPos pPos, BlockState pNewState, int pFlags, int pRecursionLeft, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) BlockState arg1, @Local(ordinal = 1) BlockState arg2) {
        if(arg2.getBlock() != arg1.getBlock()) {
            if(arg2.getBlock() instanceof BlockChangeListenable bcl)
                bcl.onBlockBroken((Level)(Object)this, pPos, arg1, arg2); 
            if(arg1.getBlock() instanceof BlockChangeListenable bcl2)
                bcl2.onBlockPlaced((Level)(Object)this, pPos, arg2, arg1); 
        }
    }
}