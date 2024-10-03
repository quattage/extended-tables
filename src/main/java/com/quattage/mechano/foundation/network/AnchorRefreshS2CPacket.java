package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.grid.network.GridSyncHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
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
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleAsClient(target)));
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("resource")
    public static void handleAsClient(BlockPos target) {
        GridSyncHelper.markChunksChanged(Minecraft.getInstance().level, target);
    }
}