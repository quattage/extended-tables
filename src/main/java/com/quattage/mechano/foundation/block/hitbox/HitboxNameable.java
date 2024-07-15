package com.quattage.mechano.foundation.block.hitbox;

public interface HitboxNameable {
    
    default String getHitboxName() {
        return "hitbox";
    }
}
