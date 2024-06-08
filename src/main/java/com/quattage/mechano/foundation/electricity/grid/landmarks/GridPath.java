package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.core.watt.unit.WattUnit;

public class GridPath {

    private final GridVertex[] path;
    private final WattUnit transferStats;

    public GridPath(GridVertex[] path, WattUnit transferStats) {
        this.path = path;
        this.transferStats = transferStats;
    }

    public GridPath(Collection<GridVertex> members, WattUnit transferStats) {
        this.path = members.toArray(GridVertex[]::new);
        this.transferStats = transferStats;
    }

    /**
     * Unwinds a Map, where key-value pairs represent links, into a <code>GridPath</code> and returns it.
     * @param path Map of GridVertices describing a path
     * @param goal The ending GridVertex, used to establish insertion order of the resulting GridPath
     * @param slowestRate The slowest rate encountered during each leap of this GridPath, used to describe the total TransferRate of the resulting GridPath.
     * @return A new GridPath instance, or null if no valid path could be created frorm the supplied map.
     */
    @Nullable
    public static GridPath ofUnwound(Map<GridVertex, GridVertex> path, GridVertex goal, WattUnit slowestRate) {

        if(path == null || path.isEmpty()) return null;

        final List<GridVertex> pathList = new ArrayList<GridVertex>();
        pathList.add(goal);

        while(path.containsKey(goal)) {
            goal = path.get(goal);
            pathList.add(goal);
        }

        return (!pathList.isEmpty()) ? new GridPath(pathList, slowestRate) : null;
    }

    public WattUnit getTransferStats() {
        return transferStats;
    }

    /**
     * Gets the "starting" point GridVertex. <p>
     * <strong>Note:</strong> Paths technically don't have any directionality/handedness. 
     * The terms "start" and "end" are used here colloquially and only indicate the arbitrary order that
     * this path was made in, but the path itself can work in both directions.
     * @return The GridVertex at index <code>0</code> of this path.
     */
    public GridVertex getStart() {
        return path[0];
    }

    /**
     * Gets the "ending" point GridVertex <p>
     * <strong>Note:</strong> Paths technically don't have any directionality/handedness. 
     * The terms "start" and "end" are used here colloquially and only indicate the arbitrary order that
     * this path was made in, but the path itself can work in both directions.
     * @return The GridVertex at index <code>length - 1</code> of this path.
     */
    public GridVertex getEnd() {
        return path[path.length - 1];
    }

    /**
     * Converts this GridPath into a {@link GIDPair <code>GIDPair</code>} object
     * for easy hashing
     * @return A new GIDPair instance containing the two ends of this path.
     */
    public GIDPair getHashable() {
        return new GIDPair(path[0].getID(), path[path.length - 1].getID());
    }

    /**
     * @return An array representing each "leap" in this path
     */
    public GridVertex[] members() {
        return path;
    }

    /**
     * @return The amount of members in this path
     */
    public int size() { 
        return path.length;
    }

    public String toString() {
        if(path.length == 0) return "{EMPTY}";
        String out = "{";

        int x = 0;
        for(GridVertex vert : path) {
            x++;
            out += vert.posAsString() + (x < path.length ? " -> " : "}");
        }
        return out;
    }

    /**
     * 
     * @param other GridVertex to compare
     * @return <code>TRUE</code> if the provided GridVertex is at index <code>0</code> or <code>length - 1</code> in this GridPath.
     */
    public boolean isEnd(GridVertex other) {
        return path[0].equals(other) || path[path.length - 1].equals(other); 
    }

    public boolean containsVertex(GridVertex other) {
        for(GridVertex vert : path)
            if(other.equals(vert)) return true;
        return false;
    }

    public boolean containsKey(GID key) {
        for(GridVertex vert : path) 
            if(vert.getID().equals(key)) return true;
        return false;
    }
}
