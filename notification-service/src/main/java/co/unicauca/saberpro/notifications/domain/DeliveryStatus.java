package co.unicauca.saberpro.notifications.domain;

/** Resultado de la entrega del correo. */
public enum DeliveryStatus {
    /** Enviado por SMTP real. */
    ENVIADA,
    /** Escrito en la bandeja de salida local (modo demo/desarrollo). */
    SIMULADA,
    /** El proveedor de correo fallo; queda registrada para reintento manual. */
    FALLIDA
}
