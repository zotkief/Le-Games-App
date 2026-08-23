package com.example.le_games_app.utils.classes;

import jakarta.persistence.Embeddable;

@Embeddable
public class IntPoint2D {
    private int x;
    private int y;

    public IntPoint2D() {}

    public IntPoint2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void addPoint(IntPoint2D vector) {
        this.x += vector.x;
        this.y += vector.y;
    }

    public int squareDist(IntPoint2D point) {
        return Math.abs(this.x - point.x) + Math.abs(this.y - point.y);
    }

    public IntPoint2D copy() {
        return new IntPoint2D(this.x,this.y);
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public String toString() {
        return "("+x+","+y+")";
    }
}