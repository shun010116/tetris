package tetris.game;

import tetris.ui.SettingsManager;

public class GameEngine {
    private GameBoard gameBoard;
    private Piece currentPiece;
    private Piece nextPiece;
    private int score;
    private int level;
    private int linesCleared;
    private boolean isGameRunning;
    private boolean isPaused;

    public GameEngine() {
        this.gameBoard = new GameBoard();
        this.score = 0;
        this.level = 1;
        this.linesCleared = 0;
        this.isGameRunning = false;
        this.isPaused = false;
        generateNextPiece();
        spawnNewPiece();
    }

    public void startGame() {
        isGameRunning = true;
        isPaused = false;
    }

    public void pauseGame() {
        isPaused = !isPaused;
    }

    public void stopGame() {
        isGameRunning = false;
        isPaused = false;
    }

    public void handleKeyPress(javafx.scene.input.KeyCode keyCode) {
        if (!isGameRunning || isPaused || currentPiece == null) {
            return;
        }

        SettingsManager settings = SettingsManager.getInstance();
        String keyName = keyCode.getName().toUpperCase();

        if (keyName.equals(settings.getKeyLeft())) {
            movePieceLeft();
        } else if (keyName.equals(settings.getKeyRight())) {
            movePieceRight();
        } else if (keyName.equals(settings.getKeyDown())) {
            movePieceDown();
        } else if (keyName.equals(settings.getKeyRotate())) {
            rotatePiece();
        } else if (keyName.equals(settings.getKeyHardDrop()) || keyCode == javafx.scene.input.KeyCode.SPACE) {
            hardDrop();
        }
    }
    private void movePieceLeft() {
        if (currentPiece != null) {
            currentPiece.moveLeft();
            if (!gameBoard.isValidPosition(currentPiece)) {
                currentPiece.moveRight();
            }
        }
    }

    private void movePieceRight() {
        if (currentPiece != null) {
            currentPiece.moveRight();
            if (!gameBoard.isValidPosition(currentPiece)) {
                currentPiece.moveLeft();
            }
        }
    }

    public void movePieceDown() {
        if (currentPiece != null) {
            currentPiece.moveDown();
            if (!gameBoard.isValidPosition(currentPiece)) {
                currentPiece.moveUp();
                placePiece();
            }
        }
    }

    private void rotatePiece() {
        if (currentPiece != null) {
            currentPiece.rotate();
            if (!gameBoard.isValidPosition(currentPiece)) {
                currentPiece.rotateBack();
            }
        }
    }

    private void hardDrop() {
        if (currentPiece != null) {
            while (gameBoard.isValidPosition(currentPiece)) {
                currentPiece.moveDown();
            }
            currentPiece.moveUp();
            placePiece();
        }
    }

    private void placePiece() {
        if (currentPiece != null) {
            gameBoard.placePiece(currentPiece);
            int clearedLines = gameBoard.clearLines();
            updateScore(clearedLines);
            spawnNewPiece();

            if (!gameBoard.isValidPosition(currentPiece)) {
                stopGame();
            }
        }
    }

    private void spawnNewPiece() {
        currentPiece = nextPiece;
        generateNextPiece();
        if (currentPiece != null) {
            currentPiece.setPosition(gameBoard.getSpawnX(), gameBoard.getSpawnY());
        }
    }

    private void generateNextPiece() {
        nextPiece = PieceFactory.createRandomPiece();
    }

    private void updateScore(int clearedLines) {
        this.linesCleared += clearedLines;

        switch (clearedLines) {
            case 1:
                score += 100 * level;
                break;
            case 2:
                score += 300 * level;
                break;
            case 3:
                score += 500 * level;
                break;
            case 4:
                score += 800 * level;
                break;
        }

        // 난이도에 따라 레벨 상승 폭 변화
        SettingsManager settings = SettingsManager.getInstance();
        String difficulty = settings.getDifficulty();
        int linesPerLevel;

        switch (difficulty) {
            case "Easy":
                linesPerLevel = 12;
                break;
            case "Hard":
                linesPerLevel = 8;
                break;
            default:    // Normal
                linesPerLevel = 10;
                break;
        }

        level = (linesCleared / linesPerLevel) + 1;
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public Piece getCurrentPiece() {
        return currentPiece;
    }

    public Piece getNextPiece() {
        return nextPiece;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }
}