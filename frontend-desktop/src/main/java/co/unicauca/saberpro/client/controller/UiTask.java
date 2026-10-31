package co.unicauca.saberpro.client.controller;

import co.unicauca.saberpro.client.api.http.ApiException;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ejecuta las llamadas HTTP fuera del hilo de Swing (SwingWorker) para que
 * la interfaz no se congele, y muestra los errores de la API de forma
 * uniforme.
 */
public final class UiTask {

    private UiTask() {
    }

    public static <T> void run(Component parent, Supplier<T> work, Consumer<T> onSuccess) {
        run(parent, work, onSuccess, null);
    }

    public static <T> void run(Component parent, Supplier<T> work, Consumer<T> onSuccess, Runnable onError) {
        if (parent != null) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                if (parent != null) {
                    parent.setCursor(Cursor.getDefaultCursor());
                }
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    showError(parent, e.getCause());
                    if (onError != null) {
                        onError.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public static void showError(Component parent, Throwable error) {
        String message = error instanceof ApiException api ? api.toUserMessage()
                : "Error inesperado: " + error.getMessage();
        JOptionPane.showMessageDialog(parent, message, "No fue posible completar la operación",
                JOptionPane.WARNING_MESSAGE);
    }
}
