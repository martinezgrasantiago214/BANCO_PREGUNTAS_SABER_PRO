package co.unicauca.saberpro.user.config;

import co.unicauca.saberpro.user.access.IUserRepository;
import co.unicauca.saberpro.user.access.SQLiteUserRepository;
import co.unicauca.saberpro.user.service.DefaultPasswordPolicy;
import co.unicauca.saberpro.user.service.PBKDF2PasswordHasher;
import co.unicauca.saberpro.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/** Ensambla las dependencias del servicio (DIP): las clases de dominio no conocen Spring. */
@Configuration
public class UserServiceConfig {

    @Bean
    public IUserRepository userRepository(@Value("${saberpro.data-dir}") String dataDir) {
        File dir = new File(dataDir);
        dir.mkdirs();
        return new SQLiteUserRepository("jdbc:sqlite:" + new File(dir, "user-service.db").getAbsolutePath());
    }

    @Bean
    public UserService userService(IUserRepository userRepository) {
        return new UserService(userRepository, new DefaultPasswordPolicy(), new PBKDF2PasswordHasher());
    }
}
