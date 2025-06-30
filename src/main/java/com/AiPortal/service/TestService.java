package com.AiPortal.service;

import com.AiPortal.entity.TestEntity;
import com.AiPortal.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service-laget som håndterer forretningslogikk relatert til TestEntity.
 */
@Service
public class TestService {

    private final TestRepository testRepository;

    // Bruker constructor injection, som er anbefalt praksis for Spring.
    @Autowired
    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    /**
     * Lagrer en ny TestEntity basert på et navn.
     * @param name Navnet for den nye entiteten.
     * @return Den lagrede TestEntity med generert ID.
     */
    public TestEntity saveTestEntity(String name) {
        TestEntity entity = new TestEntity();
        entity.setName(name);
        return testRepository.save(entity);
    }

    /**
     * Henter alle TestEntity-objekter fra databasen.
     * @return En liste av alle TestEntity-objekter.
     */
    public List<TestEntity> getAllTestEntities() {
        return testRepository.findAll();
    }
}