package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.MechanoClient;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Suppresses Create's standard GoggleOverlayRenderer when 
 * an AnchorPoint is targeted, which prevents it fron rendering
 * at the same time as Mechano's
 */
@Mixin(GoggleOverlayRenderer.class)
public class GoggleOverlayRendererMixin {

    @Shadow 
    private static int hoverTicks;
    
    @Inject(method = "renderOverlay", at = {@At(value = "HEAD")}, cancellable = true)
    private static void renderOverlay(ForgeGui gui, GuiGraphics graphis, float partialTicks, int width, int height, CallbackInfo info) {
        if(MechanoClient.ANCHOR_SELECTOR.getSelectedEntry() != null) {
            hoverTicks = 0;
            info.cancel();
        }
    }
}
