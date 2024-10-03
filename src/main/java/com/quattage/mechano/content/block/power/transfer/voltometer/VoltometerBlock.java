package com.quattage.mechano.content.block.power.transfer.voltometer;

import java.util.Locale;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.hitbox.RotatableHitboxShape;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoltometerBlock extends HorizontalDirectionalBlock implements IWrenchable {
    
    public static final EnumProperty<VoltometerModelType> MODEL_TYPE = EnumProperty.create("model", VoltometerModelType.class);
    private static RotatableHitboxShape<Direction> hitbox;

    public enum VoltometerModelType implements HitboxNameable, StringRepresentable {
        FLOOR, WALL, CEILING;

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

        public static VoltometerModelType cycle(VoltometerModelType in) {
            int pos = in.ordinal();
            if(pos % 2 == 0) pos += 1;
            else pos -= 1;
            return VoltometerModelType.values()[pos];
        }
    }

    public VoltometerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(MODEL_TYPE, VoltometerModelType.FLOOR));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        hitbox = Mechano.HITBOXES.get(FACING, state.getValue(MODEL_TYPE), this);
        return hitbox.getRotated(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState out = defaultBlockState();

        if(context.getPlayer().isCrouching()) facing = facing.getOpposite(); 

        if(facing == Direction.UP) {
            facing = context.getHorizontalDirection().getOpposite();
            return out.setValue(FACING, facing).setValue(MODEL_TYPE, VoltometerModelType.FLOOR);
        }
        if(facing == Direction.DOWN) {
            facing = context.getHorizontalDirection().getOpposite();
            return out.setValue(FACING, facing).setValue(MODEL_TYPE, VoltometerModelType.CEILING);
        }
        return out.setValue(FACING, facing).setValue(MODEL_TYPE, VoltometerModelType.WALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING).add(MODEL_TYPE);
    }
}
