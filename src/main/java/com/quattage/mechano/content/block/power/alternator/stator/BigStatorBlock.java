package com.quattage.mechano.content.block.power.alternator.stator;

import java.util.Locale;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class BigStatorBlock extends AbstractStatorBlock<com.quattage.mechano.content.block.power.alternator.stator.BigStatorBlock.BigStatorModelType> {

    public static final EnumProperty<BigStatorModelType> MODEL_TYPE = EnumProperty.create("model", BigStatorModelType.class);
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper(2));

    protected static enum BigStatorModelType implements HitboxNameable, StringRepresentable, StatorTypeTransformable<BigStatorModelType> {
        
        BASE_SINGLE(false),
        BASE_END_A(false),      
        BASE_END_B(false),      
        BASE_MIDDLE(false),     

        CORNER_SINGLE(true),
        CORNER_END_A(true),     
        CORNER_END_B(true),     
        CORNER_MIDDLE(true);

        final boolean isCorner;

        BigStatorModelType(boolean isCorner) {
            this.isCorner = isCorner;
        }

        @Override
        public BigStatorModelType get() {
            return this;
        }

        @Override
        public String getHitboxName() {
            return toString();
        }

        @Override
        public boolean isCorner() {
            return this.isCorner;
        }

        @Override
        public BigStatorModelType[] enumValues() {
            return values();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String toString() {
            return getSerializedName();
        }
    }

    public BigStatorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public BlockEntry<? extends AbstractStatorBlock<BigStatorModelType>> getEntry() {
        return MechanoBlocks.BIG_STATOR;
    }

    @Override
    public boolean isStator(Block block) {
        return block == getEntry().get();
    }

    @Override
    public int getRadius() {
        return 2;
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @Override
    protected EnumProperty<BigStatorModelType> getTypeProperty() {
        return MODEL_TYPE;
    }

    @Override
    protected BigStatorModelType getDefaultModelType() {
        return BigStatorModelType.BASE_SINGLE;
    }

    @Override
    protected BlockState getAlignedModelState(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane) {
        return thisState;
    }

    @Override
    protected UpdateAlignment getAlignmentType() {
        return UpdateAlignment.CORNERS;
    }

    //////////
    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends StatorDirectionalHelper<SimpleOrientation> {

		protected PlacementHelper(int radius) {
			super(radius, state -> state.getBlock() instanceof BigStatorBlock, 
                state -> state.getValue(ORIENTATION), ORIENTATION);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof BigStatorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.BIG_STATOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if(offset.isSuccessful()) {
				offset.withTransform(offset.getTransform());
            }
			return offset;
		}
	}
}

