package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.AbstractStatorBlock;
import com.quattage.mechano.foundation.helper.shape.CircleGetter;
import com.quattage.mechano.foundation.helper.shape.ShapeGetter;
import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractRotorBlockEntity extends KineticBlockEntity {

    private final ShapeGetter circle = ShapeGetter.ofShape(CircleGetter.class).withRadius(getStatorRadius()).build();

    private byte statorCount = 0;

    @Nullable
    private BlockPos controllerPos = null;

    public AbstractRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {

        if(nbt.contains("cX")) {
            controllerPos = new BlockPos(
                nbt.getInt("cX"),
                nbt.getInt("cY"),
                nbt.getInt("cZ")
            );
        } else controllerPos = null;

        statorCount = nbt.getByte("sC");
        super.read(nbt, clientPacket);
    }

    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {

        if(controllerPos == null) {
            nbt.remove("cX");
            nbt.remove("cY");
            nbt.remove("cZ");
        } else {
            nbt.putInt("cX", controllerPos.getX());
            nbt.putInt("cY", controllerPos.getY());
            nbt.putInt("cZ", controllerPos.getZ());
        }

        nbt.putByte("sC", statorCount);

        super.write(nbt, clientPacket);
    }

    protected void findConnectedStators(boolean notifyIfChanged) {

        final Set<BlockPos> visited = new HashSet<>();

        int oldCount = statorCount;
        statorCount = 0;
        circle.moveTo(getBlockPos()).setAxis(getBlockState().getValue(RotatedPillarBlock.AXIS)).evaluatePlacement(perimeterPos -> {
            
            if(visited.contains(perimeterPos)) return null;
            BlockState perimeterState = getLevel().getBlockState(perimeterPos);
            if(perimeterState.getBlock() instanceof AbstractStatorBlock asb) {
                if(asb.hasRotor(getLevel(), perimeterPos, perimeterState))
                    statorCount++;
            }

            visited.add(perimeterPos);
            return null;
        });

        if(notifyIfChanged && (oldCount != statorCount)) {
            notifyUpdate();
        }
    }

    public void incStatorCount() {
        statorCount++;
        if(statorCount > getStatorCircumference()) {
            statorCount = (byte)getStatorCircumference();
            return;
        }

        getAndRefreshController();
        notifyUpdate();
    }

    public void decStatorCount() {
        statorCount--;
        if(statorCount < 0) {
            statorCount = 0;
            return;
        }

        getAndRefreshController();
        notifyUpdate();
    }

    private void getAndRefreshController() {

        if(hasNetwork()) {
            KineticNetwork parent = getOrCreateNetwork();
            parent.updateStressFor(this, calculateStressApplied());
            parent.sync();
        }

        if(controllerPos == null) return;
        //AA if(getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe)
            //AA srbe.initialize();
    }

    public abstract int getStatorCircumference();
    public abstract int getStatorRadius();
    protected abstract float getEfficiencyBonus();

    @Nullable
    protected SlipRingShaftBlockEntity getController() {
        if(controllerPos == null) return null;
        if(getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe) 
            return srbe;
        return null;
    }

    /**
     * Sets the BlockPos of the connected SlipRingShaft. 
     * @param controllerPos BlockPos of the slip ring
     * @param update <code>TRUE</code> to automatically re-evaluate the slip ring at the given BlockPos.
     */
    public void setControllerPos(BlockPos controllerPos, boolean update) {
        this.controllerPos = controllerPos;

        //AA if(controllerPos != null && update 
            //AA && getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe)
                //AA srbe.evaluateAlternatorStructure();

        notifyUpdate();
    }

    protected void setStatorCount(byte statorCount) {
        this.statorCount = statorCount;
        notifyUpdate();
    }

    @Override
    public float calculateStressApplied() {
        float impact = calculateStressWithStators((float) BlockStressValues.getImpact(getStressConfigKey()), false);
		this.lastStressApplied = impact;
		return impact;
    }

    private float calculateStressWithStators(float mul, boolean max) {
        float sP = (float)statorCount / (float)getStatorCircumference(); 
        if(max)
            return toNearest4((float)(0.00196f * Math.pow(262144, 1))) * mul;
        else return toNearest4((float)(0.00196f * Math.pow(262144, sP))) * mul;
    }

    private float toNearest4(float in) {
        float remainder = in % 4;
        return Math.max(1, remainder < 4 ? in - remainder : in + (4 - remainder));
    }

    public float getWeightedSpeed() {
        float out = (float)Mth.clamp((float)0.0039f * Math.pow(1.0218971487f, 2f * Math.abs(getTheoreticalSpeed())), 0f, 256f);
        return out >= 0.1 ? out : 0;
    }

    public float getMaximumRotaryStress() {
        return toNearest4(calculateStressWithStators((float)BlockStressValues.getImpact(getStressConfigKey()), false) * 256);
    }

    public float getMaximumPossibleStress() {
        return toNearest4(calculateStressWithStators((float)BlockStressValues.getImpact(getStressConfigKey()), true) * 256);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    public byte getStatorCount() {
        return statorCount;
    }

    public int getMultiplier() {
        return 1;
    }
}