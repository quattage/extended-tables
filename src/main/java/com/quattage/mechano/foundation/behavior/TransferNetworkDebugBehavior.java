package com.quattage.mechano.foundation.behavior;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import java.util.ArrayList;
import java.util.Iterator;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.system.edge.ISystemEdge;
import com.quattage.mechano.foundation.electricity.system.SystemVertex;
import com.quattage.mechano.foundation.electricity.system.TransferSystem;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;

public class TransferNetworkDebugBehavior extends ClientBehavior {

    Iterator<TransferSystem> subsystems;

    public TransferNetworkDebugBehavior(String name) {
        super(name);
    }

    @Override
    public boolean shouldTick(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand,
            Vec3 lookingPosition, BlockPos lookingBlockPos) {
        return instance.options.renderDebug;
    }

    @Override
    public void tickSafe(ClientLevel world, Player player, ItemStack mainHand, ItemStack offHand, Vec3 lookingPosition,
            BlockPos lookingBlockPos, double pTicks) {
    
        ArrayList<TransferSystem> subsystems = NETWORK.all();
        for(int x = 0; x < subsystems.size(); x++) {

            TransferSystem sys = subsystems.get(x);

            Color col = sys.getDebugColor();
            if(sys.allEdges().isEmpty()) {
                continue;
            }

            for(ISystemEdge edge : sys.allEdges()) {
                if(edge != null) {
                    CreateClient.OUTLINER
                        .showLine("edge-" + edge.getVecB() + edge.getVecA(), edge.getVecA(), edge.getVecB())
                        .lineWidth(1/16f)
                        .disableCull()
                        .disableLineNormals()
                        .colored(col);
                }
            }
        }
    }

    public void drawNetworkStatistics(ClientLevel world, SystemVertex node) {   
        BlockEntity targetBE = world.getBlockEntity(node.getPos());
        BlockPos drawLocation = node.getPos().relative(DirectionTransformer.getUp(targetBE.getBlockState()), 2);
    }
    
    @Override
    public double setTickRate() {
        return 200;
    }

    @Override
    public double setTickIncrement() {
        return 1;
    }
}   