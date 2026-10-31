package co.unicauca.saberpro.users.infrastructure.persistence;

import co.unicauca.saberpro.users.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    List<UserEntity> findByRoleOrderByFullNameAsc(Role role);
}
