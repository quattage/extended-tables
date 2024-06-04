package com.quattage.mechano.foundation.electricity.grid.sync;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridPathUpdateSyncS2CPacket implements Packetable {
    
    private final WattUnit rate;
    private final int pathSize;
    private final GridSyncPacketType type;
    @Nullable private final GID[] path;
    

    public GridPathUpdateSyncS2CPacket(@Nullable GridPath gPath, GridSyncPacketType type) {
        if(gPath == null) {
            this.rate = WattUnit.EMPTY;
            this.pathSize = -1;
            this.type = type;
            this.path = null;
        } else {
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
    }

    public GridPathUpdateSyncS2CPacket(FriendlyByteBuf buf) {
        this.rate = WattUnit.ofBytes(buf);
        this.pathSize = buf.readInt();
        this.type = GridSyncPacketType.get(buf.readInt());
        if(pathSize < 0) this.path = null;
        else {
            this.path = new GID[pathSize];
            for(int x = 0; x < pathSize; x++)
                this.path[x] = new GID(buf.readBlockPos(), buf.readInt());
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        rate.toBytes(buf);
        buf.writeInt(pathSize);
        buf.writeInt(type.ordinal());
        if(path == null) return;
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
                    if(path == null) throw new NullPointerException("Error handling GridPathUpdateS2CPacket - Type 'ADD_NEW' does not support a null path!");
                    GridClientCache.ofInstance().markValidPath(path);
                    break;
                case REMOVE:
                if(path == null) throw new NullPointerException("Error handling GridPathUpdateS2CPacket - Type 'REMOVE' does not support a null path!");
                    GridClientCache.ofInstance().unmarkPath(path);
                    break;
                case CLEAR:
                    GridClientCache.ofInstance().clearAll();
                    break;
                default:
                    break;
            }
        });
        return true;
    }
}