package com.quattage.mechano.content.block.power.alternator.stator;

import java.util.Locale;

import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;

import net.minecraft.util.StringRepresentable;

public enum StatorBlockModelType implements StringRepresentable, HitboxNameable {
        
    BASE_SINGLE(false),
    BASE_END_A(false),      
    BASE_END_B(false),      
    BASE_MIDDLE(false),     

    CORNER_SINGLE(true),
    CORNER_END_A(true),     
    CORNER_END_B(true),     
    CORNER_MIDDLE(true),    

    BASE_BIG_SINGLE(false),
    BASE_BIG_END_A(false),  
    BASE_BIG_END_B(false),  
    BASE_BIG_MIDDLE(false), 

    CORNER_BIG_SINGLE(false),
    CORNER_BIG_END_A(false),  
    CORNER_BIG_END_B(false),  
    CORNER_BIG_MIDDLE(false); 

    final boolean isCorner;

    private StatorBlockModelType(boolean isCorner) {
        this.isCorner = isCorner;
    }

    public StatorBlockModelType toCorner() {
        return this;
    }

    public StatorBlockModelType toSingle() {
        return StatorBlockModelType.values()[(this.ordinal() / 4) * 4];
    }

    public StatorBlockModelType toEndA() {
        return StatorBlockModelType.values()[(this.ordinal() / 4) * 4 + 1];
    }

    public StatorBlockModelType toEndB() {
        return StatorBlockModelType.values()[(this.ordinal() / 4) * 4 + 2];
    }

    public StatorBlockModelType toMiddle() {
        return StatorBlockModelType.values()[(this.ordinal() / 4) * 4 + 3];
    }

    public boolean isCorner() {
        return isCorner;
    }

    public StatorBlockModelType copy() {
        return StatorBlockModelType.values()[this.ordinal()];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }
}
