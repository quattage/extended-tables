package com.quattage.mechano.foundation.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

@Mixin(KineticNetwork.class)
public abstract class KineticNetworkMixin {

    @Shadow private Map<KineticBlockEntity, Float> members;
    
    @Inject(method = "getActualStressOf", at = {@At(value = "HEAD")}, cancellable = true)
    private void getActualStressOf(KineticBlockEntity be, CallbackInfoReturnable<Float> cir) {
        if(be instanceof AbstractRotorBlockEntity arbe) {
            cir.setReturnValue(members.get(be) * arbe.getWeightedSpeed());
        }
    }
}
