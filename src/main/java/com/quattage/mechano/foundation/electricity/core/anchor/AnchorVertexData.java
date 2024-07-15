package com.quattage.mechano.foundation.electricity.core.anchor;

import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Stores relevent GridVertex data to be sent to the client.
 */
public class AnchorVertexData {

    // TODO this will probably store more than just this in the future but idk lol
    private final int connections;
    private final GID id;

    public AnchorVertexData(GridVertex vertex) {
        this.connections = vertex.links.size();
        this.id = vertex.getID();
    }

    public AnchorVertexData(FriendlyByteBuf buf) {
        this(buf.readInt(), new GID(buf.readBlockPos(), buf.readInt()));
    }

    private AnchorVertexData(int connections, GID id) {
        this.connections = connections;
        this.id = id;
    }

    public FriendlyByteBuf toBytes(FriendlyByteBuf buf) {
        buf.writeInt(connections);
        buf.writeBlockPos(id.getBlockPos());
        buf.writeInt(id.getSubIndex());
        return buf;
    }

    public static AnchorVertexData ofEmpty(GID id) {
        return new AnchorVertexData(0, id);
    }

    public int getConnections() {
        return connections;
    }

    public GID getID() {
        return id;
    }
}
