// Game configuration
const GAME_API_URL = '/api/game';
const TILE_SIZE = 20;
const DUNGEON_WIDTH = 40;
const DUNGEON_HEIGHT = 30;
const TARGET_FPS = 60;
const FRAME_TIME = 1000 / TARGET_FPS; // ~16.67ms per frame

// Game state
let gameState = null;
let enemies = [];
let canvas = null;
let ctx = null;
let lastFrameTime = 0;

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
    
    // Start the game loop (handles fetching state and rendering)
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
    
    // Keyboard movement
    document.addEventListener('keydown', handleKeyPress);
}

/**
 * Handle keyboard input for player movement
 */
function handleKeyPress(event) {
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
        movePlayer(direction);
    }
}

/**
 * Send a move request to the server
 */
function movePlayer(direction) {
    fetch(`${GAME_API_URL}/move?direction=${direction}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            gameState = data;
            updateUI();
        })
        .catch(error => console.error('Error:', error));
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
 * Main game loop - runs at 60 FPS with delta time management.
 * Fetches game state and enemies each frame.
 */
function gameLoop(currentTime) {
    // Calculate delta time
    if (lastFrameTime === 0) {
        lastFrameTime = currentTime;
    }
    
    const deltaTime = currentTime - lastFrameTime;
    lastFrameTime = currentTime;
    
    // Fetch latest game state each frame
    Promise.all([
        fetch(`${GAME_API_URL}/state`).then(r => r.json()),
        fetch(`${GAME_API_URL}/enemies`).then(r => r.json())
    ])
    .then(([state, enemyList]) => {
        gameState = state;
        enemies = enemyList;
        updateUI();
        render();
    })
    .catch(error => console.error('Error fetching game state:', error));
    
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
    ctx.fillStyle = '#333333';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw dungeon border
    ctx.strokeStyle = '#666666';
    ctx.lineWidth = 2;
    ctx.strokeRect(0, 0, canvas.width, canvas.height);
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
    ctx.strokeStyle = 'rgba(100, 100, 100, 0.2)';
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
