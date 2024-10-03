package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.foundation.block.anchor.AnchorVertexData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class AnchorVertexDataSyncS2CPacket implements Packetable {

    private final AnchorVertexData data;

    public AnchorVertexDataSyncS2CPacket(AnchorVertexData data) {
        this.data = data;
    }

    public AnchorVertexDataSyncS2CPacket(FriendlyByteBuf buf) {
        this.data = new AnchorVertexData(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        data.toBytes(buf);
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleAsClient(data)));
        return true; 
    }


    @OnlyIn(Dist.CLIENT)
    public static void handleAsClient(AnchorVertexData data) {
        MechanoClient.ANCHOR_SELECTOR.hasOutgoingRequest = false;
        MechanoClient.ANCHOR_SELECTOR.setAnchorData(data);
    }
}