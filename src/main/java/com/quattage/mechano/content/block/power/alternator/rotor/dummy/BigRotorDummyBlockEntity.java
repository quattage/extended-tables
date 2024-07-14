package com.quattage.mechano.content.block.power.alternator.rotor.dummy;

import java.util.List;

import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlockEntity;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BigRotorDummyBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private BlockPos parentPos;

    public BigRotorDummyBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        return;
    }

    protected void obliterate(boolean drop) {
        if(parentPos == null) searchForParent();
        if(parentPos == null) return;

        if(level.getBlockState(parentPos).getBlock() == MechanoBlocks.BIG_ROTOR.get())
            level.destroyBlock(parentPos, drop);
    }

    protected void obliterate() {
        obliterate(true);
    }

    protected BlockPos searchForParent() {
        BlockState thisDummy = this.level.getBlockState(worldPosition);

        if(thisDummy.getBlock() != MechanoBlocks.BIG_ROTOR_DUMMY.get()) return null;

        Pair<BlockPos, BlockPos> corners = DirectionTransformer.getPositiveCorners(
                worldPosition, thisDummy.getValue(RotatedPillarBlock.AXIS));
        
        for(BlockPos vPos : BlockPos.betweenClosed(corners.getFirst(), corners.getSecond())) {
            if(level.getBlockState(vPos).getBlock() instanceof BigRotorBlock)
                return vPos;
        }

        return null;
    }

    protected void setParentPos(BlockPos parentPos) {
        this.parentPos = parentPos;
    }

    protected BlockPos getParentPos() {
        return this.parentPos;
    }

    @Override
    public void initialize() {
        parentPos = searchForParent();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        
        if(parentPos == null) return;
        tag.putInt("pX", parentPos.getX());
        tag.putInt("pY", parentPos.getY());
        tag.putInt("pZ", parentPos.getZ());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if(!tag.contains("pX")) return;

        parentPos = new BlockPos(
            tag.getInt("pX"),
            tag.getInt("pY"),
            tag.getInt("pZ")
        );
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(parentPos == null) return false;
        if(getLevel().getBlockEntity(parentPos) instanceof BigRotorBlockEntity brbe)
            return brbe.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return false;
    }
}
