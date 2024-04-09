package com.quattage.mechano.foundation.electricity.core;

import com.quattage.mechano.foundation.electricity.IBatteryBank;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface EBEWrenchable extends IWrenchable {

    /***
     * Fixes the data in ElectricBlockEntities and WireNodeBlockEntities after
     * their blocks have been rotated by wrenches.
     * @param oldState BlockState before the block was wrenched.
     * @param context
     */
    default void syncEBE(Level world, BlockPos pos) {

        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof IBatteryBank bb)
            bb.reOrient();
    }
}
