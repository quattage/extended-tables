package com.quattage.mechano.content.block.power.transfer.connector.tiered;

import java.util.Locale;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.quattage.mechano.foundation.electricity.core.EBEWrenchable;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.OptionalWattOrFE;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public abstract class AbstractConnectorBlock extends SimpleOrientedBlock implements EBEWrenchable {

    private static final VoxelShape ROOTX = Block.box(0, 7, 7, 10, 9, 9);
    private static final VoxelShape ROOTY = Block.box(7, 7, 0, 9, 9, 10);
    public static final EnumProperty<TieredConnectorModelType> MODEL_TYPE = EnumProperty.create("model", TieredConnectorModelType.class);

    public enum TieredConnectorModelType implements HitboxNameable, StringRepresentable {
        BASE, STACKED, GIRDERED;

        @Override
        public String getHitboxName() {
            return toString();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
        
        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    public AbstractConnectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(MODEL_TYPE, TieredConnectorModelType.BASE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getHitbox(state);
    }

    abstract VoxelShape getHitbox(BlockState state);

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        
        BlockState out = super.getStateForPlacement(context);

        if(!canSurvive(out, context.getLevel(), context.getClickedPos())) return out;

        BlockPos under = context.getClickedPos().relative(out.getValue(ORIENTATION).getCardinal().getOpposite());
        BlockState underState = context.getLevel().getBlockState(under);

        if(underState.getBlock() == AllBlocks.METAL_GIRDER.get())
            out = out.setValue(MODEL_TYPE, TieredConnectorModelType.GIRDERED);
        else if(underState.getBlock() == MechanoBlocks.CONNECTOR_T2.get()) {
            out = out.setValue(MODEL_TYPE, TieredConnectorModelType.STACKED)
                .setValue(ORIENTATION, underState.getValue(ORIENTATION));
        }   
            

        return out;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block pBlock, BlockPos pFromPos,
        boolean pIsMoving) {
        
        if(!canSurvive(state, world, pos))
            world.destroyBlock(pos, true);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		return hasSupport(world, pos, state);
	}

    protected boolean hasSupport(LevelReader world, BlockPos pos, BlockState state) {

        Direction dir = state.getValue(ORIENTATION).getCardinal();
        BlockPos underPos = pos.relative(dir.getOpposite());
        BlockState underState = world.getBlockState(underPos);

        if(DirectionalWattProvidable.getFEOrWattsAt(world.getBlockEntity(underPos), dir).isPresent()) return true;
        if(underState.getBlock() == AllBlocks.METAL_GIRDER.get()) return true;

        if(underState.getBlock() instanceof ConnectorTier2Block) {     // TOTEM POLE
            if(underState.getValue(ORIENTATION).getCardinal() == state.getValue(ORIENTATION).getCardinal())
                return true;
        }

        if(underState.getBlock() instanceof AbstractConnectorBlock) return false;
        if(underState.isFaceSturdy(world, pos, dir, SupportType.CENTER)) return true;

        if(isAttached(world, pos, dir, ROOTX)) return true;
        if(isAttached(world, pos, dir, ROOTY)) return true;
        
        return false;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1f;
    }

    /***
     * Used for rotating a pole made of connectors all at once.
     * @param world 
     * @param startPos Starting position
     * @param facing SimpleOrientation of the block at the starting position
     * @param invert If true, iterates opposite of the given SimpleOrientation
     */
    protected void swapPoleStates(Level world, BlockPos startPos, SimpleOrientation facing, boolean invert) {

        Direction stepDir = facing.getCardinal();
        if(invert) stepDir = stepDir.getOpposite();
        
        for(int x = 0; x < MechanoSettings.POLE_STACK_SIZE; x++) {
            
            BlockPos thisPos = startPos.relative(stepDir, x + 1);
            BlockState thisState = world.getBlockState(thisPos);

            if(!(thisState.getBlock() instanceof AbstractConnectorBlock)) return;

            if(thisState.getValue(SimpleOrientedBlock.ORIENTATION).getCardinal() == facing.getCardinal()) {
                world.setBlockAndUpdate(thisPos, thisState.setValue(SimpleOrientedBlock.ORIENTATION, facing));
                syncEBE(world, thisPos);
            }
        }
    }

    protected boolean isAttached(LevelReader world, BlockPos pos, Direction dir, VoxelShape foundation) {

        BlockPos relative = pos.relative(dir);
        return !Shapes.joinIsNotEmpty(world.getBlockState(relative)
            .getBlockSupportShape(world, relative)
            .getFaceShape(dir), ROOTY, BooleanOp.ONLY_SECOND);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult result = super.onWrenched(state, context);
        syncEBE(context.getLevel(), context.getClickedPos());
        return result;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(MODEL_TYPE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return new ItemStack(MechanoBlocks.CONNECTOR_T0.get());
    }
}
