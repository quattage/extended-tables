package com.quattage.mechano.foundation.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoRenderTypes;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientEdge;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Pair;

import java.util.HashSet;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class WireAnchorBlockRenderer<T extends WireAnchorBlockEntity> implements BlockEntityRenderer<T> {

    private static Entity renderSubject = null;
    private static BlockEntityRenderDispatcher cachedDispatcher;
    private static Minecraft instance = null;

    private static Vec3 oldToPos = new Vec3(0, 0, 0);

    private static final HashSet<AnchorPoint> nearbyAnchors = new HashSet<AnchorPoint>();
    private static AnchorPoint selectedAnchor = null;
    private static GridClientCache cache = null;

    private static float time = 0;

    public WireAnchorBlockRenderer(BlockEntityRendererProvider.Context context) {
        super();
        identifyRenderer(context.getBlockEntityRenderDispatcher());
    }

    @Override
    public void render(T be, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int light,
            int overlay) {

        if(!be.hasLevel() || be.getBlockState().getBlock() == Blocks.AIR) return;

        double delta = ((WireAnchorBlockEntity)be).getDelta(System.nanoTime());

        identifyRenderer(cachedDispatcher);
        if(renderSubject instanceof Player player) {
            showNearbyAnchorPoints(player, be, delta);
            showWireProgress(player, be, partialTicks, matrixStack, bufferSource, delta);
        }

        if(cache == null) cache = GridClientCache.ofInstance();
        else showWigglyWires(be, partialTicks, cache, matrixStack, bufferSource);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return false;
    }

    private void showWigglyWires(T be, float pTicks, GridClientCache cache, PoseStack matrixStack, MultiBufferSource bufferSource) {
        
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

    private void showWireProgress(Player player, T be, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource, double delta) {

        if(time < 1) time += delta * 0.0001f;
        else time = 0;

        // world sanity checks
        if(player == null) return;
        Level world = player.level();
        if(!world.isClientSide()) return;
        if(!instance.options.getCameraType().isFirstPerson()) return;

        // spool sanity checks
        ItemStack spool = WireSpool.getHeldSpool(player);
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
        
        if(selectedAnchor != null && AnchorPoint.getAnchorAt(world, selectedAnchor.getID()) == null)
            selectedAnchor = null;

        if(selectedAnchor != null && !selectedAnchor.equals(targetAnchor.getFirst())) {
            isAnchored = true;
            toPos = oldToPos.lerp(selectedAnchor.getPos(), pTicks * delta * 0.1);
        }
        else if(instance.hitResult instanceof BlockHitResult hit)
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

        WireModelRenderer.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, 1, 15, 4, 15, !isAnchored, (int)((Math.sin(time * 3.1) * 89f) + 60f));
        matrixStack.popPose();

        oldToPos = toPos;
    }

    private boolean isValidPair(Pair<?, ?> pair) {
        if(pair == null) return false;
        if(pair.getFirst() == null) return false;
        if(pair.getSecond() == null) return false;
        return true;
    }

    private void showNearbyAnchorPoints(Player player, T be, double delta) {

        float distance = (float)player.position().distanceTo(be.getBlockPos().getCenter());
        Vec3 raycastPos = VectorHelper.getLookingRay(player, 10).getLocation();
        AnchorPointBank<?> anchorBank = be.getAnchorBank();

        for(AnchorPoint anchor : anchorBank.getAll()) {
            
            if(anchor == null) continue;
            if(anchor.getSize() > 0 || isHoldingSpool(player) && distance < 11) {

                double dist = anchor.getDistanceToRaycast(player.getEyePosition(), raycastPos);
                if(dist < 0.8) nearbyAnchors.add(anchor);
                else {
                    nearbyAnchors.remove(anchor);
                    if(anchor.equals(selectedAnchor)) selectedAnchor = null;
                }

                if(isHoldingSpool(player)) {
                    if(anchor.equals(selectedAnchor))
                        anchor.increaseToSize(MechanoSettings.ANCHOR_SELECT_SIZE, delta * 0.2f);
                    else anchor.decreaseToSize(MechanoSettings.ANCHOR_NORMAL_SIZE, delta * 0.2f);
                } else anchor.decreaseToSize(0, delta * 0.2f);
            } else anchor.decreaseToSize(0, delta * 0.2f);

            if(anchor.getSize() > 0) {
                AABB anchorBox = anchor.getHitbox();

                int size = (int)anchor.getSize();

                if(anchorBox == null ) continue;
                CreateClient.OUTLINER.showAABB(anchor.hashCode(), anchorBox)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
                    .colored(anchor.getColor())
                    .lineWidth(size < 25 ? size * 0.0006f : size * 0.001f);
            }
        }

        double closestDist = -1;
        for(AnchorPoint near : nearbyAnchors) {
            if(near == null) continue;
            double dist = near.getDistanceToRaycast(player.getEyePosition(), raycastPos);
            if(closestDist == -1 || dist < closestDist) {
                closestDist = dist;
                selectedAnchor = near;
            }
        }

        // if(isHoldingSpool(player))
            // Mechano.logSlow("ID: " + (selectedAnchor == null ? "null" : selectedAnchor.getID()), 1000);
    }

    @Nullable
    public static AnchorPoint getSelectedAnchor() {
        return selectedAnchor;
    }

    public static void resetOldPos(Player player, AnchorPoint anchor) {
        oldToPos = player.getOnPos().getCenter().lerp(anchor.getPos(), 0.5);
    }

    public boolean isHoldingSpool(Player player) {
        return player.getMainHandItem().getItem() instanceof WireSpool ||
            player.getOffhandItem().getItem() instanceof WireSpool;
    }

    public void identifyRenderer(BlockEntityRenderDispatcher dispatcher) {
        if(dispatcher == null) return;
        cachedDispatcher = dispatcher;
        if(dispatcher.camera == null) return;
        renderSubject = dispatcher.camera.getEntity();
        instance = Minecraft.getInstance();
    }
}
