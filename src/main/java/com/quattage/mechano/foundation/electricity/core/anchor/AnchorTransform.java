package com.quattage.mechano.foundation.electricity.core.anchor;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/***
 * An AnchorTransform converts a pixel measurement offset into a Vec3 absolute position
 * relative to the parent block's rotation.
 */
public class AnchorTransform {

    private final Vector3f baseOffset;
    private Vector3f realOffset;

    public AnchorTransform(int xOffset, int yOffset, int zOffset, @Nullable BlockState parentState) {
        this.baseOffset = new Vector3f(
            toFloatMeasurement(xOffset), 
            toFloatMeasurement(yOffset), 
            toFloatMeasurement(zOffset)
        );

        if(parentState == null) 
            realOffset = baseOffset;
        else {
            rotateToFace(DirectionTransformer.extract(parentState));
        }
    }

    private float toFloatMeasurement(int x) {
        return 0.0625f * ((float) x);
    }

    public Vec3 toRealPos(BlockPos pos) {
        return new Vec3(
            ((double) pos.getX()) + realOffset.x,
            ((double) pos.getY()) + realOffset.y,
            ((double) pos.getZ()) + realOffset.z
        );
    }

    public Vector3f getRaw() {
        return realOffset;
    }

    public void rotateToFace(CombinedOrientation dir) {
        realOffset = VectorHelper.rotate(baseOffset, dir);
    }

    private String describeVector(Vector3f vec) {
        return "[" + vec.x + "," + vec.y + ", " + vec.z + "]";
    }

    public String toString() {
        return "AnchorTransform : {Base: " + describeVector(baseOffset) + " Real: " + describeVector(realOffset);
    }
}
