package co.unicauca.saberpro.users.domain.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Minimo 6 caracteres, un digito, una mayuscula y un caracter especial. */
public class DefaultPasswordPolicy implements PasswordPolicy {

    private static final int MIN_LENGTH = 6;
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^a-zA-Z0-9].*");

    @Override
    public List<String> validate(String plainPassword) {
        List<String> errors = new ArrayList<>();
        if (plainPassword == null || plainPassword.length() < MIN_LENGTH) {
            errors.add("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres.");
            return errors;
        }
        if (!DIGIT.matcher(plainPassword).matches()) {
            errors.add("La contraseña debe incluir al menos un dígito.");
        }
        if (!UPPERCASE.matcher(plainPassword).matches()) {
            errors.add("La contraseña debe incluir al menos una letra mayúscula.");
        }
        if (!SPECIAL.matcher(plainPassword).matches()) {
            errors.add("La contraseña debe incluir al menos un carácter especial.");
        }
        return errors;
    }
}
