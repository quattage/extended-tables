package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorEntry;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorVertexData;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

public class AnchorStatRequestC2SPacket implements Packetable {

    private final GID target;

    public AnchorStatRequestC2SPacket(AnchorEntry entry) {
        if(entry == null) throw new NullPointerException("Error creating AnchorStatRequest packet - AnchorEntry is null!");
        this.target = entry.get().getID();
    }

    public AnchorStatRequestC2SPacket(FriendlyByteBuf buf) {
        this.target = new GID(
            buf.readBlockPos(),
            buf.readInt()
        );
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(target.getBlockPos());
        buf.writeInt(target.getSubIndex());
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            if(!(context.getSender().level() instanceof ServerLevel world)) return;
            
            if(!(world.getBlockEntity(target.getBlockPos()) instanceof WireAnchorBlockEntity wbe)) return;

            AnchorPoint serverAnchor = wbe.getAnchorBank().get(target.getSubIndex());
        
            GridVertex participant = serverAnchor.getOrFindParticipantIn(wbe.getLevel());
            AnchorVertexData data = participant == null ? 
                data = AnchorVertexData.ofEmpty(target) : new AnchorVertexData(participant);
            
            MechanoPackets.sendToClient(new AnchorVertexDataSyncS2CPacket(data), context.getSender());
        });
        return true;
    }
}