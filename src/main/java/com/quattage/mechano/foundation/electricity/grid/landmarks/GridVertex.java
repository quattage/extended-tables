package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.DirectionalWattProvidable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.network.AnchorStatRequestC2SPacket;
import com.quattage.mechano.foundation.network.AnchorStatSummaryS2CPacket;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A GridVertex is a logical representation of a connection point within a LocalTransferGrid.
 * Where GridEdges represent the wires themselves, a GridVertex can be thought of as a connector.
 */
public class GridVertex {

    public static final VertexComparator GUIDANCE_COMPARATOR = new VertexComparator();

    // Grid architecture
    @Nullable
    private LocalTransferGrid parent;
    @Nullable
    private final WireAnchorBlockEntity host;
    
    private final GID id;

    // A* guidance variables
    private float f = 0;
    private float heuristic = 0;
    private float cumulative = Float.MAX_VALUE;
    
    // functional variables
    private boolean isMember = false;
    public final LinkedList<GridVertex> links = new LinkedList<GridVertex>();

    public GridVertex(WireAnchorBlockEntity wbe, LocalTransferGrid parent, GID id) {
        this.parent = parent;
        this.id = id;
        this.host = wbe;
    }

    public GridVertex(Level world, LocalTransferGrid parent, GID id) {
        this.parent = parent;
        this.id = id;
        this.isMember = false;

        BlockEntity be = world.getBlockEntity(id.getBlockPos());
        if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
        else this.host = null;
    }

    public GridVertex(Level world, LocalTransferGrid parent, CompoundTag in) {
        this.parent = parent;
        this.id = GID.of(in);
        this.isMember = in.getBoolean("m");

        readLinks(in.getList("l", Tag.TAG_COMPOUND), world);
        if(!isEmpty()) {
            BlockEntity be = world.getBlockEntity(id.getBlockPos());
            if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
            else this.host = null;
        } else this.host = null;
    }

    public CompoundTag writeTo(CompoundTag in) {
        id.writeTo(in);
        in.putBoolean("m", this.isMember);
        in.put("l", writeLinks());
        return in;
    }

    private ListTag writeLinks() {
        ListTag out = new ListTag();
        for(GridVertex v : links) {
            CompoundTag coord = new CompoundTag();
            v.id.writeTo(coord);
            out.add(coord);
        }
        return out;
    }

    public void readLinks(ListTag list, Level world) {
        for(int x = 0; x < list.size(); x++) {
            GID id = GID.of(list.getCompound(x));
            GridVertex target = getOrFindParent().getOrCreateOnLoad(world, id);
            if(!links.contains(target))
                this.linkTo(target);
            if(!target.links.contains(this))
                target.linkTo(this);
        }
    }

    /**
     * Syncs this <code>GridVertex</code> to its coorresponding 
     * {@link AnchorPoint <code>AnchorPoint</code>}. This GridVertex's parent BlockEntity <strong>must</strong> be accessible when invoked.
     * For simplified syncing before BE capabilities are loaded, see {@link GridVertex#doSimplifiedSync() <code>doSimplifiedSync()</code>}.
     * @param repath If <code>TRUE</code> this method will instruct the parent {@link LocalTransferGrid <code>LocalTransferGrid</code>} to 
     * find new paths to this GridVertex, only if the status of this GridVertex was changed as a result of this call.
     * @throws NullPointerException if for any reason no cooresponding <code>AnchorPoint</code> could be found
     */
    public void doFullSync(boolean repath) {

        if(hasNoHost()) throw new IllegalStateException("Could not fully sync GridVertex at  " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");

        if(host.getWattBatteryHandler().isInteractingExternally()) {
            getCoorespondingAnchor().setParticipant(this);
            if(!isMember()) {
                setMemberStatus(true);
                // TODO other such goodness
            }

            if(repath) getOrFindParent().pathfindFrom(this, true);

        } else {
            getCoorespondingAnchor().nullifyParticipant();
            if(isMember()) {
                setMemberStatus(false);
                if(!isEmpty()) getOrFindParent().removePathsEndingIn(this);
            }
        }
    }

    /**
     * Syncs this <code>GridVertex</code> to its cooresponding {@link AnchorPoint <code>AnchorPoint</code>}
     * without accessing BlockEntity capabilities. This method is designed to be invoked during the world loading
     * process. For any other case, use {@link GridVertex#doFullSync(boolean) <code>syncToHostBE()</code>}.
     */
    public void doSimplifiedSync() {

        if(hasNoHost()) throw new IllegalStateException("Could not perform simplified sync on GridVertex at  " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");

        if(isMember()) {
            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(id)) 
                    anchor.setParticipant(this);
            }
        } else {
            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(id)) 
                    anchor.nullifyParticipant();
            }
        }
    }

    /**
     * Syncs this <code>GridVertex</code> to its cooresponding {@link AnchorPoint <code>AnchorPoint</code>}
     * without accessing BlockEntity capabilities. This method is designed to be invoked during the world loading
     * process. For any other case, use {@link GridVertex#doFullSync(boolean) <code>syncToHostBE()</code>}.
     */
    public void markRemoved() {
        if(hasNoHost()) throw new IllegalStateException("Error marking GridVertex for removal at " + id + " - This GridVertex has already been marked for removal!");
        for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
            if(anchor.getID().equals(id)) 
                anchor.nullifyParticipant();
        }
        AnchorStatSummaryS2CPacket.resetAwaiting();
    }

    /**
     * Broadcasts chunk changes to the client for every chunk containing GridVertices linked 
     * to this GridVertex.
     */
    public void refreshLinkedChunks() {
        final Set<BlockPos> visited = new HashSet<>(11);
        for(GridVertex vert : links) {
            if(!vert.isAt(id))
                visited.add(vert.id.getBlockPos());
        }

        for(BlockPos pos : visited) 
            MechanoPackets.sendToAllClients(new AnchorStatRequestC2SPacket(pos));
    }

    public boolean canFormPathTo(GridVertex o) {
        if(!this.isMember()) return false;
        if(!o.isMember()) return false;
        if(this.host == null || o.host == null) return false;
        if(this.equals(o)) return false;
        return getHostCapabilityMode().isCompatableWith(o.getHostCapabilityMode());
    }

    /**
     * Finds and returns this <code>GridVertex</code>'s cooresponding {@link AnchorPoint <code>AnchorPoint</code>}.
     * The <code>AnchorPoint</code> is required for creating a logical link between this <code>GridVertex</code> and its physicalized in-world
     * location.
     * @return The AnchorPoint at this <code>GridVertex</code>'s {@link GID <code>GID</code>}
     */
    public AnchorPoint getCoorespondingAnchor() {
        if(hasNoHost()) throw new IllegalStateException("Error getting cooresponding AnchorPoint for GridVertex at " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");
        if(host.getAnchorBank() == null) return null;
        return host.getAnchorBank().get(id.getSubIndex());
    }

    /**
     * Calculates a heuristic value for A* guidance based on euclidean distance and store it in this GridVertex.
     * Whenever possible, implementations should use {@link GridVertex#getAndStoreHeuristic(GridEdge) the overload } instead.
     * @param other GridVertex to calculate heuristic with
     * @return The Euclidian distance between this vertex and the given vertex
     */
    public float getAndStoreHeuristic(GridVertex other) {
        BlockPos a = id.getBlockPos();
        BlockPos b = other.id.getBlockPos();
        this.heuristic = (float)Math.sqrt(Math.pow(a.getX() - b.getX(), 2f) + Math.pow(a.getY() - b.getY(), 2f) + Math.pow(a.getZ() - b.getZ(), 2f));
        this.f = this.cumulative + this.heuristic;
        return this.heuristic;
    }

    /**
     * Gets the pre-computed heuristic value from the provided GridEdge and store it in this GridVertex.
     * @param other GridEdge to grab the pre-computed heuristic from
     * @return The Euclidian distance (length) of the given edge.
     */
    public float getAndStoreHeuristic(GridEdge edge) {
        this.heuristic = edge.getDistance();
        this.f = cumulative + heuristic;
        return this.heuristic;
    }

    /**
     * Gets the current heuristic value without modification. Not particularly useful for anything
     * other than debugging.
     * @see {@link GridVertex#getAndStoreHeiristic(Gridvertex)} for use with A* stuff
     */
    public float getStoredHeuristic() {
        return this.heuristic;
    }

    public float getF() {
        return this.f;
    }

    public float getCumulative() {
        return this.cumulative;
    }

    public void setCumulative(float g) {
        this.cumulative = g;
    }

    /**
     * Resets the F, heuristic, and cumulative values
     * to their defaults
     * @return this GridVertex, modified as a result of this call.
     */
    public GridVertex resetHeuristics() {
        this.f = 0;
        this.heuristic = 0;
        this.cumulative = Float.MAX_VALUE;
        return this;
    }

    /***
     * Generate a unique identifier for this GridVertex for storing in datasets
     * that require hashing.
     * @return a new GID object representing this GridVertex's BlockPos and SubIndex summarized as a String.
     */
    public GID getID() {
        return id;
    }

    public boolean isAt(BlockPos pos) {
        return id.getBlockPos().equals(pos);
    }

    public boolean isAt(GID id) {
        return this.id.equals(id);
    }

    /***
     * Adds a link from the given GridVertex to this GridVertex.
     * @param other Other GridVertex within the given LocalTransferGrid to add to this GridVertex
     * @return True if the list of connections within this GridVertex was changed.
     */
    public boolean linkTo(GridVertex other) {
        if(links.contains(other)) return false;
        return links.add(other);
    }

    /***
     * Removes the link from this GridVertex to the given GridVertex.
     * @param other
     * @returns True if this GridVertex was modified as a result of this call
     */
    public boolean unlinkFrom(GridVertex other) {
        return links.remove(other);
    }

    /***
     * Checks whether this GridVertex is linked to the given GridVertex.
     * @param other GridVertex to check for 
     * @return True if this GridVertex contains a link to the given GridVertex
     */
    public boolean isLinkedTo(GridVertex other) {
        return this.links.contains(other);
    }

    protected void unlinkAll() {
        links.clear();
    }

    /***
     * Checks whether this GridVertex has any links attached to it
     * @return True if this GridVertex doesn't contain any links
     */
    public boolean isEmpty() {
        if(links == null) return true;
        return links.isEmpty();
    }

    public String toString() {
        String sig = isMember ? "Type - 'M'" : "Type - 'A'";
        String mode = "Mode - '" + getHostCapabilityMode().toString() + "'";
        // String content = "at " + posAsString() + ", ---> ";
        // for(int x = 0; x < links.size(); x++) {
        //     GridVertex connectionTarget = links.get(x);
        //     content += connectionTarget.posAsString();
        //     if(x < links.size() - 1)
        //         content += ", ";
        // }
        return "GridVertex {" + sig  + ", " + mode + ", At " + posAsString() + "}";
    }

    public String toFormattedString() {
        String sig = isMember ? "Type - §d§l'Member'§r§7" : "Type - §5§l'Actor'§r§7";
        String mode = "Mode - §r§l'" + getHostCapabilityMode().toString() + "' §r§7";
        return "§r§7(" + sig  + ", " + mode + ")\n                Host: " + posAsString() + "\n";
    }

    public String posAsString() {
        return id.toString();
    }

    /***
     * Equivalence comparison by position. Does not compare links to other nodes.
     */
    public boolean equals(Object other) {
        if(other instanceof GridVertex ov)
            return this.id.equals(ov.id);
        return false;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public void setMemberStatus(boolean isMember) {
        this.isMember = isMember;
    }

    public boolean isMember() {
        return isMember;
        // return !isEmpty() && isMember;
    }

    public ExternalInteractMode getHostCapabilityMode() {
        if(hasNoHost()) throw new IllegalStateException("Error getting ExternalInteractMode for GridVertex at " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");
        return host.getWattBatteryHandler().getMode();
    }

    public LocalTransferGrid getOrFindParent() {
        if(parent == null) {
            if(hasNoHost()) throw new IllegalStateException("Error getting LocalTransferGrid parent for GridVertex at " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");
            Pair<Integer, LocalTransferGrid> sub = GlobalTransferGrid.of(host.getLevel()).getSystemContaining(id);
            if(sub == null) throw new NullPointerException("Error getting LocalTransferGrid parent for GridVertex at " + id + " - No LocalTransferGrid could be found containing a GridVertex at this ID!");
            parent = sub.getSecond();
        } 
        return parent;
    }

    public void replaceParent(LocalTransferGrid parent) {
        if(parent == null) throw new NullPointerException("Error replacing GridVertex parent - Parent cannot be set to a null value!");
        this.parent = parent;
        resetHeuristics();
    }

    /**
     * A GridVertex has no host if its parent BlockEntity is null. This can happen in two ways:
     * <li> <code>-</code> No BlockEntity could be found at this GridVertex's cooresponding <code>GID</code> block position. In this case, the constructor attempts to find a BE, but will leave this GridVertex in an invalid state if one cannot be found.
     * <li> <code>-</code> This GridVertex doesn't have at least one link during construction. In this case, the constructor never populates the links list. If the list is empty, the BE finding step is skipped, leaving it null.
     * <li> <strong>The host is a final field</strong> - GridVertices with no host must be properly invalidated and dumped.
     * @return <code>TRUE</code> if this GridVertex host is null.
     */
    public boolean hasNoHost() {
        return host == null;
    }

    // Used to sort the tentative vertex list during A* pathfinding to achieve a deterministic sorting order guided by a heuristic.
    private static class VertexComparator implements Comparator<GridVertex> {

        @Override
        public int compare(GridVertex vertA, GridVertex vertB) {
            if(vertA.f > vertB.f) return 1;
            if(vertA.f < vertB.f) return -1;

            BlockPos a = vertA.id.getBlockPos();
            BlockPos b = vertB.id.getBlockPos();
            if(a.getX() > b.getX()) return 1;
            if(a.getX() < b.getX()) return -1;
            if(a.getY() > b.getY()) return 1;
            if(a.getY() < b.getY()) return -1;
            if(a.getZ() > b.getZ()) return 1;
            if(a.getZ() < b.getZ()) return -1;
            return 0;
        }
        
    }
}