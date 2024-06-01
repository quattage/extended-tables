package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientVertex;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

public class AnchorStatRequestC2SPacket implements Packetable {

    private final BlockPos target;

    public AnchorStatRequestC2SPacket(BlockPos target) {
        this.target = target;
    }

    public AnchorStatRequestC2SPacket(FriendlyByteBuf buf) {
        this.target = buf.readBlockPos();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(target);
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            if(!(context.getSender().level() instanceof ServerLevel world)) return;
            if(!(world.getBlockEntity(target) instanceof WireAnchorBlockEntity wbe)) return;

            GridClientVertex[] summary = new GridClientVertex[wbe.getAnchorBank().size()];
            GlobalTransferGrid grid = GlobalTransferGrid.of(world);

            int x = 0;
            for(AnchorPoint anchor : wbe.getAnchorBank().getAll()) {
                
                GridVertex vert = anchor.getParticipant();
                if(vert == null) vert = grid.getVertAt(anchor.getID());

                summary[x] = new GridClientVertex(
                    vert.links.size(),
                    vert.getF(),
                    vert.getStoredHeuristic(),
                    vert.getCumulative(),
                    vert.isMember()
                );

                x++;
            }

            MechanoPackets.sendToClient(new AnchorStatSummaryS2CPacket(target, summary), context.getSender());
        });
        return true;
    }
}