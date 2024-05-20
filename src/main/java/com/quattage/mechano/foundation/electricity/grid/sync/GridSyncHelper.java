package com.quattage.mechano.foundation.electricity.grid.sync;

import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The GridSyncDirector does a few things: <p>
 * - Contains helper methods for marking chunks and sending packets<p>
 * - Maintains a Hashmap of chunks and what players are looking at them<p>
 * - Automatically sends packets to players when they look at chunks containing Grid info<p>
 */
@EventBusSubscriber(modid = Mechano.MOD_ID)
public class GridSyncHelper {

    private static final HashMultimap<UUID, ChunkPos> targetedGridChunks = HashMultimap.create();
    
    public GridSyncHelper() {}

    public static void informPlayerEdgeUpdate(GridSyncPacketType type, GridClientEdge edge) {
        MechanoPackets.sendToAllClients(new GridEdgeUpdateSyncS2CPacket(type, edge));
    }

    public static void informPlayerVertexDestroyed(GridSyncPacketType type, GID edge) {
        MechanoPackets.sendToAllClients(new GridVertDestroySyncS2CPacket(type, edge.getPos()));
    }

    public static void markChunksChanged(ClientLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);
    }

    public static void sendPathDebug(GridPath path, GridSyncPacketType type) {
        MechanoPackets.sendToAllClients(new GridPathUpdateS2CPacket(path, type));
    }

    private static void syncGridChunkWithPlayer(Level world, ChunkPos chunkPos, Player player, GridSyncPacketType type) {
        GlobalTransferGrid grid = GlobalTransferGrid.of(world);
        if(!(player instanceof ServerPlayer sPlayer)) return;
        for(LocalTransferGrid sys : grid.getSubgrids()) {
            for(GridEdge edge : sys.allEdges()) {
                if(chunkPos.equals(new ChunkPos(edge.getSideA().getPos()))) 
                    MechanoPackets.sendToClient(new GridEdgeUpdateSyncS2CPacket(type, edge.toLightweight()), sPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkEnterPlayerView(ChunkWatchEvent.Watch event) {
        if(targetedGridChunks.put(event.getPlayer().getUUID(), event.getPos()))
            syncGridChunkWithPlayer(event.getLevel(), event.getPos(), event.getPlayer(), GridSyncPacketType.ADD_WORLD);
    }

    @SubscribeEvent
    public static void onChunkLeavePlayerView(ChunkWatchEvent.UnWatch event) {
        if(targetedGridChunks.remove(event.getPlayer().getUUID(), event.getPos()))
            syncGridChunkWithPlayer(event.getLevel(), event.getPos(), event.getPlayer(), GridSyncPacketType.REMOVE);
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        Level world = event.getLevel();
        Entity entity = event.getEntity();
        if(world.isClientSide()) return;
        if(!(entity instanceof ServerPlayer player)) return;

        GlobalTransferGrid allGrids = GlobalTransferGrid.of(world);
        for(LocalTransferGrid grid : allGrids.getSubgrids()) {
            for(GridPath path : grid.allPaths()) {
                MechanoPackets.sendToClient(new GridPathUpdateS2CPacket(path, GridSyncPacketType.ADD_NEW), player);
            }
        }
    }
}
