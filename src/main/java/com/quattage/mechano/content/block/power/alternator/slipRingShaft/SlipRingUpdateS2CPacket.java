package com.quattage.mechano.content.block.power.alternator.slipRingShaft;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.network.Packetable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class SlipRingUpdateS2CPacket implements Packetable {
    
    private final BlockPos target;

    public SlipRingUpdateS2CPacket(BlockPos target) {
        this.target = target;
    }

    public SlipRingUpdateS2CPacket(FriendlyByteBuf buf) {
        target = buf.readBlockPos();
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
        Level world = Minecraft.getInstance().level;
            if(world.isLoaded(target) && world.getBlockEntity(target) instanceof SlipRingShaftBlockEntity srbe) 
                srbe.onSpeedChanged(Float.MIN_VALUE); 
    }
}