package com.aljodomo.kontis.persistence;

import com.aljodomo.kontis.model.Report;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
public interface ReportRepository {
    void create(Report report);

    Report findById(String id);

    List<Report> findAll();

    List<Report> findActive();
}
