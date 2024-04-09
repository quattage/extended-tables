package com.quattage.mechano.content.block.power.alternator.stator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SmallStatorBlock extends AbstractStatorBlock {

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper(1));

    public SmallStatorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public BlockEntry<? extends AbstractStatorBlock> getEntry() {
        return MechanoBlocks.SMALL_STATOR;
    }

    @Override
    public boolean isStator(Block block) {
        return block == getEntry().get();
    }

    @Override
    public int getRadius() {
        return 1;
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends StatorDirectionalHelper<SimpleOrientation> {

		protected PlacementHelper(int radius) {
			super(radius, state -> state.getBlock() instanceof SmallStatorBlock, 
                state -> state.getValue(ORIENTATION), ORIENTATION);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof SmallStatorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.SMALL_STATOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful()) {
				offset.withTransform(offset.getTransform());
            }
			return offset;
		}
	}
}
