package com.quattage.mechano.foundation.block.upgradable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.ForgeRegistries;

import static com.quattage.mechano.Mechano.UPGRADES;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.AbstractConnectorBlock;
import com.quattage.mechano.foundation.block.upgradable.UpgradeCache.UpgradeStep;
import com.simibubi.create.AllItems;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;

public interface BlockUpgradable {

    default <T extends Enum<T> & StringRepresentable> InteractionResult onUpgradeInitiated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand) {
        
        if(world.isClientSide()) return InteractionResult.FAIL;

        Block block = state.getBlock();
        UpgradeStep upgrade = UPGRADES.get(block);

        ItemStack heldItem = player.getItemInHand(hand);
        if(heldItem == null) return InteractionResult.FAIL;

        boolean downgrade = false;
        if(player.isShiftKeyDown() && heldItem.getItem() == AllItems.WRENCH.get()) {
            if(upgrade == null) upgrade = UPGRADES.getDowngrade(state);
            else upgrade = UPGRADES.getDowngrade(upgrade);
            downgrade = true;
        }

        if(upgrade == null) return InteractionResult.FAIL;
        if(!downgrade && !upgrade.getItem().equals(ForgeRegistries.ITEMS.getKey(player.getItemInHand(hand).getItem()))) 
            return InteractionResult.FAIL;

        Block product = ForgeRegistries.BLOCKS.getValue(downgrade ? upgrade.getBase() : upgrade.getResult());
        if(product == null) {
            Mechano.LOGGER.warn("Error handling block upgrade initialization - A block named '" 
                + upgrade.getResult() + "' could not be found!");
            return InteractionResult.FAIL;
        }

        BlockState newState = swapInPlace(world, pos, state, product, getStatesToPreserve(new ArrayList<EnumProperty<T>>()));
        if(newState != null) {
            world.sendBlockUpdated(pos, state, state, 3);
            playUpgradeSound(world, pos, upgrade.getStepNumber());
            if(downgrade) {
                if(!player.isCreative()) {
                    Block.getDrops(state, (ServerLevel) world, pos, world.getBlockEntity(pos), player, heldItem)
					.forEach(itemStack -> {
						player.getInventory().placeItemBackInInventory(itemStack);
					});
                }
            } else
                subtractFromHand(player, hand, heldItem, 1);

            if(state != newState)
                onStateChange(world, state, newState, pos);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Called whenever a block's state is successfully changed as a result of an upgrade or downgrade
     * @param oldState state before the upgrade/downgrade
     * @param newState state after the upgrade/downgrade
     * @param pos BlockPos of the upgraded/downgraded block
     */
    abstract void onStateChange(Level world, BlockState oldState, BlockState newState, BlockPos pos);

    @Nullable
    default BlockState swapInPlace(Level world, BlockPos pos, BlockState operantState, Block product) {
        return swapInPlace(world, pos, operantState, product, null);
    }

    @Nullable
    default <T extends Enum<T> & StringRepresentable> BlockState swapInPlace(Level world, BlockPos pos, BlockState operantState, Block product, @Nullable List<EnumProperty<T>> statesToPreserve) {        

        BlockState productState = product.defaultBlockState();

        if(statesToPreserve == null || statesToPreserve.isEmpty()) {
            if(!world.getBlockState(pos).equals(productState)) {
                world.destroyBlock(pos, false);
                world.setBlock(pos, productState, 0);
                return productState;
            }
    
            return null;
        }

        for(EnumProperty<T> property : statesToPreserve) {
            if(productState.hasProperty(property) && operantState.hasProperty(property))
                productState = productState.setValue(property, operantState.getValue(property));
        }

        if(!world.getBlockState(pos).equals(productState)) {
            world.destroyBlock(pos, false);
            world.setBlock(pos, productState, 0);
            return productState;
        }

        return null;
    }

    default void subtractFromHand(Player player, InteractionHand hand, ItemStack heldItem, int amount) {
        if(player.isCreative()) return;
        ItemStack subtracted = heldItem.split(heldItem.getCount() - amount);
        player.setItemInHand(hand, subtracted);
    }

    default void playUpgradeSound(Level world, BlockPos pos, int upgradeStep) {
        world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, 1.2f * (float)(upgradeStep / 2f));
        world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.2f, 1);
    }

    default void buildUpgradableLoot(Block block, RegistrateBlockLootTables lt) {
        lt.add(block, defineDrops(LootTable.lootTable(), LootPool.lootPool()));
    }

    @Nullable
    abstract <T extends Enum<T> & StringRepresentable> List<EnumProperty<T>> getStatesToPreserve(ArrayList<EnumProperty<T>> out);
    abstract LootTable.Builder defineDrops(LootTable.Builder table, LootPool.Builder pool);
}
