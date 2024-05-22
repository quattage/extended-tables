package com.quattage.mechano;

import com.quattage.mechano.foundation.network.Packetable;
import com.quattage.mechano.content.block.power.alternator.rotor.AlternatorUpdateS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.sync.GridEdgeUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.sync.GridPathUpdateS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.sync.GridVertDestroySyncS2CPacket;
import com.quattage.mechano.foundation.network.AnchorRefreshS2CPacket;
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
    }

    public static void registerPackets() {  

        //S2C
        NETWORK.messageBuilder(WattSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(WattSyncS2CPacket::new)
            .encoder(WattSyncS2CPacket::toBytes)
            .consumerMainThread(WattSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridVertDestroySyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridVertDestroySyncS2CPacket::new)
            .encoder(GridVertDestroySyncS2CPacket::toBytes)
            .consumerMainThread(GridVertDestroySyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridEdgeUpdateSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridEdgeUpdateSyncS2CPacket::new)
            .encoder(GridEdgeUpdateSyncS2CPacket::toBytes)
            .consumerMainThread(GridEdgeUpdateSyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridPathUpdateS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridPathUpdateS2CPacket::new)
            .encoder(GridPathUpdateS2CPacket::toBytes)
            .consumerMainThread(GridPathUpdateS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(AlternatorUpdateS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(AlternatorUpdateS2CPacket::new)
            .encoder(AlternatorUpdateS2CPacket::toBytes)
            .consumerMainThread(AlternatorUpdateS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(AnchorRefreshS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(AnchorRefreshS2CPacket::new)
            .encoder(AnchorRefreshS2CPacket::toBytes)
            .consumerMainThread(AnchorRefreshS2CPacket::handle)
            .add();

        //C2S
        NETWORK.messageBuilder(AnchorSelectC2SPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(AnchorSelectC2SPacket::new)
            .encoder(AnchorSelectC2SPacket::toBytes)
            .consumerMainThread(AnchorSelectC2SPacket::handle)
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
