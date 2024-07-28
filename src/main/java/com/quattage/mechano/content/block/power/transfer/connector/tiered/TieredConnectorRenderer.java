package com.quattage.mechano.content.block.power.transfer.connector.tiered;

import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.rendering.WireAnchorBlockRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class TieredConnectorRenderer extends WireAnchorBlockRenderer<WireAnchorBlockEntity> {
    public TieredConnectorRenderer(Context context) {
        super(context);
    }
}
