package com.quattage.mechano.content.block.power.alternator.stator;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.Hitbox;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractStatorBlock extends SimpleOrientedBlock {

    public static final EnumProperty<StatorBlockModelType> MODEL_TYPE = EnumProperty.create("model", StatorBlockModelType.class);
    private static Hitbox<SimpleOrientation> hitbox;

    public AbstractStatorBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(MODEL_TYPE, StatorBlockModelType.BASE_SINGLE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(hitbox == null) hitbox = MechanoClient.HITBOXES.get(ORIENTATION, state.getValue(MODEL_TYPE), this);
        return hitbox.getRotated(state.getValue(ORIENTATION));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        updateRotorsAround(pLevel, pPos, pState);
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }

    private void updateRotorsAround(Level pLevel, BlockPos pPos, BlockState pState) {
        // TODO impl
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean pIsMoving) {

        Direction cardinal = state.getValue(SmallStatorBlock.ORIENTATION).getCardinal();
        Direction orientation = DirectionTransformer.toDirection(state.getValue(SmallStatorBlock.ORIENTATION).getOrient());

        BlockState rear = world.getBlockState(pos.relative(orientation));
        BlockState front = world.getBlockState(pos.relative(orientation.getOpposite()));

        boolean hasRear = this.isStator(rear.getBlock()) && rear.getValue(SmallStatorBlock.ORIENTATION).getCardinal() == cardinal;
        boolean hasFront = this.isStator(front.getBlock()) && front.getValue(SmallStatorBlock.ORIENTATION).getCardinal() == cardinal;

        StatorBlockModelType type = state.getValue(SmallStatorBlock.MODEL_TYPE);
        StatorBlockModelType typeNew = type.copy();

        Mechano.log("F: " + hasFront + "  R: " + hasRear);

        if(hasFront && hasRear) typeNew = typeNew.toMiddle();
        else if(hasFront) typeNew = typeNew.toEndA();
        else if(hasRear) typeNew =  typeNew.toEndB();
        else typeNew = typeNew.toSingle();

        if(typeNew != type) setModel(world, pos, state, typeNew);

        super.neighborChanged(state, world, pos, block, fromPos, pIsMoving);
    }

    protected void setModel(Level world, BlockPos pos, BlockState state, StatorBlockModelType bType) {
        world.setBlock(pos, state.setValue(MODEL_TYPE, bType), Block.UPDATE_ALL);
    }



    public abstract BlockEntry<? extends AbstractStatorBlock> getEntry();
    public abstract boolean isStator(Block block);
    public abstract int getRadius();

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODEL_TYPE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult ray) {
        IPlacementHelper helper = PlacementHelpers.get(getPlacementHelperId());

        ItemStack heldItem = player.getItemInHand(hand);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

		return InteractionResult.PASS;
	}

    protected abstract int getPlacementHelperId();
}
