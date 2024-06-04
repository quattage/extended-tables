package com.quattage.mechano.foundation.electricity.grid.sync;

public enum GridSyncPacketType {
    ADD_NEW,
    ADD_WORLD,
    REMOVE,
    SYNC,
    CLEAR;

    public static GridSyncPacketType get(int x) {
        return GridSyncPacketType.values()[x];
    }

    public String toString() {
        return this.name();
    }
}
