package co.unicauca.saberpro.questions.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OptionEmbeddable {

    @Column(name = "option_text", nullable = false, length = 1000)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "option_order", nullable = false)
    private int position;

    protected OptionEmbeddable() {
    }

    public OptionEmbeddable(String text, boolean correct, int position) {
        this.text = text;
        this.correct = correct;
        this.position = position;
    }

    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
    public int getPosition() { return position; }
}
