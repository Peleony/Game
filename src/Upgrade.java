public class Upgrade {
    public enum Type { EXTRA_LIFE }
    public final Type type;
    public int row, col;

    public Upgrade(Type type, int row, int col) {
        this.type = type;
        this.row = row;
        this.col = col;
    }
}