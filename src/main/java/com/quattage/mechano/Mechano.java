package com.quattage.mechano;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.registry.MechanoBlocks;
import com.quattage.mechano.registry.MechanoMenus;
import com.quattage.mechano.registry.MechanoGroup;
import com.quattage.mechano.registry.MechanoItems;
import com.quattage.mechano.registry.MechanoPartials;
import com.quattage.mechano.registry.MechanoRecipes;
import com.quattage.mechano.content.block.power.Inductor.InductorBlockRenderer;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Mechano.MOD_ID)
public class Mechano {
    public static final String MOD_ID = "mechano";
    public static final String ESC = "\u001b";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(Mechano.MOD_ID);
    
    public Mechano() {
        genericSetup();
        new MechanoGroup("main");
    }

    public void genericSetup() {
        Mechano.log("loading mechano");
        IEventBus bussy = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerEventListeners(bussy);

        MechanoBlocks.register(bussy);
        MechanoItems.register(bussy);
        MechanoMenus.register(bussy);
        MechanoBlockEntities.register(bussy);
        MechanoRecipes.register(bussy);

        bussy.addListener(this::clientSetup);
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(MechanoRenderers::init);
        logReg("renderers");
        MechanoPartials.register(); // this will likely cause issues but it doesn't do anything yet so its fine
    }

    public static void log(String message) {      
        String prefix = ESC + "[1;35m[quattage/" + MOD_ID + "]>> " + ESC + "[1;36m";
        String suffix = ESC + "[1;35m -" + ESC;
        System.out.println(prefix + message + suffix);
    }

    public static void logReg(String message) {      
        log("Registering " + MOD_ID + " " + message);
    }


    public static ResourceLocation asResource(String filepath) {
        return new ResourceLocation(MOD_ID, filepath);
    }

    public static MutableComponent asKey(String key) {
        return Component.translatable(MOD_ID + "." + key);
    }
}
