package co.unicauca.saberpro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Politica de contrasenas: minimo 6 caracteres, al menos un digito, al menos
 * un caracter especial y al menos una mayuscula.
 */
public class DefaultPasswordPolicy implements IPasswordPolicy {

    private static final int MIN_LENGTH = 6;
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^a-zA-Z0-9].*");

    @Override
    public List<String> validate(String plainPassword) {
        List<String> errors = new ArrayList<>();

        if (plainPassword == null || plainPassword.length() < MIN_LENGTH) {
            errors.add("La contrasena debe tener al menos " + MIN_LENGTH + " caracteres.");
            return errors;
        }
        if (!DIGIT.matcher(plainPassword).matches()) {
            errors.add("La contrasena debe incluir al menos un digito.");
        }
        if (!UPPERCASE.matcher(plainPassword).matches()) {
            errors.add("La contrasena debe incluir al menos una letra mayuscula.");
        }
        if (!SPECIAL.matcher(plainPassword).matches()) {
            errors.add("La contrasena debe incluir al menos un caracter especial.");
        }
        return errors;
    }
}
