package com.quattage.mechano;

import com.quattage.mechano.foundation.electricity.core.watt.WattStorable;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGridDispatcher;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.GridClientCacheProvider;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class MechanoCapabilities {

    public final Capability<GlobalTransferGrid> SERVER_GRID_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public final Capability<GridClientCache> CLIENT_CACHE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public final Capability<WattStorable> WATT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    public void registerTo(IEventBus bus) {
        bus.addGenericListener(Level.class, this::addWorldCapabilities);
        Mechano.logReg("capabilities");
    }

    @SuppressWarnings({"resource"})
    public void addWorldCapabilities(AttachCapabilitiesEvent<Level> event) {
        if(event.getObject().isClientSide) {
            Mechano.LOGGER.info("Attaching ClientCache capability to " + event.getObject().dimension().location());
            event.addCapability(Mechano.asResource("transfer_grid_client_cache"), new GridClientCacheProvider((ClientLevel)event.getObject()));
        } else {
            Mechano.LOGGER.info("Attaching ServerGrid capability to " + event.getObject().dimension().location());
            event.addCapability(Mechano.asResource("transfer_grid_server_manager"), new GlobalTransferGridDispatcher(event.getObject()));
        }
    }

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(WattStorable.class);
    }
}
