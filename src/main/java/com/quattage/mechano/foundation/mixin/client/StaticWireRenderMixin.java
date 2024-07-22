package com.quattage.mechano.foundation.mixin.client;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.quattage.mechano.foundation.electricity.grid.GridClientCache;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;


/**
 * This mixin manually injects Mechano's wire rendering workflow directly into Minecraft's chunk meshing system.
 * I was surprised to find that forge just doesn't have a native way to do this - this way of doing things is very hacky and brittle.
 * With Embeddium present, {@link com.quattage.mechano.foundation.compat.embeddium.EmbeddiumWireInjector EmbeddiumWireInjector}
 * is used instead, which is much more straightforward and much less prone to breaking.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public abstract class StaticWireRenderMixin {

    @SuppressWarnings("target") @Shadow(aliases = {"this$1", "f_112859_"}) 
    private ChunkRenderDispatcher.RenderChunk this$1;
    private Set<RenderType> mechano$chunkRenderTypes;

    @ModifyVariable(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	public Set<RenderType> getRenderLayers(Set<RenderType> set) {
		this.mechano$chunkRenderTypes = set;
		return set;
	}

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    public void injectEdgeRendering(float pX, float pY, float pZ, ChunkBufferBuilderPack buffer, CallbackInfoReturnable<?> cir) {
        GridClientCache.ofInstance().renderConnectionsInChunk(this$1, mechano$chunkRenderTypes, buffer, this$1.getOrigin());
		this.mechano$chunkRenderTypes = null;
	}
}
