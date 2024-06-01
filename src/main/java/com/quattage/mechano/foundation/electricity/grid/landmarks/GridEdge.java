package com.quattage.mechano.foundation.electricity.grid.landmarks;

import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientEdge;
import com.quattage.mechano.foundation.electricity.grid.sync.GridSyncHelper;
import com.quattage.mechano.foundation.electricity.grid.sync.GridSyncPacketType;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/***
 * A GridEdge is a logical representation of a connection between two verticies in a LocalTransferGrid
 * Where GridVertices represent connectors themselves, edges can be thought of as the wires between connectors.
 */
public class GridEdge {

    private final WireSpool wireType;
    private final GIDPair target;
    private final float distance;
    private boolean canTransfer = true;

    public GridEdge(GlobalTransferGrid parent, GIDPair edgeID, int wireType) {
        if(parent == null) throw new NullPointerException("Error instantiating GridEdge - Parent network is null!");
        if(edgeID == null) throw new NullPointerException("Error instantiating GridEdge - Target is null!");
        this.target = edgeID;
        this.wireType = WireSpool.ofType(wireType);

        BlockPos a = target.getSideA().getBlockPos();
        BlockPos b = target.getSideB().getBlockPos();
        this.distance = (float)Math.sqrt(Math.pow(a.getX() - b.getX(), 2f) + Math.pow(a.getY() - b.getY(), 2f) + Math.pow(a.getZ() - b.getZ(), 2f));

        GridSyncHelper.informPlayerEdgeUpdate(GridSyncPacketType.ADD_NEW, this.toLightweight());
    }

    public GridEdge(GlobalTransferGrid parent, CompoundTag tag) {
        if(parent == null) throw new NullPointerException("Error instantiating GridEdge - Parent network is null!");
        this.wireType = WireSpool.ofType(tag.getInt("t"));
        this.target = GIDPair.of(tag.getCompound("e"));

        BlockPos a = target.getSideA().getBlockPos();
        BlockPos b = target.getSideB().getBlockPos();
        this.distance = (float)Math.sqrt(Math.pow(a.getX() - b.getX(), 2f) + Math.pow(a.getY() - b.getY(), 2f) + Math.pow(a.getZ() - b.getZ(), 2f));
    }

    public CompoundTag writeTo(CompoundTag nbt) {
        nbt.putInt("t", wireType.getSpoolID());
        nbt.put("e", target.writeTo(new CompoundTag()));
        return nbt;
    }

    public float getDistance() {
        return this.distance;
    }

    public float calcScore() {
        return 1.0f / wireType.getRating().getCurrent();
    }

    public WireSpool getWireType() {
        return wireType;
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
     * @return True if this edge's trasnfer rate (in watts) is greater than the minimum watt potential
     */
    public boolean canTransfer() {
        return this.canTransfer && (!getTransferStats().hasNoPotential());
    }

    /**
     * Gets the transfer rate in volts & amps of this GridEdge.
     * @return A WattUnit describing the aforementioned transfer statistics
     */
    public WattUnit getTransferStats() {
        return wireType.getRating();
    }

    /***
     * Whether or not this GridEdge interacts with the SystemNode at the given SVID
     * @param target SVID to check
     * @return True if one of this GridEdge's ends are at this SVID
     */
    public boolean connectsTo(GID target) {
        return target == getSideA() || target == getSideB(); 
    }

    public GID getSideA() {
        return target.getSideA();
    }

    public GID getSideB() {
        return target.getSideB();
    }

    /***
     * @return BlockPos position of this edge's A side
     */
    public BlockPos getPosA() {
        return getSideA().getBlockPos();
    }

    /***
     * @return BlockPos position of this edge's B side
     */
    public BlockPos getPosB() {
        return getSideB().getBlockPos();
    }

    public GIDPair getHashable() {
        return target;
    }

    public GridClientEdge toLightweight() {
        return new GridClientEdge(this.target, this.wireType.getSpoolID());
    }

    public Pair<Vec3, Vec3> getPositions(Level world) {
        Pair<AnchorPoint, WireAnchorBlockEntity> cA = AnchorPoint.getAnchorAt(world, getSideA());
        Pair<AnchorPoint, WireAnchorBlockEntity> cB = AnchorPoint.getAnchorAt(world, getSideB());

        if(cA == null || cA.getFirst() == null || cB == null || cB.getFirst() == null) return null;
        return Pair.of(cA.getFirst().getPos(), cB.getFirst().getPos());
    }
}
