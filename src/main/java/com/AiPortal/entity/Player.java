// src/main/java/com/AiPortal/entity/Player.java
package com.AiPortal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representerer en unik fotballspiller.
 * Denne tabellen lagrer grunnleggende informasjon om spilleren for å unngå
 * duplisering av data på tvers av andre tabeller som PlayerMatchStatistics.
 */
@Entity
@Table(name = "players")
public class Player {

    /**
     * Spillerens unike ID fra api-sports.io. Vi bruker denne som vår primærnøkkel
     * og setter den manuelt, i stedet for å la databasen generere den.
     */
    @Id
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String firstname;

    private String lastname;

    private Integer age;

    private String birthDate; // Lagres som streng, f.eks. "1993-05-13"

    private String birthPlace;

    private String birthCountry;

    private String nationality;

    private String height; // Lagres som streng, f.eks. "185 cm"

    private String weight; // Lagres som streng, f.eks. "80 kg"

    private Boolean injured; // Fra spillerprofilen, ikke kampspesifikk

    private String photoUrl; // URL til spillerens bilde


    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getBirthCountry() {
        return birthCountry;
    }

    public void setBirthCountry(String birthCountry) {
        this.birthCountry = birthCountry;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public Boolean getInjured() {
        return injured;
    }

    public void setInjured(Boolean injured) {
        this.injured = injured;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}