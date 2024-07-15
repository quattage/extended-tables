package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.foundation.electricity.core.watt.WattSendSummary;
import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlock;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.AbstractConnectorBlock;
import com.quattage.mechano.foundation.block.SimpleOrientedBlock;
import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.helper.TimeTracker;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/***
 * Extending from WireAnchorBlockEntity grants the subclass access to 
 * all of the wire-connector related features of Mechano, as well as a fluent
 * builder to set up properties and settings for this WBE.
 */
public abstract class WireAnchorBlockEntity extends ElectricBlockEntity {

    private final AnchorPointBank<WireAnchorBlockEntity> anchors;

    private final TimeTracker wireTickTracker = new TimeTracker();
    private final TimeTracker anchorTickTracker = new TimeTracker();

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public WireAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
        AnchorBankBuilder<WireAnchorBlockEntity> init = new AnchorBankBuilder<WireAnchorBlockEntity>().at(this);
        createWireNodeDefinition(init);
        anchors = init.build();
    }

    /***
     * Prepare this ElectricBlockEntity instance with all of its associated properties.
     * <pre>
        anchorPointBank
        .newNode()            // build a new node
            .at(0, 6, 11)     // define the pixel offset of the node
            .connections(2)   // this node can connect to up to two other nodes
            .build()          // finish building this node
        .newNode()            // build a new node
            .at(16, 10, 6)    // define the pixel offset of the node
            .connections(2)   // this node can connect to up to two other nodes
            .build()          // finish building this node
        ;
     * </pre>
     * @param builder The AnchorBankBuilder to add connections to
     */
    public abstract void createWireNodeDefinition(AnchorBankBuilder<WireAnchorBlockEntity> builder);

    public AnchorPointBank<?> getAnchorBank() {
        return anchors;
    }

    @Override
    public void tick() {
        if(getLevel().isClientSide()) return;
        tickPaths();
        super.tick();
    }

    @Override
    public void reOrient(BlockState state) {
        anchors.reflectStateChange(state);
        super.reOrient(state);
    }

    @Override
    public void remove() {
        anchors.destroy();
        super.remove();
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    public void stepMode() {
        setMode(getWattBatteryHandler().getMode().next());
    }

    public boolean setMode(ExternalInteractMode mode) {
        if(getWattBatteryHandler().getMode() != mode) {
            getWattBatteryHandler().setMode(mode);
            anchors.sync(getLevel());
            return true;
        }

        return false;
    }

    public double getTimeSinceLastTick() {
        if(getLevel().isClientSide()) return anchorTickTracker.getDeltaTime();
        return 0;
    }

    public double getTimeSinceLastRender() {
        if(getLevel().isClientSide()) return wireTickTracker.getDeltaTime();
        return 0;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if(anchors.isAwaitingConnection || GridClientCache.hasNewEdge(this)) 
            return AABB.ofSize(getBlockPos().getCenter(), Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        return super.getRenderBoundingBox();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        lang().text(getWattBatteryHandler().getEnergyHolder().getStoredWatts() + " watts").forGoggles(tooltip);
        lang().text("Mode - " + battery.getMode()).style(ChatFormatting.GRAY).forGoggles(tooltip);

        if(getBlockState().getBlock() instanceof AbstractConnectorBlock) {
            BlockState adjState = getLevel().getBlockState(getBlockPos().relative(getBlockState().getValue(SimpleOrientedBlock.ORIENTATION).getCardinal().getOpposite()));
            if(adjState.getBlock() instanceof SlipRingShaftBlock) {

                MechanoClient.ANCHOR_SELECTOR.tenaciousTerriblyTemporaryTickingTracker.tickTimer();

                if(MechanoClient.ANCHOR_SELECTOR.tenaciousTerriblyTemporaryTickingTracker.isFulfilled()) {
                    lang().text("").forGoggles(tooltip);
                    lang().text("").forGoggles(tooltip);
                    lang().text("hi").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).forGoggles(tooltip);
                    lang().text("P.S. the floating connector is temporary").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).forGoggles(tooltip);
                    lang().text("have fun :)").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).forGoggles(tooltip);
                }
            }
        }

        return true;
    }

    private void tickPaths() {
        if(getLevel().isClientSide()) return;
        if(!battery.getEnergyHolder().canExtract()) return;
        
        for(AnchorPoint anchor : anchors.getAll()) {
            GridVertex participant = anchor.getParticipant();
            if(participant == null) continue;
            sendWattsAcross(participant.getConnectedPaths());
        }
    }

    /**
     * Sends watts through the given set of paths.
     * @param paths Paths that watts will follow - Can be acquired in the BE context via <code>participant.getConnectedPaths()</code>
     */
    public void sendWattsAcross(final Set<GridPath> paths) {


        if(paths == null || paths.isEmpty()) return;
        final List<WattSendSummary> sends = new ArrayList<>();
        GridVertex startVert = null;

        for(GridPath path : paths) {
            if(startVert == null)
                startVert = path.getStart();
            else if(!startVert.getID().getBlockPos().equals(this.getBlockPos())) {
                throw new IllegalStateException("Error ticking path (" + path + ") at GridVertex " + startVert.getID() 
                    + " - This vertex's path set has conflicting start points! (Bad data has populated this LocalTransferGrid, you may need to reset)");
            }

            final GridVertex endVert = path.getEnd();
            if(startVert.getID().getBlockPos().equals(endVert.getID().getBlockPos())) {
                throw new IllegalStateException("Error ticking path (" + path 
                    + ") - Start and ending positions are the same! (Bad data has populated this LocalTransferGrid, you may need to reset))");
            }

            if(!startVert.canFormPathTo(endVert)) continue;

            WattBatteryHandler<?> destination = endVert.getHost().battery;
            if(destination != null) {
                sends.add(
                    new WattSendSummary(
                        destination, this.getBlockPos(), 
                        endVert.getID().getBlockPos(), 
                        path
                    )
                );
            }
        }

        if(sends.isEmpty()) return;
        battery.distributeWattsTo(sends);
    }

    @Override
    public void initialize() {
        super.initialize();
        anchors.initialize(getLevel());
    }
}
