package co.unicauca.saberpro.events;

/**
 * Topologia de mensajeria compartida: un exchange de tipo topic y las claves
 * de enrutamiento de cada evento. Cada microservicio declara SU propia cola
 * y decide a que claves se suscribe.
 */
public final class EventTopology {

    public static final String EXCHANGE = "saberpro.events";
    public static final String DEAD_LETTER_EXCHANGE = "saberpro.events.dlx";

    public static final String QUESTION_SUBMITTED = "question.submitted";
    public static final String REVIEWERS_ASSIGNED = "review.reviewers-assigned";
    public static final String EVALUATION_SUBMITTED = "review.evaluation-submitted";
    public static final String OBSERVATION_ADDED = "review.observation-added";
    public static final String REVIEW_COMPLETED = "review.completed";

    private EventTopology() {
    }
}
