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
    
    // Invulnerability system (3 seconds = 3000 ms)
    private long invulnerabilityEndTime = 0;
    private static final long INVULNERABILITY_DURATION = 3000;  // 3 seconds in milliseconds
    
    // Jackson requires public getters for serialization - these are read by the frontend
    @com.fasterxml.jackson.annotation.JsonProperty("invulnerabilityTimeRemaining")
    public long getInvulnerabilityTimeRemainingForSerialization() {
        return getRemainingInvulnerabilityTime();
    }
    
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
        this.invulnerabilityEndTime = 0;  // Initialize invulnerability
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
     * Linear path: Spawn -> Top -> Top-Left -> Mid-Left -> Bot-Left -> Bot-Mid -> Bot-Right -> Mid-Right -> Top-Right(Boss)
     */
    private void initializeRooms() {
        // Create a 3x3 grid of rooms
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                String key = getRoomKey(x, y);
                Room room = new Room(key, x, y);
                rooms.put(key, room);
                
                // Add enemies based on room position
                if (x == 0 && y == 0) {
                    // Spawn room (0, 0) - starting area with enemies
                    room.addEnemy(new Enemy(15, 7, 20, 10));
                    room.addEnemy(new Enemy(10, 12, 20, 10));
                } else if (x == 0 && y == -1) {
                    // Top-Middle (0, -1)
                    room.addEnemy(new Enemy(12, 8, 20, 10));
                    room.addEnemy(new Enemy(8, 7, 20, 10));
                } else if (x == -1 && y == -1) {
                    // Top-Left (-1, -1)
                    room.addEnemy(new Enemy(10, 10, 20, 10));
                } else if (x == -1 && y == 0) {
                    // Middle-Left (-1, 0)
                    room.addEnemy(new Enemy(14, 8, 20, 10));
                    room.addEnemy(new Enemy(6, 7, 20, 10));
                } else if (x == -1 && y == 1) {
                    // Bottom-Left (-1, 1)
                    room.addEnemy(new Enemy(10, 9, 20, 10));
                } else if (x == 0 && y == 1) {
                    // Bottom-Middle (0, 1)
                    room.addEnemy(new Enemy(12, 7, 20, 10));
                } else if (x == 1 && y == 1) {
                    // Bottom-Right (1, 1)
                    room.addEnemy(new Enemy(10, 8, 20, 10));
                } else if (x == 1 && y == 0) {
                    // Middle-Right (1, 0)
                    room.addEnemy(new Enemy(15, 10, 20, 10));
                    room.addEnemy(new Enemy(5, 6, 20, 10));
                } else if (x == 1 && y == -1) {
                    // Top-Right (1, -1) - Boss room
                    room.addEnemy(new Enemy(10, 7, 20, 10));
                    room.addEnemy(new Enemy(10, 4, 20, 10));
                    room.addEnemy(new Enemy(10, 10, 20, 10));
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
     * Checks invulnerability status before applying damage.
     */
    public boolean takeDamage(int damage) {
        // Check if player is currently invulnerable
        if (isInvulnerable()) {
            return false;  // Damage blocked by invulnerability
        }
        
        this.playerHealth = Math.max(0, playerHealth - damage);
        // Set invulnerability for 3 seconds after taking damage
        this.invulnerabilityEndTime = System.currentTimeMillis() + INVULNERABILITY_DURATION;
        return true;  // Damage was applied
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
     * Rooms have predefined entry/exit doors, so door states are already correct.
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
            // Move to the next room
            currentRoom = rooms.get(nextRoomKey);
            playerRoomX = nextRoomX;
            playerRoomY = nextRoomY;
            
            // Teleport player to opposite side of new room
            Door.Side oppositeSide = door.getOppositeSide();
            teleportToOppositeSide(oppositeSide);
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
     * Respects invulnerability frames.
     */
    public void damagePlayer(int damage) {
        takeDamage(damage);
    }

    
    
    /**
     * Check if player is currently invulnerable.
     */
    public boolean isInvulnerable() {
        return System.currentTimeMillis() < invulnerabilityEndTime;
    }
    
    /**
     * Get remaining invulnerability time in milliseconds.
     */
    public long getRemainingInvulnerabilityTime() {
        long remaining = invulnerabilityEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Update all enemies in the game.
     * Removes dead enemies and updates door states.
     */
    public void updateEnemies() {
        // Remove dead enemies from the room
        currentRoom.getEnemies().removeIf(enemy -> enemy.getHealth() <= 0);
        
        // Update living enemies
        for (Enemy enemy : currentRoom.getEnemies()) {
            if (enemy.updateWithPlayerPosition(getPlayerX(), getPlayerY(), currentRoom)) {
                damagePlayer(enemy.getDamage());
            }
        }
        // Update door states based on enemy status
        currentRoom.updateDoorStates();
    }

    /**
     * Player performs a sword attack in the given direction.
     * Checks for enemies and damages them with knockback.
     * Uses tolerance-based hit detection to account for smooth decimal movement.
     */
    public void attackInDirection(String direction) {
        int attackX = playerX;
        int attackY = playerY;
        Enemy.Direction attackDir = null;

        switch (direction.toLowerCase()) {
            case "up":
                attackY--;
                attackDir = Enemy.Direction.UP;
                break;
            case "down":
                attackY++;
                attackDir = Enemy.Direction.DOWN;
                break;
            case "left":
                attackX--;
                attackDir = Enemy.Direction.LEFT;
                break;
            case "right":
                attackX++;
                attackDir = Enemy.Direction.RIGHT;
                break;
            default:
                return;
        }

        if (attackX < 0 || attackX >= Room.ROOM_WIDTH || attackY < 0 || attackY >= Room.ROOM_HEIGHT) {
            return;
        }

        // Use tolerance-based hit detection to handle smooth decimal movement
        // Enemy positions might be offset slightly due to movement speed
        double hitTolerance = 0.6;  // Tolerance for hit detection
        
        for (Enemy enemy : currentRoom.getEnemies()) {
            if (enemy.getHealth() > 0) {
                // Check if enemy is close enough to the attack tile
                double distX = Math.abs(enemy.getXRaw() - attackX);
                double distY = Math.abs(enemy.getYRaw() - attackY);
                
                if (distX < hitTolerance && distY < hitTolerance) {
                    enemy.takeDamage(10, attackDir);
                    break;
                }
            }
        }

        // After attack, update door states in case enemies died
        currentRoom.updateDoorStates();
    }
    
    /**
     * Get the current room.
     */
    public Room getCurrentRoom() {
        return currentRoom;
    }
    
    /**
     * Reset invulnerability (used when resetting the game).
     */
    public void resetInvulnerability() {
        this.invulnerabilityEndTime = 0;
    }
}
