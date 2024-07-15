package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.foundation.electricity.builder.WattBatteryHandlerBuilder;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/***
 * The ElectroKineticBlockEntityBlockEntity provides the base functionality and handlers required to 
 * use the sided WattStorable capability as a subclass of Create's KineticBlockEntity. BlockEntities that
 * need both Kinetics and WattStorable functionality should extend from this.
 * @see {@link com.quattage.mechano.foundation.electricity.ElectricBlockEntity <code>ElectricBlockEntity</code>} for non-kinetic purposes.
 */
public abstract class ElectroKineticBlockEntity extends KineticBlockEntity implements WattBatteryHandlable{

    public final WattBatteryHandler<ElectroKineticBlockEntity> battery;

    public ElectroKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        WattBatteryHandlerBuilder<ElectroKineticBlockEntity> init = new WattBatteryHandlerBuilder<ElectroKineticBlockEntity>().at(this);
        createWattHandlerDefinition(init);
        battery = init.build();
    }

    @Override
    public void reOrient(BlockState state) {
        battery.reflectStateChange(state);
    }
    
    @Override
    public WattBatteryHandler<? extends WattBatteryHandlable> getWattBatteryHandler() {
        return battery;
    }

    @Override
    public void onLoad() {
        battery.loadAndUpdate(this.getBlockState(), !(getUpdateTag().contains("m")));
        super.onLoad();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return battery.provideEnergyCapabilities(cap, side);
    }

    @Override // runs on first tick
    public void initialize() {
        super.initialize();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        battery.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        battery.writeTo(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        battery.readFrom(tag);
        super.read(tag, clientPacket);
    }

    

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        battery.readFrom(tag);
    }
}
