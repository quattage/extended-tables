package com.quattage.mechano.foundation.helper;

public class TickingTimeTracker extends TimeTracker {

        private final int maxTicks;
        private float ticks = 0;

        public TickingTimeTracker(int maxTicks) {
            this.maxTicks = maxTicks;
        }

        public void tickTimer() {
            ticks += getDeltaTime();
        }

        public float getTimerProgress() {
            return ticks;
        }

        public boolean isDoneTicking() {
            if(ticks > maxTicks) {
                reset();
                return true;
            }
            return false;
        }

        public boolean isFulfilled() {
            return ticks > maxTicks;
        }

        public void reset() {
            ticks = 0;
            resetDelta();
        }

        public void skip() {
            ticks = maxTicks + 1;
        }
    }