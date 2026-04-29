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
        
        // Start in the main dungeon room
        this.currentRoom = rooms.get("dungeon_main");
    }
    
    /**
     * Initialize all dungeon rooms.
     */
    private void initializeRooms() {
        // Main dungeon room
        Room mainDungeon = new Room("dungeon_main");
        mainDungeon.addEnemy(new Enemy(15, 7, 30, 10));
        mainDungeon.addEnemy(new Enemy(10, 12, 30, 10));
        rooms.put("dungeon_main", mainDungeon);
        
        // Additional rooms (for now, just empty rooms)
        rooms.put("dungeon_top", new Room("dungeon_top"));
        rooms.put("dungeon_bottom", new Room("dungeon_bottom"));
        rooms.put("dungeon_left", new Room("dungeon_left"));
        rooms.put("dungeon_right", new Room("dungeon_right"));
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
        String nextRoomId = getNextRoomId(door.getSide());
        if (nextRoomId != null && rooms.containsKey(nextRoomId)) {
            currentRoom = rooms.get(nextRoomId);
            
            // Teleport player to opposite side of new room
            Door.Side oppositeSide = door.getOppositeSide();
            teleportToOppositeSide(oppositeSide);
        }
    }
    
    /**
     * Get the next room ID based on the door side.
     */
    private String getNextRoomId(Door.Side side) {
        switch (side) {
            case TOP:
                return "dungeon_top";
            case BOTTOM:
                return "dungeon_bottom";
            case LEFT:
                return "dungeon_left";
            case RIGHT:
                return "dungeon_right";
            default:
                return null;
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
            if (enemy.updateWithPlayerPosition(playerX, playerY, currentRoom)) {
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
