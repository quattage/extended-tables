package com.quattage.mechano.foundation.electricity.grid.sync;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridPathUpdateS2CPacket implements Packetable {
    
    private final WattUnit rate;
    private final int pathSize;
    private final GridSyncPacketType type;
    private final GID[] path;
    

    public GridPathUpdateS2CPacket(GridPath gPath, GridSyncPacketType type) {
        
        this.rate = gPath.getTransferStats();
        this.pathSize = gPath.size();
        this.type = type;
        
        this.path = new GID[pathSize];
        int x = 0;
        for(GridVertex vert : gPath.members()) {
            this.path[x] = vert.getID();
            x++;
        }
    }

    public GridPathUpdateS2CPacket(FriendlyByteBuf buf) {
        this.rate = WattUnit.ofBytes(buf);
        this.pathSize = buf.readInt();
        this.type = GridSyncPacketType.get(buf.readInt());
        this.path = new GID[pathSize];
        for(int x = 0; x < pathSize; x++)
            this.path[x] = new GID(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        rate.toBytes(buf);
        buf.writeInt(pathSize);
        buf.writeInt(type.ordinal());
        for(GID id : path) {
            buf.writeBlockPos(id.getBlockPos());
            buf.writeInt(id.getSubIndex());
        }
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            switch(type) {
                case ADD_NEW:
                    GridClientCache.ofInstance().markValidPath(path);
                    break;
                case REMOVE:
                    GridClientCache.ofInstance().unmarkPath(path);
                    break;
                default:
                    break;
            }
        });
        return true;
    }
}