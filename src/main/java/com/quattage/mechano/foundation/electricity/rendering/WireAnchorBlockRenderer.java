package com.quattage.mechano.foundation.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.MechanoRenderTypes;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientEdge;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.simibubi.create.foundation.utility.Pair;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.quattage.mechano.MechanoClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class WireAnchorBlockRenderer<T extends WireAnchorBlockEntity> implements BlockEntityRenderer<T> {

    private static Vec3 oldToPos = new Vec3(0, 0, 0);
    private static GridClientCache cache = null;
    private static float timeTick = 0;

    public WireAnchorBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    @SuppressWarnings("resource")
    public void render(T be, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int light,
            int overlay) {

        if(!be.hasLevel() || be.getBlockState().getBlock() == Blocks.AIR) return;

        drawWireProgress(Minecraft.getInstance().player, 
            be, partialTicks, 
            matrixStack, bufferSource, 
            ((WireAnchorBlockEntity)be).getTimeSinceLastRender()
        );

        MechanoClient.ANCHOR_SELECTOR.tickAnchorsBelongingTo(be);

        if(cache == null) cache = GridClientCache.ofInstance();
        else drawWigglyWires(be, partialTicks, cache, matrixStack, bufferSource);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return false;
    }

    private void drawWigglyWires(T be, float pTicks, GridClientCache cache, PoseStack matrixStack, MultiBufferSource bufferSource) {
        
        for(GridClientEdge edge : cache.getAllNewEdges()) {

            if(!edge.getSideA().getBlockPos().equals(be.getBlockPos())) continue;

            float age = edge.getAge();
            if(age == 0) continue;
            float percent = 1f - (age / (float)edge.getInitialAge());
            float sagOverride = scalarToSag(Mth.lerp(percent, (float)Math.sin(age / 20f) * 5, 10f));

            Pair<AnchorPoint, WireAnchorBlockEntity> fromAnchor = AnchorPoint.getAnchorAt(cache.getWorld(), edge.getSideA());
            if(!isValidPair(fromAnchor)) continue;
        
            Pair<AnchorPoint, WireAnchorBlockEntity> toAnchor = AnchorPoint.getAnchorAt(cache.getWorld(), edge.getSideB());
            if(!isValidPair(toAnchor)) continue;

            Vec3 fromPos = fromAnchor.getFirst().getPos();
            Vec3 fromOffset = fromAnchor.getFirst().getLocalOffset();
            Vec3 toPos = toAnchor.getFirst().getPos();

            matrixStack.pushPose();
            matrixStack.translate(fromOffset.x, fromOffset.y, fromOffset.z);
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WireSpool.ofType(edge.getTypeID()).asResource()));

            Vector3f offset = WireModelRenderer.getWireOffset(fromPos, toPos);
            int[] lightmap = WireModelRenderer.deriveLightmap(cache.getWorld(), fromPos, toPos);

            matrixStack.translate(offset.x(), 0, offset.z());
            
            Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
            Vec3 endPos = toPos.add(-offset.x(), 0, -offset.z());
            Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

            float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
            matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

            WireModelRenderer.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, sagOverride, lightmap[0], lightmap[1], lightmap[2], lightmap[3]);
            matrixStack.popPose();
        }
    }

    // converts a scalar (-1 to 1) to an arbitrary sag value used in the wire rendering pipeline
    private float scalarToSag(float scalar) {
        if(scalar == 0) return 10f;
        return (10 * (1f / Math.abs(scalar))) * Math.signum(scalar);
    }

    @SuppressWarnings("resource")
    private void drawWireProgress(Player player, T be, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource, double delta) {

        if(timeTick < 1) timeTick += delta * 0.0001f;
        else timeTick = 0;

        // world sanity checks
        if(player == null) return;
        Level world = player.level();
        if(!world.isClientSide()) return;
        if(!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        // spool sanity checks
        ItemStack spool = WireSpool.getHeldByPlayer(player);
        if(spool == null) return;
        CompoundTag spoolTag = spool.getOrCreateTag();
        if(!GID.isValidTag(spoolTag)) return;

        GID connectID = GID.of(spoolTag);
        Pair<AnchorPoint, WireAnchorBlockEntity> targetAnchor = AnchorPoint.getAnchorAt(world, connectID);

        // anchor sanity checks
        if(!isValidPair(targetAnchor)) return;
        if(!be.equals(targetAnchor.getSecond())) return;

        Vec3 fromPos = targetAnchor.getFirst().getPos();
        Vec3 fromOffset = targetAnchor.getFirst().getLocalOffset();
        Vec3 toPos;
        boolean isAnchored = false;

        AnchorPoint selectedAnchor = MechanoClient.ANCHOR_SELECTOR.getSelectedAnchor(world);
        if(selectedAnchor != null && !selectedAnchor.equals(targetAnchor.getFirst())) {
            isAnchored = true;
            toPos = oldToPos.lerp(selectedAnchor.getPos(), pTicks * delta * 0.1);
        }
        else if(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)
            toPos = oldToPos.lerp(hit.getBlockPos().relative(hit.getDirection(), 1).getCenter(), pTicks * delta * 0.04);
        else
            toPos = oldToPos;

        matrixStack.pushPose();
        matrixStack.translate(fromOffset.x, fromOffset.y, fromOffset.z);
        VertexConsumer buffer = bufferSource.getBuffer(MechanoRenderTypes.getWireTranslucent(((WireSpool)spool.getItem()).asResource()));

        Vector3f offset = WireModelRenderer.getWireOffset(fromPos, toPos);
        matrixStack.translate(offset.x(), 0, offset.z());
        
        Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = toPos.add(-offset.x(), 0, -offset.z());
        Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

        float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
        matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        WireModelRenderer.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, 1, 15, 4, 15, !isAnchored, (int)((Math.sin(timeTick * 3.1) * 89f) + 60f));
        matrixStack.popPose();

        oldToPos = toPos;
    }

    private boolean isValidPair(Pair<?, ?> pair) {
        if(pair == null) return false;
        if(pair.getFirst() == null) return false;
        if(pair.getSecond() == null) return false;
        return true;
    }

    public static void resetOldPos(Player player, AnchorPoint anchor) {
        oldToPos = player.getOnPos().getCenter().lerp(anchor.getPos(), 0.5);
    }
}
