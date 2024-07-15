package com.quattage.mechano.content.block.power.alternator.slipRingShaft;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.AbstractConnectorBlock;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.WattBatteryHandler;
import com.quattage.mechano.foundation.electricity.core.watt.WattSendSummary;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnitConversions;
import com.quattage.mechano.foundation.helper.NullSortedArray;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import javax.annotation.Nullable;

public class SlipRingShaftBlockEntity extends KineticBlockEntity {

    private SlipRingShaftStatus status = SlipRingShaftStatus.NONE;

    @Nullable
    public BlockPos opposingPos = null;

    private int length;
    private int currentPowerScore = 0;
    private int potentialPowerScore = 0;
    private float currentStress = 0;
    private float maximumStress = 0;

    private boolean isBuilt = false;

    public NullSortedArray<WattSendSummary> sends = new NullSortedArray<>(8);
    
    public WattUnit energyProduced = WattUnit.EMPTY;
    public WattUnit maxEnergyProduced = WattUnit.EMPTY;

    public SlipRingShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {
        status = SlipRingShaftStatus.values()[nbt.getByte("sO")];
        currentPowerScore = nbt.getInt("cS");
        potentialPowerScore = nbt.getInt("pS");
        length = nbt.getInt("lE");
        isBuilt = nbt.getBoolean("V");
        energyProduced = WattUnit.ofTag(nbt.getCompound("cE"));
        maxEnergyProduced = WattUnit.ofTag(nbt.getCompound("mE"));

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
    protected void write(CompoundTag nbt, boolean clientPacket) {
        nbt.putByte("sO", (byte)status.ordinal());
        nbt.putInt("cS", currentPowerScore);
        nbt.putInt("pS", potentialPowerScore);
        nbt.putInt("lE", length);
        nbt.putBoolean("V", isBuilt);
        nbt.put("cE", energyProduced.writeTo(new CompoundTag()));
        
        nbt.put("mE", maxEnergyProduced.writeTo(new CompoundTag()));

        if(opposingPos != null) {
            nbt.putInt("opX", opposingPos.getX());
            nbt.putInt("opY", opposingPos.getY());
            nbt.putInt("opZ", opposingPos.getZ());
        }

        super.write(nbt, clientPacket);
    }

    @Override
    public void tick() {
        super.tick();
        energyProduced = WattUnitConversions.toWatts(currentStress * getTheoreticalSpeed(), getTheoreticalSpeed());
        if(energyProduced.getWatts() < 8)
            energyProduced = WattUnit.EMPTY;

        if(canControl() && (!getLevel().isClientSide()))
            WattBatteryHandler.awardWattsTo(energyProduced, sends);
    }

    @Override
    public void initialize() {
        evaluateAlternatorStructure();
        super.initialize();
        findAdjacentConnectors();
    }

    public void findAdjacentConnectors() {
        Direction facing = getBlockState().getValue(DirectionalKineticBlock.FACING);
        for(BlockPos adjacent : DirectionTransformer.getAllAdjacent(getBlockPos(), facing.getAxis()))
            ((SlipRingShaftBlock)getBlockState().getBlock()).evaluateNeighbor(getLevel(), getBlockPos(), adjacent);
    }

    /**
     * Evaluates the validity, size, and power statistics of the connected rotors and stators.
     */
    public void evaluateAlternatorStructure() {

        // reset all values to zero
        this.length = 0;
        this.currentPowerScore = 0;
        this.potentialPowerScore = 0;        
        this.currentStress = 0;
        this.maximumStress = 0;
        this.maxEnergyProduced = WattUnit.EMPTY;

        // grab the facing direction if this slip ring
        Direction dir = getBlockState().getValue(DirectionalKineticBlock.FACING);

        // iterate over the blocks in the facing direction
        for(int x = 0; x < MechanoSettings.ALTERNATOR_MAX_LENGTH + 1; x++) {

            // get the block and blockentity in the facing direction
            BlockPos thisPos = getBlockPos().relative(dir, x + 1);
            BlockEntity thisEntity = getLevel().getBlockEntity(thisPos);

            // if the iterated block is a connected rotor, increment all relevent values
            if(thisEntity instanceof AbstractRotorBlockEntity arbe && isConnected(arbe, dir)) {
                this.status = SlipRingShaftStatus.ROTORED_NO_OPPOSITE;
                this.length++;
                this.potentialPowerScore += arbe.getStatorCircumference();
                this.currentPowerScore += arbe.getStatorCount();
                this.maximumStress += arbe.getMaximumPossibleStress();
                this.currentStress += arbe.calculateStressApplied();
                arbe.setControllerPos(getBlockPos(), false);

                if(x == MechanoSettings.ALTERNATOR_MAX_LENGTH) {
                    this.status = SlipRingShaftStatus.ROTORED_TOO_LONG;
                }

                continue;
            }

            // if the iterated block is a connected slip ring shaft, copy values over to it
            if(thisEntity instanceof SlipRingShaftBlockEntity srbe && isConnected(srbe, dir)) {
                this.status = SlipRingShaftStatus.ROTORED_PARENT;
                this.opposingPos = srbe.getBlockPos();
                
                srbe.opposingPos = getBlockPos();
                srbe.status = SlipRingShaftStatus.ROTORED_CHILD;
                srbe.length = this.length;
                srbe.potentialPowerScore = this.potentialPowerScore;
                srbe.currentPowerScore = this.currentPowerScore;
                srbe.maximumStress = this.maximumStress;
                srbe.currentStress = this.currentStress;
                

                this.maxEnergyProduced = WattUnitConversions.toWatts(maximumStress, 256);
                srbe.maxEnergyProduced = this.maxEnergyProduced;

                continue;
            }

            break;
        }

        // if the status has changed after the execution of the loop, evaluate the validity of this alternator's structure
        // and call notifyUpdate();

        this.isBuilt = status.hasComplementary && getStatorPercent() >= MechanoSettings.ALTERNATOR_MINIMUM_PERCENT;
        notifyUpdate();

        if(!this.status.canControl) forgetAlternatorStructure(dir);
        else copyStatsToChild();
    }

    /////////////////////////// helpers for evaluateAlternatorStructure() //////////////////////////
    private boolean isConnected(AbstractRotorBlockEntity arbe, Direction dir) {
        return dir.getAxis() == arbe.getBlockState().getValue(RotatedPillarKineticBlock.AXIS);
    } 

    private boolean isConnected(SlipRingShaftBlockEntity arbe, Direction dir) {
        return dir.getOpposite() == arbe.getBlockState().getValue(DirectionalKineticBlock.FACING);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Tells all connected rotors to "forget" their controller position and cuts ties with the 
     * opposing slip ring.
     * @param dir Direction that this slip ring is facing
     */
    public void forgetAlternatorStructure(Direction dir) {

        boolean forgotOpposite = false;
        if(opposingPos != null) {
            forgotOpposite = true;
            if(getLevel().getBlockEntity(opposingPos) instanceof SlipRingShaftBlockEntity srbe) {
                srbe.status = SlipRingShaftStatus.ROTORED_NO_OPPOSITE;
                srbe.isBuilt = false;
                opposingPos = null;
            }
        }

        for(int x = 0; x < MechanoSettings.ALTERNATOR_MAX_LENGTH; x++) {
            BlockPos pos = getBlockPos().relative(dir, x + 1);
            if(getLevel().getBlockEntity(pos) instanceof AbstractRotorBlockEntity arbe)
                arbe.setControllerPos(null, false);
            else if(!forgotOpposite && getLevel().getBlockEntity(pos) instanceof SlipRingShaftBlockEntity srbe) {
                srbe.status = SlipRingShaftStatus.ROTORED_NO_OPPOSITE;
                srbe.isBuilt = false;
                srbe.opposingPos = null;
            }
            else break;
        }
    }

    /**
     * Copies all data from parent to child. If this is called by a child slip ring, it'll just be ignored.
     */
    private void copyStatsToChild() {
        if(opposingPos == null) return;
        if(!(getLevel().getBlockEntity(opposingPos) instanceof SlipRingShaftBlockEntity srbe)) 
            return;    
        //throw new NullPointerException("Error copying SlipRingShaftBlockEntity stats from parent to child - No child Slip Ring could be found at " + opposingPos);
        srbe.length = this.length;
        srbe.potentialPowerScore = this.potentialPowerScore;
        srbe.currentPowerScore = this.currentPowerScore;
        srbe.maximumStress = this.maximumStress;
        srbe.currentStress = this.currentStress;
        srbe.opposingPos = null;
        srbe.isBuilt = this.isBuilt;
        srbe.opposingPos = getBlockPos();
        srbe.sends = sends;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        Direction dir = getBlockState().getValue(DirectionalKineticBlock.FACING);
        if(this.status != SlipRingShaftStatus.ROTORED_CHILD) {

            currentStress = 0;
            for(int x = 0; x < length; x++) {
                BlockEntity thisEntity = getLevel().getBlockEntity(getBlockPos().relative(dir, x + 1));
                if(!(thisEntity instanceof AbstractRotorBlockEntity arbe))
                    break;
                this.currentStress += arbe.calculateStressApplied();
            }
        }

        copyStatsToChild();
    }

    public boolean canControl() {
        return this.status.canControl;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        if(!this.status.canControl && opposingPos != null && getLevel().getBlockEntity(opposingPos) instanceof SlipRingShaftBlockEntity srbe) {
            if(srbe.isBuilt) {
                if(!isPlayerSneaking)
                    return buildStatsTooltip(tooltip, srbe.length, srbe.currentPowerScore, srbe.potentialPowerScore, this.status);
                else return buildPredictedStatsTooltip(tooltip, srbe.length, srbe.currentPowerScore, srbe.potentialPowerScore, srbe.currentStress, srbe.maximumStress, this.status, true);
            } else { 
                if(!isPlayerSneaking)
                    return buildAssemblyChecklistTooltip(tooltip, srbe.length, srbe.potentialPowerScore, srbe.currentPowerScore, this.status);
                else return buildPredictedStatsTooltip(tooltip, srbe.length, srbe.currentPowerScore, srbe.potentialPowerScore, srbe.currentStress, srbe.maximumStress, this.status, false);
            }
        }

        if(isBuilt) {
            if(!isPlayerSneaking)
                return buildStatsTooltip(tooltip, length, currentPowerScore, potentialPowerScore, status);
                else return buildPredictedStatsTooltip(tooltip, length, currentPowerScore, potentialPowerScore, currentStress, maximumStress, this.status, true);
        } else { 
            if(!isPlayerSneaking)
            return buildAssemblyChecklistTooltip(tooltip, length, currentPowerScore, potentialPowerScore, status);
            else return buildPredictedStatsTooltip(tooltip, length, currentPowerScore, potentialPowerScore, currentStress, maximumStress, this.status, false);
        }
    }

    private boolean buildAssemblyChecklistTooltip(List<Component> tooltip, int len, int cScore, int pScore, SlipRingShaftStatus status) {

        if(status == SlipRingShaftStatus.NONE) return false;
        final boolean hasOpposite = status != SlipRingShaftStatus.ROTORED_NO_OPPOSITE;
        final int sPercent = Math.round(getStatorPercent());

        lang().text("§7§l🔧§9§l ").translate("gui.alternator.status.unfinishedTitle")
            .forGoggles(tooltip);
        
        if(len > 0)
            lang().text("  §2§l✔ ").translate("gui.alternator.status.hasLength").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else lang().text("  §c§l❌ ").translate("gui.alternator.status.noLength").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if(hasOpposite)
            lang().text("  §2§l✔ ").translate("gui.alternator.status.hasOpposing").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else lang().text("  §c§l❌ ").translate("gui.alternator.status.noOpposing").style(ChatFormatting.GRAY).forGoggles(tooltip);

        if(sPercent >= MechanoSettings.ALTERNATOR_MINIMUM_PERCENT) {
            if(sPercent == 100)
                lang().text("  §2§l✔ ").translate("gui.alternator.status.perfectCoverage").style(ChatFormatting.GRAY).forGoggles(tooltip);    
            else 
                lang().text("  §6§l✔ ").translate("gui.alternator.status.decentCoverage").style(ChatFormatting.GRAY).forGoggles(tooltip); 
        }
            
        else lang().text("  §c§l❌ ").translate("gui.alternator.status.badCoverage").text(" (§l§c" + cScore + " / " + (int)(pScore * ((float)MechanoSettings.ALTERNATOR_MINIMUM_PERCENT / 100f) + 1)  + "§r)").style(ChatFormatting.GRAY).forGoggles(tooltip);

        return true;
    }

    private boolean buildPredictedStatsTooltip(List<Component> tooltip, int len, int cScore, int pScore, float cStress, float mStress, SlipRingShaftStatus status, boolean detail) {
        lang().text("§7§l☳ §9§l").translate("gui.alternator.status.detailedTitle").style(ChatFormatting.BLUE).style(ChatFormatting.BOLD).text(" ")
            .forGoggles(tooltip);

        final float sPercent = (float)cScore / (float)pScore;

        final int barPercent = sPercent > 0.99f ? Math.round(sPercent * 4f) : (int)Math.floor(sPercent * 4f);
        int diameter = 0;
        float rStress = cStress * Math.abs(getTheoreticalSpeed());

        lang().translate("gui.alternator.status.statusTitle").style(ChatFormatting.GRAY).forGoggles(tooltip);;
        if(status == SlipRingShaftStatus.ROTORED_CHILD)
            lang().text("  §3§l♠ ").translate("gui.slipring.state.child").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip);
        else if(status == SlipRingShaftStatus.ROTORED_PARENT)
            lang().text("  §b§l⌂ ").add(lang().translate("gui.slipring.state.parent").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip);
        else lang().text("  §c⚠ ").style(ChatFormatting.RED).add(lang().translate("gui.slipring.state.invalid").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip);
        
        if(getLevel().getBlockEntity(getBlockPos().relative(getBlockState().getValue(DirectionalKineticBlock.FACING))) instanceof AbstractRotorBlockEntity arbe)
            diameter = (2 * arbe.getStatorRadius()) + 1;

        boolean badCoverage = sPercent * 100 < MechanoSettings.ALTERNATOR_MINIMUM_PERCENT;
        if(sPercent < 1) {
            
            lang().translate("gui.alternator.status.stators")
                .text(": (" + cScore + "/" + pScore + ")")
                    .style(ChatFormatting.GRAY)
                .add(lang().text(
                    TooltipHelper.makeProgressBar(4, barPercent))
                        .style(badCoverage ? ChatFormatting.RED : ChatFormatting.GOLD)
                    .text("(" + Math.round(sPercent * 100) + "%)")
                ).forGoggles(tooltip);

            if(badCoverage)
                lang().space().space().translate("gui.alternator.status.badCoverage").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip);
            else lang().text(" ").translate("gui.alternator.status.decentCoverage").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip);
        } else {
            lang().translate("gui.alternator.status.stators")
                .text(": (" + cScore + "/" + pScore + ")")
                    .style(ChatFormatting.GRAY)
                .add(lang().text(
                    TooltipHelper.makeProgressBar(4, 4))
                        .style(ChatFormatting.GREEN)
                    .text("(100%)")
                ).forGoggles(tooltip);
            lang().space().space().translate("gui.alternator.status.perfectCoverage").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip);
        }

        lang().translate("gui.alternator.status.dimensions").style(ChatFormatting.GRAY).text(": " + length + " x " + diameter).forGoggles(tooltip);
        lang().space().space().text("(").translate("gui.alternator.status.max").style(ChatFormatting.DARK_GRAY).text(" " + MechanoSettings.ALTERNATOR_MAX_LENGTH + " x " + 5 + ")").forGoggles(tooltip);

        lang().text("").forGoggles(tooltip);
        lang().text("§7§l⚡ §9§l").translate("gui.alternator.status.predictiveTitle").style(ChatFormatting.BLUE).style(ChatFormatting.BOLD).text(" ")
            .forGoggles(tooltip);
        
        float cWatts = energyProduced.getWatts();
        float mWatts = maxEnergyProduced.getWatts();
        float wPercent = 1 - (cWatts / mWatts);

        LangBuilder su = Lang.translate("generic.unit.stress").style(ChatFormatting.DARK_GRAY);
        ChatFormatting filledColor = (badCoverage || wPercent > 0.74 || WattUnit.hasNoPotential(cWatts)) ? ChatFormatting.RED : StressImpact.of(wPercent).getRelativeColor();

        LangBuilder stress = Lang.number(rStress).style(filledColor).text(ChatFormatting.GRAY, " / ").add(Lang.number(mStress).style(ChatFormatting.DARK_GRAY));

        LangBuilder w = lang().translate("generic.unit.watts").style(ChatFormatting.DARK_GRAY);
        LangBuilder watts = Lang.number(cWatts).style(filledColor).text(ChatFormatting.GRAY, " / ").add(Lang.number(mWatts).style(ChatFormatting.DARK_GRAY));

        lang().translate("gui.alternator.status.predictiveSubtitle").style(ChatFormatting.GRAY).forGoggles(tooltip);
        lang().space().space().add(stress).add(su).forGoggles(tooltip);
        lang().space().space().add(watts).add(w).forGoggles(tooltip);
        
        if(cWatts >= mWatts && isBuilt && !WattUnit.hasNoPotential(cWatts))
            lang().text("§b§l>> §r§8(").translate("gui.slipring.state.perfect").text(")").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip);

        return true;
    }

    // TODO are these utf8 or nah lol
    // ✔ ❌    §    https://hypixel.net/attachments/colorcodes-png.2694223/
    private boolean buildStatsTooltip(List<Component> tooltip, int len, int cScore, int pScore, SlipRingShaftStatus status) {

        if(status == SlipRingShaftStatus.NONE) return false;
        final boolean hasOpposite = status != SlipRingShaftStatus.ROTORED_NO_OPPOSITE;
        final int sPercent = Math.round(getStatorPercent());

        lang().translate("gui.alternator.status.title")
			.forGoggles(tooltip);

        lang().text("§9§l————— §7§l⚡§9§l Status —————").style(ChatFormatting.GRAY).forGoggles(tooltip);
        lang().text(cScore + "/" + pScore + " stators").forGoggles(tooltip);

        lang().text("stress: " + currentStress * Math.abs(getTheoreticalSpeed()) + " / " + maximumStress).forGoggles(tooltip);
        WattUnit power = WattUnitConversions.toWatts(currentStress * getTheoreticalSpeed(), getTheoreticalSpeed());
        lang().text("Output: " + power).forGoggles(tooltip);
        lang().text("FE Equivalent: " + WattUnitConversions.toFE(power)).forGoggles(tooltip);
        lang().text("Valid? " + isBuilt).forGoggles(tooltip);


        if(status == SlipRingShaftStatus.ROTORED_CHILD)
            lang().text("♠ Child").style(ChatFormatting.GRAY).forGoggles(tooltip);
        else if(status == SlipRingShaftStatus.ROTORED_PARENT)
            lang().text("⌂ Parent").style(ChatFormatting.GRAY).forGoggles(tooltip);

        return true;
    }

    public float getStatorPercent() {
        return 100 * ((float)currentPowerScore / (float)potentialPowerScore);
    }

    private static enum SlipRingShaftStatus {
        ROTORED_PARENT(true, true),
        ROTORED_CHILD(false, true),
        ROTORED_TOO_LONG(false, false),
        ROTORED_NO_OPPOSITE(false, false),
        NONE(false, false);

        private final boolean canControl;
        private final boolean hasComplementary;

        private SlipRingShaftStatus(boolean canControl, boolean hasComplementary) {
            this.canControl = canControl;
            this.hasComplementary = hasComplementary;
        }
    }
}