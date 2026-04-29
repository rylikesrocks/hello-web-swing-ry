// Game configuration
const GAME_API_URL = '/api/game';
const TILE_SIZE = 40;
const DUNGEON_WIDTH = 20;
const DUNGEON_HEIGHT = 15;
const TARGET_FPS = 60;
const FRAME_TIME = 1000 / TARGET_FPS; // ~16.67ms per frame
const INPUT_TICK_RATE = 10; // Process one buffered input every 10 frames (6 inputs/sec max)
const ENEMY_FETCH_RATE = 5; // Fetch enemies every 5 frames (~12 fps enemy update)

// Game state
let gameState = null;
let enemies = [];
let doors = [];
let canvas = null;
let ctx = null;
let lastFrameTime = 0;

// Input buffering
let pressedKeys = new Set();
let inputTickCounter = 0;
let enemyTickCounter = 0;

// Initialize the game when the page loads
window.addEventListener('DOMContentLoaded', () => {
    initializeGame();
    setupEventListeners();
    gameLoop();
});

/**
 * Initialize the game canvas and start the game loop
 */
function initializeGame() {
    canvas = document.getElementById('gameCanvas');
    ctx = canvas.getContext('2d');
    
    // Fetch initial game state, enemies, and doors
    Promise.all([
        fetch(`${GAME_API_URL}/state`).then(r => r.json()),
        fetch(`${GAME_API_URL}/enemies`).then(r => r.json()),
        fetch(`${GAME_API_URL}/doors`).then(r => r.json())
    ])
    .then(([state, enemyList, doorList]) => {
        gameState = state;
        enemies = enemyList;
        doors = doorList;
        updateUI();
    })
    .catch(error => console.error('Error initializing game:', error));
    
    // Start the game loop
    gameLoop(0);
}

/**
 * Set up button and keyboard event listeners
 */
function setupEventListeners() {
    document.getElementById('damageBtn').addEventListener('click', () => {
        fetch(`${GAME_API_URL}/damage?amount=10`, { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                gameState = data;
                updateUI();
            })
            .catch(error => console.error('Error:', error));
    });
    
    document.getElementById('healBtn').addEventListener('click', () => {
        fetch(`${GAME_API_URL}/heal?amount=20`, { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                gameState = data;
                updateUI();
            })
            .catch(error => console.error('Error:', error));
    });
    
    document.getElementById('resetBtn').addEventListener('click', () => {
        fetch(`${GAME_API_URL}/reset`, { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                gameState = data;
                updateUI();
            })
            .catch(error => console.error('Error:', error));
    });
    
    // Keyboard movement - buffer key presses instead of sending requests immediately
    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('keyup', handleKeyUp);
}

/**
 * Handle keyboard key down - buffer the key press
 */
function handleKeyDown(event) {
    let direction = null;
    
    switch(event.key) {
        case 'ArrowUp':
        case 'w':
        case 'W':
            direction = 'up';
            event.preventDefault();
            break;
        case 'ArrowDown':
        case 's':
        case 'S':
            direction = 'down';
            event.preventDefault();
            break;
        case 'ArrowLeft':
        case 'a':
        case 'A':
            direction = 'left';
            event.preventDefault();
            break;
        case 'ArrowRight':
        case 'd':
        case 'D':
            direction = 'right';
            event.preventDefault();
            break;
        default:
            return;
    }
    
    if (direction) {
        pressedKeys.add(direction);
    }
}

/**
 * Handle keyboard key up - stop tracking this key
 */
function handleKeyUp(event) {
    let direction = null;
    
    switch(event.key) {
        case 'ArrowUp':
        case 'w':
        case 'W':
            direction = 'up';
            event.preventDefault();
            break;
        case 'ArrowDown':
        case 's':
        case 'S':
            direction = 'down';
            event.preventDefault();
            break;
        case 'ArrowLeft':
        case 'a':
        case 'A':
            direction = 'left';
            event.preventDefault();
            break;
        case 'ArrowRight':
        case 'd':
        case 'D':
            direction = 'right';
            event.preventDefault();
            break;
        default:
            return;
    }
    
    if (direction) {
        pressedKeys.delete(direction);
    }
}

/**
 * Send a move request to the server and fetch updated game state, enemies, and doors
 */
function movePlayer(direction) {
    fetch(`${GAME_API_URL}/move?direction=${direction}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            gameState = data;
            updateUI();
            // Also fetch enemies and doors to get their updated positions
            Promise.all([
                fetch(`${GAME_API_URL}/enemies`).then(r => r.json()),
                fetch(`${GAME_API_URL}/doors`).then(r => r.json())
            ])
            .then(([enemyList, doorList]) => {
                enemies = enemyList;
                doors = doorList;
            })
            .catch(error => console.error('Error fetching enemies/doors:', error));
        })
        .catch(error => console.error('Error:', error));
}

/**
 * Fetch the latest game state and enemies from the server
 */
function fetchGameState() {
    Promise.all([
        fetch(`${GAME_API_URL}/state`).then(r => r.json()),
        fetch(`${GAME_API_URL}/enemies`).then(r => r.json())
    ])
    .then(([state, enemyList]) => {
        gameState = state;
        enemies = enemyList;
        updateUI();
    })
    .catch(error => console.error('Error fetching game state:', error));
}

/**
 * Update the UI elements (health bar, etc.)
 */
function updateUI() {
    if (!gameState) return;
    
    // Update health bar
    const healthPercent = (gameState.playerHealth / gameState.playerMaxHealth) * 100;
    document.getElementById('healthFill').style.width = healthPercent + '%';
    document.getElementById('healthText').textContent = 
        `${gameState.playerHealth}/${gameState.playerMaxHealth}`;
    
    // Change health bar color based on health
    const healthFill = document.getElementById('healthFill');
    if (healthPercent > 50) {
        healthFill.style.background = 'linear-gradient(to right, #4CAF50, #8BC34A)';
    } else if (healthPercent > 25) {
        healthFill.style.background = 'linear-gradient(to right, #FFC107, #FF9800)';
    } else {
        healthFill.style.background = 'linear-gradient(to right, #F44336, #E91E63)';
    }
}

/**
 * Main game loop - runs at 60 FPS with buffered input processing and periodic enemy fetching.
 * Processes one buffered input every INPUT_TICK_RATE frames to prevent request spam.
 * Fetches enemy positions every ENEMY_FETCH_RATE frames to show their movement.
 */
function gameLoop(currentTime) {
    // Calculate delta time
    if (lastFrameTime === 0) {
        lastFrameTime = currentTime;
    }
    
    const deltaTime = currentTime - lastFrameTime;
    lastFrameTime = currentTime;
    
    // Process buffered input at controlled rate
    inputTickCounter++;
    if (inputTickCounter >= INPUT_TICK_RATE && pressedKeys.size > 0) {
        inputTickCounter = 0;
        
        // Get the first direction from the buffer and send it
        const direction = pressedKeys.values().next().value;
        movePlayer(direction);
    }
    
    // Fetch enemies and doors periodically to show their movement and room state
    enemyTickCounter++;
    if (enemyTickCounter >= ENEMY_FETCH_RATE) {
        enemyTickCounter = 0;
        
        // Fetch enemies and doors to update their positions
        Promise.all([
            fetch(`${GAME_API_URL}/enemies`).then(r => r.json()),
            fetch(`${GAME_API_URL}/doors`).then(r => r.json())
        ])
        .then(([enemyList, doorList]) => {
            enemies = enemyList;
            doors = doorList;
        })
        .catch(error => console.error('Error fetching enemies/doors:', error));
    }
    
    // Render every frame
    render();
    
    // Schedule next frame
    requestAnimationFrame(gameLoop);
}

/**
 * Render the game scene
 */
function render() {
    if (!gameState || !ctx) return;
    
    // Clear the canvas
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw dungeon background
    drawDungeon();
    
    // Draw doors
    drawDoors();
    
    // Draw enemies
    drawEnemies();
    
    // Draw player
    drawPlayer();
    
    // Draw grid (optional, for visualization)
    drawGrid();
}

/**
 * Draw the dungeon background
 */
function drawDungeon() {
    ctx.fillStyle = '#8D8C86';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw dungeon border
    ctx.strokeStyle = '#3a3a3a';
    ctx.lineWidth = 2;
    ctx.strokeRect(0, 0, canvas.width, canvas.height);
}

/**
 * Draw all doors in the current room
 */
function drawDoors() {
    if (!doors || doors.length === 0) return;
    
    for (let door of doors) {
        // Draw first tile of door
        const doorx1 = door.x1 * TILE_SIZE;
        const doory1 = door.y1 * TILE_SIZE;
        
        // Draw door with distinct color (cyan/bright blue)
        ctx.fillStyle = 'rgba(0, 200, 255, 0.6)';
        ctx.fillRect(doorx1, doory1, TILE_SIZE, TILE_SIZE);
        
        // Draw door border
        ctx.strokeStyle = '#00D4FF';
        ctx.lineWidth = 2;
        ctx.strokeRect(doorx1, doory1, TILE_SIZE, TILE_SIZE);
        
        // Draw second tile of door if different positions
        if (door.x1 !== door.x2 || door.y1 !== door.y2) {
            const doorx2 = door.x2 * TILE_SIZE;
            const doory2 = door.y2 * TILE_SIZE;
            
            ctx.fillStyle = 'rgba(0, 200, 255, 0.6)';
            ctx.fillRect(doorx2, doory2, TILE_SIZE, TILE_SIZE);
            
            ctx.strokeStyle = '#00D4FF';
            ctx.lineWidth = 2;
            ctx.strokeRect(doorx2, doory2, TILE_SIZE, TILE_SIZE);
        }
    }
}

/**
 * Draw the player character
 */
function drawPlayer() {
    const playerScreenX = gameState.playerX * TILE_SIZE;
    const playerScreenY = gameState.playerY * TILE_SIZE;
    
    // Draw player as a green square
    ctx.fillStyle = '#4CAF50';
    ctx.fillRect(playerScreenX, playerScreenY, TILE_SIZE, TILE_SIZE);
    
    // Draw player border
    ctx.strokeStyle = '#8BC34A';
    ctx.lineWidth = 2;
    ctx.strokeRect(playerScreenX, playerScreenY, TILE_SIZE, TILE_SIZE);
}

/**
 * Draw all enemies
 */
function drawEnemies() {
    for (let enemy of enemies) {
        const enemyScreenX = enemy.x * TILE_SIZE;
        const enemyScreenY = enemy.y * TILE_SIZE;
        
        // Draw enemy as a red square
        ctx.fillStyle = '#F44336';
        ctx.fillRect(enemyScreenX, enemyScreenY, TILE_SIZE, TILE_SIZE);
        
        // Draw enemy border (orange)
        ctx.strokeStyle = '#FF9800';
        ctx.lineWidth = 2;
        ctx.strokeRect(enemyScreenX, enemyScreenY, TILE_SIZE, TILE_SIZE);
    }
}

/**
 * Draw grid for visualization (optional)
 */
function drawGrid() {
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.3)';
    ctx.lineWidth = 0.5;
    
    for (let i = 0; i <= DUNGEON_WIDTH; i++) {
        const x = i * TILE_SIZE;
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, canvas.height);
        ctx.stroke();
    }
    
    for (let i = 0; i <= DUNGEON_HEIGHT; i++) {
        const y = i * TILE_SIZE;
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(canvas.width, y);
        ctx.stroke();
    }
}
