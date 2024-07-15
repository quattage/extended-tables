package com.quattage.mechano.foundation.block.upgradable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class UpgradeCache {

    private final Map<UpgradeKey, UpgradeStep> upgrades = new HashMap<>();

    public UpgradeCache() {}

    public UpgradeBuilder upgradesFrom(BlockEntry<? extends BlockUpgradable> from) {
        return new UpgradeBuilder(from.getId());
    }

    @Nullable
    public UpgradeStep get(Block block) {

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if(key == null) throw new NullPointerException("Error getting Upgrade object from UpgradeCache - The provided block has no key!");
        return upgrades.get(new UpgradeKey(key));
    }

    @Nullable
    public UpgradeStep getDowngrade(UpgradeStep base) {
        for(UpgradeStep upg : upgrades.values()) {
            if(upg.getResult().equals(base.getBase()))
                return upg;
        }

        return null;
    }

    @Nullable
    public UpgradeStep getDowngrade(BlockState state) {
        ResourceLocation blockLoc = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        for(UpgradeStep upg : upgrades.values()) {
            if(upg.getResult().equals(blockLoc))
                return upg;
        }

        return null;
    }

    public List<UpgradeStep> getInversePath(UpgradeStep base) {
        List<UpgradeStep> path = new ArrayList<>();
        populatePath(base, path);
        return path;
    }

    public List<UpgradeStep> getInversePath(BlockState base) {
        List<UpgradeStep> path = new ArrayList<>();
        populatePath(base, path);
        return path;
    }

    private void populatePath(BlockState base, List<UpgradeStep> path) {
        UpgradeStep step = getDowngrade(base);
        if(step != null) {
            path.add(step);
            populatePath(step, path);
        }
    }

    private void populatePath(UpgradeStep base, List<UpgradeStep> path) {
        UpgradeStep step = getDowngrade(base);
        if(step != null) {
            path.add(step);
            populatePath(step, path);
        }
    }

    ///
    public class UpgradeBuilder {

        private final ResourceLocation base;

        private UpgradeBuilder(ResourceLocation base) {
            this.base = base;
        }

        public BuildableUpgradeBuilder withStep(int step) {
            return new BuildableUpgradeBuilder(this, step);
        }
    }


    ///
    public class BuildableUpgradeBuilder {
        
        private final UpgradeBuilder builder;
        private final int step;

        private BuildableUpgradeBuilder(UpgradeBuilder builder, int step) {
            this.builder = builder;
            this.step = step;
        }
    
        public <B extends Block & BlockUpgradable, P> NonNullUnaryOperator<BlockBuilder<B, P>> whenClickedWith(RegistryEntry<? extends ItemLike> item) {

            return b -> {
                upgrades.put(new UpgradeKey(builder.base), new UpgradeStep(builder.base, new ResourceLocation(b.getOwner().getModid(), b.getName()), item.getId(), step));
                return b;
            };
        }
    }


    ///
    private class UpgradeKey {
        final ResourceLocation base;

        protected UpgradeKey(ResourceLocation base) {
            this.base = base;
        }

        public boolean equals(Object obj) {
            if(obj instanceof UpgradeKey other)
                return this.base.equals(other.base);
            return false;
        }

        public int hashCode() {
            return base.hashCode();
        }
    }


    ///
    public class UpgradeStep extends UpgradeKey {

        final ResourceLocation result;
        final ResourceLocation item;
        final int stepNumber;

        protected UpgradeStep(ResourceLocation base, ResourceLocation result, ResourceLocation item, int stepNumber) {
            super(base);
            this.result = result;
            this.item = item;
            this.stepNumber = stepNumber;
        }

        public ResourceLocation getBase() {
            return base;
        }

        public ResourceLocation getResult() {
            return result;
        }

        public ResourceLocation getItem() {
            return item;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public String toString() {
            return "[ " + base + " + " + item + " = " + result + " ]";
        }
    }
}
