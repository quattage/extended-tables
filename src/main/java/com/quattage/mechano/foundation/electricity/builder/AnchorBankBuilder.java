package com.quattage.mechano.foundation.electricity.builder;

import java.util.ArrayList;

import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorTransform;
import com.quattage.mechano.foundation.electricity.power.features.GID;

import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A fluent builder for AnchorPointBanks
 */
public class AnchorBankBuilder<T extends BlockEntity> {
    private T target = null;
    
    private final ArrayList<AnchorPoint> nodesToAdd = new ArrayList<AnchorPoint>();

    public AnchorBankBuilder() {};

    /***
     * Bind this NodeBank to a given BlockEntity.
     * @param target BlockEntity to bind this NodeBank to
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> at(T target) {
        this.target = target;
        return this;
    }

    protected T getTarget() {
        return target;
    }

    /***
     * Adds a new ElectricNode to this NodeBank
     * @return
     */
    public AnchorPointBuilder newNode() {
        return new AnchorPointBuilder(this);
    }

    protected AnchorBankBuilder<T> add(AnchorTransform transform, int maxConnections) {
        return add(new AnchorPoint(transform, new GID(target.getBlockPos(), nodesToAdd.size()), maxConnections));
    }

    private AnchorBankBuilder<T> add(AnchorPoint node) {
        nodesToAdd.add(node);
        return this;
    }

    private void doCompleteCheck() {
        if(target == null) throw new IllegalStateException("NodeBank cannot be built - BlockEntity target is null. (Use .at() during construction)");
        if(nodesToAdd.isEmpty()) throw new IllegalStateException("NodeBank cannot be built - Must contain at least 1 ElectricNode (Use .newNode to add a node)");
    }

    /***
     * Finalizes all changes made to this AnchorPointBankBuilder and returns
     * a new AnchorPointBank. Use this after you've set up all your values.
     * @throws IllegalStateException If you haven't yet configured a BlockEntity target
     * (see {@link #at(BlockEntity) at()})
     * @return a NodeBank instance.
     */
    public AnchorPointBank<T> build() {
        doCompleteCheck();
        return new AnchorPointBank<T>(target, nodesToAdd);
    }
}
