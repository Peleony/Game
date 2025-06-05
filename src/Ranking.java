import java.io.Serializable;

public class Ranking implements Serializable {
    public final String name;
    public final int score;

    public Ranking(String name, int score) {
        this.name = name;
        this.score = score;
    }

    @Override
    public String toString() {
        return String.format("%-15s %5d", name, score);
    }
}