package com.quattage.mechano.foundation.electricity.grid.landmarks.client;

public class GridClientVertex {

    private final int cc;

    // for testing only
    private final float f;
    private final float heur;
    private final float cum;
    private final boolean isMember;

    public GridClientVertex(int cc, float f, float heur, float cum, boolean isMember) {
        this.cc = cc;
        this.f = f;
        this.heur = heur;
        this.cum = cum;
        this.isMember = isMember;
    }

    public int getConnections() { return cc; }
    public float getF() { return f; } 
    public float getHeuristic() { return heur; }
    public float getCumulative() { return cum; }
    public boolean isMember() { return isMember; }
}