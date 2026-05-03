package com.example.hellowebswing;

/**
 * Door represents a doorway that connects rooms.
 * Doors are positioned on the edges of rooms and can be traversed by the player.
 */
public class Door {
    
    // Door sides
    public enum Side {
        TOP, BOTTOM, LEFT, RIGHT;
    }
    
    private int x1, y1;  // First tile of door
    private int x2, y2;  // Second tile of door (for 2-tile doors)
    private Side side;   // Which side of the room this door is on
    private boolean active; // Whether the door is currently accessible
    
    /**
     * Create a door at the specified location and side.
     * Top/Bottom doors are 2x1 (2 tiles wide).
     * Left/Right doors are 1x2 (1 tile tall for each).
     */
    public Door(int x1, int y1, int x2, int y2, Door.Side side) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.side = side;
        this.active = true;  // Doors are active by default
    }
    
    /**
     * Check if a position is on this door (only if door is active).
     */
    public boolean containsPosition(int x, int y) {
        if (!active) {
            return false;
        }
        return (x == x1 && y == y1) || (x == x2 && y == y2);
    }
    
    /**
     * Get the opposite side of the room (where player exits).
     */
    public Side getOppositeSide() {
        switch (side) {
            case TOP:
                return Side.BOTTOM;
            case BOTTOM:
                return Side.TOP;
            case LEFT:
                return Side.RIGHT;
            case RIGHT:
                return Side.LEFT;
            default:
                return null;
        }
    }
    
    public int getX1() {
        return x1;
    }
    
    public int getY1() {
        return y1;
    }
    
    public int getX2() {
        return x2;
    }
    
    public int getY2() {
        return y2;
    }
    
    public Side getSide() {
        return side;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}
