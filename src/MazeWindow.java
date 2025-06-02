import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class MazeWindow extends JFrame {
    private final JTable table;
    private final MazeModel model;
    private final JLabel scoreLabel = new JLabel();
    private int ghostTick = 0;

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
            // Prędkość rozgrywki
        Timer timer = new Timer(150, e -> {
            model.stepPacman();
            ghostTick++;

            // Prędkość ruchu duchów
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

            scoreLabel.setText("Wynik: " + model.getScore() + "   Życia: " + model.lives);
            model.fireTableDataChanged();

            // Sprawdź kolizję z duchem
            if (model.isPacmanCaught()) {
                model.lives--;
                if (model.lives > 0) {
                    // Resetuj pozycję Pac-Mana i duchów
                    model.resetPositions();
                    JOptionPane.showMessageDialog(this, "Straciłeś życie! Pozostało żyć: " + model.lives);
                } else {
                    ((Timer)e.getSource()).stop();
                    JOptionPane.showMessageDialog(this, "Koniec gry! Przegrałeś.");
                    saveScore(model.getScore());
                }
            }

            // Sprawdź wygraną
            if (!model.arePointsLeft()) {
                ((Timer)e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "Wygrałeś! Wszystkie punkty zebrane!");
                saveScore(model.getScore());
            }
        });
        timer.start();
            // Ustawienia okna
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
