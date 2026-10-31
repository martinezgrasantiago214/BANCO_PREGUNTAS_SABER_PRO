package co.unicauca.saberpro.questions.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, String>,
        JpaSpecificationExecutor<QuestionEntity> {
}
