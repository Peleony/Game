import javax.swing.*;
import java.awt.*;
import java.io.*;

public class StartMenu extends JFrame {
    public StartMenu() {
        setTitle("Pac-Man - Menu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JButton newGameBtn = new JButton("New Game");
        JButton leaderboardBtn = new JButton("Leaderboard");
        JButton exitBtn = new JButton("Exit");

        gbc.gridy = 0;
        add(newGameBtn, gbc);
        gbc.gridy = 1;
        add(leaderboardBtn, gbc);
        gbc.gridy = 2;
        add(exitBtn, gbc);

        newGameBtn.addActionListener(e -> {
            String rowsStr = JOptionPane.showInputDialog(this, "Podaj wysokość mapy (nieparzyste od 10 do 100):");
            String colsStr = JOptionPane.showInputDialog(this, "Podaj szerokość mapy (nieparzyste od 10 do 100):");
            try {
                int rows = Integer.parseInt(rowsStr);
                int cols = Integer.parseInt(colsStr);
                if (rows < 7 || cols < 7) throw new NumberFormatException();
                MazeModel model = new MazeModel(rows, cols);
                new MazeWindow(model);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Nieprawidłowe wymiary mapy!");
            }
        });

        leaderboardBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader("scores.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException ex) {
                sb.append("Brak zapisanych wyników.");
            }
            ImageIcon icon = new ImageIcon("icon.png");
            JOptionPane.showMessageDialog(this, sb.toString(), "RANKING", JOptionPane.INFORMATION_MESSAGE, icon);
        });

        exitBtn.addActionListener(e -> System.exit(0));

        setSize(300, 250);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}