package com.AiPortal.repository;

import com.AiPortal.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository-grensesnitt for TestEntity.
 * Utvider JpaRepository for å få standard databaseoperasjoner (save, findById, findAll, delete, etc.).
 * Spring vil automatisk lage en implementasjon av dette grensesnittet ved kjøretid.
 */
@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {
    // Du kan legge til egendefinerte spørringer her senere om nødvendig, f.eks.:
    // List<TestEntity> findByName(String name);
}