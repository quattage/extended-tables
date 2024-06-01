package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class WattModeSyncS2CPacket implements Packetable {

    private final BlockPos target;
    private final byte mode;

    public static WattSyncS2CPacket ofSource(WattStorable battery, BlockPos pos) {
        if(battery == null || pos == null) return null;
        return new WattSyncS2CPacket(battery.getStoredWatts(), pos, battery.getOvervoltBehavior());
    }

    public WattModeSyncS2CPacket(BlockPos target, ExternalInteractMode mode) {
        this.target = target;
        this.mode = (byte)mode.ordinal();
    }

    public WattModeSyncS2CPacket(FriendlyByteBuf buf) {
        this.target = buf.readBlockPos();
        this.mode = buf.readByte();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(target);
        buf.writeByte(mode);
    }

    @Override
    @SuppressWarnings("resource")
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(target) instanceof ElectricBlockEntity ebe)
                ebe.getWattBatteryHandler().setModeAsClient(ExternalInteractMode.values()[mode]);
        });
        return true;
    }
}