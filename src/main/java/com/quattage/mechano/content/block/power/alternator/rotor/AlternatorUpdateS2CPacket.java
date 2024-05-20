package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.function.Supplier;

import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class AlternatorUpdateS2CPacket implements Packetable {
    private final BlockPos target;
    private final Type type;

    public AlternatorUpdateS2CPacket(BlockPos target, Type type) {
        this.target = target;
        this.type = type;
    }

    public AlternatorUpdateS2CPacket(FriendlyByteBuf buf) {
        this.target = buf.readBlockPos();
        this.type = Type.values()[buf.readByte()];
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(target);
        buf.writeByte(type.ordinal());
    }

    @Override
    @SuppressWarnings("resource")
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            if(type == Type.ROTOR_INCREMENT) {
                if(Minecraft.getInstance().level.getBlockEntity(target) instanceof AbstractRotorBlockEntity arbe) 
                    arbe.incStatorCount();
                return;
            }

            if(type == Type.ROTOR_DECREMENT) {
                if(Minecraft.getInstance().level.getBlockEntity(target) instanceof AbstractRotorBlockEntity arbe)
                    arbe.decStatorCount();
                return;
            }

            if(type == Type.CONTROLLER_UPDATE) {
                if(Minecraft.getInstance().level.getBlockEntity(target) instanceof SlipRingShaftBlockEntity srbe)
                    srbe.initialize();
                return;
            }

            if(type == Type.CONTROLLER_INVALIDATE) {
                if(Minecraft.getInstance().level.getBlockEntity(target) instanceof SlipRingShaftBlockEntity srbe)
                    srbe.undoAlternator();
                return;
            }
        });
        return true;
    }

    public static enum Type {
        ROTOR_INCREMENT,
        ROTOR_DECREMENT,
        CONTROLLER_UPDATE,
        CONTROLLER_INVALIDATE;
    }
}