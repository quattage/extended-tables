package com.quattage.mechano.content.block.power.alternator.slipRingShaft;

import com.quattage.mechano.Mechano;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlockEntity;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.ElectroKineticBlockEntity;
import com.quattage.mechano.foundation.electricity.BatteryBankUpdatable;
import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import javax.annotation.Nullable;

public class SlipRingShaftBlockEntity extends ElectroKineticBlockEntity {
    
    private SlipRingShaftStatus status = SlipRingShaftStatus.NONE;

    @Nullable
    private BlockPos opposingPos = null;

    private int length;
    private int currentPowerScore = 0;
    private int potentialPowerScore = 0;

    public SlipRingShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {
        nbt.putByte("sO", (byte)status.ordinal());
        nbt.putInt("cS", currentPowerScore);
        nbt.putInt("pS", potentialPowerScore);
        nbt.putInt("lE", length);

        if(opposingPos != null) {
            nbt.putInt("opX", opposingPos.getX());
            nbt.putInt("opY", opposingPos.getY());
            nbt.putInt("opZ", opposingPos.getZ());
        }

        super.write(nbt, clientPacket);
    }

    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {
        status = SlipRingShaftStatus.values()[nbt.getByte("sO")];
        currentPowerScore = nbt.getInt("cS");
        potentialPowerScore = nbt.getInt("pS");
        length = nbt.getInt("lE");

        if(nbt.contains("opX")) {
            opposingPos = new BlockPos(
                nbt.getInt("opX"),
                nbt.getInt("opY"),
                nbt.getInt("opZ")
            );
        } else opposingPos = null;

        super.read(nbt, clientPacket);
    }

    @Override
    public void initialize() {
        super.initialize();
        Direction dir = getBlockState().getValue(DirectionalKineticBlock.FACING);
        refreshSlipRingStatus(dir, true);
        refreshRotorScore(dir);
    }

    @Override
    public void remove() {
        Direction dir = getBlockState().getValue(DirectionalKineticBlock.FACING);
        undoAlternator(dir);
        super.remove();
    }

    private void resetScores() {
        this.length = 0;
        this.currentPowerScore = 0;
        this.potentialPowerScore = 0;
    }

    private void refreshSlipRingStatus(Direction dir, boolean preserveParentChildRelationship) {

        for(int x = 0; x < MechanoSettings.ALTERNATOR_MAX_LENGTH; x++) {

            BlockPos thisPos = getBlockPos().relative(dir, x + 1);
            BlockEntity thisEntity = getLevel().getBlockEntity(thisPos);

            if(thisEntity instanceof AbstractRotorBlockEntity arbe) {

                if(arbe.getBlockState().getValue(RotatedPillarKineticBlock.AXIS) != dir.getAxis())
                    break;
                continue;

            } else if(thisEntity instanceof SlipRingShaftBlockEntity srbe) {

                if(srbe.getBlockState().getValue(DirectionalKineticBlock.FACING) != dir.getOpposite()) 
                    break;

                srbe.opposingPos = this.getBlockPos();
                this.opposingPos = srbe.getBlockPos();

                if(preserveParentChildRelationship) {
                    if(srbe.status.canControl) {
                        setStatusAndUpdate(SlipRingShaftStatus.ROTORED_CHILD);
                        srbe.setStatusAndUpdate(SlipRingShaftStatus.ROTORED_PARENT);    
                    } else {
                        srbe.setStatusAndUpdate(SlipRingShaftStatus.ROTORED_CHILD);
                        setStatusAndUpdate(SlipRingShaftStatus.ROTORED_PARENT);
                    }
                } else {
                    // TODO additional events
                    srbe.setStatusAndUpdate(SlipRingShaftStatus.ROTORED_CHILD);
                    setStatusAndUpdate(SlipRingShaftStatus.ROTORED_PARENT);
                }

                return;

            } 

            if(x > 0) setStatusAndUpdate(SlipRingShaftStatus.ROTORED_NO_OPPOSITE);
            else setStatusAndUpdate(SlipRingShaftStatus.NONE);
            opposingPos = null;
            return;
        }
    }

    private void refreshRotorScore(Direction dir) {

        resetScores();
        for(int x = 0; x < MechanoSettings.ALTERNATOR_MAX_LENGTH; x++) {

            BlockPos thisPos = getBlockPos().relative(dir, x + 1);
            BlockEntity thisEntity = getLevel().getBlockEntity(thisPos);

            if(thisEntity instanceof AbstractRotorBlockEntity arbe) {
                this.length++;
                this.potentialPowerScore += arbe.getStatorCircumference();
                this.currentPowerScore += arbe.getStatorCount();
                if(this.status.canControl) arbe.setControllerPos(this.getBlockPos());
                continue;
            }

            break;
        }
    }

    public void undoAlternator() {
        undoAlternator(getBlockState().getValue(DirectionalKineticBlock.FACING));
    }

    protected void undoAlternator(Direction dir) {

        resetScores();

        if(!this.status.canControl) {

            if(getLevel().getBlockEntity(getBlockPos().relative(dir)) instanceof AbstractRotorBlockEntity arbe &&
                arbe.getBlockState().getValue(RotatedPillarKineticBlock.AXIS) == dir.getAxis()) {
                setStatusAndUpdate(SlipRingShaftStatus.ROTORED_NO_OPPOSITE);
            } else setStatusAndUpdate(SlipRingShaftStatus.NONE);

            if(opposingPos != null && getLevel().getBlockEntity(opposingPos) instanceof SlipRingShaftBlockEntity srbe && srbe.status.canControl) {
                srbe.undoAlternator(dir.getOpposite());
                opposingPos = null;
            }
            
            return;
        } 

        for(int x = 0; x < MechanoSettings.ALTERNATOR_MAX_LENGTH; x++) {

            BlockPos thisPos = getBlockPos().relative(dir, x + 1);
            BlockEntity thisEntity = getLevel().getBlockEntity(thisPos);

            if(thisEntity instanceof AbstractRotorBlockEntity arbe) {
                if(arbe.getBlockState().getValue(RotatedPillarKineticBlock.AXIS) != dir.getAxis())
                    break;

                arbe.setControllerPos(null);
            }

            if(x > 0) setStatusAndUpdate(SlipRingShaftStatus.ROTORED_NO_OPPOSITE);
            else setStatusAndUpdate(SlipRingShaftStatus.NONE);
        }

        if(opposingPos != null && getLevel().getBlockEntity(opposingPos) instanceof SlipRingShaftBlockEntity srbe) {
            srbe.undoAlternator(dir.getOpposite());
            opposingPos = null;
        }
    }

    private void setStatusAndUpdate(SlipRingShaftStatus newStatus) {
        if(this.status != newStatus) {
            this.status = newStatus;
            notifyUpdate();
        }
    }

    /**
     * Called exclusively by {@link SlipRingShaftBlock#use}
     * <p> Special behavior is such that manually updating a Slip Ring
     * will allow it to relinquish its controller status.
     */
    protected static boolean refreshForManualUpdate(Level world, BlockPos pos) {
        if(!(world.getBlockEntity(pos) instanceof SlipRingShaftBlockEntity srbe)) return false;
        SlipRingShaftStatus oldStatus = srbe.status;

        Direction dir = srbe.getBlockState().getValue(DirectionalKineticBlock.FACING);
        srbe.refreshSlipRingStatus(dir, false);
        srbe.refreshRotorScore(dir);
        if(oldStatus != srbe.status) return true;
        return false;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        if(!this.status.canControl && opposingPos != null && getLevel().getBlockEntity(opposingPos) instanceof SlipRingShaftBlockEntity srbe) {
            return buildStats(tooltip, srbe.length, srbe.currentPowerScore, srbe.potentialPowerScore, this.status);
        }

        return buildStats(tooltip, this.length, this.currentPowerScore, this.potentialPowerScore, this.status);
    }


    // TODO are these utf8 or nah lol
    // ✔ ❌    §    https://hypixel.net/attachments/colorcodes-png.2694223/
    private boolean buildStats(List<Component> tooltip, int len, int cScore, int pScore, SlipRingShaftStatus status) {

        if(status == SlipRingShaftStatus.NONE) return false;
        final boolean hasOpposite = status != SlipRingShaftStatus.ROTORED_NO_OPPOSITE;
        final float sPercent = Math.round(((float)cScore / (float)pScore) * 100);

        lang().translate("gui.alternator.status.title")
			.forGoggles(tooltip);

        lang().text("§9§l———— §7§l🔧§9§l Assembly —————").style(ChatFormatting.GRAY).forGoggles(tooltip);

        if(len > 1)
            lang().text("  §2§l✔ ").translate("gui.alternator.status.hasLength").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else lang().text("  §2§l❌ ").translate("gui.alternator.status.noLength").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if(hasOpposite)
            lang().text("  §2§l✔ ").translate("gui.alternator.status.hasOpposing").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else lang().text("  §c§l❌ ").translate("gui.alternator.status.noOpposing").style(ChatFormatting.GRAY).forGoggles(tooltip);

        if(sPercent >= MechanoSettings.ALTERNATOR_MINIMUM_PERCENT) {
            if(sPercent == 100)
                lang().text("  §2§l✔ ").translate("gui.alternator.status.perfectCoverage").style(ChatFormatting.GRAY).forGoggles(tooltip);    
            else if(sPercent > 90)
                lang().text("  §2§l✔ ").translate("gui.alternator.status.goodCoverage").style(ChatFormatting.GRAY).forGoggles(tooltip); 
            else 
                lang().text("  §6§l✔ ").translate("gui.alternator.status.decentCoverage").style(ChatFormatting.GRAY).forGoggles(tooltip); 
        }
            
        else lang().text("  §c§l❌ ").translate("gui.alternator.status.badCoverage").text(" (§l§c" + cScore + " / " + (int)(pScore * ((float)MechanoSettings.ALTERNATOR_MINIMUM_PERCENT / 100f)) + "§r)").style(ChatFormatting.GRAY).forGoggles(tooltip);

        lang().text("").forGoggles(tooltip);
        lang().text("§9§l————— §7§l⚡§9§l Status —————").style(ChatFormatting.GRAY).forGoggles(tooltip);
        lang().text(cScore + "/" + pScore + " stators").forGoggles(tooltip);

        if(status == SlipRingShaftStatus.ROTORED_CHILD)
            lang().text("♠ Child").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else if(status == SlipRingShaftStatus.ROTORED_PARENT)
            lang().text("⌂ Parent").style(ChatFormatting.GRAY).forGoggles(tooltip);

        return true;
    }

    @Override
    public void createBatteryBankDefinition(BatteryBankBuilder<? extends BatteryBankUpdatable> builder) {
        builder.capacity(10000)
                .newInteraction(Relative.BOTTOM).onlySendEnergy().buildInteraction();
    }


    private static enum SlipRingShaftStatus {
        ROTORED_PARENT(true, true),
        ROTORED_CHILD(false, true),
        ROTORED_TOO_LONG(false, false),
        ROTORED_NO_OPPOSITE(false, false),
        NONE(false, false);

        private final boolean canControl;

        @SuppressWarnings("unused")
        private final boolean hasComplementary;

        private SlipRingShaftStatus(boolean canControl, boolean hasComplementary) {
            this.canControl = canControl;
            this.hasComplementary = hasComplementary;
        }
    }
}
