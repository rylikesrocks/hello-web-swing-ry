package com.example.hellowebswing;

import java.util.ArrayList;
import java.util.List;

/**
 * Room represents a room in the dungeon.
 * Each room has a grid boundary (20x15), enemies, and doors to adjacent rooms.
 * Rooms are positioned in a grid system where player can move 2 rooms in each direction.
 * Eventually will include obstacles, lava, and different enemy types.
 */
public class Room {
    
    // Room boundaries
    public static final int ROOM_WIDTH = 20;
    public static final int ROOM_HEIGHT = 15;
    
    // Room position in the dungeon grid
    private int roomX;
    private int roomY;
    
    private String roomId;
    private List<Enemy> enemies;
    private List<Door> doors;
    
    /**
     * Create a room at the specified dungeon position.
     */
    public Room(String roomId, int roomX, int roomY) {
        this.roomId = roomId;
        this.roomX = roomX;
        this.roomY = roomY;
        this.enemies = new ArrayList<>();
        this.doors = new ArrayList<>();
        
        // Create doors on all four sides
        initializeDoors();
    }
    
    /**
     * Initialize doors in the center of each side.
     * Top/Bottom doors: 2x1 (tiles 9-10 on their respective rows)
     * Left/Right doors: 1x2 (tiles on their columns, rows 7-8)
     * Disables doors based on room position constraints (max 2 rooms in each direction).
     */
    private void initializeDoors() {
        // Top door (center top, 2 tiles wide) - disabled if at topmost room (roomY == -2)
        Door topDoor = new Door(9, 0, 10, 0, Door.Side.TOP);
        if (roomY > -2) {
            doors.add(topDoor);
        }
        
        // Bottom door (center bottom, 2 tiles wide) - disabled if at bottommost room (roomY == 2)
        Door bottomDoor = new Door(9, 14, 10, 14, Door.Side.BOTTOM);
        if (roomY < 2) {
            doors.add(bottomDoor);
        }
        
        // Left door (center left, 2 tiles tall) - disabled if at leftmost room (roomX == -2)
        Door leftDoor = new Door(0, 7, 0, 8, Door.Side.LEFT);
        if (roomX > -2) {
            doors.add(leftDoor);
        }
        
        // Right door (center right, 2 tiles tall) - disabled if at rightmost room (roomX == 2)
        Door rightDoor = new Door(19, 7, 19, 8, Door.Side.RIGHT);
        if (roomX < 2) {
            doors.add(rightDoor);
        }
    }
    
    /**
     * Check if a position is on a door and return that door.
     */
    public Door getDoorAt(int x, int y) {
        for (Door door : doors) {
            if (door.containsPosition(x, y)) {
                return door;
            }
        }
        return null;
    }
    
    /**
     * Add an enemy to the room.
     */
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }
    
    /**
     * Get all enemies in the room.
     */
    public List<Enemy> getEnemies() {
        return enemies;
    }
    
    /**
     * Get all doors in the room.
     */
    public List<Door> getDoors() {
        return doors;
    }
    
    /**
     * Update all enemies in the room.
     */
    public void updateEnemies(int playerX, int playerY) {
        for (Enemy enemy : enemies) {
            enemy.updateWithPlayerPosition(playerX, playerY, this);
        }
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public int getRoomX() {
        return roomX;
    }
    
    public int getRoomY() {
        return roomY;
    }
}
