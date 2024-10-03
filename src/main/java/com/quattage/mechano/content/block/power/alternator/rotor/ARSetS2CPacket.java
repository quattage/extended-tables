package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.function.Supplier;

import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlock.RotorModelType;
import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkEvent.Context;

public class ARSetS2CPacket implements Packetable {

    private final BlockPos target;
    private final RotorModelType type;

    public ARSetS2CPacket(BlockPos target, RotorModelType type) {
        this.target = target;
        this.type = type;
    }

    public ARSetS2CPacket(FriendlyByteBuf buf) {
        this.target = buf.readBlockPos();
        this.type = RotorModelType.values()[buf.readByte()];
    }


    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(target);
        buf.writeByte((byte)type.ordinal());
    }

    @Override
    public boolean handle(Supplier<Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleAsClient(target, type)));
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("resource")
    private static void handleAsClient(BlockPos target, RotorModelType type) {
        Level world = Minecraft.getInstance().level;
        BlockState state = world.getBlockState(target);
        if(state.getBlock() instanceof AbstractRotorBlock arb)
            arb.setModel(world, target, state, type);
    }
}
