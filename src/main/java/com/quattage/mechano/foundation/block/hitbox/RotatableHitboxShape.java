package com.quattage.mechano.foundation.block.hitbox;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import java.util.List;
import java.util.Locale;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RotatableHitboxShape<T extends Enum<T> & StringRepresentable> {

    private final Map<T, VoxelShape> shapes;
    private final EnumProperty<T> propertyGroup;
    private final String typeName;

    @SuppressWarnings("unchecked")
    protected RotatableHitboxShape(String typeName, EnumProperty<T> group, List<List<Double>> deserialized) {
    
        this.propertyGroup = group;
        this.shapes = new HashMap<>();
        this.typeName = typeName;

        VoxelShapeBuilder shape = new VoxelShapeBuilder();
        for(List<Double> c : deserialized)
            shape.addBox(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5));

        if(!shape.hasFeatures()) {
            Mechano.LOGGER.error("Something went wrong building a Hitbox bound to " + group.toString() + 
                " - the resulting VoxelShape is empty, and was skipped.");
            shapes.put((T)(group.getPossibleValues().toArray()[0]), VoxelShapeBuilder.CUBE);
            return;
        }

        shape.optimize();

        for(T orient : group.getPossibleValues()) 
            shapes.put(orient, shape.getRotatedCopy(DirectionTransformer.getRotation(group, orient)));
    }

    private RotatableHitboxShape(String typeName, EnumProperty<T> group, Map<T, VoxelShape> shapes) {
        this.propertyGroup = group;
        this.shapes = shapes;
        this.typeName = typeName;
    }

    protected String getTypeName() {
        return typeName;
    }

    protected EnumProperty<T> getPropertyGroup() {
        return propertyGroup;
    }

    @SuppressWarnings("unchecked")
    protected static <R extends Enum<R> & StringRepresentable> RotatableHitboxShape<R> ofMissingResource(EnumProperty<R> group) {
        Map<R, VoxelShape> shapes = new HashMap<>();
        shapes.put((R)(group.getPossibleValues().toArray()[0]), VoxelShapeBuilder.CUBE);
        return new RotatableHitboxShape<R>(DefaultModelType.DEFAULT.getHitboxName(), group, shapes);
    }

    @SuppressWarnings("unchecked")
    public T getDefaultProperty() {
        return (T)propertyGroup.getPossibleValues().toArray()[0];
    }

    public VoxelShape getRotated(@Nullable T orient) {

        if(orient == null || shapes.size() <= 1)
            return getDefault();

        VoxelShape shape = shapes.get(orient);
        if(shape == null) {
            Mechano.LOGGER.warn("Error getting rotated VoxelShape for hitbox bound to " + propertyGroup.getName() + 
                "- The given Property '" + orient.toString() + "' is not supported by this Hitbox!");
            return VoxelShapeBuilder.CUBE;
        }
        return shape;
    }

    public VoxelShape getDefault() {
        return shapes.get(getDefaultProperty());
    }

    public String toString() {
        return "[ Hitbox of " + shapes.size() + " variations, using type '" + propertyGroup.getName() + "' ]";
    }

    public enum DefaultModelType implements StringRepresentable, HitboxNameable {
        DEFAULT;

        @Override
        public String toString() {
            return getSerializedName();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
        
    }
}
