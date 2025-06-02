import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

class MazeCellRenderer extends DefaultTableCellRenderer {
    private final MazeModel model;

    private final ImageIcon pacmanUpIcon    = new ImageIcon("img/pacman_up.png");
    private final ImageIcon pacmanDownIcon  = new ImageIcon("img/pacman_down.png");
    private final ImageIcon pacmanLeftIcon  = new ImageIcon("img/pacman_left.png");
    private final ImageIcon pacmanRightIcon = new ImageIcon("img/pacman_right.png");
    private final ImageIcon blinkyIcon = new ImageIcon("img/blinky.png");
    private final ImageIcon pinkyIcon = new ImageIcon("img/pinky.png");
    private final ImageIcon inkyIcon = new ImageIcon("img/inky.png");
    private final ImageIcon clydeIcon = new ImageIcon("img/clyde.png");
    private final ImageIcon bonusIcon = new ImageIcon("img/bonus.png");

    private final ImageIcon blinkyUpIcon    = new ImageIcon("img/blinky_up.png");
    private final ImageIcon blinkyDownIcon  = new ImageIcon("img/blinky_down.png");
    private final ImageIcon blinkyLeftIcon  = new ImageIcon("img/blinky_left.png");
    private final ImageIcon blinkyRightIcon = new ImageIcon("img/blinky_right.png");
    private final ImageIcon pinkyUpIcon    = new ImageIcon("img/pinky_up.png");
    private final ImageIcon pinkyDownIcon  = new ImageIcon("img/pinky_down.png");
    private final ImageIcon pinkyLeftIcon  = new ImageIcon("img/pinky_left.png");
    private final ImageIcon pinkyRightIcon = new ImageIcon("img/pinky_right.png");
    private final ImageIcon inkyUpIcon    = new ImageIcon("img/inky_up.png");
    private final ImageIcon inkyDownIcon  = new ImageIcon("img/inky_down.png");
    private final ImageIcon inkyLeftIcon  = new ImageIcon("img/inky_left.png");
    private final ImageIcon inkyRightIcon = new ImageIcon("img/inky_right.png");
    private final ImageIcon clydeUpIcon    = new ImageIcon("img/clyde_up.png");
    private final ImageIcon clydeDownIcon  = new ImageIcon("img/clyde_down.png");
    private final ImageIcon clydeLeftIcon  = new ImageIcon("img/clyde_left.png");
    private final ImageIcon clydeRightIcon = new ImageIcon("img/clyde_right.png");

    public MazeCellRenderer(MazeModel model) {
        this.model = model;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        int cellSize = table.getRowHeight();

        // Pac-Man
        if (model.getPacmanRow() == row && model.getPacmanCol() == column) {
            ImageIcon icon = null;
            int dr = model.getPacmanDirR();
            int dc = model.getPacmanDirC();
            if (dr == -1) icon = pacmanUpIcon;
            else if (dr == 1) icon = pacmanDownIcon;
            else if (dc == -1) icon = pacmanLeftIcon;
            else if (dc == 1) icon = pacmanRightIcon;
            // Fallback: jeśli nie ma ruchu lub brak grafiki, użyj domyślnej
            ImageIcon scaled = getScaledIcon(icon, cellSize);
            if (scaled != null) {
                setIcon(scaled);
                setText("");
            } else if (pacmanRightIcon.getIconWidth() > 0) {
                setIcon(getScaledIcon(pacmanRightIcon, cellSize));
                setText("");
            } else {
                setIcon(null);
                setForeground(Color.YELLOW);
                setText("C");
            }
            setBackground(Color.BLACK);
            return this;
        }

        // Duch
        Ghost ghost = model.getGhostAt(row, column);
        if (ghost != null) {
            ImageIcon icon = null;
            Color fallback = Color.WHITE;
            String fallbackText = "G";
            int dr = ghost.dirR;
            int dc = ghost.dirC;
            switch (ghost.type) {
                case BLINKY:
                    if (dr == -1) icon = blinkyUpIcon;
                    else if (dr == 1) icon = blinkyDownIcon;
                    else if (dc == -1) icon = blinkyLeftIcon;
                    else if (dc == 1) icon = blinkyRightIcon;
                    if (icon == null) icon = blinkyIcon; // fallback
                    fallback = Color.RED; fallbackText = "B";
                    break;
                case PINKY:
                    if (dr == -1) icon = pinkyUpIcon;
                    else if (dr == 1) icon = pinkyDownIcon;
                    else if (dc == -1) icon = pinkyLeftIcon;
                    else if (dc == 1) icon = pinkyRightIcon;
                    if (icon == null) icon = pinkyIcon; // fallback
                    fallback = Color.PINK; fallbackText = "P";
                    break;
                case INKY:
                    if (dr == -1) icon = inkyUpIcon;
                    else if (dr == 1) icon = inkyDownIcon;
                    else if (dc == -1) icon = inkyLeftIcon;
                    else if (dc == 1) icon = inkyRightIcon;
                    if (icon == null) icon = inkyIcon; // fallback
                    fallback = Color.CYAN; fallbackText = "I";
                    break;
                case CLYDE:
                    if (dr == -1) icon = clydeUpIcon;
                    else if (dr == 1) icon = clydeDownIcon;
                    else if (dc == -1) icon = clydeLeftIcon;
                    else if (dc == 1) icon = clydeRightIcon;
                    if (icon == null) icon = clydeIcon; // fallback
                    fallback = Color.ORANGE; fallbackText = "C";
                    break;
            }
            ImageIcon scaled = getScaledIcon(icon, cellSize);
            if (scaled != null) {
                setIcon(scaled);
                setText("");
            } else {
                setIcon(null);
                setForeground(fallback);
                setText(fallbackText);
            }
            setBackground(Color.BLACK);
            return this;
        }

        // Bonus
        Upgrade upgrade = model.getUpgrade();
        if (upgrade != null && upgrade.row == row && upgrade.col == column) {
            ImageIcon scaled = getScaledIcon(bonusIcon, cellSize);
            if (scaled != null) {
                setIcon(scaled);
                setText("");
            } else {
                setIcon(null);
                setForeground(Color.GREEN);
                setText("+");
            }
            setBackground(Color.BLACK);
            return this;
        }

        // Zwykłe pole
        MazeModel.CellType cell = model.grid[row][column];
        setHorizontalAlignment(SwingConstants.CENTER);
        setIcon(null);
        setText("");

        switch (cell) {
            case WALL:
                c.setBackground(Color.BLUE);
                break;
            case GHOST_ROOM:
                c.setBackground(Color.CYAN);
                break;
            case PATH:
                c.setBackground(Color.BLACK);
                break;
            case POINT:
                c.setBackground(Color.BLACK);
                setForeground(Color.YELLOW);
                setText("•");
                break;
            default:
                c.setBackground(Color.BLACK);
        }
        return c;
    }

    private ImageIcon getScaledIcon(ImageIcon icon, int size) {
        if (icon == null || icon.getIconWidth() <= 0) return null;
        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}
