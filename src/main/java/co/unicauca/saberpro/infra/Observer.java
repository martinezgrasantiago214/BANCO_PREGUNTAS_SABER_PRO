package co.unicauca.saberpro.infra;

/**
 * Patron Observer: contrato que deben cumplir las vistas que necesitan
 * enterarse de un cambio de estado en el banco de preguntas (HU02: "Tan
 * pronto cambie de estado, las vistas seran notificadas para que rendericen
 * su interfaz grafica").
 */
public interface Observer {

    void actualizar();
}
