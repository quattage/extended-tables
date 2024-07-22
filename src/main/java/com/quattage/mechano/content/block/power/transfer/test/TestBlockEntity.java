package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.foundation.electricity.WattBatteryHandlable;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.watt.WattStorable.OvervoltBehavior;
import com.quattage.mechano.foundation.helper.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.helper.builder.WattBatteryHandlerBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockEntity extends WireAnchorBlockEntity { 

    public int test;

    public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

	@Override
	public void createWireNodeDefinition(AnchorBankBuilder<WireAnchorBlockEntity> builder) {
		builder.newNode()
            .at(16, 10, 6) 
            .connections(2)
            .build()
        .newNode()
            .at(0, 6, 11)
            .connections(2)
            .build()
        .newNode()
            .at(8, 16, 8)
            .connections(2)
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
