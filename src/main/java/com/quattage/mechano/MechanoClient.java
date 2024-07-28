
package com.quattage.mechano;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import com.jozufozu.flywheel.core.PartialModel;
import com.quattage.mechano.foundation.block.hitbox.HitboxCache;
import com.quattage.mechano.foundation.block.hitbox.HitboxProvider;
import com.quattage.mechano.foundation.compat.embeddium.EmbeddiumWireCompat;
import com.quattage.mechano.foundation.electricity.grid.WireAnchorSelectionManager;
import com.quattage.mechano.foundation.electricity.rendering.WireTextureProvider;
import com.quattage.mechano.foundation.ui.MechanoIconAtlas;

@OnlyIn(Dist.CLIENT)
public class MechanoClient {

    public static final WireTextureProvider WIRE_TEXTURE_PROVIDER = new WireTextureProvider();
    public static final MechanoIconAtlas ICONS = new MechanoIconAtlas(Mechano.asResource("textures/gui/icons.png"), 128);
    public static final WireAnchorSelectionManager ANCHOR_SELECTOR = new WireAnchorSelectionManager(Minecraft.getInstance());
    public static final HitboxCache HITBOXES = new HitboxCache();
    protected static final HitboxProvider HITBOX_PROVIDER = new HitboxProvider();

    public static final PartialModel 
        PART_DIAGIRDER_SDF = newPartial("diagonal_girder/partials/short_down_flat"),
        PART_DIAGIRDER_SDV = newPartial("diagonal_girder/partials/short_down_vert"),
        PART_DIAGIRDER_SUF = newPartial("diagonal_girder/partials/short_up_flat"),
        PART_DIAGIRDER_SUV = newPartial("diagonal_girder/partials/short_up_vert"),
        PART_DIAGIRDER_LDF = newPartial("diagonal_girder/partials/long_down_flat"),
        PART_DIAGIRDER_LDV = newPartial("diagonal_girder/partials/long_down_vert"),
        PART_DIAGIRDER_LUF = newPartial("diagonal_girder/partials/long_up_flat"),
        PART_DIAGIRDER_LUV = newPartial("diagonal_girder/partials/long_up_vert"),
        PART_CHEV_OVERLAY = newPartial("generic/chevron_cutout"),
        PART_CHEV_OVERLAY_INV = newPartial("generic/chevron_cutout_2");

    private static PartialModel newPartial(String path) {
        return new PartialModel(Mechano.asResource("block/" + path));
	}

    protected static void init(IEventBus modBus, IEventBus forgeBus) {
        if(ModList.get().isLoaded("embeddium"))
            EmbeddiumWireCompat.registerCompatModule(forgeBus);
    }
}
