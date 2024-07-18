package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.MechanoClient;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Suppresses Create's standard GoggleOverlayRenderer when 
 * an AnchorPoint is targeted, which prevents it fron rendering
 * at the same time as Mechano's
 */
@Mixin(GoggleOverlayRenderer.class)
public abstract class GoggleOverlayRendererMixin {

    @Shadow(remap = false)
    private static int hoverTicks;

    @Shadow(remap = false)
    private static BlockPos lastHovered;
    
    @Inject(method = "renderOverlay", at = {@At(value = "HEAD")}, cancellable = true, remap = false)
    private static void mechano_suppressGoggleOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height, CallbackInfo info) {

        if(hoverTicks == 0)
            MechanoClient.ANCHOR_SELECTOR.tenaciousTerriblyTemporaryTickingTracker.reset();

        if(MechanoClient.ANCHOR_SELECTOR.getSelectedEntry() != null) {
            hoverTicks = 0;
            info.cancel();
        }
    }
}
