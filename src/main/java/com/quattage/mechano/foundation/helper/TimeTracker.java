package com.quattage.mechano.foundation.helper;

public class TimeTracker {

    private long oldTime = -1;

    public TimeTracker() {}

    public double getDeltaTime() {
        long time = System.nanoTime();
        if(oldTime <= 0) oldTime = time;
        double out = (time - oldTime) * 0.000001f;
        oldTime = time;
        return out;
    }

    public void resetDelta() {
        oldTime = -1;
    }
}
