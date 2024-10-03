package com.quattage.mechano;

import com.quattage.mechano.foundation.network.Packetable;
import com.quattage.mechano.foundation.network.WattModeSyncS2CPacket;
import com.quattage.mechano.foundation.network.GridPathViewMaskS2CPacket;
import com.quattage.mechano.foundation.network.AnchorStatRequestC2SPacket;
import com.quattage.mechano.foundation.network.AnchorVertexDataSyncS2CPacket;
import com.quattage.mechano.content.block.power.alternator.rotor.ARSetS2CPacket;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingUpdateS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.network.GridEdgeUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.network.GridPathUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.network.GridVertUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.network.AnchorSelectC2SPacket;
import com.quattage.mechano.foundation.network.WattSyncS2CPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class MechanoPackets {

    private static SimpleChannel NETWORK;
    private static int packetId = 0;

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
            .named(Mechano.asResource("packets"))
            .networkProtocolVersion(() -> "0.1")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();
        
        NETWORK = net;

        registerPackets();
        Mechano.logReg("packets");
    }

    public static void registerPackets() {  

        //S2C
        NETWORK.messageBuilder(WattSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(WattSyncS2CPacket::new)
            .encoder(WattSyncS2CPacket::toBytes)
            .consumerMainThread(WattSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(WattModeSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(WattModeSyncS2CPacket::new)
            .encoder(WattModeSyncS2CPacket::toBytes)
            .consumerMainThread(WattModeSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridVertUpdateSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridVertUpdateSyncS2CPacket::new)
            .encoder(GridVertUpdateSyncS2CPacket::toBytes)
            .consumerMainThread(GridVertUpdateSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridEdgeUpdateSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridEdgeUpdateSyncS2CPacket::new)
            .encoder(GridEdgeUpdateSyncS2CPacket::toBytes)
            .consumerMainThread(GridEdgeUpdateSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridPathUpdateSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridPathUpdateSyncS2CPacket::new)
            .encoder(GridPathUpdateSyncS2CPacket::toBytes)
            .consumerMainThread(GridPathUpdateSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(AnchorStatRequestC2SPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(AnchorStatRequestC2SPacket::new)
            .encoder(AnchorStatRequestC2SPacket::toBytes)
            .consumerMainThread(AnchorStatRequestC2SPacket::handle)
            .add();

        NETWORK.messageBuilder(AnchorVertexDataSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(AnchorVertexDataSyncS2CPacket::new)
            .encoder(AnchorVertexDataSyncS2CPacket::toBytes)
            .consumerMainThread(AnchorVertexDataSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridPathViewMaskS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridPathViewMaskS2CPacket::new)
            .encoder(GridPathViewMaskS2CPacket::toBytes)
            .consumerMainThread(GridPathViewMaskS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(SlipRingUpdateS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SlipRingUpdateS2CPacket::new)
            .encoder(SlipRingUpdateS2CPacket::toBytes)
            .consumerMainThread(SlipRingUpdateS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(ARSetS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(ARSetS2CPacket::new)
            .encoder(ARSetS2CPacket::toBytes)
            .consumerMainThread(ARSetS2CPacket::handle)
            .add();


        //C2S
        NETWORK.messageBuilder(AnchorSelectC2SPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(AnchorSelectC2SPacket::new)
            .encoder(AnchorSelectC2SPacket::toBytes)
            .consumerMainThread(AnchorSelectC2SPacket::handle)
            .add();
        
        NETWORK.messageBuilder(AnchorStatRequestC2SPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(AnchorStatRequestC2SPacket::new)
            .encoder(AnchorStatRequestC2SPacket::toBytes)
            .consumerMainThread(AnchorStatRequestC2SPacket::handle)
            .add();
    }

    public static <T extends Packetable> void sendToServer(T message) {
        if(message == null) return;
        NETWORK.sendToServer(message);
    }

    public static <T extends Packetable> void sendToClient(T message, ServerPlayer recipient) {
        if(message == null) return;
        NETWORK.send(PacketDistributor.PLAYER.with(() -> recipient), message);
    }

    public static <T extends Packetable> void sendToAllClients(T message) {
        if(message == null) return;
        NETWORK.send(PacketDistributor.ALL.noArg(), message);
    }

    private static int nextId() {
        return packetId++;
    }
}
