package com.quattage.mechano.content.item.wire;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnitConversions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HookupWire extends Item {

    public HookupWire(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {

        
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
        if(pLevel.isClientSide()) return InteractionResultHolder.success(stack);

        // stupid debugging haha

        for(int x = 0; x <= 200; x += 1) {
            WattUnit wattScaled = WattUnitConversions.toWatts(x, true);
            WattUnit wattUnscaled = wattScaled.copy().adjustCurrent(6);
            boolean test = wattScaled.getRoundedWatts() == wattUnscaled.getRoundedWatts();
            Mechano.log("[" + x + " FE] -> " + (test ? "- pass - " + wattScaled + "   ==   " + wattUnscaled : "- FAIL -  " + wattScaled + "   !=   " + wattUnscaled));
        }

        return InteractionResultHolder.success(stack);
    }
}
