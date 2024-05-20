package com.quattage.mechano;

import net.minecraftforge.eventbus.api.IEventBus;

public class MechanoSettings {
    
    // TODO json
    public static int ALTERNATOR_MAX_LENGTH = 16;
    public static int ALTERNATOR_MINIMUM_PERCENT = 75;
    public static int WIRE_ANIM_LENGTH = 300;
    public static int ANCHOR_SELECT_SIZE = 40;
    public static int ANCHOR_NORMAL_SIZE = 20;
    public static int POLE_STACK_SIZE = 16;

    public static int FE2W_RATE = 8; // 1 watt = (FE2W_RATE) FE
    public static int FE2W_VOLTAGE = 120;

    protected static void init(IEventBus modBus) {
        
    }
}
