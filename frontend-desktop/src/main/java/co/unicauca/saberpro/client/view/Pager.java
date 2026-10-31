package co.unicauca.saberpro.client.view;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;

/** Control de paginacion reutilizable: Anterior / Pagina x de y / Siguiente. */
public class Pager extends JPanel {

    private final JLabel info = new JLabel();
    private final JButton prev = UiTheme.secondary("◀ Anterior");
    private final JButton next = UiTheme.secondary("Siguiente ▶");
    private int page = 1;
    private int totalPages = 1;

    public Pager(IntConsumer onPageChange) {
        super(new FlowLayout(FlowLayout.CENTER, 10, 4));
        setBackground(UiTheme.BACKGROUND);
        add(prev);
        add(info);
        add(next);
        prev.addActionListener(e -> { if (page > 1) onPageChange.accept(--page); });
        next.addActionListener(e -> { if (page < totalPages) onPageChange.accept(++page); });
        update(1, 1, 0);
    }

    public void update(int page, int totalPages, long totalItems) {
        this.page = page;
        this.totalPages = Math.max(1, totalPages);
        info.setText("Página " + page + " de " + this.totalPages + "  (" + totalItems + " registros)");
        prev.setEnabled(page > 1);
        next.setEnabled(page < this.totalPages);
    }

    public int page() {
        return page;
    }

    public void reset() {
        page = 1;
    }
}
