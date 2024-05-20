package com.quattage.mechano.foundation.electricity;

import java.util.List;

import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
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
     * @param builder The NodeBuilder to add connections to
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

    public void syncInGrid() {
        if(batteryBank == null) return;
        if(anchors.isEmpty()) return;
        anchors.sync();
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

    @Override
    public AABB getRenderBoundingBox() {
        if(anchors.isAwaitingConnection || GridClientCache.hasNewEdge(this)) 
            return AABB.ofSize(getBlockPos().getCenter(), Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        return super.getRenderBoundingBox();
    }

    @Override
    public void initialize() {
        super.initialize();
        this.anchors.initialize(getLevel());
    }
}
