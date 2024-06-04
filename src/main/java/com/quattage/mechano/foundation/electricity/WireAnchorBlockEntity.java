package com.quattage.mechano.foundation.electricity;

import java.util.List;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.client.GridClientVertex;
import com.quattage.mechano.foundation.network.AnchorStatRequestC2SPacket;
import com.quattage.mechano.foundation.network.AnchorStatSummaryS2CPacket;
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
    
    private long oldTime = 0;
    private static double time = 0;

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

    public double getDelta(long time) {
        if(oldTime == 0) oldTime = System.nanoTime();
        double out = (time - oldTime) * 0.000001f;
        oldTime = time;
        return out;
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

    @Override
    public AABB getRenderBoundingBox() {
        if(anchors.isAwaitingConnection || GridClientCache.hasNewEdge(this)) 
            return AABB.ofSize(getBlockPos().getCenter(), Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        return super.getRenderBoundingBox();
    }

    protected void requestClientUpdate(boolean immediate) {
        if((time < MechanoSettings.ANCHOR_OBSERVE_RATE * 50) && (!immediate))
            time += getDelta(System.nanoTime());
        else {
            MechanoPackets.sendToServer(new AnchorStatRequestC2SPacket(getBlockPos()));
            time = 0;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        AnchorStatSummaryS2CPacket summary = AnchorStatSummaryS2CPacket.getAwaiting();
        if(summary == null || (!summary.getBlockPos().equals(this.getBlockPos()))) {
            requestClientUpdate(true);
            lang().text("...").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return true;
        }

        requestClientUpdate(false);

        lang().text("Mode - " + getWattBatteryHandler().getMode()).style(ChatFormatting.GRAY).forGoggles(tooltip);

        for(int x = 0; x < summary.getVertices().length; x++) {

            AnchorPoint anchor = getAnchorBank().get(x);
            GridClientVertex vert = summary.getVertices()[x];

            lang().text("Anchor " + (anchor.getID().getSubIndex() + 1) + ": ").style(ChatFormatting.GRAY).forGoggles(tooltip);
            lang().text("    member: " + vert.isMember()).style(ChatFormatting.GRAY).forGoggles(tooltip);
            lang().text("    f: " + vert.getF()).style(ChatFormatting.GRAY).forGoggles(tooltip);
            lang().text("    heuristic: " + vert.getHeuristic()).style(ChatFormatting.GRAY).forGoggles(tooltip);
            lang().text("    cumulative: " + vert.getCumulative()).style(ChatFormatting.GRAY).forGoggles(tooltip);
            lang().text("    connections: " + vert.getConnections() + "/" + anchor.getMaxConnections()).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
        return true;
    }

    @Override
    public void initialize() {
        super.initialize();
        anchors.initialize(getLevel());
    }
}
