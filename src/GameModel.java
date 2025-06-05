import javax.swing.table.AbstractTableModel;
import java.util.*;

public class GameModel extends AbstractTableModel {
    enum CellType { WALL, PATH, GHOST_ROOM, POINT, PACMAN, GHOST }

    // Plansza gry
    CellType[][] grid;
    int rows, cols;

    // Pozycja Pac-Mana
    int pacmanRow, pacmanCol;

    // Wynik i liczba żyć
    int score = 0;
    public int lives = 3;

    // Tablica duchów
    private final Ghost[] ghosts = new Ghost[4];

    // Kierunek ruchu Pac-Mana (aktualny i oczekiwany)
    private int pacmanDirR = 0, pacmanDirC = 0;
    private int wantedDirR = 0, wantedDirC = 0;

    // Ulepszenie na planszy
    private Upgrade upgrade;

    // Czasy trwania efektów
    private long speedBoostEndTime = 0;
    private long frightenedEndTime = 0;
    private long invincibleEndTime = 0;
    private long timeFreezeEndTime = 0;
    private final Map<Ghost, Long> ghostRespawnTimes = new HashMap<>();

    private boolean waitingForFirstMove = true;

    // --- Konstruktor i generowanie planszy ---

    public GameModel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new CellType[rows][cols];
        generateMaze();
    }

    private void generateMaze() {
        for (int r = 0; r < rows; r++)
            Arrays.fill(grid[r], CellType.WALL);

        generatePaths(1, 1);

        for (int r = rows / 2 - 1; r <= rows / 2; r++)
            for (int c = cols / 2 - 1; c <= cols / 2 + 1; c++)
                grid[r][c] = CellType.GHOST_ROOM;

        for (int c = cols / 2 - 1; c <= cols / 2 + 1; c++)
            grid[rows / 2 - 2][c] = CellType.PATH;

        addExtraHoles(0.08);

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == CellType.PATH)
                    grid[r][c] = CellType.POINT;

        pacmanRow = 1;
        pacmanCol = 1;
        if (grid[pacmanRow][pacmanCol] == CellType.POINT)
            grid[pacmanRow][pacmanCol] = CellType.PATH;

        int gr = rows / 2 - 1, gc = cols / 2 - 1;
        ghosts[0] = new Ghost(Ghost.Type.BLINKY, gr, gc, 0, 1);
        ghosts[1] = new Ghost(Ghost.Type.PINKY, gr, gc + 1, 0, 1);
        ghosts[2] = new Ghost(Ghost.Type.INKY, gr + 1, gc, 0, 1);
        ghosts[3] = new Ghost(Ghost.Type.CLYDE, gr + 1, gc + 1, 0, 1);
    }

    private void generatePaths(int r, int c) {
        grid[r][c] = CellType.PATH;
        int[] dr = {-2, 2, 0, 0};
        int[] dc = {0, 0, -2, 2};
        Integer[] dirs = {0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(dirs));

        for (int i : dirs) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && grid[nr][nc] == CellType.WALL) {
                grid[r + dr[i] / 2][c + dc[i] / 2] = CellType.PATH;
                generatePaths(nr, nc);
            }
        }
    }

    private void addExtraHoles(double percentage) {
        Random rand = new Random();
        int holesToAdd = (int) (rows * cols * percentage);
        int count = 0;
        while (count < holesToAdd) {
            int r = rand.nextInt(rows - 2) + 1;
            int c = rand.nextInt(cols - 2) + 1;
            if (grid[r][c] == CellType.WALL && isBetweenWalls(r, c)) {
                grid[r][c] = CellType.PATH;
                count++;
            }
        }
    }

    private boolean isBetweenWalls(int r, int c) {
        boolean verticalWalls = false;
        boolean horizontalWalls = false;
        if (r > 0 && r < rows - 1)
            verticalWalls = (grid[r - 1][c] == CellType.WALL && grid[r + 1][c] == CellType.WALL);
        if (c > 0 && c < cols - 1)
            horizontalWalls = (grid[r][c - 1] == CellType.WALL && grid[r][c + 1] == CellType.WALL);
        return (verticalWalls ^ horizontalWalls);
    }

    // --- Ruch Pac-Mana i duchów ---

    public boolean isMovableTo(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        return grid[r][c] != CellType.WALL;
    }

    public void movePacmanTo(int r, int c) {
        if (grid[r][c] == CellType.POINT) {
            score++;
            grid[r][c] = CellType.PATH;
        }
        pacmanRow = r;
        pacmanCol = c;
    }

    public void moveSingleGhost(Ghost.Type type) {
        for (Ghost ghost : ghosts) {
            if (ghost.type == type) {
                ghost.move(this);
            }
        }
    }

    public boolean isGhostAt(int r, int c) {
        for (Ghost ghost : ghosts) {
            if (ghost.row == r && ghost.col == c) return true;
        }
        return false;
    }

    public Ghost getGhostAt(int r, int c) {
        for (Ghost ghost : ghosts) {
            if (ghost.row == r && ghost.col == c) return ghost;
        }
        return null;
    }

    public Ghost getGhostByType(Ghost.Type type) {
        for (Ghost ghost : ghosts) {
            if (ghost.getType() == type) {
                return ghost;
            }
        }
        return null;
    }

    // --- Gettery i obsługa planszy ---

    public int getPacmanRow() { return pacmanRow; }
    public int getPacmanCol() { return pacmanCol; }
    public int getScore() { return score; }

    @Override
    public int getRowCount() { return rows; }
    @Override
    public int getColumnCount() { return cols; }
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) { return grid[rowIndex][columnIndex]; }

    public void resetPositions() {
        pacmanRow = 1;
        pacmanCol = 1;
        int gr = rows / 2 - 1, gc = cols / 2 - 1;
        ghosts[0].row = gr;     ghosts[0].col = gc;
        ghosts[1].row = gr;     ghosts[1].col = gc + 1;
        ghosts[2].row = gr + 1; ghosts[2].col = gc;
        ghosts[3].row = gr + 1; ghosts[3].col = gc + 1;
        waitingForFirstMove = true;
        pacmanDirR = 0; pacmanDirC = 0;
        wantedDirR = 0; wantedDirC = 0;
    }

    public boolean isWaitingForFirstMove() { return waitingForFirstMove; }
    public void notifyFirstMove() { waitingForFirstMove = false; }

    public void setPacmanDirection(int dr, int dc) {
        wantedDirR = dr;
        wantedDirC = dc;
        if (waitingForFirstMove) waitingForFirstMove = false;
    }

    public void stepPacman() {
        int tryRow = pacmanRow + wantedDirR;
        int tryCol = pacmanCol + wantedDirC;
        if (isMovableTo(tryRow, tryCol)) {
            pacmanDirR = wantedDirR;
            pacmanDirC = wantedDirC;
        }
        int newRow = pacmanRow + pacmanDirR;
        int newCol = pacmanCol + pacmanDirC;
        if (isMovableTo(newRow, newCol)) {
            movePacmanTo(newRow, newCol);
            checkUpgrade();
        }
    }

    // --- BFS ---

    public int[] getNextStepBFS(int fromRow, int fromCol, int toRow, int toCol) {
        boolean[][] visited = new boolean[rows][cols];
        int[][] prevDir = new int[rows * cols][2];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{fromRow, fromCol});
        visited[fromRow][fromCol] = true;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];
            if (r == toRow && c == toCol) {
                while (prevDir[r * cols + c][0] != 0 || prevDir[r * cols + c][1] != 0) {
                    int pr = r - prevDir[r * cols + c][0];
                    int pc = c - prevDir[r * cols + c][1];
                    if (pr == fromRow && pc == fromCol) {
                        return new int[]{prevDir[r * cols + c][0], prevDir[r * cols + c][1]};
                    }
                    r = pr;
                    c = pc;
                }
            }
            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d], nc = c + dc[d];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                    CellType cell = grid[nr][nc];
                    if (cell == CellType.POINT || cell == CellType.PATH || cell == CellType.GHOST_ROOM || cell == CellType.PACMAN) {
                        visited[nr][nc] = true;
                        prevDir[nr * cols + nc][0] = dr[d];
                        prevDir[nr * cols + nc][1] = dc[d];
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return new int[]{0, 0};
    }

    // --- Punkty i kolizje ---

    public boolean arePointsLeft() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == CellType.POINT) return true;
        return false;
    }

    public boolean isPacmanCaught() {
        if (isFrightenedActive()) return false;
        for (Ghost ghost : ghosts) {
            if (ghost.row == pacmanRow && ghost.col == pacmanCol) {
                return true;
            }
        }
        return false;
    }

    // --- Ulepszenia ---

    private boolean tryAddUpgrade(Upgrade.Type type) {
        if (upgrade != null) return false;
        java.util.List<Ghost> available = new ArrayList<>();
        for (Ghost g : ghosts) {
            // Ulepszenie nie pojawia się pod Pac-Manem
            if ((g.row != pacmanRow || g.col != pacmanCol) && grid[g.row][g.col] == CellType.POINT) {
                available.add(g);
            }
        }
        if (available.isEmpty()) return false;
        Ghost chosen = available.get(new Random().nextInt(available.size()));
        upgrade = new Upgrade(type, chosen.row, chosen.col);
        return true;
    }

    public boolean tryAddExtraLife() {
        return tryAddUpgrade(Upgrade.Type.EXTRA_LIFE);
    }
    public boolean tryAddSpeedUpgrade() {
        return tryAddUpgrade(Upgrade.Type.SPEED);
    }
    public boolean tryAddFrightenedUpgrade() {
        return tryAddUpgrade(Upgrade.Type.FRIGHTENED);
    }
    public boolean tryAddInvincibleUpgrade() {
        return tryAddUpgrade(Upgrade.Type.INVINCIBLE);
    }
    public boolean tryAddTimeFreezeUpgrade() {
        return tryAddUpgrade(Upgrade.Type.TIMEFREEZE);
    }

    public void checkUpgrade() {
        if (upgrade != null && pacmanRow == upgrade.row && pacmanCol == upgrade.col) {
            if (upgrade.type == Upgrade.Type.EXTRA_LIFE) {
                lives++;
            } else if (upgrade.type == Upgrade.Type.SPEED) {
                speedBoostEndTime = System.currentTimeMillis() + 4000;
            } else if (upgrade.type == Upgrade.Type.FRIGHTENED) {
                frightenedEndTime = System.currentTimeMillis() + 7000;
            } else if (upgrade.type == Upgrade.Type.INVINCIBLE) {
                invincibleEndTime = System.currentTimeMillis() + 7000;
            } else if (upgrade.type == Upgrade.Type.TIMEFREEZE) {
                timeFreezeEndTime = System.currentTimeMillis() + 5000;
            }
            upgrade = null;
        }
    }

    public boolean isSpeedBoostActive() {
        return System.currentTimeMillis() < speedBoostEndTime;
    }
    public boolean isFrightenedActive() {
        return System.currentTimeMillis() < frightenedEndTime;
    }
    public boolean isInvincibleActive() {
        return System.currentTimeMillis() < invincibleEndTime;
    }
    public boolean isTimeFreezeActive() {
        return System.currentTimeMillis() < timeFreezeEndTime;
    }
    public Upgrade getUpgrade() {
        return upgrade;
    }

    // --- Duchy: respawn i kolizje ---

    public boolean isGhostRespawning(Ghost ghost) {
        Long until = ghostRespawnTimes.get(ghost);
        return until != null && System.currentTimeMillis() < until;
    }

    public void respawnGhost(Ghost ghost) {
        int gr = rows / 2 - 1, gc = cols / 2 - 1;
        ghost.row = gr;
        ghost.col = gc;
        ghostRespawnTimes.put(ghost, System.currentTimeMillis() + 5000);
    }

    public int getPacmanDirR() { return pacmanDirR; }
    public int getPacmanDirC() { return pacmanDirC; }

    public boolean handlePacmanGhostCollision() {
        for (Ghost ghost : ghosts) {
            if (ghost.row == pacmanRow && ghost.col == pacmanCol && !isGhostRespawning(ghost)) {
                if (isFrightenedActive()) {
                    score += 10;
                    respawnGhost(ghost);
                    return false;
                } else if (isInvincibleActive()) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}


