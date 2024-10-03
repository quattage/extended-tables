package com.quattage.mechano.foundation.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

@Mixin(KineticNetwork.class)
public abstract class KineticNetworkMixin {

    @Shadow(remap = false) 
    private Map<KineticBlockEntity, Float> members;
    
    @Inject(method = "getActualStressOf", at = {@At(value = "HEAD")}, cancellable = true, remap = false)
    private void getActualStressOf(KineticBlockEntity be, CallbackInfoReturnable<Float> cir) {
        if(be instanceof CustomStressable arbe) 
            cir.setReturnValue(members.get(be) * arbe.defineCustomStress());
    }


    /**
     * Implement this interface in instances of KineticBlockEntity to 
     * define custom stress behaviors for the parent KineticNetwork.
     */
    public interface CustomStressable {

        /**
         * Used to define more a more grainular stress number. Normally,
         * stress consumption is determined by multiplying a KineticBlockEntity's 
         * base stress by its current rotation speed. If you want, for example, an exponential
         * stress curve, implement your own math in this method to return the actual stress.
         * @return A float representing the desired stress output given your current BE's stats and whatnot
         */
        abstract float defineCustomStress();
    }
}
