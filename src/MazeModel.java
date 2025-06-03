import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * Model logiki gry Pac-Man: plansza, Pac-Man, duchy, punkty, ulepszenia, liczba żyć.
 */
public class MazeModel extends AbstractTableModel {
    // Typy pól na planszy
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
    private int pacmanDirR = 0; // -1, 1, 0, 0
    private int pacmanDirC = 0; // 0, 0, -1, 1
    private int wantedDirR = 0;
    private int wantedDirC = 0;

    // Kierunki duchów (nieużywane, logika w Ghost)
    private final int[] ghostDirR = new int[4];
    private final int[] ghostDirC = new int[4];

    // Ulepszenie na planszy (np. dodatkowe życie)
    private Upgrade upgrade;

    // Czas trwania przyśpieszenia w milisekundach
    private long speedBoostEndTime = 0;
    private long frightenedEndTime = 0;
    private final Map<Ghost, Long> ghostRespawnTimes = new HashMap<>();

    // Flaga: czy gra czeka na pierwszy ruch po starcie lub respawnie
    private boolean waitingForFirstMove = true;

    /**
     * Konstruktor modelu gry.
     */
    public MazeModel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new CellType[rows][cols];
        generateMaze();
    }

    /**
     * Generuje labirynt, rozmieszcza punkty, Pac-Mana, duchy i ulepszenie.
     */
    private void generateMaze() {
        // Wypełnij planszę ścianami
        for (int r = 0; r < rows; r++)
            Arrays.fill(grid[r], CellType.WALL);

        // Wygeneruj ścieżki
        generatePaths(1, 1);

        // Pokój duchów
        for (int r = rows / 2 - 1; r <= rows / 2; r++) {
            for (int c = cols / 2 - 1; c <= cols / 2 + 1; c++) {
                grid[r][c] = CellType.GHOST_ROOM;
            }
        }

        // Przejście do pokoju duchów
        for (int c = cols / 2 - 1; c <= cols / 2 + 1; c++) {
            grid[rows / 2 - 2][c] = CellType.PATH;
        }

        // Dodaj dodatkowe przejścia
        addExtraHoles(0.08);

        // Zamień PATH na POINT (punkty do zbierania)
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == CellType.PATH)
                    grid[r][c] = CellType.POINT;

        // Ustaw startową pozycję Pac-Mana
        pacmanRow = 1;
        pacmanCol = 1;
        if (grid[pacmanRow][pacmanCol] == CellType.POINT) {
            grid[pacmanRow][pacmanCol] = CellType.PATH;
        }

        // Dodaj duchy
        int gr = rows / 2 - 1;
        int gc = cols / 2 - 1;
        ghosts[0] = new Ghost(Ghost.Type.BLINKY, gr, gc, 0, 1);
        ghosts[1] = new Ghost(Ghost.Type.PINKY, gr, gc + 1, 0, 1);
        ghosts[2] = new Ghost(Ghost.Type.INKY, gr + 1, gc, 0, 1);
        ghosts[3] = new Ghost(Ghost.Type.CLYDE, gr + 1, gc + 1, 0, 1);

        // Dodaj upgrade (np. dodatkowe życie) w losowym miejscu na ścieżce
        Random rand = new Random();
        while (true) {
            int ur = rand.nextInt(rows);
            int uc = rand.nextInt(cols);
            if (grid[ur][uc] == CellType.POINT && (ur != pacmanRow || uc != pacmanCol)) {
                upgrade = new Upgrade(Upgrade.Type.EXTRA_LIFE, ur, uc);
                break;
            }
        }
    }

    /**
     * Rekurencyjnie generuje ścieżki w labiryncie.
     */
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

    /**
     * Dodaje dodatkowe przejścia w ścianach.
     */
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

    /**
     * Sprawdza, czy pole jest pomiędzy ścianami (do generowania przejść).
     */
    private boolean isBetweenWalls(int r, int c) {
        boolean verticalWalls = false;
        boolean horizontalWalls = false;

        if (r > 0 && r < rows - 1) {
            verticalWalls = (grid[r - 1][c] == CellType.WALL && grid[r + 1][c] == CellType.WALL);
        }

        if (c > 0 && c < cols - 1) {
            horizontalWalls = (grid[r][c - 1] == CellType.WALL && grid[r][c + 1] == CellType.WALL);
        }

        // True jeśli tylko jeden kierunek ma ściany (XOR)
        return (verticalWalls ^ horizontalWalls);
    }

    /**
     * Sprawdza, czy można wejść na dane pole (czy nie jest ścianą).
     */
    public boolean isMovableTo(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        return grid[r][c] != CellType.WALL;
    }

    /**
     * Przesuwa Pac-Mana na nowe pole, zbiera punkt jeśli jest.
     */
    public void movePacmanTo(int r, int c) {
        if (grid[r][c] == CellType.POINT) {
            score++;
            grid[r][c] = CellType.PATH;
        }
        pacmanRow = r;
        pacmanCol = c;
    }

    /**
     * Przesuwa wszystkie duchy (lub tylko Clyde'a jeśli onlyClyde=true).
     */
    public void moveGhosts(boolean onlyClyde) {
        for (Ghost ghost : ghosts) {
            if (onlyClyde && ghost.type != Ghost.Type.CLYDE) continue;
            if (!onlyClyde && ghost.type == Ghost.Type.CLYDE) continue;
            ghost.move(this);
        }
    }

    /**
     * Przesuwa tylko wybranego ducha.
     */
    public void moveSingleGhost(Ghost.Type type) {
        for (Ghost ghost : ghosts) {
            if (ghost.type == type) {
                ghost.move(this);
            }
        }
    }

    /**
     * Sprawdza, czy na danym polu jest duch.
     */
    public boolean isGhostAt(int r, int c) {
        for (Ghost ghost : ghosts) {
            if (ghost.row == r && ghost.col == c) return true;
        }
        return false;
    }

    /**
     * Zwraca ducha na danym polu (lub null).
     */
    public Ghost getGhostAt(int r, int c) {
        for (Ghost ghost : ghosts) {
            if (ghost.row == r && ghost.col == c) return ghost;
        }
        return null;
    }

    /**
     * Zwraca ducha o danym typie (lub null).
     */
    public Ghost getGhostByType(Ghost.Type type) {
        for (Ghost ghost : ghosts) {
            if (ghost.getType() == type) {
                return ghost;
            }
        }
        return null;
    }

    /**
     * Zwraca wiersz Pac-Mana.
     */
    public int getPacmanRow() {
        return pacmanRow;
    }

    /**
     * Zwraca kolumnę Pac-Mana.
     */
    public int getPacmanCol() {
        return pacmanCol;
    }

    /**
     * Zwraca aktualny wynik.
     */
    public int getScore() {
        return score;
    }

    /**
     * Zwraca liczbę wierszy planszy.
     */
    @Override
    public int getRowCount() {
        return rows;
    }

    /**
     * Zwraca liczbę kolumn planszy.
     */
    @Override
    public int getColumnCount() {
        return cols;
    }

    /**
     * Zwraca typ pola na danej pozycji.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return grid[rowIndex][columnIndex];
    }

    /**
     * Resetuje pozycje Pac-Mana i duchów po utracie życia.
     */
    public void resetPositions() {
        pacmanRow = 1;
        pacmanCol = 1;
        // Resetuj duchy na startowe pozycje
        int gr = rows / 2 - 1;
        int gc = cols / 2 - 1;
        ghosts[0].row = gr;     ghosts[0].col = gc;
        ghosts[1].row = gr;     ghosts[1].col = gc + 1;
        ghosts[2].row = gr + 1; ghosts[2].col = gc;
        ghosts[3].row = gr + 1; ghosts[3].col = gc + 1;
        waitingForFirstMove = true; // zatrzymaj czas i duchy do pierwszego ruchu

        // Resetuj kierunki Pac-Mana
        pacmanDirR = 0;
        pacmanDirC = 0;
        wantedDirR = 0;
        wantedDirC = 0;
    }

    /**
     * Czy gra czeka na pierwszy ruch?
     */
    public boolean isWaitingForFirstMove() {
        return waitingForFirstMove;
    }

    /**
     * Ustawia, że pierwszy ruch został wykonany.
     */
    public void notifyFirstMove() {
        waitingForFirstMove = false;
    }

    /**
     * Ustawia oczekiwany kierunek ruchu Pac-Mana.
     */
    public void setPacmanDirection(int dr, int dc) {
        wantedDirR = dr;
        wantedDirC = dc;
        if (waitingForFirstMove) {
            waitingForFirstMove = false; // odblokuj grę po pierwszym ruchu
        }
    }

    /**
     * Wykonuje krok Pac-Mana (zmiana kierunku, ruch, zbieranie punktów i upgrade).
     */
    public void stepPacman() {
        // Najpierw próbuj skręcić w oczekiwany kierunek
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

    /**
     * Ustawia początkowy kierunek duchów (nieużywane, logika w Ghost).
     */
    public void setGhostsInitialDirection() {
        for (int i = 0; i < 4; i++) {
            ghostDirR[i] = 0;
            ghostDirC[i] = 1;
        }
    }

    /**
     * BFS: Zwraca pierwszy krok na najkrótszej ścieżce z (fromRow,fromCol) do (toRow,toCol).
     */
    public int[] getNextStepBFS(int fromRow, int fromCol, int toRow, int toCol) {
        boolean[][] visited = new boolean[rows][cols];
        int[][] prevDir = new int[rows * cols][2]; // [r*cols+c][0]=dr, [][1]=dc
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{fromRow, fromCol});
        visited[fromRow][fromCol] = true;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];
            if (r == toRow && c == toCol) {
                // Cofnij się do pierwszego kroku
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
        // Jeśli nie ma ścieżki, nie ruszaj się
        return new int[]{0, 0};
    }

    /**
     * Sprawdza, czy na planszy są jeszcze punkty do zebrania.
     */
    public boolean arePointsLeft() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == CellType.POINT) return true;
            }
        }
        return false;
    }

    /**
     * Sprawdza, czy Pac-Man został złapany przez ducha.
     */
    public boolean isPacmanCaught() {
        // Sprawdzaj tylko, gdy tryb frightened NIE jest aktywny
        if (isFrightenedActive()) return false;
        for (Ghost ghost : ghosts) {
            if (ghost.row == pacmanRow && ghost.col == pacmanCol) {
                return true;
            }
        }
        return false;
    }

    /**
     * Próbuje dodać Extra Life w losowym miejscu na planszy.
     * Zwraca true jeśli dodano, false jeśli nie.
     */
    public boolean tryAddExtraLife() {
        if (upgrade != null) return false; // już jest
        Random rand = new Random();
        for (int i = 0; i < 100; i++) { // max 100 prób
            int ur = rand.nextInt(rows);
            int uc = rand.nextInt(cols);
            if (grid[ur][uc] == CellType.POINT && (ur != pacmanRow || uc != pacmanCol)) {
                upgrade = new Upgrade(Upgrade.Type.EXTRA_LIFE, ur, uc);
                return true;
            }
        }
        return false;
    }

    /**
     * Próbuje dodać Speed Upgrade w losowym miejscu na planszy.
     * Zwraca true jeśli dodano, false jeśli nie.
     */
    public boolean tryAddSpeedUpgrade() {
        if (upgrade != null) return false; // już jest jakiś upgrade
        Random rand = new Random();
        for (int i = 0; i < 100; i++) { // max 100 prób
            int ur = rand.nextInt(rows);
            int uc = rand.nextInt(cols);
            if (grid[ur][uc] == CellType.POINT && (ur != pacmanRow || uc != pacmanCol)) {
                upgrade = new Upgrade(Upgrade.Type.SPEED, ur, uc);
                return true;
            }
        }
        return false;
    }

    /**
     * Próbuje dodać Frightened Upgrade w losowym miejscu na planszy.
     * Zwraca true jeśli dodano, false jeśli nie.
     */
    public boolean tryAddFrightenedUpgrade() {
        if (upgrade != null) return false;
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            int ur = rand.nextInt(rows);
            int uc = rand.nextInt(cols);
            if (grid[ur][uc] == CellType.POINT && (ur != pacmanRow || uc != pacmanCol)) {
                upgrade = new Upgrade(Upgrade.Type.FRIGHTENED, ur, uc);
                return true;
            }
        }
        return false;
    }

    /**
     * Sprawdza, czy Pac-Man zebrał ulepszenie i obsługuje efekt (np. dodatkowe życie, przyśpieszenie).
     */
    public void checkUpgrade() {
        if (upgrade != null && pacmanRow == upgrade.row && pacmanCol == upgrade.col) {
            if (upgrade.type == Upgrade.Type.EXTRA_LIFE) {
                lives++;
            } else if (upgrade.type == Upgrade.Type.SPEED) {
                speedBoostEndTime = System.currentTimeMillis() + 4000;
            } else if (upgrade.type == Upgrade.Type.FRIGHTENED) {
                frightenedEndTime = System.currentTimeMillis() + 7000;
            }
            upgrade = null;
        }
    }

    /**
     * Zwraca true jeśli Pac-Man ma aktywne przyśpieszenie.
     */
    public boolean isSpeedBoostActive() {
        return System.currentTimeMillis() < speedBoostEndTime;
    }

    /**
     * Zwraca true jeśli Pac-Man ma aktywne przerażenie.
     */
    public boolean isFrightenedActive() {
        return System.currentTimeMillis() < frightenedEndTime;
    }

    public boolean isGhostRespawning(Ghost ghost) {
        Long until = ghostRespawnTimes.get(ghost);
        return until != null && System.currentTimeMillis() < until;
    }

    public void respawnGhost(Ghost ghost) {
        int gr = rows / 2 - 1;
        int gc = cols / 2 - 1;
        ghost.row = gr;
        ghost.col = gc;
        ghostRespawnTimes.put(ghost, System.currentTimeMillis() + 5000); // 5 sekund pauzy
    }

    /**
     * Zwraca aktualny upgrade na planszy (lub null).
     */
    public Upgrade getUpgrade() {
        return upgrade;
    }

    /**
     * Zwraca aktualny kierunek ruchu Pac-Mana (wiersz).
     */
    public int getPacmanDirR() { return pacmanDirR; }

    /**
     * Zwraca aktualny kierunek ruchu Pac-Mana (kolumna).
     */
    public int getPacmanDirC() { return pacmanDirC; }

    public boolean handlePacmanGhostCollision() {
        for (Ghost ghost : ghosts) {
            if (ghost.row == pacmanRow && ghost.col == pacmanCol && !isGhostRespawning(ghost)) {
                if (isFrightenedActive()) {
                    score += 10;
                    respawnGhost(ghost);
                    return false; // Pac-Man nie ginie
                } else {
                    return true; // Pac-Man ginie
                }
            }
        }
        return false;
    }
}
