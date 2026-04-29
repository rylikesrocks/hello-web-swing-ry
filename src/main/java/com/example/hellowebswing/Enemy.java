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
    private int x;
    private int y;
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
    private int moveTickCounter;    // counter to slow down movement (move every N ticks)
    private static final int MOVE_FREQUENCY = 10;  // Move every 10 game ticks (~6 moves/sec at 60 FPS)
    
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
        this.moveTickCounter = 0;
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
        return x;
    }
    
    public int getY() {
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
     * Movement is throttled to move every MOVE_FREQUENCY ticks for slower, more controllable movement.
     */
    public boolean update(GameModel gameModel) {
        // This method is kept for backward compatibility with GameLoopService
        return updateWithPlayerPosition(gameModel.getPlayerX(), gameModel.getPlayerY(), null);
    }
    
    /**
     * Update enemy movement with specific player position and room.
     * Returns true if the enemy damaged the player, false otherwise.
     */
    public boolean updateWithPlayerPosition(int playerX, int playerY, Room room) {
        boolean damagedPlayer = false;
        
        // Increment tick counter and only move if we've hit the move frequency
        moveTickCounter++;
        if (moveTickCounter < MOVE_FREQUENCY) {
            return false;  // Don't move this tick
        }
        
        // Reset tick counter and proceed with movement
        moveTickCounter = 0;
        
        // Calculate new position based on current direction
        int newX = x + currentDirection.dx;
        int newY = y + currentDirection.dy;
        
        // Get boundaries (use room if provided, otherwise use class constants)
        int maxX = (room != null) ? Room.ROOM_WIDTH : MAX_X;
        int maxY = (room != null) ? Room.ROOM_HEIGHT : MAX_Y;
        
        // Check bounds and bounce off edges
        if (newX < 0 || newX >= maxX) {
            currentDirection = (currentDirection == Direction.LEFT) ? Direction.RIGHT : Direction.LEFT;
            newX = x + currentDirection.dx;
        }
        if (newY < 0 || newY >= maxY) {
            currentDirection = (currentDirection == Direction.UP) ? Direction.DOWN : Direction.UP;
            newY = y + currentDirection.dy;
        }
        
        // Check if moving into player
        if (newX == playerX && newY == playerY) {
            if (room != null) {
                // If in a room context, don't directly damage; let room handle it
                damagedPlayer = true;
            }
        } else {
            // Move to the new position
            x = newX;
            y = newY;
        }
        
        // Increment spaces moved in current direction
        spacesMovedInDirection++;
        
        // Check if it's time to switch direction
        if (spacesMovedInDirection >= spacesUntilSwitch) {
            chooseNewDirection();
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
