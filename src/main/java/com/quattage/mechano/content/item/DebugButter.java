package com.quattage.mechano.content.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.behavior.GridEdgeDebugBehavior;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.TransferPathManager;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.grid.network.GridPathUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncPacketType;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static com.quattage.mechano.Mechano.lang;

@EventBusSubscriber
public class DebugButter extends Item {

    private static byte confirmed = 0;
    private static boolean hasMask = false;

    public DebugButter(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, world, tooltip, isAdvanced);
        lang().text("yum").style(ChatFormatting.GRAY).addTo(tooltip);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {

        if(!(event.getItemStack().getItem() instanceof DebugButter)) return;

        event.setCanceled(true);
        Level world = event.getLevel();
        if(world.isClientSide) return;

        BlockPos clicked = event.getPos();
        if(event.getLevel().getBlockEntity(clicked) instanceof WireAnchorBlockEntity wbe) {
            wbe.getWattBatteryHandler().cycleMode();
            return;
        }

        GlobalTransferGrid grid = GlobalTransferGrid.of(world);

        String message = "";
        if(confirmed == 0) {
            message = "\n§cYou are about to wipe all data from §l" + grid.getSubsystemCount() + "§r§c subgrids. §r§7This will destroy all connectors and wires in the world.\n§bAre you sure? §r§7(l-click again to confirm)";
            confirmed = 1;
        } 
        else if(confirmed == 1) {
            message = "\n§cAre you §lAbsolutely sure? §r§7(l-click again to confirm)";
            confirmed = 2;
        } 
        else if(confirmed == 2) {

            Set<BlockPos> invalids = new HashSet<BlockPos>();

            for(BlockPos vertPos : grid.collectAllVertexPositions()) {
                if(world.getBlockEntity(vertPos) instanceof WireAnchorBlockEntity)
                    world.destroyBlock(vertPos, true);
                else 
                    invalids.add(vertPos);
            }


            int invalidCount = 0;
            for(BlockPos vertPos : invalids) 
                invalidCount += grid.removeAllVertsAt(vertPos);

            grid.clear();
            message = "\n§c§l Wiped all data!";
            MechanoPackets.sendToAllClients(new GridPathUpdateSyncS2CPacket(null, GridSyncPacketType.CLEAR));
            if(invalidCount > 0) message += " - §aremoved §l" + invalids + " §r§ainvalid subsgrids!";
            Mechano.LOGGER.warn("Network was wiped, here's the resulting NBT: " + grid.writeTo(new CompoundTag()));
            confirmed = 0;
        }

        event.getEntity().displayClientMessage(Component.literal(message), false);
    }


    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity entity, int pSlotId, boolean pIsSelected) {
        super.inventoryTick(pStack, pLevel, entity, pSlotId, pIsSelected);

        if(!(entity instanceof ServerPlayer player)) return;
        if(!pIsSelected) {
            if(hasMask == true) {
                GridEdgeDebugBehavior.setMask(pLevel, null, player);
                hasMask = false;
            }

            if(confirmed != 0) {
                confirmed = 0;
                player.displayClientMessage(Component.literal("\n§r§7Global grid wipe cancelled."), false);
            }
        }
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {

        Player guy = context.getPlayer();
        if(context.getLevel().isClientSide()) 
            return InteractionResult.PASS;

        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof WireAnchorBlockEntity wbe)) 
            return InteractionResult.PASS;

        final GlobalTransferGrid network = GlobalTransferGrid.of(context.getLevel());

        if(!guy.isCrouching()) {

            int count = 0;
            final List<Boolean> forced = new ArrayList<>();
            final List<LocalTransferGrid> grids = new ArrayList<>();

            for(int x = 0; x <  wbe.getAnchorBank().size(); x++) {

                AnchorPoint anchor = wbe.getAnchorBank().get(x);

                GridVertex vert = anchor.getParticipant();
                if(vert == null) {
                    vert = network.getVertAt(anchor.getID());
                    forced.add(true);
                } else forced.add(false);

                if(vert != null) {
                    grids.add(vert.getOrFindParent());
                    count++;
                }
                else grids.add(null);
            }

            String message = "\n§7-----§r  Displaying data from §l§a" + count + "§r LocalTransferGrid(s):  §7-----§r";
        
            for(int x = 0; x < grids.size(); x++) {
                message += "\n\n§r§7  • Grid §r§b§3" + (x + 1) + "§r§7:";

                boolean force = forced.get(x);
                LocalTransferGrid sub = grids.get(x);

                if(sub == null) {
                    message += "  §c§lNo data §r§c(null)";
                    continue;
                }

                Mechano.log(sub);

                message += "§r§7\n     -   Aquisition mode: §r§b§3" + (force ? " iterative brute force" : " cache retrieval") + ", §r§7(global subgrid §r§b§3" + (network.getSubgrids().indexOf(sub) + 1) + "§r§7 of §r§b§3" + (network.getSubsystemCount()) + "§r§7)";
                message += "§r§7\n     -   Composition: §r§b§3" + sub.allVerts().size() + " §r§7vertices, §r§b§3";

                final TransferPathManager paths = sub.getPathManager();
                final int declaringCount = paths.getDeclaratorCount();
                final int expected = declaringCount * (declaringCount - 1);
                final int actual = paths.getActualPathCount();
                final String pathFilled = (expected == actual ? ("§r§a§l" + expected + "§r§a/§r§a§l" + expected) : ("§r§c§l"+ actual + "§r§c/§r§c§l" + expected));

                message += "§r§7\n     -   Paths: §r§b§3" + paths.getUniquePathCount() + " §r§7unique, §r§b§3" + declaringCount + " §r§7declarators, " + pathFilled + " §r§7total";
                message += "§r§7\n     -   Owner: '§r§b§3" + sub.getParent().getDimensionName() + "§r§7'";
                message += "§r§7\n     -   §r§b§9Unordered Vertex Info: §r§7" + sub.toFormattedString(context.getClickedPos());
            }

            guy.displayClientMessage(Component.literal(message), false);

        } else {

            if(!(context.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.PASS;

            final Set<GID> ids = new HashSet<>();
            for(AnchorPoint anchor : wbe.getAnchorBank().getAll()) {
                GridVertex vert = anchor.getParticipant();
                if(vert == null) vert = network.getVertAt(anchor.getID());
                if(vert == null) continue; 
                ids.add(vert.getID());
            }

            if(ids.size() == 0) {
                GridEdgeDebugBehavior.setMask(context.getLevel(), null, sp);
                hasMask = false;
            }
            else {
                GridEdgeDebugBehavior.setMask(context.getLevel(), ids, sp);
                hasMask = true;
            }
        }

        return InteractionResult.PASS;
    }
}
