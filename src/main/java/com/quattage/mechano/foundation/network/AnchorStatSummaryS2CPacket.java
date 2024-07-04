package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientVertex;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class AnchorStatSummaryS2CPacket implements Packetable {

    private static AnchorStatSummaryS2CPacket AWAITING; 

    // TODO I AM VERY STUPID AND FORGOT THAT MULTIPLE PEOPLE CAN LOOK AT VERTICES SIMULTANEIOUSLY SO THIS WONT WORK ON SERVERS

    private final BlockPos pos;
    private final GridClientVertex[] vertSummary;

    public AnchorStatSummaryS2CPacket(BlockPos pos, GridClientVertex[] vertSummary) {
        this.pos = pos;
        this.vertSummary = vertSummary;
    }

    public AnchorStatSummaryS2CPacket(BlockPos pos, GridVertex... vertices) {
        this.pos = pos;
        this.vertSummary = new GridClientVertex[vertices.length];
        for(int x = 0; x < vertices.length; x++) {
            GridVertex vert = vertices[x];
            vertSummary[x] = new GridClientVertex(
                vert.links.size(),
                vert.getF(),
                vert.getStoredHeuristic(),
                vert.getCumulative(),
                vert.isMember()
            );
        }
    }

    public AnchorStatSummaryS2CPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.vertSummary = new GridClientVertex[buf.readInt()];
        for(int x = 0; x < vertSummary.length; x++)  {
            vertSummary[x] = new GridClientVertex(
                buf.readInt(), 
                buf.readFloat(), 
                buf.readFloat(), 
                buf.readFloat(), 
                buf.readBoolean()
            );
        }
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(vertSummary.length);
        for(GridClientVertex vert : vertSummary) {
            buf.writeInt(vert.getConnections());
            buf.writeFloat(vert.getF());
            buf.writeFloat(vert.getHeuristic());
            buf.writeFloat(vert.getCumulative());
            buf.writeBoolean(vert.isMember());
        }
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            AWAITING = this;
        });

        return true;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public GridClientVertex[] getVertices() {
        return vertSummary;
    }

    public static AnchorStatSummaryS2CPacket getAwaiting() {
        return AWAITING;
    }

    public static void resetAwaiting() {
        AWAITING = null;
    }
}