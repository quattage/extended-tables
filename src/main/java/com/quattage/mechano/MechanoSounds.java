package com.quattage.mechano;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSoundEvents.SoundEntry;

import net.minecraft.sounds.SoundSource;

public class MechanoSounds {

    public static final SoundEntry
        CABLE_CREATE = AllSoundEvents.create(Mechano.asResource("cable_create"))
            .category(SoundSource.BLOCKS)
            .build(),
        CONNECTOR_CLICK_UP = AllSoundEvents.create(Mechano.asResource("connector_click_up"))
            .category(SoundSource.BLOCKS)
            .build(),
        CONNECTOR_CLICK_DOWN = AllSoundEvents.create(Mechano.asResource("connector_click_down"))
            .category(SoundSource.BLOCKS)
            .build(),
        CONNECTOR_CLICK_DENY = AllSoundEvents.create(Mechano.asResource("connector_click_deny"))
            .category(SoundSource.BLOCKS)
            .build();



    public static void add() {
        
    }

}
