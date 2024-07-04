package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.core.watt.unit.Voltage;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientEdge;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncHelper;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncPacketType;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/***
 * A GridEdge is a logical representation of a connection between two verticies in a LocalTransferGrid
 * Where GridVertices represent connectors themselves, edges can be thought of as the wires between connectors.
 */
public class GridEdge {

    
    private final GridVertex origin;
    private final GridVertex destination;

    private final float distance;

    private final int typeID;
    private final Voltage optimalVoltage; // the voltage where this GridEdge's transfer is 100% efficient
    private final float maxWatts; // the maxmumum amps that can flow through this wire

    private boolean canTransfer = true;
    private WattUnit throughput = WattUnit.EMPTY;

    @Nullable
    private final GridEdge inverse;

    ////////////////////////////////////////////////////////////
    public GridEdge(GridVertex startVert, GridVertex endVert, int wireType, @Nullable GridEdge base) {
        if(startVert == null) throw new NullPointerException("Error instantiating GridEdge - starting point is null!");
        if(endVert == null) throw new NullPointerException("Error instantiating GridEdge - ending point is null!");

        this.origin = startVert;
        this.destination = endVert;
        this.typeID = wireType;

        final WireSpool typeSpool = WireSpool.ofType(wireType);
        this.optimalVoltage = typeSpool.getOptimalVoltage();
        this.maxWatts = typeSpool.getMaxWatts();
        this.distance = getEuclideanDistance(startVert.getID().getBlockPos(), endVert.getID().getBlockPos());

        if(base == null) this.inverse = new GridEdge(destination, origin, typeID, this);
        else this.inverse = base;
        inverse.throughput = this.throughput;
    }

    public GridEdge(GridVertex startVert, GridVertex endVert, WireSpool wireType, @Nullable GridEdge base) {
        this(startVert, endVert, wireType.getSpoolID(), base);
    }

    public GridEdge(GridVertex startVert, GridVertex endVert, WireSpool wireType) {
        this(startVert, endVert, wireType.getSpoolID(), null);
    }

    public GridEdge(GridVertex startVert, GridVertex endVert, int wireType) {
        this(startVert, endVert, wireType, null);
    }

    public static float getEuclideanDistance(BlockPos a, BlockPos b) {
        return (float)Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2f) + 
            Math.pow(a.getY() - b.getY(), 2f) + 
            Math.pow(a.getZ() - b.getZ(), 2f)
        );
    }

    /**
     * Deserializes a CompoundTag into a GridEdge and adds GridVertices to the parent network if they don't already exist.
     * Also ensures that the resulting GridEdge has its ends linked to each other.
     * @param world World to operate within (used for creating GridVertex objects)
     * @param parent LocalTransferGrid to call upon for serialization
     * @param startVert Starting vertex (the vertex that is calling this operation)
     * @param tag Tag that contains the edge data.
     */
    public static void deserializeLink(Level world, LocalTransferGrid parent, GridVertex startVert, CompoundTag tag) {

        if(parent == null) throw new NullPointerException("Error instantiating GridEdge - Parent network is null!");
        if(startVert == null) throw new NullPointerException("Error instantiating GridEdge - The provided starting vertex is null!");

        GridVertex endVert = parent.getOrCreateOnLoad(world, GID.of(tag.getCompound("d")));

        GridEdge edge = new GridEdge(
            startVert, endVert, 
            tag.getInt("t")
        );

        startVert.addLink(edge);
        endVert.addLink(edge.getInverse());
        GridSyncHelper.informPlayerEdgeUpdate(GridSyncPacketType.ADD_WORLD, edge.toLightweight());
    }
    ////////////////////////////////////////////////////////////

    public CompoundTag writeTo(CompoundTag nbt) {
        nbt.putInt("t", typeID);
        nbt.put("d", destination.getID().writeTo(new CompoundTag()));
        return nbt;
    }

    /***
     * @return the pre-computed euclidean distance between the ends of this edge.
     */
    public float getDistance() {
        return this.distance;
    }

    /***
     * Whether or not this GridEdge should have a wire rendered for it
     * @return
     */
    public boolean rendersWire() {
        return true;
    }

    /***
     * Whether or not this GridEdge is "real."
     * "Real" wires are tied to a logical representation of some sort in the world - 
     * they can break if stretched too far, they'll drop wire when broken, they can be
     * placed by the player, etc. 
     * 
     * Non-real wires are usually added by the network itself, and are intangible. A good example
     * of a non-real wire would be a wireless or cross-dimensional form of energy transport.
     * @return
    */
    public boolean isReal() { return true; }

    /***
     * Whether or not this GridEdge can transfer any watts per tick
     * @return <code>TRUE</code> if this edge's transfer rate (in watts) is greater than the minimum watt potential
     */
    public boolean canTransfer() {
        return this.canTransfer && (!WattUnit.hasNoPotential(maxWatts));
    }

    /***
     * Whether or not this GridEdge interacts with the SystemNode at the given SVID
     * @param target SVID to check
     * @return True if one of this GridEdge's ends are at this SVID
     */
    public boolean connectsTo(GID target) {
        return target == origin.getID() || target == destination.getID();
    }

    /**
     * @return A WattUnit representing the quantity of power currently passing through this wire
     */
    public WattUnit getActiveThroughput() {
        return throughput;
    }

    /**
     * @return The GridVertex at the beginning of this edge
     */
    public GridVertex getOriginVertex() {
        return origin;
    }

    /**
     * @return The GridVertex at the end of this edge
     */
    public GridVertex getDestinationVertex() {
        return destination;
    }

    /**
     * @return The amount of power power (represented by a WattUnit) that this edge is currently seeing
     */
    public WattUnit getThroughput() {
        return throughput;
    }

    /**
     * Increases the load across this GridEdge by the given WattUnit
     * @param amount WattUnit to increase by
     * @return This GridEdge, modified as a result of this call
     */
    public GridEdge loadThroughput(WattUnit amount) {
        throughput.add(amount);
        if(throughput.getWatts() > maxWatts) 
            throughput = WattUnit.of(amount.getVoltage(), maxWatts);
        inverse.throughput = this.throughput;
        return this;
    }

    /**
     * @return The remaining power that can be conveyed by this GridEdge <code>(maximum - throughput)</code>
     */
    public float getWattsRemaining() {
        return maxWatts - throughput.getWatts();
    }

    public void forgetLoad() {
        throughput.setZero();
        inverse.throughput = this.throughput;
    }

    public float getMaximumWatts() {
        return maxWatts;
    }

    public GridClientEdge toLightweight() {
        return new GridClientEdge(this, typeID);
    }

    public boolean equals(Object o) {
        if(o instanceof GridEdge that) {
            return this.getOriginVertex().equals(that.getOriginVertex()) 
            && this.getDestinationVertex().equals(that.getDestinationVertex());
        }

        return false;
    }

    public GridEdge getInverse() {
        return inverse;
    }

    public boolean isInverseOf(GridEdge other) {
        return getInverse().equals(other);
    }

    public String toString() {
        WireSpool typeSpool = WireSpool.ofType(typeID);
        return "[{" + origin + "->" + destination + "}, type: " + (canTransfer ? typeSpool.getName().toUpperCase() : "NO TRANSFER") + "]";
    }
}
