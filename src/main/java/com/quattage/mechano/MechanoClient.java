package com.quattage.mechano;

import net.minecraftforge.eventbus.api.IEventBus;

import com.quattage.mechano.foundation.block.hitbox.HitboxCache;
import com.quattage.mechano.foundation.block.hitbox.HitboxProvider;
import com.quattage.mechano.foundation.electricity.rendering.WireTextureProvider;



public class MechanoClient {

    public static final WireTextureProvider WIRE_TEXTURE_PROVIDER = new WireTextureProvider();
    public static final HitboxCache HITBOXES = new HitboxCache();

    protected static final HitboxProvider HITBOX_PROVIDER = new HitboxProvider();

    protected static void init(IEventBus modBus, IEventBus forgeBus) {
        MechanoPartials.register();
    }
}
