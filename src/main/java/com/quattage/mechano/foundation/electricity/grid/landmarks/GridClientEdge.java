package com.quattage.mechano.foundation.electricity.grid.landmarks;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/***
 * A lightweight alternative to GridEdge <p>
 * Can be written to and from a buffer to
 * send to the client. <p>
 */
public class GridClientEdge {

    private final GID sideA;
    private final GID sideB;
    private final int wireType;

    private final int initialAge;
    
    private float age = (float)MechanoSettings.WIRE_ANIM_LENGTH;

    public GridClientEdge(GridEdge edge, int wireType) {
        this.sideA = edge.getSideA();
        this.sideB = edge.getSideB();
        this.wireType = wireType;
        this.age = age * (int)((float)Mth.clamp(cheb(), 3, 90) * 0.4f);
        this.initialAge = 0;
    }

    public GridClientEdge(GIDPair edge, int wireType) {
        this.sideA = edge.getSideA();
        this.sideB = edge.getSideB();
        this.wireType = wireType;
        this.age = age * (int)((float)Mth.clamp(cheb(), 3, 90) * 0.4f);
        this.initialAge = 0;
    }

    public GridClientEdge(FriendlyByteBuf buf) {
        this.sideA = new GID(buf.readBlockPos(), buf.readInt());
        this.sideB = new GID(buf.readBlockPos(), buf.readInt());
        this.wireType = buf.readInt();
        this.age = age * (int)((float)Mth.clamp(cheb(), 3, 90) * 0.4f);
        this.initialAge = (int)age;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(sideA.getPos());
        buf.writeInt(sideA.getSubIndex());
        buf.writeBlockPos(sideB.getPos());
        buf.writeInt(sideB.getSubIndex());
        buf.writeInt(wireType);
    }

    public int cheb() {
        BlockPos a = sideA.getPos();
        BlockPos b = sideB.getPos();
        return 
            Math.max(Math.max(
                Math.abs(a.getX() - b.getX()), 
                Math.abs(a.getY() - b.getY())), 
                Math.abs(a.getZ() - b.getZ()
            ));
    } 

    public GID getSideA() {
        return sideA;
    }

    public GID getSideB() {
        return sideB;
    }

    public String toString() {
        BlockPos a = sideA.getPos();
        BlockPos b = sideB.getPos();
        return "GridClientEdge{[" + a.getX() + ", " + a.getY() + ", " + a.getZ() + ", " + sideA.getSubIndex() 
            + "], [" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ", " + sideB.getSubIndex() + "]}";
    }

    public Pair<Vec3, Vec3> getPositions(Level world) {
        Pair<AnchorPoint, WireAnchorBlockEntity> cA = AnchorPoint.getAnchorAt(world, sideA);
        Pair<AnchorPoint, WireAnchorBlockEntity> cB = AnchorPoint.getAnchorAt(world, sideB);

        if(cA == null || cA.getFirst() == null || cB == null || cB.getFirst() == null) return null;
        return Pair.of(cA.getFirst().getPos(), cB.getFirst().getPos());
    }

    public boolean equals(Object other) {
        if(other instanceof GridClientEdge otherEdge)
            return (sideA.equals(otherEdge.sideA) && sideB.equals(otherEdge.sideB)) ||  
                (sideB.equals(otherEdge.sideA) && sideA.equals(otherEdge.sideB));
        return  false;
    }

    public int hashCode() {
        return (sideA.hashCode() + sideB.hashCode()) * 31;
    }

    public boolean contains(GID id) {
        return sideA.equals(id) || sideB.equals(id);
    }

    public boolean existsIn(ClientLevel world) {
        return getPositions(world) != null;
    }

    public float getAge() {
        return this.age;
    }

    public void tickAge(long delta) {
        this.age = (float)(this.age - (1l * (delta * 0.000001d)));
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getInitialAge() {
        return initialAge;
    }

    public GIDPair toHashable() {
        return new GIDPair(sideA, sideB);
    }

    public boolean containsPos(BlockPos pos) {
        if(sideA.getPos().equals(pos)) return true;
        if(sideB.getPos().equals(pos)) return true;
        return false;
    }

    public boolean goesNowhere() {
        return sideA.equals(sideB);
    }

    public int getTypeID() {
        return wireType;
    }
}
