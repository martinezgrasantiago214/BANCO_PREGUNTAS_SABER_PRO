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
    /** HU06: el autor saco una version mejorada de una pregunta rechazada. */
    public static final String QUESTION_REVISED = "question.revised";
    /** HU06: una version mejorada llego a revision y se abrio una ronda nueva. */
    public static final String QUESTION_RESUBMITTED = "review.resubmitted";

    private EventTopics() {
    }
}
