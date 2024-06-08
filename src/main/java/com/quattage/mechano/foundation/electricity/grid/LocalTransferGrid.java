package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GIDPair;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.grid.sync.GridSyncHelper;
import com.quattage.mechano.foundation.electricity.grid.sync.GridSyncPacketType;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.utility.Color;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/***
 * The LocalTransferGrid provides the framework for managing, caching, finding, and updating GridVertices, GridPaths, and GridEdges.
 * <p>
 * For clarification: 
 *    - GridVertex -> Connector, or a wire source/destination
 *    - GridEdge -> The wire itself
 *    - GridPath -> A chain of wires connecting a power source to a power consumer
 * <p>
 * This object stores GridVertex objects in a Map, forming an undirected graph / adjacency list.
 * More info on graph theory: https://en.wikipedia.org/wiki/Adjacency_list
 * and also: https://en.wikipedia.org/wiki/A*_search_algorithm
 */
public class LocalTransferGrid {

    private Map<GID, GridVertex> vertMatrix = new Object2ObjectOpenHashMap<>();
    private Map<GIDPair, GridEdge> edges = new Object2ObjectOpenHashMap<>();
    private Map<GIDPair, GridPath> paths = new Object2ObjectOpenHashMap<>();

    private final GlobalTransferGrid parent;

    /***
     * Instantiates a blank LocalTransferGrid
     */
    public LocalTransferGrid(GlobalTransferGrid parent) {
        this.parent = parent;
    }

    /***
     * Creates a new shallow-copied LocalTransferGrid which contains the contents of both provided LocalTransferGrids. <p>
     * <strong>Important note:</strong> The resulting grid will contain discontinuities, as there are no inferred connections
     * between the two provided grids. Discontinuities <strong>must</strong> be dealt with by ASAP creating at least one 
     * connection between the disconnected clusters within the resulting LocalTransferGrid object.
     * @param parent The GlobalTransferGrid parent (usually the object that is initiating this merge)
     * @param sync If <code>TRUE</code>, all GridVertices will be synced to their host BlockEntities before being added to the resulting LocalTransferGrid. This is rather expensive, and shouldn't be used very often.
     * @param grids Any amount of LocalTransferGrids to pull matrix values from - The resulting grid will contain all vertices and edges, but no paths.
     * @return a new LocalTransferGrid object.
     */
    public static LocalTransferGrid ofMerged(GlobalTransferGrid parent, boolean sync, LocalTransferGrid... grids) {
        LocalTransferGrid out = new LocalTransferGrid(parent);
        
        for(LocalTransferGrid grid : grids) {
            for(GridVertex vert : grid.allVerts()) {
                vert.replaceParent(out);
                if(sync) vert.doFullSync(false);
                out.addVert(vert);
            }
            for(GridEdge edge : grid.allEdges())
                out.edges.put(edge.getHashable(), edge);
            for(GridPath path : grid.allPaths())
                out.addPath(path);
        }

        return out;
    }

    /***
     * Populates this LocalTransferGrid with members of a list
     * No pathfinding is performed as a result of this call, that must be done seperately
     * @param cluster ArrayList of GridVertexs to add to this LocalTransferGrid upon creation
     */
    public LocalTransferGrid(GlobalTransferGrid parent, ArrayList<GridVertex> cluster, Map<GIDPair, GridEdge> edgeMatrix) {
        this.parent = parent;
        for(GridVertex vert : cluster) {
            vert.replaceParent(this);
            vertMatrix.put(vert.getID(), vert);
        }
        this.edges = ((Object2ObjectOpenHashMap<GIDPair, GridEdge>)edgeMatrix).clone();
    }

    public LocalTransferGrid(GlobalTransferGrid parent, CompoundTag in, Level world) {
        this.parent = parent;
        ListTag net = in.getList("nt", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {

            CompoundTag vertC = net.getCompound(x);
            GridVertex vertToAdd = getVertAt(GID.of(vertC));

            // if the vertex has already been added to this grid (by the next step) then make sure its links are up to date
            if(vertToAdd != null) {
                vertToAdd.setMemberStatus(vertC.getBoolean("m"));
                vertToAdd.readLinks(vertC.getList("l", Tag.TAG_COMPOUND), world);
                continue;
            }

            // if the vertex isn't in this grid, new it and add it. The GridVertex constructor also adds new GridVertices
            // to describe its links, which is what the previous step will take care of.
            vertToAdd = new GridVertex(world, this, vertC);

            // if this GridVertex has bad data, don't add it
            if(vertToAdd.hasNoHost()) {
                Mechano.LOGGER.warn("LocalTransferGrid skipping registration of GridVertex at " + vertToAdd.getID());
                continue;
            }

            vertToAdd.doSimplifiedSync();
            vertMatrix.put(vertToAdd.getID(), vertToAdd);
        }

        // TODO this step can be skipped if the previous code is reworked a lil bit
        ListTag edgeList = in.getList("ed", Tag.TAG_COMPOUND);
        for(int x = 0; x < edgeList.size(); x++) {
            GridEdge edge = new GridEdge(this.parent, edgeList.getCompound(x));
            edges.put(edge.getHashable(), edge);
        }

        findAllPaths(false);
    }

    //  Write this LocalTransferGrid to NBT.  //
    protected CompoundTag writeTo(CompoundTag in) {
        in.put("nt", writeMatrix());
        in.put("ed", writeEdges());
        return in;
    }

    private ListTag writeMatrix() {
        ListTag out = new ListTag();
        for(GridVertex v : vertMatrix.values())
            out.add(v.writeTo(new CompoundTag()));
        return out;
    }

    private ListTag writeEdges() {
        ListTag out = new ListTag();
        for(GridEdge edge : edges.values())
            out.add(edge.writeTo(new CompoundTag()));
        return out;
    }
    ////////////////////////////////////////////////

    /**
     * Adds the provided vertex to this LocalTransferGrid's vertex matrix
     * @throws NullPointerException if the provided GridVertex is null
     * @param vertex GridVertex to add
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call
     */
    protected boolean addVert(GridVertex vertex) {
        if(vertex == null) 
        throw new NullPointerException("Failed to add vertex to LocalTransferGrid - Cannot store a null vertex!");
        return vertMatrix.put(vertex.getID(), vertex) != null;
    }

    /**
     * Removes the vertex at the provided GID from this LocalTransferGrid's vertex matrix
     * and performs additional operations to remove outdated edge and path data. 
     * @param id ID to remove
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call
     */
    protected boolean removeVert(GID id) {
        if(id == null) throw new NullPointerException("Error removing GridVertex - The provided GID is null!");
        GridVertex poppedVert = vertMatrix.remove(id);
        if(poppedVert == null) return false;
        if(!poppedVert.isEmpty()) removePathsInvolving(poppedVert);
        poppedVert.markRemoved();

        for(GridVertex linked : poppedVert.links) {
            if(linked.unlinkFrom(poppedVert)) {
                if(linked.isEmpty())
                    removeVert(linked.getID());
                GridEdge found = edges.remove(new GIDPair(linked.getID(), id));
                if(found == null) continue;
                GridSyncHelper.informPlayerEdgeUpdate(GridSyncPacketType.REMOVE, found.toLightweight());
            }
        }

        GridSyncHelper.informPlayerVertexUpdate(GridSyncPacketType.REMOVE, id);
        return true;
    }

    /***
     * Creates a link (edge) between two GridVertices
     * @throws NullPointerException If either provided GridVertex
     * does not exist within this LocalTransferGrid.
     * @param first GridVertex to link
     * @param second GridVertex to link
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    protected boolean linkVerts(GridVertex first, GridVertex second, int wireType, boolean shouldPath) {
        requireValidVertex("Failed to link GridVertices", first, second);
        if(first.linkTo(second) && second.linkTo(first)) {
            first.doFullSync(false);
            second.doFullSync(false);
            addEdge(first.getID(), second.getID(), wireType);
            if(shouldPath) findAllPaths(true);
            return true;
        }
        return false;
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    protected boolean linkVerts(GridVertex first, GridVertex second, int wireType) {
        return linkVerts(first, second, wireType, true);
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    protected boolean linkVerts(GID first, GID second, int wireType, boolean shouldPath) {
        requireValidID("Failed to link GridVertices", first, second);
        GridVertex vertF = getVertAt(first);
        GridVertex vertT = getVertAt(second);
        if(vertF.linkTo(vertT) && vertT.linkTo(vertF)) {
            vertF.doFullSync(false);
            vertT.doFullSync(false);
            addEdge(first, second, wireType);
            if(shouldPath) findAllPaths(true);
            return true;
        }
        return false;
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @param shouldPath if <code>TRUE</code> paths will be re-addressed after this link is added.
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GID first, GID second, int wireType) {
        return linkVerts(first, second, wireType, true);
    }

    /***
     * Performs a DFS to determine all of the different "clusters"
     * that form this LocalTransferGrid. Individual vertices that are found to 
     * possess no connections are discarded, and are not included in the 
     * resulting clusters. <strong>Does not modify this system in-place.</strong>
     * 
     * @return ArrayList of LocalTransferGrids formed from the individual clusters 
     * within this LocalTransferGrid.
     */
    protected List<LocalTransferGrid> trySplit() {
        HashSet<GID> visited = new HashSet<>();
        List<LocalTransferGrid> clusters = new ArrayList<LocalTransferGrid>();

        for(GID identifier : vertMatrix.keySet()) {
            if(visited.contains(identifier)) continue;
            ArrayList<GridVertex> clusterVerts = new ArrayList<>();
            depthFirstPopulate(identifier, visited, clusterVerts);
            if(clusterVerts.size() > 1) {
                LocalTransferGrid clusterResult = new LocalTransferGrid(parent, clusterVerts, edges);
                clusterResult.cleanEdges();
                clusterResult.collectPaths(paths);
                clusters.add(clusterResult);
            }
        }
        return clusters;
    }

    /**
     * Grabs GridPath objects that coorespond to GridVertices in this LocalTransferGrid.
     * @param pathsToCollect
     */
    private void collectPaths(Map<GIDPair, GridPath> pathsToCollect) {
        for(Map.Entry<GIDPair, GridPath> path : pathsToCollect.entrySet()) {
            if(path.getKey().isIn(vertMatrix.keySet()))
                paths.put(path.getKey(), path.getValue());
        }
    }

    /**
     * The recursive DFS initiated by {@link LocalTransferGrid#trySplit() <code>trySplit()</code>} is performed here.
     */
    private void depthFirstPopulate(GID startID, HashSet<GID> visited, ArrayList<GridVertex> vertices) {
        GridVertex thisIteration = getVertAt(startID);
        visited.add(startID);

        if(thisIteration == null) return;
        vertices.add(thisIteration);

        if(thisIteration.isEmpty()) return;
        for(GridVertex neighbor : thisIteration.links) {
            GID currentID = neighbor.getID();
            if(!visited.contains(currentID))
                depthFirstPopulate(currentID, visited, vertices);
        }
    }

    /***
     * Retrieves a vertex in this network.
     * @param BlockPos
     * @return GridVertex at the given BlockPos
     */
    public GridVertex getVertAt(GID pos) {
        return vertMatrix.get(pos);
    }

    /***
     * Examines the edge map and the vertex map to find weirdness.
     * If there are any edges that point to vertices that aren't in this LocalTransferGrid,
     * this call will remove them.
     * @return True if this LocalTransferGrid was modified as a result of this call.
     */
    protected boolean cleanEdges() {
        Iterator<Map.Entry<GIDPair, GridEdge>> matrixIterator = edges.entrySet().iterator();
        boolean changed = false;
        while(matrixIterator.hasNext()) {
            GridEdge currentEdge = matrixIterator.next().getValue();
            if((!vertMatrix.containsKey(currentEdge.getSideA()) && (!vertMatrix.containsKey(currentEdge.getSideB())))) {
                changed = true;
                matrixIterator.remove();
            }
        }
        return changed;
    }

    /***
     * Finds the optimal path between the given GridVertex and all other GridVertices marked 
     * as members and stores those GridPath objects in this LocalTransferGrid.
     * @param origin GridVertex to find paths from/to. All resulting paths start or end with this GridVertex.
     * @param shouldUpdate If <code>TRUE</code>, each new path that is found will call {@link LocalTransferGrid#onPathsUpdated(GridPath, boolean) <code>onPathsUpdated()</code>}
     * @param forceOpportunities If <code>TRUE</code> the member status of each involved GridVertex will be manually re-addressed. This process is more expensive but is required
     * in some scenarios.
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call. (if any paths were found)
     */
    public boolean pathfindFrom(GridVertex origin, boolean shouldUpdate, boolean forceOpportunities) {
        boolean exists = false;
        if(forceOpportunities) origin.doFullSync(false);
        for(GridVertex vert : vertMatrix.values()) {

            if(forceOpportunities) vert.doFullSync(false);
            if(!vert.canFormPathTo(origin)) continue;

            if(paths.containsKey(new GIDPair(origin.getID(), vert.getID()))) 
                continue;

            GridPath path = astar(origin, vert);
            if(path != null) {
                exists = true;
                addPath(path);
                if(shouldUpdate) onPathsUpdated(path, true);
            }
        }

        return exists;
    }

    /***
     * Finds the optimal path between the given GridVertex and all other GridVertices marked 
     * as members and stores those GridPath objects in this LocalTransferGrid.
     * @param origin GridVertex to find paths from/to. All resulting paths start or end with this GridVertex.
     * @param shouldUpdate If <code>TRUE</code>, each new path that is found will call {@link LocalTransferGrid#onPathsUpdated(GridPath, boolean) <code>onPathsUpdated()</code>}
     * @param forceOpportunities If <code>TRUE</code> the member status of each involved GridVertex will be manually re-addressed. This process is more expensive but is required
     * in some scenarios.
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call. (if any paths were found)
     */
    public boolean pathfindFrom(GridVertex origin, boolean forceOpportunities) {
        return pathfindFrom(origin, true, forceOpportunities);
    }

    /***
     * Finds optimal paths between every relevent GridVertex in this grid.
     * @param shouldUpdate If <code>TRUE</code>, each new path that is found will call {@link LocalTransferGrid#onPathsUpdated(GridPath, boolean) <code>onPathsUpdated()</code>}
     * @return <code>TRUE</code> if the LocalTransferGrid was modified as a result of this call.
     */
    public boolean findAllPaths(boolean shouldUpdate) {        
        boolean exists = false;
        for(GridVertex vert : vertMatrix.values()) {
            if(!vert.isMember()) continue;
            if(pathfindFrom(vert, shouldUpdate, false)) exists = true;
        }

        return exists;
    }

    /***
     * may god have mercy on my soul
     * @return GridPath object representing the optimal path between 
     * the two vertices, or null if no path could be fouund.
     */
    @Nullable
    private GridPath astar(GridVertex start, GridVertex goal) {
        final Queue<GridVertex> openVerts = new PriorityQueue<>(11, GridVertex.GUIDANCE_COMPARATOR);
        final Map<GridVertex, GridVertex> path = new HashMap<GridVertex, GridVertex>();

        // All vertices that are addressed in the A* operation are contained here.
        // Their A* guidance variables must be reset in order to function properly in subsequent calls to this method.
        final Set<GridVertex> outdatedVertices = new HashSet<>();

        WattUnit slowestRate = WattUnit.INFINITY;

        start.setCumulative(0);
        start.getAndStoreHeuristic(goal);
        openVerts.add(start);

        while(!openVerts.isEmpty()) {
            final GridVertex local = openVerts.poll();

            // loop terminates here if successful
            if(local.equals(goal)) {
                resetPathData(outdatedVertices);
                return GridPath.ofUnwound(path, goal, slowestRate);
            }

            // mark this edge as visited
            local.markVisited();

            for(GridVertex neighbor : local.links) {

                // if we've already addressed this vertex the iteration can be skipped
                if(neighbor.hasBeenVisited()) continue;
                
                // get the cooresponding edge from local to neighbor
                GridEdge edge = lookupEdge(local, neighbor);
                if(edge == null) throw new NullPointerException("Error initiating A* - edge from " + local + " to " + neighbor + " could not be found.");
                if(!edge.canTransfer()) continue;

                // get the slowest volts & amps between the current rate and the current edge's rate
                slowestRate = slowestRate.getLowerStats(edge.getTransferStats());

                // calculate the tentative A* value and store the heuristic in the current edge
                float tentative = local.getAndStoreHeuristic(neighbor) + local.getCumulative();
                outdatedVertices.add(local);

                // if the tentative is less than the cumulative (that is, if we have points to spend here) address this path
                if(tentative < neighbor.getCumulative()) {

                    neighbor.setCumulative(tentative);
                    neighbor.getAndStoreHeuristic(local);
                    outdatedVertices.add(neighbor);
                    
                    // add this vertex to the resulting path and open its neighbor
                    path.put(neighbor, local);
                    if(!openVerts.contains(neighbor)) 
                        openVerts.add(neighbor);
                }
            }
        }
        resetPathData(outdatedVertices);
        return null;
    }

    private void resetPathData(Set<GridVertex> verts) {
        for(GridVertex vert : verts)
            vert.resetHeuristics();
    }

    /***
     * Removes all valid paths that involve the given GridVertex
     * @param vert GridVertex to compare
     */
    public void removePathsInvolving(GridVertex vert) { 
        Iterator<GridPath> pathIter = paths.values().iterator();
        while(pathIter.hasNext()) {
            GridPath path = pathIter.next();
            if(path.containsVertex(vert)) {
                pathIter.remove();
                onPathsUpdated(path, false);
            }
        }
    }

    /***
     * Removes paths whose ends (at index <code>0</code> or <code>length - 1</code>)
     * are equal to the given vertex
     * @param vert GridVertex to compare
     */
    public void removePathsEndingIn(GridVertex vert) {
        Iterator<GridPath> pathIter = paths.values().iterator();
        while(pathIter.hasNext()) {
            GridPath path = pathIter.next();
            if(path.getEnd().equals(vert) || path.getStart().equals(vert)) {
                pathIter.remove();
                onPathsUpdated(path, false);
            }
        }
    }

    /**
     * Removes a path by direct GIDPair access
     * @param id GIDPair id to remove
     */
    protected void removePath(GIDPair id) {
        GridPath path = paths.remove(id);
        if(path != null)
            onPathsUpdated(path, false);
    }

    /***
     * Gets the GridVertex at the given ID, or creates a new one & adds it to this 
     * LocalTransferGrid, and returns it. To be called during the loading process.<p>
     * <strong>This method is public because it is called by the GridVertex's constructor, but
     * it is not designed to be called in any other context.</strong>
     * @param world World to operate within
     * @param id ID of GridVertex to find
     * @return The GridVertex that was found or created as a result of this call.
     */
    public GridVertex getOrCreateOnLoad(Level world, GID id) {
        
        GridVertex target = getVertAt(id);
        if(target != null) return target;

        target = new GridVertex(world, this, id);
        addVert(target);
        return target;
    }

    /**
     * Adds a GridPath to this LocalTransferGrid
     * @param path GridPath to add
     * @return <code>TRUE</code> if this LocalTransferGrid was modified as a result of this call (if the path didn't already exist)
    */
    private boolean addPath(GridPath path) {
        if(path == null) throw new NullPointerException("Error adding path to LocalTransferGrid - Path cannot be null!");
        return paths.put(path.getHashable(), path) == null;
    }

    /**
     * Gets the GridEdge between the given vertices if one exists.
     * @param a Gridvertex A - direction agnostic
     * @param b GridVertex B - direction agnostic
     * @return GridEdge stored in this LocalTransferGrid, or null of one does not exist.
     */
    @Nullable
    protected GridEdge lookupEdge(GridVertex a, GridVertex b) {
        return edges.get(new GIDPair(a.getID(), b.getID()));
    }

    private void addEdge(GID idA, GID idB, int wireType) {
        requireValidID("Failed to add new GridEdge", idA, idB);
        GIDPair hashed = new GIDPair(idA, idB);
        edges.put(hashed, new GridEdge(parent, new GIDPair(idA, idB), wireType));
    }

    public Color getDebugColor() {
        return VectorHelper.toColor(((GridVertex)vertMatrix.values().toArray()[0]).getID().getBlockPos().getCenter());
    }

    public GlobalTransferGrid getParent() {
        return parent;
    }

    /**
     * @return Int representing how many GridVertices are in this LocalTransferGrid
     */
    public int size() {
        return vertMatrix.size();
    }

    /**
     * @return <code>TRUE</code> if this LocalTransferGrid has no GridVertices
     */
    public boolean isEmpty() {
        return vertMatrix.isEmpty();
    }

    public Collection<GridVertex> allVerts() {
        return vertMatrix.values();
    }

    public Collection<GridEdge> allEdges() {
        return edges.values();
    }

    /**
     * @param id GIDPair to look for
     * @return <code>TRUE</code> if this LocalTransferGrid contains an edge mapped to the given key.
     */
    protected boolean containsEdge(GIDPair id) {
        return edges.containsKey(id);
    }

    public Collection<GridPath> allPaths() {
        return paths.values();
    }

    public String toString() {
        
        String output = "\n";
        
        int x = 1;
        for(GridVertex vert : vertMatrix.values()) {
            output += ("Vertex " + x + ": ") + vert + "\n";
            x++;
        }
        
        x = 1;
        for(GridPath path : paths.values()) {
            output += ("Path " + x + ": ") + path + "\n";
            x++;
        }

        x = 1;
        for(GridEdge edge : edges.values()) {
            output += ("Edge " + x + ": ") + edge + "\n";
            x++;
        }

        return output;
    }

    /** 
     * Callable by debuggers to produce formatted text for minecraft chat
     * @param target BlockPos, usually of the targeted vertex. Vertices at this BlockPos will
     * be marked with a <code>'>>>'</code> in the resulting string.
     */
    public String toFormattedString(@Nullable BlockPos target) {
        String output = "\n";
        int x = 1;

        if(vertMatrix.size() == 0) {
            return "\n            §6this grid is... blank?? §lhow did you do that";
        }

        for(GridVertex vert : vertMatrix.values()) {
            if(vert.getID().getBlockPos().equals(target))
                output += ("    §6§l>>>  §r§7Vertex §r§b§l" + x + ": §r§7") + vert.toFormattedString() + "\n";
            else output += ("            §r§7Vertex §r§b§l" + x + ": §r§7") + vert.toFormattedString() + "\n";
            x++;
        }
        return output;
    }

    /***
     * Called internally whenever a GridPath is added or removed
     * @param path The path in question
     * @param add True if this path was added, false if this path was removed
     */
    private void onPathsUpdated(GridPath path, boolean add) {
        if(add) GridSyncHelper.informPlayerPathUpdate(GridSyncPacketType.ADD_NEW, path);
        else GridSyncHelper.informPlayerPathUpdate(GridSyncPacketType.REMOVE, path);
    }


    /////////////////////// sanity check helpers ///////////////////////////////////////////////////////
    private void requireValidID(String failMessage, GID... idSet) {
        for(GID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!vertMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid GridVertex matching SystemID " 
                + id + " could be found!");
        }
    }

    private void requireValidVertex(String failMessage, GridVertex... vertexSet) {
        for(GridVertex vertex : vertexSet) {
            if(vertex == null)
                throw new NullPointerException(failMessage + " - The provided GridVertex is null!");
            if(!vertMatrix.containsValue(vertex))
                throw new NullPointerException(failMessage + " - The provided GridVertex does not exist in this LocalTransferGrid!");
        }       
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}