package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.User;
import co.unicauca.saberpro.service.OperationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Permite al administrador asignar uno o mas revisores a una pregunta que
 * esta en estado "Pendiente de revision". Al asignar, el sistema notifica
 * por correo a cada revisor.
 */
public class AssignReviewersFrame extends JFrame {

    private final AppContext context;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Autor", "Tema", "Pregunta"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JList<String> reviewersList = new JList<>();

    public AssignReviewersFrame(AppContext context) {
        super("Asignar revisores");
        this.context = context;
        buildUi();
        loadPendingQuestions();
        loadReviewers();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(860, 580);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Asignar revisores"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(new EmptyBorder(12, 12, 12, 12));
        UiTheme.applyBackground(center);

        JLabel info = new JLabel("Preguntas pendientes de revision:");
        info.setForeground(UiTheme.TEXT);
        center.add(info, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel east = new JPanel(new BorderLayout(6, 6));
        UiTheme.applyBackground(east);
        east.setBorder(BorderFactory.createTitledBorder("Revisores disponibles"));
        reviewersList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        east.add(new JScrollPane(reviewersList), BorderLayout.CENTER);
        east.setPreferredSize(new Dimension(260, 0));
        center.add(east, BorderLayout.EAST);

        JButton btnAssign = UiTheme.primaryButton("Asignar revisores");
        btnAssign.addActionListener(e -> onAssign());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(south);
        south.add(btnAssign);
        center.add(south, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void loadPendingQuestions() {
        tableModel.setRowCount(0);
        List<Question> pending = context.getQuestionService().listarPendientesDeRevision();
        for (Question q : pending) {
            tableModel.addRow(new Object[]{q.getId(), q.getAuthorLogin(), q.getTopic(), q.getDirectQuestion()});
        }
        String dbError = context.getQuestionService().getUltimoErrorDeBaseDeDatos();
        if (dbError != null) {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron consultar las preguntas pendientes en la base de datos.\n\n" + dbError,
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadReviewers() {
        List<User> reviewers = context.getUserService().listReviewers();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (User reviewer : reviewers) {
            model.addElement(reviewer.getLogin() + " - " + reviewer.getFullName());
        }
        reviewersList.setModel(model);
    }

    private void onAssign() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta pendiente de revision.",
                    "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> selectedLogins = new ArrayList<>();
        for (String item : reviewersList.getSelectedValuesList()) {
            selectedLogins.add(item.split(" - ")[0]);
        }
        if (selectedLogins.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un revisor.",
                    "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String questionId = (String) tableModel.getValueAt(row, 0);
        OperationResult result = context.getQuestionService().asignarRevisores(questionId, selectedLogins);

        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Revisor(es) asignado(s). La pregunta paso a estado 'En revision' "
                            + "y se notifico por correo a cada revisor.",
                    "Exito", JOptionPane.INFORMATION_MESSAGE);
            loadPendingQuestions();
        } else {
            JOptionPane.showMessageDialog(this, result.getErrorsAsText(),
                    "No fue posible asignar revisores", JOptionPane.WARNING_MESSAGE);
        }
    }
}
