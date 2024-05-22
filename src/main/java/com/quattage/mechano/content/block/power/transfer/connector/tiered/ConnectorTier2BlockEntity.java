package com.quattage.mechano.content.block.power.transfer.connector.tiered;

import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.WattBatteryHandlable;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.electricity.builder.WattBatteryHandlerBuilder;
import com.quattage.mechano.foundation.electricity.core.watt.WattStorable.OvervoltBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorTier2BlockEntity extends WireAnchorBlockEntity {

    public ConnectorTier2BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
	public void createWireNodeDefinition(AnchorBankBuilder<WireAnchorBlockEntity> builder) {
		builder.newNode()
            .at(1, 14, 8) 
            .connections(8)
            .build()
        .newNode()
            .at(15, 14, 8) 
            .connections(8)
            .build();
	}


	@Override
	public void createWattHandlerDefinition(WattBatteryHandlerBuilder<? extends WattBatteryHandlable> builder) {
		builder
			.defineBattery(b -> b
				.withFlux(120)
				.withVoltageTolerance(120)
				.withMaxCharge(2048)
				.withMaxDischarge(2048)
				.withCapacity(2048)
				.withIncomingPolicy(OvervoltBehavior.LIMIT_LOSSY)
				.withNoEvent()
			)
			.newInteraction(Relative.BOTTOM)
				.buildInteraction()
			.build();
	}
}
