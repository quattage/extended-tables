package com.quattage.mechano.content.block.power.alternator.stator;

import net.minecraft.util.StringRepresentable;

public interface StatorTypeTransformable<T extends Enum<T> & StringRepresentable> {

    default T toSingle() {
        return enumValues()[(get().ordinal() / 4) * 4];
    }

    default T toEndA() {
        return enumValues()[(get().ordinal() / 4) * 4 + 1];
    }

    default T toEndB() {
        return enumValues()[(get().ordinal() / 4) * 4 + 2];
    }

    default T toMiddle() {
        return enumValues()[(get().ordinal() / 4) * 4 + 3];
    }

    default T copy() {
        return enumValues()[get().ordinal()];
    }

    default T getDefualt() {
        return enumValues()[0];
    }

    abstract boolean shouldContnue(T other);
    abstract T get();
    abstract T[] enumValues();
}
