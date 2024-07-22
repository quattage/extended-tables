package com.quattage.mechano.foundation.electricity.grid.network;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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

    public static void informPlayerVertexUpdate(GridSyncPacketType type, GID edge) {
        MechanoPackets.sendToAllClients(new GridVertUpdateSyncS2CPacket(type, edge.getBlockPos()));
    }

    public static void informPlayerPathUpdate(GridSyncPacketType type, @Nullable GridPath path) {
        MechanoPackets.sendToAllClients(new GridPathUpdateSyncS2CPacket(path, type));
    }


    public static void markChunksChanged(ClientLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);
    }

    private static void syncGridChunkWithPlayer(Level world, ChunkPos chunkPos, Player player, GridSyncPacketType type) {

        GlobalTransferGrid grid = GlobalTransferGrid.of(world);
        final Set<GridClientEdge> edgesToSend = new HashSet<>();

        // TODO this is slow as balls
        if(!(player instanceof ServerPlayer sPlayer)) return;
        for(LocalTransferGrid sys : grid.getSubgrids()) {
            for(GridVertex vert : sys.allVerts()) {
                if(chunkPos.equals(new ChunkPos(vert.getID().getBlockPos()))) {
                    for(GridEdge edge : vert.links)
                        edgesToSend.add(edge.toLightweight());
                }
            }
        }

        for(GridClientEdge edge : edgesToSend) 
            MechanoPackets.sendToClient(new GridEdgeUpdateSyncS2CPacket(type, edge), sPlayer);
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
            grid.getPathManager().forEachPath(
                path -> {
                    MechanoPackets.sendToClient(new GridPathUpdateSyncS2CPacket(
                        path, GridSyncPacketType.ADD_NEW), player);
                }
            );
        }
    }
}
