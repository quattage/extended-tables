package com.quattage.mechano.foundation.electricity.grid.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridPathUpdateSyncS2CPacket implements Packetable {

    private final float lowestWatts;
    private final int pathSize;
    private final GridSyncPacketType type;
    @Nullable private final GID[] path;
    

    public GridPathUpdateSyncS2CPacket(@Nullable GridPath gPath, GridSyncPacketType type) {
        if(gPath == null) {
            this.lowestWatts = 0;
            this.pathSize = -1;
            this.type = type;
            this.path = null;
        } else {
            this.lowestWatts = gPath.getMaxTransferRate();
            this.pathSize = gPath.size() + 1;
            this.type = type;
            this.path = new GID[pathSize];

            gPath.forEachVertex(vert -> {
                path[vert.getFirst()] = vert.getSecond().getID();
            });
        }
    }

    public GridPathUpdateSyncS2CPacket(FriendlyByteBuf buf) {
        this.lowestWatts = buf.readFloat();
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
        buf.writeFloat(lowestWatts);
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
                    GridClientCache.ofInstance().markValidPath(path);
                    break;
                case REMOVE:
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

    public String toString() {
        String out = "";
        for(GID id : path) {
            out += "\n" + id;
        }
        return out;
    }
}