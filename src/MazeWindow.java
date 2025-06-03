import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class MazeWindow extends JFrame {
    private final JTable table;
    private final MazeModel model;
    private final JLabel scoreLabel = new JLabel();
    private int ghostTick = 0;
    private volatile boolean running = true;
    private long lastUpgradeCheck = System.currentTimeMillis();

    // Dodaj pole do przechowywania czasu gry
    private long gameStartTime = System.currentTimeMillis();
    private long pausedTime = 0; // suma czasu pauzy (czekania na pierwszy ruch)
    private long pauseStart = 0; // czas rozpoczęcia pauzy

    public MazeWindow(MazeModel model) {
        this.model = model;
        setTitle("Labirynt Pac-Mana");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new MazeCellRenderer(model));
        table.setTableHeader(null);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setEnabled(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        scoreLabel.setText("Wynik: 0");
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(scoreLabel, BorderLayout.NORTH);

        // Obsługa klawiszy strzałek do sterowania Pac-Mana
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        model.setPacmanDirection(-1, 0);
                        break;
                    case KeyEvent.VK_DOWN:
                        model.setPacmanDirection(1, 0);
                        break;
                    case KeyEvent.VK_LEFT:
                        model.setPacmanDirection(0, -1);
                        break;
                    case KeyEvent.VK_RIGHT:
                        model.setPacmanDirection(0, 1);
                        break;
                }
            }
        });

        // Zamiast Timer - osobny wątek gry
        Thread gameThread = new Thread(() -> {
            while (running) {
                // Obsługa pauzy na pierwszy ruch
                if (model.isWaitingForFirstMove()) {
                    if (pauseStart == 0) pauseStart = System.currentTimeMillis();
                    try { Thread.sleep(30); } catch (InterruptedException ex) { break; }
                    continue;
                } else if (pauseStart != 0) {
                    pausedTime += System.currentTimeMillis() - pauseStart;
                    pauseStart = 0;
                }

                try {
                    long sleepTime = model.isSpeedBoostActive() ? 75 : 150;
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex) {
                    break;
                }
                SwingUtilities.invokeLater(() -> {
                    model.stepPacman();

                    long now = System.currentTimeMillis();
                    if (now - lastUpgradeCheck > 10_000) {
                        lastUpgradeCheck = now;
                        if (Math.random() < 0.2) {
                            model.tryAddExtraLife();
                        }
                        if (Math.random() < 0.2) {
                            model.tryAddSpeedUpgrade();
                        }
                        if (Math.random() < 1) {
                            model.tryAddFrightenedUpgrade();
                        }
                    }

                    // --- LICZNIK CZASU ---
                    long elapsed = (System.currentTimeMillis() - gameStartTime - pausedTime) / 1000;
                    String timeStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60);

                    scoreLabel.setText("Wynik: " + model.getScore() +
                            "   Życia: " + model.lives +
                            "   Czas: " + timeStr);

                    model.fireTableDataChanged();

                    // Sprawdź kolizję z duchem
                    if (model.handlePacmanGhostCollision()) {
                        // Pac-Man ginie
                        model.lives--;
                        if (model.lives > 0) {
                            model.resetPositions();
                            JOptionPane.showMessageDialog(MazeWindow.this, "Straciłeś życie! Pozostało żyć: " + model.lives);
                        } else {
                            running = false;
                            JOptionPane.showMessageDialog(MazeWindow.this, "Koniec gry! Przegrałeś.");
                            saveScore(model.getScore());
                            dispose();
                        }
                    }

                    // Sprawdź wygraną
                    if (!model.arePointsLeft()) {
                        running = false;
                        JOptionPane.showMessageDialog(MazeWindow.this, "Wygrałeś! Wszystkie punkty zebrane!");
                        saveScore(model.getScore());
                        dispose();
                    }
                });
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();

        // Wątek duchów - zawsze stała prędkość
        Thread ghostThread = new Thread(() -> {
            while (running) {
                if (model.isWaitingForFirstMove()) {
                    try { Thread.sleep(30); } catch (InterruptedException ex) { break; }
                    continue;
                }
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ex) {
                    break;
                }
                SwingUtilities.invokeLater(() -> {
                    ghostTick++;
                    if (model.isFrightenedActive()) {
                        if (ghostTick % 2 == 0) { // duchy poruszają się wolniej w trybie frightened
                            for (Ghost.Type t : Ghost.Type.values()) {
                                Ghost g = model.getGhostByType(t);
                                if (g != null && !model.isGhostRespawning(g)) g.moveFrightened(model);
                            }
                        }
                    } else {
                        // Standardowa prędkość duchów
                        if (ghostTick % 2 == 0) {
                            model.moveSingleGhost(Ghost.Type.BLINKY);
                        }
                        if (ghostTick % 6 == 0) {
                            model.moveSingleGhost(Ghost.Type.PINKY);
                        }
                        if (ghostTick % 3 == 0) {
                            model.moveSingleGhost(Ghost.Type.INKY);
                        }
                        model.moveSingleGhost(Ghost.Type.CLYDE);
                    }
                    model.fireTableDataChanged();
                });
            }
        });
        ghostThread.setDaemon(true);
        ghostThread.start();

        setFocusable(true);
        setSize(800, 800);
        setLocationRelativeTo(null);
        setVisible(true);
        resizeCells();

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resizeCells();
            }
        });
    }

    // Metoda do zmiany rozmiaru komórek w tabeli
    private void resizeCells() {
        int panelWidth = getContentPane().getWidth();
        int panelHeight = getContentPane().getHeight() - scoreLabel.getHeight();
        int cellWidth = panelWidth / model.getColumnCount();
        int cellHeight = panelHeight / model.getRowCount();
        int cellSize = Math.min(cellWidth, cellHeight);

        table.setRowHeight(cellSize);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(cellSize);
        }
    }

    private void saveScore(int score) {
        String name = JOptionPane.showInputDialog(this, "Podaj swój podpis do wyniku:");
        if (name == null || name.trim().isEmpty()) return;

        // Wczytaj istniejące wyniki do mapy
        java.util.Map<String, Integer> scores = new java.util.HashMap<>();
        java.io.File file = new java.io.File("scores.txt");
        if (file.exists()) {
            try (java.util.Scanner sc = new java.util.Scanner(file)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    int sep = line.lastIndexOf(" - ");
                    if (sep > 0) {
                        String n = line.substring(0, sep).trim();
                        int s = Integer.parseInt(line.substring(sep + 3).trim());
                        scores.put(n, s);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Zastąp wynik, jeśli gracz już istnieje lub dodaj nowy
        scores.put(name, Math.max(score, scores.getOrDefault(name, 0)));

        // Posortuj wyniki malejąco
        java.util.List<java.util.Map.Entry<String, Integer>> list = new java.util.ArrayList<>(scores.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Zapisz do pliku
        try (java.io.FileWriter fw = new java.io.FileWriter("scores.txt", false)) {
            for (java.util.Map.Entry<String, Integer> entry : list) {
                fw.write(entry.getKey() + " - " + entry.getValue() + System.lineSeparator());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu wyniku: " + ex.getMessage());
        }
    }
}
