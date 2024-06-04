package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/***
 * A LocalGrid stores GridVertices in an undirected graph, forming an adjacency list.
 * Connections between these vertices are stored in two ways: <p>
 * - Each GridVertex contains a LinkedList of vertices that said vertex is connected to <p>
 * - Each LocalTransferGrid contains a HashMap of each edge for direct access. <p>
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
                out.addEdge(edge);
            for(GridPath path : grid.allPaths())
                out.addPath(path);
        }

        return out;
    }

    /***
     * Populates this LocalTransferGrid with members of a list
     * @param cluster ArrayList of GridVertexs to add to this LocalTransferGrid upon creation
     */
    public LocalTransferGrid(GlobalTransferGrid parent, ArrayList<GridVertex> cluster, Map<GIDPair, GridEdge> edgeMatrix) {
        for(GridVertex vertex : cluster)
            vertMatrix.put(vertex.getID(), vertex);
        this.edges = ((Object2ObjectOpenHashMap<GIDPair, GridEdge>)edgeMatrix).clone();
        this.parent = parent;
    }

    public LocalTransferGrid(GlobalTransferGrid parent, CompoundTag in, Level world) {
        this.parent = parent;
        ListTag net = in.getList("nt", Tag.TAG_COMPOUND);
        for(int x = 0; x < net.size(); x++) {

            CompoundTag vertC = net.getCompound(x);
            GridVertex vertToAdd = getVert(GID.of(vertC));

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

    public void getEdgesWithin(SectionPos section) {
        for(GridEdge edge : edges.values()) {
            if(edge == null) continue;
            
        }
    }

    protected boolean addVert(GridVertex vertex) {
        if(vertex == null) 
        throw new NullPointerException("Failed to add vertex to LocalTransferGrid - Cannot store a null vertex!");
        return vertMatrix.put(vertex.getID(), vertex) != null;
    }

    public boolean removeVert(GID id) {
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
     * Gets all vertices assigned to the given BlockPos
     * @param pos BlockPos to check
     * @return An ArrayList of GridVertices at this BlockPos
     */
    public ArrayList<GridVertex> getVertsAt(BlockPos pos) {
        ArrayList<GridVertex> out = new ArrayList<>();
        for(GridVertex vert : vertMatrix.values())
            if(vert.isAt(pos)) out.add(vert);
        return out;
    }

    /***
     * @return True if this LocalTransferGrid contains the given GridVertex
     */
    public boolean contains(GridVertex vert) {
        return contains(vert.getID());
    }

    /***
     * @return True if this LocalTransferGrid contains a GridVertex at the given GID
     */
    public boolean contains(GID id) {
        return vertMatrix.containsKey(id);
    }

    /***
     * Creates a link (edge) between two GridVertices
     * @throws NullPointerException If either provided GridVertex
     * does not exist within this LocalTransferGrid.
     * @param first GridVertex to link
     * @param second GridVertex to link
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GridVertex first, GridVertex second, int wireType, boolean shouldPath) {
        requireValidNode("Failed to link GridVertices", first, second);
        if(first.linkTo(second) && second.linkTo(first)) {
            first.doFullSync(false);
            second.doFullSync(false);
            addEdge(first.getID(), second.getID(), wireType);
            if(shouldPath) findAllPaths(true);
            return true;
        }
        return false;
    }

    public boolean linkVerts(GridVertex first, GridVertex second, int wireType) {
        return linkVerts(first, second, wireType, true);
    }

    /***
     * Creates a link (edge) between two GridVertices at the provided GIDs
     * @throws NullPointerException If either provided GID doesn't 
     * indicate the location of a GridVertex in this LocalTransferGrid.
     * @param first GID to locate and link
     * @param second GID object to locate and link
     * @return True if the GridVertices were modified as a result of this call,
     */
    public boolean linkVerts(GID first, GID second, int wireType, boolean shouldPath) {
        requireValidLink("Failed to link GridVertices", first, second);
        GridVertex vertF = vertMatrix.get(first);
        GridVertex vertT = vertMatrix.get(second);
        if(vertF.linkTo(vertT) && vertT.linkTo(vertF)) {
            vertF.doFullSync(false);
            vertT.doFullSync(false);
            addEdge(first, second, wireType);
            if(shouldPath) findAllPaths(true);
            return true;
        }
        return false;
    }

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
    public List<LocalTransferGrid> trySplit() {
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

    private void collectPaths(Map<GIDPair, GridPath> pathsToCollect) {
        for(Map.Entry<GIDPair, GridPath> path : pathsToCollect.entrySet()) {
            if(path.getKey().isIn(vertMatrix.keySet()))
                paths.put(path.getKey(), path.getValue());
        }
    }

    /**
     * The recursive DFS initiated by {@link LocalTransferGrid#trySplit() <code>trySplit()</code>} is performed here.
     */
    private void depthFirstPopulate(GID vertex, HashSet<GID> visited, ArrayList<GridVertex> vertices) {
        GridVertex thisIteration = getVert(vertex);
        visited.add(vertex);

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
     * Appends all elements from the supplied LocalTransferGrid to this
     * LocalTransferGrid, modifying it in-place.
     * @return This LocalTransferGrid (for chaining)
     */
    public LocalTransferGrid mergeWith(LocalTransferGrid other) {
        // vertMatrix.putAll(other.vertMatrix);
        edges.putAll(other.edges);
        paths.putAll(other.paths);

        for(Entry<GID, GridVertex> vertEntry : other.vertMatrix.entrySet()) {
            GridVertex vert = vertEntry.getValue();

            vert.replaceParent(this);
            if(!vert.isEmpty())
                vertMatrix.put(vertEntry.getKey(), vert);
        }

        return this;
    }

    /***
     * Retrieves a vertex in this network.
     * @param BlockPos
     * @return GridVertex at the given BlockPos
     */
    public GridVertex getVert(GID pos) {
        return vertMatrix.get(pos);
    }

    /***
     * Gets all GridVertices regardless of subIndex
     * @param pos
     * @return A List of all GridVertices ta the given BlockPos
     */
    public List<GridVertex> getAllNodesAt(BlockPos pos) {
        ArrayList<GridVertex> vertices = new ArrayList<GridVertex>();
        for(GridVertex vert : vertMatrix.values())
            if(vert.getID().getBlockPos().equals(pos)) vertices.add(vert);
        return vertices;
    }

    /***
     * @return True if this LocalTransferGrid contains the given GridVertex
     */
    public boolean containsNode(GridVertex vertex) {
        return vertMatrix.containsValue(vertex);
    }

    /***
     * @return False if ALL of the GridVertexs in this TranferSystem are empty
     * (This network has no edges)
     */
    public boolean hasLinks() {
        for(GridVertex vertex : vertMatrix.values())
            if(!vertex.isEmpty()) return true;
        return false;
    }

    /***
     * Examines the edge map and the vertex map to find weirdness.
     * If there are any edges that point to vertices that aren't in this LocalTransferGrid,
     * this call will remove them.
     * @return True if this LocalTransferGrid was modified as a result of this call.
     */
    public boolean cleanEdges() {
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
     * @return True if this LocalTransferGrid was modified as a result of this call. (if any paths were found)
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
     * Called internally whenever a GridPath is added or removed
     * @param path The path in question
     * @param add True if this path was added, false if this path was removed
     */
    private void onPathsUpdated(GridPath path, boolean add) {
        if(add)
            GridSyncHelper.informPlayerPathUpdate(GridSyncPacketType.ADD_NEW, path);
        else
            GridSyncHelper.informPlayerPathUpdate(GridSyncPacketType.REMOVE, path);
    }

    /***
     * may god have mercy on my soul
     * @return GridPath object representing the optimal path between 
     * the two vertices, or null if no path could be fouund.
     */
    @Nullable
    private GridPath astar(GridVertex start, GridVertex goal) {
        final Queue<GridVertex> openVerts = new PriorityQueue<>(11, GridVertex.GUIDANCE_COMPARATOR);
        final Set<GridVertex> visited = new HashSet<>();
        final Map<GridVertex, GridVertex> path = new HashMap<GridVertex, GridVertex>();
        WattUnit slowestRate = WattUnit.INFINITY;

        start.setCumulative(0);
        start.getAndStoreHeuristic(goal);
        openVerts.add(start);

        while(!openVerts.isEmpty()) {
            final GridVertex local = openVerts.poll();

            // loop terminates here if successful
            if(local.equals(goal)) 
                return GridPath.ofUnwound(start, path, goal, slowestRate, true);

            // mark this edge as visited
            visited.add(local);
            for(GridVertex neighbor : local.links) {

                // if we've already addressed this vertex the iteration can be skipped
                if(visited.contains(neighbor)) continue;
                GridEdge edge = lookupEdge(local, neighbor);

                if(edge == null) throw new NullPointerException("Error initiating A* - edge from " + local + " to " + neighbor + " could not be found.");
                if(!edge.canTransfer()) continue;

                // returns the slowest volts & amps between the current rate and the current edge's rate
                slowestRate = slowestRate.getLowerStats(edge.getTransferStats());

                // calculate the tentative A* value and store the heuristic in the current edge
                float tentative = local.getAndStoreHeuristic(neighbor) + local.getCumulative();

                // if the tentative is greater than the cumulative (that is, if we have points to spend here) address this path
                if(tentative < neighbor.getCumulative()) {

                    neighbor.setCumulative(tentative);
                    neighbor.getAndStoreHeuristic(local);
                    
                    // add this vertex to the resulting path and open its neighbor
                    path.put(neighbor, local);
                    if(!openVerts.contains(neighbor))
                        openVerts.add(neighbor);
                }
            }
        }
        resetPathData(start, visited, goal);
        return null;
    }

    private void resetPathData(GridVertex start, Set<GridVertex> verts, GridVertex goal) {
        start.resetHeuristics();
        goal.resetHeuristics();
        for(GridVertex vert : verts)
            vert.resetHeuristics();
    }

    /***
     * Removes all valid paths that involve the given GridVertex
     * @param vert GridVertex to compare
     */
    private void removePathsInvolving(GridVertex vert) { 
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

    protected void removePath(GIDPair id) {
        GridPath path = paths.remove(id);
        if(path != null) {
            onPathsUpdated(path, false);
        }
    }

    /***
     * 
     * Gets the GridVertex at the given ID, or creates a new one & adds it to this 
     * LocalTransferGrid, and returns it.
     * To be called only by GridVertex during the loading process from NBT.
     * @param world World to operate within
     * @param id ID of GridVertex to find
     * @return The GridVertex that was found or created as a result of this call.
     */
    public GridVertex getOrCreateOnLoad(Level world, GID id) {
        
        GridVertex target = getVert(id);
        if(target != null) return target;

        target = new GridVertex(world, this, id);
        addVert(target);
        return target;
    }

    protected void addPath(GridPath path) {
        if(path == null) throw new NullPointerException("Error adding path to LocalTransferGrid - Path cannot be null!");
        paths.put(path.getHashable(), path);
    }

    protected GridEdge lookupEdge(GridVertex a, GridVertex b) {
        return edges.get(new GIDPair(a.getID(), b.getID()));
    }

    protected void addEdge(GID idA, GID idB, int wireType) {

        if(getVert(idA) == null)
            throw new IllegalArgumentException("Error adding edge to LocalTransferGrid - This LocalTransferGrid does not contain a GridVertex at " + idA + "!");
        if(getVert(idB) == null)
            throw new IllegalArgumentException("Error adding edge to LocalTransferGrid - This LocalTransferGrid does not contain a GridVertex at " + idB + "!");

        GIDPair hashed = new GIDPair(idA, idB);
        edges.put(hashed, new GridEdge(parent, hashed, wireType));
    }   

    protected void addEdge(GridEdge edge) {
        if(edge == null) throw new NullPointerException("Error adding edge to LocalTransferGrid - Edge cannot be null!");
        edges.put(edge.getHashable(), edge);
    }

    protected boolean removeEdge(GIDPair key) {
        return edges.remove(key) != null;
    }

    public Color getDebugColor() {
        return VectorHelper.toColor(((GridVertex)vertMatrix.values().toArray()[0]).getID().getBlockPos().getCenter());
    }

    public GlobalTransferGrid getParent() {
        return parent;
    }

    public int size() {
        return vertMatrix.size();
    }

    public boolean isEmpty() {
        return vertMatrix.isEmpty();
    }

    public Collection<GridVertex> allVerts() {
        return vertMatrix.values();
    }

    public Collection<GridEdge> allEdges() {
        return edges.values();
    }

    public Map<GIDPair, GridEdge> getEdgeMap() {
        return edges;
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
        return output;
    }

    public String toFormattedString(BlockPos target) {
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

    public void resetVertices() {
        for(GridVertex vert : vertMatrix.values()) {
            vert.resetHeuristics();
            vert.replaceParent(this);
        }
    }

    /////////
    private void requireValidLink(String failMessage, GID... idSet) {
        for(GID id : idSet) {
            if(id == null) 
                throw new NullPointerException(failMessage + " - The provided SystemLink is null!");
            if(!vertMatrix.containsKey(id))
                throw new NullPointerException(failMessage + " - No valid GridVertex matching SystemID " 
                + id + " could be found!");
        }
    }

    private void requireValidNode(String failMessage, GridVertex... vertexSet) {
        for(GridVertex vertex : vertexSet) {
            if(vertex == null)
                throw new NullPointerException(failMessage + " - The provided GridVertex is null!");
            if(!vertMatrix.containsValue(vertex))
                throw new NullPointerException(failMessage + " - The provided GridVertex does not exist in this LocalTransferGrid!");
        }       
    }
}