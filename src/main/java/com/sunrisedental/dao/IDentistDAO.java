package com.sunrisedental.dao;

import com.sunrisedental.model.Dentist;
import java.util.List;

/**
 * Data Access Object Interface for Dentists and Practitioners.
 */
public interface IDentistDAO {
    List<Dentist> findAll();
    Dentist findByName(String name);
    boolean save(Dentist dentist);
}
