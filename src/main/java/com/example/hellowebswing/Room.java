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
    
    // Entry and exit doors for linear progression
    private Door.Side entryDoor;  // Door player came from (always active if it exists)
    private Door.Side exitDoor;   // Door player can use to progress (active only when enemies defeated)
    
    /**
     * Create a room at the specified dungeon position.
     */
    public Room(String roomId, int roomX, int roomY) {
        this.roomId = roomId;
        this.roomX = roomX;
        this.roomY = roomY;
        this.enemies = new ArrayList<>();
        this.doors = new ArrayList<>();
        
        // Determine entry and exit doors based on room position
        determineEntryAndExitDoors();
        
        // Create only entry and exit doors
        initializeDoors();
    }
    
    /**
     * Determine entry and exit doors based on room position.
     * Creates a linear progression through the dungeon.
     */
    private void determineEntryAndExitDoors() {
        // Room layout: (x, y) coordinates where x: -1=left, 0=middle, 1=right; y: -1=top, 0=middle, 1=bottom
        // Linear path: Spawn -> Top -> Top-Left -> Mid-Left -> Bot-Left -> Bot-Mid -> Bot-Right -> Mid-Right -> Top-Right(Boss)
        
        if (roomX == 0 && roomY == 0) {
            // Spawn room: entry=null, exit=TOP
            entryDoor = null;
            exitDoor = Door.Side.TOP;
        } else if (roomX == 0 && roomY == -1) {
            // Top-Middle: entry=BOTTOM, exit=LEFT
            entryDoor = Door.Side.BOTTOM;
            exitDoor = Door.Side.LEFT;
        } else if (roomX == -1 && roomY == -1) {
            // Top-Left: entry=RIGHT, exit=BOTTOM
            entryDoor = Door.Side.RIGHT;
            exitDoor = Door.Side.BOTTOM;
        } else if (roomX == -1 && roomY == 0) {
            // Middle-Left: entry=TOP, exit=BOTTOM
            entryDoor = Door.Side.TOP;
            exitDoor = Door.Side.BOTTOM;
        } else if (roomX == -1 && roomY == 1) {
            // Bottom-Left: entry=TOP, exit=RIGHT
            entryDoor = Door.Side.TOP;
            exitDoor = Door.Side.RIGHT;
        } else if (roomX == 0 && roomY == 1) {
            // Bottom-Middle: entry=LEFT, exit=RIGHT
            entryDoor = Door.Side.LEFT;
            exitDoor = Door.Side.RIGHT;
        } else if (roomX == 1 && roomY == 1) {
            // Bottom-Right: entry=LEFT, exit=TOP
            entryDoor = Door.Side.LEFT;
            exitDoor = Door.Side.TOP;
        } else if (roomX == 1 && roomY == 0) {
            // Middle-Right: entry=BOTTOM, exit=TOP
            entryDoor = Door.Side.BOTTOM;
            exitDoor = Door.Side.TOP;
        } else if (roomX == 1 && roomY == -1) {
            // Top-Right (Boss): entry=BOTTOM, exit=null (win condition)
            entryDoor = Door.Side.BOTTOM;
            exitDoor = null;
        }
    }
    
    /**
     * Initialize doors - create only entry and exit doors for linear progression.
     * Top/Bottom doors: 2x1 (tiles 9-10 on their respective rows)
     * Left/Right doors: 1x2 (tiles on their columns, rows 7-8)
     */
    private void initializeDoors() {
        // Add entry door if it exists (always visible/active)
        if (entryDoor != null) {
            addDoorBySide(entryDoor);
        }
        
        // Add exit door if it exists (visibility/activation based on enemy status)
        if (exitDoor != null) {
            addDoorBySide(exitDoor);
        }
    }
    
    /**
     * Helper method to create a door on the specified side.
     */
    private void addDoorBySide(Door.Side side) {
        switch (side) {
            case TOP:
                doors.add(new Door(9, 0, 10, 0, Door.Side.TOP));
                break;
            case BOTTOM:
                doors.add(new Door(9, 14, 10, 14, Door.Side.BOTTOM));
                break;
            case LEFT:
                doors.add(new Door(0, 7, 0, 8, Door.Side.LEFT));
                break;
            case RIGHT:
                doors.add(new Door(19, 7, 19, 8, Door.Side.RIGHT));
                break;
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

    /**
     * Check if all enemies in the room have been defeated.
     */
    public boolean areAllEnemiesDefeated() {
        if (enemies.isEmpty()) {
            return true;
        }
        for (Enemy enemy : enemies) {
            if (enemy.getHealth() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Update door active state and visibility based on whether enemies are defeated.
     * Entry door is always active/visible.
     * Exit door is only active/visible when all enemies are defeated.
     */
    public void updateDoorStates() {
        boolean enemiesDefeated = areAllEnemiesDefeated();
        
        for (Door door : doors) {
            // Entry door is always active and visible
            if (entryDoor != null && door.getSide() == entryDoor) {
                door.setActive(true);
                door.setVisible(true);
            }
            // Exit door is only active/visible when enemies defeated
            else if (exitDoor != null && door.getSide() == exitDoor) {
                door.setActive(enemiesDefeated);
                door.setVisible(enemiesDefeated);
            }
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
    
    /**
     * Disable a door by its side.
     */
    public void disableDoor(Door.Side side) {
        for (Door door : doors) {
            if (door.getSide() == side) {
                door.setActive(false);
                return;
            }
        }
    }
    
    /**
     * Enable a door by its side.
     */
    public void enableDoor(Door.Side side) {
        for (Door door : doors) {
            if (door.getSide() == side) {
                door.setActive(true);
                return;
            }
        }
    }
}
