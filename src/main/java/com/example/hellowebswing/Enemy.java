package com.example.hellowebswing;

import java.util.Random;

/**
 * Enemy represents an enemy character in the game.
 * Contains enemy attributes: health, damage, position, and movement pattern logic.
 */
public class Enemy {
    
    private int health;
    private int maxHealth;
    private int damage;
    private double x;  // Now uses decimal positions for smooth movement
    private double y;
    private static final Random random = new Random();
    
    // Game boundaries
    private static final int MAX_X = 20;
    private static final int MAX_Y = 15;
    
    // Movement pattern - one of: UP, DOWN, LEFT, RIGHT
    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);
        
        public final int dx;
        public final int dy;
        
        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }
    
    private Direction currentDirection;
    private int spacesMovedInDirection;
    private int spacesUntilSwitch;  // random interval (5-12) before switching direction
    private static final double MOVEMENT_SPEED = 0.1;  // Move 0.1 tiles per tick for smooth gliding
    
    /**
     * Create an enemy at the specified position.
     */
    public Enemy(int x, int y, int health, int damage) {
        this.x = x;
        this.y = y;
        this.maxHealth = health;
        this.health = health;
        this.damage = damage;
        
        // Initialize movement pattern
        this.currentDirection = Direction.values()[random.nextInt(Direction.values().length)];
        this.spacesMovedInDirection = 0;
        this.spacesUntilSwitch = random.nextInt(8) + 5; // Random 5-12
    }
    
    // Getters
    public int getHealth() {
        return health;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public int getX() {
        return (int) Math.round(x);
    }
    
    public int getY() {
        return (int) Math.round(y);
    }
    
    public double getXRaw() {
        return x;
    }
    
    public double getYRaw() {
        return y;
    }
    
    /**
     * Apply damage to the enemy.
     */
    public void takeDamage(int damage) {
        this.health = Math.max(0, this.health - damage);
    }
    
    /**
     * Update enemy movement and handle collisions with player.
     * Returns true if the enemy damaged the player, false otherwise.
     * Enemies move smoothly at MOVEMENT_SPEED per tick for continuous gliding.
     */
    public boolean update(GameModel gameModel) {
        // This method is kept for backward compatibility with GameLoopService
        return updateWithPlayerPosition(gameModel.getPlayerX(), gameModel.getPlayerY(), null);
    }
    
    /**
     * Update enemy movement with specific player position and room.
     * Returns true if the enemy damaged the player, false otherwise.
     * Moves smoothly using decimal positions.
     */
    public boolean updateWithPlayerPosition(int playerX, int playerY, Room room) {
        boolean damagedPlayer = false;
        
        // Calculate new position based on current direction (fractional movement)
        double newX = x + (currentDirection.dx * MOVEMENT_SPEED);
        double newY = y + (currentDirection.dy * MOVEMENT_SPEED);
        
        // Get boundaries (use room if provided, otherwise use class constants)
        double maxX = (room != null) ? Room.ROOM_WIDTH : MAX_X;
        double maxY = (room != null) ? Room.ROOM_HEIGHT : MAX_Y;
        
        // Check bounds and bounce off edges
        if (newX < 0 || newX >= maxX) {
            currentDirection = (currentDirection == Direction.LEFT) ? Direction.RIGHT : Direction.LEFT;
            newX = x + (currentDirection.dx * MOVEMENT_SPEED);
        }
        if (newY < 0 || newY >= maxY) {
            currentDirection = (currentDirection == Direction.UP) ? Direction.DOWN : Direction.UP;
            newY = y + (currentDirection.dy * MOVEMENT_SPEED);
        }
        
        // Check if current position is close to player (within 0.5 tiles)
        int currentTileX = (int) Math.round(newX);
        int currentTileY = (int) Math.round(newY);
        
        if (currentTileX == playerX && currentTileY == playerY) {
            damagedPlayer = true;
        } else {
            // Move to the new position
            x = newX;
            y = newY;
            
            // Increment spaces moved (track progress for direction changes)
            spacesMovedInDirection += MOVEMENT_SPEED;
            
            // Check if it's time to switch direction (crossed a tile boundary)
            if (spacesMovedInDirection >= spacesUntilSwitch) {
                chooseNewDirection();
            }
        }
        
        return damagedPlayer;
    }
    
    /**
     * Choose a new random direction and set the interval for the next switch.
     */
    private void chooseNewDirection() {
        currentDirection = Direction.values()[random.nextInt(Direction.values().length)];
        spacesMovedInDirection = 0;
        spacesUntilSwitch = random.nextInt(8) + 5; // Random 5-12
    }
}
