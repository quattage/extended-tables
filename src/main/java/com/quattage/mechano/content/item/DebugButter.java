package com.quattage.mechano.content.item;

import java.util.ArrayList;
import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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

        GlobalTransferGrid grid = GlobalTransferGrid.of(world);
        Mechano.log("ALL: " + grid);

        String message = "";
        if(confirmed == 0) {
            message = "\n§cYou are about to wipe all data from §l" + grid.getSubsystemCount() + "§r§c subgrids. §r§7This will destroy all connectors and wires in the world.\n§bAre you sure? §r§7(r-click again to confirm)";
            confirmed = 1;
        } 
        else if(confirmed == 1) {
            message = "\n§cAre you §lAbsolutely sure? §r§7(r-click again to confirm)";
            confirmed = 2;
        } 
        else if(confirmed == 2) {

            int invalids = 0;

            for(LocalTransferGrid subgrid : grid.getSubgrids()) {
                if(subgrid == null) {
                    invalids++;
                    continue;
                }
                for(GridVertex vert : subgrid.allVerts()) {
                    if(!(world.getBlockEntity(vert.getID().getBlockPos()) instanceof WireAnchorBlockEntity wbe))
                        continue;
                    world.destroyBlock(wbe.getBlockPos(), true);
                    vert = null;
                }
            }
            grid.getSubgrids().clear();
            message = "\n§c§l Wiped all data!";
            if(invalids > 0) message += " - §aremoved §l" + invalids + " §r§ainvalid subsgrids!";
            confirmed = 0;
        }

        event.getEntity().displayClientMessage(Component.literal(message), false);
    }


    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity entity, int pSlotId, boolean pIsSelected) {
        super.inventoryTick(pStack, pLevel, entity, pSlotId, pIsSelected);

        if(!(entity instanceof Player player)) return;
        if(!pIsSelected && confirmed != 0) {
            confirmed = 0;
            player.displayClientMessage(Component.literal("\n§r§7Global grid wipe cancelled."), false);
        }
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {

        Player guy = context.getPlayer();

        if(context.getLevel().isClientSide()) return InteractionResult.PASS;
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof WireAnchorBlockEntity wbe)) return InteractionResult.PASS;

        if(!guy.isCrouching()) {

            int count = 0;
            final List<Boolean> forced = new ArrayList<>();
            final List<LocalTransferGrid> grids = new ArrayList<>();
            final GlobalTransferGrid network = GlobalTransferGrid.of(context.getLevel());

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

                message += "§r§7\n     -   Aquisition mode: §r§b§3" + (force ? " iterative brute force" : " cache retrieval") + ", §r§7(global subgrid §r§b§3" + (network.getSubgrids().indexOf(sub) + 1) + "§r§7 of §r§b§3" + (network.getSubsystemCount()) + "§r§7)";
                message += "§r§7\n     -   Composition: §r§b§3" + sub.allVerts().size() + " §r§7vertices, §r§b§3" + sub.allEdges().size() + " §r§7edges, §r§b§3"  + sub.allPaths().size() +  " §r§7paths";
                message += "§r§7\n     -   Owner: '§r§b§3" + sub.getParent().getDimensionName() + "§r§7'";
                message += "§r§7\n     -   §r§b§9Unordered Vertex Info: §r§7" + sub.toFormattedString(context.getClickedPos());
            }

            guy.displayClientMessage(Component.literal(message), false);

        } else {
            wbe.getWattBatteryHandler().cycleMode();
            context.getPlayer().displayClientMessage(Component.literal("MODE: " + wbe.getWattBatteryHandler().getMode()), true);
        }

        return InteractionResult.PASS;
    }
}
