package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable.OvervoltBehavior;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class WattSyncS2CPacket implements Packetable {

    private final float watts;
    private final BlockPos target;
    private final byte ovb;

    public static WattSyncS2CPacket ofSource(WattStorable battery, BlockPos pos) {
        if(battery == null || pos == null) return null;
        return new WattSyncS2CPacket(battery.getStoredWatts(), pos, battery.getOvervoltBehavior());
    }

    public WattSyncS2CPacket(float watts, BlockPos target, OvervoltBehavior ovb) {
        this.watts = watts;
        this.target = target;
        this.ovb = (byte)ovb.ordinal();
    }

    public WattSyncS2CPacket(FriendlyByteBuf buf) {
        this.watts = buf.readFloat();
        this.target = buf.readBlockPos();
        this.ovb = buf.readByte();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(watts);
        buf.writeBlockPos(target);
        buf.writeByte(ovb);
    }

    @Override
    @SuppressWarnings("resource")
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if(Minecraft.getInstance().level.getBlockEntity(target) instanceof ElectricBlockEntity ebe)
                ebe.battery.getEnergyHolder().setStoredWatts(watts, false);
        });
        return true;
    }
}