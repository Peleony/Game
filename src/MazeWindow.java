import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class GameWindow extends JFrame {
    private final JTable table;
    private final GameModel model; // <-- zmiana z MazeModel na GameModel
    private final JLabel scoreLabel = new JLabel();
    private int ghostTick = 0;
    private volatile boolean running = true;
    private long lastUpgradeCheck = System.currentTimeMillis();

    // Czas gry i pauzy
    private long gameStartTime = System.currentTimeMillis();
    private long pausedTime = 0;
    private long pauseStart = 0;

    private boolean gameEnded = false; // dodaj pole w klasie

    public GameWindow(GameModel model) { // <-- zmiana z MazeModel na GameModel
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

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(scrollPane, gbc);
        add(centerPanel, BorderLayout.CENTER);

        scoreLabel.setText("Wynik: 0");
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(scoreLabel, BorderLayout.NORTH);

        // Obsługa klawiszy
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    model.setPacmanDirection(-1, 0); break;
                    case KeyEvent.VK_DOWN:  model.setPacmanDirection(1, 0);  break;
                    case KeyEvent.VK_LEFT:  model.setPacmanDirection(0, -1); break;
                    case KeyEvent.VK_RIGHT: model.setPacmanDirection(0, 1);  break;
                }
                // Skrót Ctrl+Shift+Q - powrót do menu
                if (e.getKeyCode() == KeyEvent.VK_Q && e.isControlDown() && e.isShiftDown()) {
                    running = false;
                    JOptionPane.showMessageDialog(GameWindow.this, "Przerwano grę. Powrót do menu.");
                    dispose();
                    new StartMenu();
                }
            }
        });

        // Wątek gry (ruch Pac-Mana, czas, bonusy, kolizje)
        Thread gameThread = new Thread(() -> {
            while (running) {
                // Pauza na pierwszy ruch
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
                    if (now - lastUpgradeCheck > 10000) {
                        lastUpgradeCheck = now;
                        if (Math.random() < 0.2) model.tryAddSpeedUpgrade();
                        if (Math.random() < 0.2) model.tryAddTimeFreezeUpgrade();
                        if (Math.random() < 0.2) model.tryAddExtraLife();
                        if (Math.random() < 0.2) model.tryAddInvincibleUpgrade();
                        if (Math.random() < 0.2) model.tryAddFrightenedUpgrade();
                    }

                    // Licznik czasu
                    long elapsed = (System.currentTimeMillis() - gameStartTime - pausedTime) / 1000;
                    String timeStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60);

                    scoreLabel.setText("Wynik: " + model.getScore() +
                            "   Życia: " + model.lives +
                            "   Czas: " + timeStr);

                    model.fireTableDataChanged();

                    // Kolizja z duchem
                    if (!gameEnded && model.handlePacmanGhostCollision()) {
                        model.lives--;
                        if (model.lives > 0) {
                            model.resetPositions();
                            JOptionPane.showMessageDialog(GameWindow.this, "Straciłeś życie! Pozostało żyć: " + model.lives);
                        } else {
                            gameEnded = true;
                            running = false;
                            JOptionPane.showMessageDialog(GameWindow.this, "Koniec gry! Przegrałeś.");
                            saveScore(model.getScore());
                            dispose();
                        }
                    } else if (!gameEnded && !model.arePointsLeft()) {
                        gameEnded = true;
                        running = false;
                        JOptionPane.showMessageDialog(GameWindow.this, "Wygrałeś! Wszystkie punkty zebrane!");
                        saveScore(model.getScore());
                        dispose();
                    }
                });
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();

        // Wątek duchów
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
                    if (model.isTimeFreezeActive()) {
                        return;
                    }
                    ghostTick++;
                    if (model.isFrightenedActive()) {
                        if (ghostTick % 2 == 0) {
                            for (Ghost.Type t : Ghost.Type.values()) {
                                Ghost g = model.getGhostByType(t);
                                if (g != null && !model.isGhostRespawning(g)) g.moveFrightened(model);
                            }
                        }
                    } else {
                        if (ghostTick % 2 == 0) model.moveSingleGhost(Ghost.Type.BLINKY);
                        if (ghostTick % 6 == 0) model.moveSingleGhost(Ghost.Type.PINKY);
                        if (ghostTick % 3 == 0) model.moveSingleGhost(Ghost.Type.INKY);
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

    // Zmiana rozmiaru komórek w tabeli
    private void resizeCells() {
        int rows = model.getRowCount();
        int cols = model.getColumnCount();

        // Pobierz rozmiar panelu centralnego (nie całego okna!)
        JPanel centerPanel = (JPanel) ((BorderLayout)getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        int panelWidth = centerPanel.getWidth();
        int panelHeight = centerPanel.getHeight();

        // Wyznacz rozmiar komórki, by plansza się mieściła i była kwadratowa
        int cellSize = Math.min(panelWidth / cols, panelHeight / rows);

        // Ustaw rozmiar komórek
        table.setRowHeight(cellSize);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(cellSize);
        }

        // Ustaw rozmiar JTable i JScrollPane dokładnie na planszę
        int tableWidth = cellSize * cols;
        int tableHeight = cellSize * rows;
        table.setPreferredScrollableViewportSize(new Dimension(tableWidth, tableHeight));
        table.setPreferredSize(new Dimension(tableWidth, tableHeight));
        table.revalidate();

        JScrollPane scrollPane = (JScrollPane) table.getParent().getParent();
        scrollPane.setPreferredSize(new Dimension(tableWidth, tableHeight));
        scrollPane.setMaximumSize(new Dimension(tableWidth, tableHeight));
        scrollPane.setMinimumSize(new Dimension(tableWidth, tableHeight));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Wyśrodkuj planszę w panelu
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    // Zapis wyniku do rankingu
    private void saveScore(int score) {
        String name = JOptionPane.showInputDialog(this, "Podaj swój nick:");
        if (name == null || name.trim().isEmpty()) return;

        java.util.List<Ranking> scores = loadScores();

        boolean updated = false;
        for (Ranking entry : scores) {
            if (entry.name.equals(name)) {
                if (score > entry.score) {
                    scores.remove(entry);
                    scores.add(new Ranking(name, score));
                }
                updated = true;
                break;
            }
        }
        if (!updated) {
            scores.add(new Ranking(name, score));
        }

        scores.sort((a, b) -> Integer.compare(b.score, a.score));

        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.FileOutputStream("scores.ser"))) {
            out.writeObject(scores);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu wyniku: " + ex.getMessage());
        }

        showHighScores();
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Ranking> loadScores() {
        java.io.File file = new java.io.File("scores.ser");
        if (!file.exists()) return new java.util.ArrayList<>();
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(file))) {
            return (java.util.List<Ranking>) in.readObject();
        } catch (Exception ex) {
            return new java.util.ArrayList<>();
        }
    }

    // Wyświetlanie rankingu w JList
    private void showHighScores() {
        java.util.List<Ranking> scores = loadScores();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Ranking entry : scores) {
            model.addElement(entry.toString());
        }
        if (model.isEmpty()) model.addElement("Brak wyników.");

        JList<String> list = new JList<>(model);
        list.setFont(new Font("Monospaced", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(300, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Ranking", JOptionPane.PLAIN_MESSAGE);
    }
}
