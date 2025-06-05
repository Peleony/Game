import java.awt.Color;

public class Ghost {
    public enum Type { BLINKY, PINKY, INKY, CLYDE }

    public final String name;
    public Type type;
    public final Color color;
    public int row, col;
    public int dirR, dirC;

    public Ghost(Type type, int row, int col, int dirR, int dirC) {
        this.type = type;
        String ghostName;
        Color ghostColor;
        switch (type) {
            case BLINKY:
                ghostName = "Blinky";
                ghostColor = Color.RED;
                break;
            case PINKY:
                ghostName = "Pinky";
                ghostColor = Color.PINK;
                break;
            case INKY:
                ghostName = "Inky";
                ghostColor = Color.CYAN;
                break;
            case CLYDE:
                ghostName = "Clyde";
                ghostColor = Color.ORANGE;
                break;
            default:
                ghostName = "Unknown";
                ghostColor = Color.WHITE;
        }
        this.name = ghostName;
        this.color = ghostColor;
        this.row = row;
        this.col = col;
        this.dirR = dirR;
        this.dirC = dirC;
    }

    public void move(GameModel model) {
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        java.util.Random rand = new java.util.Random();

        if (type == Type.BLINKY) {
            int[] step = model.getNextStepBFS(row, col, model.getPacmanRow(), model.getPacmanCol());
            dirR = step[0];
            dirC = step[1];
        } else if (type == Type.PINKY) {
            int dist = Math.abs(row - model.getPacmanRow()) + Math.abs(col - model.getPacmanCol());
            int[] step;
            if (dist <= 4) {
                step = model.getNextStepBFS(row, col, model.getPacmanRow(), model.getPacmanCol());
            } else {
                int targetRow = model.getPacmanRow() + 4 * model.getPacmanDirR();
                int targetCol = model.getPacmanCol() + 4 * model.getPacmanDirC();
                targetRow = Math.max(0, Math.min(model.rows - 1, targetRow));
                targetCol = Math.max(0, Math.min(model.cols - 1, targetCol));
                step = model.getNextStepBFS(row, col, targetRow, targetCol);
            }
            dirR = step[0];
            dirC = step[1];
        } else if (type == Type.INKY) {
            int targetRow = model.getPacmanRow() + 4 * model.getPacmanDirR();
            int targetCol = model.getPacmanCol() + 4 * model.getPacmanDirC();
            targetRow = Math.max(0, Math.min(model.rows - 1, targetRow));
            targetCol = Math.max(0, Math.min(model.cols - 1, targetCol));
            int[] step = model.getNextStepBFS(row, col, targetRow, targetCol);
            dirR = step[0];
            dirC = step[1];
        } else {
            java.util.List<int[]> possibleDirs = new java.util.ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int nr = row + dr[d];
                int nc = col + dc[d];
                if (nr >= 0 && nr < model.rows && nc >= 0 && nc < model.cols) {
                    GameModel.CellType cell = model.grid[nr][nc];
                    if (cell == GameModel.CellType.POINT || cell == GameModel.CellType.PATH ||
                        cell == GameModel.CellType.GHOST_ROOM || cell == GameModel.CellType.PACMAN) {
                        possibleDirs.add(new int[]{dr[d], dc[d]});
                    }
                }
            }
            int nextR = row + dirR;
            int nextC = col + dirC;
            boolean canContinue = false;
            if (nextR >= 0 && nextR < model.rows && nextC >= 0 && nextC < model.cols) {
                GameModel.CellType cell = model.grid[nextR][nextC];
                if (cell == GameModel.CellType.POINT || cell == GameModel.CellType.PATH ||
                    cell == GameModel.CellType.GHOST_ROOM || cell == GameModel.CellType.PACMAN) {
                    canContinue = true;
                }
            }
            if (!canContinue || rand.nextDouble() < 0.2) {
                if (!possibleDirs.isEmpty()) {
                    int[] dir = possibleDirs.get(rand.nextInt(possibleDirs.size()));
                    dirR = dir[0];
                    dirC = dir[1];
                }
            }
        }
        row += dirR;
        col += dirC;
    }

    public void moveFrightened(GameModel model) {
        moveRandom(model);
    }

    private void moveRandom(GameModel model) {
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        java.util.Random rand = new java.util.Random();

        int randomDirection = rand.nextInt(4);
        int newRow = row + dr[randomDirection];
        int newCol = col + dc[randomDirection];

        if (newRow >= 0 && newRow < model.rows && newCol >= 0 && newCol < model.cols) {
            GameModel.CellType cell = model.grid[newRow][newCol];
            if (cell == GameModel.CellType.POINT || cell == GameModel.CellType.PATH ||
                cell == GameModel.CellType.GHOST_ROOM || cell == GameModel.CellType.PACMAN) {
                row = newRow;
                col = newCol;
            }
        }
    }

    public Type getType() {
        return type;
    }
}