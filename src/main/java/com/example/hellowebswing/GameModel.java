package com.example.hellowebswing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameModel represents the game state and contains all game logic.
 * Stores information like player health, position, inventory, etc.
 * Manages rooms and navigation between them.
 * Handles all state changes and game mechanics.
 */
public class GameModel {
    
    private int playerHealth;
    private int playerMaxHealth;
    private int playerX;
    private int playerY;
    
    // Track player position in dungeon grid
    private int playerRoomX;
    private int playerRoomY;
    
    private Room currentRoom;
    private Map<String, Room> rooms;
    
    /**
     * Initialize the game model with default values.
     */
    public GameModel() {
        this.playerMaxHealth = 100;
        this.playerHealth = 100;
        this.playerX = 10;
        this.playerY = 7;
        
        // Initialize rooms
        this.rooms = new HashMap<>();
        initializeRooms();
        
        // Start in the center of the dungeon (0, 0)
        this.playerRoomX = 0;
        this.playerRoomY = 0;
        this.currentRoom = rooms.get(getRoomKey(0, 0));
    }
    
    /**
     * Initialize all 9 dungeon rooms in a 3x3 grid.
     * Grid coordinates: X and Y range from -1 to 1.
     * Center room is at (0, 0).
     */
    private void initializeRooms() {
        // Create a 3x3 grid of rooms
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                String key = getRoomKey(x, y);
                Room room = new Room(key, x, y);
                rooms.put(key, room);
                
                // Add enemies only to certain rooms for variety
                if (x == 0 && y == 0) {
                    // Center room with some enemies
                    room.addEnemy(new Enemy(15, 7, 30, 10));
                    room.addEnemy(new Enemy(10, 12, 30, 10));
                }
            }
        }
    }
    
    /**
     * Get the unique key for a room at the given coordinates.
     */
    private String getRoomKey(int x, int y) {
        return "dungeon_" + x + "_" + y;
    }
    
    // Getters
    public int getPlayerHealth() {
        return playerHealth;
    }
    
    public int getPlayerMaxHealth() {
        return playerMaxHealth;
    }
    
    public int getPlayerX() {
        return playerX;
    }
    
    public int getPlayerY() {
        return playerY;
    }
    
    public int getPlayerRoomX() {
        return playerRoomX;
    }
    
    public int getPlayerRoomY() {
        return playerRoomY;
    }
    
    /**
     * Apply damage to the player (game logic).
     */
    public void takeDamage(int damage) {
        this.playerHealth = Math.max(0, Math.min(playerHealth - damage, playerMaxHealth));
    }
    
    /**
     * Heal the player (game logic).
     */
    public void heal(int amount) {
        this.playerHealth = Math.max(0, Math.min(playerHealth + amount, playerMaxHealth));
    }
    
    /**
     * Move the player in a given direction (game logic).
     * Enforces dungeon boundaries and handles room transitions through doors.
     */
    public void movePlayer(String direction) {
        int newX = playerX;
        int newY = playerY;
        
        switch (direction.toLowerCase()) {
            case "up":
                newY--;
                break;
            case "down":
                newY++;
                break;
            case "left":
                newX--;
                break;
            case "right":
                newX++;
                break;
            default:
                return;
        }
        
        // Check if player is moving to a door
        Door door = currentRoom.getDoorAt(newX, newY);
        if (door != null) {
            // Transition to adjacent room through the door
            transitionRoom(door);
        } else if (newX >= 0 && newX < Room.ROOM_WIDTH && newY >= 0 && newY < Room.ROOM_HEIGHT) {
            // Normal movement within room bounds
            playerX = newX;
            playerY = newY;
        }
    }
    
    /**
     * Transition to an adjacent room through a door.
     */
    private void transitionRoom(Door door) {
        // Calculate the next room position based on the door side
        int nextRoomX = playerRoomX;
        int nextRoomY = playerRoomY;
        
        switch (door.getSide()) {
            case TOP:
                nextRoomY--;
                break;
            case BOTTOM:
                nextRoomY++;
                break;
            case LEFT:
                nextRoomX--;
                break;
            case RIGHT:
                nextRoomX++;
                break;
        }
        
        // Check if the next room exists
        String nextRoomKey = getRoomKey(nextRoomX, nextRoomY);
        if (rooms.containsKey(nextRoomKey)) {
            // Disable the door we just used in the current room
            currentRoom.disableDoor(door.getSide());
            
            // Enable the opposite door in the current room (so player can come back)
            // Actually no - we already created the room with the opposite door disabled
            // We need to enable the door the player is leaving FROM in the current room
            
            // Move to the next room
            currentRoom = rooms.get(nextRoomKey);
            playerRoomX = nextRoomX;
            playerRoomY = nextRoomY;
            
            // Teleport player to opposite side of new room
            Door.Side oppositeSide = door.getOppositeSide();
            teleportToOppositeSide(oppositeSide);
            
            // Enable the door we came through in the new room (so player can go back)
            currentRoom.enableDoor(oppositeSide);
        }
    }
    
    /**
     * Teleport player to the opposite side of the room.
     */
    private void teleportToOppositeSide(Door.Side side) {
        switch (side) {
            case TOP:
                playerX = 10;
                playerY = 1;
                break;
            case BOTTOM:
                playerX = 10;
                playerY = 13;
                break;
            case LEFT:
                playerX = 1;
                playerY = 7;
                break;
            case RIGHT:
                playerX = 18;
                playerY = 7;
                break;
        }
    }
    
    /**
     * Get the list of enemies in the current room.
     */
    public List<Enemy> getEnemies() {
        return currentRoom.getEnemies();
    }
    
    /**
     * Check if an enemy is at the specified position (excluding a specific enemy).
     */
    public boolean isEnemyAt(int x, int y, Enemy exclude) {
        for (Enemy enemy : currentRoom.getEnemies()) {
            if (enemy != exclude && enemy.getX() == x && enemy.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Damage the player from an enemy attack.
     */
    public void damagePlayer(int damage) {
        this.playerHealth = Math.max(0, playerHealth - damage);
    }
    
    /**
     * Update all enemies in the game.
     */
    public void updateEnemies() {
        for (Enemy enemy : currentRoom.getEnemies()) {
            if (enemy.updateWithPlayerPosition(getPlayerX(), getPlayerY(), currentRoom)) {
                damagePlayer(enemy.getDamage());
            }
        }
    }
    
    /**
     * Get the current room.
     */
    public Room getCurrentRoom() {
        return currentRoom;
    }
}
