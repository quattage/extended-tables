package com.quattage.mechano.foundation.block.orientation;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.client.model.generators.ModelFile;

public class DynamicStateGenerator extends SpecialBlockStateGen {

    private final @Nullable EnumProperty<?> typeDelegate;
    private @Nullable String[] customIn;
    private @Nullable String[] customSub;


    public DynamicStateGenerator(EnumProperty<?> typeDelegate) {
        this.typeDelegate = typeDelegate;
    }

    public DynamicStateGenerator() {
        this.typeDelegate = null;
    }

    public DynamicStateGenerator in(String... customIn) {
        this.customIn = customIn;
        return this;
    }

    public DynamicStateGenerator sub(String... customSub) {
        this.customSub = customSub;
        return this;
    }

    @Override
    protected int getXRotation(BlockState state) {
        return (int)DirectionTransformer.getRotation(state).x();
    }

    @Override
    protected int getYRotation(BlockState state) {
        return (int)DirectionTransformer.getRotation(state).y();
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx,
        RegistrateBlockstateProvider provider, BlockState state) {

        String typeName = (typeDelegate == null) ? "base" : 
            state.getValue(typeDelegate).getSerializedName();

        String orientSuffix = 
            (DirectionTransformer.isDistinctionRequired(state) &&
            DirectionTransformer.isHorizontal(state)) 
            ? "_side" : "";

        return provider.models().getExistingFile(Mechano.extend(ctx, "block", customIn, customSub, typeName + orientSuffix));
    }
}
