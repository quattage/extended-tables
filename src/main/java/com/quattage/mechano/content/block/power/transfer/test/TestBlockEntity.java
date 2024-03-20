package com.quattage.mechano.content.block.power.transfer.test;

import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.IBatteryBank;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
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
    public void createBatteryBankDefinition(BatteryBankBuilder<? extends IBatteryBank> builder) {
        builder
            .capacity(5000)
            .maxIO(2500)
            .newInteraction(Relative.BOTTOM)
            .buildInteraction()
        .build();
    }
}
