package tetris.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.animation.AnimationTimer;
import tetris.ui.SceneManager;
import tetris.ui.SettingsManager;
import tetris.game.GameEngine;
import tetris.game.GameBoard;
import tetris.game.Piece;

import java.net.URL;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.ResourceBundle;

/**
 * Controls the main game screen's UI and game loop.
 * This class is responsible for rendering the game board, pieces, and updating UI elements like score.
 */
public class GameScreenController implements Initializable {

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Label scoreLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label linesLabel;

    @FXML
    private Canvas nextPieceCanvas;

    private SceneManager sceneManager;
    private SettingsManager settingsManager;
    private GameEngine gameEngine;
    private AnimationTimer gameLoop;
    private long lastUpdateTime = 0;
    private long fallSpeed = 1_000_000_000; // 1 second in nanoseconds

    /** The size of each block in pixels. */
    private static final int BLOCK_SIZE = 25;
    /** The color palette for different Tetris pieces, designed to be colorblind-safe. */
    private static final Color[] PIECE_COLORS = {
        Color.BLACK,                    
        Color.web("#56B4E9"),          // 1 - I piece (Sky Blue)
        Color.web("#F0E442"),          // 2 - O piece (Yellow)
        Color.web("#CC79A7"),          // 3 - T piece (Pink/Purple)
        Color.web("#009E73"),          // 4 - S piece (Green)
        Color.web("#D55E00"),          // 5 - Z piece (Reddish-brown)
        Color.web("#0072B2"),          // 6 - J piece (Blue)
        Color.web("#E69F00")           // 7 - L piece (Orange)
    };

    /** Symbols used for accessibility mode to represent different pieces. */
    private static final String[] PIECE_SYMBOLS = {
        " ", // 0
        "O", // 1 - I
        "●", // 2 - O
        "★", // 3 - T
        "▲", // 4 - S
        "■", // 5 - Z
        "◆", // 6 - J
        "◇"  // 7 - L
    };

    /**
     * Initializes the controller class. This method is automatically called
     * after the FXML file has been loaded.
     * @param location The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param resources The resources used to localize the root object, or null if the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 설정 매니저 초기화
        settingsManager = SettingsManager.getInstance();
        
        // 게임 엔진 초기화
        gameEngine = new GameEngine();
        setupGameCanvas();
        setupNextPieceCanvas();
        
        // Scene이 설정된 후 키 핸들러 등록
        if (gameCanvas != null) {
            gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupKeyHandler();
                }
            });
        }
        
        startGameLoop();
        gameEngine.startGame();
    }

    /**
     * Sets the scene manager for this controller.
     * @param sceneManager The scene manager to use.
     */
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    /**
     * Sets up the main game canvas dimensions.
     */
    private void setupGameCanvas() {
        if (gameCanvas != null) {
            gameCanvas.setWidth(GameBoard.BOARD_WIDTH * BLOCK_SIZE);
            gameCanvas.setHeight(GameBoard.BOARD_HEIGHT * BLOCK_SIZE);
            // 포커스 비활성화 - Scene 레벨에서 키 입력 처리
            gameCanvas.setFocusTraversable(false);
        }
    }
    
    /**
     * Sets up the key event handler for the scene.
     */
    public void setupKeyHandler() {
        // Scene에 키 이벤트 핸들러 등록
        if (gameCanvas != null && gameCanvas.getScene() != null) {
            gameCanvas.getScene().setOnKeyPressed(event -> {
                if (gameEngine != null && gameEngine.isGameRunning() && !gameEngine.isPaused()) {
                    // 게임 진행 중에만 키 입력을 게임 엔진으로 전달
                    gameEngine.handleKeyPress(event.getCode());
                    // 이벤트를 consume하여 버튼으로 전파되지 않도록 차단
                    event.consume();
                }
            });
        }
    }

    /**
     * Sets up the canvas for displaying the next piece.
     */
    private void setupNextPieceCanvas() {
        if (nextPieceCanvas != null) {
            nextPieceCanvas.setWidth(6 * BLOCK_SIZE);
            nextPieceCanvas.setHeight(5 * BLOCK_SIZE);
            // Canvas 테두리 그리기
            drawNextPieceCanvasBorder();
        }
    }
    
    /**
     * Draws a border around the next piece canvas.
     */
    private void drawNextPieceCanvasBorder() {
        if (nextPieceCanvas != null) {
            GraphicsContext gc = nextPieceCanvas.getGraphicsContext2D();
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, nextPieceCanvas.getWidth() - 2, nextPieceCanvas.getHeight() - 2);
        }
    }

    /**
     * Starts the main game loop using an AnimationTimer.
     */
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastUpdateTime == 0) {
                    lastUpdateTime = now;
                }

                // 게임 오버 체크
                if (!gameEngine.isGameRunning()) {
                    gameLoop.stop();
                    showGameOver();
                    return;
                }

                if (now - lastUpdateTime >= fallSpeed) {
                    if (gameEngine.isGameRunning() && !gameEngine.isPaused()) {
                        gameEngine.movePieceDown();
                    }
                    lastUpdateTime = now;
                }

                renderGame();
                renderNextPiece();
                updateUI();
                updateFallSpeed();
            }
        };
        gameLoop.start();
    }

    /**
     * Updates the fall speed of the pieces based on the current level.
     */
    private void updateFallSpeed() {
        fallSpeed = (long) (1_000_000_000 / (1 + 0.1 * gameEngine.getLevel()));
    }

    /**
     * Renders the entire game state, including the board and the current piece.
     */
    private void renderGame() {
        if (gameCanvas == null || gameEngine == null) return;

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();

        // 배경을 검은색으로 설정
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // 게임 보드 렌더링
        GameBoard board = gameEngine.getGameBoard();
        for (int row = 0; row < GameBoard.BOARD_HEIGHT; row++) {
            for (int col = 0; col < GameBoard.BOARD_WIDTH; col++) {
                int cellValue = board.getCell(row, col);
                if (cellValue > 0) {
                    renderBlock(gc, col * BLOCK_SIZE, row * BLOCK_SIZE, PIECE_COLORS[cellValue], cellValue);
                }
            }
        }

        // 현재 피스 렌더링
        Piece currentPiece = gameEngine.getCurrentPiece();
        if (currentPiece != null) {
            renderPiece(gc, currentPiece);
        }

        // 테두리 렌더링
        renderBorder(gc);
    }

    /**
     * Renders the next piece in its dedicated canvas.
     */
    private void renderNextPiece() {
        if (nextPieceCanvas == null || gameEngine == null) return;

        GraphicsContext gc = nextPieceCanvas.getGraphicsContext2D();

        // 배경을 검은색으로 설정
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, nextPieceCanvas.getWidth(), nextPieceCanvas.getHeight());

        Piece nextPiece = gameEngine.getNextPiece();
        if (nextPiece != null) {
            int[][] shape = nextPiece.getShape();
            Color color = PIECE_COLORS[nextPiece.getType()];

            for (int row = 0; row < shape.length; row++) {
                for (int col = 0; col < shape[row].length; col++) {
                    if (shape[row][col] != 0) {
                        renderBlock(gc, (col + 1) * BLOCK_SIZE, (row + 1) * BLOCK_SIZE, color, nextPiece.getType());
                    }
                }
            }
        }
        
        // 테두리 다시 그리기
        drawNextPieceCanvasBorder();
    }

    /**
     * Renders a single Tetris piece on the canvas.
     * @param gc The graphics context to draw on.
     * @param piece The piece to render.
     */
    private void renderPiece(GraphicsContext gc, Piece piece) {
        int[][] shape = piece.getShape();
        Color color = PIECE_COLORS[piece.getType()];
        int pieceX = piece.getX();
        int pieceY = piece.getY();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int x = (pieceX + col) * BLOCK_SIZE;
                    int y = (pieceY + row) * BLOCK_SIZE;
                    renderBlock(gc, x, y, color, piece.getType());
                }
            }
        }
    }

    /**
     * Renders a single block, either with color or with an accessibility symbol.
     * @param gc The graphics context to draw on.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param color The color of the block.
     * @param pieceType The type of the piece, used for accessibility symbols.
     */
    private void renderBlock(GraphicsContext gc, int x, int y, Color color, int pieceType) {
        // 접근성 모드가 켜져 있으면 색 대신 심볼로 채운다
        if (settingsManager != null && settingsManager.isAccessibilityModeEnabled()) {
            // 배경을 검게 유지
            gc.setFill(Color.BLACK);
            gc.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);

            String symbol = (pieceType >= 0 && pieceType < PIECE_SYMBOLS.length) ? PIECE_SYMBOLS[pieceType] : "?";

            int fontSize = Math.max(8, BLOCK_SIZE - 2);
            Font font = Font.font("Monospaced", fontSize);
            gc.setFont(font);
            gc.setFill(Color.WHITE);

            Text text = new Text(symbol);
            text.setFont(font);
            double textWidth = text.getLayoutBounds().getWidth();
            double textHeight = text.getLayoutBounds().getHeight();

            double tx = x + (BLOCK_SIZE - textWidth) / 2.0;
            double ty = y + (BLOCK_SIZE + textHeight) / 2.0 - 4;

            gc.fillText(symbol, tx, ty);
            return;
        }

        // 기본 렌더링: 색으로 채우고 테두리 그림
        gc.setFill(color);
        gc.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);

        // 블록 테두리
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
    }
    
    /**
     * Renders the border of the main game canvas.
     * @param gc The graphics context to draw on.
     */
    private void renderBorder(GraphicsContext gc) {
        // 접근성 모드에서도 게임 보드 외곽 테두리 표시
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
    }

    /**
     * Updates all UI labels (score, level, lines).
     */
    private void updateUI() {
        if (gameEngine != null) {
            updateScore(gameEngine.getScore());
            updateLevel(gameEngine.getLevel());
            updateLines(gameEngine.getLinesCleared());
        }
    }

    /**
     * Handles the pause button click event.
     */
    @FXML
    private void onPause() {
        if (gameEngine != null) {
            gameEngine.pauseGame();
        }
    }

    /**
     * Handles the "Back to Menu" button click event.
     */
    @FXML
    private void onBackToMenu() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        if (gameEngine != null) {
            gameEngine.stopGame();
        }
        if (sceneManager != null) {
            sceneManager.showMainMenu();
        }
    }

    /**
     * Updates the score label.
     * @param score The new score.
     */
    public void updateScore(int score) {
        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + score);
        }
    }

    /**
     * Updates the level label.
     * @param level The new level.
     */
    public void updateLevel(int level) {
        if (levelLabel != null) {
            levelLabel.setText("Level: " + level);
        }
    }

    /**
     * Updates the lines cleared label.
     * @param lines The new number of lines cleared.
     */
    public void updateLines(int lines) {
        if (linesLabel != null) {
            linesLabel.setText("Lines: " + lines);
        }
    }

    /**
     * Shows the game over screen.
     */
    public void showGameOver() {
        if (sceneManager != null) {
            int finalScore = gameEngine.getScore();
            sceneManager.showGameOverScreen(finalScore);
        }
    }
}