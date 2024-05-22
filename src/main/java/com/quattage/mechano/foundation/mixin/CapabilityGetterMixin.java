package com.quattage.mechano.foundation.mixin;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

@Mixin(CapabilityProvider.class)
public abstract class CapabilityGetterMixin {
    
    @Shadow private boolean valid;
    @Shadow protected abstract @Nullable CapabilityDispatcher getCapabilities();

    @Overwrite //    >:)
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        final CapabilityDispatcher disp = getCapabilities();
        return !valid || disp == null ? LazyOptional.empty() : disp.getCapability(cap, side);
    }
}
