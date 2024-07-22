
package com.quattage.mechano.content.block.power.transfer.connector.tiered;

import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.foundation.electricity.WattBatteryHandlable;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.watt.WattStorable.OvervoltBehavior;
import com.quattage.mechano.foundation.helper.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.helper.builder.WattBatteryHandlerBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorTier0BlockEntity extends WireAnchorBlockEntity {

    public ConnectorTier0BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

	@Override
	public void createWireNodeDefinition(AnchorBankBuilder<WireAnchorBlockEntity> builder) {
		builder.newNode()
            .at(8, 16, 8) 
            .connections(2)
            .build();
	}

	@Override
	public void createWattHandlerDefinition(WattBatteryHandlerBuilder<? extends WattBatteryHandlable> builder) {
		builder
			.defineBattery(b -> b
				.withFlux(120)
				.withVoltageTolerance(MechanoSettings.FE2W_VOLTAGE)
				.withMaxCharge(2048)
				.withMaxDischarge(2048)
				.withCapacity(2048)
				.withIncomingPolicy(OvervoltBehavior.TRANSFORM_LOSSLESS)
				.withNoEvent()
			)
			.newInteraction(Relative.BOTTOM)
				.buildInteraction()
			.build();
	}
}
