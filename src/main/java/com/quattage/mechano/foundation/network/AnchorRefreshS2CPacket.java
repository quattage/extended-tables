package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.grid.sync.GridSyncHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class AnchorRefreshS2CPacket implements Packetable {
    private final BlockPos target;

    public AnchorRefreshS2CPacket(BlockPos target) {
        this.target = target;
    }

    public AnchorRefreshS2CPacket(FriendlyByteBuf buf) {
        this.target = buf.readBlockPos();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(target);
    }

    @Override
    @SuppressWarnings("resource")
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            GridSyncHelper.markChunksChanged(Minecraft.getInstance().level, target);
        });
        return true;
    }
}