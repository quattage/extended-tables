package com.quattage.mechano.foundation.electricity.grid.sync;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridEdgeUpdateSyncS2CPacket implements Packetable {
    
    private final GridSyncPacketType type;
    private final GridClientEdge edge;

    public GridEdgeUpdateSyncS2CPacket(GridSyncPacketType type, GridClientEdge edge) {
        this.type = type;
        this.edge = edge;
    }

    public GridEdgeUpdateSyncS2CPacket(FriendlyByteBuf buf) {
        this.type = GridSyncPacketType.get(buf.readInt());
        this.edge = new GridClientEdge(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(type.ordinal());
        edge.toBytes(buf);
    }

    
    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            switch(type) {
                case ADD_NEW:
                    GridClientCache.ofInstance().addToQueue(edge, true);
                    break;
                case ADD_WORLD:
                    GridClientCache.ofInstance().addToQueue(edge, false);
                    break;
                case REMOVE:
                    GridClientCache.ofInstance().removeFromQueue(edge);
                    break;
                default:
                    break;
            }
        });
        return true;
    }
}