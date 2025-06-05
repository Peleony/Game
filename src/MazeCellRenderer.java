import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

class MazeCellRenderer extends DefaultTableCellRenderer {
    private final GameModel model;

    // Ikony Pac-Mana
    private final ImageIcon pacmanUpIcon    = new ImageIcon("img/pacman_up.png");
    private final ImageIcon pacmanDownIcon  = new ImageIcon("img/pacman_down.png");
    private final ImageIcon pacmanLeftIcon  = new ImageIcon("img/pacman_left.png");
    private final ImageIcon pacmanRightIcon = new ImageIcon("img/pacman_right.png");
    private final ImageIcon pacmanInvincibleIcon = new ImageIcon("img/pacman_invincible.png");

    // Ikony duchów
    private final ImageIcon blinkyIcon = new ImageIcon("img/blinky_up.png");
    private final ImageIcon pinkyIcon = new ImageIcon("img/pinky_up.png");
    private final ImageIcon inkyIcon = new ImageIcon("img/inky_up.png");
    private final ImageIcon clydeIcon = new ImageIcon("img/clyde_up.png");

    private final ImageIcon blinkyUpIcon    = new ImageIcon("img/blinky_up.png");
    private final ImageIcon blinkyDownIcon  = new ImageIcon("img/blinky_up.png");
    private final ImageIcon blinkyLeftIcon  = new ImageIcon("img/blinky_left.png");
    private final ImageIcon blinkyRightIcon = new ImageIcon("img/blinky_right.png");

    private final ImageIcon pinkyUpIcon    = new ImageIcon("img/pinky_up.png");
    private final ImageIcon pinkyDownIcon  = new ImageIcon("img/pinky_up.png");
    private final ImageIcon pinkyLeftIcon  = new ImageIcon("img/pinky_left.png");
    private final ImageIcon pinkyRightIcon = new ImageIcon("img/pinky_right.png");

    private final ImageIcon inkyUpIcon    = new ImageIcon("img/inky_up.png");
    private final ImageIcon inkyDownIcon  = new ImageIcon("img/inky_up.png");
    private final ImageIcon inkyLeftIcon  = new ImageIcon("img/inky_left.png");
    private final ImageIcon inkyRightIcon = new ImageIcon("img/inky_right.png");

    private final ImageIcon clydeUpIcon    = new ImageIcon("img/clyde_up.png");
    private final ImageIcon clydeDownIcon  = new ImageIcon("img/clyde_up.png");
    private final ImageIcon clydeLeftIcon  = new ImageIcon("img/clyde_left.png");
    private final ImageIcon clydeRightIcon = new ImageIcon("img/clyde_right.png");

    private final ImageIcon frightenedGhostIcon = new ImageIcon("img/frightened_ghost.png");

    public MazeCellRenderer(GameModel model) { // <-- zmiana z MazeModel na GameModel
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

            if (model.isInvincibleActive()) {
                icon = pacmanInvincibleIcon;
            } else {
                if (dr == -1) icon = pacmanUpIcon;
                else if (dr == 1) icon = pacmanDownIcon;
                else if (dc == -1) icon = pacmanLeftIcon;
                else if (dc == 1) icon = pacmanRightIcon;
            }
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

            if (model.isFrightenedActive()) {
                icon = getScaledIcon(frightenedGhostIcon, cellSize);
                fallback = Color.BLUE;
                fallbackText = "F";
            } else {
                switch (ghost.type) {
                    case BLINKY:
                        if (dr == -1) icon = blinkyUpIcon;
                        else if (dr == 1) icon = blinkyDownIcon;
                        else if (dc == -1) icon = blinkyLeftIcon;
                        else if (dc == 1) icon = blinkyRightIcon;
                        if (icon == null) icon = blinkyIcon;
                        fallback = Color.RED; fallbackText = "B";
                        break;
                    case PINKY:
                        if (dr == -1) icon = pinkyUpIcon;
                        else if (dr == 1) icon = pinkyDownIcon;
                        else if (dc == -1) icon = pinkyLeftIcon;
                        else if (dc == 1) icon = pinkyRightIcon;
                        if (icon == null) icon = pinkyIcon;
                        fallback = Color.PINK; fallbackText = "P";
                        break;
                    case INKY:
                        if (dr == -1) icon = inkyUpIcon;
                        else if (dr == 1) icon = inkyDownIcon;
                        else if (dc == -1) icon = inkyLeftIcon;
                        else if (dc == 1) icon = inkyRightIcon;
                        if (icon == null) icon = inkyIcon;
                        fallback = Color.CYAN; fallbackText = "I";
                        break;
                    case CLYDE:
                        if (dr == -1) icon = clydeUpIcon;
                        else if (dr == 1) icon = clydeDownIcon;
                        else if (dc == -1) icon = clydeLeftIcon;
                        else if (dc == 1) icon = clydeRightIcon;
                        if (icon == null) icon = clydeIcon;
                        fallback = Color.ORANGE; fallbackText = "C";
                        break;
                }
            }
            ImageIcon scaled = getScaledIcon(icon, cellSize);
            if (scaled != null) {
                setIcon(scaled);
                setText("");
            } else {
                setIcon(null);
                setText(fallbackText);
                setBackground(fallback);
            }
            setForeground(Color.BLACK);
            return this;
        }

        // Bonus
        Upgrade upgrade = model.getUpgrade();
        if (upgrade != null && upgrade.row == row && upgrade.col == column) {
            ImageIcon scaled = null;
            Color fallbackColor = Color.LIGHT_GRAY;
            String fallbackText = "+";

            if (upgrade.type == Upgrade.Type.EXTRA_LIFE) {
                scaled = getScaledIcon(new ImageIcon("img/bonus_life.png"), cellSize);
                fallbackColor = Color.GREEN;
                fallbackText = "1";
            } else if (upgrade.type == Upgrade.Type.SPEED) {
                scaled = getScaledIcon(new ImageIcon("img/bonus_speed.png"), cellSize);
                fallbackColor = Color.ORANGE;
                fallbackText = "S";
            } else if (upgrade.type == Upgrade.Type.FRIGHTENED) {
                scaled = getScaledIcon(new ImageIcon("img/bonus_frightened.png"), cellSize);
                fallbackColor = Color.BLUE;
                fallbackText = "F";
            } else if (upgrade.type == Upgrade.Type.INVINCIBLE) {
                scaled = getScaledIcon(new ImageIcon("img/bonus_invincible.png"), cellSize);
                fallbackColor = Color.YELLOW;
                fallbackText = "I";
            } else if (upgrade.type == Upgrade.Type.TIMEFREEZE) {
                scaled = getScaledIcon(new ImageIcon("img/bonus_timefreeze.png"), cellSize);
                fallbackColor = Color.CYAN;
                fallbackText = "T";
            }

            if (scaled != null && scaled.getIconWidth() > 0) {
                setIcon(scaled);
                setText("");
            } else {
                setIcon(null);
                setForeground(fallbackColor);
                setText(fallbackText);
            }
            setBackground(Color.BLACK);
            return this;
        }

        // Zwykłe pole
        GameModel.CellType cell = model.grid[row][column]; // <-- zmiana z MazeModel.CellType
        setHorizontalAlignment(SwingConstants.CENTER);
        setIcon(null);
        setText("");

        switch (cell) {
            case WALL:
                c.setBackground(Color.BLUE);
                break;
            case GHOST_ROOM:
                c.setBackground(Color.BLACK);
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
