package com.quattage.mechano.foundation.block.hitbox;

import java.util.Map;

import net.minecraft.util.StringRepresentable;

public class Hitbox<R extends Enum<R> & StringRepresentable> {
    
    private final Map<String, RotatableHitboxShape<R>> hitboxes;

    protected Hitbox(Map<String, RotatableHitboxShape<R>> hitboxes) {
        this.hitboxes = hitboxes;
    }

    public Hitbox() {
        this.hitboxes = null;
    }

    public boolean needsBuilt() {
        if(hitboxes == null) return true;
        return hitboxes.isEmpty();
    }

    public <T extends Enum<T> & StringRepresentable & HitboxNameable> RotatableHitboxShape<R> get(T type) {
        RotatableHitboxShape<R> shape = hitboxes.get(type.getHitboxName());
        if(shape == null) {
            throw new NullPointerException("Error getting RotatableHitbox from Hitbox - '" 
            + type.getHitboxName() + "' does not exist in this hitbox!");
        }
        return shape;
    }
}
