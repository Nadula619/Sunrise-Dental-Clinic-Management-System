package com.sunrisedental.dao;

import com.sunrisedental.model.Treatment;
import java.util.List;

/**
 * Data Access Object Interface for Dental Treatments Catalog.
 */
public interface ITreatmentDAO {
    List<Treatment> findAll();
    Treatment findByCode(String code);
    Treatment findByName(String name);
    boolean save(Treatment treatment);
}
