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
            throw new NullPointerException("Exception getting Hitbox BlockState from the provided hitbox! - '" 
            + type.getHitboxName() + "' does not exist in {" + this + "}");
        }
        return shape;
    }

    public String toString() {

        String out = "";
        boolean first = true;
        for(String name : hitboxes.keySet()) {
            if(first) {
                out += name;
                first = false;
            }
            out += ", " + name;
        }

        return out;
    }
}
