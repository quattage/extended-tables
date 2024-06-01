package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
     * @param start The starting GridVertex, just provided to reset A* guidance variables
     * @param path Map of GridVertices describing a path
     * @param goal The ending GridVertex, used to establish insertion order of the resulting GridPath
     * @param slowestRate The slowest rate encountered during each leap of this GridPath, used to describe the total TransferRate of the resulting GridPath.
     * @param reset If <code>TRUE</code> this call will reset the A* values of each supplied <code>GridVertex</code> instance during iteration.
     * @return A new GridPath instance, or null if no valid path could be created frorm the supplied map.
     */
    @Nullable
    public static GridPath ofUnwound(GridVertex start, Map<GridVertex, GridVertex> path, GridVertex goal, WattUnit slowestRate, boolean reset) {
        
        if(path == null || path.isEmpty()) return null;

        final List<GridVertex> pathList = new ArrayList<GridVertex>();
        pathList.add(goal);
        if(reset) {
            start.resetHeuristics();
            goal.resetHeuristics();
        }

        while(path.containsKey(goal)) {
            goal = path.get(goal);
            if(reset) goal.resetHeuristics();
            pathList.add(goal);
        }

        return (!pathList.isEmpty()) ? new GridPath(pathList, slowestRate) : null;
    }

    public WattUnit getTransferStats() {
        return transferStats;
    }

    public GridVertex getStart() {
        return path[0];
    }

    public GridVertex getEnd() {
        return path[path.length - 1];
    }

    public GIDPair getHashable() {
        return new GIDPair(path[0].getID(), path[path.length - 1].getID());
    }

    public GridVertex[] members() {
        return path;
    }

    public int size() { 
        return path.length;
    }

    public String toString() {
        if(path.length == 0) return "Path {EMPTY}";
        String out = "Path {";

        int x = 0;
        for(GridVertex vert : path) {
            x++;
            out += vert.posAsString() + (x < path.length ? " -> " : "}");
        }
        return out;
    }

    public boolean isEnd(GridVertex other) {
        return path[0].equals(other) || path[path.length - 1].equals(other); 
    }

    public boolean contains(GridVertex other) {
        for(GridVertex vert : path)
            if(other.equals(vert)) return true;
        return false;
    }
}
