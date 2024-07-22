
package com.quattage.mechano.foundation.block.anchor;

import com.ibm.icu.text.DecimalFormat;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A wrapper object for an AnchorPoint that stores extra data for easy access.
 * Used by the client to sort AnchorPoints in the world by their distance from the player.
 */
public class AnchorEntry implements Comparable<AnchorEntry> {

    private final WireAnchorBlockEntity parent;
    private final AnchorPoint anchor;
    private double distance;

    public AnchorEntry(WireAnchorBlockEntity parent, AnchorPoint anchor, Player player) {
        this.parent = parent;
        this.anchor = anchor;
        if(player == null ) this.distance = Double.NEGATIVE_INFINITY;
        else this.distance = anchor.getDistanceToRaycast(player.getEyePosition(), VectorHelper.getLookingRay(player).getLocation());
    }

    public static boolean isValid(AnchorEntry entry) {
        if(entry == null) return false;

        if(entry.distance > Double.NEGATIVE_INFINITY)
            return entry.anchor != null && entry.parent != null;

        return false;
    }

    @Override
    public int compareTo(AnchorEntry that) {
        if(this.distance < that.distance) return 1;
        if(this.distance > that.distance) return -1;

        final Vec3 thisPos = this.anchor.getPos();
        final Vec3 thatPos = that.anchor.getPos();

        if(thisPos.x > thatPos.x) return 1;
        if(thisPos.x < thatPos.x) return -1;
        if(thisPos.y > thatPos.y) return 1;
        if(thisPos.y < thatPos.y) return -1;
        if(thisPos.z > thatPos.z) return 1;
        if(thisPos.z < thatPos.z) return -1;
        
        return 0;
    }
    
    public AnchorPoint get() {
        return anchor;
    }

    public WireAnchorBlockEntity getParent() {
        return parent;
    }

    public double getDistance() {
        return distance;
    }

    public double refreshDistance(Player player) {
        if(player == null) return distance;
        distance = anchor.getDistanceToRaycast(player.getEyePosition(), VectorHelper.getLookingRay(player).getLocation());
        return distance;
    }

    public String toString() {
        Vec3 pos = anchor.getPos();
        return "[" + pos.x + ", " + pos.y + ", " + pos.z +", " + new DecimalFormat("0.00").format(distance) + "]";
    }

    public boolean equals(Object o) {
        if(!(o instanceof AnchorEntry that)) return false;
        return this.anchor.getID().equals(that.anchor.getID());
    }

    public int hashCode() {
        return this.anchor.getID().hashCode();
    }
}
