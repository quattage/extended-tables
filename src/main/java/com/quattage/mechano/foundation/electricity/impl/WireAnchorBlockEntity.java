
package com.quattage.mechano.foundation.electricity.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joml.Vector3f;

import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.WattBatteryHandler;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.watt.WattSendSummary;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;
import com.quattage.mechano.foundation.helper.TimeTracker;
import com.quattage.mechano.foundation.helper.builder.AnchorBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private final ChevronTransform[] chevrons;
    private final TimeTracker wireTickTracker = new TimeTracker();
    private final TimeTracker anchorTickTracker = new TimeTracker();
    private float currentArrowRot = 0;
    private float prevArrowRot = 0;

    public WireAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
        AnchorBankBuilder<WireAnchorBlockEntity> init = new AnchorBankBuilder<WireAnchorBlockEntity>().at(this);
        createWireNodeDefinition(init);
        List<ChevronTransform> chevrons = addChevronLocations(new ArrayList<ChevronTransform>());
        this.chevrons = chevrons.toArray(new ChevronTransform[chevrons.size()]);
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

    public ChevronTransform[] getChevronLocations() {
        return chevrons;
    }

    @Override
    public void initialize() {
        super.initialize();
        anchors.initialize(getLevel());
    }

    public List<ChevronTransform> addChevronLocations(List<ChevronTransform> locations) {
        locations.add(new ChevronTransform(8, 2, 2, Relative.FRONT));
        locations.add(new ChevronTransform(2, 2, 8, Relative.LEFT));
        locations.add(new ChevronTransform(8, 2, 14, Relative.BACK));
        locations.add(new ChevronTransform(14, 2, 8, Relative.RIGHT));
        return locations;
    }

    public float getPrevRotation() {
        return prevArrowRot;
    }

    public float getCurrentRotation() {
        return currentArrowRot;
    }

    public float getNextRotation() {
        prevArrowRot = currentArrowRot;

        if(battery.getMode() == ExternalInteractMode.PULL_IN) {
            if(currentArrowRot < 1)
                currentArrowRot += 0.009f;
            else currentArrowRot = 1;
        } else if(battery.getMode() == ExternalInteractMode.PUSH_OUT) {
            if(currentArrowRot > 0)
                currentArrowRot -= 0.009f;
            else currentArrowRot = 0;
        }

        return currentArrowRot;
    }

    public void resetChevronRotation() {
        float mod = 0;
        if(!battery.canChangeMode) mod = 0.2f;
        if(battery.getMode() == ExternalInteractMode.PULL_IN)
            currentArrowRot = 0 + mod;
        else if(battery.getMode() == ExternalInteractMode.PUSH_OUT)
            currentArrowRot = 1 - mod;
        prevArrowRot = currentArrowRot;
    }

    @Override
    public void onAwaitingModeChange() {
        resetChevronRotation();
    }

    public boolean hasRotation() {
        return currentArrowRot < 1 && 0 > currentArrowRot;
    }

    public class ChevronTransform {
        private final Vector3f offset;
        private final RelativeDirection facing;

        public ChevronTransform(int x, int y, int z, Relative facing) {
            this.offset = new Vector3f(x, y, z).mul(0.0625f);
            this.facing = new RelativeDirection(facing);
        }

        public Direction getRotated(CombinedOrientation orient) {
            return facing.rotate(orient).get();
        }

        public Direction getDefault() {
            return facing.getRaw().getDefaultDir();
        }

        public Vector3f getOffset() {
            return offset;
        }
    }
}
