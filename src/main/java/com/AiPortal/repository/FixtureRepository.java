package com.AiPortal.repository;

import com.AiPortal.entity.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, Long> {
    // Standard JpaRepository-metoder er tilstrekkelig for nå.
    // findById(id) vil bli brukt for å sjekke om en fixture allerede eksisterer.
}