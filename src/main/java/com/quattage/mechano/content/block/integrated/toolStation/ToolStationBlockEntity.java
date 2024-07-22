package com.quattage.mechano.content.block.integrated.toolStation;

import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlock.WideBlockModelType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


public class ToolStationBlockEntity extends SmartBlockEntity implements MenuProvider {

    // storage data
    public final ToolStationInventory INVENTORY;
    private String maxUpgrade = "";

    // progress data
    private int heat = 0;
    private int maxHeat = 25;

    // gui information

    public ToolStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        INVENTORY = new ToolStationInventory(this);
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
    }

    public void sendToMenu(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(getBlockPos());
		buffer.writeNbt(getUpdateTag());
	}

    @Override
    public Component getDisplayName() {
        return Mechano.asKey("toolStationBlockEntity");
    }

    @Override
    public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player player) {
        return ToolStationMenu.create(menuId, playerInventory, this);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        heat = nbt.getInt("operations");
        maxUpgrade = nbt.getString("maxUpgradeApplied");
        super.read(nbt, clientPacket);
        
    }

    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {
        nbt.putInt("operations", heat);
        nbt.putString("maxUpgradeApplied", maxUpgrade);
        super.write(nbt, clientPacket);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ToolStationBlockEntity entity) {
        if(level.isClientSide) {
            return;
        }

        /*
        if(hasRecipe(entity)) {
            
        } 
        */
        
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // no implemtation
    }

    public void doUpgradeEffects(BlockState state, WideBlockModelType blockType) {
        switch (blockType) {
            case BASE:
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, 0.6f);
                level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 2f, 0.6f);
                break;
            case FORGED:
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_BREAK, SoundSource.BLOCKS, 0.7f, 0.9f);
                level.playSound(null, worldPosition, SoundEvents.POLISHED_DEEPSLATE_PLACE, SoundSource.BLOCKS, 3f, 0.3f);
                break;
            case HEATED:
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_BREAK, SoundSource.BLOCKS, 0.7f, 0.9f);
                level.playSound(null, worldPosition, SoundEvents.POLISHED_DEEPSLATE_PLACE, SoundSource.BLOCKS, 3f, 0.3f);
                break;
            case MAXIMIZED:
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_BREAK, SoundSource.BLOCKS, 0.5f, 0.5f);
                level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.2f, 0.3f);
                break;
            default:
                return;
        }
    }
}