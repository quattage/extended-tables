package com.quattage.mechano.content.block.power.alternator.rotor;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SmallRotorBlock extends AbstractRotorBlock implements IBE<SmallRotorBlockEntity>{

    public SmallRotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SmallRotorBlockEntity> getBlockEntityClass() {
        return SmallRotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmallRotorBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.SMALL_ROTOR.get();
    }

    @Override
    boolean isRotor(Block block) {
        return block == MechanoBlocks.SMALL_ROTOR.get();
    }
}
