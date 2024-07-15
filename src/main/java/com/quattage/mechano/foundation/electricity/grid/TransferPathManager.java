package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GIDPair;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncHelper;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncPacketType;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * The TransferPathManager hashes paths, where the starting point of the path is the key, and a set of all paths
 * at that starting point is the value. <p>
 * When a path is added, the TransferPathManager actually stores two paths - The forawrd, and the inverse.
 * This is to mitigate the amount of brute force iteration required to find paths from either end.
 * Memory performance isn't great, but doing it this way ensures that from one GridVertex, every 
 * relevent path is only one hash lookup away from access.
 */
public class TransferPathManager {
    
    private final Object2ObjectOpenHashMap<GID, Set<GridPath>> paths = new Object2ObjectOpenHashMap<>(11);
    private boolean shouldUpdate = true;

    /**
     * Adds the provided <code>path</code> to this TransferPathManager.
     * The starting GridVertex ID is used as the path's hash, acquired by <code>path.getStart().getID()</code>.
     * Both the forward (unchanged) and inverse (acquired with <code>path.copyAndInvert()</code>) are stored as a 
     * result of this call.
     * @param path GridPath to add. If no grid paths currently exist at this GridPath's vertex hash, a new set is created
     * at that hash's location.
     */
    protected void putPath(GridPath path) {

        if(path == null) throw new NullPointerException("Error adding GridPath to TransferPathManager - Path cannot be null!");
        if(path.isEmpty()) throw new IllegalArgumentException("Error adding GridPath to TransferPathManager - Path is empty!");

        GID keyA = path.getStart().getID();
        GridPath pathInv = path.copyAndInvert();
        GID keyB = pathInv.getStart().getID();

        Set<GridPath> queryA = paths.get(keyA);
        Set<GridPath> queryB = paths.get(keyB);
        if(queryA == null && queryB == null) {
            queryA = new HashSet<>();
            queryA.add(path);
            paths.put(keyA, queryA);
            queryB = new HashSet<>();
            queryB.add(pathInv);
            paths.put(keyB, queryB);
            onPathsUpdated(path, GridSyncPacketType.ADD_NEW);
        } else if(queryA != null && queryB == null) {
            queryA.add(path);
            queryB = new HashSet<>();
            queryB.add(pathInv);
            paths.put(keyB, queryB);
            onPathsUpdated(path, GridSyncPacketType.ADD_NEW);
        } else if(queryA == null && queryB != null) {
            queryA = new HashSet<>();
            queryA.add(path);
            paths.put(keyA, queryA);
            queryB.add(pathInv);
            onPathsUpdated(path, GridSyncPacketType.ADD_NEW);
        } else {
            if(queryA.add(path) && queryB.add(pathInv))
                onPathsUpdated(path, GridSyncPacketType.ADD_NEW);
        }
    }

    /**
     * Removes <code>path</code> from this TransferPathManager along with its inverse path.
     * @param path GridPath to remove
     * @return True if this TransferPathManager was modified as a result of this call.
     */
    protected boolean removePath(GridPath path) {

        if(path == null) throw new NullPointerException("Error removing GridPath from TransferPathManager - Path cannot be null!");

        Set<GridPath> lookupA = paths.get(path.getStart().getID());
        if(lookupA == null) {
            throw new IllegalArgumentException("Cannot remove GridPath [" + path.getStart().getID() 
                + " -> " + path.getEnd().getID() + "] as it does not exist in this TransferPathManager!");
        }

        if(lookupA.remove(path)) {
            Set<GridPath> lookupB = paths.get(path.getEnd().getID());
            if(!lookupB.remove(path.copyAndInvert())) {
                throw new IllegalArgumentException("Error removing GridPath " + path + " - One side was found, but the inverse path (" + path.copyAndInvert() 
                    + ") was not found. All paths need a stored inverse. This was found to not be the case, which means something probably confusing and stupid went wrong.");
            }
            onPathsUpdated(path, GridSyncPacketType.REMOVE);
            return true;
        }
        return false;
    }

    /**
     * Removes all paths starting or ending with the given GID.
     * @param id GID to look for
     */
    public void removePathsEndingIn(GID key) {
        if(key == null) throw new NullPointerException("Error removing GridPath from TransferPathManager - The provided key cannot be null!");
        paths.remove(key);

        Iterator<Set<GridPath>> allPathsIterator = paths.values().iterator();
        while(allPathsIterator.hasNext()) {

            Set<GridPath> thesePaths = allPathsIterator.next();

            Iterator<GridPath> thesePathsIterator = thesePaths.iterator();
            while(thesePathsIterator.hasNext()) {
                GridPath thisPath = thesePathsIterator.next();
                if(thisPath.getStart().getID().equals(key) || thisPath.getEnd().getID().equals(key)) {
                    thesePathsIterator.remove();
                    onPathsUpdated(thisPath, GridSyncPacketType.REMOVE);
                }
            }
        }
    }

    /**
     * Removes all paths starting, ending, or containing the given vertex.
     * A path is said to contain the given GID <code>key</code> if 
     * {@link GridPath#containsKey(GID) <code>GridPath.containsKey(key)</code>} evaluates to true.
     * @param key GID to look for
     */
    public void removeAllPathsInvolving(GID key) {

        if(key == null) throw new NullPointerException("Error removing GridPath from TransferPathManager - The provided key cannot be null!");
        paths.remove(key);

        Iterator<Set<GridPath>> allPathsIterator = paths.values().iterator();
        while(allPathsIterator.hasNext()) {
            
            Set<GridPath> thesePaths = allPathsIterator.next();

            Iterator<GridPath> thesePathsIterator = thesePaths.iterator();
            while(thesePathsIterator.hasNext()) {
                GridPath thisPath = thesePathsIterator.next();
                if(thisPath.containsKey(key)) {
                    thesePathsIterator.remove();
                    onPathsUpdated(thisPath, GridSyncPacketType.REMOVE);
                }
            }

            if(thesePaths.isEmpty()) allPathsIterator.remove();
        }
    }

    public Map<GID, Set<GridPath>> getAll() {
        return paths;
    }

    /**
     * A for-each that iterates over every path in the set mapped to the given GID.
     * @param id GID to find. This GID's associated path set will be iterated.
     * If no mapping is tied to this GID, the execution will simply do nothing.
     * @param action Function to execute for every path found
     */
    public void forEachPathAt(GID id, Consumer<GridPath> action) {
        Set<GridPath> pathSet = paths.get(id);
        if(pathSet == null) return;
        for(GridPath path : pathSet)
            action.accept(path);
    }

    /**
     * A simple for-each that iterates over every path in this TransferPathManager.
     * @param action Function to execute for every path
     */
    public void forEachPath(Consumer<GridPath> action) {
        for(Set<GridPath> pathSet : getAll().values()) {
            for(GridPath path : pathSet)
                action.accept(path);
        }
    }

    /**
     * Gets all paths mapped to the given GID <code>id</code>
     * @param id GID to find
     * @return A set containing all paths at <code>id</code>
     */
    public Set<GridPath> getPathsAt(GID id) {
        return paths.get(id);
    }

    /**
     * Completely clears this TransferPathManager's underlying map.
     */
    protected void clearAllPaths() {
        paths.clear();
        paths.trim(11);
        onPathsUpdated(null, GridSyncPacketType.CLEAR);
    }

    // TODO probably wont use lol
    protected List<GridPath> getEveryPath() {
        List<GridPath> out = new ArrayList<>();
        for(Set<GridPath> pathSet : paths.values()) {
            for(GridPath path : pathSet)
                out.add(path);
        }

        return out;
    }

    /**
     * Used only for debugging, A "unique" path is one that has a unique start and end point.
     * Paths that are the inverse of a previous paths are not considered unique.
     * @return The amount of unique paths in this TransferPathManager.
     */
    public int getUniquePathCount() {
        final Set<GIDPair> pairs = new HashSet<>();
        forEachPath(path -> {
            pairs.add(new GIDPair(path.getStart().getID(), path.getEnd().getID()));
        });
        return pairs.size();
    }

    /**
     * Used only for debugging. Returns the size of the two-dimensional paths map.
     * @return The amount of total paths (the map's size)
     */
    public int getActualPathCount() {
        final List<GIDPair> pairs = new ArrayList<>();
        forEachPath(path -> {
            pairs.add(new GIDPair(path.getStart().getID(), path.getEnd().getID()));
        });
        return pairs.size();
    }

    /**
     * Gets the size of only the X axis of the two-dimensional paths map.
     * @return The amount of unique GIDs in this TransferPathManager's map
     */
    public int getDeclaratorCount() {
        return paths.size();
    }

    /**
     * When called, subsequent modifications to this TransferPathManager will not send 
     * any packets.
     * @return This TransferPathManager
     */
    
    public TransferPathManager skipUpdates() {
        shouldUpdate = false;
        return this;
    }

    /**
     * When called, subsequent modifications to this TransferPathManager will send packets
     * to the client.
     * @return This TransferPathManager
     */
    public TransferPathManager withUpdates() {
        shouldUpdate = true;
        return this;
    }

    /**
     * If <code>FALSE</code>, subsequent calls to {@link #onPathsUpdated(GridPath, type) <code>onPathsUpdated()</code>}
     * can be ignored.
     * @param shouldUpdate If <code>TRUE</code>, this TransferPathManger will send packets.
     * @return This TransferPathManager
     */
    public TransferPathManager shouldUpdate(boolean shouldUpdate) {
        this.shouldUpdate = shouldUpdate;
        return this;
    }

    /**
     * Called internally whenever a GridPath is added or removed
     * @param path The path in question
     * @param type The type of packet to send, which dicates how the packet is handled by the client
     */
    private void onPathsUpdated(@Nullable GridPath path, GridSyncPacketType type) {
        if(shouldUpdate) GridSyncHelper.informPlayerPathUpdate(type, path);
    }
}
