package com.quattage.mechano.content.block.power.alternator.rotor;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BigRotorBlock extends AbstractRotorBlock implements IBE<BigRotorBlockEntity>{

    public BigRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<BigRotorBlockEntity> getBlockEntityClass() {
        return BigRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BigRotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.BIG_ROTOR.get();
    }

    @Override
    boolean isRotor(Block block) {
        return block == MechanoBlocks.BIG_ROTOR.get();
    }
}
