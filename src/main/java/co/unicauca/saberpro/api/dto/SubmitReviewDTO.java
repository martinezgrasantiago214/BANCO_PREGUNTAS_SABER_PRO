package co.unicauca.saberpro.api.dto;

/** Cuerpo JSON para enviar una pregunta a revision (HU02). */
public class SubmitReviewDTO {

    private String authorLogin;

    public SubmitReviewDTO() {
    }

    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
}
