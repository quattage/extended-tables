package com.quattage.mechano.foundation.network;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.behavior.GridEdgeDebugBehavior;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridPathViewMaskS2CPacket implements Packetable {

    @Nullable
    private final Set<GID> mask;

    public GridPathViewMaskS2CPacket(Set<GID> mask) {
        this.mask = mask;
    }

    public GridPathViewMaskS2CPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();

        if(size < 1) this.mask = null;
        else {
            this.mask = new HashSet<GID>();
            for(int x = 0; x < size; x++) {
                BlockPos pos = buf.readBlockPos();
                mask.add(new GID(pos, buf.readInt()));
            }
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {

        if(mask == null || mask.size() < 1) {
            buf.writeInt(-1);
            return;
        }

        buf.writeInt(mask.size());
        for(GID id : mask) {
            buf.writeBlockPos(id.getBlockPos());
            buf.writeInt(id.getSubIndex());
        }
    }

    @Override
    @SuppressWarnings("resource")
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            GridEdgeDebugBehavior.setMask(Minecraft.getInstance().level, mask, null);
        });
        return true;
    }
}