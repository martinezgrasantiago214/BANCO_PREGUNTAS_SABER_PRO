package co.unicauca.saberpro.common.events;

/**
 * Nombres del exchange y de las claves de enrutamiento de los eventos. Todos
 * los eventos se publican en un unico exchange de tipo "topic"; cada servicio
 * consumidor declara su propia cola y la enlaza con las claves que le
 * interesan. Varias instancias de un mismo servicio comparten la cola
 * (consumidores competidores), lo que permite escalar horizontalmente.
 */
public final class EventTopics {

    public static final String EXCHANGE = "saberpro.events";

    public static final String QUESTION_CREATED = "question.created";
    public static final String QUESTION_SUBMITTED_FOR_REVIEW = "question.submitted";
    public static final String REVIEWERS_ASSIGNED = "review.assigned";
    public static final String QUESTION_EVALUATED = "review.evaluated";

    private EventTopics() {
    }
}
