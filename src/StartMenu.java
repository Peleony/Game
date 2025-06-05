import javax.swing.*;
import java.awt.*;

public class StartMenu extends JFrame {
    public StartMenu() {
        setTitle("Pac-Man - Menu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Przyciski
        JButton newGameBtn = new JButton("New Game");
        JButton highScoresBtn = new JButton("High Scores");
        JButton exitBtn = new JButton("Exit");

        Dimension buttonSize = new Dimension(180, 40);
        newGameBtn.setPreferredSize(buttonSize);
        highScoresBtn.setPreferredSize(buttonSize);
        exitBtn.setPreferredSize(buttonSize);

        gbc.gridy = 0;
        add(newGameBtn, gbc);
        gbc.gridy = 1;
        add(highScoresBtn, gbc);
        gbc.gridy = 2;
        add(exitBtn, gbc);


        // New Game
        newGameBtn.addActionListener(e -> {
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            JTextField rowsField = new JTextField();
            JTextField colsField = new JTextField();
            panel.add(new JLabel("Wysokość mapy (od 10 do 100):"));
            panel.add(rowsField);
            panel.add(new JLabel("Szerokość mapy (od 10 do 100):"));
            panel.add(colsField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Nowa gra", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int rows = Integer.parseInt(rowsField.getText());
                    int cols = Integer.parseInt(colsField.getText());
                    if (rows < 7 || cols < 7) throw new NumberFormatException();
                    GameModel model = new GameModel(rows, cols);
                    new GameWindow(model);
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Nieprawidłowe wymiary mapy!");
                }
            }
        });

        // High Scores
        highScoresBtn.addActionListener(e -> {
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

            JOptionPane.showMessageDialog(this, scrollPane, "RANKING", JOptionPane.PLAIN_MESSAGE);
        });

        // Exit
        exitBtn.addActionListener(e -> System.exit(0));

        setSize(300, 250);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    
    // Ranking z pliku
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
}