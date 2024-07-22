package com.quattage.mechano.foundation.compat.embeddium;

import java.util.List;

import org.embeddedt.embeddium.api.ChunkMeshEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.WireSpool;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.rendering.WireModelRenderer;
import com.quattage.mechano.foundation.electricity.rendering.WireModelRenderer.BakedModelHashKey;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * In vanilla, this injection code is usually done by {@link com.quattage.mechano.foundation.mixin.client.StaticWireRenderMixin StaticWireRenderMixin},
 * but Vanilla's chunk meshing tasks are completely skipped with Embeddium present. Fortunately, embeddium has a convenient callback specifically for this purpose.
 * In NeoForge, this event is built directly into the loader itself and is supported natively without any Embeddium-specific implementation required.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class EmbeddiumWireInjector {

    @SubscribeEvent
    public static void onEmbeddiumMeshInject(ChunkMeshEvent event) {

        // this bit of code here ensures that this event doesn't damage the integrity of Embeddium's optimization measures, 
        // since Embeddium skips empty sections unless they have an appender present. (TODO <-- I THINK, BUT I MIGHT BE WRONG AND IM NOT ENTURELY SURE THIS IS NECESSARY IN THIS CONTEXT)
        if(event.getWorld() == null) return;
        LevelChunk chunk = event.getWorld().getChunk(event.getSectionOrigin().x(), event.getSectionOrigin().z());
        LevelChunkSection verticalSlice = chunk.getSection(chunk.getSectionIndexFromSectionY(event.getSectionOrigin().y()));
        if(verticalSlice == null || verticalSlice.hasOnlyAir()) return;
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        final GridClientCache wires = GridClientCache.of(event.getWorld());
        List<GridClientEdge> edges = wires.getEdgeCache().get(event.getSectionOrigin());

        if(edges == null || edges.isEmpty()) return;

        event.addMeshAppender(context -> {

            SectionPos s = context.sectionOrigin();
            BlockAndTintGetter accessor = context.blockRenderView();
            VertexConsumer builder = context.vertexConsumerProvider().apply(RenderType.cutout());

            for(int x = 0; x < edges.size(); x++) {

                GridClientEdge edge = edges.get(x);

                if(edge.getAge() > 0) continue;
                Pair<AnchorPoint, WireAnchorBlockEntity> fromAnchor = 
                    AnchorPoint.getAnchorAt(accessor, edge.getSideA());
                if(fromAnchor == null || fromAnchor.getFirst() == null) 
                    continue;

                Pair<AnchorPoint, WireAnchorBlockEntity> toAnchor = 
                    AnchorPoint.getAnchorAt(accessor, edge.getSideB());
                if(toAnchor == null || toAnchor.getFirst() == null) 
                    continue;

                PoseStack matrixStack = new PoseStack();
                matrixStack.pushPose();

                BlockPos chunkCorner = fromAnchor.getSecond().getBlockPos().subtract(
                    new BlockPos(s.minBlockX(), s.minBlockY(), s.minBlockZ()));

                Vec3 startOffset = fromAnchor.getFirst().getLocalOffset();
                matrixStack.translate(
                    chunkCorner.getX() + startOffset.x, 
                    chunkCorner.getY() + startOffset.y, 
                    chunkCorner.getZ() + startOffset.z
                );

                Vec3 startPos = fromAnchor.getFirst().getPos();
                Vec3 endPos = toAnchor.getFirst().getPos();
                Vector3f wireOrigin = new Vector3f(
                    (float)(endPos.x - startPos.x), 
                    (float)(endPos.y - startPos.y), 
                    (float)(endPos.z - startPos.z)
                );

                float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
                matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

                int[] lightmap = WireModelRenderer.deriveLightmap(accessor, startPos, endPos);
                WireModelRenderer.INSTANCE.renderStatic(
                    new BakedModelHashKey(startPos, endPos), 
                    builder, matrixStack, wireOrigin, 
                    lightmap[0], lightmap[1], lightmap[2], lightmap[3], 
                    WireSpool.ofType(edge.getTypeID()).getWireSprite()
                );

                matrixStack.popPose();
            }
        });
    }
}
